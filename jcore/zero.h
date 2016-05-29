/********************************************************************************
 * DomainZero
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifndef ZERO_H
#define ZERO_H

#include "load.h"
#include "smp.h"
#include "irq.h"
#include "thread.h"
#include "zero_Memory.h"



extern jint gc_methodindex;

extern ObjectDesc *memoryManagerInstance;
extern DEPDesc *ihandlers[NUM_IRQs];
extern DomainDesc *idomains[MAX_NR_CPUS][NUM_IRQs];
extern ThreadDesc *ithreads[MAX_NR_CPUS][NUM_IRQs];
extern u4_t imissed[NUM_IRQs];
extern u4_t idelayed[NUM_IRQs];
extern u4_t iprocessed[MAX_NR_CPUS][NUM_IRQs];
extern ObjectDesc *ifirstlevel_object[MAX_NR_CPUS][NUM_IRQs];
extern u4_t ifirstlevel_happened[MAX_NR_CPUS][NUM_IRQs];
extern u4_t ifirstlevel_processed[MAX_NR_CPUS][NUM_IRQs];
extern struct irqInfos iInfos[NUM_IRQs];

struct nameValue_s {
	Proxy *obj;
	char *name;
	struct nameValue_s *next;
};
extern struct nameValue_s *nameValue;

#ifdef EVENT_LOG

typedef struct EventLog_s {
	jint number;
	u4_t info;
	u4_t info2;
	u8_t timestamp;
} EventLog;

//extern EventLog events[MAX_EVENTS];
extern EventLog *events;
extern u4_t n_events;
extern jint n_event_types;
extern char eventTypes[MAX_EVENT_TYPES][MAX_EVENT_TYPE_SIZE];


#define RECORD_EVENT(nr)\
  DISABLE_IRQ;\
  if (nr >= n_event_types) domain_panic(curdom(), "event number out of range");\
  if (n_events == MAX_EVENTS) domain_panic(curdom(), "too many events");\
  events[n_events].number = nr;\
  rdtsc(events[n_events].timestamp);\
  n_events++;\
  RESTORE_IRQ;

#define RECORD_EVENT_INFO(__nr__,__info__)\
  DISABLE_IRQ;\
  if ((__nr__) >= n_event_types) domain_panic(curdom(), "event number out of range");\
  if (n_events == MAX_EVENTS) domain_panic(curdom(), "too many events");\
  events[n_events].number = (__nr__);\
  events[n_events].info = (__info__);\
  events[n_events].timestamp = get_tsc();\
  n_events++;\
  RESTORE_IRQ;

#define RECORD_EVENT_INFO2(__nr__,__info__,__info2__)\
  DISABLE_IRQ;\
  if ((__nr__) >= n_event_types) domain_panic(curdom(), "event number out of range");\
  if (n_events == MAX_EVENTS) domain_panic(curdom(), "too many events");\
  events[n_events].number = (__nr__);\
  events[n_events].info = (__info__);\
  events[n_events].info2 = (__info2__);\
  rdtsc(events[n_events].timestamp);\
  n_events++;\
  RESTORE_IRQ


#endif				/* EVENT_LOG */

extern code_t extern_panic;

/***************/
/* conversions */

/* CPUState */

static inline ObjectDesc *thread2CPUState(ThreadDesc * thread)
{
	if (thread == NULL)
		return NULL;
	return (ObjectDesc *) (((u4_t *) thread) - 1);
}

static inline ThreadDesc *cpuState2thread(ObjectDesc * obj)
{
	if (obj == NULL)
		return NULL;
	//return (ThreadDesc *) (((u4_t *) obj) + 1);
	if ((getObjFlags(obj) & FLAGS_MASK) == OBJFLAGS_CPUSTATE) {
		return &(((ThreadDescProxy *)obj)->desc);
	} else if ((getObjFlags(obj) & FLAGS_MASK) == OBJFLAGS_FOREIGN_CPUSTATE) {
		return findThreadDesc((ThreadDescForeignProxy*)obj);
	} else {
		sys_panic("cpustate object has wrong flags");
	}
	return NULL;
}


#ifdef STACK_ON_HEAP
/* Stack */

static inline StackProxy *stack2Obj(char *stack)
{
	if (stack == NULL)
		return NULL;
	return (ObjectDesc *) (((u4_t *) stack) - 3); /* vtable,size,tcb*/
}

static inline char *obj2stack(StackProxy * obj)
{
	if (obj == NULL)
		return NULL;
	return (char*)(((u4_t*)obj)+3); /* vtable,size,tcb*/
}
#endif /* STACK_ON_HEAP */

