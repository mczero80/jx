/********************************************************************************
 * Thread management (code only needed in emulation mode)
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#ifndef KERNEL

#include "all.h"


int no_signal_please = 0;

typedef void (*signal_handler_f) (int);


void sigsegv_handler(int signo, struct sigcontext sc)
{
	save(&sc);
	printf("Fault at 0x%lx\n", (jint) sc.eip);
	print_eip_info((char *) sc.eip);
	printf("\n");
	printStackTrace("FAULT", curthr(), sc.ebp);
	monitor(NULL);
}

#ifdef TIMESLICING_TIMER_IRQ
void sim_timer_irq();

extern u4_t timerticks;

void signal_handler(int signum, struct sigcontext sc)
{
	ThreadDesc *next;
	int ret;

	if (no_signal_please)
		return;

	//if (emulation_wont_interrupted(sc.eip, sc.ebp)) return;

	//    irq_store_disable();

#ifdef PROFILE
	profile_stop(curthr());
#endif

	//dprintf("IRQ eip=0x%08lx\n", sc.eip);

	save(&sc);
	//printf("*%d\n", timerticks);

	irq_handler_new();

	sys_panic("should not be reached");
}
#else
#ifndef SAMPLING_TIMER_IRQ
void signal_handler(int signum, struct sigcontext sc)
{
}
#endif
#endif				/* TIMESLICING_TIMER_IRQ */

#ifdef SAMPLING_TIMER_IRQ
extern int do_sampling;
extern jint *eip_samples;
extern jint n_eip_samples;
void signal_handler(int signum, struct sigcontext sc)
{
	if (!do_sampling)
		return;
	if (n_eip_samples < MAX_EIP_SAMPLES) {
		/*printf("eip:%p\n",sc.eip); */
		eip_samples[n_eip_samples++] = sc.eip;
	}
#ifdef PROFILE_SAMPLE_PMC0
	if (n_pmc0_samples < MAX_PMC0_SAMPLES)
		pmc_read(0, pmc0_samples[n_pmc0_samples++]);
#endif

#ifdef PROFILE_SAMPLE_PMC1
	if (n_pmc1_samples < MAX_PMC1_SAMPLES)
		pmc_read(1, pmc1_samples[n_pmc1_samples++]);
#endif

}
#endif

void sigint_handler(int signum, struct sigcontext sc)
{
	curthr()->context[PCB_EIP] = sc.eip;
	curthr()->context[PCB_ESP] = sc.esp;
	curthr()->context[PCB_EBP] = sc.ebp;

	printTraceFromStoredCtx("          ", curthr(), curthr()->context);
	print_threadinfo(curthr());

	monitor(NULL);
	/*  unblock_sigalrm(); */
}
void sigtrap_handler(int signum, struct sigcontext sc)
{
	printf("  SINGLESTEP eip=%p ", (u4_t *) sc.eip);
	print_eip_info((char *) sc.eip);
	printf("\n");
	printStackTrace("EX1", curthr(), (u4_t *) (sc.ebp));
	sc.eflags &= 0xfffffeff;
	monitor(&sc);
	/*  unblock_sigalrm(); */
}

#ifndef KERNEL
void signal_handler();

void install_alrmhandler(int sig, void (*handler) (int))
{
	struct sigaction act;

	memset(&act, 0, sizeof(act));
	act.sa_handler = handler;
	//#ifdef SAMPLING_TIMER_IRQ

	//  act.sa_flags = SA_NODEFER;

	//#endif /* SAMPLING_TIMER_IRQ */
	if (sigaction(sig, &act, NULL) != 0) {
		perror("Error installing signal handler");
		exit(1);
	}
}

void install_inthandler(int sig, void (*handler) (int))
{
	struct sigaction act;

	memset(&act, 0, sizeof(act));
	act.sa_handler = handler;
	sigemptyset(&act.sa_mask);
	sigaddset(&act.sa_mask, SIGALRM);
	if (sigaction(sig, &act, NULL) != 0) {
		perror("Error installing signal handler");
		exit(1);
	}
}
#endif


#ifndef KERNEL

/* needed by switch */
inline void save_sigmask_to(ThreadDesc * thread)
{
	sigset_t set, oldset;
	sigemptyset(&set);
	sigprocmask(SIG_BLOCK, &set, &(thread->sigmask));
	thread->preempted = 0;
	//printf("save sigmask %p=%p\n", thread, thread->sigmask);
	if (thread == 0x00000000)
		sys_panic("");
}

inline void restore_sigmask_from(ThreadDesc * thread)
{
	//printf("set sigmask %p <- %p\n", thread, thread->sigmask);
	restore_irq(&(thread->sigmask));
}

void get_currentthread_sigmask()
{
	save_sigmask_to(curthr());
}

void set_currentthread_sigmask()
{
	checkStackTraceNew();
	restore_sigmask_from(curthr());
}

void restore_irq(const sigset_t * set)
{
	no_signal_please = 1;
	sigprocmask(SIG_SETMASK, set, NULL);
	no_signal_please = 0;
}

void disable_irq(sigset_t * set, sigset_t * oldset)
{
	sigemptyset(set);
	sigaddset(set, SIGALRM);
#ifdef DISABLE_SIGINT
	sigaddset(set, SIGINT);
#endif
	sigprocmask(SIG_BLOCK, set, oldset);

	ASSERTCLI;
}

void enable_irq()
{
	sigset_t set, oldset;
#ifdef VERBOSE_IRQ
	printf("\nIRQ ENABLE in domain %p (%s) [Thread: %p (%s) caller=", curdom(), curdom()->domainName, curthr(),
	       curthr()->name);
	{
		code_t ip = getCallerIP();
		print_eip_info(ip);
	}
	printf("]\n");
#endif
	sigemptyset(&set);
	sigaddset(&set, SIGALRM);
#ifdef DISABLE_SIGINT
	sigaddset(&set, SIGINT);
#endif
	sigprocmask(SIG_UNBLOCK, &set, &oldset);
}

sigset_t irq_store_disable()
{
	sigset_t set, oldset;
	sigemptyset(&set);
	sigaddset(&set, SIGALRM);
#ifdef DISABLE_SIGINT
	sigaddset(&set, SIGINT);
#endif
	sigprocmask(SIG_BLOCK, &set, &oldset);
#ifdef VERBOSE_IRQ
	printf("\nIRQ STOREDISABLE(%s) in domain %p (%s) [Thread: %p (%s) caller=",
	       sigismember(&oldset, SIGALRM) ? "was disabled" : "was enabled", curdom(), curdom()->domainName, curthr(),
	       curthr()->name);
	{
		code_t ip = getCallerIP();
		print_eip_info(ip);
	}
	printf("]\n");
#endif
	return oldset;
}

#endif


void threads_emulation_init()
{
	printf("init threads system - emulation part\n");

#ifndef NO_TIMER_IRQ
	install_alrmhandler(SIGALRM, (signal_handler_f) signal_handler);
#endif

	install_inthandler(SIGINT, (signal_handler_f) sigint_handler);
	install_inthandler(SIGTRAP, (signal_handler_f) sigtrap_handler);
}

#endif /*KERNEL*/
