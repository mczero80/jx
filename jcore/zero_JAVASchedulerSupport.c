#include "all.h"



/*
 *
 * JAVAschedulerSupport Portal
 *
 */

static void JAVAschedulerSupport_switchTo(ObjectDesc * self, ObjectDesc * cpuState)
{
	ThreadDesc *thread = cpuState2thread(cpuState);
	ASSERTTHREAD(thread);
	/*printf("JAVAschedulerSuppor_switchTo called\n"); */
#ifdef KERNEL
	/* may only be called in interrupt handler */
	icurrent[get_processor_id()] = thread;
#else
	sys_panic("should never be executed (in emulation mode)");
#endif
}

static ObjectDesc *JAVAschedulerSupport_getCPUState(ObjectDesc * self)
{
#ifdef KERNEL
	return thread2CPUState(icurrent[get_processor_id()]);
#else
	sys_panic("should never be executed (in emulation mode)");
	return 0;
#endif
}

static ObjectDesc *JAVAschedulerSupport_getIdleThread(ObjectDesc * self)
{
	return thread2CPUState(__idle_thread[get_processor_id()]);
}

void JAVAschedulerSupport_setTimeslice(ObjectDesc * self, int time)
{
	/*printf("CPU%d: (clock: %d) next interrupt time set to: %d\n",get_processor_id(),read_APIC_clock(),time); */
	DISABLE_IRQ;
#ifdef KERNEL
#   ifdef APIC
	if (apic_found)
		set_APIC_clock(time);
	else
		schedule_counter_init = schedule_counter = time;
#   else
	schedule_counter_init = schedule_counter = time;
#   endif
#else
	{			/* in msec */
		struct itimerval value;
		value.it_interval.tv_sec = value.it_value.tv_sec = time / 1000;
		value.it_interval.tv_usec = value.it_value.tv_usec = (time % 1000) * 1000;
		setitimer(ITIMER_REAL, &value, NULL);
	}
#endif
	RESTORE_IRQ;

}

void JAVAschedulerSupport_storeThreadInfo(ObjectDesc * self, ObjectDesc * cpuState, ObjectDesc * data)
{
	ThreadDesc *thread = cpuState2thread(cpuState);
	ASSERTTHREAD(thread);
	if (thread == NULL)
		return;
	thread->schedInfo = data;
}

ObjectDesc *JAVAschedulerSupport_getThreadInfo(ObjectDesc * self, ObjectDesc * cpuState)
{
	ThreadDesc *thread = cpuState2thread(cpuState);
	ASSERTTHREAD(thread);
	if (thread == NULL)
		return 0;
	return thread->schedInfo;
}

static ObjectDesc *JAVAschedulerSupport_getThreadDomain(ObjectDesc * self, ObjectDesc * cpuState)
{
	ThreadDesc *thread = cpuState2thread(cpuState);
	ASSERTTHREAD(thread);
	if (thread == NULL)
		return NULL;
	return domainDesc2Obj(thread->domain);
}

void JAVAschedulerSupport_dumpThread(ObjectDesc * self, ObjectDesc * cpuState)
{				//ttt
	ThreadDesc *t = cpuState2thread(cpuState);
	dumpThreadInfo(t);
}

MethodInfoDesc JAVAschedulerSupportMethods[] = {
	{"getCurThr", "", (code_t) JAVAschedulerSupport_getCPUState},
	{"switchTo", "", (code_t) JAVAschedulerSupport_switchTo},
	{"getIdleThread", "", (code_t) JAVAschedulerSupport_getIdleThread},
	{"setTimeslice", "", (code_t) JAVAschedulerSupport_setTimeslice},
	{"storeThreadInfo", "",
	 (code_t) JAVAschedulerSupport_storeThreadInfo},
	{"getThreadInfo", "", (code_t) JAVAschedulerSupport_getThreadInfo},
	{"getThreadDomain", "",
	 (code_t) JAVAschedulerSupport_getThreadDomain},
	{"dumpThread", "", (code_t) JAVAschedulerSupport_dumpThread},
};

void init_javaschedulersupport_portal()
{
	init_zero_dep_without_thread("jx/zero/JAVASchedulerSupport", "JAVASchedulerSupport", JAVAschedulerSupportMethods,
				     sizeof(JAVAschedulerSupportMethods), "<jx/zero/JAVASchedulerSupport>");
}
