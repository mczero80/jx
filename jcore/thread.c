/********************************************************************************
 * Thread management
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#include "all.h"
#include "runq.h"
#include "sched.h"

#include "scheduler_inlined.h"

#include "threadimpl.h"

void thread_profile_irq(u4_t eip);

#ifdef KERNEL
void irq_destroy_switch_to(ThreadDesc ** current, ThreadDesc * to);
#endif

void idle(void *x);

#ifdef JAVASCHEDULER
void idle(void *x)
{
#  ifdef DEBUG
	check_current = 1;	/* may be deactivated */
#  endif
	for (;;) {
		/* printf("idle yield\n"); */
#  ifdef NO_TIMER_IRQ
		threadyield();
#  endif
	}
}
#endif


static void destroyCurrentThread();

void thread_exit()
{
#ifdef DBG_THREAD
	printf("THREADEXIT %d.%d\n", curdom()->id, curthr()->id);
#endif
	destroyCurrentThread();
	printf("Looping forever. Should never be reached.\n");
	for (;;);
}

void check_eflags0(ThreadDesc * t, u4_t flags)
{
	if ((flags & 0x200) == 0) {
		printf("IF cleared FLAGS0: %p = %08lx\n", curthr(), flags);
		printStackTrace("EFLAGS: ", curthr(), (u4_t *) & t - 2);
	}
}
void check_eflags1(ThreadDesc * t, u4_t flags)
{
	if ((flags & 0x200) == 0) {
		printf("IF cleared FLAGS1: %p = %08lx\n", curthr(), flags);
		printStackTrace("EFLAGS: ", curthr(), (u4_t *) & t - 2);
	}
}

void pcb_init(ThreadDesc * thread)
{
	thread->context[PCB_GS] = KERNEL_DS;
	thread->context[PCB_FS] = KERNEL_DS;
	thread->context[PCB_ES] = KERNEL_DS;	/* used by C */
	thread->context[PCB_DS] = KERNEL_DS;	/* data segment, dont touch */
	thread->context[PCB_EDI] = 0;
	thread->context[PCB_ESI] = 0;
	thread->context[PCB_EBP] = 0;
	thread->context[PCB_ESP] = 0;
	thread->context[PCB_EBX] = 0;
	thread->context[PCB_EDX] = 0;
	thread->context[PCB_ECX] = 0;
	thread->context[PCB_EAX] = 0;
	thread->context[PCB_EIP] = 0;
	thread->context[PCB_EFLAGS] = 0x00000212;
}

ThreadDesc *createThreadInMem(DomainDesc * domain, thread_start_t thread_start, void *param, ObjectDesc * e, u4_t stackSize,
			      int state, int schedParam);
ThreadDesc *specialAllocThreadDesc(DomainDesc * domain);

void start_initial_thread(void *dummy);

ThreadDesc *createInitialDomainThread(DomainDesc * domain, int state, int schedParam)
{
	return createThreadInMem(domain, start_initial_thread, NULL, NULL, 1024, state, schedParam);
}


ThreadDesc *createThread(DomainDesc * domain, thread_start_t thread_start, void *param, int state, int schedParam)
{
	ThreadDesc *t = createThreadInMem(domain, thread_start, param, NULL, 1024, state, schedParam);
	return t;
}

//#define STACK_ALIGN     STACK_CHUNK_SIZE
#define STACK_ALIGN 4

extern ClassDesc *cpuStateClass;
extern ClassDesc *stackClass;

