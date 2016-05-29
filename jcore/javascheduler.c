/********************************************************************************
 * Java-level scheduling
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#include "config.h"

#ifdef JAVASCHEDULER
#include "all.h"
#include "serialdbg.h"
#include "zero.h"
#include "monitor.h"
#include "malloc.h"
#include "smp.h"
#include "execJAVA.h"
#include "lapic.h"

CpuDesc *CpuInfo[MAX_NR_CPUS];


#ifdef DEBUG
int _check_not_in_runq(int cpu_id, ThreadDesc * thread);
#define ASSERTNIR(thread) if(_check_not_in_runq(cpu_id,thread)==-1) sys_panic("NIR");
#else
#define ASSERTNIR(thread)
#endif

#define ASSERTLOCKED ASSERTCLI

//Prototypes (for static functions)
static void DomZero_yielded(int cpu_id, ObjectDesc * thread);
static void DomZero_unblocked(int cpu_id, ObjectDesc * thread);
static void DomZero_created(int cpu_id, ObjectDesc * thread);
static void DomZero_switchTo(int cpu_id, ThreadDesc * thread);
static void DomZero_blocked(int cpu_id, ObjectDesc * thread);
static void DomZero_blockedInPortalCall(int cpu_id, ObjectDesc * thread);
static jboolean DomZero_portalCalled(int cpu_id, ObjectDesc * thread);
static void DomZero_switchedTo(int cpu_id, ObjectDesc * thread);
static void DomZero_destroyed(int cpu_id, ObjectDesc * thread);

static void DomZero_HLSched_register(int cpu_id);

/*********************** interface compatibility *************************/
struct runqueue_s {
	volatile ThreadDesc *first;
	volatile ThreadDesc *last;
};
static struct runqueue_s c_runq[MAX_NR_CPUS];	/*may be allocated dynamically */

#ifndef NEW_SCHED
void runq_init(void)
#else
void sched_init(void)
#endif
{
	CpuInfo[0] = jxmalloc(sizeof(CpuDesc) MEMTYPE_OTHER);
	memset(CpuInfo[0], 0, sizeof(CpuDesc));

	c_runq[0].first = NULL;
	c_runq[0].last = NULL;
	DomZero_HLSched_register(0);
}

#ifdef SMP
void smp_scheduler_init(void)
{
	int i;
	for (i = 1; i < num_processors_online; i++) {
		CpuInfo[i] = jxmalloc(sizeof(CpuDesc) MEMTYPE_OTHER);
		memset(CpuInfo[i], 0, sizeof(CpuDesc));

		c_runq[i].first = NULL;
		c_runq[i].last = NULL;
		DomZero_HLSched_register(i);
	}

}
#endif

void dump_runq(void)
{

	LLSchedDesc *LLS;
	DISABLE_IRQ;

	//printf("dump CPU%d \n",get_processor_id());

	LLS = &CpuInfo[get_processor_id()]->LowLevel;
	if (LLS->SchedObj != NULL)
//     callnative_special(NULL,curLLSched,CpuInfo[get_processor_id()]->LowLevel.dump_code,0);
//     executeSpecial(curdom(), "jx/zero/Scheduler", "dump", "()V", scheduler[get_processor_id()], NULL, 0);
		call_JAVA_method0(LLS->SchedObj, LLS->SchedThread, LLS->dump_code);
	else
		DomZero_dump(get_processor_id());

	RESTORE_IRQ;
}

void dump_runqOfDomain(DomainDesc * domain)
{
	LLschedulerSupport_dumpHLSched(NULL, domainDesc2Obj(domain));
}

static void MaybeRunnable(ThreadDesc * t)
{
	HLSchedDesc *HLS;
	int cpu_id = get_processor_id();

	HLS = t->schedulingDomain->Scheduler[cpu_id];
	if (!HLS->maybeRunnable) {
		HLS->maybeRunnable = JNI_TRUE;
#ifdef CONT_PORTAL
		while (t->mostRecentlyCalledBy != NULL) {
			t = t->mostRecentlyCalledBy;
			t->schedulingDomain->Scheduler[cpu_id]->maybeRunnable = JNI_TRUE;
		}
#endif
	}
}


/*********************** JAVA wrappers *************************/

#define GENERATE_WARPER(NAME, CUSTOM_CODE)                         \
void Sched_##NAME (ThreadDesc *thread) {                          \
     DomainDesc* domain = thread->schedulingDomain;                \
     HLSchedDesc* HLS;  /* the HighLevel Scheduler */              \
     ASSERTTHREAD (thread);                                        \
     ASSERTCLI;                                                    \
                                                                   \
     if (domain == NULL)                                           \
	  sys_panic("no scheduling Domain\n");                     \
                                                                   \
     sched_dprintf(2,"CPU%d: "#NAME" Thread of Domain: %s scheduled by Domain: %s\n",\
		   get_processor_id(), thread->domain->domainName, domain->domainName);\
     HLS = domain->Scheduler[get_processor_id()];                  \
     if (HLS == NULL)                                              \
	  sys_panic("no scheduler\n");                             \
     else {                                                        \
          CUSTOM_CODE;                                             \
	  call_JAVA_method1(HLS->SchedObj, HLS->SchedThread,       \
			    HLS->NAME##_code, (long)thread2CPUState(thread));     \
     }                                                             \
}

