/********************************************************************************
 * System monitor
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Christian Wawersich
 * Copyright 2001-2002 Meik Felser
 * Copyright 2001-2002 Joerg Baumann
 *******************************************************************************/

#include "all.h"

#include "gc_common.h"
#include "gc_impl.h"
#include "gc_stack.h"
#include "gc_pgc.h"
#include "gc_pa.h"

#ifdef BINARY_DATA_TRANSMISSION
#include "bdt.h"
#endif

#ifdef PROFILE_GC
#include "gc_pgc.h"
#endif

#include "misc.h"

#ifdef SMP
#include "lapic.h"
#include "spinlock.h"
#endif

#define DATABLOCK 4096

extern char _start[], end[];

#ifndef KERNEL
extern u4_t total_ram;
#endif

#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE
extern jint n_eip_samples;
extern jint *eip_samples;
#endif
#ifdef PREEMPTION_SAMPLE
extern jint *eip_caller_samples;
extern jint *preempted_samples;
#endif

#ifdef PROFILE_SAMPLE_HEAPUSAGE
extern jint n_heap_samples;
extern struct heapsample_s *heap_samples;
#endif

#ifdef PROFILE_SAMPLE_PMC0
extern jint n_pmc0_samples;
extern jlong *pmc0_samples;
#endif

#ifdef PROFILE_SAMPLE_PMC1
extern jint n_pmc1_samples;
extern jlong *pmc1_samples;
#endif

ClassDesc *sharedArrayClasses;

#if 0
#ifdef PROFILE_EVENT_THREADSWITCH
static ThreadDesc *allthr[100];
#endif
#endif

u4_t timerticks = 0;

struct breakpoint_s {
	char *addr;
	char save;
};
static struct breakpoint_s breakpoint[10];
static int n_breakpoints = 0;

typedef struct eipinfo_s {
	MethodDesc *method;
	ClassDesc *class;
	char *corename;
	u4_t number;
} eipinfo_t;

#if ! defined(PRODUCTION) || defined(MONITOR)

#ifdef JAVA_MONITOR_COMMANDS

/****************/
/* Java registered commands */
#define MAX_COMMANDS 10
static int ncommands = 0;
#define MAX_COMMAND_LEN 40
struct {
	char name[MAX_COMMAND_LEN];
	ObjectHandle cmd;
	DomainDesc *domain;
	u4_t domainID;
} cmds[MAX_COMMANDS];

void register_command(char *name, ObjectHandle cmd, DomainDesc * domain)
{
	if (ncommands == MAX_COMMANDS)
		return;
	strncpy(cmds[ncommands].name, name, MAX_COMMAND_LEN);
	cmds[ncommands].cmd = cmd;
	cmds[ncommands].domain = domain;
	ncommands++;
}

u4_t execjava(DomainDesc * cd, char *name, char *meth, char *sig, int i)
{
	u4_t index, ret;
	Proxy *c;
	DISABLE_IRQ;
#ifdef DIRECT_SEND_PORTAL
	c = *(cmds[i].cmd);
	index = findDEPMethodIndex(cd, name, meth, sig);
	asm volatile
	 ("movl %1, %%ecx ; pushl $0; pushl %2 ; call direct_send_portal ; movl %%eax, %0 ; addl $8,%%esp":"=r" (ret):"r"(index),
	  "r"(c));
#else				/* DIRECT_SEND_PORTAL */
	sys_panic("");
#endif
	RESTORE_IRQ;
	return ret;
}
extern ThreadDesc *monitorThread;

void dump_javacommands_internal()
{
	int i;
	u4_t ret;
	char *name = "jx/zero/debug/MonitorCommand";
	char *meth = "getHelp";
	char *sig = "()Ljava/lang/String;";
	char value[128];

	for (i = 0; i < ncommands; i++) {
		printf("%s: ", cmds[i].name);
		ret = execjava(curdom(), name, meth, sig, i);
		if (ret != NULL) {
			stringToChar(ret, value, sizeof(value));
			printf("%s", value);
		}
		printf("\n");
	}
}
extern ThreadDesc *switchBackTo;

void dump_javacommands()
{
#if defined(JAVA_MONITOR_COMMANDS)
	if (monitorThread) {
		start_thread_using_code1(NULL, monitorThread, dump_javacommands_internal, NULL);
		switchBackTo = NULL;
	}
#endif
}

/* returns 1 if command was understood, 0 otherwise */
int exec_javacommand_internal(char *line)
{
	int i;
	char *name = "jx/zero/debug/MonitorCommand";
	char *meth = "execCommand";
	char *sig = "([Ljava/lang/String;)V";
	if (line == NULL)
		return;
	for (i = 0; i < ncommands; i++) {
		printf("%s %s %d\n", line, cmds[i].name, strlen(cmds[i].name));
		if (strncmp(line, cmds[i].name, strlen(cmds[i].name)) == 0) {
			execjava(curdom(), name, meth, sig, i);
			return 1;
		}
	}
	return 0;
}

int exec_javacommand(char *line)
{
#if defined(JAVA_MONITOR_COMMANDS)
	if (monitorThread) {
		start_thread_using_code1(NULL, monitorThread, exec_javacommand_internal, line);
		switchBackTo = NULL;
	} else {
		printf("No monitor thread.\n");
	}
#endif
}

void gc_monitor_commands(DomainDesc * domain, HandleReference_t handler)
{
#if 0				/* objects are proxies in domainzero */
	int i;
	for (i = 0; i < ncommands; i++) {
		if (cmds[i].domainID == domain->id) {
			handler(domain, (ObjectDesc **) & (cmds[i].cmd));
		}
	}
#endif
}
#endif				/* JAVA_MONITOR_COMMANDS */

/****************/
/* prints the next "anz" words in memory beginnig at sp 
   and tries to find the method at that addr */
void dumpstack(u4_t * sp, int anz)
{
	int i;
	for (i = 0; i < anz; i++) {
		printf("STACK: %p: %p ", sp, *sp);
		print_eip_info((char *) *sp);
		printf("\n");
		sp++;
	}
	printf("\n");
}

void printStackTraceNew(char *prefix)
{
	register unsigned int _temp__;
	asm volatile ("movl %%ebp, %%eax; movl (%%eax), %0":"=r" (_temp__));
	printStackTrace(prefix, curthr(), _temp__);
}

void printStackTrace(char *prefix, ThreadDesc * thread, u4_t * base)
{
	int i, frame;
	u4_t *sp, *ebp, *eip;
	if (thread == NULL)
		return;
	ASSERTTHREAD(thread);
	sp = base;

	printf("%s\n", prefix);

	for (i = 0; i < STACK_TRACE_LIMIT; i++) {

		if (sp <= thread->stack || sp + 2 >= thread->stackTop) {
			if (sp != NULL)
				printf("Framepointer %p out of stack memory(%p..%p).\n", sp, thread->stack, thread->stackTop);
			break;
		}

		ebp = (u4_t *) * sp++;
		eip = (u4_t *) * sp++;
		/*if (eip >= (u4_t*)callnative_static && eip <= (u4_t*)callnative_static+100 ) break; */

#ifndef STACK_ON_HEAP
		frame = ((int) ebp & (0xffffffff >> (32 - STACK_CHUNK)));
#else
		frame = thread->stack - ebp;
#endif

#ifdef COMPACT_EIP_INFO
		printf("%s(%d) ip=%p sp=%p", prefix, i, eip, sp);
#else
		printf("%s(%d) ip=%p sp=%p stack=%d", prefix, i, eip, sp, frame);
#endif
		print_eip_info((char *) eip);
		printf("\n");

		if (frame == 0)
			break;

		sp = ebp;
	}
}

/* similar to printStackTrace, but prints the N last stack frames 
   and does not stop if the stackpointer is invalid (not in the stack of the given thread) */
void printNStackTrace(char *prefix, ThreadDesc * thread, u4_t * base, int n)
{
	int i, frame;
	u4_t *sp, *ebp, *eip;
	if (thread == NULL)
		thread = idle_thread;
	ASSERTTHREAD(thread);
//if (thread->magic!=MAGIC_THREAD)
//    printf("printNStackTrace: thread->magix != MAGIC_THREAD");
	sp = base;

	printf("%s\n", prefix);

	for (i = 0; i < n; i++) {
		if (sp <= thread->stack || sp + 2 >= thread->stackTop) {
			if (sp != NULL)
				printf("X ");
			else
				break;
		}

		ebp = (u4_t *) * sp++;
		eip = (u4_t *) * sp++;

#ifndef STACK_ON_HEAP
		frame =		/*STACK_CHUNK_SIZE- */
		    ((int) ebp & (0xffffffff >> (32 - STACK_CHUNK)));
#else
		frame = thread->stack - ebp;
#endif
		printf("%s(%d) ip=%p sp=%p stack=%d", prefix, i, eip, sp, frame);
		print_eip_info((char *) eip);
		printf("\n");

		sp = ebp;
	}
}



