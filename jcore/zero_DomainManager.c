/********************************************************************************
 * DomainZero DomainManager
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#include "all.h"

#ifdef PROFILE_EVENT_CREATEDOMAIN
static int event_createdomain_start, event_createdomain_end, event_initialthread_start, event_initialthread_end;
#define CREATEDOMAIN_EVENT_START  RECORD_EVENT(event_createdomain_start)
#define CREATEDOMAIN_EVENT_END    RECORD_EVENT(event_createdomain_end)
#define CREATEDOMAIN_EVENT_INITIALTHREAD_STARTINIT    RECORD_EVENT(event_initialthread_start)
#define CREATEDOMAIN_EVENT_INITIALTHREAD_ENDINIT      RECORD_EVENT(event_initialthread_end)
#else
#define CREATEDOMAIN_EVENT_START
#define CREATEDOMAIN_EVENT_END
#define CREATEDOMAIN_EVENT_INITIALTHREAD_STARTINIT
#define CREATEDOMAIN_EVENT_INITIALTHREAD_ENDINIT
#endif

#define CALLERDOMAIN (curthr()->mostRecentlyCalledBy?curthr()->mostRecentlyCalledBy->domain:domainZero)

extern ClassDesc *domainClass;

#ifdef JAVASCHEDULER
static void installHLSfromClassName(DomainDesc * domain)
{

	Class *SchedClass;
	ObjectDesc *Scheduler, *string;
	int n_names, cpu_ID, i;
	char HLSchedulerClassName[100];
	DomainDesc *newSchedDom;

	/* create an register HLScheduler objects */
	if (domain->HLSNames != NULL) {
		/* consistency check */
		n_names = getArraySize(domain->HLSNames);
		for (cpu_ID = 0, i = 0; cpu_ID < MAX_NR_CPUS; cpu_ID++)
			if (domain->cpu[cpu_ID] != NULL) {
				if (i >= n_names)
					sys_panic("#HLSchedulerNames < #CPUs");	/* todo:  maybe we schould create default Scheduler */
				string = getReferenceArrayElement(domain->HLSNames, i);
				if (string == NULL)
					sys_panic("HLSchedulerName (%d) == NULL", i);
				stringToChar(string, HLSchedulerClassName, sizeof(HLSchedulerClassName));
				/* create HLScheduler objects */
				SchedClass = findClass(domain, HLSchedulerClassName);
				ASSERT(SchedClass != NULL);
				ASSERTCLASS(SchedClass);
				Scheduler = allocObjectInDomain(domain, SchedClass->classDesc);
				executeSpecial(domain, HLSchedulerClassName, "<init>", "()V", Scheduler, NULL, 0);
				/* register HLScheduler objects */
				register_HLScheduler(domain->cpu[cpu_ID], domain, domain, Scheduler);
				dprintf("CPU%d: HighLevel-Scheduler created for Domain %s.\n", cpu_ID, domain->domainName);
				i++;
			}
		domain->HLSNames = NULL;	/* no longer needed */
	}
	if (domain->Scheduler[get_processor_id()] != NULL)
		curthr()->schedulingDomain = domain;

#ifdef ENABLE_GC
	if (domain->gc.gcThread->state == STATE_AVAILABLE) {
		if (domain->Scheduler[get_processor_id()] != NULL)
			domain->gc.gcThread->schedulingDomain = domain;
	} else
		sys_panic("GC started while initializing new Domain (%s)", domain->domainName);
#endif
}
#endif