#if ! defined(STACK_ON_HEAP) && defined(STACK_ALLOW_GROW)
void thread_inc_current_stack(u4_t inc)
{
	int size, nsize;
	u4_t *new_stack, *new_top, *fp, *p, *nfp, *ebp, *old_stack;
	ThreadDesc *thread;

	thread = curthr();

	size = (thread->stackTop - thread->stack) << 2;

	new_stack = malloc_threadstack(curdom(), size + inc, STACK_ALIGN);
	new_top = new_stack + ((size + inc) >> 2);

	ebp = (u4_t *) & inc - 2;
	fp = thread->stack;
	nfp = new_top - (thread->stackTop - fp);
	p = new_top - (thread->stackTop - ebp);

	ASSERT(((thread->stack <= fp) && (fp < thread->stackTop)));

	old_stack = thread->stack;

	while (fp < thread->stackTop) {
		if (fp == ebp) {
			ebp = (u4_t *) * fp;
			if (ebp != NULL) {
				ASSERT(((thread->stack < ebp) && (ebp < thread->stackTop)));
				*nfp = new_top - (thread->stackTop - ebp);
			} else {
				*nfp = NULL;
			}
		} else {
			*nfp = *fp;
		}
		fp++;
		nfp++;
	}

	thread->stackTop = new_top;
	thread->stack = new_stack;

/*
0x804ed71 <thread_inc_current_stack+625>:       mov    0xffffffec(%ebp),%ecx
0x804ed74 <thread_inc_current_stack+628>:       mov    %ecx,%ebp
0x804ed76 <thread_inc_current_stack+630>:       mov    %ebp,%esp
*/
	asm volatile ("movl %0, %%ebp;"::"r" (p));	/* switch to new stack */
	asm volatile ("movl %0, %%esp;"::"r" (p));
	printf("REALLOC STACK IN THREAD %d.%d\n", thread->domain->id, thread->id);
	free_threadstack(thread->domain, old_stack, size);

	return;
}
#else
void thread_inc_current_stack(u4_t inc)
{
	sys_panic("");
}

#endif

/* stackSize in words */
ThreadDesc *createThreadInMem(DomainDesc * domain, thread_start_t thread_start, void *param, ObjectDesc * entry, u4_t stackSize,
			      int state, int schedParam)
{
	u4_t *sp1;
	ThreadDesc *thread;
	ObjectDesc *obj;
	ThreadDescProxy *threadproxy;

	threadproxy = allocThreadDescProxyInDomain(domain, cpuStateClass);
	thread = &(threadproxy->desc);
#ifdef STACK_ON_HEAP
#if 1
	thread->stackObj = allocStackInDomain(domain, stackClass, STACK_CHUNK_SIZE /*stackSize */ );
	thread->stackObj->thread = thread;
	thread->stack = obj2stack(thread->stackObj);
	thread->stackTop = thread->stack + STACK_CHUNK_SIZE;
#else
	thread->stack = malloc_threadstack(domain, STACK_CHUNK_SIZE, STACK_ALIGN);
	thread->stackTop = thread->stack + (STACK_CHUNK_SIZE >> 2);
#endif
#else
	thread->stack = malloc_threadstack(domain, STACK_CHUNK_SIZE, STACK_ALIGN);
	thread->stackTop = thread->stack + (STACK_CHUNK_SIZE >> 2);
#endif

	thread->entry = entry;

#ifdef USE_QMAGIC
	thread->magic = MAGIC_THREAD;
#endif
	thread->contextPtr = &(thread->context);
	thread->schedInfo = 0;
	thread->domain = domain;
#ifdef NEW_PORTALCALL
	thread->nextInReceiveQueue = NULL;
#endif
	thread->name[0] = '\0';
	thread->portalParameter = NULL;
	thread->myparams = NULL;
	domain->sched.currentThreadID++;
	thread->id = domain->sched.currentThreadID;

#ifdef JAVASCHEDULER
	if (__current[get_processor_id()] == NULL)
		thread->schedulingDomain = domainZero;
	else
		thread->schedulingDomain = curthr()->schedulingDomain;
#endif
#ifdef CONT_PORTAL
	thread->linkedDEPThr = thread;
#endif

#ifndef NEW_SCHED
	thread->nextInRunQueue = NULL;
#endif

#ifndef SMP
	thread->nextInDomain = domain->threads;
	if (domain->threads)
		domain->threads->prevInDomain = thread;
	thread->prevInDomain = NULL;
	domain->threads = thread;
#else
	UPDATE prevInDomain
	do {
		thread->nextInDomain = domain->threads;
	} while (!cas((u4_t *) & domain->threads, (u4_t) thread->nextInDomain, (u4_t) thread));

	/* set current CPU of the new Thread */
	thread->curCpuId = get_processor_id();
#endif

	/*dprintf("     THREAD-> 0x%lx,\n", thread);
	   dprintf("     THREADCTX-> at 0x%lx\n",  thread->contextPtr); */

//      printf("STACK : %p .. %p obj=%p\n", thread->stackTop, thread->stack, thread->stackObj);
	sp1 = thread->stackTop;
	*--sp1 = (u4_t) thread_exit;
	*--sp1 = (u4_t) param;
	*--sp1 = (u4_t) thread_exit;

	pcb_init(thread);
	thread->context[PCB_EIP] = (u4_t) thread_start;
	thread->context[PCB_ESP] = (u4_t) sp1;
	thread->context[PCB_EBP] = 0;
	thread->context[PCB_EFLAGS] = 0x00000212;
	thread->state = state;

#ifndef KERNEL
	sigemptyset(&(thread->sigmask));
#endif

#ifdef PROFILE
	/* alloc memory for profiling */
	thread->profile = profile_new_desc();
#endif

	Sched_created(thread, schedParam);

	return thread;
}

