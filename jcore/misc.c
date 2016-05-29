#include "all.h"

#define MAXBUF (sizeof(long int) * 8)	/* enough for binary */

code_t extern_panic = (code_t) sys_panic;
code_t extern_printf = (code_t) printf;
code_t getCaller(u4_t nup);
int_code_t extern_getCaller = (int_code_t) getCaller;
DomainDesc *(*extern_dom) () = curdom;


void dtostr(char *cbuf, int maxbuf, u4_t u, int base)
{
	char buf[MAXBUF];	/* build number here */
	char *p = &buf[MAXBUF - 1];
	static char digs[] = "0123456789abcdef";

	do {
		*p-- = digs[u % base];
		u /= base;
	} while (u != 0);

	while (++p != &buf[MAXBUF])
		*cbuf++ = *p;
	*cbuf = 0;
}

#ifdef KERNEL
  /* write to serial */
int dprintf(const char *fmt, ...)
{
	va_list args;
	int err;

	DISABLE_IRQ;

	va_start(args, fmt);
	vprintf(fmt, args);
	va_end(args);

	RESTORE_IRQ;
}

#else
#endif				/*KERNEL */


static int in_panic = 0;
void sys_panic(char *msg, ...)
{
	jint *base;
	va_list args;
	CLI;
#ifdef DEBUG
	check_current = 0;
#endif
#ifdef LOG_PRINTF
	printf2mem = 0;
#endif
	if (in_panic) {
		monitor(0);
		exit(1);
	}
	in_panic = 1;
	printf("PANIC\n");
#ifdef SMP
	printf("CPU%d ", get_processor_id());
#endif
	printf("PANIC:\n");
	va_start(args, msg);
	vprintf(msg, args);
	va_end(args);
	printf("\n");

	clean_domainsys();	/* in case foreachDomain was terminated by panic */

#ifndef KERNEL
	base = (u4_t *) & msg - 2;
	printStackTrace("PANIC ", curthr(), base);
	//asm("int $3");
	monitor(NULL);
#else
	base = (u4_t *) & msg - 2;

	printStackTrace("PANIC ", curthr(), base);


	monitor(NULL);
#endif
	/*exceptionHandler(THROW_RuntimeException); */
	exit(1);
}


#define MAXBUF (sizeof(long int) * 8)	/* enough for binary */

void sprintnum(char *s, u4_t u, int base)
{
	char buf[MAXBUF];	/* build number here */
	register char *p = &buf[MAXBUF - 1];
	static char digs[] = "0123456789abcdef";

	do {
		*p-- = digs[u % base];
		u /= base;
	} while (u != 0);

	while (++p != &buf[MAXBUF])
		*s++ = *p;
	*s = '\0';
}

/* get caller address nup stack frames up */
code_t getCaller(u4_t nup)
{
	u4_t i;
	u4_t *eip, *ebp, *sp;
	sp = &nup - 2;
	for (i = 0; i < nup + 1; i++) {
		if (sp == NULL)
			return NULL;
		ebp = (u4_t *) * sp++;
		eip = (u4_t *) * sp++;
		//print_eip_info(eip);
		//printf("\n");
		sp = ebp;
	}
	return (code_t) eip;
}


void setTimer()
{
#ifndef KERNEL
	struct itimerval value;
	int timeslice_microsec = 1000000 / TIMER_HZ;
	value.it_interval.tv_sec = 0;
	value.it_interval.tv_usec = timeslice_microsec;
	value.it_value.tv_sec = value.it_interval.tv_sec;
	value.it_value.tv_usec = value.it_interval.tv_usec;
#ifndef NO_TIMER_IRQ
	setitimer(ITIMER_REAL, &value, NULL);
#  endif

#else				/* KERNEL */
	/*
	 * Setup Timer interrupt
	 * Needed for timeslicing and to poll serial line
	 * even if no timeslicing is deactivated the timer isr is used to poll the serial line 
	 */

#       define HZ            1193182
	int value = HZ / TIMER_HZ;
	printf("SETTING TIMER\n\n");
	outb(0x43, 0x30 | 0x04);	/* set mode: 16bit and mode rategen */
	outb(0x40, value & 0xff);	/* set first counter */
	outb(0x40, (value >> 8) & 0xff);	/* set first counter */
#endif
}