void start_initial_thread(void *dummy)
{
	LibDesc *lib;
	char value[80];
	jint i, n_libs;
	ObjectDesc *o;
	u4_t dz[3];
	Proxy *proxy;
	DomainDesc *domain;

	DISABLE_IRQ;

	CREATEDOMAIN_EVENT_INITIALTHREAD_STARTINIT;

	domain = curdom();
	/*printf("Running initial thread %p of domain %p (%s)\n", curthr(), domain, domain->domainName); */
	if (domain->libNames != NULL) {
		n_libs = getArraySize(domain->libNames);
		for (i = 0; i < n_libs; i++) {
			o = getReferenceArrayElement(domain->libNames, i);
			stringToChar(o, value, sizeof(value));
			lib = load(curdom(), value);
			if (lib == NULL) {
				sys_panic("Cannot load lib %s\n", value);
				return;
			}
		}
		domain->libNames = NULL;	/* no longer needed */
	}

	stringToChar(domain->dcodeName, value, sizeof(value));
	lib = load(curdom(), value);
	if (lib == NULL)
		sys_panic("Cannot load domain file %s\n", value);

	domain->dcodeName = NULL;	/* no longer needed */

	stringToChar(domain->startClassName, value, sizeof(value));
	domain->startClassName = NULL;	/* don't need this object any longer */

	dz[0] = (jint) getInitialNaming();

	CREATEDOMAIN_EVENT_INITIALTHREAD_ENDINIT;

	RESTORE_IRQ;
#ifndef KERNEL
	enable_irq();		// FIXME   
#endif

	/* The following code is executed with interrupts enabled */

#if 0
	printf("BEFORE %s\n", lib->sharedLib->name);
#ifdef CHECK_HEAPUSAGE
	curdon()->gc.check_heap(curdom(), JNI_FALSE);
#endif				/* CHECK_HEAPUSAGE */
#endif
	/* execute class constructors */
	for (i = 0; i < curdom()->numberOfLibs; i++) {
		lib = curdom()->libs[i];
		callClassConstructors(curdom(), lib);
	}
#if 0
	printf("AFTER %s\n", lib->sharedLib->name);
#ifdef CHECK_HEAPUSAGE
	curdon()->gc.check_heap(curdom(), JNI_FALSE);
#endif				/* CHECK_HEAPUSAGE */
#endif

#ifdef JAVASCHEDULER
	installHLSfromClassName(domain /*, domain->HLSNames */ );
#endif

	if (!curdom()->argv) {
		curdom()->argv = newStringArray(curdom(), 0, NULL);
	}
	/* try to find new init method */
	if (findMethod(curdom(), value, "init", "(Ljx/zero/Naming;[Ljava/lang/String;[Ljava/lang/Object;)V")
	    != NULL) {
		dz[1] = curdom()->argv;
		dz[2] = curdom()->initialPortals;
		executeStatic(curdom(), value, "init", "(Ljx/zero/Naming;[Ljava/lang/String;[Ljava/lang/Object;)V", (jint *) dz,
			      3);
	} else if (findMethod(curdom(), value, "init", "(Ljx/zero/Naming;[Ljava/lang/String;)V") != NULL) {
		dz[1] = curdom()->argv;
		executeStatic(curdom(), value, "init", "(Ljx/zero/Naming;[Ljava/lang/String;)V", (jint *) dz, 2);
	} else if (findMethod(curdom(), value, "init", "(Ljx/zero/Naming;)V")
		   != NULL) {
		/* try to find old fashioned init method */
		executeStatic(curdom(), value, "init", "(Ljx/zero/Naming;)V", (jint *) dz, 1);
	} else {
		printf(" %s.init(Ljx/zero/Naming;[Ljava/lang/String;)V\n", value);
		sys_panic("init method not found for domain %d\n", domain->id);
	}
}

