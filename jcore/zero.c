#include "all.h"

#include "spinlock.h"

static spinlock_t memory_revoke_lock = SPIN_LOCK_UNLOCKED;

#define debugl(x)
/*#define debugl(x) printf x*/
#define debugz(x)
/*#define debugdm(x) printf x*/
#define debugdm(x)

void registerPortal(DomainDesc * domain, ObjectDesc * dep, char *name);
void receiveDomainDEPThread(void *arg);
void jxbytecpy(char *source, char *target, jint nbytes);
inline ThreadDesc *cpuState2thread(ObjectDesc * obj);

ClassDesc *portalInterface = NULL;

Proxy *getInitialNaming()
{
	ASSERT(curdom()->initialNamingProxy);
	return curdom()->initialNamingProxy;
}

void installInitialNaming(DomainDesc * srcDomain, DomainDesc * dstDomain, Proxy * naming)
{
	u4_t quota = 1000;
	thread_prepare_to_copy();
	/*printf("installInitialNaming: %d -> %d\n", srcDomain->id, dstDomain->id); */
	dstDomain->initialNamingProxy = copy_reference(srcDomain, dstDomain, naming, &quota);
}

/********** support functions *******************/


/* thread entry point for domain-domainZero DEP handler threads */

typedef jint(*code2_f) (jint a, jint b);


void receive_dep(void *arg)
{
	u4_t depIndex = (u4_t) arg;
	receive_portalcall(depIndex);
}

ClassDesc *createClassDescImplementingInterface(DomainDesc * domain, ClassDesc * cl, MethodInfoDesc * methods, int numMethods,
						char *name)
{
	ClassDesc *c;

	ASSERTCLASSDESC(cl);

	c = malloc_classdesc(domain, strlen(name) + 1);
	c->classType = CLASSTYPE_CLASS;
#ifdef USE_QMAGIC
	c->magic = MAGIC_CLASSDESC;
#endif
	c->definingLib = NULL;
	c->superclass = java_lang_Object;
	c->instanceSize = 0;
	c->numberOfInterfaces = 1;
	c->interfaces = malloc_classdesctable(domain, c->numberOfInterfaces);
	c->interfaces[0] = cl;
	c->vtableSym = cl->vtableSym;
	c->vtableSize = cl->vtableSize;
	strcpy(c->name, name);
	createVTable(domain, c);
	installVtables(domain, c, methods, numMethods, cl);
	return c;
}

Class *createSubClass(Class * cl, ClassDesc * classDesc)
{
	Class *c;

	ASSERTCLASS(cl);

	c = (Class *) jxmalloc(sizeof(Class) MEMTYPE_OTHER);
	memset(c, 0, sizeof(Class));

	c->classDesc = classDesc;
#ifdef USE_QMAGIC
	c->magic = MAGIC_CLASS;
#endif
	/* superclass */
	c->superclass = cl;
	/* static fields */
	c->staticFields = NULL;

	return c;
}


ClassDesc *createDZClass(SharedLibDesc * zeroLib, char *name, MethodInfoDesc * methods, jint numberOfMethods, char *subname)
{
	ClassDesc *c;
	ClassDesc *cd;
	ASSERTSLIB(zeroLib);
	c = findClassDescInSharedLib(zeroLib, name);
	if (c == NULL) {
		sys_panic("Cannot find class %s. Perhaps you are using a wrong zero lib.", name);
	}
	ASSERT(c != NULL);
	ASSERTCLASSDESC(c);
	cd = createClassDescImplementingInterface(domainZero, c, methods, numberOfMethods, subname);
	return cd;
}


extern SharedLibDesc *zeroLib;

ClassDesc *init_zero_class(char *ifname, MethodInfoDesc * methods, jint size, jint instanceSize, jbyte * typeMap, char *subname)
{
	ObjectDesc *instance;
	ClassDesc *cd;
	DEPDesc *dep;
	ThreadDesc *thread;
	jint mapBytes;
	cd = createDZClass(zeroLib, ifname, methods, size / sizeof(MethodInfoDesc), subname);
	cd->instanceSize = instanceSize;
	mapBytes = (instanceSize + 7) >> 3;
	cd->mapBytes = mapBytes;
	cd->map = typeMap;

	return cd;
}