#ifdef STACK_ON_HEAP
void freeThreadMem(ThreadDesc * t)
{
}
#else
void freeThreadMem(ThreadDesc * t)
{
	u4_t *threadmem;
	u4_t *memBorder, *mem;
	DomainDesc *domain;

	ASSERTCLI;

	threadmem = ObjectDesc2ptr(ThreadDesc2ObjectDesc(t));
	domain = t->domain;
	if (t->myparams)
		jxfree(t->myparams, MYPARAMS_SIZE MEMTYPE_OTHER);
	//if (t->copied) jxfree(t->copied, sizeof(struct copied_s)*t->max_copied);

	mem = t->stack;

	free_threadstack(domain, mem, STACK_CHUNK_SIZE);
	//  printf("stack=%p, border=%p\n", mem, memBorder);

}
#endif

ThreadDesc *findThreadByID(DomainDesc * domain, u4_t id)
{
	ThreadDesc *t;
	ASSERTCLI;
	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		if (t->id == id)
			return t;
	}
	return NULL;
}

ThreadDesc *findThreadDesc(ThreadDescForeignProxy * proxy)
{
	DomainDesc *domain;
	ASSERTCLI;
	if (proxy->domain->domain->id == proxy->domain->domainID) {
		domain = proxy->domain->domain;
	} else {
		/* domain terminated -> no TCBs */
		return NULL;
	}

	if (proxy->gcEpoch != domain->gc.epoch) {
		ThreadDesc *t;
		printf("new GC epoch -> find TCB\n");
		for (t = domain->threads; t != NULL; t = t->nextInDomain) {
			if (t->id == proxy->threadID) {
				/* found thread -> update proxy and return TCB */
				proxy->thread = t;
				proxy->gcEpoch = domain->gc.epoch;
				printf("new GC epoch -> found %p\n", t);
				return t;
			}
		}
		/* thread not found (probably terminated) -> return NULL */
		return NULL;
	}
	return proxy->thread;	// TODO: CHECK DOMAIN AND GC EPOCH
}

#ifdef SMP
static spinlock_t lock = SPIN_LOCK_UNLOCKED;
#endif

