#include "all.h"

#define debugz(x)


/*
 *
 * SMPcpuManager Portal
 *
 */

typedef void (*Sched_register_fkt) (DomainDesc * domain, ObjectDesc * sched);
static void _SMPcpuManager_register_Scheduler(int cpu_id, DomainDesc * domain, ObjectDesc * new_sched,
					      Sched_register_fkt Sched_register);

#ifdef SMP
struct func_parameter1 {
	ObjectDesc *self;
};
struct func_parameter2 {
	ObjectDesc *self;
	CPUDesc *cpuObj;
};

struct func_parameter3 {
	ObjectDesc *self;
	CPUDesc *cpuObj;
	ThreadDesc *cpuState;
};

struct func_parameter4 {
	//     CPUDesc    *cpuObj;
	int cpu_id;
	DomainDesc *domain;
	ObjectDesc *new_sched;
	Sched_register_fkt Sched_register;
};

/*Prototypes*/
void SMPcpuManager_dump(ObjectDesc * self, ObjectDesc * cpuObj);

/* remote functions */
/* register_Scheduler */
static void remote_SMPcpuManager_register_Scheduler(struct func_parameter4
						    *data)
{
	_SMPcpuManager_register_Scheduler(data->cpu_id, data->domain, data->new_sched, data->Sched_register);
}

/* dump */
static void remote_SMPcpuManager_dump(struct func_parameter2 *data)
{
	SMPcpuManager_dump(data->self, data->cpuObj);
}
#endif

void SMPcpuManager_register_LLScheduler(ObjectDesc * self, ObjectDesc * cpuObj, ObjectDesc * new_sched)
{
	CPUDesc *cpu = obj2cpuDesc(cpuObj);
	int cpu_id = cpu->cpu_id;
#ifdef JAVASCHEDULER
	int i = 0;
	ASSERTCPU(cpu);
	if (cpu == NULL)
		sys_panic("SMPCPUManager::register_LLScheduler: CPU Object not valid\n");
	if (new_sched == NULL)
		sys_panic("SMPCPUManager::register_LLScheduler: can't install NULL Schedulern\n");
	/* check if this Scheduler is already registered */
#ifdef SMP
	for (i = 0; i < num_processors_online; i++)
#endif
		if (new_sched == CpuInfo[i]->LowLevel.SchedObj)
			sys_panic("SMPCPUManager::register_LLScheduler: this Scheduler is already installed (on CPU %d)\n", i);

	printf("SMPCPUManager:register_LLScheduler: in Domain %s\n", curdom()->domainName);
	_SMPcpuManager_register_Scheduler(cpu_id, curdom(), new_sched, LLSched_register);
#else
	printf("SMPCPUManager::register_LLScheduler: JAVASCHEDULER not defined\n");
#endif
}


void register_HLScheduler(CPUDesc * cpu, DomainDesc * InstallDomain, DomainDesc * ObjDomain, ObjectDesc * new_sched)
{
	int cpu_id = cpu->cpu_id;
#ifdef JAVASCHEDULER
	HLSchedDesc *new_HLSchedDesc;

	if (cpu == NULL)
		sys_panic("SMPCPUManager::register_HLScheduler: CPU Object not valid\n");
	if (new_sched == NULL)
		sys_panic("SMPCPUManager::register_HLScheduler: can't install NULL Scheduler\n");
	/* todo: check if this Scheduler is already registered */

	if (InstallDomain->Scheduler[cpu_id] != NULL)
		sys_panic("SMPCPUManager::register_HLScheduler: reregistering is not allowed yet (Domain: %s)\n",
			  InstallDomain->domainName);

	new_HLSchedDesc = createHLSchedDesc(ObjDomain, new_sched);
	_SMPcpuManager_register_Scheduler(cpu_id, InstallDomain,
					  /* FIXME HACK */
					  (ObjectDesc *) new_HLSchedDesc, HLSched_register);
#else
	printf("register_HLScheduler: JAVASCHEDULER not defined!!!\n");
#endif
}

