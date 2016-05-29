/*
 * Serial control program for JX.
 * Authors: Michael Golm, Jörg Baumann
 */
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <assert.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <signal.h>
#ifdef LINUX
#  include <asm/ioctls.h>
#  define CBAUDEXT CBAUDEX
#  include <termios.h>
#endif

#ifdef SOLARIS
// #  include <pty.h>
#  include <termios.h>
#endif

#include "types.h"
#include "minilzo.h"
#include "crc32.h"

#define CTRL_C 3
#define ESC 27
#define CURSOR_UP 65
#define CURSOR_DOWN 66
#define CURSOR_RIGHT 67
#define CURSOR_LEFT 68


#define ERR(x) { \
  int err; \
  while ((err=(int)(x))<0) { \
    if (errno!=EINTR) { \
      fprintf(stderr, "wrap: %s: %d: %s\n", __FILE__, __LINE__, strerror(errno));\
      exit(1);\
    } \
  } \
}

sig_atomic_t quit = 0;
int fd = -1;
int reset = 0;
char sigint_char = CTRL_C;

void install_sighandler(int sig, void (*handler) (int))
{
	struct sigaction act;

	memset(&act, 0, sizeof(act));
	act.sa_handler = handler;
	sigemptyset(&act.sa_mask);
	if (sigaction(sig, &act, NULL) != 0) {
		perror("Error installing signal handler");
		exit(1);
	}
}

void catch_int(int sig)
{
	//fprintf(stderr, "Forward SIGINT to target.\n");
	tcsendbreak(fd, 0);
	tcsendbreak(fd, 0);
	write(fd, &sigint_char, 1);
	reset = 1;
}

void catch_usr1(int sig)
{
	char *boot = "\r";
	reset = 0;
	if (fd > 0)
		write(fd, boot, strlen(boot));
}

void catch_quit(int sig)
{
	fprintf(stderr, "Quit.\n");
	kill(getpid(), SIGTERM);
	quit = 1;
}

/* insert new wrapped commands here */
char *trigger[] = { "dpa\n", "d1umpfile", "dpgc\n", "rswitches\n", "rlogts\n", "printflog\n", NULL };

/* filedescriptor for binary output file */
int bout = -1;

/* buffer for binary transfer */
#define PACKETBUFSIZE 20000
char packetbuf[PACKETBUFSIZE];

u8_t getMilliSeconds()
{
	struct timeval tv;
	u8_t ms;

	if (gettimeofday(&tv, NULL) == -1) {
		perror("gettime");
		exit(1);
	}
	ms = tv.tv_usec / 1000;
	ms += tv.tv_sec * 1000;

	return ms;
}

void init_stdin()
{
	struct termios arg;
	int flags;

	if (tcgetattr(STDIN_FILENO, &arg) == -1) {
		perror("Fehler beim Lesen der Terminalparameter");
		exit(EXIT_FAILURE);
	}
	arg.c_lflag &= ~ICANON;
	arg.c_lflag &= ~ECHO;
	arg.c_cc[VMIN] = 1;
	arg.c_cc[VTIME] = 0;
	if (tcsetattr(STDIN_FILENO, TCSANOW, &arg) == -1) {
		perror("Fehler beim Einstellen der Terminalparameter");
		exit(EXIT_FAILURE);
	}
	flags = fcntl(0, F_GETFL, 0);
	ERR(fcntl(0, F_SETFL, flags | O_NONBLOCK));
}

