/********************************************************************************
 * DomainZero CPUManager
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#include "all.h"
#include "serialdbg.h"

//#define VERBOSE_UNBLOCK 1

/*
 *
 * CpuManager Portal
 *
 */
jint cpuManager_receive(ObjectDesc * self, ObjectDesc * obj)
{
	exceptionHandler(THROW_RuntimeException);
}

jint cpuManager_yield(ObjectDesc * self)
{
	/* printf("YIELD!!\n"); */
	threadyield();
	//     ASSERTNOCLI1;      
}

void cpuManager_sleep(ObjectDesc * self, jint msec, jint nsec)
{
	sys_panic("SLEEP NO LONGER SUPPORTED. USE jx.timer.SleepManager");
}

jint cpuManager_wait(ThreadDesc * source)
{
	sys_panic("should never be executed");
	return 0;
}

jint cpuManager_notify(ThreadDesc * source)
{
	sys_panic("should never be executed");
	return 0;
}

jint cpuManager_notifyAll(ThreadDesc * source)
{
	sys_panic("should never be executed");
	return 0;
}

void dumpVTable(ObjectDesc * o, u4_t n)
{
	u4_t i;
	for (i = 0; i < n; i++) {
		printf("%p\n", o->vtable[i]);
	}
}

jint cpuManager_dump(ObjectDesc * self, ObjectDesc * msg, ObjectDesc * ref)
{
	char c[128];
	int i = 0;
	ClassDesc *cl;
	c[0] = '\0';
	if (msg != NULL)
		stringToChar(msg, c, sizeof(c));
	//printStackTraceNew("DUMP");
	printf("DUMP %s 0x%lx ", c, ref);
	if (ref == NULL)
		return;
	if ((getObjFlags(ref) & FLAGS_MASK) == OBJFLAGS_MEMORY) {
#if 0
		MemoryProxy *m = (MemoryProxy *) ref;
		printf("   MEMORY: dz=%p mem=%p size=%d valid=%d refcount=%d\n", m->dz, m->mem, m->size, m->dz->valid,
		       m->dz->refcount);
#ifdef DEBUG_MEMORY_CREATION
		if (m->dz->createdBy) {
			printf("     created at : ");
			print_eip_info(m->dz->createdAt);
			printf(", by: %s ", m->dz->createdBy->domainName);
			if (m->dz->createdUsing)
				printf(" using %s", m->dz->createdUsing);
#endif
		} else {
			printf(", unknown creator");
		}
		printf("\n");
		{
#if 0
			DZMemoryProxy *dz;
			printf("--\n");
			dz = m->dz;
			while (dz->prev) {
				printf("  PREV %p owner=%d size=%d valid=%d\n", dz->prev, dz->prevOwner, dz->prev->size,
				       dz->prev->valid);
				dz = dz->prev;
				if (i++ > 20)
					sys_panic("POSSIBLE CIRCULARITY IN MEMORY CHAIN");
			}
			dz = m->dz;
			while (dz->next) {
				printf("  NEXT %p owner=%d size=%d valid=%d\n", dz->next, dz->nextOwner, dz->next->size,
				       dz->next->valid);
				dz = dz->next;
			}
		}
#endif

#endif				/* DEBUG_MEMORY_CREATION */
		{
			int i;
			ClassDesc *cd = obj2ClassDesc(ref);
			ASSERTCLASSDESC(cd);
			/*
			   printf("vtable: \n");
			   for(i=0; i<cd->vtableSize; i++) {
			   if (cd->vtable[i] != NULL) printf("%d %p\n", i, cd->vtable[i]);
			   }
			 */
		}
	} else if ((getObjFlags(ref) & FLAGS_MASK) == OBJFLAGS_PORTAL) {
		printf("   PORTAL: index=%d\n", ((Proxy *) ref)->index);
		//dumpVTable(ref, cl->vtableSize);
	} else if ((getObjFlags(ref) & FLAGS_MASK) == OBJFLAGS_OBJECT) {
		cl = obj2ClassDesc(ref);
		printf("     INSTANCE of class: %s\n", cl->name);
	} else if ((getObjFlags(ref) & FLAGS_MASK) == OBJFLAGS_CAS) {
		printf("     CAS\n");
	} else if ((getObjFlags(ref) & FLAGS_MASK) == OBJFLAGS_SERVICE) {
		DEPDesc *s = (DEPDesc *) ref;
		printf("     Service: interface=%s\n", s->interface->name);
	} else if ((getObjFlags(ref) & FLAGS_MASK) == OBJFLAGS_SERVICE_POOL) {
		printf("     Servicepool\n");
#if 0
	} else if (getObjFlags(ref) == OBJFLAGS_EXTERNAL_CPUSTATE) {
		printf("     CPUSTATE thread %d.%d\n", TID(cpuState2thread(ref)));
		//printStackTraceNew("CPUSTATE");
#endif
	} else {
		printf("     unknown object type. flags=(%p)\n", getObjFlags(ref));
	}
	return 0;
}