ObjectDesc *init_zero_dep(char *ifname, char *depname, MethodInfoDesc * methods, jint size, char *subname)
{
	Proxy *proxy;
	ClassDesc *ifclass;
	ClassDesc *cd;
	ObjectDesc *obj;

	/* check if sufficient stack space exist to run this function */
	CHECK_STACK_SIZE(ifname, 256);

	ifclass = findClassDescInSharedLib(zeroLib, ifname);
	cd = createDZClass(zeroLib, ifname, methods, size / sizeof(MethodInfoDesc), subname);
	obj = allocObjectInDomain(domainZero, cd);

	proxy = portal_auto_promo(domainZero, obj);
	ASSERTPROXY(proxy);
	if (depname != NULL)
		registerPortal(domainZero, proxy, depname);
	return proxy;
}

ObjectDesc *init_zero_dep_without_thread(char *ifname, char *depname, MethodInfoDesc * methods, jint size, char *subname)
{
	ClassDesc *cd;
	u4_t index;
	Proxy *instance;
	u4_t depIndex;

	cd = createDZClass(zeroLib, ifname, methods, size / sizeof(MethodInfoDesc), subname);
	cd->proxyVtable = cd->vtable;
	instance = allocProxyInDomain(domainZero, cd, NULL, domainZero->id, 0);
	instance->vtable = cd->vtable;	/* NOT a real proxy */
	instance->targetDomain = NULL;	/* indicates direct portal */
	instance->targetDomainID = domainZero->id;
#ifdef NEW_PORTALCALL
	depIndex = createService(domainZero, instance, cd, NULL);
#else
	depIndex = createService(domainZero, instance, cd);
#endif
	if (depname != NULL)
		registerPortal(domainZero, instance, depname);
	index = depIndex;

	return instance;
}



void IN_jxbytecpy(char *source, char *target, jint nbytes);
void jxbytecpy(char *source, char *target, jint nbytes)
{
#ifdef SAMPLE_FASTPATH
	if (do_sampling)
		printStackTrace("SLOWOPERATION-BYTECPY ", curthr(), &source - 2);
#endif
	IN_jxbytecpy(source, target, nbytes);
}



extern ClassDesc *vmmethodClass;

void addZeroVtables()
{
	u4_t i, j;
	SharedLibDesc *lib = zeroLib;
	for (i = 0; i < lib->numberOfClasses; i++) {
		for (j = 0; j < lib->allClasses[i].numberOfMethods; j++) {
			lib->allClasses[i].methods[j].objectDesc_vtable = vmmethodClass->vtable;
		}
	}
}





/****************************************/
/* INIT */

static int n_libs = 4;
static char *start_libs[] = {
	"zero.jll",
	"jdk0.jll",
	"zero_misc.jll",
#ifdef JAVASCHEDULER
	"scheduler.jll",
#endif
};
static char *start_domain = INIT_LIB;

static inline void sti(void)
{
	asm volatile ("sti");
}

static void create_CPUObjs(void)
{
	extern ClassDesc *cpuClass;
	CPUDesc *cpuInstance;
#ifdef SMP
	int cpu;
#endif
	int cpu_ID;

/* for all CPUs do */
#ifdef SMP
	for (cpu = 0, cpu_ID = online_cpu_ID[cpu]; cpu < num_processors_online; cpu++, cpu_ID = online_cpu_ID[cpu])
#else
	cpu_ID = 0;
#endif
	{
		cpuInstance = specialAllocCPUDesc();
		cpuInstance->cpu_id = cpu_ID;
		domainZero->cpu[cpu_ID] = cpuInstance;
	}
}

void start_initial_thread(void *dummy);

extern Proxy *initialNamingProxy;
ThreadDesc *monitorThread = NULL;