void init_serial(char *name)
{
	struct termios ts;

	ERR(fd = open(name, O_RDWR | O_NONBLOCK | O_NOCTTY));

	if (!isatty(fd)) {
		fprintf(stderr, "not a tty: %s\n", name);
		exit(1);
	}

	ERR(ioctl(fd, TIOCEXCL, NULL));

	ts.c_iflag = BRKINT | IGNPAR | IXON;
	ts.c_oflag = 0;
#ifdef SOLARIS
	ts.c_cflag = CLOCAL | CREAD | CS8 | CIBAUD | B9600;
#else
	ts.c_cflag = CLOCAL | CREAD | CS8;
#endif

	ts.c_lflag = IEXTEN | ECHOCTL | ECHOKE | ECHOK | ECHOE;
	/*assert(ts.c_iflag == 02006);
	   assert(ts.c_oflag == 0);
	   assert(ts.c_cflag == 03204275);
	   printf("FIXME %o\n",ts.c_cflag);
	   assert(ts.c_lflag == 0105060);
	   assert(ts.c_iflag == 0x406);
	   assert(ts.c_oflag == 0);
	   assert(ts.c_cflag == 0xd08bd);
	   assert(ts.c_lflag == 0x8a30); */

	/* set to 38400 baud */
#if ! defined(SERIAL_BAUD_115200) && ! defined(SERIAL_BAUD_9600)
	fprintf(stderr, "Set serial line speed to 38400\n");
	cfsetispeed(&ts, B38400);
	cfsetospeed(&ts, B38400);
#endif

#ifdef SERIAL_BAUD_115200
	fprintf(stderr, "Set serial line speed to 115200\n");
	cfsetispeed(&ts, B115200);
	cfsetospeed(&ts, B115200);
#endif

#ifdef SERIAL_BAUD_9600
	fprintf(stderr, "Set serial line speed to 9600\n");
	cfsetispeed(&ts, B9600);
	cfsetospeed(&ts, B9600);
#endif

	/*memcpy(ts.c_cc, 
	   "\x03\x1c\x7f\x08\x64\x02\x00\x00\x11\x13\x1a"
	   "\x19\x12\x0f\x17\x16\x00\x00\x00", 19);
	 */

	memcpy(ts.c_cc, "\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00" "\x00\x00\x00\x00\x00\x00\x00\x00", 19);
	ERR(ioctl(fd, TCSETSW, &ts));
	ERR(fcntl(fd, F_SETFL, O_RDONLY | O_NONBLOCK));
}


#define MONITOR "Monitor: "
#define MONITOR2 "Monitor: \r"

static int sequence = 1;

#define INBUFSIZE 1024
char inbuf[INBUFSIZE];

#define BUFSIZE 20000

u1_t buf[BUFSIZE];
int bufSpace = BUFSIZE;
u1_t *bufPtr = buf;
int bufNumberUnprocessed = 0;
u1_t *bufUnprocessed = buf;

int magiccomplete = 0;

int dbgfd = -1;
int pressanycomplete = 0;
const char pressany[] = "Press any key to continue.";

int readdata(int fd, u1_t * data, int size)
{
	int t;
	int ret = 0;
	int n, i;
#ifdef SERIAL_BAUD_115200
	int timeout = 300 + size * 1;
#endif
#if ! defined(SERIAL_BAUD_115200) && ! defined(SERIAL_BAUD_9600)
	int timeout = 1000 + size * 3;
#endif
#ifdef SERIAL_BAUD_9600
	int timeout = 1000 + size * 5;
#endif
	u8_t start;

	//fprintf(stderr, "need %d bytes ", size);

	start = getMilliSeconds();

	/* first empty buffer (if data is available) */
	n = size < bufNumberUnprocessed ? size : bufNumberUnprocessed;
	memcpy(data, bufUnprocessed, n);
	//for(i=0;i<n;i++) fprintf(stderr,"#%x ", data[i]);
	size -= n;
	data += n;
	bufNumberUnprocessed -= n;
	bufUnprocessed += n;

	//  fprintf(stderr, "got %d bytes ", n);

	if (size == 0)
		return;

	while (size) {
		for (;;) {
			errno = 0;
			if ((ret = read(fd, data, size)) != 0) {
				if (ret > 0 || (ret == -1 && errno != EAGAIN))
					break;
			}
			if ((getMilliSeconds() - start) > timeout) {
				u4_t ackseq = sequence - 1;
				printf("TIMEOUT %d bytes missing\n", size);
				write(fd, &ackseq, 4);
				return 0;
			}
		}
		//for(n=0;n<ret;n++) fprintf(stderr,"%x ", data[n]);
		if (ret == -1) {
			perror("readdata");
			exit(1);
		}
		size -= ret;
		data += ret;
	}
	return 1;
}