#ifndef KERNEL
void printTraceFromCtx(char *prefix, ThreadDesc * thread, struct sigcontext *ctx)
{
#else
void printTraceFromCtx(char *prefix, ThreadDesc * thread, struct irqcontext *ctx)
{
#endif
	Class *classInfo;
	MethodDesc *method;
	jint bytecodePos;

	printf("%sMethod: ip=%p ", prefix, ctx->eip);
	print_eip_info((char *) (ctx->eip));
	printf("\n");

	printStackTrace(prefix, thread, (jint *) ctx->ebp);
}

void printTraceFromStoredCtx(char *prefix, ThreadDesc * thread, u4_t * ctx)
{
	ClassDesc *classInfo;
	MethodDesc *method;
	jint bytecodePos;

	printf("%sMethod: ip=%p ", prefix, ctx[PCB_EIP]);
	print_eip_info((char *) (ctx[PCB_EIP]));
	printf("\n");

	printStackTrace(prefix, thread, (jint *) ctx[PCB_EBP]);
}

#ifdef DEBUG
void printHandle(DomainDesc * domain)
{
#ifdef ENABLE_GC
	int i;
	if (strcmp(domain->domainName, "DomainZero") == 0)
		return;
	printf("%s:\n", domain->domainName);
	for (i = 0; i < MAX_REGISTERED; i++) {
		if (domain->gc.registeredObjects[i] != NULL) {
			printf("%3d %p", domain->id, domain->gc.registeredObjects[i]);
#ifdef DEBUG_HANDLE
			if (domain->gc.registrationPoints)
				print_eip_info(domain->gc.registrationPoints[i]);
#endif
			printf("\n");
		}
	}
#else				/* ENABLE_GC */
	printf("no gc");
#endif				/* ENABLE_GC */
}
#endif				/* DEBUG */

void printMemUsage(DomainDesc * d)
{
	printf("  Total memory usage of domain %d: %12d bytes (%s)\n", d->id, d->memusage * 4, d->domainName);
}

#ifdef PROFILE_HEAPUSAGE
void printHeapUsage(DomainDesc * d)
{
	jint l, nc;
	InstanceCounts_t counts;
	Class *c;
	ClassDesc *cd;

	printf("  Heap usage of domain %d (%s):  %d bytes\n", d->id, d->domainName, (gc_totalWords(d) - gc_freeWords(d)) * 4);

	/* RESET */

	for (l = 0; l < d->numberOfLibs; l++) {
		LibDesc *lib = d->libs[l];
		for (nc = 0; nc < lib->numberOfClasses; nc++) {
			ClassDesc *c = lib->allClasses[nc].classDesc;
			c->n_instances = 0;
		}
	}

	c = d->arrayClasses;
	while (c != NULL) {
		ArrayClassDesc *a = c->classDesc;
		a->n_arrayelements = 0;
		c = a->nextInDomain;
	}

	for (cd = sharedArrayClasses; cd != NULL; cd = cd->next) {
		cd->n_arrayelements = 0;
	}

	/* COUNT */

	gc_countInstances(d, &counts);

	/* OUTPUT */

	printf("  Objects: %ld bytes\n", counts.objbytes);
	printf("     MemoryUsed  NumInstances  Name\n");
	for (l = 0; l < d->numberOfLibs; l++) {
		LibDesc *lib = d->libs[l];
		for (nc = 0; nc < lib->numberOfClasses; nc++) {
			// FIXME jgbauman
			ClassDesc *c = lib->allClasses[nc].classDesc;
			jint objsize = c->instanceSize	/* field data */
			    + XMOFF	/* magic */
			    + 1	/* flags at negative index */
			    + 1 /* vtable pointer */ ;
			if (c->n_instances > 0) {
				printf("     %8d    %8d    %s\n", c->n_instances * objsize * 4, c->n_instances, c->name);
			}
		}
	}
	printf("  Arrays: %ld bytes\n", counts.arrbytes);
	printf("     MemoryUsed   Classname\n");

	c = d->arrayClasses;
	while (c != NULL) {
		ArrayClassDesc *a = c->classDesc;
		if (a->n_arrayelements > 0) {
			printf("     %8d      %s (domain-local)\n", a->n_arrayelements * 4, a->name);
		}
		c = a->nextInDomain;
	}
	for (cd = sharedArrayClasses; cd != NULL; cd = cd->next) {
		if (cd->n_arrayelements > 0) {
			printf("     %8d      %s\n", cd->n_arrayelements * 4, cd->name);
		}
	}

	printf("  Portals: %ld bytes\n", counts.portalbytes);
	printf("  MemoryPortal: %ld bytes\n", counts.memproxybytes);
	printf("  CPUStatePortal: %ld bytes\n", counts.cpustatebytes);
	printf("  AtomicVariablePortal: %ld bytes\n", counts.atomvarbytes);
	printf("  ServiceDesc: %ld bytes\n", counts.servicebytes);
	printf("  CAS: %ld bytes\n", counts.casbytes);
	printf("  TCB: %ld bytes\n", counts.tcbbytes);
	printf("  Stack: %ld bytes\n", counts.stackbytes);
}
#endif				/* PROFILE_HEAPUSAGE */


void dumpObject(ObjectDesc * o)
{
	ClassDesc *c;
	int i;
	MethodDesc *c_method;
	ClassDesc *c_class;
	jint c_pos;
	jint c_line;

	printStackTrace("DUMP", curthr(), (u4_t *) & o - 2);
	printf("DUMP OBJECT: %p\n", o);
	printf("  VTABLE:  %p\n", o->vtable);
	c = obj2ClassDesc(o);
	printf("     class:  %p %s\n", c, c->name);
	for (i = 0; i < c->vtableSize; i++) {
		if (c->vtable[i] != NULL) {
			/*findMethodAtAddr(c->vtable[i], &c_method,&c_class,&c_pos,&c_line);            
			   printf("       %03i:  %p  \n", i, c->vtable[i]);
			   if (c_method != NULL) printf("             %s.%s\n", c_class->name, c_method->name); */
			printf("       %03i:  %p", i, c->vtable[i]);
			if (c->methodVtable[i])
				printf("DUMP-VTABLE:         %s %s", c->methodVtable[i]->name, c->methodVtable[i]->signature);
			printf("\n");
		}
	}
}

void dump_data(ObjectDesc * o)
{
	cpuManager_dump(NULL, NULL, o);
}


void dumpThreadInfo(ThreadDesc * t)
{
	ASSERTTHREAD(t);
	printf("       ");
	if (t == NULL) {
		printf("dumpThreadInfo(NULL) called\n");
		return;
	}
	printf("%d.%d (%s) ", t->domain->id, t->id, t->domain->domainName);
	if (t->name != NULL)
		printf("%s ", t->name);
#ifdef PRINT_POINTERS
	printf("%p", t);
#endif
	printf(" (%s) at eip=0x%lx ", get_state(t), (jint) t->context[PCB_EIP]);
	print_eip_info((char *) t->context[PCB_EIP]);

#ifdef CPU_USAGE_STATISTICS
	printf(" cputime: %ld microsec, preempted: %d times", (t->cputime / CPU_MHZ), t->numberPreempted);
#endif				/* CPU_USAGE_STATISTICS */

#    ifdef JAVASCHEDULER
	printf(" SchedDom: %d", t->schedulingDomain->id);
#    endif
	printf("\n");
#ifdef DEBUG
	if (t->processingDEP != NULL) {
		printf("          Currently processing portal call 0x%lx", t->processingDEP);
		printf("\n");
	}
#endif
	if (t->blockedInDomain != NULL) {
		switch (t->state) {
		case STATE_PORTAL_WAIT_FOR_RCV:
			printf("          Currently waiting for ");
			break;
		case STATE_PORTAL_WAIT_FOR_SND:
		case STATE_PORTAL_WAIT_FOR_PARAMCOPY:
		case STATE_PORTAL_WAIT_FOR_RETCOPY:
			printf("          Currently using  ");
			break;
		default:
			printf("          blockedIn with unknown state?  ");
		}
		printf("service of domain %d (%s), portal %d (%s)\n", t->blockedInDomain->id, t->blockedInDomain->domainName,
		       t->blockedInServiceIndex, t->blockedInDomain->services[t->blockedInServiceIndex]->interface->name);
	}
	if (t->mostRecentlyCalledBy == (ThreadDesc *) 0xffffffff) {
		printf("          Most recently called by interrupt hardware\n");
	} else if (t->mostRecentlyCalledBy != NULL) {
		printf("          Most recently called by thread %d.%d (%s)\n", TID(t->mostRecentlyCalledBy),
		       t->mostRecentlyCalledBy->domain->domainName);
	}
}

void dumpPortalInfo(DEPDesc * d)
{
	ThreadDesc *t;
	ASSERTDEP(d);
#ifdef PRINT_POINTERS
	printf(" %p ", (jint) d);
	printf(" obj=%p ", d->obj);
#endif
	printf(" class=%s refcount=%d\n", obj2ClassDesc(d->obj)->name, d->refcount);
#ifdef PORTAL_STATISTICS
	printf("              no receiver=%ld, handoff=%ld\n", d->statistics_no_receiver, d->statistics_handoff);
#endif				/* PORTAL_STATISTICS */
#ifdef NEW_PORTALCALL
	{
		ServiceThreadPool *pool = d->pool;
		if (pool != NULL) {
			ThreadDesc *t = pool->firstReceiver;
			printf("              Pool %p (refs=%d), ReceiveThreads: ", pool, pool->refcount);
			for (; t; t = t->nextInReceiveQueue) {
				printf("%d.%d ", TID(t));
			}
			printf(" SendThreads: ");
			for (t = pool->firstWaitingSender; t; t = t->nextInDEPQueue) {
				printf("%d.%d ", TID(t));
			}
		} else {
			printf("              No pool.");
		}
		printf("\n");
	}
#else
	if (d->firstWaitingSender == NULL);	//printf("              no waiting sender\n");
	else
		for (t = d->firstWaitingSender; t != NULL; t = t->nextInDEPQueue) {
			printf("           SendThread: %d.%d", TID(t));
#ifdef PRINT_POINTERS
			printf(" 0x%lx", (jint) t);
#endif
			printf("\n", (jint) t);
		}

	if (d->receiver) {
		ThreadDesc *t = d->receiver;
		printf("              ReceiveThread: %d.%d", TID(t));
		if (t->mostRecentlyCalledBy == (ThreadDesc *) 0xffffffff) {
			printf("\n              Most recently called by interrupt hardware\n");
		} else if (t->mostRecentlyCalledBy != NULL) {
			printf("\n              Most recently called by thread %d.%d\n", TID(t->mostRecentlyCalledBy));
		} else {
			printf(" is idle\n");
		}
	} else {
		printf("              no ReceiveThread\n");
	}
#endif				/* NEW_PORTALCALL */
}


void dumpDomainInfo(DomainDesc * domain)
{
	u4_t j, codesum, codealloc;
	ThreadDesc *t;
	DEPDesc *d;
	if (domain == NULL)
		return;
	ASSERTDOMAIN(domain);
	printf("   ID=%d Name=%s", domain->id, domain->domainName);
#ifdef PRINT_POINTERS
	printf(" at 0x%lx:", domain);
#endif
	printf("\n");
	printf("   State: %d\n", domain->state);
#if defined(PORTAL_INTERCEPTOR) || defined(PORTAL_TRANSFER_INTERCEPTOR)
	printf("   (TCB: %s)\n", domain->memberOfTCB == JNI_TRUE ? "yes" : "no");
#endif
	printf("   InitialNaming: domain=%d svc=%d\n", domain->initialNamingProxy->targetDomainID,
	       domain->initialNamingProxy->index);
	printf("   GC epoch: %d\n", domain->gc.epoch);
	gc_printInfo(domain);
	printf("   Memory object bytes=%d", domain->memoryObjectBytes);
#ifdef PRINT_POINTERS
	domain->printInfo(domain);
#endif
	printf("\n");

#ifdef MEASURE_GC_TIME
	printf("   Total GC runs %d; GC time %d microsec; collected %d bytes\n", domain->gc.gcRuns, (u4_t) domain->gc.gcTime,
	       domain->gc.gcBytesCollected);
#endif
#ifdef PORTAL_STATISTICS
	printf("   Portal arguments/returns copied (bytes): in=%ld out=%ld\n", domain->portal_statistics_copyin_rcv * 4,
	       domain->portal_statistics_copyout_rcv * 4);
#endif				/* PORTAL_STATISTICS */

	printf("   Code segments: %ld\n", domain->cur_code + 1);
	codesum = 0;
	codealloc = 0;
	for (j = 0; j < domain->cur_code + 1; j++) {
		u4_t used, codesize;
		used = domain->codeTop[j] - domain->code[j];
		codesize = domain->codeBorder[j] - domain->code[j];
		printf("        Segment %d Used/Size=%d/%d", j, used, codesize);
#ifdef PRINT_POINTERS
		printf(" Start=0x%lx StartFree=0x%lx Border=0x%lx", domain->codeTop[j], domain->code[j], domain->codeBorder[j]);
#endif
		printf("\n");
		codesum += used;
		codealloc += codesize;
	}
	printf("      Total: Used/Size=%d/%d\n", codesum, codealloc);


#if defined(PORTAL_TRANSFER_INTERCEPTOR)
	printf("   Portal transfer interceptor: ");
	if (domain->portalInterceptorThread)
		printf(" thread=%d.%d", TID(domain->portalInterceptorThread));
	if (domain->portalInterceptorObject)
		printf(" class=%s", obj2ClassDesc(domain->portalInterceptorObject)->name);
	printf("\n");
#endif

	printf("   Threads:\n");
	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		dumpThreadInfo(t);
	}
#ifdef NEW_SCHED
	Sched_dumpDomain(domain);
#endif
	printf("   Services:\n");
	for (j = 0; j < MAX_SERVICES; j++) {
		d = domain->services[j];
		if (d != SERVICE_ENTRY_FREE && d != SERVICE_ENTRY_CHANGING) {
			printf("       %2d ", j);
			dumpPortalInfo(d);
		}
	}
}