static void destroyCurrentThread()
{
	ThreadDesc **t;
	DomainDesc *domain;

	DISABLE_IRQ;

	domain = curdom();

	/* should call terminateThread(curthr()) */

	curthr()->state = STATE_ZOMBIE;

	terminateThread_internal(curthr());

	/* check if there are any other threads or services or other connections from the outside into this domain 
	 * otherwise: domain is no longer useful and can be terminated (garbage collected) */
	/*if (! Sched_threadsExist(curdom())) {
	   domain->state = DOMAIN_STATE_ZOMBIE;
	   } */
#ifndef NEW_SCHED
	Sched_destroy_switch_to_nextThread(domain);
#else
	Sched_destroyed_current(domain);
#endif

	RESTORE_IRQ;		/* dummy */
	sys_panic("switched back to DEAD thread");
}

/* thread must not be in any queue !! caller of terminateThread must ensure this! */
void terminateThread(ThreadDesc * thread)
{
	DomainDesc *domain;

	DISABLE_IRQ;

#ifdef SMP
	spin_lock(&lock);
#endif

	terminateThread_internal(thread);


	RESTORE_IRQ;
}

/* used for bot current and other thread */
void terminateThread_internal(ThreadDesc * thread)
{
	ASSERTCLI;

	/* remove thread from domain thread list */
	if (thread->prevInDomain) {
		thread->prevInDomain->nextInDomain = thread->nextInDomain;
	} else {
		thread->domain->threads = thread->nextInDomain;
	}
	if (thread->nextInDomain) {
		thread->nextInDomain->prevInDomain = thread->prevInDomain;
	}
#ifdef SMP
	spin_unlock(&lock);
#endif
	Sched_destroyed(thread);

	freeThreadMem(thread);

}

void print_esp()
{
	int a;
	printf("ESP=%p\n", &a);
}

void setThreadName(ThreadDesc * thread, char *n1, char *n2)
{
	int l;
	ASSERTCLI;
	l = strlen(n1);
	strncpy(thread->name, n1, THREAD_NAME_MAXLEN);
	if (l >= THREAD_NAME_MAXLEN || n2 == NULL)
		return;
	strncpy(thread->name + l, n2, THREAD_NAME_MAXLEN - l);
}


/*******/
void threadyield()
{
	ThreadDesc *next;

	DISABLE_IRQ;

	ASSERT(curthr()->state == STATE_RUNNABLE);

#ifdef KERNEL
#ifdef CHECK_SERIAL_IN_YIELD
	{
		static int less_checks;
		if ((less_checks++ % 20) == 0)
			check_serial();
	}
#endif
#endif

	//printf ("YIELD %d.%d of Domain %s\n",TID(curthr()), curthr()->domain->domainName);
#ifndef NEW_SCHED
	Sched_yielded(curthr());
	Sched_switch_to_nextThread();
#else
	Sched_yield();
#endif
	//printf ("back from YIELD %d.%d (Domain %s)\n",TID(curthr()), curthr()->domain->domainName);

	RESTORE_IRQ;
}

/*  unblocks the  given Thread 
  ! be sure the IRQs are disabled  */
inline void threadunblock(ThreadDesc * t)
{
	ASSERTTHREAD(t);
	ASSERTCLI;
	t->state = STATE_RUNNABLE;
#ifndef NEW_SCHED
	Sched_unblocked(t);
#else
	Sched_unblock(t);
#endif
}

/* unblocks the  given Thread
   but deactivates the Interrupts at first */
void locked_threadunblock(ThreadDesc * t)
{
#ifdef KERNEL
	DISABLE_IRQ;
	threadunblock(t);
	RESTORE_IRQ;
#else
	threadunblock(t);
	//    sys_panic("unblock should not be called");
#endif
}

/* blocks the current Thread */
inline void threadblock()
{
	//ASSERTCLI;
	DISABLE_IRQ;

	SCHED_BLOCK_USER;


	RESTORE_IRQ;
}

/* 
   blocks the current Thread
   but deactivates the Interrupts at first
 */
void locked_threadblock()
{
#ifdef KERNEL
	DISABLE_IRQ;
	threadblock();
	RESTORE_IRQ;
#else
	sys_panic("block should not be called");
#endif
}

