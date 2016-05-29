#ifndef NEW_SCHED
/********************************************************************************
 * Scheduler
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#include "config.h"

#ifndef JAVASCHEDULER

#include "all.h"
#include "runq.h"

#ifndef DEBUG
static inline void check_runnable(DomainDesc * domain)
{
}
static inline void check_completerunnable()
{
}
#endif


volatile struct runqueue_s domain_runqueue = { NULL, NULL };

#ifndef PRODUCTION
#define INLINE
#include "runq_inline.c"
#endif

/** Scheduler Interface **/

void Sched_created(ThreadDesc * thread, int param)
{
	ASSERTLOCKED;
	//printf("Created: %d.%d\n", thread->domain->id, thread->id);
	if (param == SCHED_CREATETHREAD_DEFAULT) {
		if (threadrunq_append(thread->domain, thread)) {	/* domain's runq was empty */
			//printf("Domain %d gets runnable.\n", thread->domain->id);
			/* add domain to global runq */
			if (curdom() != thread->domain) {
				domainrunq_append(thread->domain);
			} else {
				//printf(" ... but its already running.\n");
			}

		}
	}
}
void Sched_destroyed(ThreadDesc * thread)
{
	ASSERTLOCKED;
	ASSERT(thread->state != STATE_RUNNABLE);
}

void Sched_blocked(ThreadDesc * thread)
{
	ASSERTLOCKED;
	ASSERT(thread->state != STATE_RUNNABLE);
}

void Sched_unblocked(ThreadDesc * thread)
{
	ASSERTLOCKED;
	if (threadrunq_append(thread->domain, thread)) {	/* domain's runq was empty */
		/* add domain to global runq */
		if (curdom() != thread->domain)
			domainrunq_append(thread->domain);
	}
}
void Sched_yielded(ThreadDesc * thread)
{
	ASSERTLOCKED;
	if (thread == idle_thread)
		return;
	/* append myself to runqueue */
#if 1
	threadrunq_append(curthr()->domain, curthr());
#else				/* this is done elsewhere??? */
	if (threadrunq_append(curdom(), curthr())) {	/* domain's runq was empty */
		/* add domain to global runq */
		domainrunq_append(curdom());
	}
#endif
}

/*
 * Misc. Interface 
 */
/* 0: no threads exist; 1: at least one thread exists */
int Sched_threadsExist(DomainDesc * domain)
{
	if (domain->firstThreadInRunQueue == NULL)
		return 0;
	return 1;
}


/*
 * Interface to the interrupt system
 */
/* notifies the scheduler that the interrupt handler thread gets activated */
void Sched_activate_interrupt_thread(ThreadDesc * irqThread)
{
#ifdef PROFILE_EVENT_THREADSWITCH
	profile_event_threadswitch_to(irqThread);
#endif
	//printf("activate: curthr: %d.%d irqthr: %d.%d\n", TID(curthr()), TID(irqThread));
	if (curdom() != irqThread->domain) {
		runqueue_switchdomains(curdom(), irqThread->domain);
	}
}

/* notifies the scheduler that the interrupt handler thread gets deactivated */
/* this is called in the interrupt thread */
void Sched_deactivate_interrupt_thread(ThreadDesc * normalThread)
{
#ifdef PROFILE_EVENT_THREADSWITCH
	profile_event_threadswitch_to(normalThread);
#endif
	//  printf("deactivate: curthr: %d.%d irqthr: %d.%d\n", TID(curthr()), TID(normalThread));
	if (curdom() != normalThread->domain)
		runqueue_switchdomains(curdom(), normalThread->domain);
}


/*
 * Interface to timeslicing
 */

void Sched_reschedule()
{
	ThreadDesc *next;
	ASSERTLOCKED;

	next = runqueue_removeFirstOrNULL(curdom());
	if (next != NULL) {
#ifdef CPU_USAGE_STATISTICS
		curthr()->numberPreempted++;
		curdom()->preempted++;
#endif				/* CPU_USAGE_STATISTICS */
		/* add current thread to runq */
		if (curthr() != idle_thread) {
			ThreadDesc *thread = curthr();
			if (threadrunq_append(thread->domain, thread)) {	/* domain's runq was empty */
				/* add domain to global runq */
				if (next->domain != thread->domain) {
					domainrunq_append(thread->domain);
				}
			}
		}
		ASSERT(next != NULL);
		ASSERT(curthr() != next);

		ASSERT(check_in_runq(curthr()));
		check_not_in_runq(next);

		destroy_switch_to(curthrP(), next);
	}
}

