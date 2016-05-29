#include "all.h"

extern ClassDesc *cpuStateClass;
extern ClassDesc *stackClass;
extern ClassDesc *domainClass;
extern ClassDesc *domainZeroClass;

static jint cpuState_getState(ObjectDesc * self)
{
	ThreadDesc *thread = cpuState2thread(self);
	ASSERTTHREAD(thread);
	return thread->state;
}
static jboolean cpuState_isPortalThread(ObjectDesc * self)
{
	ThreadDesc *thread = cpuState2thread(self);
	ASSERTTHREAD(thread);
	return thread->isPortalThread;
}
static ObjectDesc *cpuState_setNext(ObjectDesc * self, ObjectDesc * next)
{
	ObjectDesc *result;
	ThreadDesc *thread = cpuState2thread(self);
	ASSERTTHREAD(thread);
	if (thread == NULL)
		return NULL;
	result = thread2CPUState(thread->next);
	ASSERTTHREAD(thread->next);

	thread->next = cpuState2thread(next);
	ASSERTTHREAD(thread->next);
	return result;
}
static ObjectDesc *cpuState_getNext(ObjectDesc * self)
{
	ObjectDesc *result;
	ThreadDesc *thread = cpuState2thread(self);
	ASSERTTHREAD(thread);
	if (thread == NULL)
		return NULL;
	result = thread2CPUState(thread->next);
	ASSERTTHREAD(thread->next);
	return result;
}

MethodInfoDesc cpuStateMethods[] = {
	{"getState", "", (code_t) cpuState_getState}
	,
	{"isPortalThread", "", (code_t) cpuState_isPortalThread}
	,
	{"setNext", "", (code_t) cpuState_setNext}
	,
	{"getNext", "", (code_t) cpuState_getNext}
	,
};

static jbyte cpuStateTypeMap[] = { 0, 0 };

MethodInfoDesc stackMethods[] = {
};

static jbyte stackTypeMap[] = { 0, 0 };

void init_cpustate_portal()
{
	cpuStateClass =
	    init_zero_class("jx/zero/CPUState", cpuStateMethods, sizeof(cpuStateMethods), 1, cpuStateTypeMap,
			    "<jx/zero/CPUState>");
#ifdef STACK_ON_HEAP
	stackClass = init_zero_class("jx/zero/Stack", stackMethods, sizeof(stackMethods), 1, stackTypeMap, "<jx/zero/Stack>");
#endif
}
