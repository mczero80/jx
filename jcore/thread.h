/********************************************************************************
 * Thread management
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#ifndef THREAD_H
#define THREAD_H

#include "misc.h"
#include "load.h"
#include "context.h"
#include "lock.h"
#include "profile.h"
#include "portal.h"
#include "smp.h"
#include "intr.h"

#define STATE_INIT      1
#define STATE_RUNNABLE  2
#define STATE_ZOMBIE    3
#define STATE_SLEEPING  5
#define STATE_WAITING   6
#define STATE_PORTAL_WAIT_FOR_RCV  7	/* waiting for portal thread to become available */
#define STATE_PORTAL_WAIT_FOR_SND  8	/* waiting for incoming portal call */
#define STATE_PORTAL_WAIT_FOR_RET  9	/* waiting for portal call to return */
#define STATE_BLOCKEDUSER 10
#define STATE_AVAILABLE   11
#define STATE_WAIT_ONESHOT   12
#define STATE_PORTAL_WAIT_FOR_PARAMCOPY 13	/* client waiting for portal param copy */
#define STATE_PORTAL_WAIT_FOR_RETCOPY   14	/* client waiting for portal return value copy */

#define THREAD_PPCB  0
#define THREAD_STATE 0x8

#ifndef ASSEMBLER
#include "lapic.h"

/* list of objects copied from caller to callee domain */
#ifndef NEW_COPY
/* list of objects copied from caller to callee domain */
struct copied_s {
    ObjectDesc * src;
    ObjectDesc * dst;
};
#else
/* list of objects copied from caller to callee domain (dst found with forwarding pointer) */
struct copied_s {
	ObjectDesc * src;
	u4_t flags;
};
#endif

struct StackProxy_s;

#define PORTAL_RETURN_TYPE_NUMERIC   0
#define PORTAL_RETURN_TYPE_REFERENCE 1
#define PORTAL_RETURN_TYPE_EXCEPTION 3

#define PORTAL_RETURN_IS_OBJECT(x) (x & 1)
#define PORTAL_RETURN_IS_EXCEPTION(x) (x == 3)
#define PORTAL_RETURN_SET_EXCEPTION(x) ((x) = 3)

typedef struct {
	u4_t dummy[5];
} SchedDescUntypedThreadMemory_t;