/*
 * Interface to ???
 */

void Sched_domainLeave(DomainDesc * domain)
{
	ThreadDesc *t;
	ASSERTLOCKED;
	domainrunq_remove_domain(domain);
}

/* Destroy current thread and choose next thread to run */
void Sched_destroy_switch_to_nextThread(DomainDesc * currentDomain)
{
	ThreadDesc *next;
	ASSERTLOCKED;

	next = runqueue_removeFirstOrIdle(currentDomain);

	check_not_in_runq(next);

	destroy_switch_to(curthrP(), next);
}


/******************************************************/
/***************** Generic Implementation *************/

void idle(void *x)
{
#ifdef DEBUG
	check_current = 1;	/* may be deactivated */
#endif
	for (;;) {
		/* loop forever */
#ifndef SMP
		while (domain_runqueue.first == NULL && domainZero->firstThreadInRunQueue == NULL);
		threadyield();
#endif
	}
}


void runq_init(void)
{
	domain_runqueue.first = NULL;
	domain_runqueue.last = NULL;
}

static inline int runqueue_isEmpty()
{
	return domain_runqueue.first == NULL;
}

/* a thread in domain "to" got runnable and is scheduled immediately without adding it to the runqueue 
 * if the "to" domain has runnable threads it is already in the runqueue
 * in this case it must be removed from the runqueue
 * if the current domain contains runnable threads it must be added to the runqueue */
void runqueue_switchdomains(DomainDesc * currentDomain, DomainDesc * to)
{
	ASSERT(curdom() == currentDomain);
	if (currentDomain->firstThreadInRunQueue != NULL) {
		/* current domain contains runnable threads */
		domainrunq_append(currentDomain);
	}

	if (to->firstThreadInRunQueue != NULL) {
		DomainDesc *d;
		DomainDesc *prev;
		/* remove from runqueue */
		//printf("DRQ -%d (switchdomains)\n", to->id);
		prev = NULL;
		d = (DomainDesc *) domain_runqueue.first;
		while (d != NULL) {
			if (d == to) {
				if (prev) {
					prev->nextInRunQueue = to->nextInRunQueue;
				} else {
					/* first */
					domain_runqueue.first = to->nextInRunQueue;
				}
				if (to->nextInRunQueue == NULL) {
					ASSERT(to == domain_runqueue.last);
					domain_runqueue.last = prev;
				}
				to->nextInRunQueue = NULL;
				break;
			}
			prev = d;
			d = d->nextInRunQueue;
		}
	}
	//dump_runq();
	if (!domain_runqueue.first) {
		ASSERT(domain_runqueue.last == NULL);
	}
	if (!domain_runqueue.last) {
		ASSERT(domain_runqueue.first == NULL);
	}

}

void domainrunq_remove_domain(DomainDesc * domain)
{
	DomainDesc *d;
	DomainDesc *prev;
	prev = NULL;
	d = (DomainDesc *) domain_runqueue.first;
	while (d != NULL) {
		if (d == domain) {
			if (prev) {
				prev->nextInRunQueue = domain->nextInRunQueue;
			} else {
				/* first */
				domain_runqueue.first = domain->nextInRunQueue;
			}
			if (domain->nextInRunQueue == NULL) {
				ASSERT(domain == domain_runqueue.last);
				domain_runqueue.last = prev;
			}
			domain->nextInRunQueue = NULL;
			break;
		}
		prev = d;
		d = d->nextInRunQueue;
	}
}

static inline ThreadDesc *runqueue_removeFirstOrNULL(DomainDesc * currentDomain)
{
	DomainDesc *newdom;
	ThreadDesc *next;
	ASSERTLOCKED;

#ifdef DEBUG
	check_completerunnable();
#endif

	newdom = domainrunq_remove();
	if (newdom == NULL) {
		/* no other domain is runnable -> check whether current domain has nonempty runq */
		next = threadrunq_remove(currentDomain);
		return next;
	}

	ASSERT(newdom->firstThreadInRunQueue);	/* newdom must contain runnable threads */

	//check_runq();

	/* other domain is runnable; add current domain to runq and select thread of other domain */
	if (currentDomain->firstThreadInRunQueue != NULL) {
		/* current domain contains runnable threads */
		domainrunq_append(currentDomain);
	}

	/* select next runnable thread from next domain */
	next = threadrunq_remove(newdom);

	ASSERT(next != NULL);	/* otherwise d would not be in domainrunq */
	ASSERT(next->state == STATE_RUNNABLE);

	return next;
}

