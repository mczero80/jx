/********************************************************************************
 * Domain-management related data structures
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifndef DOMAIN_H
#define DOMAIN_H

#include "lock.h"

#define LOCK_DOMAINS           DISABLE_IRQ
#define UNLOCK_DOMAINS         RESTORE_IRQ

struct ThreadDesc_s;
struct HLSchedDesc_s;		//form scheduler.h

#include "portal.h"
#include "smp.h"
#include "malloc.h"
#include "object.h"

#define DOMAIN_TERMINATED_EXCEPTION  (-1)

#define DOMAIN_STATE_FREE        0
#define DOMAIN_STATE_CREATING    1
#define DOMAIN_STATE_ACTIVE      2
#define DOMAIN_STATE_TERMINATING 3
#define DOMAIN_STATE_FREEZING    4
#define DOMAIN_STATE_FROZEN      5
#define DOMAIN_STATE_THAWING     6
#define DOMAIN_STATE_ZOMBIE      7


/* 
 * GC related data structures 
 */

# ifdef PROFILE_HEAPUSAGE
struct InstanceCounts_s {
	jint objbytes;
	jint arrbytes;
	jint portalbytes;
	jint memproxybytes;
	jint cpustatebytes;
	jint atomvarbytes;
	jint servicebytes;
	jint servicepoolbytes;
	jint casbytes;
	jint tcbbytes;
	jint stackbytes;
};
typedef struct InstanceCounts_s InstanceCounts_t;
# endif

struct DomainDesc_s;

typedef struct {
	u4_t dummy[30];
} GCDescUntypedMemory_t;

typedef void (*HandleObject_t) (struct DomainDesc_s * domain,
				ObjectDesc * obj, u4_t objSize,
				u4_t flags);

typedef u4_t *(*HandleReference_t) (struct DomainDesc_s *, struct ObjectDesc_s **);


typedef struct GCDesc_s {
	/* private part -- for each implementation only */
	GCDescUntypedMemory_t untypedMemory;

	/* protected part -- for garbage collectors & management only */
	ObjectDesc *gcObject;
	struct ThreadDesc_s *gcThread;
	code_t gcCode;
	void *data;

	ObjectDesc **registeredObjects;	/* add these objects to the root set */
#ifdef DEBUG_HANDLE
	code_t *registrationPoints;	/* instruction pointer of register call (debugging) */
	code_t *registrationPoints1;	/* instruction pointer of register call (debugging) */
	code_t *registrationPoints2;	/* instruction pointer of register call (debugging) */
#endif
	// jint numberOfRegistered;   /* number of registered objects */
#ifdef MEASURE_GC_TIME
	u8_t gcTime;
	u4_t gcRuns;
	u4_t gcBytesCollected;
#endif

#ifdef PROFILE_AGING
	jlong memTime;
#endif

	u4_t epoch;                     /* GC epoch; incremented during GC run */

#ifndef GC_USE_ONLY_ONE
	 ObjectHandle(*allocDataInDomain) (struct DomainDesc_s * domain,
					   int objsize, u4_t flags);
	void (*done) (struct DomainDesc_s * domain);
	void (*gc) (struct DomainDesc_s * domain);
	// FIXME
	void (*finalizeMemory) (struct DomainDesc_s * domain);
	void (*finalizePortals) (struct DomainDesc_s * domain);
	 jboolean(*isInHeap) (struct DomainDesc_s * domain,
			      ObjectDesc * obj);
	 jboolean(*ensureInHeap) (struct DomainDesc_s * domain,
			      ObjectDesc * obj);
	void (*walkHeap) (struct DomainDesc_s * domain,
			  HandleObject_t handleObject,
			  HandleObject_t handleArray,
			  HandleObject_t handlePortal,
			  HandleObject_t handleMemory,
			  HandleObject_t handleService,
			  HandleObject_t handleCAS,
			  HandleObject_t handleAtomVar,
			  HandleObject_t handleDomainProxy,
			  HandleObject_t handleCPUStateProxy,
			  HandleObject_t handleServicePool,
			  HandleObject_t handleStackProxy);

	 u4_t(*freeWords) (struct DomainDesc_s * domain);
	 u4_t(*totalWords) (struct DomainDesc_s * domain);
	void (*printInfo) (struct DomainDesc_s * domain);

#ifdef NEW_COPY
	/* support for portal param copy */
	void (*setMark)(struct DomainDesc_s *domain);
	ObjectDesc* (*atMark)(struct DomainDesc_s *domain);
#endif
#endif
} GCDesc;


/*
 * Scheduling related data structures
 */

typedef struct {
	u4_t dummy[5];
} SchedDescUntypedMemory_t;

typedef struct {
	u4_t dummy[5+20];
} GlobalSchedDescUntypedMemory_t;

typedef struct SchedDesc_s {
	/* private part -- for each implementation only */
	GlobalSchedDescUntypedMemory_t untypedGlobalMemory;
	SchedDescUntypedMemory_t untypedLocalMemory;

	u4_t currentThreadID;

	/* function table: local -> global */
	 u4_t(*becomesRunnable) (struct DomainDesc_s * domain);
} SchedDesc;

/*
 * DomainDesc
 */
