#include "all.h"

#ifdef USE_EKHZ
#define EKHZ_HZ 100
#define EKHZ_CLOCK_TICK_RATE 1193180
#define EKHZ_LATCH  ((EKHZ_CLOCK_TICK_RATE + EKHZ_HZ/2) / EKHZ_HZ)
#define EKHZ_CALIBRATE_LATCH (5 * EKHZ_LATCH)
#define EKHZ_CALIBRATE_TIME  (5 * 1000020/EKHZ_HZ)

#define EKHZ_BITS 32
#define EKHZ_Q50 ((1LL << EKHZ_BITS) / 50000)
#define EKHZ_Q66 ((3LL << EKHZ_BITS) / 200000)
#define EKHZ_O50 ((200LL << EKHZ_BITS) / 3)
#define EKHZ_POINT5 (1LL << (EKHZ_BITS-1))

#define EKHZ_READ_BUF 1024

#ifdef KERNEL
static unsigned long ekhz_calibrate_tsc(void)
{
	/* Set the Gate high, disable speaker */
	outb((inb(0x61) & ~0x02) | 0x01, 0x61);

	/*
	 * Now let's take care of CTC channel 2
	 *
	 * Set the Gate high, program CTC channel 2 for mode 0,
	 * (interrupt on terminal count mode), binary count,
	 * load 5 * LATCH count, (LSB and MSB) to begin countdown.
	 */
	outb(0xb0, 0x43);	/* binary, mode 0, LSB/MSB, Ch 2
				 */
	outb(EKHZ_CALIBRATE_LATCH & 0xff, 0x42);	/* LSB of count */
	outb(EKHZ_CALIBRATE_LATCH >> 8, 0x42);	/* MSB of count */

	{
		unsigned long startlow, starthigh;
		unsigned long endlow, endhigh;
		unsigned long count;

		u8_t tmp = get_tsc();
		startlow = tmp;
		starthigh = (tmp >> 32);

		count = 0;
		do {
			count++;
		} while ((inb(0x61) & 0x20) == 0);

		tmp = get_tsc();
		endlow = tmp;
		endhigh = (tmp >> 32);

		/* Error: ECTCNEVERSET */
		if (count <= 1)
			goto bad_ctc;

		/* 64-bit subtract - gcc just messes up with long longs */
	      __asm__("subl %2,%0\n\t" "sbbl %3,%1":"=a"(endlow), "=d"(endhigh)
	      :	"g"(startlow), "g"(starthigh), "0"(endlow), "1"(endhigh));

		/* Error: ECPUTOOFAST */
		if (endhigh)
			goto bad_ctc;

		/* Error: ECPUTOOSLOW */
		if (endlow <= EKHZ_CALIBRATE_TIME)
			goto bad_ctc;

	      __asm__("divl %2":"=a"(endlow), "=d"(endhigh)
	      :	"r"(endlow), "0"(0), "1"(EKHZ_CALIBRATE_TIME));

		return endlow;
	}

	/*
	 * The CTC wasn't reliable: we got a hit on the very first read,
	 * or the CPU was so fast/slow that the quotient wouldn't fit in
	 * 32 bits..
	 */
      bad_ctc:
	return 0;
}

static u4_t ekhz_estimate_kHZ()
{
	unsigned long tsc_quotient = ekhz_calibrate_tsc();
	u4_t cpu_khz;

	// FIXME
	return CPU_MHZ * 1000;
	/*
	   if (tsc_quotient) {
	   unsigned long eax=0, edx=1000;
	   __asm__("divl %2"
	   :"=a" (cpu_khz), "=d" (edx)
	   :"r" (tsc_quotient),
	   "0" (eax), "1" (edx));
	   return cpu_khz;
	   } 
	   else return 0;
	 */
}
#else				/* KERNEL */
static u4_t ekhz_estimate_kHZ()
{
	FILE *fp;
	char buf[EKHZ_READ_BUF];

	fp = fopen("/proc/cpuinfo", "r");
	if (fp == NULL)
		return 0;

	while (1) {
		if (fgets(buf, EKHZ_READ_BUF, fp) == NULL)
			break;
		if (strncmp(buf, "cpu MHz\t\t:", 10) != 0)
			continue;
		return atof(buf + 10) * 1000;
	}
	fclose(fp);
	return 0;
}
#endif				/* KERNEL */

static u4_t ekhz_round_MHZ(u4_t khz)
{
	u8_t m50 = ((EKHZ_Q50 * khz + EKHZ_POINT5) >> EKHZ_BITS) * 50;
	u8_t m66 = (((EKHZ_Q66 * khz + EKHZ_POINT5) >> EKHZ_BITS) * EKHZ_O50 + EKHZ_POINT5) >> EKHZ_BITS;

	return abs(m50 - khz) < abs(m66 - khz) ? m50 : m66;
}

void getCPUFrequency(CPUFrequency * freq)
{
	freq->khz = ekhz_estimate_kHZ();
	freq->mhz = ekhz_round_MHZ(freq->khz);
	printf("CPU frequency: %ld kHz, rounded MHz: %ld\n", freq->khz, freq->mhz);
}

#endif				/* USE_EKHZ */