typedef struct ThreadDesc_s {
	ContextDesc *contextPtr;
	u4_t *stack;		/* pointer to lowest element in stack (small address) */
	u4_t *stackTop;		/* pointer to topmost element in stack (large address) */
	u4_t state;
	DomainDesc *domain;
#ifndef NEW_SCHED
	struct ThreadDesc_s *nextInRunQueue;
#endif
	struct ThreadDesc_s *nextInDEPQueue;
	ContextDesc context;
	jint *depParams;
	jint depMethodIndex;
//    jboolean depSwitchBack;    /* if true the PortalThread should switch back to the caller */ 
#ifdef CONT_PORTAL
	struct ThreadDesc_s *linkedDEPThr;	/* if thread is servicing a Portal  this is the Pointer to 
						   the original caller (transitiv)
						   else this points to the servicing Thread (transitiv)
						   if no portal is used, this points to the thread itself */
#endif
#ifdef NEW_PORTALCALL
	struct ThreadDesc_s *nextInReceiveQueue;
#endif
	struct ThreadDesc_s *nextInGCQueue;
#ifdef SMP
	jint curCpuId;		/* the ID of the last used CPU */
#endif
	jboolean isPortalThread;	/* this thread is a Portalthread */
	DEPDesc *processingDEP;	/* this thread is currently servicing a Portal */
	DomainDesc *blockedInDomain;	/* this thread is currently waiting for a service of that domain */
	u4_t  blockedInDomainID;	/* this thread is currently waiting for a service of that domain (detect terminated domain)*/
	u4_t blockedInServiceIndex;	/* index of the service this thread is waiting for */
	struct ThreadDesc_s * blockedInServiceThread;	/* service thread that executes our call 
							 * (needed to update mostRecentlyCalledBy during GC) */
	struct ThreadDesc_s *mostRecentlyCalledBy; /* needed to update the client thread pointer after svc exec */
	struct DomainDesc_s *callerDomain;
	u4_t callerDomainID;
	struct ThreadDesc_s *nextInDomain;
	struct ThreadDesc_s *prevInDomain;
	struct ThreadDesc_s *next;     /* general Pointer, used from Java-Level and Atomic Variable */

	jint wakeupTime;
	jint unblockedWithoutBeingBlocked;
	ObjectDesc *schedInfo;	/* may be used by a scheduler to store thread specific infos */
	jint debug[2];		/* two debug slots */
	SchedDescUntypedThreadMemory_t untypedSchedMemory;
	/* profiling infos */
#ifdef PROFILE
	ProfileDesc *profile;
#endif
#ifndef KERNEL
	sigset_t sigmask;
	jint preempted;
#endif
#ifdef USE_QMAGIC
	u4_t magic;
#endif
#ifdef JAVASCHEDULER
	DomainDesc *schedulingDomain;
#endif
	char name[THREAD_NAME_MAXLEN];
	ObjectDesc *portalParameter;	/* implizit parameter passed during a portal call 
					   can be used to pass credentials to target domain
					 */
	ObjectDesc *entry;	/* thread entry object containing run method */
	ObjectDesc *portalReturn;	/* may contain object reference or numeric data ! */
	jint portalReturnType;	/* 0=numeric 1=reference 3=exception */

	struct copied_s *copied;	/* onlu used when thread receives portal calls */
	u4_t n_copied;
	u4_t max_copied;
	u4_t isInterruptHandlerThread;
	u4_t isGCThread;
	jint *myparams;		/* FIXME */
	u4_t id;
#ifdef STACK_ON_HEAP
	struct StackProxy_s *stackObj;
#endif
#ifdef CPU_USAGE_STATISTICS
	u4_t numberPreempted;
	u8_t cputime;
#endif				/* CPU_USAGE_STATISTICS */
} ThreadDesc;

#ifdef USE_QMAGIC
#define MAGIC_THREAD 0xcabaaffe
#endif
#if defined (NORMAL_MAGIC) && defined(USE_QMAGIC)
#define ASSERTTHREAD(x) ASSERT(x->magic==MAGIC_THREAD)
#else
#define ASSERTTHREAD(x)
#endif

typedef struct ThreadDescProxy_s {
	code_t *vtable;
	ThreadDesc desc;
} ThreadDescProxy;

typedef struct ThreadDescForeignProxy_s {
	code_t *vtable;
	DomainProxy *domain; /* used to find domain */
	int threadID;        /* used to find thread if thread pointer is invalid */
	ThreadDesc *thread;  /* used to find thread fast; is only valid if gcEpoch is equal to gc epoch of target domain */
	int gcEpoch;         /* GC epoch of target domain during creation of this object */
} ThreadDescForeignProxy;

#ifdef ENABLE_MAPPING
typedef struct MappedMemoryProxy_s {
	code_t *vtable;
	char *mem;
} MappedMemoryProxy;
#endif /* ENABLE_MAPPING */

#ifdef STACK_ON_HEAP
typedef struct StackProxy_s {
	code_t *vtable;
	u4_t size; /* in words */
	ThreadDesc *thread; /* really needed?*/
	/* char stack[...]; */
} StackProxy;
#endif

#ifdef PROFILE_EVENT_THREADSWITCH
struct profile_event_threadswitch_s {
	unsigned long long timestamp;
	ThreadDesc *from;
	ThreadDesc *to;
	char *ip_from;
	char *ip_to;
};
u4_t do_event_threadswitch;
extern struct profile_event_threadswitch_s
    *profile_event_threadswitch_samples;
extern u4_t profile_event_threadswitch_n_samples;
#ifndef MAX_EVENT_THREADSWITCH
#define MAX_EVENT_THREADSWITCH 2000000
#endif
#endif				/* PROFILE_EVENT_THREADSWITCH */

typedef void (*thread_start_t) (void *);


// Prototypes
void threads_init();
ThreadDesc *createThread(DomainDesc * domain, thread_start_t thread_start,
			 void *param, int state, int schedParam);
ThreadDesc *createThreadUsingThreadEntry(DomainDesc * domain,
					 ObjectDesc * entry);
