#include "all.h"

/*
 * Atomic Variable DEP
 */
ClassDesc *atomicvariableClass = NULL;

void atomicvariable_set(AtomicVariableProxy * self, ObjectDesc * value)
{
	self->value = value;
}

ObjectDesc *atomicvariable_get(AtomicVariableProxy * self)
{
	return self->value;
}

void atomicvariable_atomicUpdateUnblock(AtomicVariableProxy * self, ObjectDesc * value, CPUStateProxy * cpuStateProxy)
{
	//printf("atomicUpdateUnblock: self=%p value=%p cpustate=%p\n", self, value,  cpuStateProxy);
	DISABLE_IRQ;
	self->value = value;

	if (self->listMode == 1) {
		ThreadDesc *t;
		for (t = self->blockedThread; t != NULL; t = t->next)
			threadunblock(t);
		self->blockedThread = NULL;
	} else {
		if (cpuStateProxy != NULL && self->blockedThread != NULL) {
			ThreadDesc *cpuState = cpuState2thread(cpuStateProxy);
			ASSERTTHREAD(cpuState);
#ifdef DEBUG
			if (self->blockedThread != cpuState) {
				if (self->blockedThread == NULL)
					printf("BlockedThread: NULL\n");
				else
					printf("BlockedThread: %d%d.\n", self->blockedThread->domain->id,
					       self->blockedThread->id);
				printf("ThreadToUnblock: %d.%d\n", cpuState->domain->id, cpuState->id);
			}
#endif
			ASSERT(self->blockedThread == cpuState);
			if (cpuState->state == STATE_BLOCKEDUSER) {
				self->blockedThread = NULL;
				threadunblock(cpuState);
			}
		}
	}
	RESTORE_IRQ;
}

void atomicvariable_blockIfEqual(AtomicVariableProxy * self, ObjectDesc * test)
{
	DISABLE_IRQ;

	if (self->value == test) {
		ThreadDesc *thread = curthr();
		if (self->listMode == 1) {
			thread->next = self->blockedThread;
			self->blockedThread = thread;
		} else {
			ASSERT(self->blockedThread == NULL || self->blockedThread == thread);
			self->blockedThread = thread;
		}
		threadblock();
	}
	RESTORE_IRQ;
}

void atomicvariable_blockIfNotEqual(AtomicVariableProxy * self, ObjectDesc * test)
{
	ThreadDesc *next;
	DISABLE_IRQ;

	if (self->value != test) {
		ThreadDesc *thread = curthr();
		if (self->listMode == 1) {
			thread->next = self->blockedThread;
			self->blockedThread = thread;
		} else {
			ASSERT(self->blockedThread == NULL || self->blockedThread == thread);
			self->blockedThread = thread;
		}
		threadblock();
	}

	RESTORE_IRQ;
}

void atomicvariable_activateListMode(AtomicVariableProxy * self)
{
	self->listMode = 1;
}

MethodInfoDesc atomicvariableMethods[] = {
	{"set", "", atomicvariable_set}
	,
	{"get", "", atomicvariable_get}
	,
	{"atomicUpdateUnblock", "", atomicvariable_atomicUpdateUnblock}
	,
	{"blockIfEqual", "", atomicvariable_blockIfEqual}
	,
	{"blockIfNotEqual", "", atomicvariable_blockIfNotEqual}
	,
	{"activateListMode", "", atomicvariable_activateListMode}
	,
};

static jbyte atomicvariableTypeMap[] = { 1 };

void init_atomicvariable_portal()
{
	atomicvariableClass =
	    init_zero_class("jx/zero/AtomicVariable", atomicvariableMethods, sizeof(atomicvariableMethods), 1,
			    atomicvariableTypeMap, "<jx/zero/AtomicVariable>");
}