int compare_eipinfo(const void *a, const void *b)
{
	eipinfo_t *x = a;
	eipinfo_t *y = b;

	return x->number > y->number;
}

#ifdef SMP
static inline void sti(void)
{
	asm volatile ("sti");
}
static spinlock_t in_monitor = SPIN_LOCK_UNLOCKED;
static int wait_while_in_monitor(void *nix)
{
	/*printf("\nCPU%d halted\n",get_processor_id()); */
	sti();
	spin_unlock_wait(in_monitor);
}
#endif

 /*
  * Support functions for domain traversal 
  */
static void monitor_print_threadinfo(DomainDesc * domain)
{
	ThreadDesc *t;
	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		dumpThreadInfo(t);
	}
}

#ifdef CPU_USAGE_STATISTICS
static u8_t top2;
static ThreadDesc *topThread;
static void monitor_top_cputime(DomainDesc * domain)
{
	ThreadDesc *t;
	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		if (t->cputime < top2 && (topThread == NULL || t->cputime > topThread->cputime)) {
			topThread = t;
		}
	}
}

#endif

static void print_domaininfo(DomainDesc * domain)
{
#ifdef PRINT_POINTERS
	printf(" %4d %p %s  ", domain->id, domain, domain->domainName);
#else
	printf(" %4d %s  ", domain->id, domain->domainName);
#endif
#ifdef CPU_USAGE_STATISTICS
	printf(" cputime: %ld microsec, preempted: %d times", (domain->cputime / CPU_MHZ), domain->preempted);
#endif				/* CPU_USAGE_STATISTICS */
	printf("\n");
}

static void print_domainmethods(DomainDesc * domain, void *mname)
{
	u4_t l, c, m;
	printf("Domain: %s \n", domain->domainName);
	for (l = 0; l < domain->numberOfLibs; l++) {
		LibDesc *lib = domain->libs[l];
		for (c = 0; c < lib->numberOfClasses; c++) {
			ClassDesc *cl = lib->allClasses[c].classDesc;
			for (m = 0; m < cl->numberOfMethods; m++) {
				MethodDesc *cm = &(cl->methods[m]);
				if (strcmp((char *) mname, cm->name) == 0) {
					printf("%s.%s%s desc:0x%lx addr:0x%lx size:%d\n", cl->name, cm->name, cm->signature, cm,
					       cm->code, cm->numberOfCodeBytes);
				}
			}
		}
	}
}

/** read input */

#ifndef KERNEL
void readline(char *buf, int max)
{
	char c;
	for (;;) {
		if (max == 0)
			return;
		c = 0;
		while (read(STDIN_FILENO, &c, 1) != 1);
		if (c == '\n') {
			*buf = 0;
			return;
		}
		*buf++ = c;
		max--;
	}
}
#else				/* KERNEL */
void readline(char *buf, int max)
{
	read_line(0, buf, max);
}
#endif				/* KERNEL */


/*
 * Main monitor function
 */

extern ClassDesc *deviceMemoryClass;

#if 0
#ifdef KERNEL
void monitor_internal(struct irqcontext_timer *ctx);
#else
void monitor_internal(struct sigcontext *ctx);
#endif