//IRQs need not to be disabled!!
#define GENERATE_LOCKED_WARPER(NAME)                    	   \
void Sched_locked_##NAME(ThreadDesc *thread) {                   \
     DISABLE_IRQ;                                                  \
     Sched_##NAME(thread);                                       \
     RESTORE_IRQ;                                                  \
}


/* must be called with IRQs disabled!! */


GENERATE_WARPER(yielded,);	/* void Sched_yielded  (ThreadDesc *thread) */
GENERATE_WARPER(created, MaybeRunnable(thread););	/* void Sched_created(ThreadDesc *thread) */
GENERATE_WARPER(unblocked, MaybeRunnable(thread););	/* void Sched_unblocked(ThreadDesc *thread) */
GENERATE_WARPER(blocked,);	/* void Sched_blocked  (ThreadDesc *thread) */
GENERATE_WARPER(destroyed,);	/* void Sched_destroyed (ThreadDesc *thread) */
#if ENABLE_GC
GENERATE_WARPER(GCunblocked, MaybeRunnable(thread););	/* void Sched_GCunblocked (ThreadDesc *thread) */
GENERATE_WARPER(GCdestroyed,);	/* void Sched_GCdestroyed (ThreadDesc *thread) */
#endif
#ifndef CONT_PORTAL
GENERATE_WARPER(blockedInPortalCall,);	/* void Sched_blockedInPortalCall(ThreadDesc *thread) */
#endif

//IRQs need not to be disabled!!
GENERATE_LOCKED_WARPER(created);	/* void Sched_locked_created(ThreadDesc *thread) */
GENERATE_LOCKED_WARPER(blockedInPortalCall);	/* void Sched_locked_blockedInPortalCall(ThreadDesc *thread) */

/*
 * Interface to portal invocation system
 */

/* sender wants to send but did not find an available receiver thread
 * thread is already in the service wait queue
 *  set this threads state to PORTAL_WAIT_FOR_RCV and switch to next runnable thread */
void Sched_block_portal_sender()
{
	curthr()->state = STATE_PORTAL_WAIT_FOR_RCV;
#ifdef VISIBLE_PORTAL
	Sched_blockedInPortalCall(curthr());
#endif
	Sched_switch_to_nextThread();
	ASSERT(curthr()->state == STATE_RUNNABLE);
	curthr()->state = STATE_RUNNABLE;
}

/* receiver is idle, the wait queue is empty, wait for sender
 *  set this threads state to PORTAL_WAIT_FOR_SND and switch to next runnable thread */
void Sched_portal_waitfor_sender()
{
	curthr()->state = STATE_PORTAL_WAIT_FOR_SND;
	Sched_switch_to_nextThread();
	ASSERT(curthr()->state == STATE_RUNNABLE);
	curthr()->state = STATE_RUNNABLE;
}


/* this function is called by a portal sender that wishes to handoff the timeslot to
 * thread "receiver"
 */
void Sched_portal_handoff_to_receiver(ThreadDesc * receiver)
{
#ifdef PORTAL_HANDOFF
	jboolean handoff = JNI_TRUE;
#else
	jboolean handoff = JNI_FALSE;
#endif
#ifdef VISIBLE_PORTALS
	HLSchedDesc *HLS = NULL;
#endif

	curthr()->state = STATE_PORTAL_WAIT_FOR_RCV2;
	receiver->state = STATE_RUNNABLE;

#ifdef VISIBLE_PORTALS
	/* check if we should ask the (callers)Scheduler to hand off */
	HLS = curthr()->schedulingDomain->Scheduler[get_processor_id()];
	if (HLS != NULL && HLS->resume == JNI_TRUE) {	/* the Scheduler Thread is busy */
		Sched_blockedInPortalCall(curthr());
		handoff = JNI_FALSE;
	} else			/* ask the Schduler if he wants to handoff the timeslot to the Portal */
		handoff = Sched_portalCalled(curthr());
#endif

	if (handoff == JNI_FALSE) {	/* no hand-off Scheduling */
//        target->depSwitchBack = JNI_FALSE;
		threadunblock(receiver);	/* sheduler was informed by Sched_blockedInPortalCall */
		Sched_switch_to_nextThread();
	} else {		/* hand-off Scheduling */
//        target->depSwitchBack = JNI_TRUE;
#ifdef VISIBLE_PORTALS
		Sched_switchTo(receiver);
#else
		switch_to(curthrP(), receiver);
#endif
//        curthr()->state = STATE_RUNNABLE;
	}
	ASSERT(curthr()->state == STATE_RUNNABLE);
}

/* this function is called by a portal sender that wishes to handoff the timeslot to
 * thread "target"
 * isRunnable: 1=this thread is runnable, 0=this thread is in state PORTAL_WAIT_FOR_SND
 */