ThreadDesc *createInitialDomainThread(DomainDesc * domain, int state, int schedParam);
ThreadDesc *createThreadInMem(DomainDesc * domain, thread_start_t thread_start, void *param, ObjectDesc * entry, u4_t stackSize, int state, int schedParam);
void receive_dep(void *arg);
void receiveDomainDEP(void *arg);
void threadyield();
void threadunblock();
void locked_threadunblock();
void threadblock();
void locked_threadblock();
void threadreceive(Proxy * domainZeroProxy, Proxy * depProxy);
void threadswitchto(Proxy * domainZeroProxy, ThreadDesc * nextthread);
void print_eip_info(char *addr);
#ifdef SMP
void smp_idle_threads_init();
#endif
void thread_exit();
void terminateThread(ThreadDesc * t);

ThreadDesc *findThreadDesc(ThreadDescForeignProxy *proxy);

// FIXME jgbauman
u4_t start_thread_using_code1(ObjectDesc * obj, ThreadDesc * thread,
			      code_t c, u4_t param);

#ifndef DEBUG
extern ThreadDesc *__current[MAX_NR_CPUS];
static inline ThreadDesc *curthr()
{
	return __current[get_processor_id()];
}
static inline ThreadDesc **curthrP()
{
	return &__current[get_processor_id()];
}
static inline void set_current(ThreadDesc * t)
{
	__current[get_processor_id()] = t;
}
static inline DomainDesc *curdom()
{
	return curthr()->domain;
}
#else
extern int check_current;
ThreadDesc *curthr();
ThreadDesc **curthrP();
void set_current(ThreadDesc * t);
DomainDesc *curdom();
#endif

extern ThreadDesc *__idle_thread[MAX_NR_CPUS];
static inline ThreadDesc *cur_idle_thread(void)
{
	return __idle_thread[get_processor_id()];
}

#define idle_thread cur_idle_thread()

#define LOCK(obj) { register int reg = 1; for(;;) {__asm__ __volatile__ ("xchgl %1,%0" :"=r" (reg), "=m" (*obj) :"r" (reg), "m" (*obj)); if (reg == 0) break; }}

#define UNLOCK(obj) { register int reg = 0;        __asm__ __volatile__ ("xchgl %1,%0" :"=r" (reg), "=m" (*obj) : "r" (reg), "m" (*obj));}


/* domainID.threadID list for printf output */
#define TID(t) (t)->domain->id, (t)->id


#ifndef DEBUG
static inline void check_not_in_runq(ThreadDesc * thread)
{
}
#endif

#ifdef TRACESCHEDULER
extern u4_t *trace_sched_ip;
extern u4_t *last_trace_sched_ip;
#endif

jint switch_to(ThreadDesc ** current, ThreadDesc * to);
jint switchfully_to(ThreadDesc ** current, ThreadDesc * to);
char *get_state(ThreadDesc * t);
jint internal_switch_to(ThreadDesc ** current, ThreadDesc * to);

#ifndef KERNEL
void disable_irq(sigset_t * set, sigset_t * oldset);
#endif


#ifdef KERNEL
void save(struct irqcontext *sc);
#else
void save(struct sigcontext *sc);
#endif

static inline void stack_push(u4_t ** sp, u4_t data)
{
	(*sp)--;
	**sp = data;
}



/*
 * Read the 64-bit timestamp counter (TSC) register.
 * Works only on Pentium and higher processors,
 * and in user mode only if the TSD bit in CR4 is not set.
 */
#if HAVE_RDTSC
#define get_tsc() \
    ({ \
	unsigned long low, high; \
	asm volatile("rdtsc" : "=d" (high), "=a" (low)); \
	((unsigned long long)high << 32) | low; \
    })
#define rdtsc(val) asm volatile ("rdtsc":"=A" (val));
#else
#define get_tsc() \
    ({ \
	unsigned long low, high; \
	asm volatile( \
	".byte 0x0f; .byte 0x31" \
	: "=d" (high), "=a" (low)); \
	((unsigned long long)high << 32) | low; \
    })
#define rdtsc(val)
#endif

#endif				/* ASSEMBLER */




#endif
