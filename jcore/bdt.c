#ifdef BINARY_DATA_TRANSMISSION

/* support for binary data transmission (bdt)*/

#include "all.h"

#include "bdt.h"
#include "crc32.h"
#include "minilzo.h"

#ifndef KERNEL
#include <termios.h>
#include <errno.h>
#include <string.h>
#endif

/* private part */
#define PACKAGE_HEAD_OVERHEAD (sizeof(u1_t) + sizeof(u1_t) + sizeof(s2_t))
#define PACKAGE_OVERHEAD (PACKAGE_HEAD_OVERHEAD + sizeof(u4_t))
#define PACKAGE_MAX_RAW_SIZE (1024 * 2)
#define PACKAGE_MAX_SIZE (PACKAGE_MAX_RAW_SIZE + PACKAGE_MAX_RAW_SIZE / 64 + 16 + 3)
#define ACK_TIMEOUT 50

#define MAX_LEN (1024 * 2)

struct bdtBuffer_s {
	u2_t used;
	u1_t hasCRC;
	u1_t isPlain;
	u1_t tag;
	u1_t data[PACKAGE_MAX_SIZE + PACKAGE_OVERHEAD];
	u1_t raw[PACKAGE_MAX_RAW_SIZE];
};

typedef struct bdtBuffer_s bdtBuffer;

#define HEAP_ALLOC(var,size) long __LZO_MMODEL var [ ((size) + (sizeof(long) - 1)) / sizeof(long) ]

static HEAP_ALLOC(wrkmem, LZO1X_1_MEM_COMPRESS);
static bdtBuffer bdtOutBuf;
static int bdtSeqNum;
static int bdtOK = 1;

#ifdef BDT_ACCOUNTING
static int accACKTries = 0;
static int accACKGots = 0;
static int accACKSuccesses = 0;
static int accACKTimeouts = 0;
static int accNPackages = 0;
static int accNSent = 0;

#define ACC(x) x
static void accPrint()
{
	fprintf(stderr, "T:%d G:%d S:%d C:%d P:%d S:%d\n", accACKTries, accACKGots, accACKSuccesses, accACKTimeouts, accNPackages,
		accNSent);
}
#else
static void accPrint()
{
}

#define ACC(x)
#endif

static struct {
	u4_t id_len;
	u4_t size;
	u4_t nelem;
	const char *id;
	u4_t sizeInBytes;
	u4_t compress;
	u4_t sentBytes;
} chunk;

static int bdtInitialized = 0;

enum { COMPRESSED, PLAIN };

// FIXME
static u8_t getMilliSeconds()
{
	/*  struct timeval tv;
	   u8_t ms;

	   gettimeofday(&tv, NULL);  
	   ms = tv.tv_usec / 1000;
	   ms += tv.tv_sec * 1000;

	   return ms;
	 */

	u8_t ms;
	asm volatile ("rdtsc":"=A" (ms):);
	ms /= 500 * 1000;
	return ms;
}

static void bdtBufferInit(bdtBuffer * buf, u1_t tag, u1_t isPlain)
{
	buf->used = 0;
	buf->hasCRC = 0;
	buf->tag = tag;
	buf->isPlain = isPlain;
}

static int bdtInit()
{
	bdtInitialized = 1;

	if (lzo_init() != LZO_E_OK)
		sys_panic("lzo_init() failed");

	return 0;
}

#ifdef KERNEL
int bdtGetChar()
{
	return ser_trygetchar(debug_port);
}

static void bdtSendBytes(const void *ptr, u4_t len)
{
	ser_dump(debug_port, ptr, len);
}
#else

int bdtGetChar()
{
	fd_set rfds;
	struct timeval timeout;
	int fd = 0;
	int r;

	FD_ZERO(&rfds);
	FD_SET(fd, &rfds);

	timeout.tv_sec = 0;
	timeout.tv_usec = 1;

	if (!((r = select(fd + 1, &rfds, NULL, NULL, &timeout)) >= 0)) {
		fprintf(stderr, "ERR: %s\n", strerror(errno));
		sys_panic("ERR: %s", strerror(errno));
	}

	if (r && FD_ISSET(fd, &rfds)) {
		r = 0;
		if (!(1 == read(fd, &r, 1))) {
			fprintf(stderr, "ERR: %s\n", strerror(errno));
			sys_panic("ERR: %s", strerror(errno));
		}
		return r;
	}
	return -1;
}

static void bdtSendBytes(const void *ptr, u4_t len)
{
	struct termios ts1, ts2;
	u4_t l;
	tcgetattr(1, &ts1);
	ts2 = ts1;
	ts2.c_oflag &= ~ONLCR;
	tcsetattr(1, TCSANOW, &ts2);

	if ((l = write(1, ptr, len)) != len) {
		tcsetattr(1, TCSANOW, &ts1);
		//printf("\nMonitor: ");
		fprintf(stderr, "printProfileAging: write error %s", strerror(errno));
		sys_panic("printProfileAging: write error %s", strerror(errno));
	}
	/*  u4_t i;

	   for (i=0; i< len; i++) {
	   u4_t,j,x,a;

	   a = rand()%2000;
	   if (a<1) x=0;
	   else if (a<2) x=2;
	   else x=1;
	   if (x!=1) fprintf(stderr, "\n#\n");
	   for (j=0;j<x;j++)
	   if (write(1, ptr+i, 1) != 1) {
	   tcsetattr(1, TCSANOW, &ts1);
	   //printf("\nMonitor: ");
	   fprintf(stderr, "printProfileAging: write error %s", strerror(errno));
	   sys_panic("printProfileAging: write error %s", strerror(errno));
	   }
	   } */
	tcsetattr(1, TCSANOW, &ts1);
}
#endif				/* KERNEL */