typedef struct DomainDesc_s {
#ifdef USE_QMAGIC
	u4_t magic;
#endif
	jint maxNumberOfLibs;
	jint numberOfLibs;
	LibDesc **libs;		/* code used by this domain */
#ifdef USE_LIB_INDEX
	LibDesc **ndx_libs;	/* system width libindex-list */
	jint **sfields;
#endif

	ArrayClassDesc *arrayClasses;

	char *codeBorder[CODE_FRAGMENTS];	/* pointer to border of code segment (last allocated word  + 1) */
	char *code[CODE_FRAGMENTS];	/* all code lifes here */
	char *codeTop[CODE_FRAGMENTS];	/* pointer to free code space */
	s4_t cur_code;		/* current code chunk */
	s4_t code_bytes;	/* max code memory  */

	char *domainName;
	/* --- */
	GCDesc gc;
	/* --- */
	SchedDesc sched;
	CPUDesc *cpu[MAX_NR_CPUS];	/* CPU Object(s) of this domain */
#ifdef JAVASCHEDULER
	ArrayDesc *HLSNames;	/* class Names of the HLS(s) */
	struct HLSchedDesc_s *Scheduler[MAX_NR_CPUS];	/*  HighLevelScheduler(s) of this domain */
#endif
	jint irqHandler_interruptHandlerIndex;

	int advancingThreads;

#ifdef CPU_USAGE_STATISTICS
	u4_t preempted;
	u8_t cputime;
#endif				/* CPU_USAGE_STATISTICS */
	struct ThreadDesc_s *threads;
	/* --- */
	ObjectDesc *startClassName;
	ObjectDesc *dcodeName;
	ArrayDesc *libNames;
	ArrayDesc *argv;
	ArrayDesc *initialPortals;
	DEPDesc *services[MAX_SERVICES];
#ifdef NEW_PORTALCALL
	ServiceThreadPool *pools[MAX_SERVICES];
#endif
	int state;
	u4_t id;
#ifndef NEW_SCHED
 	struct ThreadDesc_s *firstThreadInRunQueue;
 	struct ThreadDesc_s *lastThreadInRunQueue;
 	struct DomainDesc_s *nextInRunQueue;
#endif
#if defined(PORTAL_INTERCEPTOR) || defined(PORTAL_TRANSFER_INTERCEPTOR)
	struct ThreadDesc_s *outboundInterceptorThread;
	code_t outboundInterceptorCode;
	ObjectDesc *outboundInterceptorObject;
	struct ThreadDesc_s *inboundInterceptorThread;
	code_t inboundInterceptorCode;
	ObjectDesc *inboundInterceptorObject;
	struct ThreadDesc_s *portalInterceptorThread;
	code_t createPortalInterceptorCode;
	code_t destroyPortalInterceptorCode;
	ObjectDesc *portalInterceptorObject;
	jboolean memberOfTCB;
#endif				/* PORTAL_INTERCEPTOR */
	u4_t libMemSize;
	ObjectDesc *naming;	/* the global naming portal used inside this domain */
	u4_t memoryObjectBytes;	/* memory allocated as memory objects */
	u4_t memusage;		/* total memory usage counted in jxmalloc */
	Proxy *initialNamingProxy;	/* Naming proxy that can be obtained by calling InitialNaming.getInitialNaming() */
	char *scratchMem;	/* memory to store temporary data; use only when no other domain activity (example use: portal parameter copy) */
	u4_t scratchMemSize;	/* size of scratch memory */
	u4_t inhibitGCFlag;	/* 1==no GC allowed; 0==GC allowed */
#ifdef PORTAL_STATISTICS
	u4_t portal_statistics_copyout_rcv;
	u4_t portal_statistics_copyin_rcv;
#endif				/* PORTAL_STATISTICS */
} DomainDesc;

#define SERVICE_ENTRY_FREE     ((DEPDesc*)0x00000000)
#define SERVICE_ENTRY_CHANGING ((DEPDesc*)0xffffffff)


#ifdef USE_QMAGIC
#define MAGIC_DOMAIN 0xd0d0eeee
#endif
#if defined (USE_QMAGIC) && defined (NORMAL_MAGIC)
#define ASSERTDOMAIN(x) ASSERT(x->magic==MAGIC_DOMAIN)
#else
#define ASSERTDOMAIN(x)
#endif

typedef void (*domain_f) (DomainDesc *);
typedef void (*domain1_f) (DomainDesc *, void *);


void init_domainsys();
DomainDesc *createDomain(char *domainName, jint gcinfo0, jint gcinfo1, jint gcinfo2, char *gcinfo3, jint gcinfo4,
			 u4_t code_bytes, int gcImpl, ArrayDesc* schedinfo);
jint getNumberOfDomains();
void domain_panic(DomainDesc * domain, char *msg, ...);

#ifdef MONITOR
void foreachDomain(domain_f func);
void foreachDomain1(domain1_f func, void *arg);
#  ifdef CHECK_RUNNABLE_IN_RUNQ
void foreachDomainRUNQ(domain_f func);
#  endif
#endif /*MONITOR*/
char **malloc_tmp_stringtable(DomainDesc * domain, TempMemory * mem,
			      u4_t number);


// FIXME jgbauman
int findMethodAtAddrInDomain(DomainDesc * domain, u1_t * addr,
			     MethodDesc ** method, ClassDesc ** classInfo,
			     jint * bytecodePos, jint * lineNumber);
#endif				/* DOMAIN_H */
