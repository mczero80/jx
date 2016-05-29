#ifdef NEW_SCHED
/********************************************************************************
 * Scheduler
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#include "all.h"
#include "runq.h"
#include "sched.h"

#ifdef SCHED_GLOBAL_RR

#define IF_DBG(x)
//#define IF_DBG(x) x

#ifdef PROFILE_EVENT_THREADSWITCH
static int event_preempted = 0;
static int event_blocked = 0;
static int event_yielded = 0;
static int event_idle_start = 0;
static int event_idle_end = 0;
static int event_activate_irq_thread = 0;
static int event_deactivate_irq_thread = 0;
static int event_activate_thread = 0;
static int event_portal_to_sender = 0;
#endif

//#define DEBUG_SCHED_GLOBAL_RR 1
//#define DEBUG_SCHED_GLOBAL_RR_IRQ 1

typedef struct sched_global_rr_mem_s {
	struct DomainDesc_s *nextInRunQueue;
	u4_t queued;		/* 1 if domain is in runq, 0 otherwise */
	sched_local_functions_t local;
} sched_global_rr_mem_t;

#define SCHED_RR_MEM(domain) (*(sched_global_rr_mem_t*)(&((domain)->sched.untypedGlobalMemory)))

static volatile struct {
	volatile DomainDesc *first;
	volatile DomainDesc *last;
} runq = {
NULL, NULL};

static void save_state()
{
	register unsigned int _temp__;
	u4_t *sp, *ebp, *eip;
	unsigned long int eflags;
	asm volatile ("movl %%ebp, %%eax; movl (%%eax), %0":"=r" (_temp__));

	sp = _temp__;

//      sp = (u4_t *) * sp; /* up one frame */

	ebp = (u4_t *) * sp++;
	eip = (u4_t *) * sp++;

	curthr()->context[PCB_ESP] = sp;
	curthr()->context[PCB_EIP] = eip;
	curthr()->context[PCB_EBP] = ebp;
#ifdef KERNEL
	asm volatile ("pushfl;" "popl %0":"=r" (eflags));
	curthr()->context[PCB_EFLAGS] = eflags;
#else
	get_currentthread_sigmask();
#endif
}

static DomainDesc *queue_remove()
{
	DomainDesc *d = (DomainDesc *) runq.first;
	if (d == NULL)
		return NULL;
#ifdef DEBUG
	if (d == curdom()) {
		//printf("Domain %d\n", d->id);
//              sys_panic("running domain should not be in runq");
	}
#endif
	runq.first = SCHED_RR_MEM(d).nextInRunQueue;
	if (d == runq.last) {
		runq.last = NULL;
	}
	SCHED_RR_MEM(d).nextInRunQueue = NULL;
	SCHED_RR_MEM(d).queued = 0;
#ifdef DEBUG_SCHED_GLOBAL_RR
	printf("remove domain %d\n", d->id);
#endif
	return d;
}

static void queue_append(DomainDesc * domain)
{
#ifdef DEBUG_SCHED_GLOBAL_RR
	printf("%d.%d append domain %d\n", TID(curthr()), domain->id);
	printStackTraceNew("AD");
#endif
	SCHED_RR_MEM(domain).queued = 1;
	SCHED_RR_MEM(domain).nextInRunQueue = NULL;
	if (runq.last == NULL) {
		runq.last = runq.first = domain;
	} else {
		SCHED_RR_MEM(runq.last).nextInRunQueue = domain;
		runq.last = domain;
	}
}

static void queue_prepend(DomainDesc * domain)
{				/* only needed for interrupt */
#ifdef DEBUG_SCHED_GLOBAL_RR
	printf("prepend domain %d\n", domain->id);
	printStackTraceNew("FRONT");
#endif
	ASSERT(SCHED_RR_MEM(domain).queued == 0);
	SCHED_RR_MEM(domain).queued = 1;
	SCHED_RR_MEM(domain).nextInRunQueue = runq.first;
	runq.first = domain;
	if (runq.last == NULL) {
		runq.last = runq.first;
	}
}

static DomainDesc *queue_removeFirst()
{				/* only needed for interrupt */
	DomainDesc *domain;
#ifdef DEBUG_SCHED_GLOBAL_RR
	printf("prepend domain %d\n", domain->id);
	printStackTraceNew("FRONT");
#endif
	if (runq.first == NULL)
		return NULL;
	domain = runq.first;
	runq.first = SCHED_RR_MEM(domain).nextInRunQueue;
	if (runq.first == NULL) {
		runq.last = NULL;
	}
	SCHED_RR_MEM(domain).queued = 0;
	SCHED_RR_MEM(domain).nextInRunQueue = NULL;
	return domain;
}



