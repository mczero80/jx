/********************************************************************************
 * Garbage collector
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Joerg Baumann
 *******************************************************************************/

#ifdef ENABLE_GC
#include "all.h"

//FIXME
//#include "gc_memcpy.h"
#include "gc_move.h"
#include "gc_pa.h"
#include "gc_pgc.h"
#include "gc_common.h"
#include "gc_thread.h"
#include "gc_stack.h"
#include "gc_impl.h"

/*
 * freeze the threads in domain at known checkpoints, so we know the stackmaps 
 */
void freezeThreads(DomainDesc * domain)
{
	ThreadDesc *thread;

	IF_DBG_GC(printf("checking thread Positions ...\n"));
	for (thread = domain->threads; thread != NULL; thread = thread->nextInDomain) {
		if (thread == domain->gc.gcThread)
			continue;	/* don't scan my own stack */
		if (thread->isInterruptHandlerThread)
			continue;	/* don't scan interrupt stacks, they can not be interrupted by a GC and so they are not active */
		if (thread->state == STATE_AVAILABLE)
			continue;	/* don't scan stack of available threads */
		check_thread_position(domain, thread);
	}
}

#ifdef JAVASCHEDULER
/*
 * visit all scheduler objects for thsi domain
 */
static void walkSchedulerObjects(DomainDesc * domain, HandleReference_t handler)
{
	u4_t i;

	for (i = 0; i < MAX_NR_CPUS; i++)
		if (domain->Scheduler[i] != NULL)
			if (domain->Scheduler[i]->SchedObj != NULL)
				if (domain == domain->Scheduler[i]->SchedThread->domain) {
					gc_dprintf("move HLScheduler %d\n", i);
					handler(domain, &(domain->Scheduler[i]->SchedObj));
				}
}
#endif

/* 
 * visit all static refernces in class cl 
 */
static void walkClass(DomainDesc * domain, Class * cl, HandleReference_t handler)
{
	ClassDesc *c = cl->classDesc;

#ifdef DBG_GCSTATIC
	if (c->staticFieldsSize) {
		printf("   scan class %s\n", cl->classDesc->name);
	}
	FORBITMAP(c->staticsMap, c->staticFieldsSize, {
		  /* reference slot */
		  printf("      %d found ref %p\n", index, cl->staticFields[index]);
		  handleReference(domain, (ObjectDesc **) & (cl->staticFields[index]));
		  }
		  , {
		  printf("      %d numeric\n", index);
		  }
	);
#else
	FORBITMAP(c->staticsMap, c->staticFieldsSize,	/* reference slot */
		  handler(domain, (ObjectDesc **) & (cl->staticFields[index])), {
		  });
#endif				/*DBG_GCSTATIC */
}

/* visit all references on the stacks for this domain*/
void walkStacks(DomainDesc * domain, HandleReference_t handler)
{
	ThreadDesc *thread;

	PGCB(STACK);
	IF_DBG_GC(printf("Scanning stacks...\n"));
	for (thread = domain->threads; thread != NULL; thread = thread->nextInDomain) {
		if (thread == domain->gc.gcThread)
			continue;	/* don't scan my own stack */
		if (thread->isInterruptHandlerThread)
			continue;	/* don't scan interrupt stacks, they can not be interrupted by a GC and so they are not active */
		if (thread->state == STATE_AVAILABLE)
			continue;	/* don't scan stack of available threads */
		walkStack(domain, thread, handler);
	}
	PGCE(STACK);
}

/*
 * visit all static references for this domain
 */
void walkStatics(DomainDesc * domain, HandleReference_t handler)
{
	u4_t i, k;
	LibDesc *lib;

	PGCB(STATIC);
	IF_DBG_GC(printf("Scanning classes...\n"));
	for (k = 0; k < domain->numberOfLibs; k++) {
		lib = domain->libs[k];
		for (i = 0; i < lib->numberOfClasses; i++) {
			walkClass(domain, &(lib->allClasses[i]), handler);
		}
	}
	PGCE(STATIC);
}

/* 
 * visit all portals for this domain 
 */
