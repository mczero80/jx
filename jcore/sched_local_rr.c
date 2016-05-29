#ifdef NEW_SCHED
/********************************************************************************
 * Scheduler
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#include "all.h"
#include "runq.h"
#include "sched.h"

#ifdef SCHED_LOCAL_RR

//#define IF_DBG(x) x
#define IF_DBG(x)

//#define DEBUG_SCHED_LOCAL_RR 1
//#define DEBUG_SCHED_LOCAL_RR_TRACE 1

typedef struct sched_local_rr_mem_s {
	volatile ThreadDesc *first;
	volatile ThreadDesc *last;
	volatile ThreadDesc *current;
} sched_local_rr_mem_t;

typedef struct sched_local_rr_tmem_s {
	volatile ThreadDesc *next;
} sched_local_rr_tmem_t;

#define SCHED_LOCAL_RR_MEM(domain) (*(sched_local_rr_mem_t*)(&((domain)->sched.untypedLocalMemory)))
#define SCHED_LOCAL_RR_TMEM(thread) (*(((sched_local_rr_tmem_t*)(&(thread)->untypedSchedMemory))))

static ThreadDesc *nextThread(DomainDesc * domain)
{
	ThreadDesc *t = SCHED_LOCAL_RR_MEM(domain).first;

	if (t == NULL)
		return NULL;

	ASSERT(t != NULL);
	ASSERT(t->state == STATE_RUNNABLE);

	SCHED_LOCAL_RR_MEM(domain).first = SCHED_LOCAL_RR_TMEM(t).next;
	if (t == SCHED_LOCAL_RR_MEM(domain).last) {
		ASSERT(SCHED_LOCAL_RR_TMEM(t).next == NULL);
		SCHED_LOCAL_RR_MEM(domain).last = NULL;
		SCHED_LOCAL_RR_MEM(domain).first = NULL;
	}
	SCHED_LOCAL_RR_TMEM(t).next = NULL;
#ifdef DEBUG_SCHED_LOCAL_RR
	printf("%d.%d: remove thread %d.%d \n", TID(curthr()), TID(t));
#endif
	ASSERT(t->state == STATE_RUNNABLE);

	return t;
}

static void insertThread(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
#ifdef DEBUG_SCHED_LOCAL_RR
	printf("%d.%d: append thread %d.%d \n", TID(curthr()), TID(thread));
#ifdef DEBUG_SCHED_LOCAL_RR_TRACE
	if (curthr() == thread)
		printStackTraceNew("AT");
#endif
#endif
	ASSERT(thread->state == STATE_RUNNABLE);
#ifdef DEBUG
	rr_check_notin_runq(domain, thread);
#endif
	SCHED_LOCAL_RR_TMEM(thread).next = NULL;
	if (SCHED_LOCAL_RR_MEM(domain).first == NULL) {	/* domain's runq was empty */
		SCHED_LOCAL_RR_MEM(domain).last = SCHED_LOCAL_RR_MEM(domain).first = thread;
		/* notify parent scheduler that we want to run */
		//printf("INFORM PARENT SCHEDULER\n");
		domain->sched.becomesRunnable(domain);
	} else {		/* there are already threads in the domain's runq */
		ThreadDesc *t = SCHED_LOCAL_RR_MEM(domain).last;
		SCHED_LOCAL_RR_TMEM(t).next = thread;
		SCHED_LOCAL_RR_MEM(domain).last = thread;
	}
}

static void removeThread(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	ThreadDesc *t, *prev;
#ifdef DEBUG_SCHED_LOCAL_RR
	printf("%d.%d: remove thread %d.%d \n", TID(curthr()), TID(thread));
#endif
	prev = NULL;
	t = SCHED_LOCAL_RR_MEM(domain).first;
	while (t != NULL) {
		if (t == thread) {
			if (prev) {
				SCHED_LOCAL_RR_TMEM(prev).next = SCHED_LOCAL_RR_TMEM(thread).next;
			} else {
				/* first */
				SCHED_LOCAL_RR_MEM(domain).first = SCHED_LOCAL_RR_TMEM(thread).next;
			}
			if (SCHED_LOCAL_RR_TMEM(thread).next == NULL) {
				SCHED_LOCAL_RR_MEM(domain).last = prev;
			}
			SCHED_LOCAL_RR_TMEM(thread).next = NULL;
			break;
		}
		prev = t;
		t = SCHED_LOCAL_RR_TMEM(t).next;
	}
}

/***************/

static void sched_local_rr_preempted(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_rr_preempted: %d.%d\n", TID(thread)));
}

static void sched_local_rr_interrupted(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_rr_interrupted: %d.%d\n", TID(thread)));
}

static void sched_local_rr_created(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_rr_created: %d.%d\n", TID(thread)));
	insertThread(domain, thread);
}

static void sched_local_rr_unblocked(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_rr_unblocked: %d.%d\n", TID(thread)));
	insertThread(domain, thread);
}

static void sched_local_rr_yielded(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_rr_yielded: %d.%d\n", TID(thread)));
}

static void sched_local_rr_destroyed(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_rr_destroyed: %d.%d\n", TID(thread)));
	if (thread == SCHED_LOCAL_RR_MEM(domain).current) {
		thread = nextThread(domain);
		SCHED_LOCAL_RR_MEM(domain).current = thread;
	} else {
		removeThread(domain, thread);
	}
}

