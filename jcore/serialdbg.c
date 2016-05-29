/********************************************************************************
 * Support for serial debugging
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#include "misc.h"
#include "types.h"
#include "intr.h"
#include "config.h"
#include "minic.h"
#include "lock.h"
#include "all.h"
#include "serialdbg.h"
#include "minilzo.h"

int debug_port = -1;
#ifdef LOG_PRINTF
#define LOG_SPACE_SIZE        1024*1024*5
#define COMPRESS_BLOCK_LEN	(64*1024L)
char *log_space = NULL;
char *compressed_log_space = NULL;
int log_cursor = 0;
#endif

/* monitor command history */
#define MAX_HISTORY 64		/* max entries */
#define LEN_HISTORY 128		/* line length */
char history[LEN_HISTORY][MAX_HISTORY];
int history_entries = 0;
int history_current = 0;
void add_to_history(char *str)
{
	strncpy(history[history_current], str, LEN_HISTORY);
	history_current = (history_current + 1) % MAX_HISTORY;
	history_entries++;
	if (history_entries > MAX_HISTORY)
		history_entries = MAX_HISTORY;
}
int get_from_history(int pastindex, char *str, int len)
{
	int i = ((history_current + MAX_HISTORY) - pastindex) % MAX_HISTORY;
	if (i > history_entries)
		return -1;
	strncpy(str, history[i], MAX(len, LEN_HISTORY));
	return 0;
}



static int ser_io_base[] = { 0x3f8, 0x2f8, 0x3e8, 0x2e8 };
static int ser_irq[] = { 4, 3, -1, -1 };

 /* init to 8N1 9600 */
 /* port=0..3 (COM1..COM4) */
void init_serial(int port)
{
	unsigned char dfr;
	unsigned divisor;
	ASSERT(port >= 0);
	ASSERT(port <= 3);

	/* now really init serial */


	dfr = 0x00;
	dfr &= ~0x18;		/* no parity */
	dfr &= ~0x04;		/* 1 stop bit */
	dfr |= 0x03;		/* 8 data bits */
	/* Convert the baud rate into a divisor latch value.  */
#if SERIAL_BAUD_115200
	divisor = 115200 / 115200;
#else
#if SERIAL_BAUD_9600
	divisor = 115200 / 9600;
#else
	divisor = 115200 / 38400;
#endif
#endif

	/* Initialize the serial port.  */
	outb(ser_io_base[port] + 3, 0x80 | dfr);	/* DLAB = 1 */
	outb(ser_io_base[port] + 0, divisor & 0xff);
	outb(ser_io_base[port] + 1, divisor >> 8);
	outb(ser_io_base[port] + 3, 0x03 | dfr);	/* DLAB = 0, frame = 8N1 */
	outb(ser_io_base[port] + 1, 0x00);	/* no interrupts enabled */
	outb(ser_io_base[port] + 4, 0x0b);	/* OUT2, RTS, and DTR enabled */

	/* make sure the FIFO is on */
	outb(ser_io_base[port] + 2, 0x41);	/* 4 byte trigger (0x40); on (0x01) */

	/* Clear all serial interrupts.  */
	inb(ser_io_base[port] + 6);	/* ID 0: read RS-232 status register */
	inb(ser_io_base[port] + 2);	/* ID 1: read interrupt identification reg */
	inb(ser_io_base[port] + 0);	/* ID 2: read receive buffer register */
	inb(ser_io_base[port] + 5);	/* ID 3: read serialization status reg */

	debug_port = port;
}

u4_t sercount = 0;

int is_serial_event()
{
	int irr;
	outb(0x20, 0xa);
	irr = inb(0x20);
	return (irr & 0x18) /* irq 3,4 */ ;
}

void check_serial_timer(struct irqcontext_timer sc)
{
#ifdef DEBUG
	checkStackTrace(curthr(), (u4_t *) (sc.ebp));
#endif
	if (is_serial_event()) {
		save_timer(&sc);
		monitor(NULL);
	}
}


void check_serial()
{
	if (is_serial_event()) {
		monitor(NULL);
	}
	//  printf("*%d",sercount); // 29415
	//if (sercount ==  29415) sys_panic("");
	//if (sercount == 100) traceme();
	sercount++;
}

