#include "all.h"

extern int disable_threadswitching;


/*
 *
 * Scheduler Portal
 *
 */
static jboolean scheduler_disableThreadSwitching(ObjectDesc * self)
{
#ifdef JAVASCHEDULER
	sys_panic("scheduler_disableThreadSwitching not implemented for Java scheduler");
#else
	disable_threadswitching = 1;
	return JNI_TRUE;
#endif
}
static void scheduler_enableThreadSwitching(ObjectDesc * self)
{
	disable_threadswitching = 0;
}

static void scheduler_blockAndEnableThreadSwitching(ObjectDesc * self)
{
	DISABLE_IRQ;
	disable_threadswitching = 0;
	threadblock();
	RESTORE_IRQ;
}

MethodInfoDesc schedulerMethods[] = {
	{"disableThreadSwitching", "",
	 (code_t) scheduler_disableThreadSwitching},
	{"enableThreadSwitching", "",
	 (code_t) scheduler_enableThreadSwitching},
	{"blockAndEnableThreadSwitching", "",
	 (code_t) scheduler_blockAndEnableThreadSwitching},
};

void init_scheduler_portal()
{
	init_zero_dep_without_thread("jx/zero/Scheduler", "Scheduler", schedulerMethods, sizeof(schedulerMethods),
				     "<jx/zero/Scheduler>");
}
