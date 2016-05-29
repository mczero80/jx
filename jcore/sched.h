#ifndef SCHED_H
#define SCHED_H

#include "all.h"
#include "runq.h"

/*
 *
 *  VM --> GLOBAL SCHEDULER
 *
 */

/* running thread's timeslice is over
*/
void Sched_preempted();

/* notify scheduler about thread creation */
void Sched_created(ThreadDesc * thread, int param);
#define SCHED_CREATETHREAD_DEFAULT 0
#define SCHED_CREATETHREAD_NORUNQ  1

/* notify scheduler about thread termination 
 * thread data structure is valid until Sched_destroyed is left
*/
void Sched_destroyed(ThreadDesc * thread);

/* current thread is destroyed; current TCB is not available */
void Sched_destroyed_current(DomainDesc * currentDomain);

/* yield cpu */
void Sched_yield();

/* unblock thread */
void Sched_unblock(ThreadDesc * thread);

/* block current thread with specific state */
void Sched_block(u4_t state);

/* domain is terminated or suspended */
void Sched_domainLeave(DomainDesc * domain);

/* print scheduler info for debugging */
void Sched_dump(void);

/* print domain specific scheduler info for debugging */
void Sched_dumpDomain(DomainDesc *domain);

/*
 *
 *  GARBAGE COLLECTOR --> GLOBAL SCHEDULER
 *
 */
/* walk root set that is referenced by this scheduler 
 * (usually this root set is empty) 
 */
void Sched_gc_rootSet(DomainDesc * domain, HandleReference_t handler);

void Sched_gc_tcb(DomainDesc * domain, ThreadDesc *t, HandleReference_t handler);

/*
 *
 *  LOCAL SCHEDULER --> GLOBAL SCHEDULER
 *
 */

/* local scheduler informs global scheduler that it is runnable */
void Sched_becomesRunnable(DomainDesc * domain);


/*
 *
 *  IRQ --> GLOBAL SCHEDULER
 *
 */
/* notifies the scheduler that the interrupt handler thread gets activated */
void Sched_activate_interrupt_thread(ThreadDesc * irqThread);
/* notifies the scheduler that the interrupt handler thread gets deactivated */
/* this is called in the interrupt thread */
void Sched_deactivate_interrupt_thread(ThreadDesc * normalThread);


/*
 *
 *  Local interface (only needed by 2-level scheduler)
 *
 */


void sched_local_init(DomainDesc *domain, int schedImpl);


/* interface of domain specific thread scheduler */
typedef struct sched_local_functions_s {
	void (*preempted) (struct DomainDesc_s * domain, struct ThreadDesc_s * thread);
	void (*interrupted) (struct DomainDesc_s * domain, struct ThreadDesc_s * thread);
	void (*created) (struct DomainDesc_s * domain, struct ThreadDesc_s * thread);
	void (*unblocked) (struct DomainDesc_s * domain, struct ThreadDesc_s * thread);
	void (*yielded) (struct DomainDesc_s * domain, struct ThreadDesc_s * thread);
	void (*destroyed) (struct DomainDesc_s * domain, struct ThreadDesc_s * thread);
	void (*blocked) (struct DomainDesc_s * domain, struct ThreadDesc_s * thread);
	void (*blockedInPortal) (struct DomainDesc_s * domain, struct ThreadDesc_s * thread);
	void (*portalCalled) (struct DomainDesc_s * domain, struct ThreadDesc_s * thread);

	/* domain is activated */
	void (*activated) (struct DomainDesc_s * domain);

	/* please switch to thread */
	void (*switchTo) (struct DomainDesc_s * domain, struct ThreadDesc *thread);


	u4_t(*isRunnable) (struct DomainDesc_s * domain);
	u4_t(*dump) (struct DomainDesc_s * domain);
	u4_t(*walkSpecials) (struct DomainDesc_s * domain, HandleReference_t handler);
	u4_t(*walkThreadSpecials) (struct DomainDesc_s * domain, struct ThreadDesc_s *thread, HandleReference_t handler);
	u4_t(*initTCB) (struct DomainDesc_s * domain, struct ThreadDesc_s *thread);
} sched_local_functions_t;


#endif
