#include "all.h"

/*
 * Profile DEP
 */

void profiler_restart(ObjectDesc * self)
{
#ifdef PROFILE
	printf("profiler restart called\n");
	//profile_shell("Test","clear");
#else
	sys_panic("no profiling support");
#endif
}

void profiler_start_calibration(ObjectDesc * self)
{
#ifdef PROFILE
	ThreadDesc *thr;

	for (thr = curdom()->threads; thr != NULL; thr = thr->nextInDomain) {
#ifdef USE_QMAGIC
		if (thr->magic != MAGIC_THREAD)
			continue;
#endif
		if (thr->profile != NULL)
			clear_hash(thr->profile);
	}

	profile_drift_in = 0;
	profile_drift_out = 0;
#endif
}

jint profiler_end_calibration(ObjectDesc * self, jint time_empty_method, jint time_without_profiling, jint time_with_profiling)
{
#ifdef PROFILE
	ThreadDesc *thr;
	jint result = JNI_TRUE;

	printf("CALIBRATION: %d %d %d\n", time_empty_method, time_without_profiling, time_with_profiling);

	if (time_empty_method > 0) {
		profile_drift_in += time_empty_method;
	} else {
		profile_drift_in -= time_empty_method;
	}

	if ((profile_drift_in > 1000) || (profile_drift_in < 0)) {
		profile_drift_in = 0;
		result = JNI_FALSE;
	}

	profile_drift_out += (time_with_profiling - time_without_profiling) - profile_drift_in;

	if ((profile_drift_out > 3000) || (profile_drift_out < 0)) {
		profile_drift_out = 0;
		result = JNI_FALSE;
	}

	printf("CALIBRATION: drift in: %lu out: %lu \n", profile_drift_in, profile_drift_out);

	for (thr = curdom()->threads; thr != NULL; thr = thr->nextInDomain) {
#ifdef USE_QMAGIC
		if (thr->magic != MAGIC_THREAD)
			continue;
#endif
		if (thr->profile != NULL)
			clear_hash(thr->profile);
	}

	return result;
#else
	return JNI_FALSE;
#endif
}

jint profiler_getAverageCyclesOfMethod(ObjectDesc * self, ObjectDesc * methodName)
{
#ifdef PROFILE
	char method_name1[256];
	char method_name2[256];
	ProfileEntry *p_lst, *p_entry;
	DomainDesc *domain;
	ThreadDesc *thr;
	Class *class;
	jint result;
	int len;

	result = -1;
	domain = curdom();
	stringToChar(methodName, method_name1, sizeof(method_name1));

	p_lst = NULL;
	for (thr = domain->threads; thr != NULL; thr = thr->nextInDomain) {
#ifdef USE_QMAGIC
		if (thr->magic != MAGIC_THREAD)
			break;
#endif
		if (thr->profile == NULL) {
			//printf("Thread 0x%lx has no profiling data \n",(long)thr);
			continue;
		}
		if ((p_lst = create_entry_lst(p_lst, thr->profile)) == NULL) {
			//printf("Thread 0x%lx no profiling data found \n",(long)thr);
			continue;
		}
	}

	len = strlen(method_name1);
	for (p_entry = p_lst; p_entry != NULL; p_entry = p_entry->next) {

		if (findClassForMethod(p_entry->callee, &class))
			continue;

		ASSERTCLASS(class);

		if (strncmp
		    (method_name1, methodName2str(class->classDesc, p_entry->callee, method_name2, sizeof(method_name2)),
		     len) == 0) {
			result = (jint) (p_entry->time / p_entry->count);
			break;
		}
	}

	free_entry_lst(p_lst);

	return result;
#endif
}

void profiler_shell(ObjectDesc * self)
{
#ifdef KERNEL
	DISABLE_IRQ;
#endif
#ifdef PROFILE
	sys_panic("profiler dump called\nuse monitor\n");
	//  profile_shell("Test",NULL);
#else
	sys_panic("no profiling support");
#endif
#ifdef KERNEL
	RESTORE_IRQ;
#endif
}

#ifdef PROFILE_SAMPLE
extern int do_sampling;
#endif

void profiler_startSampling(ObjectDesc * self)
{
#ifdef PROFILE_SAMPLE
	//printStackTraceNew("STARTSAMPLING");
	//  printf("Start Sampling\n");
	do_sampling = 1;
#endif
}

void profiler_stopSampling(ObjectDesc * self)
{
#ifdef PROFILE_SAMPLE
	//  printf("Stop Sampling\n");
	do_sampling = 0;
#endif
}

jboolean profiler_isSampling(ObjectDesc * self)
{
#ifdef PROFILE_SAMPLE
	return do_sampling;
#endif
}

MethodInfoDesc profilerMethods[] = {
	{"restart", "", profiler_restart}
	,
	{"shell", "", profiler_shell}
	,
	{"startCalibration", "", profiler_start_calibration}
	,
	{"endCalibration", "", profiler_end_calibration}
	,
	{"getAverageCyclesOfMethod", "", profiler_getAverageCyclesOfMethod}
	,
	{"startSampling", "", profiler_startSampling}
	,
	{"stopSampling", "", profiler_stopSampling}
	,
	{"isSampling", "", profiler_isSampling}
	,
};


void init_profiler_portal()
{
	init_zero_dep_without_thread("jx/zero/Profiler", "Profiler", profilerMethods, sizeof(profilerMethods),
				     "<jx/zero/Profiler>");
}