void start_domain_zero()
{
	ThreadDesc *domainInit_thread;
	DomainDesc *domainInit;
	DomainProxy *domainProxy;
	LibDesc *lib;
	ObjectDesc *dm;
#ifdef SMP
	int cpu;
#endif
	int cpu_ID;
#ifdef JAVASCHEDULER
	Class *SchedClass;
	ObjectDesc *Scheduler;
	char *LLSchedulerClassName = LL_SCHEDULER;
	char *HLSchedulerClassName = HL_SCHEDULER_INIT;
#endif
	ArrayDesc *arr;

	DISABLE_IRQ;

	printf("Started DomainZero.\n");

  /*********************************
   * Low level initialization
   *********************************/

#ifdef APIC
#ifdef SMP
	if (smp_detect())
		smp_init();
#else
	detect_lAPIC();		/* is a local APIC present?? */
	if (apic_found) {
		printf("APIC found!!");
		install_apicIRQs();
		enable_local_APIC();
		calibrate_APIC_clock();	/* APICs and IRQs must already be setup */
	}
#endif
#endif

	setTimer();

#ifdef KERNEL
#if defined(TIMESLICING_TIMER_IRQ) || defined(CHECK_SERIAL_IN_TIMER) || defined(SAMPLING_TIMER_IRQ)
	enableIRQ(0);		/* needed to poll serial line */
#endif
#endif				/* KERNEL */

#ifdef KERNEL
	/* Enable processor interrupts */
	sti();
#endif



  /*********************************
   * High level initialization
   *********************************/

	/* load zero lib and create portals */
	lib = load(domainZero, "zero.jll");
	if (lib == NULL)
		sys_panic("Cannot load lib %s\n", "zero.jll");
	ASSERTLIB(lib);

	/*
	 * Create virtual method tables for interaction between 
	 * Java code and C-code that implements DomainZero.
	 */
	zeroLib = lib->sharedLib;

	init_zero_from_lib(domainZero, lib->sharedLib);

	/* Domainzero's naming does now exist.
	 * Make it available.
	 */
	domainZero->initialNamingProxy = initialNamingProxy;

	callClassConstructors(domainZero, lib);

	lib = load(domainZero, "jdk0.jll");
	if (lib == NULL)
		sys_panic("Cannot load lib %s\n", "jdk0.jll");
	ASSERTLIB(lib);
	callClassConstructors(domainZero, lib);

	DISABLE_IRQ;

	/*********************************
	 * Create monitor thread
	 *********************************/
	monitorThread = createThread(domainZero, NULL, NULL, STATE_AVAILABLE, SCHED_CREATETHREAD_NORUNQ);

	RESTORE_IRQ;


#ifdef EVENT_CALIBRATION
  /*********************************
   * Calibrate event loggin system
   *********************************/
	{
		int i;
		int event_calibrate = createNewEvent("CALIBRATE");
		int event_calibrate_cache = createNewEvent("CALIBRATE_CACHE");
		disable_cache();
		for (i = 0; i < 100; i++) {
			RECORD_EVENT(event_calibrate);
		}
		enable_cache();
		for (i = 0; i < 100; i++) {
			RECORD_EVENT(event_calibrate_cache);
		}
	}
#endif

  /*********************************
   * Create and start initial Java domain
   *********************************/
	dm = lookupPortal("DomainManager");
	arr = newStringArray(domainZero, sizeof(start_libs) / sizeof(char *), start_libs);

	DISABLE_IRQ;

	thread_prepare_to_copy();

	domainProxy =
	    domainManager_createDomain(dm, newString(domainZero, "Init"), NULL, NULL, newString(domainZero, INIT_LIB), arr,
				       newString(domainZero, "jx/init/Main"), HEAP_BYTES_INIT, -1, -1, NULL, -1,
				       CODE_BYTES_DOMAININIT, NULL, domainZero->initialNamingProxy, NULL,
				       GC_IMPLEMENTATION_DEFAULT, NULL);
	domainInit = domainProxy->domain;

	RESTORE_IRQ;

  /*********************************
   * CPU and Scheduler initialization
   *********************************/

	/* install data for each CPU */
	create_CPUObjs();
#ifdef SMP
	for (cpu = 0, cpu_ID = online_cpu_ID[cpu]; cpu < num_processors_online; cpu++, cpu_ID = online_cpu_ID[cpu])
#else
	cpu_ID = 0;
#endif
	{
		/* install CPU Object for each CPU */
		domainInit->cpu[cpu_ID] = domainZero->cpu[cpu_ID];

		/* install Schedulers for each CPU */
#if 0
#ifdef JAVASCHEDULER
		/* install a LowLevelScheduler for this CPU */
		SchedClass = findClass(domainInit, LLSchedulerClassName);
		ASSERT(SchedClass != NULL);
		ASSERTCLASS(SchedClass);
		Scheduler = allocObjectInDomain(domainInit, SchedClass->classDesc);
		executeSpecial(domainInit, LLSchedulerClassName, "<init>", "()V", Scheduler, NULL, 0);
		SMPcpuManager_register_LLScheduler(NULL, cpuDesc2Obj(domainInit->cpu[cpu_ID]), Scheduler);
		dprintf("CPU%d: LowLevel-Scheduler created.\n", cpu_ID);

		/* install a HighLevelScheduler for this CPU */
		SchedClass = findClass(domainInit, HLSchedulerClassName);
		ASSERT(SchedClass != NULL);
		ASSERTCLASS(SchedClass);
		Scheduler = allocObjectInDomain(domainInit, SchedClass->classDesc);
		executeSpecial(domainInit, HLSchedulerClassName, "<init>", "()V", Scheduler, NULL, 0);
		register_HLScheduler(domainInit->cpu[cpu_ID], domainInit, domainInit, Scheduler);
		dprintf("CPU%d: HighLevel-Scheduler created for Domain Init.\n", cpu_ID);
#endif
	}			// end for each CPU

#ifdef JAVASCHEDULER
	if (domainInit->Scheduler[get_processor_id()] != NULL)
		curthr()->schedulingDomain = domainInit;
#ifdef ENABLE_GC
	domainInit->gc.gcThread->schedulingDomain = domainInit;;
#endif
#endif

#else				/*0 */
	}			// end for each CPU
