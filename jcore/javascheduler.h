#ifndef __JAVASCHEDULER_H
#define __JAVASCHEDULER_H

#include "thread.h"
#include "smp.h"

/* Debug Level from 0 (none) to 4(max) */
#define SCHEDULER_DEBUG_LEVEL 0

#if(SCHEDULER_DEBUG_LEVEL == DEBUG_NO)
#define sched_dprintf(args...)
#else
#define sched_dprintf(dbg_level,args...)   do{ if (dbg_level <= SCHEDULER_DEBUG_LEVEL) {printf (args);} }while(0)
#endif


typedef struct HLSchedDesc_s {
	ObjectDesc *SchedObj;
	ThreadDesc *SchedThread;
	ThreadDesc *SchedActivateThread;
	jboolean maybeRunnable;

	java_method0_t registered_code;

	java_method0_t SchedPreempted_code;
	java_method0_t SchedInterrupted_code;
	jboolean resume;

	java_method0_t activated_code;

	java_method1_t preempted_code;
	java_method1_t interrupted_code;
	java_method1_t yielded_code;
	java_method1_t unblocked_code;
	java_method1_t created_code;

	java_method1_t blocked_code;

	java_method1_t blockedInPortalCall_code;
	java_method1_t portalCalled_code;

	java_method1_t switchedTo_code;
	java_method1_t destroyed_code;

#ifdef ENABLE_GC
	java_method2_t startGC_code;
	java_method1_t GCdestroyed_code;
	java_method1_t GCunblocked_code;
#endif
	java_method0_t dump_code;
} HLSchedDesc;


typedef struct LLSchedDesc_s {
	ObjectDesc *SchedObj;
	ThreadDesc *SchedThread;
	java_method1_t registered_code;

	java_method1_t registerDomain_code;
	java_method1_t unregisterDomain_code;
	java_method2_t setTimeSlice_code;
	java_method0_t activateCurrDomain_code;
	java_method0_t activateNextDomain_code;
	java_method0_t dump_code;
} LLSchedDesc;

typedef struct CpuDesc_s {
	LLSchedDesc LowLevel;
} CpuDesc;

//Variables
extern CpuDesc *CpuInfo[MAX_NR_CPUS];

//Prototypes
void smp_scheduler_init(void);

//void Sched_locked_created(ThreadDesc *thread);
void Sched_unblocked(ThreadDesc * thread);
void Sched_yielded(ThreadDesc * thread);
void Sched_blocked(ThreadDesc * thread);
void Sched_blockedInPortalCall(ThreadDesc * thread);
void Sched_switchedTo(ThreadDesc * thread);
void Sched_destroyed(ThreadDesc * thread);
jboolean Sched_portalCalled(ThreadDesc * thread);

void DomZero_dump(int cpu_id);
void DomZero_activated(int cpu_id);
void DomZero_preempted(int cpu_id, ObjectDesc * thread);
void DomZero_interrupted(int cpu_id, ObjectDesc * thread);

void LLSched_register(DomainDesc * domain, ObjectDesc * new_sched);
HLSchedDesc *createHLSchedDesc(DomainDesc * ObjDomain,
			       ObjectDesc * SchedObj);
void HLSched_register(DomainDesc * domain, HLSchedDesc * new_HLSched);

/* disable IRQs before calling!! */
void destroy_activate_nextDomain(void);
void destroy_activate_nextThread(void);

/* in schedSWITCH.S */
jint switch_to_nextThread(void);
jint switch_to_nextDomain(void);

#ifdef DEBUG
#define check_not_in_runq(thread) _check_not_in_runq(get_processor_id(),thread)
#endif

#endif /*__JAVASCHEDULER_H*/