void Sched_portal_handoff_to_sender(ThreadDesc * sender, int isRunnable)
{
	if (isRunnable) {
		ASSERT(curthr()->state == STATE_RUNNABLE);
		Sched_yielded(curthr());	/* I am still runnable */
	} else {
		curthr()->state = STATE_PORTAL_WAIT_FOR_SND;	/* waiting for next call */
#ifdef VISIBLE_PORTALS
		Sched_blocked(curthr());	/* I will block */
#endif
	}
#ifdef PORTAL_HANDOFF
	sender->state = STATE_RUNNABLE;
#   ifdef VISIBLE_PORTALS
	Sched_switchTo(sender);
#   else
	switch_to(curthrP(), sender);
#   endif
#else
	threadunblock(sender);	/* sets state to RUNNABLE */
	Sched_switch_to_nextThread();
#endif
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
}

/* notifies the scheduler that the interrupt handler thread gets deactivated */
/* this is called in the interrupt thread */
void Sched_deactivate_interrupt_thread(ThreadDesc * normalThread)
{
	//LLschedulerSupport_interruptCurrentThread(0);
#ifdef PROFILE_EVENT_THREADSWITCH
	profile_event_threadswitch_to(normalThread);
#endif
	//  printf("deactivate: curthr: %d.%d irqthr: %d.%d\n", TID(curthr()), TID(normalThread));
}


/* must be called with IRQs disabled!! */
jboolean Sched_portalCalled(ThreadDesc * thread)
{
	DomainDesc *domain = thread->schedulingDomain;
	HLSchedDesc *HLS;	/* the HighLevel Scheduler */
	ASSERTTHREAD(thread);

	if (domain == NULL)
		sys_panic("no scheduling Domain\n");

	sched_dprintf(2, "CPU%d: called a Portal Thread of Domain: %s scheduled by Domain: %s\n", get_processor_id(),
		      thread->domain->domainName, domain->domainName);
	HLS = domain->Scheduler[get_processor_id()];
	if (HLS == NULL)
		sys_panic("no scheduler\n");
	else if (HLS->portalCalled_code == NULL)
		return JNI_TRUE;	/* return true if not implemented */
	else {
		MaybeRunnable(thread);
#ifdef CONT_PORTAL
		return call_JAVA_method2(HLS->SchedObj, HLS->SchedThread, HLS->portalCalled_code, (long) thread2CPUState(thread),
					 (long) thread2CPUState(thread));
#else
		return call_JAVA_method1(HLS->SchedObj, HLS->SchedThread, HLS->portalCalled_code, (long) thread2CPUState(thread));
#endif
	}
	sys_panic("never reached\n");
	return JNI_FALSE;	/* to satisfy compiler */
}


jboolean Sched_locked_portalCalled(ThreadDesc * thread)
{
	jboolean result;
	DISABLE_IRQ;
	result = Sched_portalCalled(thread);
	RESTORE_IRQ;
	return result;
}

#ifdef CONT_PORTAL
void Sched_blockedInPortalCall(ThreadDesc * thread)
{
	DomainDesc *domain = thread->schedulingDomain;
	HLSchedDesc *HLS;	/* the HighLevel Scheduler */
	ASSERTTHREAD(thread);

	if (domain == NULL)
		sys_panic("no scheduling Domain\n");

	sched_dprintf(2, "CPU%d: Sched_blockedInPortalCall Thread of Domain: %s scheduled by Domain: %s\n", get_processor_id(),
		      thread->domain->domainName, domain->domainName);
	HLS = domain->Scheduler[get_processor_id()];
	if (HLS == NULL)
		sys_panic("no scheduler\n");
	else
		call_JAVA_method2(HLS->SchedObj, HLS->SchedThread, HLS->blockedInPortalCall_code, (long) thread2CPUState(thread),
				  (long) thread2CPUState(thread));
}
#endif

/* does not return */
/* must be called with IRQs disabled!! */
void Sched_destroy_switchTo(ThreadDesc * thread)
{
	DomainDesc *domain = thread->schedulingDomain;
	HLSchedDesc *HLS;	/* the HighLevel Scheduler */
	ASSERTTHREAD(thread);
	ASSERTCLI;

	if (domain == NULL)
		sys_panic("no scheduling Domain\n");

	sched_dprintf(1, "CPU%d: Sched_destroy_switchTo Thread (%p) of Domain: %s scheduled by Domain: %s\n", get_processor_id(),
		      thread, thread->domain->domainName, domain->domainName);

	if (domain == domainZero)
		DomZero_switchTo(get_processor_id(), thread);

	HLS = domain->Scheduler[get_processor_id()];
	if (HLS == NULL) {
		sched_dprintf(1, "no Scheduler for Scheduling Domain %s \n", domain->domainName);
		MaybeRunnable(idle_thread);
		destroy_switch_to(curthrP(), thread);
	} else {
		MaybeRunnable(thread);
		if (HLS->switchedTo_code == NULL)	/* not implemented */
			destroy_switch_to(curthrP(), thread);
		else {
			if (HLS->resume == JNI_TRUE)
				sys_panic
				    ("Domain (%s) wants to be resumed but HLSThread (%p) is needed to activate the specified Thread (=Portal)!!\n",
				     domain->domainName, HLS->SchedActivateThread);
			destroy_call_JAVA_method1(HLS->SchedObj, HLS->SchedActivateThread, HLS->switchedTo_code, (long)
						  thread2CPUState(thread), CALL_WITH_ENABLED_IRQS);
		}





	}
	sys_panic("never reached\n");
}