#define ACK_LEN 3

static int bdtAck(u1_t num)
{
	static u1_t bdtAckBuf[ACK_LEN];
	static int pos = 0;

	int c;

	c = bdtGetChar();

	ACC(accACKTries++);
	if (c < 0)
		return 0;
	ACC(accACKGots++);
	bdtAckBuf[pos] = c;
	pos = (pos + 1) % ACK_LEN;

	if (bdtAckBuf[(pos + ACK_LEN - 3) % ACK_LEN] != 'A')
		return 0;
	if (bdtAckBuf[(pos + ACK_LEN - 2) % ACK_LEN] != num)
		return 0;
	if (bdtAckBuf[(pos + ACK_LEN - 1) % ACK_LEN] != (u1_t) ~ num)
		return 0;
	ACC(accACKSuccesses++);
	return 1;
}

static void bdtSendBuffer(bdtBuffer * buf)
{
	s2_t len;
	int stop;

	if (!buf->hasCRC) {
		buf->data[0] = buf->tag;
		buf->data[1] = bdtSeqNum++;
		len = buf->used;
		if (!buf->isPlain) {
			int nlen;

			if (lzo1x_1_compress(buf->raw, buf->used, buf->data + PACKAGE_HEAD_OVERHEAD, &nlen, wrkmem) != LZO_E_OK)
				sys_panic("lzo1x_1_compress() failed");
			len = nlen;
			*(s2_t *) & (buf->data[2]) = -len;
		} else {
			*(s2_t *) & (buf->data[2]) = len;
		}
		*(crc32_t *) & (buf->data[PACKAGE_HEAD_OVERHEAD + len]) =
		    crc32end(crc32(crc32start(), buf->data, PACKAGE_HEAD_OVERHEAD + len));
		ACC(accNPackages++);
	}





	stop = 0;

	do {
		u8_t t1, t2;

		bdtSendBytes(buf->data, len + PACKAGE_OVERHEAD);
		ACC(accNSent++);
		t1 = getMilliSeconds();
		do {
			if ((stop = bdtAck(buf->data[1])))
				break;
			t2 = getMilliSeconds();
		} while ((t2 - t1) <= ACK_TIMEOUT);
		ACC(if ((t2 - t1) > ACK_TIMEOUT) accACKTimeouts++);
	} while (!stop);

	buf->used = 0;
	buf->hasCRC = 0;
	accPrint();

}

static void bdtFlushBuffer(bdtBuffer * buf)
{
	if (buf->used != 0)
		bdtSendBuffer(buf);
}

static void bdtAddToBuffer(bdtBuffer * buf, const u1_t * src, u4_t len, u4_t max)
{

	u1_t *dest = buf->data + PACKAGE_HEAD_OVERHEAD;
	if (!buf->isPlain)
		dest = buf->raw;

	while (len) {

		u4_t l = max - buf->used;
		if (len < l)
			l = len;

		memcpy(dest + buf->used, src, l);

		src += l;
		buf->used += l;
		len -= l;

		if (buf->used == max)
			bdtSendBuffer(buf);
	}

}

/* public part */

/* start transmission */
int bdtStart()
{
	const char *msg = "startBDT";

	if (!bdtOK)
		return 1;

	if (!bdtInitialized)
		bdtInit();
	bdtSeqNum = 0;

	accPrint();
	bdtBufferInit(&bdtOutBuf, 'S', PLAIN);
	bdtAddToBuffer(&bdtOutBuf, msg, strlen(msg), MAX_LEN);
	bdtFlushBuffer(&bdtOutBuf);
	return 0;
}

/* end transmission */
int bdtStop()
{
	const char *msg = "endBDT";

	if (!bdtOK)
		return 1;


	bdtFlushBuffer(&bdtOutBuf);

	bdtBufferInit(&bdtOutBuf, 'S', PLAIN);
	bdtAddToBuffer(&bdtOutBuf, msg, strlen(msg), MAX_LEN);
	bdtFlushBuffer(&bdtOutBuf);
	accPrint();
	return 0;
}

/* start a chunk */
int bdtStartChunk(const char *id, u4_t size, u4_t nelem, char sendCompressed)
{
	if (!bdtOK)
		return -1;

	chunk.id = id;
	chunk.id_len = strlen(id);
	chunk.size = size;
	chunk.nelem = nelem;
	chunk.sizeInBytes = size * nelem;
	chunk.compress = sendCompressed;
	chunk.sentBytes = 0;

	bdtFlushBuffer(&bdtOutBuf);
	bdtBufferInit(&bdtOutBuf, 'S', PLAIN);
	bdtAddToBuffer(&bdtOutBuf, (u1_t *) & chunk.id_len, sizeof(chunk.id_len), MAX_LEN);
	bdtAddToBuffer(&bdtOutBuf, (u1_t *) & chunk.size, sizeof(chunk.size), MAX_LEN);
	bdtAddToBuffer(&bdtOutBuf, (u1_t *) & chunk.nelem, sizeof(chunk.nelem), MAX_LEN);
	bdtAddToBuffer(&bdtOutBuf, (u1_t *) chunk.id, chunk.id_len, MAX_LEN);

	if (sendCompressed) {
		bdtFlushBuffer(&bdtOutBuf);
		bdtBufferInit(&bdtOutBuf, 'S', COMPRESSED);
	}

	return 0;
}

/* send data */
int bdtSend(const char *data, u4_t size, u4_t nelem)
{
	if (!bdtOK)
		return -1;

	bdtAddToBuffer(&bdtOutBuf, data, size * nelem, 1111 /*MAX_LEN */ );
	chunk.sentBytes += size * nelem;
	return 0;
}

#endif				/* BINARY_DATA_TRANSMISSION */