static void SMPcpuManager_register_HLScheduler(ObjectDesc * self, ObjectDesc * cpuObj, ObjectDesc * new_sched,
					       ObjectDesc * domainObj)
{
#ifdef JAVASCHEDULER
	CPUDesc *cpu = obj2cpuDesc(cpuObj);
	DomainDesc *domain = obj2domainDesc(domainObj);
	register_HLScheduler(cpu, domain, curdom(), new_sched);
	if (domain == curdom()) {
		sys_panic("not tested");
		//curthr()->schedulingDomain = curdom();
	}
#else
	printf("SMPCPUManager::register_HLScheduler: JAVASCHEDULER not defined!!!\n");
#endif
}

static void SMPcpuManager_unregister_HLScheduler(ObjectDesc * self, ObjectDesc * cpuObj, ObjectDesc * sched,
						 ObjectDesc * domainObj)
{
	CPUDesc *cpu = obj2cpuDesc(cpu);
	DomainDesc *domain = obj2domainDesc(domainObj);
	int cpu_id = cpu->cpu_id;
#ifdef JAVASCHEDULER
	printf("unregistering may result in errors if a Portal of Domain %s is called or if a Thread is blocked in a Portal\n",
	       domain->domainName);
//     monitor(NULL);

	if (cpu == NULL)
		sys_panic("SMPCPUManager::unregister_HLScheduler: CPU Object not valid\n");
	if (sched == NULL)
		sys_panic("SMPCPUManager::unregister_HLScheduler: can't remove NULL Scheduler\n");

	if (domain->Scheduler[cpu_id]->SchedObj != sched)
		sys_panic("SMPCPUManager::unregister_HLScheduler: invalid Parameters (Domain: %s)\n", domain->domainName);
	if (cpu_id != get_processor_id()) {
		sys_panic("SMPCPUManager::unregister_HLScheduler: SMP System not yet supported\n");
	}
	HLSched_unregister(domain);
#else
	printf("SMPCPUManager::unregister_HLScheduler: JAVASCHEDULER not defined!!!\n");
#endif
}
static void SMPcpuManager_swap_HLScheduler(ObjectDesc * self, ObjectDesc * cpuObj, ObjectDesc * domainObj, ObjectDesc * oldSched,
					   ObjectDesc * newSched)
{
	CPUDesc *cpu = obj2cpuDesc(cpu);
	DomainDesc *domain = obj2domainDesc(domainObj);
	DISABLE_IRQ;
	SMPcpuManager_unregister_HLScheduler(self, cpuObj, oldSched, domainObj);
	SMPcpuManager_register_HLScheduler(self, cpuObj, newSched, domainObj);
	RESTORE_IRQ;
}


static void _SMPcpuManager_register_Scheduler(int cpu_id, DomainDesc * domain, ObjectDesc * new_sched,
					      Sched_register_fkt Sched_register)
{
	ASSERTDOMAIN(domain);
	if (cpu_id != get_processor_id()) {
#ifdef SMP
		struct func_parameter4 data;
		printf("registering scheduler on remote CPU %d\n", cpu_id);
		data.cpu_id = cpu_id;
		data.domain = domain;
		data.new_sched = new_sched;
		data.Sched_register = Sched_register;
		smp_call_function(cpu_id, remote_SMPcpuManager_register_Scheduler, &data, 1, NULL);
		return;
#else
		sys_panic("_SMPCPUManager::register_Scheduler called with cpu_id=%d\n but this Kernel does not support SMP\n",
			  cpu_id);
#endif
	}
	Sched_register(domain, new_sched);

	printf("Scheduler for CPU %d registered\n", cpu_id);
	return;
}



int intr_tsc_lo = 0;
int intr_tsc_hi = 0;
static int SMPcpuManager_test(ObjectDesc * self, int i)
{				//ttt
//    return get_processor_id();
	return (intr_tsc_lo & 0x7fffffff);
}
static int SMPcpuManager_test2(ObjectDesc * self, int i)
{				//ttt
/*   unsigned long long v1,v2;
    asm volatile("rdtsc" : "=A" (v1) : );
    for (i=0; i<10000000; i++)
	get_processor_id();
    asm volatile("rdtsc" : "=A" (v2) : );
    return (unsigned long)((v2-v1)/(unsigned long long)500000);
*/
	return (intr_tsc_hi & 0x7fffffff);
}