#ifdef ENABLE_GC
/* must be called with IRQs disabled!! */
void Sched_startGCThread(ThreadDesc * GCThread)
{
	DomainDesc *domain;
	HLSchedDesc *HLS;	/* the HighLevel Scheduler */
	ThreadDesc *interruptedThread = curthr();
	int cpu_id = get_processor_id();
	ASSERTTHREAD(interruptedThread);
	ASSERTTHREAD(GCThread);
	ASSERTCLI;

	domain = GCThread->schedulingDomain;
	ASSERTDOMAIN(domain);

	/*printf("GCdom:%s INTDom:%s\n",domain->domainName, interruptedThread->schedulingDomain->domainName); */
	if (domain != interruptedThread->schedulingDomain) {
		printf("\nschedulingDomain of interruptedThread != schedDom of GCThread\n");
		printf("interruptedThread:%p, schedDom:%s\n", interruptedThread, interruptedThread->schedulingDomain->domainName);
		printf("         GCThread:%p, schedDom:%s\n", GCThread, domain->domainName);
		sys_panic("\n");
	}
	if (domain == domainZero)
		DomZero_switchTo(get_processor_id(), GCThread);

	HLS = domain->Scheduler[cpu_id];
	if (HLS == NULL)
		sys_panic("no scheduler\n");
	else if (HLS->startGC_code == NULL)
		sys_panic("FIXME: Scheduler of Domain %d does not implement startGCThread\n", domain->id);
	else {
		if (interruptedThread == domain->Scheduler[cpu_id]->SchedActivateThread) {

			if (call_JAVA_method0
			    (domain->Scheduler[cpu_id]->SchedObj, domain->Scheduler[cpu_id]->SchedThread,
			     domain->Scheduler[cpu_id]->SchedPreempted_code) == JNI_FALSE) {
				/* Thread schould be resumed at the next CPU slot */
#ifdef DEBUG
				if (domain->Scheduler[cpu_id]->resume == JNI_TRUE)
					sys_panic
					    ("System Error in LLschedulerSupport_preemptCurrentThread: resume schould be false!");
#endif
				ASSERT(domain->Scheduler[cpu_id]->resume == JNI_FALSE);
				domain->Scheduler[cpu_id]->resume = JNI_TRUE;
				interruptedThread = NULL;
			}
			/* else:  everythig is ok (the Scheduler has handled the problem ) */
		}
		destroy_call_JAVA_method2(HLS->SchedObj, HLS->SchedThread, HLS->startGC_code, (long)
					  thread2CPUState(interruptedThread), (long) thread2CPUState(GCThread),
					  CALL_WITH_DISABLED_IRQS);
	}
	sys_panic("never reached\n");
	return;			/* to satisfy compiler */
}
#endif


/*********************** doamin activity  *************************/
void Sched_domainLeave(DomainDesc * domain)
{
	ThreadDesc *t;
	ASSERTLOCKED;
	sys_panic("Sched_DomainStopped not implemented");
//     for(t=domain->threads; t!=NULL; t=t->nextInDomain) {
//      if (t->state == STATE_RUNNABLE)  runqueue_remove(t);
//     }
}

void Sched_DomainTerminated(DomainDesc * domain)
{
	sys_panic("Sched_DomainTerminated not implemented");
}

/*********************** activate_next  *************************/
int Sched_switch_to_nextThread()
{
	ASSERTCLI;
	switch_to_nextDomain();
}

void Sched_destroy_switch_to_nextThread(DomainDesc * currentDomain)
{
	destroy_activate_nextThread();
}

/* Does not return!!! */
void destroy_activate_nextThread()
{
	LLSchedDesc *LLS = &CpuInfo[get_processor_id()]->LowLevel;
	/*printf("destroy_activate_nextThread called\n"); */

	ASSERTCLI;
//     CLI;

	if (LLS->SchedObj == NULL) {
		sched_dprintf(1, "no LLscheduler -> activating domainZero\n");
		DomZero_activated(get_processor_id());
		sys_panic("should not return\n");
	}

	destroy_call_JAVA_function(LLS->SchedObj, LLS->SchedThread, LLS->activateCurrDomain_code, CALL_WITH_DISABLED_IRQS);

	sys_panic("should not return\n");
}

/* does not return!!! */
void destroy_activate_nextDomain(void)
{
	LLSchedDesc *LLS = &CpuInfo[get_processor_id()]->LowLevel;
	/*printf("destroy_activate_nextDOMAIN called\n"); */

	ASSERTCLI;
	//CLI;

	/*printf("LLS->SchedObj:%p\n",LLS->SchedObj); */

	if (LLS->SchedObj == NULL) {
		sched_dprintf(1, "no LLscheduler -> activating domainZero\n");
		DomZero_activated(get_processor_id());
		sys_panic("should not return\n");
	}
	destroy_call_JAVA_function(LLS->SchedObj, LLS->SchedThread, LLS->activateNextDomain_code, CALL_WITH_DISABLED_IRQS);
	sys_panic("never reached\n");
}

/*********************** LowLevelScheduler  *************************/
static void scheduler_thread()
{
	sys_panic("scheduler_thread: SHOULD NOT BE CALLED");
}