/* schedule new thread
 * save_state: 0=do not save thread state; 1=save thread state
 * thread: currently running thread; if NULL no information about current thread is used 
*/
#if 0
static void Sched_reschedule(DomainDesc * domain, ThreadDesc * thread, int save_state, int add_runq)
{
	ThreadDesc *next;
	DomainDesc *nextDomain;
	ASSERTLOCKED;

	nextDomain = queue_remove();

	/* no waiting domain -> continue with current domain */
	if (nextDomain == NULL) {
		nextDomain = curdom();
	}

	next = SCHED_RR_MEM(nextDomain).local.nextThread(nextDomain);

	if (next == NULL) {
		ASSERT(nextDomain == domain);
		if (!add_runq) {
			next = idle_thread;
			nextDomain = idle_thread->domain;
		} else {
			return;	/* preempted or yielded */
		}
	}

	if (add_runq) {
		if (thread && thread->state == STATE_RUNNABLE) {
			SCHED_RR_MEM(domain).local.insertThread(domain, thread);
		}
		if (domain && !SCHED_RR_MEM(domain).queued && SCHED_RR_MEM(domain).local.isRunnable(domain)
		    && nextDomain != domain) {
#ifdef DEBUG
			if (domain == next->domain) {
				printf("Domain %d\n", next->domain->id);
				sys_panic("APPEND: running domain should not be in runq");
			}
#endif
			queue_append(domain);
		}
	}

	ASSERT(next != NULL);
	ASSERT(thread != next);
#ifdef DEBUG
	if (next->state != STATE_RUNNABLE) {
		print_full_threadinfo(next);
	}
#endif
	ASSERT(next->state == STATE_RUNNABLE);

#ifdef DEBUG_SCHED_GLOBAL_RR
	if (thread)
		printStackTraceNew("");
	printf("switchTo %d.%d\n", TID(next));
	print_full_threadinfo(next);
#endif
#ifdef DEBUG
	check_domain_not_in_runq(next->domain);
#endif

#ifdef PROFILE_EVENT_THREADSWITCH
	RECORD_EVENT_INFO2(event_activate_thread, (next->domain->id << 16) | (next->id & 0xffff), next->context[PCB_EIP]);
#endif

	if (!save_state)
		destroy_switch_to(curthrP(), next);
	else
		switch_to(curthrP(), next);
}
#endif

static DomainDesc *chooseNextDomain(int add_runq)
{
	ThreadDesc *next;
	DomainDesc *nextDomain;
	ASSERTLOCKED;

	nextDomain = queue_remove();

	/* no waiting domain -> continue with current domain */
	if (nextDomain == NULL) {
		return NULL;
	}

	if (add_runq) {
		DomainDesc *domain = curdom();
		ThreadDesc *thread = curthr();
		if (domain && !SCHED_RR_MEM(domain).queued && SCHED_RR_MEM(domain).local.isRunnable(domain)
		    && nextDomain != domain) {
#ifdef DEBUG
			if (domain == next->domain) {
				printf("Domain %d\n", next->domain->id);
				sys_panic("APPEND: running domain should not be in runq");
			}
#endif
			queue_append(domain);
		}
	}

	return nextDomain;
}

static void activateNext(DomainDesc * domain)
{
	if (domain == NULL)
		domain = queue_remove();	/* preemption ! */
	while (domain && !SCHED_RR_MEM(domain).local.isRunnable(domain)) {
		domain = queue_remove();
	}
	if (domain == NULL) {
		destroy_switch_to(curthrP(), idle_thread);
	}
	SCHED_RR_MEM(domain).local.activated(domain);
	sys_panic("NEVER REACHED");
}

void Sched_preempted()
{
	DomainDesc *domain = curdom();
	DomainDesc *nextDomain;
	ASSERTTHREAD(curthr());
	ASSERTDOMAIN(curdom());
#ifdef PROFILE_EVENT_THREADSWITCH
	RECORD_EVENT_INFO2(event_preempted, (curthr()->domain->id << 16) | (curthr()->id & 0xffff), curthr()->context[PCB_EIP]);
#endif
//      Sched_reschedule(curdom(), curthr(), 0, 1);

	SCHED_RR_MEM(domain).local.preempted(domain, curthr());
	queue_append(domain);
	activateNext(NULL);
}