/* 0=ok -1=error */
int check_checksum(void *data, unsigned long len, unsigned short checksum)
{
	unsigned long sum = checksum;
	u2_t *d = (u2_t *) data;
	len >>= 1;
	while (len--) {
		sum += *(d++);
		if (sum > 0xFFFF)
			sum -= 0xFFFF;
	}
	if (sum != 0xffff) {
		return -1;
	}
	return 0;
}

int anykey_sequence()
{
	int i;
	if (bufNumberUnprocessed > 0) {
		for (i = 0; i < bufNumberUnprocessed; i++) {
			if (dbgfd >= 0)
				write(dbgfd, &bufUnprocessed[i], 1);
			if (bufUnprocessed[i] == pressany[pressanycomplete]) {
				pressanycomplete++;
				if (pressany[pressanycomplete] == 0) {
					fprintf(stderr, "key pressed ;-)\n");
					pressanycomplete = 0;
					return 1;
				}
			} else {
				pressanycomplete = 0;
			}
		}
	}
	return 0;
}

int magic_sequence()
{
	int i;
	if (bufNumberUnprocessed > 0) {
		for (i = 0; i < bufNumberUnprocessed; i++) {
			if (bufUnprocessed[i] == 0xaa) {
				magiccomplete++;
				if (magiccomplete == 4) {
					magiccomplete = 0;
					/* write buf to stdout until magic */
					i++;
					if (i > 4) {
						ERR(write(1, bufUnprocessed, i - 4));
					}
					fprintf(stderr, "GOT MAGIC!\n");
					bufUnprocessed += i;
					bufNumberUnprocessed -= i;
					/*if (bufSpace == 0) {
					   bufPtr = buf;
					   bufUnprocessed = buf;
					   bufSpace = BUFSIZE;
					   } */
					return 1;
				}
			} else {
				magiccomplete = 0;
			}
		}
	}
	return 0;
}

#if !defined(USE_LITTLE_ENDIAN) && !defined(USE_BIG_ENDIAN)
ERROR:need USE_LITTLE_ENDIAN or USE_BIG_ENDIAN
#endif
    u4_t rdint(u1_t * buf)
{
#ifdef USE_LITTLE_ENDIAN
	return *(u4_t *) buf;
#endif
#ifdef USE_BIG_ENDIAN
	u4_t n;
	buf += 3;
	n = *buf--;
	n <<= 8;
	n += *buf--;
	n <<= 8;
	n += *buf--;
	n <<= 8;
	n += *buf--;
	return n;
#endif
}

void wrint(u1_t * buf, u4_t n)
{
#ifdef USE_LITTLE_ENDIAN
	*(u4_t *) buf = n;
#endif
#ifdef USE_BIG_ENDIAN
	buf[0] = n & 0xff;
	buf[1] = (n >> 8) & 0xff;
	buf[2] = (n >> 16) & 0xff;
	buf[3] = (n >> 24) & 0xff;
#endif
}