void ser_enable_break()
{
	ASSERT(debug_port >= 0);
	ASSERT(debug_port <= 3);
	/*printf("Serial break enabled.\n"); */
	outb(ser_io_base[debug_port] + 1, inb(ser_io_base[debug_port] + 1) | 0x04);	/* BREAK */
	outb(ser_io_base[debug_port] + 1, inb(ser_io_base[debug_port] + 1) | 0x01);	/* RX */
	/* break interrupt enabled */
	enableIRQ(ser_irq[debug_port]);
}
static u2_t svalue = 0x0f40;

void ser_breakpoint_ex(struct irqcontext_timer sc)
{
	/*
	   u2_t *screen_start = (u2_t*)0xb8000; 
	   u2_t *screen_end = screen_start + 80*24;
	   u2_t *s;
	   jint iid;
	   jint *st;
	   jint i;
	   for(s=screen_start; s<screen_end; s++) {
	   *s = svalue;
	   }
	   svalue++;
	 */

	/* Clear all serial interrupts.  */
	inb(ser_io_base[debug_port] + 6);	/* ID 0: read RS-232 status register */
	inb(ser_io_base[debug_port] + 2);	/* ID 1: read interrupt identification reg */
	inb(ser_io_base[debug_port] + 0);	/* ID 2: read receive buffer register */
	inb(ser_io_base[debug_port] + 5);	/* ID 3: read serialization status reg */

	/* disable serial interrupts */
	outb(ser_io_base[debug_port] + 1, 0);

	/*dprintf("  SERIALBREAKPOINT\n"); */
	save_timer(&sc);

	monitor(&sc);

	inb(ser_io_base[debug_port] + 0);	/* ID 2: read receive buffer register */
	init_serial(debug_port);
	ser_enable_break();
}

#ifdef KERNEL
void putchar(int ch)
{
	ser_putchar(debug_port, ch);
}
void puts(char *s)
{
	dbg_print(debug_port, s);
}
#endif

#define TRANSMIT(ch) while (!(inb(ser_io_base[port] + 5) & 0x20)); outb(ser_io_base[port] + 0, ch);

void ser_putchar(int port, int ch)
{
#ifdef CHECK_SERIAL
	static int less_checks;
	if ((less_checks++ % 20) == 0)
		check_serial();
	if (ch == 'ÿ')
		monitor(NULL);
#endif
	//  DISABLE_IRQ;

	if (ch == '\n') {
		TRANSMIT('\r');
		TRANSMIT('\n');
	} else {
		TRANSMIT(ch);
	}

	/* Wait for the transmit buffer to become available.  */

	//RESTORE_IRQ;
}

#define CTRL_C 3
#define ESC 27
#define CURSOR_UP 65
#define CURSOR_DOWN 66
#define CURSOR_RIGHT 67
#define CURSOR_LEFT 68

#define CHAR_UNKNOWN 128
#define CHAR_CURSOR_UP 129
#define CHAR_CURSOR_DOWN 130
#define CHAR_CURSOR_RIGHT 131
#define CHAR_CURSOR_LEFT 132

unsigned char ser_waitforchar(int port)
{
	int ch;

	/* Wait for a character to arrive.  */
	for (;;) {
		u1_t d;
		d = inb(ser_io_base[port] + 5);
		if (d & 0x10) {
			pc_reset();
		}
		if (d & 0x01) {
			/* Grab it.  */
			ch = inb(ser_io_base[port] + 0);
			/*printf("*GOT %d\n", ch); */
			break;
		}
	}
	return ch;
}


static unsigned char ser_getchar(int port)
{
	unsigned char ch;
	ch = ser_waitforchar(port);
	if (ch == '\b')
		return ch;	/* don't echo */
	else if (ch == ESC)
		if (ser_waitforchar(port) == 91) {
			ch = ser_waitforchar(port);
			switch (ch) {
			case CURSOR_DOWN:
				return CHAR_CURSOR_DOWN;
			case CURSOR_RIGHT:
				return CHAR_CURSOR_RIGHT;
			case CURSOR_LEFT:
				return CHAR_CURSOR_LEFT;
			case CURSOR_UP:
				return CHAR_CURSOR_UP;
			default:
				return CHAR_UNKNOWN;
			}
		}
	ser_putchar(port, ch);	/* echo */
	return ch;
}