static void sched_local_rr_blocked(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	ASSERT(curthr() == SCHED_LOCAL_RR_MEM(domain).current);
	IF_DBG(printf("sched_local_rr_blocked: %d.%d\n", TID(thread)));
	thread = nextThread(domain);
	SCHED_LOCAL_RR_MEM(domain).current = thread;
	if (thread == NULL)
		return;
	IF_DBG(printf("sched_local_rr_blocked: switchto=%d.%d\n", TID(thread)));
}

static void sched_local_rr_blockedInPortal(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_rr_blockedInPortal: %d.%d\n", TID(thread)));
	sched_local_rr_blocked(domain, thread);
}

static int sched_local_rr_portalCalled(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_rr_portalCalled: %d.%d\n", TID(thread)));
}



/***************/

static void sched_local_rr_activated(struct DomainDesc_s *domain)
{
	ThreadDesc *thread;
	IF_DBG(printf("sched_local_rr_activated: domain=%d\n", domain->id));
	thread = SCHED_LOCAL_RR_MEM(domain).current;
	if (thread == NULL) {
		thread = nextThread(domain);
		SCHED_LOCAL_RR_MEM(domain).current = thread;
		if (thread == NULL) {
			sys_panic("ACTIVATED DOMAIN IS NOT RUNNABLE");
		}
	}
	IF_DBG(printf("sched_local_rr_activated: switchto=%d.%d\n", TID(thread)));
	destroy_switch_to(curthrP(), thread);
}

static void sched_local_rr_switchTo(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	sched_local_rr_activated(domain);
}


/***************/

static int sched_local_rr_isRunnable(struct DomainDesc_s *domain)
{
	return (SCHED_LOCAL_RR_MEM(domain).current != NULL || SCHED_LOCAL_RR_MEM(domain).first != NULL);
}

static int sched_local_rr_dump(struct DomainDesc_s *domain)
{
	ThreadDesc *t;
	if (SCHED_LOCAL_RR_MEM(domain).current != NULL)
		printf("   current= %d.%d\n", TID(SCHED_LOCAL_RR_MEM(domain).current));
	printf("Runqueue:\n");
	if (SCHED_LOCAL_RR_MEM(domain).first == NULL)
		printf("   *** EMPTY ***\n");
	for (t = SCHED_LOCAL_RR_MEM(domain).first; t != NULL; t = SCHED_LOCAL_RR_TMEM(t).next) {
		printf("   %d.%d\n", TID(t));
	}
}

void sched_local_rr_initTCB(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	SCHED_LOCAL_RR_TMEM(thread).next = NULL;
}

#include "gc_impl.h"

void sched_local_rr_walkSpecials(DomainDesc * domain, HandleReference_t handler)
{
	ThreadDesc *tp;
	ThreadDescProxy *tpr;
	MOVETCB(SCHED_LOCAL_RR_MEM(domain).first);
	MOVETCB(SCHED_LOCAL_RR_MEM(domain).last);
	printf("move gc thread TCB\n");
}

void sched_local_rr_walkThreadSpecials(DomainDesc * domain, ThreadDesc * thread, HandleReference_t handler)
{
	ThreadDesc *tp;
	ThreadDescProxy *tpr;
	MOVETCB(SCHED_LOCAL_RR_TMEM(thread).next);
	printf("move gc thread TCB\n");
}

void Sched_becomesRunnable(DomainDesc * domain);

void sched_local_rr_init(DomainDesc * domain, sched_local_functions_t * sched)
{
	ASSERT(sizeof(SchedDescUntypedMemory_t) >= sizeof(sched_local_rr_mem_t));
	ASSERT(sizeof(SchedDescUntypedThreadMemory_t) >= sizeof(sched_local_rr_tmem_t));
	SCHED_LOCAL_RR_MEM(domain).first = NULL;
	SCHED_LOCAL_RR_MEM(domain).last = NULL;

#if 0
	sched->nextThread = sched_local_rr_nextThread;
	sched->insertThread = sched_local_rr_insertThread;
	sched->removeThread = sched_local_rr_removeThread;
#endif
	sched->preempted = sched_local_rr_preempted;
	sched->interrupted = sched_local_rr_interrupted;
	sched->created = sched_local_rr_created;
	sched->unblocked = sched_local_rr_unblocked;
	sched->yielded = sched_local_rr_yielded;

	sched->destroyed = sched_local_rr_destroyed;
	sched->blocked = sched_local_rr_blocked;
	sched->blockedInPortal = sched_local_rr_blockedInPortal;
	sched->portalCalled = sched_local_rr_portalCalled;

	sched->activated = sched_local_rr_activated;
	sched->switchTo = sched_local_rr_switchTo;

	sched->isRunnable = sched_local_rr_isRunnable;
	sched->dump = sched_local_rr_dump;
	sched->walkSpecials = sched_local_rr_walkSpecials;
	sched->walkThreadSpecials = sched_local_rr_walkThreadSpecials;
	sched->initTCB = sched_local_rr_initTCB;

	domain->sched.becomesRunnable = Sched_becomesRunnable;
}

/***********/
/* private */
static void rr_check_notin_runq(struct DomainDesc_s *domain, ThreadDesc * thread)
{
	ThreadDesc *t;
	if (SCHED_LOCAL_RR_MEM(domain).first == NULL)
		return;
	for (t = SCHED_LOCAL_RR_MEM(domain).first; t != NULL; t = SCHED_LOCAL_RR_TMEM(t).next) {
		if (t == thread)
			sys_panic("NIR failed");
	}
}


#endif
#endif