void LLSched_register(DomainDesc * domain, ObjectDesc * new_sched)
{
	int index;
	int timer_irq_nr = 0;
	LLSchedDesc *LLS = &CpuInfo[get_processor_id()]->LowLevel;
#ifdef APIC
	if (apic_found)
		timer_irq_nr = LAPIC_TIMER_IRQNR;
#endif

	sched_dprintf(3, "LLSched_register called\n");

	if (!cas((u4_t *) (&LLS->SchedObj), (u4_t) NULL, (u4_t) new_sched)) {
		sys_panic("a LLscheduler is already installed (on CPU%d)\n", get_processor_id());
	}

	DISABLE_IRQ;

//    LLS->SchedObj = new_sched;  // done by CAS
	LLS->SchedThread = createThread(domain, scheduler_thread, NULL);
	LLS->SchedThread->name = "LLSThread";
	LLS->SchedThread->isInterruptHandlerThread = 1;
	LLS->SchedThread->state = STATE_AVAILABLE;
	index = findDEPMethodIndex(domain, "jx/zero/LowLevelScheduler", "registered", "(I)V");
	LLS->registered_code = (java_method1_t) LLS->SchedObj->vtable[index];

	index = findDEPMethodIndex(domain, "jx/zero/LowLevelScheduler", "registerDomain", "(Ljx/zero/Domain;)V");
	LLS->registerDomain_code = (java_method1_t) LLS->SchedObj->vtable[index];

	index = findDEPMethodIndex(domain, "jx/zero/LowLevelScheduler", "unregisterDomain", "(Ljx/zero/Domain;)V");
	LLS->unregisterDomain_code = (java_method1_t) LLS->SchedObj->vtable[index];

	index = findDEPMethodIndex(domain, "jx/zero/LowLevelScheduler", "setTimeSlice", "(Ljx/zero/Domain;I)V");
	LLS->setTimeSlice_code = (java_method2_t) LLS->SchedObj->vtable[index];

	index = findDEPMethodIndex(domain, "jx/zero/LowLevelScheduler", "activate_currDomain", "()V");
	LLS->activateCurrDomain_code = (java_method0_t) LLS->SchedObj->vtable[index];

	index = findDEPMethodIndex(domain, "jx/zero/LowLevelScheduler", "activate_nextDomain", "()V");
	LLS->activateNextDomain_code = (java_method0_t) LLS->SchedObj->vtable[index];

	index = findDEPMethodIndex(domain, "jx/zero/LowLevelScheduler", "dump", "()V");
	LLS->dump_code = (java_method0_t) LLS->SchedObj->vtable[index];

	/* now tell the scheduler that he is registered */
	call_JAVA_method1(LLS->SchedObj, LLS->SchedThread, LLS->registered_code, timer_irq_nr);

	/* register DomainZero */
	call_JAVA_method1(LLS->SchedObj, LLS->SchedThread, LLS->registerDomain_code, (long) domainDesc2Obj(domainZero));
	/*  call_JAVA_method2(LLS->SchedObj, LLS->SchedThread,
	   LLS->setTimeSlice_code, (long)domainDesc2Obj(domainZero), 15); */

	RESTORE_IRQ;
}



/*********************** HighLevelScheduler  *************************/
/* creates a Highlevel Scheduler Descriptor from a Scheduler-Object
   (gets the Startaddress of all used Methods and creates the worker Threads)
   Parameter:  ObjDomain: the Domain, the Scheduler-Object ist located in
               SchedObj : the Scheduler-Object
   Return: the Scheduler Descriptor
*/

HLSchedDesc *createHLSchedDesc(DomainDesc * ObjDomain, ObjectDesc * SchedObj)
{
	int index;
	HLSchedDesc *new_HLSched;

	sched_dprintf(3, "createHLSchedDesc called (ObjDomain: %s) \n", ObjDomain->domainName);
	new_HLSched = jxmalloc(sizeof(HLSchedDesc) MEMTYPE_OTHER);
	memset(new_HLSched, 0, sizeof(HLSchedDesc));
	new_HLSched->SchedObj = SchedObj;
	new_HLSched->SchedThread = createThread(ObjDomain, scheduler_thread, NULL);
	new_HLSched->SchedThread->name = "HLSWorker";
	new_HLSched->SchedThread->isInterruptHandlerThread = 1;
	new_HLSched->SchedThread->state = STATE_AVAILABLE;
	new_HLSched->SchedActivateThread = createThread(ObjDomain, scheduler_thread, NULL);
	new_HLSched->SchedActivateThread->name = "HLSScheduling";
	new_HLSched->SchedActivateThread->state = STATE_AVAILABLE;
	sched_dprintf(3, "SchedThread: %p; SchedActivateThread:%p \n", new_HLSched->SchedThread,
		      new_HLSched->SchedActivateThread);
	new_HLSched->resume = JNI_FALSE;
	new_HLSched->maybeRunnable = JNI_TRUE;

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HighLevelScheduler", "registered", "()V");
	new_HLSched->registered_code = (java_method0_t) new_HLSched->SchedObj->vtable[index];

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HighLevelScheduler", "Scheduler_preempted", "()Z");
	new_HLSched->SchedPreempted_code = (java_method0_t) new_HLSched->SchedObj->vtable[index];

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HighLevelScheduler", "Scheduler_interrupted", "()Z");
	new_HLSched->SchedInterrupted_code = (java_method0_t) new_HLSched->SchedObj->vtable[index];

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_preempted", "preempted", "(Ljx/zero/CPUState;)V");
	new_HLSched->preempted_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_interrupted", "interrupted", "(Ljx/zero/CPUState;)V");
	new_HLSched->interrupted_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_yielded", "yielded", "(Ljx/zero/CPUState;)V");
	new_HLSched->yielded_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_unblocked", "unblocked", "(Ljx/zero/CPUState;)V");
	new_HLSched->unblocked_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HighLevelScheduler", "created", "(Ljx/zero/CPUState;)V");
	new_HLSched->created_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_blocked", "blocked", "(Ljx/zero/CPUState;)V");
	new_HLSched->blocked_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];