void Sched_created(ThreadDesc * thread, int schedParam)
{
	DomainDesc *domain = thread->domain;
	ASSERTLOCKED;
	printf("Created: %d.%d\n", TID(thread));

	SCHED_RR_MEM(domain).local.initTCB(domain, thread);

	if (schedParam == SCHED_CREATETHREAD_DEFAULT) {
		printf(" Insert: %d.%d\n", TID(thread));
//              SCHED_RR_MEM(domain).local.insertThread(domain, thread);
		SCHED_RR_MEM(domain).local.created(domain, thread);

		if (!SCHED_RR_MEM(domain).queued && SCHED_RR_MEM(domain).local.isRunnable(domain) && curdom() != domain) {
			queue_append(domain);
		}
	} else {
		printf(" NOInsert: %d.%d\n", TID(thread));
	}
}

void Sched_destroyed(ThreadDesc * thread)
{
	DomainDesc *domain = thread->domain;
	IF_DBG(printf("sched_global_rr_destroyed : %d.%d\n", TID(thread)));
	ASSERTLOCKED;
	ASSERT(thread->state != STATE_RUNNABLE);
//      SCHED_RR_MEM(domain).local.removeThread(domain, thread);
	SCHED_RR_MEM(domain).local.destroyed(domain, thread);
	if (SCHED_RR_MEM(domain).queued && !SCHED_RR_MEM(domain).local.isRunnable(domain)) {
		queue_remove(domain);
	}
}


void Sched_destroyed_current(DomainDesc * currentDomain)
{
	ASSERTCLI;
	IF_DBG(printf("sched_global_rr_destroyed_current \n"));
//      Sched_reschedule(currentDomain, NULL, 0, 0);
//???
	activateNext(currentDomain);
}

void Sched_yield()
{
	DomainDesc *domain = curdom();
	ThreadDesc *thread = curthr();
	IF_DBG(printf("sched_global_rr_yield : %d.%d\n", TID(thread)));
	ASSERTLOCKED;
#ifdef PROFILE_EVENT_THREADSWITCH
	RECORD_EVENT_INFO2(event_yielded, (curthr()->domain->id << 16) | (curthr()->id & 0xffff), curthr()->context[PCB_EIP]);
#endif
	SCHED_RR_MEM(domain).local.yielded(domain, thread);
//      Sched_reschedule(curdom(), curthr(), 1, 1);
}

void Sched_unblock(ThreadDesc * thread)
{
	DomainDesc *domain = thread->domain;
	ASSERTLOCKED;
	thread->state = STATE_RUNNABLE;
//      SCHED_RR_MEM(domain).local.insertThread(domain, thread);
	SCHED_RR_MEM(domain).local.unblocked(domain, thread);
	if (!SCHED_RR_MEM(domain).queued && SCHED_RR_MEM(domain).local.isRunnable(domain) && curdom() != domain) {
		queue_append(thread->domain);
	}
}

void Sched_block(u4_t state)
{
	int domainIsIdle;
	DomainDesc *domain = curdom();
	ThreadDesc *thread = curthr();
	IF_DBG(printf("sched_global_rr_block : %d.%d\n", TID(thread)));
	ASSERTCLI;
	/* inform profiler */
	PROFILE_STOP_BLOCK(curthr());

#ifdef VERBOSE_BLOCK
	printf("BLOCKED: %d.%d\n", TID(curthr()));
#endif
#ifdef PROFILE_EVENT_THREADSWITCH
	RECORD_EVENT_INFO2(event_blocked, (curthr()->domain->id << 16) | (curthr()->id & 0xffff), curthr()->context[PCB_EIP]);
#endif

#ifdef NEW_PORTALCALL
	if (curthr()->processingDEP) {
#ifdef VERBOSE_BLOCK
		printf("   BLOCKED SERVICE %s in domain %d\n", obj2ClassDesc(curthr()->processingDEP->obj)->name, curdom()->id);
		printf("   STARTING NEW THREAD FOR THIS SERVICE\n");
#endif
#ifdef NEW_SERVICE_THREADS
		createServiceThread(curdom(), curthr()->processingDEP->pool->index, "additional");
#endif				/* NEW_SERVICE_THREADS */
	}
#endif
	curthr()->state = state;
//      Sched_reschedule(curdom(), curthr(), 1, 0);

	SCHED_RR_MEM(domain).local.blocked(domain, curthr());
	save_state();
	activateNext(domain);
}



/*
 * Interface to the interrupt system
 */