void ports_outb_p(ObjectDesc * self, jint port, jbyte value);
jbyte ports_inb_p(ObjectDesc * self, jint port);

static void rtc_irq_ack()
{
	ports_outb_p(NULL, 0x70, 0xc);
	ports_inb_p(NULL, 0x71);
}

void irq_happened(u4_t irq, DEPDesc * dep /*u4_t eip, u4_t cs, u4_t eflags */ )
{
	/* printf("*****IRQ****** %p %p %p\n", eip, cs, eflags); */
	/* printf("*****IRQ %d %p******\n", irq, dep); */
	/*rtc_irq_ack(); */
}
void irq_missed(u4_t irq, DEPDesc * dep)
{
	/* printf("*****IRQ****** %p %p %p\n", eip, cs, eflags); */
	printf("*****IRQMISSED %ld %p******\n", irq, dep);
	printStackTrace("IRQMISS: ", curthr(), (u4_t *) & irq - 2);

	monitor(NULL);
}

void irq_picnotok(jint mask, jint irq, jint andmask)
{
	printf("*****PIC NOT OK: mask=0x%08lx  andmask=0x%08lx irq=%ld******\n", mask, andmask, irq);
	monitor(NULL);
}

void irq_irrnotok(jint irq)
{
	printf("*****IRR NOT OK: irq=%ld******\n", irq);
	monitor(NULL);
}

#if 0
/* send message from lowlevel irq handler to handler thread */
ThreadDesc *irq_send_msg(DEPDesc * dep)
{
	ThreadDesc *target;
	ASSERTDEP(dep);
	/*printf("SEND IRQ MESSAGE\n"); */
#ifdef NO_DEP_LOCKING
	spinlock(&dep->lock);
#endif
#ifdef DBG_IRQ
	printf("irq_send_msg 0x%lx\n", dep);
#endif
	if ((target = dep->firstWaitingReceiver) != NULL) {
		dep->firstWaitingReceiver = dep->firstWaitingReceiver->nextInDEPQueue;
		if (dep->firstWaitingReceiver == NULL) {
			dep->lastWaitingReceiver = NULL;
		}
#ifdef DBG_IRQ
		printf("irq_sm: 0x%lx  ip=%p eflags=%p recv_msg=%p\n", target, target->context[PCB_EIP],
		       target->context[PCB_EFLAGS], recv_msg);
#endif

#ifdef PROFILE_EVENT_THREADSWITCH
		target->name = "IRQT";
		profile_event_threadswitch_to(target);
#endif

		return target;
	}
	sys_panic("delayed irq handling not yet supported\n");
	return NULL;
}
#endif


/* do not need thread */
void receiveDomainDEP(void *arg)
{
	thread_exit();
}


/*****/


int disable_threadswitching = 0;


/*
 * Reschedule in interrupt context 
 */
#ifdef JAVASCHEDULER
void reschedule()
{
//     sim_timer_irq();
	sys_panic("JAVASCHEDULER defined: reschedule not implemented");
}
#else
void reschedule()
{
	ThreadDesc *next;

	if (disable_threadswitching) {
		destroy_switch_to(curthrP(), curthr());
	} else {
#ifndef NEW_SCHED
		Sched_reschedule();	/* if this fkt returns: there was no other Thread */
#else
		Sched_preempted();	/* if this fkt returns: there was no other Thread */
#endif
		//dprintf("No next thread to schedule.\n");
		destroy_switch_to(curthrP(), curthr());
		sys_panic("reschedule1: should not be reached");
	}
}
#endif				/* JAVASCHEDULER */

void softint_handler()
{
	printf("SOFTINTHANDLER\n");
	reschedule();
}