jint cpuManager_switchTo(ObjectDesc * self, ObjectDesc * cpuState)
{
	sys_panic("switchTo should not be called");
}

ObjectDesc *cpuManager_getCPUState(ObjectDesc * self)
{
	/*  return allocCPUStateProxyInDomain(curdom(), cpuStateClass, curthr()); */
	return thread2CPUState(curthr());
}

void cpuManager_block(ObjectDesc * self)
{
	//printf("BLOCK %p\n",curthr());
#ifdef KERNEL
	locked_threadblock();
	return;
#else				/* KERNEL */
	threadblock();
	return;
#endif
}

void cpuManager_blockIfNotUnblocked(ObjectDesc * self)
{
	DISABLE_IRQ;
	if (!curthr()->unblockedWithoutBeingBlocked) {
		threadblock();
	}
	RESTORE_IRQ;
}

void cpuManager_clearUnblockFlag(ObjectDesc * self)
{
	curthr()->unblockedWithoutBeingBlocked = 0;
}

jint cpuManager_waitUntilBlocked(ObjectDesc * self, CPUStateProxy * cpuStateProxy)
{
	/*  ThreadDesc *cpuState = cpuStateProxy->cpuState; */
	ThreadDesc *cpuState = cpuState2thread(cpuStateProxy);

#ifdef KERNEL
	ASSERTTHREAD(cpuState);
	DISABLE_IRQ;
	while (cpuState->state != STATE_BLOCKEDUSER) {
		threadyield();
	}
	/*printf("Thread %p is now BLOCKED\n", cpuState); */
	RESTORE_IRQ;
#else
	sys_panic("waituntilblocked should not be called");
#endif
}

void cpuManager_join(ObjectDesc * self, CPUStateProxy * cpuStateProxy)
{
	/*ThreadDesc *cpuState = cpuStateProxy->cpuState; */
	ThreadDesc *cpuState = cpuState2thread(cpuStateProxy);
#ifdef KERNEL
	ASSERTTHREAD(cpuState);
	DISABLE_IRQ;
	while (cpuState->state != STATE_ZOMBIE) {
		threadyield();
	}
	RESTORE_IRQ;
#else
	sys_panic("cpuManager_join should not be called");
#endif
}