void walkPortals(DomainDesc * domain, HandleReference_t handler)
{
	u4_t i;
	DEPDesc *d;
	//printf("MOVE PORTALS\n");

	PGCB(SERVICE);
	IF_DBG_GC(printf("Scanning portals...\n"));
	/* TODO: perform GC on copy of service table and use locking only to reinstall table 
	 * all entries of original table must be marked as changing
	 */
	LOCK_SERVICETABLE;
	for (i = 0; i < MAX_SERVICES; i++) {
		d = domain->services[i];
		if (d == SERVICE_ENTRY_FREE || d == SERVICE_ENTRY_CHANGING)
			continue;
#ifndef SERVICE_EAGER_CLEANUP
		if (domain->services[i]->refcount == 1) {
			//printf("DELETE SERVICE %s\n", obj2ClassDesc(domain->services[i]->obj)->name);
			/* delete service (service object will stay on the heap as garbage) */
			domain->services[i]->refcount = 0;
			terminateThread(domain->services[i]->receiver);	/* thread is not in any queue (receiver is idle) */
			domain->services[i] = SERVICE_ENTRY_FREE;
			continue;
		}
#else
		ASSERT(domain->services[i]->refcount > 1);
#endif
		handler(domain, (ObjectDesc **) & (domain->services[i]));
	}
#ifdef NEW_PORTALCALL
	for (i = 0; i < MAX_SERVICES; i++) {
		if (domain->pools[i]) {
			//printf("MOVE POOL %d\n", i);
			handler(domain, (ObjectDesc **) & (domain->pools[i]));
		}
	}
#endif				/* NEW_PORTALCALL */
	UNLOCK_SERVICETABLE;
	PGCE(SERVICE);
}

/*
 * visit all registered objects (= in use by C core) for this domain 
 */
void walkRegistereds(DomainDesc * domain, HandleReference_t handler)
{
	u4_t i;

	PGCB(REGISTERED);
	IF_DBG_GC(printf("Scanning registered...\n"));
	for (i = 0; i < MAX_REGISTERED; i++) {
		if (domain->gc.registeredObjects[i] != NULL) {
			printf("register obj in domain %ld object %p\n", domain->id, &(domain->gc.registeredObjects[i]));
			handler(domain, &(domain->gc.registeredObjects[i]));
		}
	}

	PGCE(REGISTERED);
}

/*
 * visit all special objects in this domain for this domain 
 */
#ifdef TIMER_EMULATION
extern AtomicVariableProxy *atomic;
#endif
void walkSpecials(DomainDesc * domain, HandleReference_t handler)
{
	ThreadDesc *t;

	PGCB(SPECIAL)
	    IF_DBG_GC(printf("Scanning special...\n"));
	/*if (domain->gc.gcObject != NULL)
	   handler(domain, (ObjectDesc **) & (domain->gc.gcObject)); */
	if (domain->startClassName)
		handler(domain, (ObjectDesc **) & (domain->startClassName));
	if (domain->dcodeName != NULL)
		handler(domain, (ObjectDesc **) & (domain->dcodeName));
	if (domain->libNames != NULL)
		handler(domain, (ObjectDesc **) & (domain->libNames));
	if (domain->argv != NULL)
		handler(domain, (ObjectDesc **) & (domain->argv));
	if (domain->initialPortals != NULL)
		handler(domain, (ObjectDesc **) & (domain->initialPortals));
	if (domain->initialNamingProxy != NULL)
		handler(domain, (ObjectDesc **) & (domain->initialNamingProxy));
#ifdef TIMER_EMULATION
	if ((atomic != NULL)
	    && GC_FUNC_NAME(isInHeap) (domain, (ObjectDesc *) atomic))
		handler(domain, (ObjectDesc **) & atomic);
#endif
#ifdef JAVASCHEDULER
	{
		int i;
		/* move references to HLscheduler objects */
		/* FIXME: 1) domain-control-blocks of other domains are touched
		   2) very expensive (all domains are visited) */

		foreachDomain1((domain1_f) walkSchedulerObjects, (void *) handler);

		/* move references to LLscheduler objects */
		for (i = 0; i < MAX_NR_CPUS; i++)
			if (domain == CpuInfo[i]->LowLevel.SchedThread->domain)
				if (CpuInfo[i]->LowLevel.SchedObj != NULL) {
					gc_dprintf("move LLScheduler %d\n", i);
					handler(domain, &(CpuInfo[i]->LowLevel.SchedObj));
				}
	}
#endif

	/*
	 *  object references in domain control block 
	 */
	if (domain->naming) {
		gc_dprintf("move naming\n");
		handler(domain, (ObjectDesc **) & (domain->naming));
	}
#ifdef PORTAL_INTERCEPTOR
	if (domain->outboundInterceptorObject) {
		gc_dprintf("move interceptor\n");
		handler(domain, (ObjectDesc **) & (domain->outboundInterceptorObject));
	}
	if (domain->inboundInterceptorObject) {
		gc_dprintf("move interceptor\n");
		handler(domain, (ObjectDesc **) & (domain->inboundInterceptorObject));
	}
#endif				/* PORTAL_INTERCEPTOR */

	/*
	 *  TCBs in domain control block 
	 * Copying the first TCB is sufficient as all other TCBs are reachable from this one
	 */
	{
		extern ThreadDesc *switchBackTo;
		ThreadDesc *tp;
		ThreadDescProxy *tpr;

		gc_dprintf("move first TCB\n");
		MOVETCB(domain->threads);
#ifndef NEW_SCHED
		gc_dprintf("move first runq TCB\n");
		MOVETCB(domain->firstThreadInRunQueue);
		gc_dprintf("move last runq TCB\n");
		MOVETCB(domain->lastThreadInRunQueue);
		gc_dprintf("move gc thread TCB\n");
#else
		Sched_gc_rootSet(domain, handler);
#endif

		MOVETCB(domain->gc.gcThread);
		if (switchBackTo && switchBackTo->domain == domain) {	/* switchback is domainzero thread when gc is fired off by domainmanager.gc() */
			gc_dprintf("move switchback TCB\n");
			MOVETCB(switchBackTo);	// thread that was interrupted by GC (oneshot) */
		}

		/* move current thread TCB */
		gc_dprintf("move current TCB\n");
		tpr = thread2CPUState(curthr());
		handler(domain, (ObjectDesc **) & (tpr));
		*(curthrP()) = cpuState2thread(tpr);


	}

	/* TCBs now are objects on the heap and are scanned for references the normal way */


#ifdef JAVA_MONITOR_COMMANDS
	gc_monitor_commands(domain);
#endif
	gc_dprintf("walkSpecials done\n");
	PGCE(SPECIAL);
}

