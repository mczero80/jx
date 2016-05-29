#include "all.h"

/*
 *
 * MAIN
 *
 */
void sigsegv_handler();
void start_domain_zero();



ObjectDesc *allocObject(ClassDesc * c);
ArrayDesc *allocArray(ClassDesc * elemClass, jint size);

void traceme()
{
	unsigned long int eflags;
	asm volatile ("pushfl;" "popl %0":"=r" (eflags));

	eflags |= 0x00000100;

	asm volatile ("pushl %0;" "popfl"::"r" (eflags));
}

void untraceme()
{
	unsigned long int eflags;
	asm volatile ("pushfl;" "popl %0":"=r" (eflags));

	eflags &= 0xfffffeff;

	asm volatile ("pushl %0;" "popfl"::"r" (eflags));
}

SharedLibDesc *zeroLib;

static void dummy_entry_point()
{
	sys_panic("dummy_entry_point SHOULD NOT BE CALLED");
}

//Proxy *domainZeroProxy;

/* This is the entry point for the initial JX managed thread */

char *zipstart;
jint ziplen;

#ifdef MICROBENCHMARKS
char *benchmem = NULL;
#endif				/* MICROBENCHMARKS */

#ifdef USE_EKHZ
CPUFrequency cpuFrequency;
#endif				/* USE_EKHZ */

#ifdef EVENT_LOG
void events_init()
{
	events = jxmalloc(sizeof(EventLog) * MAX_EVENTS);
}
#endif

int main(int argc, char *argv[])
{
	ThreadDesc *domainZero_thread;

#ifdef KERNEL
	struct multiboot_module *module;
#endif				/* KERNEL */

#ifdef USE_EKHZ
	getCPUFrequency(&cpuFrequency);
#endif				/* USE_EKHZ */

#ifndef KERNEL
	{
		sigset_t set, oldset;
		sigemptyset(&set);
		sigaddset(&set, SIGALRM);
		sigprocmask(SIG_BLOCK, &set, &oldset);
		printf("XXset sigmask %p - > %p\n", oldset, set);
	}
#endif

#ifndef KERNEL
	install_handler(SIGSEGV, sigsegv_handler);
	install_handler(SIGILL, sigsegv_handler);
	install_handler(SIGFPE, sigsegv_handler);

	jxmalloc_init();

#else				/* KERNEL */
	/* read zip from boot module */
	/*module = base_multiboot_find(ZIPFILE); */
	module = multiboot_get_module();

	if (module == NULL) {
		sys_panic("Could not find boot module");
	}

	zip_init(module->mod_start, module->mod_end - module->mod_start);

#endif				/* KERNEL */

#ifdef KERNEL
	pic_init_pmode();
	init_irq_data();
	/*
	 * Serial line
	 */
	ser_enable_break();


	dprintf("finished system init\n");

#   ifdef LOG_PRINTF
	init_log_space();
	printf2mem = 1;
#   endif
#else
	init_irq_data();
#endif				/* KERNEL */

#ifdef EVENT_LOG
	events_init();
#endif

	init_domainsys();

#ifdef KERNEL
#ifdef FRAMEBUFFER_EMULATION
	init_realmode();
#endif
#endif
	/*
	 * Init preemption-aware atomic regions
	 */

#ifdef NOPREEMPT
	nopreempt_init();
#endif
	atomicfn_init();

	threads_init();

	portals_init();


	//irq_disable(); /* don't need to disable interrupts, because there are none - timer not yet initialized */ 

#ifdef PROFILE
	profile_init();
#endif
	java_lang_Object = createObjectClassDesc();
	java_lang_Object_class = createObjectClass(java_lang_Object);

	createArrayObjectVTableProto(domainZero);
	//  class_Array = createArrayObjectClassDesc(domainZero);
	//class_Array_class = createArrayObjectClass(domainZero, class_Array);
	/* init system */
	set_current(createThread(domainZero, dummy_entry_point /* dummy */ , (void *) -1, STATE_RUNNABLE, SCHED_CREATETHREAD_NORUNQ));	/* dummy thread */
#ifdef DEBUG
	check_current = 0;
#endif

#ifdef MICROBENCHMARKS
#define BMPRINT(txt) t1 = ((u8_t)bt.a) << 32 | (u8_t)bt.b;  t2 = ((u8_t)bt.c) << 32 | (u8_t)bt.d; t3 = t2 - t1; a = t3 >> 32; b = t3 & 0xffffffff; printf("%s 0x%lx%lx\n", txt, a, b);
#define FLUSHCACHE {int i; for(i=0; i<totalmem; i++) {benchmem[i] = 1;}}
	{
		struct benchtime_s bt;
		u8_t t1, t2, t3;
		u4_t a, b;
		u4_t totalmem = 1024 * 1024;
		benchmem = jxmalloc(totalmem MEMTYPE_PROFILING);
		bench_empty(&bt);
		bench_empty(&bt);
		BMPRINT("empty");
		bench_store(&bt);
		bench_store(&bt);
		BMPRINT("store/hot");
		bench_store1(&bt);
		bench_store1(&bt);
		BMPRINT("store1/hot");
		FLUSHCACHE;
		bench_store1(&bt);
		BMPRINT("store1/cold");
		bench_load1(&bt);
		bench_load1(&bt);
		BMPRINT("load1/hot");
		FLUSHCACHE;
		bench_load1(&bt);
		BMPRINT("load1/cold");
		sys_panic("END OF BENCHMARK");
	}
#endif
	initPrimitiveClasses();

	domainZero_thread = createThread(domainZero, start_domain_zero, (void *) 0, STATE_RUNNABLE, SCHED_CREATETHREAD_DEFAULT);
	setThreadName(domainZero_thread, "DomainZero:InitialThread", NULL);
	thread_exit();

	/* not reached */
	return 0;		/* to satisfy compiler */
}