/* create Domain without HLScheduler */
static DomainDesc *__domainManager_createDomain(ObjectDesc * self, ObjectDesc * dname, ArrayDesc * cpuObjs,
						ObjectDesc * dcodeName, ArrayDesc * libsName, ObjectDesc * startClassName,
						jint gcinfo0, jint gcinfo1, jint gcinfo2, ObjectDesc * gcinfo3, jint gcinfo4,
						jint codeSize, ArrayDesc * argv, ObjectDesc * naming, ArrayDesc * moreArgs,
						jint gcImpl, ArrayDesc * schedInfo)
{
	char value[80];
	char value1[80];
	DomainDesc *domain;
	DomainDesc *sourceDomain;
	DomainDesc *dataSrcDomain;
	int i, j;
	CPUDesc *cpuObj;
	u4_t quota = CREATEDOMAIN_PORTAL_QUOTA;
	u4_t *schedinfo;

	if (dname == 0)
		return NULL;

	DISABLE_IRQ;
	//    printf("Create Domain\n");
	sourceDomain = CALLERDOMAIN;

	/* name */
	stringToChar(dname, value, sizeof(value));

	/* hack: get principle for memory accounting */
	if (gcinfo3) {
		stringToChar(gcinfo3, value1, sizeof(value1));
	} else {
		value1[0] = '\0';
	}
	domain = createDomain(value, gcinfo0, gcinfo1, gcinfo2, value1, gcinfo4, codeSize, gcImpl, schedinfo);
	if (domain == NULL)
		exceptionHandlerMsg(THROW_RuntimeException, "Cannot create domain.");

/* create GC thread before copying data to the domain */
#ifdef ENABLE_GC
	{
		DISABLE_IRQ;
		domain->gc.gcThread = createThread(domain, NULL, NULL, STATE_AVAILABLE, SCHED_CREATETHREAD_NORUNQ);
		setThreadName(domain->gc.gcThread, "GC", NULL);
		domain->gc.gcThread->isGCThread = 1;
		domain->gc.gcCode = gc_in;
		RESTORE_IRQ;
	}
#endif

	if (naming == NULL) {
		// inherit naming from parent
		installInitialNaming(sourceDomain, domain, sourceDomain->initialNamingProxy);
	} else {
		// install user supplied naming
#ifdef COPY_TO_DOMAINZERO
		installInitialNaming(curdom(), domain, naming);
#else
		installInitialNaming(sourceDomain, domain, naming);
#endif
	}
	/* inherit attibutes from parent */
#if defined(PORTAL_INTERCEPTOR) || defined(PORTAL_TRANSFER_INTERCEPTOR)
	domain->memberOfTCB = CALLERDOMAIN->memberOfTCB;
	if (CALLERDOMAIN->portalInterceptorThread) {
		installInterceptor(NULL, domain, CALLERDOMAIN->portalInterceptorObject, CALLERDOMAIN->portalInterceptorThread);
	}
#endif

	/* store CPU Objects */
	if (cpuObjs != NULL) {
		for (i = 0; i < getArraySize(cpuObjs); i++) {
			cpuObj = obj2cpuDesc(getReferenceArrayElement(cpuObjs, i));
			domain->cpu[cpuObj->cpu_id] = cpuObj;
		}
	}

	if (domain->cpu[get_processor_id()] == NULL) {	/* create Object for the current CPU */
		/*    
		   DEPDesc *aDEP = init_zero_dep_from_class_without_thread(cpuClass, NULL);
		   CPUDesc *cpuInstance = (CPUDesc*)aDEP->obj;
		   cpuInstance->cpu_id= get_processor_id();
		   domain->cpu[get_processor_id()]=cpuInstance; 
		 */
		domain->cpu[get_processor_id()] = domainZero->cpu[get_processor_id()];
	}
#ifdef COPY_TO_DOMAINZERO
	dataSrcDomain = curdom();
#else
	dataSrcDomain = sourceDomain;
#endif

#ifdef DEBUG
	//printf("LIBNAME %p\n", libsName);
	if (!gc_isValidHeapRef(dataSrcDomain, libsName)) {
		sys_panic("invalref");
	}
#endif

	domain->startClassName = copy_reference(dataSrcDomain, domain, (ObjectDesc *) startClassName, &quota);
	domain->dcodeName = copy_reference(dataSrcDomain, domain, (ObjectDesc *) dcodeName, &quota);
	domain->libNames = copy_reference(dataSrcDomain, domain, (ObjectDesc *) libsName, &quota);
	domain->argv = copy_reference(dataSrcDomain, domain, (ObjectDesc *) argv, &quota);
	domain->initialPortals = copy_reference(dataSrcDomain, domain, (ObjectDesc *) moreArgs, &quota);
	ASSERT(domain->dcodeName != NULL);
	RESTORE_IRQ;
	return domain;
}


DomainProxy *domainManager_createDomain(ObjectDesc * self, ObjectDesc * dname, ArrayDesc * cpuObjs, ArrayDesc * HLSNames,
					ObjectDesc * dcodeName, ArrayDesc * libsName, ObjectDesc * startClassName, jint gcinfo0,
					jint gcinfo1, jint gcinfo2, ObjectDesc * gcinfo3 /*hack */ , jint gcinfo4, jint codeSize,
					ArrayDesc * argv, ObjectDesc * naming, ArrayDesc * portals, jint gcImpl,
					ArrayDesc * schedInfo)
{
	ThreadDesc *thread;
	DomainDesc *domain;
	u4_t quota = 500;
	DomainProxy *domainProxy;
	DomainDesc *callerDomain;

	CHECK_STACK_SIZE(self, 256);

	callerDomain = CALLERDOMAIN;

	CREATEDOMAIN_EVENT_START;
	/*
	   printf("heap:%d \n",heapSize);
	   printf("libs:%p \n",libsName);
	 */

	/* create Domain without HLS */
	domain =
	    __domainManager_createDomain(self, dname, cpuObjs, dcodeName, libsName, startClassName, gcinfo0, gcinfo1, gcinfo2,
					 gcinfo3, gcinfo4, codeSize, argv, naming, portals, gcImpl, schedInfo);
	ASSERTDOMAIN(domain);

#ifdef JAVASCHEDULER
	domain->HLSNames = copy_reference(curdom(), domain, (ObjectDesc *) HLSNames, &quota);
#endif


	DISABLE_IRQ;

	ASSERT(domain->dcodeName != NULL);
	thread = createInitialDomainThread(domain, STATE_RUNNABLE, SCHED_CREATETHREAD_DEFAULT);

#ifdef JAVASCHEDULER
	if (CALLERDOMAIN->Scheduler[get_processor_id()] != NULL)
		/* use scheduling domain of the caller instead of DomainZero */
		thread->schedulingDomain = CALLERDOMAIN;
#endif

	domainProxy = allocDomainProxyInDomain(curdom(), domain, domain->id);
	RESTORE_IRQ;
	CREATEDOMAIN_EVENT_END;
	return domainProxy;

}