void irq_handler_new()
{
#ifdef NOPREEMPT
#ifdef DEBUG
	if (nopreempt_check(curthr()->context[PCB_EIP])) {
		//      printf(" interrupted in %p\n", curthr()->context[PCB_EIP]);
	}
#endif
#endif
#ifdef ROLLFORWARD_ON_PREEMPTION
	if (nopreempt_check(curthr()->context[PCB_EIP])) {
		curthr()->context[PCB_EIP] = nopreempt_adjust(curthr()->context[PCB_EIP]);
		printf(" forward to eip=0x%08lx\n", curthr()->context[PCB_EIP]);
#ifdef KERNEL
		cli in curthr->context
#else
		sigaddset(&(curthr()->sigmask), SIGALRM);
#endif
		destroy_switch_to(curthrP(), curthr());
		//reschedule();
		sys_panic("should not be reached");
	}
#endif				/* ROLLFORWARD_ON_PREEMPTION */

	thread_profile_irq(curthr()->context[PCB_EIP]);

	reschedule();
	sys_panic("should not be reached");
}

#ifdef KERNEL
void save(struct irqcontext *sc)
{
#else
void save(struct sigcontext *sc)
{
#endif

	ThreadDesc *thread = curthr();


	thread->context[PCB_GS] = sc->gs;
	thread->context[PCB_ES] = sc->es;
	thread->context[PCB_FS] = sc->fs;

	thread->context[PCB_EDI] = sc->edi;
	thread->context[PCB_ESI] = sc->esi;
	thread->context[PCB_EBP] = sc->ebp;
	thread->context[PCB_ESP] = sc->esp;
	thread->context[PCB_EBX] = sc->ebx;
	thread->context[PCB_EDX] = sc->edx;
	thread->context[PCB_ECX] = sc->ecx;
	thread->context[PCB_EAX] = sc->eax;
	thread->context[PCB_EIP] = sc->eip;
	thread->context[PCB_EFLAGS] = sc->eflags;
#ifndef KERNEL
	thread->preempted = 1;
	sigdelset(&thread->sigmask, SIGALRM);
#endif
}

#ifdef KERNEL
void save_timer(struct irqcontext_timer *sc)
{
	ThreadDesc *thread = curthr();

	thread->context[PCB_GS] = sc->gs;
	thread->context[PCB_ES] = sc->es;
	thread->context[PCB_FS] = sc->fs;

	thread->context[PCB_EDI] = sc->edi;
	thread->context[PCB_ESI] = sc->esi;
	thread->context[PCB_EBP] = sc->ebp;
	thread->context[PCB_ESP] = sc->esp;
	thread->context[PCB_EBX] = sc->ebx;
	thread->context[PCB_EDX] = sc->edx;
	thread->context[PCB_ECX] = sc->ecx;
	thread->context[PCB_EAX] = sc->eax;
	thread->context[PCB_EIP] = sc->eip;
	thread->context[PCB_EFLAGS] = sc->eflags;
}
#endif




void threads_init()
{
	printf("init threads system\n");

	threads_profile_init();

#ifndef NEW_SCHED
	runq_init();
#else
	sched_init();
#endif
	/* create idle thread. Do NOT insert it into runqueue */
	__idle_thread[0] = createThread(domainZero, idle, (void *) 0, STATE_RUNNABLE, SCHED_CREATETHREAD_NORUNQ);
	setThreadName(idle_thread, "Idle", NULL);

#ifndef KERNEL
	threads_emulation_init();
#endif
	printf("init threads system completed\n");
}

#ifdef SMP
void smp_idle_threads_init()
{
	int i;

#ifdef DEBUG			// current thread is not consistent, so we have to deactivate the check
	check_current = 0;
#endif
	/* create idle Threads */
	for (i = 1; i < num_processors_online; i++)
		if (__idle_thread[online_cpu_ID[i]] == NULL)
			__idle_thread[online_cpu_ID[i]] = createThread(domainZero, idle, (void *) 0);
#ifdef DEBUG
	check_current = 1;
#endif

#ifdef JAVASCHEDULER
	smp_scheduler_init();
#endif
}
#endif

#ifdef DEBUG
int check_current = 1;

#ifdef DEBUG
char *emergency_stack[1024];
#endif

ThreadDesc *curthr()
{
	int cpuID = get_processor_id();
#if 0
#ifdef CHECK_CURRENT
	u4_t *sp;
	ASSERTTHREAD(__current[cpuID]);
	if (check_current) {
		sp = (u4_t *) & cpuID;
		if (sp <= __current[cpuID]->stack || sp >= __current[cpuID]->stackTop) {
			cli();

			printf("CURRENT NOT CONSISTENT!!!\n");
			printf("THREAD: %p\n", __current[cpuID]);
			printf("ESP: %p\n", &sp + 1);
			printNStackTrace("CUR: ", __current[cpuID], (u4_t *) & cpuID + 1, 7);
			sys_panic("current not consistent %p.", __current[cpuID]);
		}
		check_not_in_runq(__current[cpuID]);
		checkStackTrace(__current[cpuID], (u4_t *) & cpuID + 1);
	}
#endif
#endif				/* 0 */

	ASSERTDOMAIN(__current[cpuID]->domain);

	return __current[cpuID];
}

ThreadDesc **curthrP()
{
	int cpuID = get_processor_id();
#ifdef CHECK_CURRENT
	u4_t *sp;
	if (check_current) {
		sp = (u4_t *) & cpuID;
		ASSERTTHREAD(__current[cpuID]);
		if (sp <= __current[cpuID]->stack || sp >= __current[cpuID]->stackTop) {
			printStackTrace("CURP: ", __current[cpuID], (u4_t *) & cpuID + 1);
			sys_panic("current not consistent %p.", __current[cpuID]);
		}
		check_not_in_runq(__current[cpuID]);
		checkStackTrace(__current[cpuID], (u4_t *) & cpuID + 1);
	}
#endif
	return &__current[cpuID];
}

DomainDesc *curdom()
{
	int cpuID = get_processor_id();
#ifdef CHECK_CURRENT
	ASSERTTHREAD(__current[cpuID]);
	if (check_current) {
		check_not_in_runq(__current[cpuID]);
	}
#endif
	if (__current[cpuID] == NULL)
		return NULL;
	return __current[cpuID]->domain;
}

void set_current(ThreadDesc * c)
{
	int cpuID = get_processor_id();
#ifdef CHECK_CURRENT
	if (check_current) {
		check_not_in_runq(c);
	}
#endif
	__current[cpuID] = c;
	ASSERTTHREAD(__current[cpuID]);
}

#endif

jint switch_to(ThreadDesc ** current, ThreadDesc * to)
{
	u4_t result;

	DISABLE_IRQ;

	//printf("switch from %d.%d TO %d.%d\n", TID(*current), TID(to));
	//if (to->id ==9) printStackTraceNew("TO");
	ASSERTTHREAD(to);
	ASSERT(current != NULL);
	ASSERTTHREAD((*current));
	//ASSERT(curthr() == idle_thread || curthr()->state != STATE_RUNNABLE); /* because curthr is NOT added to runq */  

	result = internal_switch_to(current, to);

	RESTORE_IRQ;

	return result;
}

void thread_prepare_to_copy()
{
	ThreadDesc *cur = curthr();
	ASSERTCLI;
	cur->max_copied = curdom()->scratchMemSize / sizeof(struct copied_s);
	//cur->max_copied = 500;
	cur->copied = curdom()->scratchMem;	//jxmalloc(sizeof(struct copied_s)*cur->max_copied);
	curthr()->n_copied = 0;
}

#ifdef CPU_USAGE_STATISTICS
static u8_t lasttime = 0;
void profile_cputime()
{
	u8_t t, diff;
	t = get_tsc();
	if (lasttime == 0) {
		lasttime = t;
	}
	diff = t - lasttime;
	curdom()->cputime += diff;
	curthr()->cputime += diff;
	lasttime = t;
}
#endif				/* CPU_USAGE_STATISTICS */
