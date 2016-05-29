#include "all.h"
#ifdef DOMZERO_PERF_TEST_PORTAL
/*
 *
 * testDZ Portal
 *
 */

void testDZ_emptyMethod(ObjectDesc * self)
{
	/* do nothing, just return */
}

HLSchedDesc testDZSched = { NULL, NULL, };

jint testDZ_prepareSchedulerMethod_registered_(ObjectDesc * self, ThreadDesc * testThread, ObjectDesc * scheduler)
{
	int index;
	code_t registered_code;

	if (scheduler == NULL) {
		printf("TestDZ::callSchedulerMethod_registered_: invalid parameter: HLS==NULL");
		return JNI_FALSE;
	}
	if (testThread == NULL) {
		printf("TestDZ::callSchedulerMethod_registered_: invalid parameter: worker==NULL");
		return JNI_FALSE;
	}
	ASSERTTHREAD(worker);

	index = findDEPMethodIndex(curdom(), "jx/zero/scheduler/HighLevelScheduler", "registered", "()V");
	testDZSched.registered_code = (code_t) scheduler->vtable[index];
	testDZSched.SchedObj = scheduler;
	testDZSched.SchedThread = testThread;
	return JNI_TRUE;
}

jint testDZ_callSchedulerMethod_registered_(ObjectDesc * self)
{
	if (testDZSched.SchedObj == NULL) {
		printf("call TestDZ::prepareSchedulerMethod_registered_ prior to  TestDZ::callSchedulerMethod_registered_");
		return;
	}

	/* do the test call */
	return call_JAVA_method0(testDZSched.SchedObj, testDZSched.SchedThread, testDZSched.registered_code);
}

MethodInfoDesc testDZperfMethods[] = {
	{"emptyMethod", "", (code_t) testDZ_emptyMethod}
	,
	{"prepareSchedulerMethod_registered_", "",
	 (code_t) testDZ_prepareSchedulerMethod_registered_}
	,
	{"callSchedulerMethod_registered_", "",
	 (code_t) testDZ_callSchedulerMethod_registered_}
	,
};



void init_testdzperf_portal()
{
#ifdef DOMZERO_PERF_TEST_PORTAL
	init_zero_dep_without_thread("jx/zero/TestDZperf", "TestDZperf", testDZperfMethods, sizeof(testDZperfMethods),
				     "<jx/zero/TestDZperf>");
#endif
}

#endif