#ifdef CONT_PORTAL
	index =
	    findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_blockedInService", "blockedInService",
			       "(Ljx/zero/CPUState;Ljx/zero/CPUStateLink;)V");
	new_HLSched->blockedInPortalCall_code = (code_t) new_HLSched->SchedObj->vtable[index];

	index =
	    findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_serviceCalled", "serviceCalled",
			       "(Ljx/zero/CPUState;Ljx/zero/CPUStateLink;)Z");
	new_HLSched->portalCalled_code = (code_t) new_HLSched->SchedObj->vtable[index];
#else
	index =
	    findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_blockedInPortalCall", "blockedInPortalCall",
			       "(Ljx/zero/CPUState;)V");
	new_HLSched->blockedInPortalCall_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_portalCalled", "portalCalled", "(Ljx/zero/CPUState;)Z");
	new_HLSched->portalCalled_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];
#endif
	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_switchTo", "switchedTo", "(Ljx/zero/CPUState;)V");
	new_HLSched->switchedTo_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_destroyed", "destroyed", "(Ljx/zero/CPUState;)V");
	new_HLSched->destroyed_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];

#ifdef ENABLE_GC
	index =
	    findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_GCThread", "startGCThread",
			       "(Ljx/zero/CPUState;Ljx/zero/CPUState;)V");
	new_HLSched->startGC_code = (java_method2_t) new_HLSched->SchedObj->vtable[index];
	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_GCThread", "destroyedGCThread", "(Ljx/zero/CPUState;)V");
	new_HLSched->GCdestroyed_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];
	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HLS_GCThread", "unblockedGCThread", "(Ljx/zero/CPUState;)V");
	new_HLSched->GCunblocked_code = (java_method1_t) new_HLSched->SchedObj->vtable[index];
#endif
	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HighLevelScheduler", "activated", "()V");
	new_HLSched->activated_code = (java_method0_t) new_HLSched->SchedObj->vtable[index];

	index = findDEPMethodIndex(ObjDomain, "jx/zero/scheduler/HighLevelScheduler", "dump", "()V");
	new_HLSched->dump_code = (java_method0_t) new_HLSched->SchedObj->vtable[index];

	return new_HLSched;
}

void HLSched_register(DomainDesc * domain, HLSchedDesc * new_HLSched)
{
	int cpu_id = get_processor_id();
	sched_dprintf(1, "HLSched_register called for Domain %s \n", domain->domainName);

	//     domain->Scheduler[cpu_id]=new_HLSched;
	if (!cas((u4_t *) (&(domain->Scheduler[cpu_id])), (u4_t) NULL, (u4_t) new_HLSched)) {
		sys_panic("a HLscheduler is already installed (on CPU%d)\n", get_processor_id());
	}

	DISABLE_IRQ;
	/* tell the LowLevelScheduler that there is a new Scheduler registered */
	call_JAVA_method1(CpuInfo[cpu_id]->LowLevel.SchedObj, CpuInfo[cpu_id]->LowLevel.SchedThread,
			  CpuInfo[cpu_id]->LowLevel.registerDomain_code, (long) domainDesc2Obj(domain));
	/* tell the HighLevelScheduler that he is registered */
	call_JAVA_method0(domain->Scheduler[cpu_id]->SchedObj, domain->Scheduler[cpu_id]->SchedThread,
			  domain->Scheduler[cpu_id]->registered_code);
	/* move my Threads form current Scheduler to the new one */
	domain->Scheduler[cpu_id]->SchedThread->schedulingDomain = domain;
	domain->Scheduler[cpu_id]->SchedActivateThread->schedulingDomain = domain;

	RESTORE_IRQ;

}

void HLSched_unregister(DomainDesc * domain)
{
	int cpu_id = get_processor_id();
	HLSchedDesc *oldSched;
	ASSERTDOMAIN(domain);
	sched_dprintf(3, "HLSched_unregister called for Domain %s \n", domain->domainName);
	printf("HLSched_unregister called for Domain %s \n", domain->domainName);

	DISABLE_IRQ;
	oldSched = domain->Scheduler[cpu_id];
	domain->Scheduler[cpu_id] = NULL;

	call_JAVA_method1(CpuInfo[cpu_id]->LowLevel.SchedObj, CpuInfo[cpu_id]->LowLevel.SchedThread,
			  CpuInfo[cpu_id]->LowLevel.unregisterDomain_code, (long) domainDesc2Obj(domain));
	RESTORE_IRQ;

	jxfree(oldSched, sizeof(HLSchedDesc) MEMTYPE_OTHER);	/* was allocated by createHLSchedDesc */
}