static int SMPcpuManager_test_setAPICTimer(ObjectDesc * self, int i)
{				//ttt
#define		APIC_TMICT	0x380	// Initial Count Reg. for Timer  (r/w)
#ifdef APIC
//obsolete call LLSchedulerSupport.tuneTimer
//  ack_APIC_irq();
	apic_write(APIC_TMICT, i / 16);
#else
//    setTimer0(0x26, i);
#endif
	return 0;
}

static jboolean SMPcpuManager_start(ObjectDesc * self, ObjectDesc * cpuState, ObjectDesc * cpuObj)
{
	CPUDesc *cpu = obj2cpuDesc(cpuObj);
	ThreadDesc *thread = cpuState2thread(cpuState);
	if (cpuObj == NULL) {
		sys_panic("SMPCPUManager::start: CPU Object not valid\n");
		return JNI_FALSE;
	}
#ifdef SMP
	sys_panic("SMPCPUManager::start: to be implemented\n");
	//     thread->curCpuId = cpu->cpu_id;
#endif
	return cpuManager_start(self, cpuState);
}
static jboolean SMPcpuManager_unblock(ObjectDesc * self, ObjectDesc * cpuState, ObjectDesc * cpuObj)
{
	CPUDesc *cpu = obj2cpuDesc(cpuObj);
	ThreadDesc *thread = cpuState2thread(cpuState);
	if (cpu == NULL) {
#ifdef DEBUG
		sys_panic("SMPCPUManager::unblock: CPU Object not valid\n");
#else
		printf("SMPCPUManager::unblock: CPU Object not valid\n");
#endif
		return JNI_FALSE;
	}
#ifdef SMP
	thread->curCpuId = cpu->cpu_id;
#endif
	return cpuManager_unblock(self, cpuState);
}

void SMPcpuManager_dump(ObjectDesc * self, ObjectDesc * cpuObj)
{				//ttt
	CPUDesc *cpu = obj2cpuDesc(cpuObj);
	int cpu_id = cpu->cpu_id;
	if (cpu == NULL)
		sys_panic("SMPCPUManager::dump: CPU Object not valid\n");

	if (cpu_id != get_processor_id()) {
#ifdef SMP
		struct func_parameter2 data;
		data.self = self;
		data.cpuObj = cpu;
		printf("dumping RunQ on remote CPU %d\n", cpu_id);
		smp_call_function(cpu_id, remote_SMPcpuManager_dump, &data, 1, NULL);
#else
		sys_panic("SMPCPUManager::dump called with cpu_id=%d\n but this Kernel does not support SMP\n", cpu_id);
#endif
		return;
	}
#ifndef NEW_SCHED
#else
	Sched_dump();
#endif
}

static void SMPcpuManager_sendIPI(ObjectDesc * self, ObjectDesc * dest_cpuObj, jint vector)
{
	CPUDesc *dest_cpu = obj2cpuDesc(dest_cpuObj);
	if (dest_cpu == NULL)
		sys_panic("SMPCPUManager::sendIPI: CPU Object (destination) not valid\n");
#ifdef SMP
	if (smp_found)
		send_IPI(dest_cpu->cpu_id, vector);
	else
		debugz(("can't send IPI to CPU %d in a Monoprocessor System\n", dest));
#else
	debugz(("can't send IPI in because this Kernel does not support SMP\n"));
#endif
}

static ObjectDesc *SMPcpuManager_getMyCPU(ObjectDesc * self)
{
	ASSERTCPU(curdom()->cpu[get_processor_id()]);
	return cpuDesc2Obj(curdom()->cpu[get_processor_id()]);
}

static ObjectDesc *SMPcpuManager_getCPU(ObjectDesc * self, int nr)
{
	int i, n = 0;
	DomainDesc *domain = curdom();
	for (i = 0; i < MAX_NR_CPUS; i++)
		if (domain->cpu[i] != NULL)
			if (n++ == nr)
				break;


	if (i == MAX_NR_CPUS)
		sys_panic("SMPcpuManager::getCPU: Array out of bounds (max:%d, index:%d)", n - 1, nr);
	ASSERTCPU(curdom()->cpu[i]);
	return cpuDesc2Obj(curdom()->cpu[i]);
}

