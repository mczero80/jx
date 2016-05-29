#include "all.h"

/*
 *
 * HLschedulerSupport Portal
 *
 */
static void HLschedulerSupport_activateThread(ObjectDesc * self, ObjectDesc * cpuState)
{
	ThreadDesc *thread = cpuState2thread(cpuState);
	ASSERTTHREAD(thread);
	/*printf(" %p(%s[dom:%s]) is switching to %p(%s)\n",curthr(),curthr()->name,curthr()->domain->domainName,thread,thread->name); */
	if (thread->state != STATE_RUNNABLE && thread->state != STATE_PORTAL_WAIT_FOR_SND)
		sys_panic("%p state != RUNNABLE or PORTAL_WAIT_FOR_SND (%s)", thread, get_state(thread));
#ifdef JAVASCHEDULER
	sched_dprintf(1, "HLschedulerSupport_activateThread called (Thread (%p %s) at ip:%p)\n", thread, thread->name,
		      thread->context[PCB_EIP]);
	CLI;
#ifdef SMP
	thread->curCpuId = get_processor_id();
#endif
	curthr()->state = STATE_AVAILABLE;
	destroy_switch_to(curthrP(), thread);
#else
	sys_panic("should never be executed without JAVASCHEDULER defined");
#endif
}

static jint HLschedulerSupport_activateService(ObjectDesc * self, ObjectDesc * cpuState)
{
	ThreadDesc *thread = cpuState2thread(cpuState);
#if defined(CONT_PORTAL) && defined(JAVASCHEDULER)
	sched_dprintf(1, "HLschedulerSupport_activateService called (Thread is of Dom: %s; scheduled by Dom: %s)\n",
		      thread->domain->domainName, thread->schedulingDomain->domainName);
	ASSERTTHREAD(thread);
	irq_disable();
#ifdef SMP
	thread->curCpuId = get_processor_id();
#endif
	/*printf("thr:%p (%s), linkedthr:%p (%s), state %s\n",thread,thread->name,thread->linkedDEPThr,thread->linkedDEPThr->name,get_state(thread->linkedDEPThr)); */
	if (thread->linkedDEPThr != thread && thread->linkedDEPThr->state == STATE_RUNNABLE) {
//       thread->linkedDEPThr->depSwitchBack = JNI_TRUE; 
		curthr()->state = STATE_AVAILABLE;
		Sched_destroy_switchTo(thread->linkedDEPThr);
	} else
		return JNI_FALSE;
#else
	sys_panic("CONT_PORTAL not defined");
#endif
}

static void HLschedulerSupport_yield(ObjectDesc * self)
{
#ifdef JAVASCHEDULER
	sched_dprintf(2, "CPU%d: HLschedulerSupport_yield called\n", get_processor_id());
	CLI;
	/*curthr()->schedulingDomain->Scheduler[get_processor_id()]->maybeRunnable = JNI_FALSE; */
	curthr()->state = STATE_AVAILABLE;
	destroy_activate_nextDomain();
	sys_panic("should never return");
#else
	sys_panic("HLschedulerSupport_yield: compiled without JAVASCHEDULER support!!");
#endif
}

static void HLschedulerSupport_clearMyRunnableFlag(ObjectDesc * self)
{
#ifdef JAVASCHEDULER
	int cpu_id = get_processor_id();
	curthr()->schedulingDomain->Scheduler[cpu_id]->maybeRunnable = JNI_FALSE;
#endif
}

static jint HLschedulerSupport_getTimeBaseInMicros(ObjectDesc * self)
{
	return 10000;		/* fix this? */
}

static void HLschedulerSupport_setMyTimeslice(ObjectDesc * self, jint time)
{
#ifdef JAVASCHEDULER
	LLSchedDesc *LLS = &CpuInfo[get_processor_id()]->LowLevel;
	call_JAVA_method2(LLS->SchedObj, LLS->SchedThread, LLS->setTimeSlice_code, (long) domainDesc2Obj(curthr()->domain),
			  (long) time);
#else
	sys_panic("compiled without JAVASCHEDULER defined!!");
#endif
}
static void HLschedulerSupport_setTimeslice(ObjectDesc * self, ObjectDesc * domObj, jint time)
{
#ifdef JAVASCHEDULER
	LLSchedDesc *LLS = &CpuInfo[get_processor_id()]->LowLevel;
	call_JAVA_method2(LLS->SchedObj, LLS->SchedThread, LLS->setTimeSlice_code, (long) domObj, (long) time);
#else
	printf("compiled without JAVASCHEDULER defined!!");
	return;
#endif
}

jint LLschedulerSupport_getDomainTimeslice(ObjectDesc * self);
void JAVAschedulerSupport_storeThreadInfo(ObjectDesc * self, ObjectDesc * cpuState, ObjectDesc * data);
ObjectDesc *JAVAschedulerSupport_getThreadInfo(ObjectDesc * self, ObjectDesc * cpuState);
void JAVAschedulerSupport_dumpThread(ObjectDesc * self, ObjectDesc * cpuState);

MethodInfoDesc HLschedulerSupportMethods[] = {
	{"activateThread", "", (code_t) HLschedulerSupport_activateThread},
	{"activateService", "",
	 (code_t) HLschedulerSupport_activateService},
	{"yield", "", (code_t) HLschedulerSupport_yield},
	{"clearMyRunnableFlag", "",
	 (code_t) HLschedulerSupport_clearMyRunnableFlag},
	{"getTimeBaseInMicros", "",
	 (code_t) HLschedulerSupport_getTimeBaseInMicros},
	{"setMyTimeslice", "", (code_t) HLschedulerSupport_setMyTimeslice},
	{"setTimeslice", "", (code_t) HLschedulerSupport_setTimeslice},

	{"storeThreadInfo", "",
	 (code_t) JAVAschedulerSupport_storeThreadInfo},
	{"getThreadInfo", "", (code_t) JAVAschedulerSupport_getThreadInfo},
	{"dumpThread", "", (code_t) JAVAschedulerSupport_dumpThread},

	{"getDomainTimeslice", "",
	 (code_t) LLschedulerSupport_getDomainTimeslice},

};

void init_hlschedulersupport_portal()
{
	init_zero_dep_without_thread("jx/zero/HLSchedulerSupport", "HLSchedulerSupport", HLschedulerSupportMethods,
				     sizeof(HLschedulerSupportMethods), "<jx/zero/HLSchedulerSupport>");
}