#ifdef KERNEL
void monitor(struct irqcontext_timer *ctx)
{
#else
void monitor(struct sigcontext *ctx)
{
#endif
	if (monitorThread) {
		start_thread_using_code1(NULL, monitorThread, monitor_internal, NULL);
	} else {
		monitor_internal(NULL);
	}
}
#endif

#ifdef KERNEL
void monitor(struct irqcontext_timer *ctx)
{
#else
void monitor(struct sigcontext *ctx)
{
#endif
	char line[80];
	/* unused variables
	   int i,j;
	   ThreadDesc *t;
	   DEPDesc *d;
	 */
#ifndef KERNEL
	DISABLE_IRQ;
#else
#  ifdef SMP
	printf("\nCPU%d will halt all cpus\n", get_processor_id());
	spin_lock(&in_monitor);
	smp_call_function(APIC_DEST_ALLBUT, wait_while_in_monitor, NULL, 0, NULL);
#  endif
#endif
#ifdef DEBUG
	check_current = 0;
#endif
#ifdef LOG_PRINTF
	printf2mem = 0;
#endif

	while (1) {
#ifdef SMP
		printf("\n(CPU%d) Monitor: ", get_processor_id());
#else
		printf("%s", "\nMonitor: ");
#endif

#ifndef KERNEL
		fflush(stdout);
#endif
		readline(line, 80);
		if (strcmp("cont", line) == 0 || strcmp("c", line) == 0) {
			break;
		} else if (strcmp("step", line) == 0 || strcmp("s", line) == 0) {
			if (ctx != NULL) {
				ctx->eflags |= 0x00000100;
				break;
			} else {
				printf("Single stepping not possible. No context.\n");
			}
		} else if (strncmp("vinfo ", line, 6) == 0) {
			char *cname = line + 6;
			Class *c;
			ClassDesc *cd;
			int i;
			printf("info to  %s\n", cname);
			c = findClass(domainZero, cname);
			if (c != NULL) {
				cd = c->classDesc;
				cd = mem_getDeviceMemoryClass();
				printf("Class at %p ClassDesc at %p vtable at %p\n", c, cd, cd->vtable);
				for (i = 0; i < cd->vtableSize; i++) {
					if (cd->vtable[i] != NULL)
						printf("%d %p\n", i, cd->vtable[i]);
				}
			} else {
				printf("\"%s\" not found", cname);
			}
		} else if (strcmp("minfo", line) == 0) {
			printf("usage: minfo <methodname>\n");
		} else if (strncmp("minfo ", line, 6) == 0) {
			int d, l, c, m;
			char *mname = line + 6;
			printf("info to  %s\n", mname);
			printf("core: \n");
			printCoreSymbolInformation(mname);
			foreachDomain1(print_domainmethods, mname);
#ifdef PROFILE
		} else if (strcmp("profile", line) == 0) {
			printf("usage: profile <domainID>\n");
			printf("Which Domain ?\n");
			foreachDomain(print_domaininfo);
		} else if (strncmp("profile ", line, 8) == 0) {
			u4_t id = (u4_t) strtol(line + 8, NULL, 10);
			DomainDesc *d = findDomain(id);
			profile_shell(d);
#endif				/* PROFILE */
		} else if (strcmp("slibs", line) == 0) {
			SharedLibDesc *sharedLib;
			u4_t sum_native = 0, sum_bytecodes = 0, sum_vtable = 0;
			printf(" Shared Libraries:\n");
			printf("%3s %20s %9s %9s %9s\n", "ID", "Name", "native_code_size", "bytecode_size", "vtable_size");
			printf("-------------------------------------------------------------\n");
			for (sharedLib = sharedLibs; sharedLib != NULL; sharedLib = sharedLib->next) {
				printf("%3d %20s %9d %9d %9d\n", sharedLib->id, sharedLib->name, sharedLib->codeBytes,
				       sharedLib->bytecodes, sharedLib->vtablesize * 4);
				sum_native += sharedLib->codeBytes;
				sum_bytecodes += sharedLib->bytecodes;
				sum_vtable += sharedLib->vtablesize;
			}
			printf("-------------------------------------------------------------\n");
			printf("%3s %20s %9d %9d %9d\n", "", "", sum_native, sum_bytecodes, sum_vtable);
		} else if (strncmp("slib ", line, 5) == 0) {
			SharedLibDesc *sharedLib;
			u4_t id = (u4_t) strtol(line + 5, NULL, 10);

			for (sharedLib = sharedLibs; sharedLib != NULL; sharedLib = sharedLib->next) {
				if (sharedLib->id == id) {
					int i;
					printf("SharedLib: %d %s\n", sharedLib->id, sharedLib->name);
					for (i = 0; i < sharedLib->numberOfMeta; i++) {
						printf("%s = %s\n", sharedLib->meta[i].var, sharedLib->meta[i].val);
					}
					break;
				}
			}
			if (sharedLib == NULL)
				printf("Not found.\n");
#ifdef DEBUG
		} else if (strcmp("repl", line) == 0) {
			SharedLibDesc *sharedLib;
			u4_t i, j;
			/* Clear */
			for (sharedLib = sharedLibs; sharedLib != NULL; sharedLib = sharedLib->next) {
				for (i = 0; i < sharedLib->numberOfClasses; i++) {
					ClassDesc *c = &(sharedLib->allClasses[i]);
					c->numberOfImplementors = 0;
				}
			}

			/* Count */
			for (sharedLib = sharedLibs; sharedLib != NULL; sharedLib = sharedLib->next) {
				for (i = 0; i < sharedLib->numberOfClasses; i++) {
					ClassDesc *c = &(sharedLib->allClasses[i]);
					for (j = 0; j < c->numberOfInterfaces; j++) {
						if (c->classType == CLASSTYPE_CLASS) {
							c->interfaces[j]->numberOfImplementors++;
							c->interfaces[j]->implementedBy = c;
						}
					}
				}
			}

			/* Print */
			for (sharedLib = sharedLibs; sharedLib != NULL; sharedLib = sharedLib->next) {
				for (i = 0; i < sharedLib->numberOfClasses; i++) {
					ClassDesc *c = &(sharedLib->allClasses[i]);
					if (c->numberOfImplementors > 0) {
						printf("%3d %s", c->numberOfImplementors, c->name);
						if (c->numberOfImplementors == 1) {
							printf(":%s", c->implementedBy->name);
						}
						printf("\n");
					}
				}
			}

			/* Suggest -replace option */
			printf("To improve performance use: -replace ");
			for (sharedLib = sharedLibs; sharedLib != NULL; sharedLib = sharedLib->next) {
				for (i = 0; i < sharedLib->numberOfClasses; i++) {
					ClassDesc *c = &(sharedLib->allClasses[i]);
					if (c->numberOfImplementors == 1) {
						printf("%s:%s:", c->name, c->implementedBy->name);
					}
				}
			}
			printf("\n");

#endif				/* DEBUG */
#ifdef COPY_STATISTICS
		} else if (strcmp("copystat", line) == 0) {
			SharedLibDesc *sharedLib;
			u4_t i, j;
			ClassDesc *c;
			/* classes */
			printf("Instances  Size   Total Data Copied   Classname\n");
			printf(" Copied   (bytes)     (bytes)\n");
			for (sharedLib = sharedLibs; sharedLib != NULL; sharedLib = sharedLib->next) {
				for (i = 0; i < sharedLib->numberOfClasses; i++) {
					c = &(sharedLib->allClasses[i]);
					if (c->copied > 0) {
						printf("  %3d      %3d          %3d            %s\n", c->copied,
						       c->instanceSize * 4, (c->copied * c->instanceSize) * 4, c->name);
					}
					while (c = c->arrayClass) {
						if (c->copied) {
							printf("  %3d        -          %3d            %s\n", c->copied,
							       c->copied_arrayelements * 4, c->name);
						}
					}
				}
			}
			/* arrays */
#define COPIED(CLASS) {extern Class* CLASS; c = CLASS->classDesc->arrayClass; if(c && c->copied) {printf("   %s arrays: %3d  total bytes: %6d\n", c->name, c->copied, c->copied_arrayelements*4);}}
			COPIED(class_C);
			COPIED(class_B);
			COPIED(class_Z);
			COPIED(class_I);


#endif				/* COPY_STATISTICS */
		} else if (strcmp("mem", line) == 0) {
			jxmalloc_stat();
#ifdef KERNEL
			{
				extern char _start[], end[];

				printf("Core text/data/bbs: %p .. %p\n", _start, end);
				if ((boot_info.flags & MULTIBOOT_MODS)
				    && (boot_info.mods_count > 0)) {
					struct multiboot_module *m = (struct multiboot_module *) boot_info.mods_addr;
					jint i;
					printf("Reserved Lower Memory: %p .. %p\n", boot_info.mem_lower * 1024, 0x100000);
					printf("Unusable Upper Memory: %p .. %p\n", 0x100000 + boot_info.mem_upper * 1024,
					       0xffffffff);
					printf("Boot-Modules: %d\n", boot_info.mods_count);
					printf("Modules-MemRange: %p .. %p\n", boot_info.mods_addr,
					       boot_info.mods_addr + boot_info.mods_count * sizeof(*m));
					for (i = 0; i < boot_info.mods_count; i++) {
						if (m[i].string != 0) {
							char *s = (char *) (m[i].string);
							unsigned len = strlen(s);
							printf("  String-MemRange: %p .. %p\n", m[i].string,
							       m[i].string + len + 1);
						}
						printf("  Module-MemRange: %p .. %p\n", m[i].mod_start, m[i].mod_end);
					}

				} else {
					printf("No Boot-Modules.\n");
				}
			}
#endif
#ifdef SAMPLING_TIMER_IRQ
		} else if (strcmp("startsampling", line) == 0) {
			do_sampling = 1;
			printf("Started sampling.\n");
		} else if (strcmp("stopsampling", line) == 0) {
			do_sampling = 0;
			printf("Stopped sampling.\n");
		} else if (strcmp("clearsamples", line) == 0) {
			n_eip_samples = 0;
			printf("Cleared samples.\n");
#endif				/* SAMPLING_TIMER_IRQ */
#ifdef CPU_USAGE_STATISTICS
		} else if (strcmp("top", line) == 0) {
			ThreadDesc *t;
			printf(" Top:\n");
			topThread = NULL;
			top2 = 0xffffffffffffffff;
			for (;;) {
				topThread = NULL;
				foreachDomain(monitor_top_cputime);
				if (topThread == NULL)
					break;
				printf(" %d.%d cputime: %ld microseconds\n", TID(topThread), topThread->cputime / CPU_MHZ);
				top2 = topThread->cputime;
			}
#endif				/* CPU_USAGE_STATISTICS */
#ifdef MEASURE_GC_TIME
		} else if (strcmp("gc", line) == 0) {
			printf(" Global GC statistics:\n");
			printf("    DID    Runs         Microseconds\n");
			foreachDomain(gc_printInfo);
#endif
		} else if (strcmp("threads", line) == 0) {
			printf(" Threads:\n");
			foreachDomain(monitor_print_threadinfo);
		} else if (strcmp("domains", line) == 0) {
			printf(" Domains:\n");
			foreachDomain(print_domaininfo);
		} else if (strncmp("domain ", line, 7) == 0) {
			u4_t id = (u4_t) strtol(line + 7, NULL, 10);
			DomainDesc *d = findDomain(id);
			dumpDomainInfo(d);
		} else if (strncmp("kill ", line, 5) == 0) {
			u4_t id = (u4_t) strtol(line + 5, NULL, 10);
			DomainDesc *d = findDomain(id);
			terminateDomain(d);
		} else if (strcmp("ns", line) == 0) {
			struct nameValue_s *n;
			for (n = nameValue; n != NULL; n = n->next) {
				printf("%p %s ", n->obj, n->name);
				if (n->obj->targetDomain != NULL && (n->obj->targetDomainID != n->obj->targetDomain->id)) {
					printf("deactivated portal into terminated domain (%d!=%d; target=%p)\n",
					       n->obj->targetDomainID, n->obj->targetDomain->id, n->obj->targetDomain);
				} else {
					if (n->obj->targetDomain == NULL) {
						printf("direct portal into DomainZero\n");
					} else {
						printf("portal into domain %s (%d)\n", n->obj->targetDomain->domainName,
						       n->obj->targetDomain->id);
					}
				}
			}
#if 0
			naming_listwaiters();
#endif
		} else if (strncmp("libs ", line, 5) == 0) {
			jint j;
			u4_t id = (u4_t) strtol(line + 5, NULL, 10);
			DomainDesc *d = findDomain(id);
			printf("     Libs loaded: %d\n", d->numberOfLibs);
			for (j = 0; j < d->numberOfLibs; j++) {
				LibDesc *l = d->libs[j];
#ifdef USE_LIB_INDEX
				printf("        %s(%d) %p", l->sharedLib->name, l->sharedLib->ndx, l);
#else
				printf("        %s %p", l->sharedLib->name, l);
#endif
			}
#ifndef KERNEL
		} else if (strcmp("break", line) == 0) {
			asm("int $3");
#endif
		} else if (strcmp("runq", line) == 0) {
			int cpu;
			if (curthr() != NULL) {
				printf("   Running thread: ");
				dumpThreadInfo(curthr());
			}
			printf("   Runqueue:\n");
#ifdef SMP
			for (cpu = 0; cpu < num_processors_online; cpu++)
				SMPcpuManager_dump(NULL, domainZero->cpu[online_cpu_ID[cpu]]);
#else
#ifndef NEW_SCHED
#else
			Sched_dump();
#endif
#endif

		} else if (strncmp("eip ", line, 4) == 0) {
			void *t = (void *) strtol(line + 4, NULL, 16);
			print_eip_info(t);
			printf("\n");
		} else if (strncmp("thread ", line, 7) == 0) {
			ThreadDesc *t = NULL;
			if (strncmp("current", line + 7, 7) == 0)
				t = curthr();
			else {
				DomainDesc *d = NULL;
				int i;
				for (i = 7; i < strlen(line); i++) {
					if (line[i] == '.') {
						line[i] = 0;
						d = findDomain(strtol(line + 7, NULL, 10));
						t = findThreadByID(d, strtol(line + i + 1, NULL, 10));
					}
				}
			}
			if (t == NULL) {
				printf("Error: Unknown thread\n");
			} else {
				ASSERTTHREAD(t);
				print_full_threadinfo(t);
			}
		} else if (strncmp("stacktrace ", line, 11) == 0) {
			u4_t *addr = (u4_t *) strtol(line + 11, NULL, 16);
			printNStackTrace(" ", NULL, addr, 5);
		} else if (strncmp("stacktrace2 ", line, 12) == 0) {
			u4_t *addr = (u4_t *) strtol(line + 12, NULL, 16);
			printf("%p", addr);
			printNStackTrace(" ", NULL, addr, 100);
		} else if (strncmp("stackdump ", line, 10) == 0) {
			u4_t *addr = (u4_t *) strtol(line + 10, NULL, 16);
			dumpstack(addr, 10);
		} else if (strncmp("code ", line, 5) == 0) {
			char *c = (char *) strtol(line + 5, NULL, 16);
			printf(" Code at address %p:\n", c);
			print_eip_info(c);
			printf("\n");
		} else if (strncmp("dcode ", line, 6) == 0) {
			int i, j, k;
			u4_t id = (u4_t) strtol(line + 6, NULL, 10);
			DomainDesc *d = findDomain(id);
			printf(" Code of Domain 0x%lx:\n", d);
			for (i = 0; i < d->numberOfLibs; i++) {
				LibDesc *lib = d->libs[i];
				for (j = 0; j < lib->numberOfClasses; j++) {
					printf(" %s:\n", lib->allClasses[j].classDesc->name);
					for (k = 0; k < lib->allClasses[j].classDesc->numberOfMethods; k++) {
						MethodDesc *m = &(lib->allClasses[j].classDesc->methods[k]);
						printf("   0x%lx  %s%s\n", m->code, m->name, m->signature);
					}
				}
			}
			/*
			   dumpDomainInfo(d);
			   int i,j,k;
			   DomainDesc* d = (DomainDesc*)strtol(line + 6, NULL, 16);
			   printf(" Code of Domain 0x%lx:\n", d); 
			   for(i=0; i<d->numberOfLibs; i++) {
			   LibDesc *lib = d->libs[i];
			   for(j=0; j<lib->numberOfClasses; j++) {
			   printf(" %s:\n",  lib->allClasses[j].classDesc->name);
			   for(k=0; k<lib->allClasses[j].classDesc->numberOfMethods; k++) {
			   MethodDesc *m = &(lib->allClasses[j].classDesc->methods[k]);
			   printf("    0x%lx  %s %s\n", m->code, m->name, m->signature);
			   }
			   }
			   }
			 */
		} else if (strncmp("x ", line, 2) == 0) {
			addr_t *a = (addr_t *) strtoul(line + 2, NULL, 16);
			printf("%08lx:  %08lx\n", a, *a);
		} else if (strncmp("setb ", line, 5) == 0) {
			char *cp = line + 5;
			unsigned char *dptr = (unsigned char *) strtoul(cp, &cp, 16);
			unsigned char dval = (char) strtol(cp, NULL, 0);
			*dptr = dval;
		} else if (strncmp("setw ", line, 5) == 0) {
			char *cp = line + 5;
			unsigned short *dptr = (unsigned short *) strtoul(cp, &cp, 16);
			unsigned short dval = (unsigned short) strtoul(cp, NULL, 0);
			*dptr = dval;
		} else if (strncmp("setl ", line, 5) == 0) {
			char *cp = line + 5;
			unsigned long *dptr = (unsigned long *) strtoul(cp, &cp, 16);
			unsigned long dval = (unsigned long) strtoul(cp, NULL, 0);
			*dptr = dval;
		} else if (strncmp("dump ", line, 5) == 0) {
			char *cp = line + 5;
			ObjectDesc *dptr = (ObjectDesc *) strtoul(cp, &cp, 16);
			dumpObject(dptr);
		} else if (strcmp("ticks", line) == 0) {
			printf("Timer has ticked %ld times.\n", timerticks);
#ifdef KERNEL
		} else if (strcmp("irq", line) == 0) {
			dump_irqhandlers();
#endif
#ifdef SMP
		} else if (strcmp("apic", line) == 0) {
			print_IO_APIC();
#endif
		} else if (strncmp("dep ", line, 4) == 0) {
			DEPDesc *dep = NULL;
			DomainDesc *d = NULL;
			int i;
			for (i = 4; i < strlen(line); i++) {
				if (line[i] == '.') {
					line[i] = 0;
					d = findDomain(strtol(line + 4, NULL, 10));
					dep = d->services[strtol(line + i + 1, NULL, 10)];
				}
			}
			if (dep == NULL)
				printf("Error: Unknown portal\n");
			else
				dumpPortalInfo(dep);
#ifdef EVENT_LOG
		} else if (strcmp("eventtypes", line) == 0) {
			u4_t i;
			u8_t prev;
			printf("Eventtypes:\n");
			for (i = 1; i < n_event_types; i++) {
				printf("%2d %s\n", i, eventTypes[i]);
			}
		} else if (strncmp("event ", line, 6) == 0) {
			u4_t i;
			u8_t prev;
			int t = strtol(line + 6, NULL, 10);
			printf("Events (microseconds):\n");
			prev = events[0].timestamp;
			for (i = 0; i < n_events; i++) {
				if (events[i].number == t) {
					u4_t ts = (u4_t) ((events[i].timestamp - prev) / CPU_MHZ);
					prev = events[i].timestamp;
					printf("%15ld %10ld %10ld\n", ts, events[i].info, events[i].info2);
				}
			}
		} else if (strcmp("events", line) == 0) {
#ifdef KERNEL
			u4_t i;
			u8_t prev;
			char *data = jxmalloc(DATABLOCK + 1024 MEMTYPE_OTHER);
			u4_t *d = data;
			u4_t len = 0;

			printf("Send event types:\n");
			printf("Send events (cycles)\n");

			send_name("events");

			wrint(&d, &len, n_event_types - 1);
			for (i = 1; i < n_event_types; i++) {
				wrstring(&d, &len, eventTypes[i]);
			}
			prev = events[0].timestamp;
			wrint(&d, &len, n_events);
			for (i = 0; i < n_events; i++) {
				//u8_t ts = events[i].timestamp - prev;
				u8_t ts = events[i].timestamp;
				if (events[i].timestamp < prev)
					exit(0);
				if (events[i].number == 0)
					continue;
				if (len > DATABLOCK) {
					write_data(data, len);
					len = 0;
					d = data;
				}
				prev = events[i].timestamp;
				wrlong(&d, &len, ts);
				wrint(&d, &len, events[i].number);
				wrint(&d, &len, events[i].info);
				wrint(&d, &len, events[i].info2);
			}
			if (len > 0) {
				write_data(data, len);
			}
			last_data();
#endif
		} else if (strcmp("log", line) == 0) {
			u4_t i;
			u8_t prev;
			printf("Eventtypes:\n");
			for (i = 1; i < n_event_types; i++) {
				printf("%2d %s\n", i, eventTypes[i]);
			}
			printf("Events (microseconds):\n");
			prev = events[0].timestamp;
			for (i = 0; i < n_events; i++) {
				u4_t ts = (u4_t) ((events[i].timestamp - prev) / CPU_MHZ);
				if (events[i].number == 0)
					continue;
				prev = events[i].timestamp;
				printf("%15ld %d\n", ts, events[i].number);
			}
		} else if (strcmp("logts", line) == 0) {
			u4_t i;
			u8_t prev;
			printf("-- Event log (cycles):\n");
			printf("Eventtypes:\n");
			for (i = 1; i < n_event_types; i++) {
				printf("%2d %s\n", i, eventTypes[i]);
			}
			printf("\n");
			for (i = 0; i < n_events; i++) {
				u8_t ts = events[i].timestamp;
				if (events[i].number == 0)
					continue;
				printf("0x%08x%08x %d\n", (u4_t) (ts >> 32), (u4_t) (ts & 0xffffffff), events[i].number);
			}
		} else if (strcmp("rlogts", line) == 0) {
#if defined(PROFILE_AGING) || defined(ZSTORE)
			rlogts();
#endif				/* PROFILE_AGING ZSTORE */
#endif
#ifdef LOG_PRINTF
		} else if (strcmp("printflog", line) == 0) {
			transfer_printflog();
#endif
#ifdef DEBUG
		} else if (strcmp("handle", line) == 0) {
			foreachDomain(printHandle);
#endif				/* DEBUG_HANDLE */
		} else if (strcmp("memobj", line) == 0) {
			print_memobj(-1);
#ifdef DEBUG_MEMORY_CREATION
		} else if (strncmp("memobj ", line, 7) == 0) {
			u4_t id = (u4_t) strtol(line + 7, NULL, 10);
			print_memobj(id);
#endif				/*DEBUG_MEMORY_CREATION */
		} else if (strncmp("memref ", line, 7) == 0) {
			u4_t id = (u4_t) strtol(line + 7, NULL, 10);
			print_memref(id);
#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE
		} else if (strcmp("rawsamples", line) == 0) {
			u4_t i;
			for (i = 0; i < n_eip_samples; i++) {
#ifdef PREEMPTION_SAMPLE
				printf(" tid=%p ", preempted_samples[i]);
#endif
				printf("  %p ", eip_samples[i]);
				print_eip_info(eip_samples[i]);
#ifdef PREEMPTION_SAMPLE
				printf(" %p ", eip_caller_samples[i]);
				print_eip_info(eip_caller_samples[i]);
#endif
				printf("\n");
			}
		} else if (strcmp("samples", line) == 0) {
			int rawsamples = 0;
			u4_t i, j;
			u4_t total;
			u4_t n_sorted = 0;
			eipinfo_t *sorted = jxmalloc(n_eip_samples * sizeof(eipinfo_t) MEMTYPE_OTHER);
			/* sort method names */
			n_sorted = 0;
			total = 0;
			memset(sorted, 0, sizeof(eipinfo_t) * n_eip_samples);
			if (rawsamples)
				printf("RAW: class.method:bytecodepos:linenumber\n");
			for (i = 0; i < n_eip_samples; i++) {
				MethodDesc *method;
				ClassDesc *classInfo;


				jint bytecodePos, lineNumber;
				if (findMethodAtAddr(eip_samples[i], &method, &classInfo, &bytecodePos, &lineNumber) == 0) {
					/*char *name = getMethodNameByEIP(eip_samples[i]); */
					if (rawsamples)
						printf("%s.%s:%d:%d\n", classInfo->name, method->name, bytecodePos, lineNumber);
					for (j = 0; j < n_sorted; j++) {
						/*if (strcmp((char*)sorted[j],name)==0) { */
						if (method == sorted[j].method) {
							sorted[j].number++;
							total++;
							goto next_sample1;
						}
					}
					/* not found */
					sorted[n_sorted].method = method;
					sorted[n_sorted].class = classInfo;
					sorted[n_sorted].number = 1;
					total++;
					n_sorted++;
				} else {
					char *cname = findCoreSymbol(eip_samples[i]);
					if (cname == NULL) {
						MethodDesc *method;
						Class *classInfo;
						char *sig;
						if (findProxyCode(eip_samples[i], &method, &sig, &classInfo) == 0) {
							cname = "proxy";
						} else {
							char buf[64];
							buf[0] = '0';
							buf[1] = 'x';
							dtostr(buf + 2, 8, eip_samples[i], 16);
							cname = jxmalloc(strlen(buf) + 1 MEMTYPE_OTHER);
							strcpy(cname, buf);
						}
					}
					if (rawsamples)
						printf("core %s:%p\n", cname, eip_samples[i]);
					for (j = 0; j < n_sorted; j++) {
						if (sorted[j].corename != NULL && strcmp(sorted[j].corename, cname) == 0) {
							sorted[j].number++;
							total++;
							goto next_sample1;
						}
					}
					/* not found */
					sorted[n_sorted].corename = cname;
					sorted[n_sorted].number = 1;
					total++;
					n_sorted++;

				}
			      next_sample1:
			}
#ifndef KERNEL
			qsort(sorted, n_sorted, sizeof(eipinfo_t), compare_eipinfo);
#endif
			printf("Sorted by method: \n");
			for (i = 0; i < n_sorted; i++) {
				if (sorted[i].method != NULL) {
					char *name = sorted[i].method->name;
					char *classname = sorted[i].class->name;
					printf("  %04d  %s.%s \n", (int) (sorted[i].number * 10000 / total), classname, name);
				} else if (sorted[i].corename != NULL) {
					printf("  %04d  %s \n", (int) (sorted[i].number * 10000 / total), sorted[i].corename);
				} else {
					printf("  %04d   \n", (int) (sorted[i].number * 10000 / total));
				}
			}
			printf("  %08d  TOTAL\n", total);
			jxfree(sorted, n_eip_samples * sizeof(eipinfo_t) MEMTYPE_PROFILING);
#endif				/* PROFILE_SAMPLE */
#ifdef CHECK_HEAPUSAGE
		} else if (strncmp("checkheap ", line, 10) == 0) {
			/* check whether heap is consistent */
			DomainDesc *domain = findDomain(strtol(line + 10, NULL, 10));
			if (gc_checkHeap(domain, JNI_FALSE) == JNI_TRUE) {
				printf(" Heap passed check.\n");
			} else {
				printf(" Heap contains errors.\n");
			}

#endif				/* CHECK_HEAPUSAGE */
#ifdef FIND_OBJECTS_BY_CLASS
		} else if (strncmp("dumpobj ", line, 8) == 0) {
			DomainDesc *d = findDomain(strtol(line + 8, NULL, 10));
			char *classname = line + 8;
			while (*classname != ' ' && *classname != '\0')
				classname++;
			while (*classname == ' ' && *classname != '\0')
				classname++;
			printf("Objects of class \"%s\" in domain %d (%s)\n", classname, d->id, d->domainName);
			gc_findOnHeap(d, classname);
#endif				/* FIND_OBJECTS_BY_CLASS */
		} else if (strcmp("memusage", line) == 0) {
			foreachDomain(printMemUsage);
		} else if (strncmp("mem ", line, 4) == 0) {
			DomainDesc *d = findDomain(strtol(line + 4, NULL, 10));
			printMemUsage(d);
#ifdef PROFILE_HEAPUSAGE
		} else if (strncmp("heap ", line, 5) == 0) {
			DomainDesc *d = findDomain(strtol(line + 5, NULL, 10));
			printHeapUsage(d);
#endif				/* PROFILE_HEAPUSAGE */
#ifdef PROFILE_SAMPLE_HEAPUSAGE
		} else if (strcmp("heapsample", line) == 0) {
			jint i, j;
			printf("EIP   bytes   class\n");
			for (i = 0; i < n_heap_samples; i++) {
				printf("%20s %8d ", heap_samples[i].cl->name, heap_samples[i].size);
				for (j = 0; j < 10; j++) {
					if (heap_samples[i].eip[j] != NULL) {
						printf("%p", heap_samples[i].eip[j]);
						print_eip_info(heap_samples[i].eip[j]);
						printf("; ");
					}
				}
				printf("\n");
			}
#endif				/* PROFILE_SAMPLE_HEAPUSAGE */
#ifdef PROFILE_SAMPLE_PMC0
#  ifdef PROFILE_SAMPLE_PMC_DIFF
		} else if (strcmp("pmcdiffsample", line) == 0) {
#  else
		} else if (strcmp("pmc0sample", line) == 0) {
#  endif
			jint i, j;
			for (i = 0; i < n_pmc0_samples; i++) {
				u4_t *p = (u4_t *) & (pmc0_samples[i]);
				printf("%x%08x\n", *(p + 1), *p);
			}
#endif				/* PROFILE_SAMPLE_PMC0 */
#ifdef PROFILE_SAMPLE_PMC1
		} else if (strcmp("pmc1sample", line) == 0) {
			jint i, j;
			for (i = 0; i < n_pmc1_samples; i++) {
				u4_t *p = (u4_t *) & (pmc1_samples[i]);
				printf("%x%08x\n", *(p + 1), *p);
			}
#endif				/* PROFILE_SAMPLE_PMC1 */
#if 0
#ifdef PROFILE_EVENT_THREADSWITCH
		} else if (strcmp("switches", line) == 0) {
			u4_t i, j;
			u4_t nthr = 0;
			char **ips;
			u4_t nips = 0;
			u8_t prev;
			nthr = 0;
			printf("Thread switch log (in cycles)\n");
			printf("thread      name\n");
			for (i = 0; i < profile_event_threadswitch_n_samples; i++) {
				for (j = 0; j < nthr; j++) {
					if (profile_event_threadswitch_samples[i].to == allthr[j])
						break;
				}
				if (j == nthr) {
					allthr[nthr] = profile_event_threadswitch_samples[i].to;
					if (allthr[nthr]->name != NULL) {
						printf("%d.%d %s\n", TID(allthr[nthr]), allthr[nthr]->name);
					} else {
						printf("%d.%d\n", TID(allthr[nthr]));
					}
					nthr++;
				}
			}
			printf("\n");
			ips = jxmalloc(sizeof(char *) * profile_event_threadswitch_n_samples * 2 MEMTYPE_OTHER);
			memset(ips, 0, sizeof(char *) * profile_event_threadswitch_n_samples * 2);
			for (i = 0; i < profile_event_threadswitch_n_samples; i++) {
				for (j = 0; j < nips; j++) {
					if (profile_event_threadswitch_samples[i].ip_from == ips[j])
						break;
				}
				if (j == nips) {
					ips[nips] = profile_event_threadswitch_samples[i].ip_from;
					nips++;
				}
			}
			for (i = 0; i < profile_event_threadswitch_n_samples; i++) {
				for (j = 0; j < nips; j++) {
					if (profile_event_threadswitch_samples[i].ip_to == ips[j])
						break;
				}
				if (j == nips) {
					ips[nips] = profile_event_threadswitch_samples[i].ip_to;
					nips++;
				}
			}
			for (i = 0; i < nips; i++) {
				printf("%p:", ips[i]);
				print_formatted_eip_info(ips[i]);
				printf("\n");
			}
			{
				SharedLibDesc *sharedLib;
				sharedLib = sharedLibs;
				printf("\n");
				//#ifdef KERNEL
				printf("core:%p:%p\n", _start, end);
				//#endif
				while (sharedLib != NULL) {
					printf("%s:%p:%p\n", sharedLib->name, sharedLib->code,
					       (char *) sharedLib->code + sharedLib->codeBytes);
					sharedLib = sharedLib->next;
				}
			}
			printf("\n");
			prev = 0;
			for (i = 0; i < profile_event_threadswitch_n_samples; i++) {
				ThreadDesc *t = profile_event_threadswitch_samples[i].to;
				ThreadDesc *t1 = profile_event_threadswitch_samples[i].from;
				u8_t ts = profile_event_threadswitch_samples[i].timestamp;
#if 0
				printf("0x%08x%08x %d.%d %p\n", (u4_t) (ts >> 32), (u4_t) (ts & 0xffffffff),
				       //t1,
				       TID(t),
				       //profile_event_threadswitch_samples[i].ip_from,
				       profile_event_threadswitch_samples[i].ip_to);
#endif
				printf("%20d     %d.%d\n", (u4_t) (ts - prev), TID(t));
				prev = ts;

			}
		} else if (strcmp("switchesonly", line) == 0) {
			u4_t i, j, nthr;
			char **ips;
			u4_t nips = 0;
			u8_t prev;
			nthr = 0;
			printf("Thread switch log (in cycles)\n");
			printf("thread      name\n");
			for (i = 0; i < profile_event_threadswitch_n_samples; i++) {
				for (j = 0; j < nthr; j++) {
					if (profile_event_threadswitch_samples[i].to == allthr[j])
						break;
				}
				if (j == nthr) {
					allthr[nthr] = profile_event_threadswitch_samples[i].to;
					if (allthr[nthr]->name != NULL) {
						printf("%d.%d %s\n", TID(allthr[nthr]), allthr[nthr]->name);
					} else {
						printf("%d.%d\n", TID(allthr[nthr]));
					}
					nthr++;
				}
			}
			printf("\n");
			prev = 0;
			for (i = 0; i < profile_event_threadswitch_n_samples; i++) {
				ThreadDesc *t = profile_event_threadswitch_samples[i].to;
				u8_t ts = profile_event_threadswitch_samples[i].timestamp;
				printf("%20d     %d.%d\n", (u4_t) (ts - prev), TID(t));
				prev = ts;

			}
		} else if (strcmp("rswitches", line) == 0) {
			rswitches();
#ifdef KERNEL
		} else if (strcmp("rswitchesonly", line) == 0) {
			//rswitchesNewFormat();
			int i, j;
			u8_t prev;
			int len = 0;
			char *data = jxmalloc(DATABLOCK + 1024 MEMTYPE_OTHER);
			u4_t *d = data;
			u4_t nthr = 0;
			prev = 0;
			printf("Expected number of blocks: %d\n", (profile_event_threadswitch_n_samples * 16) / DATABLOCK);
			console(5, "rswitchesonly");
			send_name("rswitchesonly");

			for (i = 0; i < profile_event_threadswitch_n_samples; i++) {
				for (j = 0; j < nthr; j++) {
					if (profile_event_threadswitch_samples[i].to == allthr[j])
						break;
				}
				if (j == nthr) {
					ASSERT(nthr < 50);
					allthr[nthr] = profile_event_threadswitch_samples[i].to;
					nthr++;
				}
			}
			wrint(&d, &len, nthr);
			for (i = 0; i < nthr; i++) {
				if (len > DATABLOCK) {
					write_data(data, len);
					len = 0;
					d = data;
				}
				wrint(&d, &len, allthr[i]->domain->id);
				wrint(&d, &len, allthr[i]->id);
				wrstring(&d, &len, allthr[i]->name);
			}

			wrint(&d, &len, profile_event_threadswitch_n_samples);

			for (i = 0; i < profile_event_threadswitch_n_samples; i++) {
				ThreadDesc *t;
				u8_t ts;

				if (len > DATABLOCK) {
					write_data(data, len);
					len = 0;
					d = data;
				}
				t = profile_event_threadswitch_samples[i].to;
				ts = profile_event_threadswitch_samples[i].timestamp;
				wrlong(&d, &len, ts - prev);
				wrint(&d, &len, t->domain->id);
				wrint(&d, &len, t->id);
				prev = ts;
			}
			if (len > 0) {
				write_data(data, len);
			}
			last_data();
			console(5, "done rswitchesonly");
#endif
#endif				/* PROFILE_EVENT_THREADSWITCH */
#endif				/* 0 */
		} else if (strncmp("b ", line, 2) == 0) {
			char className[80];
			char methodName[80];
			char signature[80];
			char bcPos[20];
			jint bc;
			char *addr;
			char *t = className;
			char *c = line + 2;
			while (*c != ':' && *c != '\0')
				*t++ = *c++;
			*t = '\0';
			c++;
			t = methodName;
			while (*c != '(' && *c != '\0')
				*t++ = *c++;
			*t = '\0';
			t = signature;
			while (*c != ':' && *c != '\0')
				*t++ = *c++;
			*t = '\0';
			c++;
			t = bcPos;
			while (*c != '\0')
				*t++ = *c++;
			*t = '\0';
			printf("!%s!%s!%s!%s!\n", className, methodName, signature, bcPos);
			bc = (jint) strtol(bcPos, NULL, 10);
			addr = (char *) findAddrOfMethodBytecode(className, methodName, signature, bc);
			if (addr == NULL) {
				printf("Method not found.\n");
			} else {
				breakpoint[n_breakpoints++].addr = addr;
				breakpoint[n_breakpoints++].save = *addr;
				*addr = 0xcc;	/* breakpoint interrupt machine code */
			}
#if defined( PROFILE_AGING) || defined(ZSTORE)
		} else if (strcmp("dpa", line) == 0) {
			//printProfileAging();
#endif
#ifdef PROFILE_GC
		} else if (strcmp("dpgc", line) == 0) {
			printProfileGC();
#endif
			// FIXME remove
		} else if (strcmp("rawt", line) == 0) {
			int c;
			for (c = 0; c < 256; c++)
				//printf("%d:#%c#\n", c, (char)c);
				printf("%03d#%c#\n", c, (char) c);
#ifdef BINARY_DATA_TRANSMISSION
		} else if (strcmp("bdt", line) == 0) {
			bdtStart();
			bdtStartChunk("COCA", 4, 4, 0);
			bdtSend("1234567890123456", 16, 1);
			bdtStartChunk("BOBA", 4, 4, 1);
			bdtSend("1234567890123456", 16, 1);
			bdtStop();
#endif
		} else if (strcmp("info", line) == 0) {
#ifdef TIMESLICING_TIMER_IRQ
			printf("timeslicing\n");
#endif
#ifdef EVENT_LOG
			printf("event log\n");
#endif
#ifdef SMP
			printf("SMP\n");
#endif
#ifdef PROFILE
			printf("profiler\n");
#endif
#ifdef PROFILE_HEAPUSAGE
			printf("profile heapusage\n");
#endif
#ifdef PROFILE_SAMPLE_HEAPUSAGE
			printf("sample heapusage\n");
#endif
#ifdef PROFILE_AGING
			printf("Profile Aging\n");
#endif
#ifdef PROFILE_GC
			printf("Profile Garbage Collector\n");
#endif
			printf("global memory objects\n");
#ifdef PROFILE_SAMPLE_PMC0
			printf("perfomance monitor counter 0\n");
#endif
#ifdef PROFILE_SAMPLE_PMC1
			printf("perfomance monitor counter 1\n");
#endif
#ifdef PROFILE_EVENT_THREADSWITCH
			printf("log thread switches\n");
#endif
		} else if (strcmp("config", line) == 0) {
			printf("System configuration:\n");
#ifdef CHECK_SERIAL
			printf("  check serial\n");
#endif
#ifdef CHECK_SERIAL_IN_PORTAL
			printf("  check serial in portal\n");
#endif
#ifdef CHECK_SERIAL_IN_TIMER
			printf("  check serial in timer\n");
#endif
#ifdef TIMESLICING_TIMER_IRQ
			printf("  Timeslicing with frequency %d HZ\n", TIMER_HZ);
#endif
#ifdef PROFILE_SAMPLE
			printf("  Sampling with slice %d sec and %d usec\n", TIMESLICE_SEC, TIMESLICE_USEC);
#endif
#ifdef DEBUG
			printf("  Debugging support enabled.\n");
#endif
#ifdef NO_DEBUG_OUT
			printf("  Debug channel disabled.\n");
#endif
#ifdef USE_QMAGIC
			printf("  Use magic numbers to protect data structures.\n");
#endif
#ifdef ENFORCE_FMA
			printf("  Enforce fast memory access.\n");
#endif
#ifdef ENABLE_GC
			printf("  GC enabled.\n");
#ifdef GC_NEW_IMPL
			printf("   %d = new gc\n", GC_IMPLEMENTATION_NEW);
#endif
#ifdef GC_COMPACTING_IMPL
			printf("   %d = compacting gc\n", GC_IMPLEMENTATION_COMPACTING);
#endif
#ifdef GC_BITMAP_IMPL
			printf("   %d = bitmap gc\n", GC_IMPLEMENTATION_BITMAP);
#endif
#ifdef GC_CHUNKED_IMPL
			printf("   %d = chunked gc\n", GC_IMPLEMENTATION_CHUNKED);
#endif
#else
			printf("  GC disabled.\n");
#endif
#ifdef NEW_COPY
			printf("  New portal param copy algorithm.\n");
#endif
#ifdef NEW_PORTALCALL
			printf("  New portal call algorithm (thread pooling).\n");
#endif
#ifdef DIRECT_SEND_PORTAL
			printf("  Direct send portal call algorithm (no client stub code).\n");
#endif
#ifdef NOTIFY_SERVICE_CLEANUP
			printf("  Call serviceFinalizer() method on service cleanup.\n");
#endif
#ifdef PROFILE
			printf("  Profiler activated.\n");
#endif
#ifdef JAVASCHEDULER
			printf("  Java scheduling.\n");
#else
			printf("  Core scheduling.\n");
#endif
#ifdef SMP
			printf("  SMP support.\n");
#endif
#ifndef KERNEL
			printf("  Emulation RAM: %d\n", total_ram);
#endif
#ifdef SERIAL_BAUD_115200
			printf("  Serialline: 115200 baud\n");
#else
#ifdef SERIAL_BAUD_9600
			printf("  Serialline: 9600 baud\n");
#else
			printf("  Serialline: 38400 baud\n");
#endif
//                      printf("  Stack Size: %d byte\n", STACK_CHUNK_SIZE);
#ifdef ALL_ARRAYS_32BIT
			printf("  All Arrays: 32 bit\n");
#else
#ifdef CHARS_8BIT
			printf("  Char Arrays: 8 bit\n");
#endif
#endif
#endif
		} else if (strcmp("help", line) == 0) {
			printf("config\t\t\t: Info about system configuration.\n");
			printf("threads\t\t\t: Info about all threads.\n");
			printf("domains\t\t\t: Info about domains.\n");
			printf("domain <domainID>\t: Info about domain <domain>.\n");
			printf("ns\t\t\t: Dump nameserver information.\n");
			printf("libs <domainID>\t\t: Info about libs of domain <domain>.\n");
			printf("slibs\t\t\t: Info about shared libraries.\n");
			printf("runq\t\t\t: Info about runqueue.\n");
			printf("thread <threadID|current>\t\t: Info about thread <addr>.\n");
			printf("stacktrace <addr>\t: prints the stacktrace (5 last frames) beginning at <addr>.\n");
			printf("stackdump <addr>\t: prints the next 10 values in memory beginnig at <addr>.\n");
			printf("dcode <domainID>\t\t: Dump all methods of domain <addr>.\n");
			printf("code <addr>\t\t: Find method at address <addr>.\n");
			printf("irq\t\t\t: Info about interrupts and interrupt handlers.\n");
			printf("minfo <methodname>\t: Info about a method.\n");
			printf("dep <depID>\t\t: Show waiting Senders of this Portal.\n");
			printf("ticks\t\t\t: Number of preemptions.\n");
			printf("gc\t\t\t: GC info.\n");
			printf
			    ("repl\t\t\t: Potential candidates for replace/inlining (Interfaces with only one implementing class).\n");
#ifdef SAMPLING_TIMER_IRQ
			printf("startsampling\t: Start sampling.\n");
			printf("stopsampling\t: Stop sampling.\n");
			printf("clearsamples\t: Clear sample data.\n");
#endif				/* SAMPLING_TIMER_IRQ */
#ifdef EVENT_LOG
			printf("log: show event log (in microseconds)\n");
			printf("logts: show event log (in cycles)\n");
			printf("events: transfer events in binary format (cycles)\n");
			printf("eventtypes: shows event types\n");
			printf("event <nr>: show specified event\n");
#endif
#ifdef LOG_PRINTF
			printf("printflog\t: transfers the printf log (binary data) \n");
#endif
#ifdef SMP
			printf("apic\t: Info about the IO_APIC\n");
#endif
			printf("c[ont]\t\t\t: Continue.\n");
#ifdef PROFILE_SAMPLE
			printf("samples: Dump instruction pointer samples sorted by method\n");
			printf("rawsamples: Dump raw data of instruction pointer samples\n");
#endif
#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE
			printf("rawsamples\t: Print eip samples\n");
#endif
#ifdef PROFILE
			printf("profile\t: start profiler\n");
#endif
#ifdef PROFILE_HEAPUSAGE
			printf("heap <domain>\t\t\t: show object distribuation of <domain>\n");
#endif
#ifdef PROFILE_SAMPLE_HEAPUSAGE
			printf("heapsample\t\t: show who allocated how much heap\n");
#endif
#ifdef PROFILE_AGING
			printf("dpa\t\t: Dump all 'Profile Aging' information. Binary data, beware of\n");
			printf("   \t\t  terminal corruption. Use only when wrapped (./bench/gc/wrap)!\n");
#endif
#ifdef PROFILE_GC
			printf("dpgc\t\t: Dump all 'Profile Garbage Collector' information.\n");
#endif
#ifdef DEBUG
			printf("handle\t\t\t: Dump all registered objects.\n");
#endif				/* DEBUG_HANDLE */
			printf("memobj\t\t\t: Dump all global memory objects.\n");
#ifdef DEBUG_MEMORY_CREATION
			printf("memobj <domain>\t: Dump all global memory objects allocated by domain <domain>.\n");
#endif				/*DEBUG_MEMORY_CREATION */
#ifdef PROFILE_SAMPLE_PMC0
			printf("pmc0sample\t: Dump perfomance monitor counter 0\n");
#endif
#ifdef PROFILE_SAMPLE_PMC1
			printf("pmc1sample\t: Dump perfomance monitor counter 1\n");
#endif
#if 0
#ifdef PROFILE_EVENT_THREADSWITCH
			printf("switches\t: Dump switches \n");
			printf("rswitches\t: Dump switches & pmc as binary data\n");
#endif
#endif				/*0 */
#ifdef BINARY_DATA_TRANSMISSION
			printf("bdt\t: transmit binary data\n");
#endif
			printf("b <class>:<method>(<params>)<return>:<bytecodepos>\t: Set breakpoint.\n");
			printf("x <address>\t: Examine memory at address.\n");
			printf("set[bwl] <address> <value>\t: set byte/word/long at memory address.\n");
#ifdef FIND_OBJECTS_BY_CLASS
			printf("dumpobj <domain> <class>\t: Find all objects of class <class> in domain <domain>\n");
#endif
#ifdef COPY_STATISTICS
			printf("copystat: print statistics about object copying across domain broders\n");
#endif				/* COPY_STATISTICS */
#ifndef KERNEL
			printf("ticks: print number of timer ticks\n");
#endif
			printf("q[uit]\t: Shutdown jx.\n");

			printf("Commands that interact with the Java level:\n");
#if defined(JAVA_MONITOR_COMMANDS)
			dump_javacommands();
#endif
		} else if (strcmp("quit", line) == 0 || strcmp("q", line) == 0) {
#ifndef KERNEL
#ifdef FRAMEBUFFER_EMULATION
			fb_shutdown();
#endif
#endif
			exit(0);
#if defined(JAVA_MONITOR_COMMANDS)
		} else if (exec_javacommand(line)) {
#endif
		} else {
			printf("Unknown command: %s\n", line);
		}
	}
#ifndef KERNEL
	RESTORE_IRQ;
#else
#  ifdef SMP
	spin_unlock(&in_monitor);
#  endif
#endif
#ifdef DEBUG
	check_current = 1;
#endif

}

#else				/*PRODUCTION */

void printStackTrace(char *prefix, ThreadDesc * thread, u4_t * base)
{
}
void printStackTraceNew(char *prefix)
{
}
void dumpThreadInfo(ThreadDesc * t)
{
}
void dump_data(ObjectDesc * o)
{
}
void dumpDomainInfo(DomainDesc * domain)
{
}

//void printCoreSymbolInformation(char *name) {}

#ifdef KERNEL
void monitor(struct irqcontext_timer *ctx)
{
	exit(2);
}
#else
void monitor(struct sigcontext *ctx)
{
	exit(2);
}
#endif

#ifndef KERNEL
void printTraceFromStoredCtx(char *prefix, ThreadDesc * thread, unsigned long *ctx)
{
}
#else
void printTraceFromStoredCtx(char *prefix, ThreadDesc * thread, unsigned long *ctx)
{
}
#endif

void dumpObject(ObjectDesc * o)
{
}


#endif				/*PRODUCTION */


#if (defined(DEBUGSUPPORT_DUMP) || defined(MONITOR)) && defined(KERNEL)

void wrint(u1_t ** data, u4_t * len, u4_t value)
{
	*(u4_t *) (*data) = value;
	*len += 4;
	*data += 4;
}

void wrlong(u1_t ** data, u4_t * len, u8_t value)
{
	*(u8_t *) (*data) = value;
	*len += 8;
	*data += 8;
}

void wrstring(u1_t ** data, u4_t * len, char *str)
{
	int i;
	if (str == NULL) {
		*(u4_t *) (*data) = 0;
		*len += 4;
		*data += 4;
		return;
	}
	*(u4_t *) (*data) = strlen(str);
	*len += 4;
	*data += 4;
	for (i = 0; i < strlen(str); i++) {
		*(u1_t *) (*data) = str[i];
		*len += 1;
		*data += 1;
	}
	if (strlen(str) & 1) {	/* pad to 2-byte border */
		*len += 1;
		*data += 1;
	}
}

u2_t monchecksum(u4_t len, u2_t * data)
{
	u4_t checksum = 0;
	len >>= 1;
	while (len--) {
		checksum += *(data++);
		if (checksum > 0xFFFF)
			checksum -= 0xFFFF;
	}
	return ((~checksum) & 0x0000FFFF);
}

#ifdef KERNEL

void send_name(char *name)
{
	char tmp[200];
	char *dptr;
	u4_t len = 0;
	dptr = tmp;
	wrstring(&dptr, &len, name);
	write_data(tmp, len);
}

void send_binary(char *name, u1_t * data, u4_t size)
{
	send_name(name);
	write_data(data, size);
	last_data();
}


#define SLOW_DOWN __asm__ __volatile__("outb %%al, $0x80": :)

static u4_t sequence = 0;
static u4_t oldseq = 0;
void write_data(char *data, u4_t len)
{
	u4_t checksum;
	u1_t c;
	int i;
	u4_t seq;
	char str[128];
	u4_t magic = 0xaaaaaaaa;

	printstr(str, len, 10);
	console(10, str);

	if (len & 1) {
		console(0, "PANIC");
		sys_panic("Data must be multiple of 2");
	}
	sequence++;

	if (data)
		checksum = monchecksum(len, data);
	for (;;) {
		console(0, "DUMP");
		printstr(str, sequence, 10);
		console(2, str);
		ser_dump(debug_port, &magic, 4);
		ser_dump(debug_port, &sequence, 4);
		ser_dump(debug_port, &len, 4);
		if (len > 0) {
			ser_dump(debug_port, data, len);
			ser_dump(debug_port, &checksum, 2);
		}
		console(0, "WAIT");
		ser_getdata(debug_port, &seq, 4);
		printstr(str, seq, 10);
		console(1, str);
		printstr(str, sequence, 10);
		console(2, str);
		/*if (oldseq+1 != seq) {
		   console(11, "???");
		   printstr(str, oldseq, 10);
		   console(12, str);
		   for(;;);
		   } */
		oldseq = seq;
		if (seq == sequence) {
			console(0, "OK");
			return;	/* OK */
		}
		console(0, "ERROR");
		for (i = 0; i < 1000; i++)
			ser_trygetchar(debug_port);
	}
}

void last_data()
{
	write_data(NULL, 0);
	sequence = 0;
}

#endif				/* KERNEL */
#else

#endif

void console(int line, char *msg)
{
#ifdef KERNEL
	u2_t *screen_start = (u2_t *) 0xb8000;
	int i;
	for (i = 0; i < strlen(msg); i++) {
		screen_start[i + 80 * line] = 0x0f00 | msg[i];
	}
	for (; i < 80; i++)
		screen_start[i + 80 * line] = 0x0f00;
#endif
}
