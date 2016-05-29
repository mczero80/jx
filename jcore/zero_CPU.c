#include "all.h"

/*
 * CPU DEP
 */


ClassDesc *cpuClass;

static void cpu_load(ObjectDesc * self, ObjectDesc * oldState, ObjectDesc * newState)
{
#if 0
	ThreadDesc *o, *n;
	if (newState == NULL) {
		printf("newState=null\n");
		return;
	}
	if (oldState == NULL) {
		printf("oldState=null\n");
		return;
	}
	o = oldState->data;
	n = newState->data;

	threadswitchto(o, n);
#endif
	sys_panic("cpuload not impl.");
}

static void cpu_save(ObjectDesc * self, ObjectDesc * state)
{
	sys_panic("cpu::save not impl.");
}

static void cpu_dump(ObjectDesc * self, ObjectDesc * state)
{
#if 0
	ThreadDesc *s;
	if (state == NULL) {
		printf("state=null\n");
		return;
	}
	s = state->data;
	printf("EIP:    %08lx\n", s->context[PCB_EIP]);
	printf("ESP:    %08lx\n", s->context[PCB_ESP]);
	printf("EFLAGS: %08lx\n", s->context[PCB_EFLAGS]);
#endif
	sys_panic("cpu::dump not impl.");
}

static jint cpu_getID(CPUDesc * self)
{
	CPUDesc *cpu = obj2cpuDesc(self);
	if (self == NULL)
		return -1;
	ASSERTCPU(cpu);
	return cpu->cpu_id;
}

static ObjectDesc *cpu_toString(ObjectDesc * self)
{
	CPUDesc *cpu = obj2cpuDesc(self);
#if 0
	char dummy[3] = { 0, 0, 0 };
	int cpu_id = cpu->cpu_id;

	dummy[0] = ((cpu_id / 10) % 10) + 48;
	if (dummy[0] == 48 && (cpu_id / 10) == 0)
		dummy[0] = (cpu_id % 10) + 48;
	else
		dummy[1] = (cpu_id % 10) + 48;
#else
	char dummy[30];
	ASSERTCPU(cpu);
	sprintnum(dummy, cpu->cpu_id, 10);
#endif
	return newString(curdom(), dummy);
}

MethodInfoDesc cpuMethods[] = {
	{"load", "", (code_t) cpu_load}
	,
	{"save", "", (code_t) cpu_save}
	,
	{"dump", "", (code_t) cpu_dump}
	,
	{"getID", "", (code_t) cpu_getID}
	,
	{"toString", "", (code_t) cpu_toString}
	,
};

static jbyte cpuTypeMap[] = { 0 };

void init_cpu_portal()
{
	cpuClass =
	    init_zero_class("jx/zero/CPU", cpuMethods, sizeof(cpuMethods), (sizeof(int)) >> 2, cpuTypeMap, "<jx/zero/CPU>");
}