/* notifies the scheduler that the interrupt handler thread gets activated */
static int idle_interrupted = 0;
void Sched_activate_interrupt_thread(ThreadDesc * irqThread)
{
//      printf("activate: curthr: %d.%d irqthr: %d.%d\n", TID(curthr()), TID(irqThread));
#ifdef PROFILE_EVENT_THREADSWITCH
	RECORD_EVENT_INFO2(event_activate_irq_thread, (curthr()->domain->id << 16) | (curthr()->id & 0xffff),
			   curthr()->context[PCB_EIP]);
#endif

#if 0
#ifdef PROFILE_EVENT_THREADSWITCH
	profile_event_threadswitch_to(irqThread);
#endif
#ifdef DEBUG_SCHED_GLOBAL_RR_IRQ
	printf("activate: curthr: %d.%d irqthr: %d.%d\n", TID(curthr()), TID(irqThread));
#endif
	if (curthr() != idle_thread && curdom() != irqThread->domain) {
		// add current domain in front of runqueue
		queue_prepend(curdom());
	} else {
		idle_interrupted = 1;
	}
#endif
}

/* notifies the scheduler that the interrupt handler thread gets deactivated */
/* this is called in the interrupt thread */
void Sched_deactivate_interrupt_thread(ThreadDesc * normalThread)
{
#ifdef PROFILE_EVENT_THREADSWITCH
	RECORD_EVENT_INFO2(event_deactivate_irq_thread, (curthr()->domain->id << 16) | (curthr()->id & 0xffff),
			   curthr()->context[PCB_EIP]);
#endif

#if 0
	DomainDesc *d;
#ifdef PROFILE_EVENT_THREADSWITCH
	profile_event_threadswitch_to(normalThread);
#endif
#ifdef DEBUG_SCHED_GLOBAL_RR_IRQ
	printf("deactivate: curthr: %d.%d irqthr: %d.%d\n", TID(curthr()), TID(normalThread));
#endif
	if (curdom() != normalThread->domain && !idle_interrupted) {
		d = queue_removeFirst();
		ASSERT(d == normalThread->domain);
	}
	idle_interrupted = 0;
#endif
}

void Sched_domainLeave(DomainDesc * domain)
{
	ASSERTLOCKED;
	queue_remove(domain);
}

void Sched_dump(void)
{
	DomainDesc *d;
	d = curdom();
	printf("Running domain %ld:\n", d->id);
	SCHED_RR_MEM(d).local.dump(d);
	d = (DomainDesc *) runq.first;
	printf("Domain runqueue:\n");
	while (d != NULL) {
		printf("Runnable domain %ld:\n", d->id);
		SCHED_RR_MEM(d).local.dump(d);
		d = SCHED_RR_MEM(d).nextInRunQueue;
	}
}

void Sched_dumpDomain(DomainDesc * domain)
{
	SCHED_RR_MEM(domain).local.dump(domain);
}

#include "gc_impl.h"

void Sched_gc_rootSet(DomainDesc * domain, HandleReference_t handler)
{
	/* call domain local scheduler to handle root references */
	SCHED_RR_MEM(domain).local.walkSpecials(domain, handler);
}

void Sched_gc_tcb(DomainDesc * domain, ThreadDesc * t, HandleReference_t handler)
{
	SCHED_RR_MEM(domain).local.walkThreadSpecials(domain, t, handler);
}


/************** INFORMATION INTERFACE ***********/

void Sched_becomesRunnable(DomainDesc * domain)
{
	/* look for domain in runq */
	/*printf("**BECOMES RUNNABLE %d q=%d**\n", domain->id, SCHED_RR_MEM(domain).queued); */
	if (!SCHED_RR_MEM(domain).queued && SCHED_RR_MEM(domain).local.isRunnable(domain)
	    && ((curdom() != domain) || (curthr()->isInterruptHandlerThread))) {
		//printf("**APPEND**\n"); 
		queue_append(domain);
	}
}




/************** PORTAL INTERFACE ***********/

void Sched_portal_handoff_to_receiver(ThreadDesc * receiver)
{
	ASSERTCLI;
	Sched_unblock(receiver);	/* sets state to RUNNABLE */
	Sched_block(STATE_PORTAL_WAIT_FOR_RET);
	ASSERT(curthr()->state == STATE_RUNNABLE);
}

void Sched_portal_handoff_to_sender(ThreadDesc * sender, int isRunnable)
{
	ASSERTCLI;
	Sched_unblock(sender);
#ifdef PROFILE_EVENT_THREADSWITCH
	RECORD_EVENT_INFO2(event_portal_to_sender, (curthr()->domain->id << 16) | (curthr()->id & 0xffff),
			   curthr()->context[PCB_EIP]);
#endif
	if (isRunnable) {
		ThreadDesc *thread = curthr();
		DomainDesc *domain = thread->domain;
		//Sched_reschedule(curdom(), curthr(), 1, 1);
		//sys_panic("???1"); 
		// do nothing because we are runnable
	} else {
		Sched_block(STATE_PORTAL_WAIT_FOR_SND);	/* waiting for next call */
	}
}

