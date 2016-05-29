#ifdef NEW_SCHED
/********************************************************************************
 * Scheduler
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#include "all.h"
#include "runq.h"
#include "sched.h"

#ifdef SCHED_LOCAL_JAVA

#define IF_DBG(x) x

//#define DEBUG_SCHED_LOCAL_RR 1
//#define DEBUG_SCHED_LOCAL_RR_TRACE 1

typedef struct sched_local_java_mem_s {
} sched_local_java_mem_t;

typedef struct sched_local_java_tmem_s {
	volatile ThreadDesc *next;
} sched_local_java_tmem_t;

#define SCHED_LOCAL_JAVA_MEM(domain) (*(sched_local_java_mem_t*)(&((domain)->sched.untypedLocalMemory)))
#define SCHED_LOCAL_JAVA_TMEM(thread) (*(((sched_local_java_tmem_t*)(&(thread)->untypedSchedMemory))))

/***************/

static void sched_local_java_preempted(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_java_preempted: %d.%d\n", TID(thread)));
}

static void sched_local_java_interrupted(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_java_interrupted: %d.%d\n", TID(thread)));
}

static void sched_local_java_created(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_java_created: %d.%d\n", TID(thread)));
}

static void sched_local_java_unblocked(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_java_unblocked: %d.%d\n", TID(thread)));
}

static void sched_local_java_yielded(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_java_yielded: %d.%d\n", TID(thread)));
}

static void sched_local_java_destroyed(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_java_destroyed: %d.%d\n", TID(thread)));
}

/* returns 1 if domain is idle */
static int sched_local_java_blocked(struct DomainDesc_s *domain)
{
	IF_DBG(printf("sched_local_java_blocked: %d.%d\n", TID(curthr())));
	return 1;
}

static void sched_local_java_blockedInPortal(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_java_blockedInPortal: %d.%d\n", TID(thread)));
}

static int sched_local_java_portalCalled(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	IF_DBG(printf("sched_local_java_portalCalled: %d.%d\n", TID(thread)));
}



/***************/


static int sched_local_java_activated(struct DomainDesc_s *domain)
{
	IF_DBG(printf("sched_local_java_activated: domain=%d\n", domain->id));
	return 0;
}

static void sched_local_java_switchTo(struct DomainDesc_s *domain)
{
	IF_DBG(printf("sched_local_java_switchTo: domain=%d\n", domain->id));
}


/***************/

static int sched_local_java_isRunnable(struct DomainDesc_s *domain)
{
	return 0;
}

static void sched_local_java_dump(struct DomainDesc_s *domain)
{
}

void sched_local_java_initTCB(struct DomainDesc_s *domain, struct ThreadDesc_s *thread)
{
	SCHED_LOCAL_JAVA_TMEM(thread).next = NULL;
}

#include "gc_impl.h"

void sched_local_java_walkSpecials(DomainDesc * domain, HandleReference_t handler)
{
}

void sched_local_java_walkThreadSpecials(DomainDesc * domain, ThreadDesc * thread, HandleReference_t handler)
{
	ThreadDesc *tp;
	ThreadDescProxy *tpr;
	MOVETCB(SCHED_LOCAL_JAVA_TMEM(thread).next);
	printf("move gc thread TCB\n");
}

void Sched_becomesRunnable(DomainDesc * domain);

void sched_local_java_init(DomainDesc * domain, sched_local_functions_t * sched)
{
	ASSERT(sizeof(SchedDescUntypedMemory_t) >= sizeof(sched_local_java_mem_t));
	ASSERT(sizeof(SchedDescUntypedThreadMemory_t) >= sizeof(sched_local_java_tmem_t));

	sched->preempted = sched_local_java_preempted;
	sched->interrupted = sched_local_java_interrupted;
	sched->created = sched_local_java_created;
	sched->unblocked = sched_local_java_unblocked;
	sched->yielded = sched_local_java_yielded;

	sched->destroyed = sched_local_java_destroyed;
	sched->blocked = sched_local_java_blocked;
	sched->blockedInPortal = sched_local_java_blockedInPortal;
	sched->portalCalled = sched_local_java_portalCalled;

	sched->activated = sched_local_java_activated;
	sched->switchTo = sched_local_java_switchTo;

	sched->isRunnable = sched_local_java_isRunnable;
	sched->dump = sched_local_java_dump;
	sched->walkSpecials = sched_local_java_walkSpecials;
	sched->walkThreadSpecials = sched_local_java_walkThreadSpecials;
	sched->initTCB = sched_local_java_initTCB;

	domain->sched.becomesRunnable = Sched_becomesRunnable;
}

/***********/
/* private */

#endif
#endif