#ifdef SMP
static inline jboolean _cpuManager_unblock(ThreadDesc * cpuState);
static inline jboolean remote_unblock(ThreadDesc * cpuState)
{
	int result;
	sched_dprintf(DEBUG_2, "CPU%d: unblocking Thread:%p on remote CPU %d\n", get_processor_id(), cpuState,
		      cpuState->curCpuId);
	smp_call_function(cpuState->curCpuId, _cpuManager_unblock, cpuState, 1, &result);
	return (jboolean) result;
}
#endif
/* needs only one Parameter */
static inline jboolean _cpuManager_unblock(ThreadDesc * cpuState)
{
	jboolean ret;
#ifdef KERNEL
#  ifdef SMP
	if (cpuState->curCpuId != get_processor_id())
		return remote_unblock(cpuState);
	else
#  endif
	{
		//jint* base = (u4_t*)&cpuState-2;
		DISABLE_IRQ;
		/*printf("CPU%d: unblock %p\n",get_processor_id(), cpuState); */
		if (cpuState->state != STATE_BLOCKEDUSER) {
#ifdef DEBUG
/*
			printf("CPU%d: CPUManager::unblock: Thread %p is in state %d (%s)\n", get_processor_id(), cpuState,
			       cpuState->state, get_state(cpuState));
*/
#endif
			//printStackTrace("STACK: ", curthr(), base);
			ret = JNI_FALSE;
		} else {
			threadunblock(cpuState);
			ret = JNI_TRUE;
		}
		RESTORE_IRQ;
		return ret;
	}
#else
	DISABLE_IRQ;
	if (cpuState->state == STATE_BLOCKEDUSER) {
		locked_threadunblock(cpuState);
		ret = JNI_TRUE;
	} else {
#ifdef DEBUG
/*
		printf("CPUManager::unblock: Thread %p is in state %d (%s)\n", cpuState, cpuState->state, get_state(cpuState));
*/
#endif
		ret = JNI_FALSE;
	}

	RESTORE_IRQ;
	return ret;

#endif
}

jboolean cpuManager_unblock(ObjectDesc * self, CPUStateProxy * cpuStateProxy)
{
	jboolean ret;
	/*ThreadDesc *cpuState = cpuStateProxy->cpuState; */
	ThreadDesc *cpuState;
	if (cpuStateProxy == NULL)
		exceptionHandler(THROW_RuntimeException);

	DISABLE_IRQ;		/* because we access data in another domain (the TCB) */

	cpuState = cpuState2thread(cpuStateProxy);
	if (cpuState == NULL) {
		ret = JNI_FALSE;
		goto finish;
	}
	ASSERTTHREAD(cpuState);
#ifdef VERBOSE_UNBLOCK
	printf("UNBLOCK %d.%d by %d.%d\n", TID(cpuState), TID(curthr()));
	/*printf("CPU%d: unblock %p\n",get_processor_id(), cpuState); */
#endif
	if (cpuState->state != STATE_BLOCKEDUSER) {
#ifdef DEBUG
/*		printf("CPU%d: CPUManager::unblock: Thread %p is in state %d (%s)\n", get_processor_id(), cpuState,
		       cpuState->state, get_state(cpuState));
*/
#endif
		//printStackTrace("STACK: ", curthr(), base);
		cpuState->unblockedWithoutBeingBlocked = 1;
		ret = JNI_FALSE;
	} else {
		threadunblock(cpuState);
		ret = JNI_TRUE;
	}
      finish:
	RESTORE_IRQ;
	return ret;

}

//static ClassDesc *jx_zero_CPUState=NULL;

static void start_thread_using_entry(void *dummy)
{
	ObjectDesc *entry = curthr()->entry;
	//printf("start thread\n");
#ifndef KERNEL
	enable_irq();
#endif
	executeInterface(curdom(), "jx/zero/ThreadEntry", "run", "()V", entry, 0, 0);
}

ObjectDesc *cpuManager_createCPUState(ObjectDesc * self, ObjectDesc * entry)
{
	ThreadDesc *thread;
	DomainDesc *sourceDomain;
	sourceDomain = curdom();

	DISABLE_IRQ;
	thread =
	    createThreadInMem(sourceDomain, start_thread_using_entry, NULL, entry, 4096, STATE_INIT, SCHED_CREATETHREAD_NORUNQ);
	RESTORE_IRQ;
	return thread2CPUState(thread);
}

