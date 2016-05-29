#ifndef NEW_SCHED
/********************************************************************************
 * Scheduler
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#include "all.h"
#include "runq.h"

extern volatile struct runqueue_s domain_runqueue;
//#define VERBOSE_BLOCK 1

INLINE void Sched_block(u4_t state)
{
	ASSERTCLI;
	/* inform profiler */
	PROFILE_STOP_BLOCK(curthr());

#ifdef VERBOSE_BLOCK
	printf("BLOCKED: %d.%d\n", TID(curthr()));
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
	Sched_blocked(curthr());
	Sched_switch_to_nextThread();
	curthr()->state = STATE_RUNNABLE;

	/* inform profiler */
	PROFILE_CONT_BLOCK(curthr());
}



/* this function is called by a portal sender that wishes to handoff the timeslot to 
 * thread "receiver"
 */
INLINE void Sched_portal_handoff_to_receiver(ThreadDesc * receiver)
{
	//printf("Sched_portal_handoff_to_receiver %d.%d\n", TID(receiver));
	curthr()->state = STATE_PORTAL_WAIT_FOR_RET;
	receiver->state = STATE_RUNNABLE;
#ifdef PORTAL_HANDOFF
	runqueue_switchdomains(curdom(), receiver->domain);
	switch_to(curthrP(), receiver);
#else
	threadunblock(receiver);	/* sets state to RUNNABLE */
	Sched_switch_to_nextThread();
#endif
	ASSERT(curthr()->state == STATE_RUNNABLE);
}

/* this function is called by a portal receiver that wishes to handoff the timeslot to 
 * thread "sender"
 * isRunnable: 1=this thread is runnable, 0=this thread is in state PORTAL_WAIT_FOR_SND
 */

INLINE void Sched_portal_handoff_to_sender(ThreadDesc * sender, int isRunnable)
{
	//printf("Sched_portal_handoff_to_sender %d.%d\n", TID(sender));
	if (isRunnable) {
		ThreadDesc *thread = curthr();
		curthr()->state = STATE_RUNNABLE;
		threadrunq_append(thread->domain, thread);
	} else {
		curthr()->state = STATE_PORTAL_WAIT_FOR_SND;	/* waiting for next call */
	}
#ifdef PORTAL_HANDOFF
	runqueue_switchdomains(curdom(), sender->domain);
	sender->state = STATE_RUNNABLE;
	switch_to(curthrP(), sender);
#else
	threadunblock(sender);
	Sched_switch_to_nextThread();
#endif
}

/* same as Sched_portal_handoff_to_sender(ThreadDesc *sender, int isRunnable) 
 * but current thread state is not saved but restored from TCB context and thread is runnable
 */
INLINE void Sched_portal_destroy_handoff_to_sender(ThreadDesc * sender)
{
	ThreadDesc *thread = curthr();
	threadrunq_append(thread->domain, thread);
#ifdef PORTAL_HANDOFF
	runqueue_switchdomains(curdom(), sender->domain);
	sender->state = STATE_RUNNABLE;
	destroy_switch_to(curthrP(), sender);
#else
	threadunblock(sender);
	Sched_destroy_switch_to_nextThread(curdom());
#endif
}

/* unblocks the sender of a portal call */
INLINE void Sched_portal_unblock_sender(ThreadDesc * sender)
{
	threadunblock(sender);
}




/*
 * general management function
 */

/* choose and activate next thread to run */
INLINE int Sched_switch_to_nextThread()
{
	ThreadDesc *next;
	ASSERTLOCKED;

	next = runqueue_removeFirstOrIdle(curdom());
	check_not_in_runq(next);
	return switch_to(curthrP(), next);
}

INLINE void domainrunq_append(DomainDesc * domain)
{
#ifdef DEBUG
	//printf("DRQ +%d\n", domain->id);
	/* check that domain is not already in runq */
	{
		volatile DomainDesc *d = domain_runqueue.first;
		while (d != NULL) {
			if (d == domain)
				sys_panic("domain already in runqueue");
			d = d->nextInRunQueue;
		}
	}
	//  if (domain == curdom()) printStackTraceNew("CURDOM");
#endif

	if (domain_runqueue.last == NULL)
		ASSERT(domain_runqueue.first == NULL);

	domain->nextInRunQueue = NULL;
	if (domain_runqueue.last == NULL) {
		domain_runqueue.last = domain_runqueue.first = domain;
	} else {
		domain_runqueue.last->nextInRunQueue = domain;
		domain_runqueue.last = domain;
	}

	//check_runq();

	//dump_runq();
	//  check_completerunnable();
}

INLINE DomainDesc *domainrunq_remove()
{
	DomainDesc *d = (DomainDesc *) domain_runqueue.first;
	if (d == NULL)
		return NULL;
	if (d == curdom())
		sys_panic("running domain should not be in runq");
	//printf("DRQ -%d\n", d->id);
	//printStackTraceNew("DRQ-");
	domain_runqueue.first = d->nextInRunQueue;
	if (d == domain_runqueue.last) {
		domain_runqueue.last = NULL;
	}
	if (!domain_runqueue.first) {
		ASSERT(domain_runqueue.last == NULL);
	}
	d->nextInRunQueue = NULL;
	return d;
}

/* remove the first thread from the threadrunqueue of "domain"
 * return NULL if there are no runnable threads in this domain
 */
INLINE ThreadDesc *threadrunq_remove(DomainDesc * domain)
{
	ThreadDesc *t = domain->firstThreadInRunQueue;

	if (t == NULL)
		return NULL;

	ASSERT(t != NULL);
	ASSERT(t->state == STATE_RUNNABLE);

	domain->firstThreadInRunQueue = t->nextInRunQueue;
	if (t == domain->lastThreadInRunQueue) {
		domain->lastThreadInRunQueue = NULL;
	}
	t->nextInRunQueue = NULL;

	return t;
}

/* return true if domain changed state to runnable 
(false if it was runnable before) */
INLINE int threadrunq_append(DomainDesc * domain, ThreadDesc * thread)
{
	thread->nextInRunQueue = NULL;
	if (domain->lastThreadInRunQueue == NULL) {	/* domain's runq was empty */
		domain->lastThreadInRunQueue = domain->firstThreadInRunQueue = thread;
		//check_runq();
		return 1;
	} else {		/* there are already threads in the domain's runq */
		domain->lastThreadInRunQueue->nextInRunQueue = thread;
		domain->lastThreadInRunQueue = thread;
		//dump_runq();
		return 0;
	}
}
#endif