ObjectDesc *domainManager_getDomainZero(ObjectDesc * self)
{
	DomainDesc *sourceDomain = CALLERDOMAIN;
	DomainProxy *domainProxy;
	printf("getDomainZero \n");
	DISABLE_IRQ;
	domainProxy = allocDomainProxyInDomain(curdom(), domainZero, domainZero->id);
	RESTORE_IRQ;
	return domainProxy;
}

ObjectDesc *domainManager_getCurrentDomain(ObjectDesc * self)
{
	DomainDesc *sourceDomain = CALLERDOMAIN;
	DomainProxy *domainProxy;
	printf("getCurrentDomain (%d)\n", sourceDomain->id);
	DISABLE_IRQ;
	domainProxy = allocDomainProxyInDomain(curdom(), sourceDomain, sourceDomain->id);
	RESTORE_IRQ;
	return domainProxy;
}

extern SharedLibDesc *zeroLib;

/*static*/ jint findZeroLibMethodIndex(DomainDesc * domain,
				       char *className, char *methodName, char *signature)
{
	jint j;
	ClassDesc *cl;
	cl = findClassDescInSharedLib(zeroLib, className);
	/*
	   Class *c = findClass(domain, className);
	   ASSERTCLASS(c);
	   if (c == NULL) {
	   sys_panic("Cannot find DEP %s\n", className);
	   }
	   cl = c->classDesc;
	 */
	ASSERTCLASSDESC(cl);
	for (j = 0; j < cl->vtableSize; j++) {
		if (cl->vtableSym[j * 3][0] == '\0')
			continue;	/* hole */
		if ((strcmp(cl->vtableSym[j * 3 + 1], methodName) == 0)
		    && (strcmp(cl->vtableSym[j * 3 + 2], signature) == 0)) {
			return j;
		}
	}
	sys_panic("Cannot find DEP method %s:: %s%s\n", className, methodName, signature);
	return 0;
}


void domainManager_installInterceptor(ObjectDesc * self, DomainProxy * domainObj, ObjectDesc * interceptor,
				      ObjectDesc * interceptorThread)
{
#if defined(PORTAL_INTERCEPTOR) || defined(PORTAL_TRANSFER_INTERCEPTOR)
	DomainDesc *domain;
	DISABLE_IRQ;
	if (domainObj->domain->id != domainObj->domainID) {
		printf("installInterceptor: Old domain object! Do nothing\n");
		goto finish;
	}
	domain = domainObj->domain;
	installInterceptor(self, domain, interceptor, cpuState2thread(interceptorThread));
      finish:
	RESTORE_IRQ;
#else
	exceptionHandler(-1);	/* no support for interception */
#endif				/*  PORTAL_INTERCEPTOR || PORTAL_TRANSFER_INTERCEPTOR */
}