jboolean cpuManager_start(ObjectDesc * self, CPUStateProxy * cpuStateProxy)
{
	ThreadDesc *cpuState = cpuState2thread(cpuStateProxy);
	jboolean result = JNI_TRUE;

	ASSERTTHREAD(cpuState);
	DISABLE_IRQ;
	if (cpuState->state != STATE_INIT) {
		printf("Start: Thread %p is in state %d (%s)\n", cpuState, cpuState->state, get_state(cpuState));
		result = JNI_FALSE;	/*sys_panic("state != STATE_INIT!"); */
	} else {
		cpuState->state = STATE_RUNNABLE;
#ifdef NEW_SCHED
		Sched_unblock(cpuState);
#else
		Sched_created(cpuState, SCHED_CREATETHREAD_DEFAULT);
#endif
	}
	RESTORE_IRQ;
	return result;
}

void cpuManager_printStackTrace(ObjectDesc * self)
{
	u4_t *base = (u4_t *) & self - 2;
	printStackTrace("STACK: ", curthr(), base);
}

extern ClassDesc *atomicvariableClass;
extern ClassDesc *casClass;
extern ClassDesc *credentialvariableClass;

ObjectDesc *cpuManager_getAtomicVariable(ObjectDesc * self)
{
	return allocAtomicVariableProxyInDomain(curdom(), atomicvariableClass);
}

ObjectDesc *cpuManager_getCAS(ObjectDesc * self, ObjectDesc * classNameObj, ObjectDesc * fieldNameObj)
{
	CASProxy *p;
	char value[128];
	ClassDesc *c;
	u4_t o;
	int i;
	/* TODO: check access permissions to class and field ! */
	if (classNameObj == NULL || fieldNameObj == NULL)
		exceptionHandlerInternal("classname or fieldname == null");
	stringToChar(classNameObj, value, sizeof(value));
	for (i = 0; i < strlen(value); i++) {
		if (value[i] == '.')
			value[i] = '/';
	}
	c = findClassDesc(value);
	if (c == NULL)
		exceptionHandlerInternal("no such class");
	stringToChar(fieldNameObj, value, sizeof(value));
	o = findFieldOffset(c, value);
	if (o == -1)
		exceptionHandlerInternal("no such field");
	p = allocCASProxyInDomain(curdom(), casClass, o);
	return p;
}

void cpuManager_setThreadName(ObjectDesc * self, ObjectDesc * name)
{
	char value[128];
	if (name == NULL)
		return;
	stringToChar(name, value, sizeof(value));

	DISABLE_IRQ;
	setThreadName(curthr(), value, "");
	RESTORE_IRQ;
}

void cpuManager_attachToThread(ObjectDesc * self, ObjectDesc * portalParameter)
{
	curthr()->portalParameter = portalParameter;
}

ObjectDesc *cpuManager_getAttachedObject(ObjectDesc * self)
{
	return curthr()->portalParameter;
}

ObjectDesc *cpuManager_getCredential(ObjectDesc * self)
{
	return allocCredentialProxyInDomain(curdom(), credentialvariableClass, curdom()->id);
}

#ifdef EVENT_LOG
//EventLog events[MAX_EVENTS];
//EventLog *events;
EventLog *events = NULL;
u4_t n_events = 0;
jint n_event_types = 1;
char eventTypes[MAX_EVENT_TYPES][MAX_EVENT_TYPE_SIZE];
#endif

void cpuManager_recordEvent(ObjectDesc * self, jint nr)
{
#ifdef EVENT_LOG
	//printf("recordEvent: %d\n", nr);
#   ifdef DEBUG
	if (nr == 0)
		printf("warning: event 0 is logged\n");
#   endif
	RECORD_EVENT(nr);
#endif
}

void cpuManager_recordEventWithInfo(ObjectDesc * self, jint nr, jint info)
{
#ifdef EVENT_LOG
	//printf("recordEventWithInfo: %d %d\n", nr, info);
#   ifdef DEBUG
	if (nr == 0)
		printf("warning: event 0 is logged\n");
#   endif
	RECORD_EVENT_INFO(nr, info);
#endif
}