static jint SMPcpuManager_getNumCPUs(ObjectDesc * self)
{
#ifdef SMP
	int i, n = 0;
	DomainDesc *domain = curdom();
	/*printf("SMPcpuManager_getNumCPUs called from Domain %s\n",curdom()->domainName); */
	for (i = 0; i < MAX_NR_CPUS; i++)
		if (domain->cpu[i] != NULL)
			n++;

	return n;
	//ttt   return num_processors_online;
#else
	return 1;
#endif
}

static ThreadDesc *SMPcpuManager_getCPUState(ObjectDesc * self)
{
	sys_panic("obsolete");
/* #ifdef KERNEL */
/*      return __current[get_processor_id()]; */
/* #else */
/*      sys_panic("should never be executed (in emulation mode)"); */
/*      return 0; */
/* #endif */
	return 0;		/* to satisfy compiler */
}

static ClassDesc *mutexClass;

static ObjectDesc *SMPcpuManager_mutex_create(ObjectDesc * self)
{				//ttt
	ObjectDesc *mutexInstance;
#if 0
	u4_t aDEP;
	aDEP = init_zero_dep_from_class_without_thread(mutexClass, NULL);
	mutexInstance = aDEP->obj;
	mutexInstance->data[0] = SPIN_LOCK_UNLOCKED;
//    mutex_init(mutexInstance->data);  
#endif
	sys_panic("not testet");
	return mutexInstance;
}

MethodInfoDesc SMPcpuManagerMethods[] = {
	{"register_LLScheduler", "(Ljx/zero/CPU;Ljx/zero/Scheduler;)V",
	 (code_t) SMPcpuManager_register_LLScheduler}
	,
/*  {"register_HLScheduler"  ,"(Ljx/zero/CPU;Ljx/zero/Scheduler;Ljx/zero/Domain;)V",(code_t)SMPcpuManager_register_HLScheduler}, 
  {"unregister_HLScheduler","(Ljx/zero/CPU;Ljx/zero/Scheduler;Ljx/zero/Domain;)V",(code_t)SMPcpuManager_unregister_HLScheduler}, 
*/ {"swap_HLScheduler", "", (code_t) SMPcpuManager_swap_HLScheduler}
	,
	{"test", "", (code_t) SMPcpuManager_test}
	,
	{"test2", "", (code_t) SMPcpuManager_test2}
	,
	{"test_setAPICTimer", "", (code_t) SMPcpuManager_test_setAPICTimer}
	,
	{"start", "(Ljx/zero/CPUState;Ljx/zero/CPU;)V",
	 (code_t) SMPcpuManager_start}
	,
	{"unblock", "(Ljx/zero/CPUState;Ljx/zero/CPU;)V",
	 (code_t) SMPcpuManager_unblock}
	,
	{"dump", "(Ljx/zero/CPU;)V", (code_t) SMPcpuManager_dump}
	,
	{"getCPUState", "(Ljx/zero/CPU;)Ljx/zero/CPUState;",
	 (code_t) SMPcpuManager_getCPUState}
	,
	{"getMyCPU", "()Ljx/zero/CPU;", (code_t) SMPcpuManager_getMyCPU}
	,
	{"getNumCPUs", "()I", (code_t) SMPcpuManager_getNumCPUs}
	,
	{"getCPU", "(I)Ljx/zero/CPU;", (code_t) SMPcpuManager_getCPU}
	,
	{"sendIPI", "(Ljx/zero/CPU;I)V", (code_t) SMPcpuManager_sendIPI}
	,
	{"createMutex", "()Ljx/zero/Mutex;",
	 (code_t) SMPcpuManager_mutex_create}
	,
};

void init_smpcpumanager_portal()
{
	init_zero_dep_without_thread("jx/zero/SMPCPUManager", "SMPCPUManager", SMPcpuManagerMethods, sizeof(SMPcpuManagerMethods),
				     "<jx/zero/SMPCPUManager>");
}