int main(int argc, char *argv[])
{
	int out;
	fd_set rfds;
	int r, s;
	char *ptr;
	int lastMonitor;
	char fn[1000];
	char *ttyname;
	char *dirname = "./";
	int doBoot = 0;
	int ndown = 0;

	if (argc < 2) {
		fprintf(stderr, "usage: %s [-b <number down>] [-d <dir>] <tty>\n", *argv);
		fprintf(stderr,
			"   -b <number down>: send simulated keystrokes to grup to start booting; send cursor down (or up if negative)\n");
		fprintf(stderr, "   -d <dir>: create transferred binary files in directory <dir>\n");
		exit(1);
	}

	argv++;

	for (;;) {
		if (strcmp(*argv, "-b") == 0) {
			argv++;
			doBoot = 1;
			ndown = atoi(*argv);
			argv++;
		} else if (strcmp(*argv, "-D") == 0) {
			argv++;
			dbgfd = open(*argv, O_RDWR);
			argv++;
		} else if (strcmp(*argv, "-d") == 0) {
			argv++;
			dirname = *argv;
			argv++;
		} else {
			break;
		}
	}

	ttyname = *argv;
	init_serial(ttyname);

	init_stdin();


	install_sighandler(SIGQUIT, catch_quit);
	install_sighandler(SIGINT, catch_int);
	install_sighandler(SIGUSR1, catch_usr1);

	/*
	   if (argc == 3) {
	   fprintf(stderr, "going to data exchange\n");
	   sequence = -1;
	   goto dataexchange;
	   }
	 */

	if (doBoot) {
		char boot[1];
		fprintf(stderr, "Write to grub. Down %d\n", ndown);
		*boot = '\r';
		write(fd, boot, 1);
		if (ndown < 0) {
			ndown = -ndown;
			for (; ndown > 0; ndown--) {
				*boot = ESC;
				write(fd, boot, 1);
				*boot = 91;
				write(fd, boot, 1);
				*boot = CURSOR_UP;
				write(fd, boot, 1);
			}
		} else {
			for (; ndown > 0; ndown--) {
				*boot = ESC;
				write(fd, boot, 1);
				*boot = 91;
				write(fd, boot, 1);
				*boot = CURSOR_DOWN;
				write(fd, boot, 1);
			}
		}
		*boot = '\r';
		write(fd, boot, 1);
	}

	out = 1;
	while (!quit) {
		FD_ZERO(&rfds);
		FD_SET(fd, &rfds);
		FD_SET(0, &rfds);

		ERR(r = select(6, &rfds, NULL, NULL, NULL));
		if (quit)
			break;

		/* stdin -> serial */
		if (FD_ISSET(0, &rfds)) {
			errno = 0;
			while ((s = read(0, inbuf, INBUFSIZE)) != 0) {
				int i;
				char *cr = "\r";
				if (s == -1)
					break;	// ERR(s);
				//fprintf(stderr, "Write %d bytes to serial\n", s);
				for (i = 0; i < s; i++) {
					if (inbuf[i] == '\n') {	/* line feed */
						//fprintf(stderr, "Write CR+LF\n");
						write(fd, cr, 1);
					} else
						write(fd, inbuf + i, 1);
				}
			}
		}
		//    fprintf(stderr, "**\n", s);

		/* serial -> buffer */
		if (bufSpace > 0 && FD_ISSET(fd, &rfds)) {
			errno = 0;
			while ((s = read(fd, bufPtr, bufSpace)) != 0) {
				if (s == -1 && errno == EAGAIN)
					break;
				if (s == -1) {
					perror("readser->buf");
					exit(1);
				}
				ERR(s);
				bufNumberUnprocessed += s;
				bufPtr += s;
				bufSpace -= s;
				//fprintf(stderr, "Read %d bytes from serial\n", s);
				if (bufSpace == 0)
					break;	/* someone needs to empty the buffer */
			}
		}
		//if (reset && anykey_sequence()) {
		if (anykey_sequence()) {
			char boot[1];
			*boot = '\r';
			write(fd, boot, 1);
			reset = 0;
		}

		if (magic_sequence()) {
			unsigned long n, i;
			char c;
			unsigned short checksum;
			int needmagic = 0;

			/* exchange data */
		      dataexchange:
			//if (reset) reset=0;
			for (;;) {
				int t;
				int TIMEOUT = 10000;
				int TIMEOUT1 = 1;
				int ret;
				char ok;
				u4_t scratch;
				int err = 0;
				unsigned long seq;
				unsigned char a;
				int mcount = 4;
				int error;
			      magic:
				error = 0;
				if (reset) {
					// end data exchange
					if (bout != -1)
						close(bout);
					bout = -1;
					reset = 0;
					break;
				}
				if (needmagic) {
					if (!readdata(fd, &a, 1))
						goto magic;
					if (a == 0xaa)
						mcount--;
					else
						mcount = 4;
					if (mcount > 0)
						goto magic;
					fprintf(stderr, "MAGIC\n");
				}
				needmagic = 1;
				if (!readdata(fd, &scratch, 4))
					goto magic;
				seq = rdint(&scratch);
				if (!readdata(fd, &scratch, 4))
					goto magic;
				n = rdint(&scratch);

				if ((sequence == seq) && (n == 0)) {
					/* end of transmission */
					//fprintf(stderr, "END OF TRANSMISSION\n");
					wrint(&scratch, seq);
					write(fd, &scratch, 4);
					sequence++;
					sequence = -1;	// in case kernel is rebooted
					break;
				}
				if (n < PACKETBUFSIZE) {
					printf("n=%d\n", n);
					if (!readdata(fd, packetbuf, n))
						goto magic;
					if (!readdata(fd, &checksum, 2))
						goto magic;
				} else {
					fprintf(stderr, "ERROR seq=%p n=%p\n", seq, n);
					error = 1;
				}
				if (!error && (check_checksum(packetbuf, n, checksum) != 0)) {
					fprintf(stderr, "wrong checksum\n");
					error = 1;
				}
				if (!error && (sequence != -1) && (seq != sequence)) {
					fprintf(stderr, "wrong sequence sequence!=seq %d!=%d\n", sequence, seq);
					error = 1;
				}
				if (error) {
					u4_t ackseq = sequence - 1;
					if (((sequence != -1) && (seq != sequence)))
						fprintf(stderr, "ERROR sequence!=seq %d!=%d (sending %d)\n", sequence, seq,
							ackseq);
					wrint(&scratch, ackseq);
					ret = write(fd, &scratch, 4);
					if (ret != 4)
						exit(0);
				} else {
					if (sequence == -1)
						sequence = seq;
					fprintf(stderr, "OK %d\n", sequence);
					if (bout == -1 && sequence == 1) {
						// open file
						u4_t l, la;
						l = rdint(packetbuf);
						//fprintf(stderr, "GOT NAME size=%d\n", l);
						la = (l + 1) & (~1);	// align string length at 2 byte border
						strcpy(fn, dirname);
						strcat(fn, "/");
						strncat(fn, packetbuf + 4, l);
						//fprintf(stderr, "\nname=%s\n", fn);
						ERR(bout = open(fn, O_CREAT | O_TRUNC | O_WRONLY, S_IRWXU | S_IRGRP | S_IROTH));
						fprintf(stderr, "\nstarted logging to %s\n", fn);
						//write(bout, packetbuf+l+4, n-l-4); // write rest of first packet
					} else if (bout != -1) {
						write(bout, packetbuf, n);
					}
					wrint(&scratch, seq);
					ret = write(fd, &scratch, 4);
					if (ret != 4)
						exit(0);
					sequence++;
				}
			}
			fprintf(stderr, "\nBinary transmission completed successfully.\n");
			if (bout != -1)
				ERR(close(bout));
			bout = -1;
		}

		if (bufNumberUnprocessed > 0) {
			//fprintf(stderr, "Write %d serial bytes to stdout\n", bufNumberUnprocessed);
			ERR(write(1, bufUnprocessed, bufNumberUnprocessed));
			bufUnprocessed += bufNumberUnprocessed;
			bufNumberUnprocessed = 0;
			if (bufSpace == 0) {
				bufPtr = buf;
				bufUnprocessed = buf;
				bufSpace = BUFSIZE;
			}
		}

	}
}