#endif				/*0 */


	printf("Terminate DomainZero initial thread.\n");

	RESTORE_IRQ;

	/* initial thread of DomainZero exists here */
}







extern ClassDesc *domainClass;

void init_zero_from_lib(DomainDesc * domain, SharedLibDesc * zeroLib)
{
#ifdef NOPREEMPT
	code_t atomic_code;
#endif

	CHECK_STACK_SIZE(domain, 256);

	domain = domainZero;


	portalInterface = findClassDescInSharedLib(zeroLib, "jx/zero/Portal");


	init_atomicvariable_portal();
	init_bootfs_portal();
	init_cas_portal();
	init_clock_portal();
	init_componentmanager_portal();
	init_cpu_portal();
	init_cpumanager_portal();
	init_cpustate_portal();
	init_credential_portal();
	init_debugchannel_portal();
	init_debugsupport_portal();
	init_domain_portal();
	init_domainmanager_portal();
	init_debugsupport_portal();
	init_irq_portal();
#if 0
	init_hlschedulersupport_portal();
	init_javaschedulersupport_portal();
	init_llschedulersupport_portal();
#endif
	init_memory_portal();
	init_memorymanager_portal();
	init_mutex_portal();
	init_naming_portal();
	init_ports_portal();
	init_profiler_portal();
	init_scheduler_portal();
	init_smpcpumanager_portal();
	init_vmclass_portal();
	init_vmmethod_portal();
	init_vmobject_portal();
	init_interceptInboundInfo_portal();

#ifdef DOMZERO_PERF_TEST_PORTAL
	init_testdzperf_portal();
#endif				/* DOMZERO_PERF_TEST_PORTAL */

#ifdef FRAMEBUFFER_EMULATION
	init_fbemulation_portal();
#endif				/* FRAMEBUFFER_EMULATION */

#ifdef DISK_EMULATION
	init_disk_emulation_portal();
#endif				/* DISK_EMULATION */

#ifdef NET_EMULATION
	init_net_emulation_portal();
#endif				/* NET_EMULATION */

#ifdef TIMER_EMULATION
	init_timer_emulation_portal();
#endif				/* TIMER_EMULATION */



	/* now we can add a vtable to the DomainZero Domain object */
	//domainDesc2Obj(domainZero)->vtable = (u4_t)domainClass->vtable;

	/* add zero vtables */
	addZeroVtables();


	init_object();

}
