/********************************************************************************
 * Thread management (debugging code)
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#include "all.h"

char *get_IDString(ThreadDesc * t)
{
#ifdef KERNEL
	ASSERTTHREAD(t);
	printf(" [*=(%d.%d)] ", t->domain->id, t->id);
	return "*";
#else
	static char buffer[10];
	ASSERTTHREAD(t);
	snprintf(buffer, 10, "%d.%d", t->domain->id, t->id);
	return buffer;
#endif
}

/* check the stack of all Threads */
void checkThreadsOfDomain(DomainDesc * domain)
{
	ThreadDesc *t;
	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		if (t == curthr())
			checkStackTrace(t, (u4_t *) & domain - 2);
		else
			checkStackTrace(t, (u4_t *) (t->context[PCB_EBP]));
	}
}


static void dump_context(ContextDesc context)
{
	u4_t f;
	dprintf("         EIP:    0x%08x\n", context[PCB_EIP]);
	dprintf("         ESP:    0x%08x\n", context[PCB_ESP]);
	dprintf("         GS:     0x%04x\n", context[PCB_GS] & 0xffff);
	dprintf("         FS:     0x%04x\n", context[PCB_FS] & 0xffff);
	dprintf("         ES:     0x%04x\n", context[PCB_ES] & 0xffff);
	dprintf("         DS:     0x%04x\n", context[PCB_DS] & 0xffff);
	dprintf("         EDI:    0x%08x\n", context[PCB_EDI]);
	dprintf("         ESI:    0x%08x\n", context[PCB_ESI]);
	dprintf("         EBP:    0x%08x\n", context[PCB_EBP]);
	dprintf("         EAX:    0x%08x\n", context[PCB_EAX]);
	dprintf("         EBX:    0x%08x\n", context[PCB_EBX]);
	dprintf("         ECX:    0x%08x\n", context[PCB_ECX]);
	dprintf("         EDX:    0x%08x\n", context[PCB_EDX]);
	f = context[PCB_EFLAGS];
	dprintf("         EFLAGS: 0x%08x ", f);
	if (f & 1)
		printf(" CF");
	if (f & 4)
		printf(" PF");
	if (f & 0x10)
		printf(" AF");
	if (f & 0x40)
		printf(" ZF");
	if (f & 0x80)
		printf(" SF");
	if (f & 0x100)
		printf(" TF");
	if (f & 0x200)
		printf(" IF");
	if (f & 0x400)
		printf(" DF");
	if (f & 0x800)
		printf(" OF");
	printf(" IOPL=%d", f & 0x3000);
	if (f & 0x4000)
		printf(" NT");
	if (f & 0x10000)
		printf(" RF");
	if (f & 0x20000)
		printf(" VM");
	if (f & 0x40000)
		printf(" AC");
	if (f & 0x80000)
		printf(" VIF");
	if (f & 0x100000)
		printf(" VIP");
	if (f & 0x200000)
		printf(" ID");
	printf("\n");
}

void print_threadinfo(ThreadDesc * t)
{
	dprintf("  Threadinfo: 0x%lx\n", t);
	dprintf("      Context at: 0x%08x\n", t->contextPtr);
	dump_context(t->context);
}

void print_full_threadinfo(ThreadDesc * t)
{
	printf("  Status of thread %d.%d:\n", t->domain->id, t->id);
	printf("    state=%d (%s)\n", t->state, get_state(t));
	if (t == idle_thread)
		printf("  This is the idle thread.");
	printf("  Running in domain %d (%s).\n", t->domain->id, t->domain->domainName);
	printTraceFromStoredCtx("          ", t, t->context);
	print_threadinfo(t);
}

#ifdef CHECK_STACKTRACE
void checkStackTraceNew()
{
	register unsigned int _temp__;
	asm volatile ("movl %%ebp, %%eax; movl (%%eax), %0":"=r" (_temp__));
	checkStackTrace(curthr(), _temp__);
}

void checkStackTrace(ThreadDesc * thread, u4_t * base)
{
	int i;
	u4_t *sp, *ebp, *eip;
	if (thread == NULL)
		return;
	ASSERTTHREAD(thread);
	sp = base;

	//  printf("<");
	for (i = 0; 1; i++) {

		if (sp <= thread->stack || sp + 2 >= thread->stackTop) {
			if (sp != NULL) {
				printStackTrace("CHECK", thread, base);
				sys_panic("Framepointer %p of thread %d.%d (%p) out of stack memory(%p..%p).\n", sp,
					  thread->domain->id, thread->id, thread, thread->stack, thread->stackTop);
			}
			//      printf("%p LASTEIP: %p ",thread,eip); print_eip_info(eip); printf("\n");
			//     if (eip != thread_exit) sys_panic("wrong exit function");
			return;
		}

		ebp = (u4_t *) * sp++;
		eip = (u4_t *) * sp++;

		sp = ebp;
	}
}
#else
void checkStackTrace(ThreadDesc * thread, u4_t * base)
{
}
void checkStackTraceNew()
{
}
#endif