int ser_trygetchar(int port)
{
	int ch;

	if (inb(ser_io_base[port] + 5) & 0x01) {
		ch = inb(ser_io_base[port] + 0);
		return ch;
	}
	return -1;
}

void dbg_print(int port, char *s)
{
	while (*s) {
		ser_putchar(port, *s);
		s++;
	}
}

#define SLOW_DOWN __asm__ __volatile__("outb %%al, $0x80": :)

void ser_dump(int port, const char *ptr, unsigned int size)
{
	DISABLE_IRQ;
	while (size--) {
		/* Wait for the transmit buffer to become available.  */
		while (!(inb(ser_io_base[port] + 5) & 0x20)) {
			SLOW_DOWN;
		}
		outb(ser_io_base[port] + 0, *ptr++);
	}
	RESTORE_IRQ;
}
void ser_getdata(int port, char *ptr, u4_t size)
{
	while (size--) {
		*ptr++ = ser_waitforchar(port);
	}
}


int read_line(int port, char *msg, int msg_size)
{
	unsigned char ch;
	int i = 0;
	int pastindex = 0;
	while ((ch = ser_getchar(port)) != '\r') {
		switch (ch) {
		case CHAR_CURSOR_UP:
			if (get_from_history(++pastindex, msg, msg_size) == -1) {
				strcpy(msg, "?????");
			}
			i = strlen(msg);
			ser_putchar(port, '\r');	/* echo */
			printf("Monitor: %s                                               ", msg);
			break;
		case CHAR_CURSOR_DOWN:
			if (get_from_history(--pastindex, msg, msg_size) == -1) {
				strcpy(msg, "?????");
			}
			i = strlen(msg);
			ser_putchar(port, '\r');	/* echo */
			printf("Monitor: %s                                               ", msg);
			break;
		case CHAR_CURSOR_LEFT:
		case '\b':
			if (i > 0) {
				i--;
				ser_putchar(port, '\b');	/* delete      */
				ser_putchar(port, ' ');	/*  last       */
				ser_putchar(port, '\b');	/*   character */
			}
			break;
		default:
			if (i >= msg_size)
				return -1;
			msg[i++] = ch;
		}
	}
	if (i >= msg_size)
		return -1;
	msg[i] = '\0';
	add_to_history(msg);
	ser_putchar(port, '\n');	/* echo */
	return i;
}

void ser_putc(int x, char c)
{
	ser_putchar(debug_port, c);
}

#ifdef LOG_PRINTF
void init_log_space()
{
	log_space = jxmalloc(LOG_SPACE_SIZE MEMTYPE_OTHER);
	compressed_log_space = jxmalloc(COMPRESS_BLOCK_LEN + COMPRESS_BLOCK_LEN / 64 + 16 + 3 MEMTYPE_OTHER);
	memset(log_space, 0, LOG_SPACE_SIZE);
	log_cursor = 0;
}
void mem_putc(int x, char c)
{
	if (log_cursor >= LOG_SPACE_SIZE) {	/* log space full ? */
		printf2mem = 0;	/* disable logging to mem */
		return;
	}
	log_space[log_cursor++] = c;
}


#define HEAP_ALLOC(var,size) \
	long __LZO_MMODEL var [ ((size) + (sizeof(long) - 1)) / sizeof(long) ]

static HEAP_ALLOC(wrkmem, LZO1X_1_MEM_COMPRESS);

#define min(a,b) ((a)<(b)?(a):(b))
void transfer_printflog()
{
	int compressed_len;
	int start = 0, len;

	printf2mem = 0;		/* disable logging */

	/* compress it */
	if (lzo_init() != LZO_E_OK)
		sys_panic("lzo_init() failed");

	while (start < log_cursor) {
		/* split in blocks of size < COMPRESS_BLOCK_LEN */
		len = min(COMPRESS_BLOCK_LEN, log_cursor - start);
		if (lzo1x_1_compress(log_space + start, len, compressed_log_space, &compressed_len, wrkmem) != LZO_E_OK)
			sys_panic("lzo1x_1_compress(...) failed");
		/* transfer it */
		DUMP(&compressed_len, sizeof(int));
		DUMP(compressed_log_space, compressed_len);
		start += len;
	}
	compressed_len = 0;
	DUMP(&compressed_len, sizeof(int));
}
#endif



/* 
void ser_putc(int x, char c) {
}
#define debug_port -1
*/