/*********************** DomainZero HighLevel Scheduler *************************/
static void DomZero_HLSched_register(int cpu_id)
{
	HLSchedDesc *new_HLSched;

	sched_dprintf(2, "DomZero_HLSched_register called\n");

	new_HLSched = jxmalloc(sizeof(HLSchedDesc) MEMTYPE_OTHER);
	memset(new_HLSched, 0, sizeof(HLSchedDesc));

	new_HLSched->SchedObj = (ObjectDesc *) cpu_id;	// store the CPU Id in the Scheduler Object Variable
	new_HLSched->SchedThread = NULL;
	new_HLSched->SchedActivateThread = NULL;

	new_HLSched->resume = JNI_FALSE;
	new_HLSched->maybeRunnable = JNI_TRUE;

	new_HLSched->registered_code = NULL;
	new_HLSched->SchedPreempted_code = NULL;
	new_HLSched->SchedInterrupted_code = NULL;
	new_HLSched->preempted_code = (java_method1_t) DomZero_preempted;
	new_HLSched->interrupted_code = (java_method1_t) DomZero_interrupted;
	new_HLSched->yielded_code = (java_method1_t) DomZero_yielded;
	new_HLSched->unblocked_code = (java_method1_t) DomZero_unblocked;
	new_HLSched->blocked_code = (java_method1_t) DomZero_blocked;
	new_HLSched->blockedInPortalCall_code = (java_method1_t) DomZero_blockedInPortalCall;
	new_HLSched->portalCalled_code = (java_method1_t) DomZero_portalCalled;
	new_HLSched->switchedTo_code = (java_method1_t) DomZero_switchedTo;
	new_HLSched->created_code = (java_method1_t) DomZero_created;
	new_HLSched->destroyed_code = (java_method1_t) DomZero_destroyed;
	new_HLSched->activated_code = (java_method0_t) DomZero_activated;
	new_HLSched->dump_code = (java_method0_t) DomZero_dump;

	if (!cas((u4_t *) (&(domainZero->Scheduler[cpu_id])), (u4_t) NULL, (u4_t) new_HLSched)) {
		sys_panic("System error: a HLScheduler for Domain Zero is already installed (for CPU%d)\n", cpu_id);
	}
}


static void check_runq(int cpu_id);
static void DomZero_addThread(int cpu_id, ObjectDesc * cpuState);
inline ThreadDesc *cpuState2thread(ObjectDesc * obj);

void DomZero_activated(int cpu_id)
{
	ThreadDesc *t;
	/*printf(" DomZero_activated called\n"); */
	ASSERTCLI;
	for (;;) {
		{
			t = (ThreadDesc *) c_runq[cpu_id].first;
			if (t == NULL)	/* runqueue empty */
				break;

			c_runq[cpu_id].first = t->nextInRunQueue;
			if (t->nextInRunQueue == NULL)
				c_runq[cpu_id].last = NULL;

			t->nextInRunQueue = NULL;
		}

		/* check result */
		if (t == NULL)
			break;
		if (t == idle_thread)
			sys_panic("idle thread in runq");
		if (t->state == STATE_RUNNABLE)
			break;
		if (t->state == STATE_SLEEPING) {
			sys_panic("RUNQUEUE (t->state == STATE_SLEEPING)");
#if 0
			printf("sleeping thread wakup at %ld (currenttime=%ld\n", t->wakeupTime, currenttime);
			if (t->wakeupTime < currenttime) {
				/* wakeup */
				t->state = STATE_RUNNABLE;
				break;
			} else {
				/* please, let me sleep */
				DomZero_addThread(t);
			}
#endif
		} else if (t->state == STATE_PORTAL_WAIT_FOR_RCV) {
			printf("Thread %p state = %d(PORTAL_WAIT_FOR_RCV)\n", t, (int) t->state);
			sys_panic("RUNQUEUE");
		} else if (t->state == STATE_PORTAL_WAIT_FOR_SND) {
			printf("Thread %p state = %d(PORTAL_WAIT_FOR_SND)\n", t, (int) t->state);
			sys_panic("RUNQUEUE");
		} else if (t->state == STATE_ZOMBIE) {
			printf("IGNORING Thread %p state = %d(ZOMBIE)\n", t, (int) t->state);
			continue;
		} else if (t->state == STATE_BLOCKEDUSER) {
			printf("Thread %p state = %d(BLOCKEDUSER)!!!!!\n", t, (int) t->state);
			break;	/* nevertheless use it */
		} else {
			printf("Thread %p state = %d (%s)\n", t, (int) t->state, get_state(t));
			sys_panic("Unknown thread state.");
		}
	}
	/*return t; */
	if (t == NULL) {
		/* printf("CPU%d: DZ empty -> activating next Domain\n",get_processor_id()); */
		ASSERT(CpuInfo[get_processor_id()]->LowLevel.SchedObj != NULL);
		domainZero->Scheduler[get_processor_id()]->maybeRunnable = JNI_FALSE;
		curthr()->state = STATE_AVAILABLE;
		destroy_activate_nextDomain();
		sys_panic("should not return");
	}
//#ifdef VISIBLE_PORTALS
//    t->depSwitchBack = JNI_FALSE;
//#endif
	//printf("CPU%d: DomainZero is activating Thread %d.%d\n",get_processor_id(),TID(t));
	destroy_switch_to(curthrP(), t);
}