void installInterceptor(ObjectDesc * self, DomainDesc * domain, ObjectDesc * interceptor, ThreadDesc * thread)
{
#if defined(PORTAL_INTERCEPTOR) || defined(PORTAL_TRANSFER_INTERCEPTOR)
	MethodDesc *method;
	code_t c;
	jint ret;
	int index;

	ASSERTCLI;


	if (domain->outboundInterceptorThread != NULL) {
		printf("installInterceptor: Cannot install interceptor, because there already is one installed!\n");
		return;
	}
	thread->isInterruptHandlerThread = 1;
	thread->state = STATE_AVAILABLE;

#ifdef PORTAL_INTERCEPTOR
	domain->outboundInterceptorThread = thread;
	domain->outboundInterceptorObject = interceptor;
	index = findZeroLibMethodIndex(domain, "jx/zero/DomainBorderOut", "outBound", "(Ljx/zero/InterceptOutboundInfo;)Z");
	domain->outboundInterceptorCode = (code_t) interceptor->vtable[index];

	domain->inboundInterceptorThread = thread;
	domain->inboundInterceptorObject = interceptor;
	index = findZeroLibMethodIndex(domain, "jx/zero/DomainBorderIn", "inBound", "(Ljx/zero/InterceptInboundInfo;)Z");
	domain->inboundInterceptorCode = (code_t) interceptor->vtable[index];
#endif

#ifdef PORTAL_TRANSFER_INTERCEPTOR
	domain->portalInterceptorThread = thread;
	domain->portalInterceptorObject = interceptor;
	index = findZeroLibMethodIndex(domain, "jx/zero/DomainBorder", "createPortal", "(Ljx/zero/PortalInfo;)Z");
	domain->createPortalInterceptorCode = (code_t) interceptor->vtable[index];
	printf("CODE %p\n", domain->createPortalInterceptorCode);
	ASSERT(domain->createPortalInterceptorCode != NULL);
	index = findZeroLibMethodIndex(domain, "jx/zero/DomainBorder", "destroyPortal", "(Ljx/zero/PortalInfo;)V");
	domain->destroyPortalInterceptorCode = (code_t) interceptor->vtable[index];
	ASSERT(domain->destroyPortalInterceptorCode != NULL);
#endif
#else
	sys_panic("");
#endif				/*  PORTAL_INTERCEPTOR || PORTAL_TRANSFER_INTERCEPTOR */
}

void domainManager_terminate(ObjectDesc * self, DomainProxy * domainObj)
{
	DomainDesc *domain;
	DISABLE_IRQ;
	if (domainObj->domain->id == domainObj->domainID) {
		terminateDomain(domainObj->domain);
	}
	RESTORE_IRQ;
}

void domainManager_terminateCaller(ObjectDesc * self)
{
	DomainDesc *domain = CALLERDOMAIN;
	DISABLE_IRQ;
	terminateDomain(domain);
	RESTORE_IRQ;
}

void domainManager_freeze(ObjectDesc * self, ObjectDesc * domainObj)
{
	DomainDesc *domain = obj2domainDesc(domainObj);
}

void domainManager_thaw(ObjectDesc * self, ObjectDesc * domainObj)
{
	DomainDesc *domain = obj2domainDesc(domainObj);
}

void domainManager_gc(ObjectDesc * self, DomainProxy * domainObj)
{
	DomainDesc *domain;
	DISABLE_IRQ;
	if (domainObj->domain->id != domainObj->domainID)
		goto finish;
	domain = domainObj->domain;
#ifdef ENABLE_GC
	if (domain->gc.gcThread == NULL)
		domain_panic(domain, "GC but no GC thread available");
	start_thread_using_code1(domain->gc.gcObject, domain->gc.gcThread, domain->gc.gcCode, (u4_t) domain);
#endif
      finish:
	RESTORE_IRQ;

}

MethodInfoDesc domainManagerMethods[] = {
	{"createDomain", "", (code_t) domainManager_createDomain}
	,
	{"getDomainZero", "", (code_t) domainManager_getDomainZero}
	,
	{"getCurrentDomain", "", (code_t) domainManager_getCurrentDomain}
	,
	{"installInterceptor", "",
	 (code_t) domainManager_installInterceptor}
	,

	{"freeze", "(Ljx/zero/Domain;)V", (code_t) domainManager_freeze}
	,
	{"thaw", "(Ljx/zero/Domain;)V", (code_t) domainManager_thaw}
	,
	{"terminate", "(Ljx/zero/Domain;)V",
	 (code_t) domainManager_terminate}
	,
	{"terminateCaller", "()V", (code_t) domainManager_terminateCaller}
	,

	{"gc", "(Ljx/zero/Domain;)V", (code_t) domainManager_gc}
	,
};

void init_domainmanager_portal()
{
	init_zero_dep("jx/zero/DomainManager", "DomainManager", domainManagerMethods, sizeof(domainManagerMethods),
		      "<jx/zero/DomainManager>");

#ifdef PROFILE_EVENT_CREATEDOMAIN
	event_createdomain_start = createNewEvent("CREATEDOMAIN_START");
	event_createdomain_end = createNewEvent("CREATEDOMAIN_END");
	event_initialthread_start = createNewEvent("INITIALTHREAD_STARTINIT");
	event_initialthread_end = createNewEvent("INITIALTHREAD_ENDINIT");
#endif

}