#ifdef EVENT_LOG
jint cpuManager_createNewEventID(char *label)
{
	if (n_event_types == MAX_EVENT_TYPES)
		return -1;
	strncpy(eventTypes[n_event_types], label, sizeof(eventTypes[n_event_types]));
	return n_event_types++;
}
#endif

jint createNewEvent(char *label)
{
#ifdef EVENT_LOG
	if (n_event_types == MAX_EVENT_TYPES)
		return -1;
	strncpy(eventTypes[n_event_types], label, MAX_EVENT_TYPE_SIZE - 1);
	eventTypes[n_event_types][MAX_EVENT_TYPE_SIZE - 1] = '\0';
	return n_event_types++;
#else
	return -1;
#endif
}

jint cpuManager_createNewEvent(ObjectDesc * self, ObjectDesc * label)
{
#ifdef EVENT_LOG
	if (n_event_types == MAX_EVENT_TYPES)
		return -1;
	stringToChar(label, eventTypes[n_event_types],
		     /*MAX_EVENT_TYPE_SIZE */
		     sizeof(eventTypes[n_event_types]));
	return n_event_types++;
#else
	return -1;
#endif
}

ObjectDesc *cpuManager_getClass(ObjectDesc * self, ObjectDesc * nameObj)
{
	char name[128];
	Class *cl;
	int i;
	ObjectDesc *vmclassObj;
	if (nameObj == NULL)
		return NULL;
	stringToChar(nameObj, name, sizeof(name));
	for (i = 0; i < strlen(name); i++) {
		if (name[i] == '.')
			name[i] = '/';
	}
	cl = findClass(curdom(), name);
	if (cl == NULL)
		return NULL;
	vmclassObj = class2Obj(cl);
#ifdef DEBUG
	{
		u4_t flags;
		flags = getObjFlags(vmclassObj) & FLAGS_MASK;
		ASSERT(flags == OBJFLAGS_EXTERNAL_CLASS);
		printf("getClass %s -> %p\n", name, vmclassObj);
	}
#endif
	return vmclassObj;
}

ObjectDesc *cpuManager_getVMClass(ObjectDesc * self, ObjectDesc * obj)
{
	ClassDesc *c = obj2ClassDesc(obj);
	Class *cl = classDesc2Class(curdom(), c);
	ObjectDesc *vmclassObj = class2Obj(cl);
#ifdef DEBUG
	{
		u4_t flags;
		flags = getObjFlags(vmclassObj) & FLAGS_MASK;
		ASSERT(flags == OBJFLAGS_EXTERNAL_CLASS);
	}
#endif
	return vmclassObj;
}

VMObjectProxy *cpuManager_getVMObject(ObjectDesc * self)
{
	return allocVMObjectProxyInDomain(curdom());
}

void cpuManager_assertInterruptEnabled(ObjectDesc * self)
{
#ifdef KERNEL
	ASSERT((getEFlags() & 0x00000200) != 0);
#else
	sigset_t set, oldset;
	sigemptyset(&set);
	sigprocmask(SIG_BLOCK, &set, &oldset);
	if (sigismember(&oldset, SIGALRM))
		sys_panic("INTERRUPTS ARE *NOT* ENABLED");
#endif
}

void cpuManager_executeClassConstructors(ObjectDesc * self, jint id)
{
	LibDesc *lib = (LibDesc *) id;	// HACK
	SharedLibDesc *sharedLib = lib->sharedLib;
	int i;
	for (i = 0; i < sharedLib->numberOfNeededLibs; i++) {
		LibDesc *l = sharedLib2Lib(curdom(), sharedLib->neededLibs[i]);
		ASSERT(l);
		if (!l->initialized)
			callClassConstructors(curdom(), l);
	}
	callClassConstructors(curdom(), lib);
}

jint cpuManager_inheritServiceThread(ObjectDesc * self)
{
	return 0;
}

void cpuManager_reboot(ObjectDesc * self)
{
	exit(0);
}