void DomZero_preempted(int cpu_id, ObjectDesc * cpuState)
{
	DomZero_addThread(cpu_id, cpuState);
}
void DomZero_interrupted(int cpu_id, ObjectDesc * cpuState)
{
	DomZero_addThread(cpu_id, cpuState);
}
static void DomZero_yielded(int cpu_id, ObjectDesc * cpuState)
{
	/*printf("CPU:%d: DomZero_yielded",get_processor_id()); */
//     domainZero->Scheduler[cpu_id]->maybeRunnable = JNI_FALSE;
	if (cpuState2thread(cpuState) != idle_thread)
		DomZero_addThread(cpu_id, cpuState);
}
static void DomZero_unblocked(int cpu_id, ObjectDesc * cpuState)
{
	MaybeRunnable(cpuState2thread(cpuState));
	DomZero_addThread(cpu_id, cpuState);
}
static void DomZero_created(int cpu_id, ObjectDesc * cpuState)
{
	/*printf("CPU:%d: DomZero_created %p\n",get_processor_id(), thread); */
	MaybeRunnable(cpuState2thread(cpuState));
	DomZero_addThread(cpu_id, cpuState);
}
static void DomZero_switchTo(int cpu_id, ThreadDesc * thread)
{
	/*printf("CPU:%d: DomZero_switchTo %p\n",get_processor_id(), thread); */
	ASSERTTHREAD(thread);
	MaybeRunnable(thread);
	destroy_switch_to(curthrP(), thread);
}
static void DomZero_blocked(int cpu_id, ObjectDesc * cpuState)
{
	/* do nothing!! */
	ASSERT(cpuState2thread(cpuState)->state != STATE_RUNNABLE);
}

static void DomZero_blockedInPortalCall(int cpu_id, ObjectDesc * cpuState)
{
	/* do nothing!! */
}
static jboolean DomZero_portalCalled(int cpu_id, ObjectDesc * cpuState)
{
	/* do nothing!! */
	MaybeRunnable(cpuState2thread(cpuState));
	//  return JNI_FALSE;
	return JNI_TRUE;	// hand-off
}
static void DomZero_switchedTo(int cpu_id, ObjectDesc * cpuState)
{
	/* do nothing!! */
}
static void DomZero_destroyed(int cpu_id, ObjectDesc * cpuState)
{
	/* do nothing!! */
}

static void DomZero_addThread(int cpu_id, ObjectDesc * cpuState)
{
	ThreadDesc *thread = cpuState2thread(cpuState);
	ASSERT(thread != NULL);
	ASSERTNIR(thread);
	ASSERTLOCKED;

	if (thread->state != STATE_RUNNABLE) {
		sys_panic("append of non-runnable thread  %d.%d (state:%s)", thread->domain->id, thread->id, get_state(thread));
	}
	if (thread == idle_thread) {
		sched_dprintf(1, "CPU%d: append of idle thread ignored\n", get_processor_id());
		return;
	}

	{
		{		/* chekc if t is already in the runqueue */
			ThreadDesc *t = (ThreadDesc *) c_runq[cpu_id].first;
			while (t != NULL) {
				if (t == thread) {
					printf("Thread %p already in runqueue.\n", thread);
					return;
				}
				t = t->nextInRunQueue;
			}
		}

		thread->nextInRunQueue = NULL;
		if (c_runq[cpu_id].last == NULL) {
			c_runq[cpu_id].last = c_runq[cpu_id].first = thread;
		} else {
			c_runq[cpu_id].last->nextInRunQueue = thread;
			c_runq[cpu_id].last = thread;
		}

		check_runq(cpu_id);
	}

	check_runq(cpu_id);
}

void DomZero_dump(int cpu_id)
{
	ThreadDesc *t = (ThreadDesc *) c_runq[cpu_id].first;
	if (t == NULL)
		printf("       none\n");
	else
		while (t != NULL) {
			dumpThreadInfo(t);
			t = t->nextInRunQueue;
		}
}

/*******/

static void check_runq(int cpu_id)
{
	ThreadDesc *t = (ThreadDesc *) c_runq[cpu_id].first;
	int n = 0;
	while (t != NULL) {
		if (t->state != STATE_RUNNABLE) {
			sys_panic("thread %p not runnable");
		}
		t = t->nextInRunQueue;
		n++;
		if (n > 100) {
			dump_runq();
			sys_panic("CYCLE IN RUNQ");

		}
	}
}

#ifdef DEBUG
void check_domain_not_in_runq(DomainDesc * cd)
{
}

int _check_not_in_runq(int cpu_id, ThreadDesc * thread)
{
	ThreadDesc *t = c_runq[cpu_id].first;
	check_runq(cpu_id);
	while (t != NULL) {
		if (t == thread) {
			check_current = 0;
			return -1;
		}
		t = t->nextInRunQueue;
	}
	return 0;
}
#endif


#ifdef CHECK_RUNNABLE_IN_RUNQ
void check_runnable(DomainDesc * domain)
{
}
#endif


#endif				/* JAVASCHEDULER */