/*
 * visit all references to interrupt handler objects in this domain
 */
void walkInterrupHandlers(DomainDesc * domain, HandleReference_t handler)
{
	u4_t i, j;

	/* Interrupt handler objects */
	PGCB(INTR);
#ifdef SMP
	sys_panic("GC only tested for single CPU systems");
#endif
	for (i = 0; i < MAX_NR_CPUS; i++) {	/* FIXME: must synchronize with these CPUs!! */
		for (j = 0; j < NUM_IRQs; j++) {
			if (ifirstlevel_object[i][j] != NULL && idomains[i][j] == domain) {
				gc_dprintf("move interrupt handler\n");
				handler(domain, &(ifirstlevel_object[i][j]));
			}
		}
	}
	PGCE(INTR);
}

/*
 * visit all non-heap references for this domain 
 */
void walkRootSet(DomainDesc * domain, HandleReference_t stackHandler, HandleReference_t staticHandler,
		 HandleReference_t portalHandler, HandleReference_t registeredHandler, HandleReference_t specialHandler,
		 HandleReference_t interruptHandlerHandler)
{
	if (stackHandler)
		walkStacks(domain, stackHandler);
	if (staticHandler)
		walkStatics(domain, staticHandler);
	if (portalHandler)
		walkPortals(domain, portalHandler);
	if (registeredHandler)
		walkRegistereds(domain, registeredHandler);
	if (specialHandler)
		walkSpecials(domain, specialHandler);
	if (interruptHandlerHandler)
		walkInterrupHandlers(domain, interruptHandlerHandler);
}

#endif				/* ENABLE_GC */

static void gc_memrefObjCB(DomainDesc * domain, ObjectDesc * obj, u4_t objsize, jint flags)
{
	ClassDesc *cl = *(ClassDesc **) (((ObjectDesc *) (obj))->vtable - 1);
	//printf("%s\n",cl->name);
	FORBITMAP(cl->map, cl->instanceSize, {
		  /* reference slot */
		  if (obj->data[index]
		      && getObjFlags((ObjectDesc *) (obj->data[index])) == OBJFLAGS_MEMORY) printf("   %s[%ld]=%p\n", cl->name,
												   index,
												   (void *) (obj->data[index]));},
		  {
		  ;});
	//printf("OK\n");
}
static void gc_memrefArrCB(DomainDesc * domain, ObjectDesc * object, u4_t objsize, jint flags)
{
	ArrayDesc *obj = (ArrayDesc *) object;
	int index;
	if (strcmp(obj->arrayClass->name, "[Ljx/zero/Memory;") == 0) {
		for (index = 0; index < obj->size; index++)
			if (obj->data[index] && getObjFlags((ObjectDesc *) (obj->data[index])) == OBJFLAGS_MEMORY)
				printf("   %s[%ld]=%p\n", obj->arrayClass->name, index, (void *) (obj->data[index]));

	}
}

u4_t *gc_memrefRootCB(DomainDesc * domain, ObjectDesc ** refPtr)
{
	u4_t *forward = NULL;
	ClassDesc *refcl;
	u4_t flags;
	ObjectDesc *ref = *refPtr;

	if (ref == NULL)
		return NULL;

	flags = getObjFlags(ref);
	flags &= FLAGS_MASK;
	if (flags == OBJFLAGS_MEMORY) {
		printf("ROOT %p\n", *refPtr);
	}
	return *refPtr;
}

void print_memref(jint domainID)
{
	DomainDesc *domain = findDomain(domainID);
	domain->gc.walkHeap(domain, gc_memrefObjCB, gc_memrefArrCB, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

	walkRootSet(domain, gc_memrefRootCB, gc_memrefRootCB, gc_memrefRootCB, gc_memrefRootCB, gc_memrefRootCB, gc_memrefRootCB);
}
