#include "all.h"
/*
 * ClockDEP
 */
jint clock_getTimeInMillis(ObjectDesc * self)
{
#ifndef KERNEL
	jlong t;
	struct timeval tp;
	gettimeofday(&tp, NULL);
	t = tp.tv_sec;
	t *= 1000;
	t += (tp.tv_usec / 1000);
	return (jint) t;
#else
	unsigned long long ret;
	unsigned long r;
	asm volatile ("rdtsc":"=A" (ret):);
	return CYCL2MILLIS(ret);
#endif
#if 0
#ifndef KERNEL
	struct timeval tp;
	/*  debugz(("CLOCK GETTIME %lx\n",(jint)self); */
	gettimeofday(&tp, NULL);
	return tp.tv_sec;
#else
	return 0;
#endif
#endif
}

jlong clock_getTicks(ObjectDesc * self)
{
	unsigned long long ret;
	asm volatile ("rdtsc":"=A" (ret):);
	return ret;
}

jint clock_getTicks_low(ObjectDesc * self)
{
	unsigned long long ret;
	asm volatile ("rdtsc":"=A" (ret):);
	return (jint) (ret & 0x000000007fffffff);
}

jint clock_getTicks_high(ObjectDesc * self)
{
	unsigned long long ret;
	asm volatile ("rdtsc":"=A" (ret):);
	return (jint) (ret >> 32);
}

jint clock_getCycles(ObjectDesc * self, ObjectDesc * cycleTime)
{
	unsigned long low, high;
	asm volatile ("rdtsc":"=d" (high), "=a"(low));
	CHECK_NULL_PTR(cycleTime);
	cycleTime->data[0] = low;
	cycleTime->data[1] = high;
}

jint clock_subtract(ObjectDesc * self, ObjectDesc * res, ObjectDesc * a, ObjectDesc * b)
{
	CHECK_NULL_POINTER(a == NULL || b == NULL || res == NULL);
	{
		u8_t ca = *((u8_t *) a->data);
		u8_t cb = *((u8_t *) b->data);
		u8_t cr = ca - cb;
		if (ca < cb)
			sys_panic("");
		*((u8_t *) res->data) = cr;
	}
}

jint clock_toMicroSec(ObjectDesc * self, ObjectDesc * a)
{
	jint ret;
	CHECK_NULL_PTR(a);
	DISABLE_IRQ;		/* clock is fast portal and performas floating point operations */
	{
		u8_t ca = *((u8_t *) a->data);
		//  printf("*%ld,%ld,%ld,%ld*", a->data[0], a->data[1],(u4_t)(ca / 500), (u4_t)(ca >> 9));
		ret = CYCL2MICROS(ca);
	}
	RESTORE_IRQ;
	return ret;
}

jint clock_toNanoSec(ObjectDesc * self, ObjectDesc * a)
{
	u8_t ret;
	CHECK_NULL_PTR(a);
	DISABLE_IRQ;		/* clock is fast portal and performas floating point operations */
	{
		u8_t ca = *((u8_t *) a->data);
		ret = CYCL2NANOS(ca);
	}
	RESTORE_IRQ;
	return ret;
}

jint clock_toMilliSec(ObjectDesc * self, ObjectDesc * a)
{
	jint ret;
	CHECK_NULL_PTR(a);
	DISABLE_IRQ;		/* clock is fast portal and performas floating point operations */
	{
		u8_t ca = *((u8_t *) a->data);
		ret = CYCL2MILLIS(ca);
	}
	RESTORE_IRQ;
	return ret;
}

MethodInfoDesc clockMethods[] = {
	{"getTimeInMillis", "", clock_getTimeInMillis}
	,
	{"getTicks", "", clock_getTicks}
	,
	{"getTicks_low", "", clock_getTicks_low}
	,
	{"getTicks_high", "", clock_getTicks_high}
	,
	{"getCycles", "", clock_getCycles}
	,
	{"subtract", "", clock_subtract}
	,
	{"toMicroSec", "", clock_toMicroSec}
	,
	{"toNanoSec", "", clock_toNanoSec}
	,
	{"toMilliSec", "", clock_toMilliSec}
	,
};

void init_clock_portal()
{
	init_zero_dep_without_thread("jx/zero/Clock", "Clock", clockMethods, sizeof(clockMethods), "<jx/zero/Clock>");
}