jint cpuManager_getStackDepth(ObjectDesc * self)
{
	ThreadDesc *thread = curthr();
	u4_t n = 0;
	u4_t *eip, *ebp;
	u4_t *sp = (u4_t *) & self - 2;	/* stackpointer */
	u4_t bytecodePos, i;
	MethodDesc *method;
	ClassDesc *classInfo;
	while (sp > thread->stack && sp < thread->stackTop) {
		ebp = (u4_t *) * sp++;
		eip = (u4_t *) * sp++;
		//printf("%d %p %p/n", n, sp, eip);
/*
		if (findMethodAtAddrInDomain(curdom(), (char *) eip, &method, &classInfo, &bytecodePos, &i) != 0)
			break;
*/
		sp = ebp;
		n++;
	}
	return n;
}

ObjectDesc *cpuManager_getStackFrameClassName(ObjectDesc * self, jint depth)
{
	ThreadDesc *thread = curthr();
	u4_t n = 0;
	u4_t *eip, *ebp;
	u4_t *sp = (u4_t *) & self - 2;	/* stackpointer */
	u4_t bytecodePos, i;
	MethodDesc *method;
	ClassDesc *classInfo;
	while (n <= depth && sp > thread->stack && sp < thread->stackTop) {
		ebp = (u4_t *) * sp++;
		eip = (u4_t *) * sp++;
		sp = ebp;
		n++;
	}
	if (findMethodAtAddrInDomain(curdom(), (char *) eip, &method, &classInfo, &bytecodePos, &i) == 0) {
		return newString(curdom(), classInfo->name);
	} else {
		return newString(curdom(), "core:");
	}
}

ObjectDesc *cpuManager_getStackFrameMethodName(ObjectDesc * self, jint depth)
{
	ThreadDesc *thread = curthr();
	u4_t n = 0;
	u4_t *eip, *ebp;
	u4_t *sp = (u4_t *) & self - 2;	/* stackpointer */
	u4_t bytecodePos, i;
	MethodDesc *method;
	ClassDesc *classInfo;
	while (n <= depth && sp > thread->stack && sp < thread->stackTop) {
		ebp = (u4_t *) * sp++;
		eip = (u4_t *) * sp++;
		sp = ebp;
		n++;
	}
	if (findMethodAtAddrInDomain(curdom(), (char *) eip, &method, &classInfo, &bytecodePos, &i) == 0) {
		return newString(curdom(), method->name);
	} else {
		char *cname = findCoreSymbol((jint) eip);
		if (cname != NULL)
			return newString(curdom(), cname);

		return NULL;
	}
}

jint cpuManager_getStackFrameLine(ObjectDesc * self, jint depth)
{
	ThreadDesc *thread = curthr();
	u4_t n = 0;
	u4_t *eip, *ebp;
	u4_t *sp = (u4_t *) & self - 2;	/* stackpointer */
	u4_t bytecodePos, i;
	MethodDesc *method;
	ClassDesc *classInfo;
	while (n <= depth && sp > thread->stack && sp < thread->stackTop) {
		ebp = (u4_t *) * sp++;
		eip = (u4_t *) * sp++;
		sp = ebp;
		n++;
	}
	if (findMethodAtAddrInDomain(curdom(), (char *) eip, &method, &classInfo, &bytecodePos, &i) == 0) {
		return i;
	} else {
		return -1;
	}
}

jint cpuManager_getStackFrameBytecode(ObjectDesc * self, jint depth)
{
	ThreadDesc *thread = curthr();
	u4_t n = 0;
	u4_t *eip, *ebp;
	u4_t *sp = (u4_t *) & self - 2;	/* stackpointer */
	u4_t bytecodePos, i;
	MethodDesc *method;
	ClassDesc *classInfo;
	while (n <= depth && sp > thread->stack && sp < thread->stackTop) {
		ebp = (u4_t *) * sp++;
		eip = (u4_t *) * sp++;
		sp = ebp;
		n++;
	}
	if (findMethodAtAddrInDomain(curdom(), (char *) eip, &method, &classInfo, &bytecodePos, &i) == 0) {
		return bytecodePos;
	} else {
		return -1;
	}
}