void print_eip_info(char *addr)
{
	ClassDesc *classInfo;
	MethodDesc *method;
	jint bytecodePos, lineNumber;
#ifdef COMPACT_EIP_INFO
	static char name_buffer[512];
#endif

	if (addr == NULL) {
		printf("(null)");
		return;
	}
	if (findMethodAtAddr(addr, &method, &classInfo, &bytecodePos, &lineNumber) == 0) {
		if (classInfo == NULL || classInfo->definingLib == NULL || method == NULL) {
			printf("( ??? )");
			return;
		}
#ifdef COMPACT_EIP_INFO
		methodName2str(classInfo, method, name_buffer, 512);
		if (lineNumber < 0) {
			printf("(%s (0x%lx) at bytecode %ld)", name_buffer, method->code, bytecodePos);
		} else {
			printf("(%s (0x%lx) at line %ld)", name_buffer, method->code, lineNumber);
		}
#else
		printf("(%s::%s.%s%s (0x%lx) at bytecode %ld, line %ld)", classInfo->definingLib->name, classInfo->name,
		       method->name, method->signature, method->code, bytecodePos, lineNumber);
#endif
	} else {
		char *cname = findCoreSymbol((jint) addr);
		if (cname != NULL) {
			printf("(core:%s)", cname);
		} else {
			/* look for proxy code */
			char *meth, *sig;
			if (findProxyCode(addr, &meth, &sig, &classInfo) == 0) {
				printf("(proxy:%s:%s%s)", classInfo->name, meth, sig);
			} else {
				printf("( ??? )");
			}
		}
	}
}

void print_formatted_eip_info(char *addr)
{
	ClassDesc *classInfo;
	MethodDesc *method;
	jint bytecodePos, lineNumber;
	if (findMethodAtAddr(addr, &method, &classInfo, &bytecodePos, &lineNumber) == 0) {
		printf("%s:%s:%s%s:%p:%ld:%ld", classInfo->definingLib->name, classInfo->name, method->name, method->signature,
		       method->code, bytecodePos, lineNumber);
	} else {
		char *cname = findCoreSymbol((jint) addr);
		if (cname != NULL) {
			printf("core:%s", cname);
		} else {
			/* look for proxy code */
			char *meth, *sig;
			if (findProxyCode(addr, &meth, &sig, &classInfo) == 0) {
				printf("proxy:%s:%s%s", classInfo->name, meth, sig);
			} else {
				printf("???");
			}
		}
	}
}

char *getMethodNameByEIP(char *addr)
{
	ClassDesc *classInfo;
	MethodDesc *method;
	jint bytecodePos, lineNumber;
	if (findMethodAtAddr(addr, &method, &classInfo, &bytecodePos, &lineNumber) == 0) {
		return method->name;
	} else {
		char *cname = findCoreSymbol((jint) addr);
		if (cname != NULL) {
			return cname;
		} else {
			return "( ??? )";
		}
	}
	return "xxx";
}


char *get_state(ThreadDesc * t)
{
	ASSERTTHREAD(t);
	switch (t->state) {
	case STATE_INIT:
		return "INIT";
	case STATE_RUNNABLE:
		return "RUNNABLE";
	case STATE_ZOMBIE:
		return "ZOMBIE";
	case STATE_SLEEPING:
		return "SLEEPING";
	case STATE_WAITING:
		return "WAITING";
	case STATE_PORTAL_WAIT_FOR_RCV:
		return "PORTAL_WAIT_FOR_RCV";
	case STATE_PORTAL_WAIT_FOR_SND:
		return "PORTAL_WAIT_FOR_SND";
	case STATE_PORTAL_WAIT_FOR_RET:
		return "PORTAL_WAIT_FOR_RET";
	case STATE_BLOCKEDUSER:
		return "BLOCKEDUSER";
	case STATE_AVAILABLE:
		return "AVAILABLE";
	case STATE_WAIT_ONESHOT:
		return "STATE_WAIT_ONESHOT";
	case STATE_PORTAL_WAIT_FOR_PARAMCOPY:
		return "STATE_PORTAL_WAIT_FOR_PARAMCOPY";
	case STATE_PORTAL_WAIT_FOR_RETCOPY:
		return "STATE_PORTAL_WAIT_FOR_RETCOPY";
	}
	return "UNKNOWN";
}