ThreadDesc *runqueue_removeFirstOrIdle(DomainDesc * currentDomain)
{
	ThreadDesc *result = runqueue_removeFirstOrNULL(currentDomain);
	if (result == NULL)
		result = idle_thread;
	return result;
}

static void runqueue_remove(ThreadDesc * thread)
{
	sys_panic("");
}

void dump_runq(void)
{
	DomainDesc *d;
	d = curdom();
	printf("Running domain %ld:\n", d->id);
	dump_runqOfDomain(d);
	d = (DomainDesc *) domain_runqueue.first;
	while (d != NULL) {
		printf("Runnable domain %ld:\n", d->id);
		dump_runqOfDomain(d);
		d = d->nextInRunQueue;
	}
}
void dump_runqOfDomain(DomainDesc * domain)
{
	ThreadDesc *t;
	t = domain->firstThreadInRunQueue;
	while (t != NULL) {
		dumpThreadInfo(t);
		t = t->nextInRunQueue;
	}
}
void check_domain_not_in_runq(DomainDesc * cd)
{
	DomainDesc *d = (DomainDesc *) domain_runqueue.first;
	while (d != NULL) {
		if (d == cd)
			sys_panic("current domain in runq");
		d = d->nextInRunQueue;
	}
}

static void check_runq(void)
{
	int i = 0;
	DomainDesc *d;
	ThreadDesc *t;
	d = curdom();
	t = d->firstThreadInRunQueue;
	while (t != NULL) {
		if (t == curthr())
			sys_panic("Thread %d.%d in runq is current thread", curdom()->id, curthr()->id);
		if (t->state != STATE_RUNNABLE)
			sys_panic("Thread in runq is not RUNNABLE");
		t = t->nextInRunQueue;
	}
	d = (DomainDesc *) domain_runqueue.first;
	while (d != NULL) {
		t = d->firstThreadInRunQueue;
		while (t != NULL) {
			if (t->state != STATE_RUNNABLE)
				sys_panic("Thread in runq is not RUNNABLE");
			t = t->nextInRunQueue;
			i++;
			if (i > 1000)
				sys_panic("possible cycle in runq");
		}
		d = d->nextInRunQueue;
	}
}

#ifdef DEBUG
void check_completerunnable()
{
	foreachDomain(check_runnable);
}

// panic if domain contains runnable threads and is NOT in runq
void check_runnable(DomainDesc * domain)
{
	ThreadDesc *t;
	DomainDesc *d;
	if (domain == curdom())
		return;
	t = domain->firstThreadInRunQueue;
	while (t != NULL) {
		if (t->state = STATE_RUNNABLE)
			break;
		t = t->nextInRunQueue;
	}
	if (t == NULL)
		return;

	d = domain_runqueue.first;
	while (d != NULL) {
		if (d == domain)
			return;
		d = d->nextInRunQueue;
	}
	sys_panic("domain %d contains runnable threads but is not in runq and not the current domain", domain->id);
}

void check_not_in_runq(ThreadDesc * thread)
{
	DomainDesc *d;
	ThreadDesc *t;
	d = curdom();
	t = d->firstThreadInRunQueue;
	while (t != NULL) {
		if (t == thread)
			sys_panic("Thread %d.%d is in runq and should not be there", thread->domain->id, thread->id);
		t = t->nextInRunQueue;
	}
	d = (DomainDesc *) domain_runqueue.first;
	while (d != NULL) {
		t = d->firstThreadInRunQueue;
		while (t != NULL) {
			if (t == thread)
				sys_panic("Thread %d.%d is in runq and should not be there", thread->domain->id, thread->id);
			t = t->nextInRunQueue;
		}
		d = d->nextInRunQueue;
	}
}

#endif				/* DEBUG */

static jboolean check_in_runq(ThreadDesc * thread)
{
	return 1;
}

#endif				/*  JAVASCHEDULER  */
#endif