jint cpuManager_inhibitScheduling(ObjectDesc * self)
{
}
jint cpuManager_allowScheduling(ObjectDesc * self)
{
}

MethodInfoDesc cpuManagerMethods[] = {
	{
	 "receive", "", (code_t) cpuManager_receive}
	, {
	   "yield", "", (code_t) cpuManager_yield}
	, {
	   "sleep", "", (code_t) cpuManager_sleep}
	, {
	   "wait", "", (code_t) cpuManager_wait}
	, {
	   "notify", "", (code_t) cpuManager_notify}
	, {
	   "notifyAll", "", (code_t) cpuManager_notifyAll}
	, {
	   "dump", "", (code_t) cpuManager_dump}
	, {
	   "switchTo", "", (code_t) cpuManager_switchTo}
	, {
	   "getCPUState", "", (code_t) cpuManager_getCPUState}
	, {
	   "block", "", (code_t) cpuManager_block}
	, {
	   "blockIfNotUnblocked", "", (code_t) cpuManager_blockIfNotUnblocked}
	, {
	   "clearUnblockFlag", "", (code_t) cpuManager_clearUnblockFlag}
	, {
	   "join", "", (code_t) cpuManager_join}
	, {
	   "waitUntilBlocked", "", (code_t) cpuManager_waitUntilBlocked}
	, {
	   "unblock", "", (code_t) cpuManager_unblock}
	, {
	   "createCPUState", "", (code_t) cpuManager_createCPUState}
	, {
	   "start", "", (code_t) cpuManager_start}
	, {
	   "printStackTrace", "", (code_t) cpuManager_printStackTrace}
	, {
	   "getAtomicVariable", "", (code_t) cpuManager_getAtomicVariable}
	, {
	   "setThreadName", "", (code_t) cpuManager_setThreadName}
	, {
	   "attachToThread", "", (code_t) cpuManager_attachToThread}
	, {
	   "getAttachedObject", "", (code_t) cpuManager_getAttachedObject}
	, {
	   "getCredential", "", (code_t) cpuManager_getCredential}
	, {
	   "createNewEvent", "", (code_t) cpuManager_createNewEvent}
	, {
	   "recordEvent", "", (code_t) cpuManager_recordEvent}
	, {
	   "recordEventWithInfo", "", (code_t) cpuManager_recordEventWithInfo}
	, {
	   "getCAS", "", (code_t) cpuManager_getCAS}
	, {
	   "getClass", "", (code_t) cpuManager_getClass}
	, {
	   "getVMClass", "", (code_t) cpuManager_getVMClass}
	, {
	   "getVMObject", "", (code_t) cpuManager_getVMObject}
	, {
	   "assertInterruptEnabled", "", (code_t) cpuManager_assertInterruptEnabled}
	, {
	   "executeClassConstructors", "", (code_t) cpuManager_executeClassConstructors}
	, {
	   "inheritServiceThread", "", (code_t) cpuManager_inheritServiceThread}
	, {
	   "reboot", "", (code_t) cpuManager_reboot}
	, {
	   "getStackDepth", "", (code_t) cpuManager_getStackDepth}
	, {
	   "getStackFrameClassName", "", (code_t) cpuManager_getStackFrameClassName}
	, {
	   "getStackFrameMethodName", "", (code_t) cpuManager_getStackFrameMethodName}
	, {
	   "getStackFrameLine", "", (code_t) cpuManager_getStackFrameLine}
	, {
	   "getStackFrameBytecode", "", (code_t) cpuManager_getStackFrameBytecode}
	, {
	   "inhibitScheduling", "", (code_t) cpuManager_inhibitScheduling}
	, {
	   "allowScheduling", "", (code_t) cpuManager_allowScheduling}
	,
};
void init_cpumanager_portal()
{
	init_zero_dep_without_thread("jx/zero/CPUManager", "CPUManager", cpuManagerMethods, sizeof(cpuManagerMethods),
				     "<jx/zero/CPUManager>");
}