void Sched_portal_destroy_handoff_to_sender(ThreadDesc * sender)
{
	ASSERTCLI;
	Sched_unblock(sender);
#ifdef PROFILE_EVENT_THREADSWITCH
	RECORD_EVENT_INFO2(event_portal_to_sender, (curthr()->domain->id << 16) | (curthr()->id & 0xffff),
			   curthr()->context[PCB_EIP]);
#endif
	//Sched_reschedule(curdom(), curthr(), 1, 1);
	sys_panic("???2");
}

void Sched_portal_unblock_sender(ThreadDesc * sender)
{
	ASSERTCLI;
	Sched_unblock(sender);
}






#ifdef DEBUG
void check_domain_not_in_runq_thr(ThreadDesc * t)
{
	check_domain_not_in_runq(t->domain);
}

void check_domain_not_in_runq(DomainDesc * cd)
{
	DomainDesc *d;
	d = (DomainDesc *) runq.first;
	while (d != NULL) {
		if (d == cd)
			sys_panic("Domain in Runq");
		d = SCHED_RR_MEM(d).nextInRunQueue;
	}

}
#endif

static void panic_if_runnable(DomainDesc * domain)
{
	if (domain != domainZero && SCHED_RR_MEM(domain).local.isRunnable(domain)) {
		sys_panic("IDLE but domain is runnable");
	}
}

void idle(void *x)
{

	printf("**** IDLE STARTING ***\n");
	for (;;) {
#ifdef PROFILE_EVENT_THREADSWITCH
		RECORD_EVENT_INFO2(event_idle_start, (curthr()->domain->id << 16) | (curthr()->id & 0xffff),
				   curthr()->context[PCB_EIP]);
#endif
		for (;;) {
			/* loop forever */
			//while (queue.first == NULL && ! domainZero->sched.isRunnable());
			//printf(".");
			DISABLE_IRQ;
			if (runq.first != NULL || SCHED_RR_MEM(domainZero).local.isRunnable(domainZero)) {
				break;
			} else {
#ifdef DEBUG
				foreachDomain(panic_if_runnable);
#endif
			}

#ifdef KERNEL
#if defined(CHECK_SERIAL_IN_YIELD) || defined(DEBUG)
			{
				static int less_checks;
				if ((less_checks++ % 20) == 0)
					check_serial();
			}
#endif
#endif
			RESTORE_IRQ;

		}
#ifdef PROFILE_EVENT_THREADSWITCH
		RECORD_EVENT_INFO2(event_idle_end, (curthr()->domain->id << 16) | (curthr()->id & 0xffff),
				   curthr()->context[PCB_EIP]);
#endif
		activateNext(NULL);
		//      Sched_reschedule(domainZero, idle_thread, 1, 0);
	}

}

#ifdef SCHED_LOCAL_RR
void sched_local_rr_init(DomainDesc * domain, sched_local_functions_t * sched);
#endif

void sched_local_init(DomainDesc * domain, int schedImpl)
{
	switch (schedImpl) {
#ifdef SCHED_LOCAL_RR
	case 0:
		sched_local_rr_init(domain, &(SCHED_RR_MEM(domain).local));
		break;
#endif
	default:
		printf("SchedImpl=%d\n", schedImpl);
		exceptionHandlerMsg(THROW_RuntimeException, "unknown local scheduler implementation");
	}
}

void sched_rr_init()
{
	//printf("%d %d\n", sizeof(GlobalSchedDescUntypedMemory_t),sizeof(sched_global_rr_mem_t));
	ASSERT(sizeof(GlobalSchedDescUntypedMemory_t) >= sizeof(sched_global_rr_mem_t));
#ifdef PROFILE_EVENT_THREADSWITCH
	event_preempted = createNewEvent("SCHED_PREEMPTED");
	event_blocked = createNewEvent("SCHED_BLOCKED");
	event_yielded = createNewEvent("SCHED_YIELDED");
	event_idle_start = createNewEvent("SCHED_IDLE_START");
	event_idle_end = createNewEvent("SCHED_IDLE_END");
	event_activate_irq_thread = createNewEvent("SCHED_ACTIVATE_IRQ");
	event_deactivate_irq_thread = createNewEvent("SCHED_DEACTIVATE_IRQ");
	event_activate_thread = createNewEvent("SCHED_ACTIVATE_THREAD");
	event_portal_to_sender = createNewEvent("SCHED_PORTAL_TO_SENDER");
#endif
}


#endif
#endif