/* CPU */

static inline ObjectDesc *cpuDesc2Obj(CPUDesc * cpu)
{
	return (ObjectDesc *) (((u4_t *) cpu) - 1);
}

static inline CPUDesc *obj2cpuDesc(ObjectDesc * obj)
{
	//  ASSERTOBJECT(obj);
	return (CPUDesc *) (((u4_t *) obj) + 1);
}

/* Domain */

static inline ObjectDesc *domainDesc2Obj(DomainDesc * domain)
{
#if 0
	ASSERTDOMAIN(domain);
	return (ObjectDesc *) (((u4_t *) domain) - 1);
#endif
	sys_panic("Domain portals are no longer direct portals.");
	return NULL;
}

static inline DomainDesc *obj2domainDesc(ObjectDesc * obj)
{
#if 0
	return (DomainDesc *) (((u4_t *) obj) + 1);
#endif
	sys_panic("Domain portals are no longer direct portals.");
	return NULL;
}


/**
 * Class -> jx/zero/VMClass (Object)
 */
static inline ObjectDesc *class2Obj(Class * cl)
{
	return (ObjectDesc *) & (cl->objectDesc_vtable);
}

/**
 * jx/zero/VMClass (Object) -> Class
 */
static inline Class *obj2class(ObjectDesc * obj)
{
	return (Class *) (((u4_t *) obj) - 1 - XMOFF);
}

/**
 * Method -> jx/zero/VMMethod (Object)
 */
static inline ObjectDesc *method2Obj(MethodDesc * m)
{
	return (ObjectDesc *) & (m->objectDesc_vtable);
}

/**
 * jx/zero/VMMethod (Object) -> Method
 */
static inline MethodDesc *obj2method(ObjectDesc * obj)
{
	return (MethodDesc *) (((u4_t *) obj) - 1 - XMOFF);
}



/*************/
/* functions */

void init_irq_data(void);
jboolean debug_init(int argc, char *argv[]);

DEPTypeDesc *findDEPByType(char *name);
void init_zero_from_lib(DomainDesc * domain, SharedLibDesc * zeroLib);
ObjectDesc *getDomainZeroInstance();
Proxy *getDomainZeroProxy();

ClassDesc *createObjectClassDesc();
Class *createObjectClass(ClassDesc * java_lang_Object);
ClassDesc *createArrayObjectClassDesc(DomainDesc * domain);
Class *createArrayObjectClass(DomainDesc * domain,
			      ClassDesc * java_lang_Array);
Class *obj2class(ObjectDesc * obj);

void createArrayObjectVTableProto(DomainDesc * domain);

u4_t init_zero_dep_from_class_without_thread(ClassDesc * cd,
					     char *depname);

ClassDesc *init_zero_class(char *ifname, MethodInfoDesc * methods,
			   jint size, jint instanceSize, jbyte * typeMap,
			   char *subname);

jint findDEPMethodIndex(DomainDesc * domain, char *className,
			char *methodName, char *signature);
jint findZeroLibMethodIndex(DomainDesc * domain, char *className,
			    char *methodName, char *signature);

void SMPcpuManager_register_Scheduler(ObjectDesc * self, ObjectDesc * cpu,
				      ObjectDesc * new_sched);
void SMPcpuManager_register_LLScheduler(ObjectDesc * self,
					ObjectDesc * cpu,
					ObjectDesc * new_sched);

u4_t *specialAllocCode(DomainDesc * domain, int completeCodeBytes);
void installObjectVtable(ClassDesc * c);

ArrayDesc *vmSpecialAllocArray(ClassDesc * elemClass0, jint size);

void switchCPUState(ThreadDesc * oldctx, ThreadDesc * newctx);	/*??? */

void installInitialNaming(DomainDesc * srcDomain, DomainDesc * dstDomain,
			  Proxy * naming);
Proxy *getDomainZeroNaming();
Proxy *getInitialNaming();





/* in zero_DomainManager */
DomainProxy *domainManager_createDomain(ObjectDesc * self,
					ObjectDesc * dname,
					ArrayDesc * cpuObjs,
					ArrayDesc * HLSNames,
					ObjectDesc * dcodeName,
					ArrayDesc * libsName,
					ObjectDesc * startClassName,
					jint gcinfo0, jint gcinfo1, jint gcinfo2, ObjectDesc* gcinfo3, jint gcinfo4,
					jint codeSize,
					ArrayDesc * argv,
					ObjectDesc * naming,
					ArrayDesc * portals,
					jint gcImpl,
					ArrayDesc *schedinfo);

#endif				/* ZERO_H */
