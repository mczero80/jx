#include "all.h"

/*
 *
 * LLschedulerSupport Portal
 *
 */

extern ThreadDesc *icurrent[];

/* ONLY called from IRQ-Handler (IRQs disabled) */
static ObjectDesc *LLschedulerSupport_preemptCurrentThread(ObjectDesc * self)
{
#ifdef JAVASCHEDULER
	/* may only be called in interrupt handler */
	int cpu_id = get_processor_id();
	DomainDesc *domain = icurrent[cpu_id]->schedulingDomain;
	ASSERTCLI;
	sched_dprintf(2, "CPU%d: LLschedulerSupport_preemptCurrentThread called for dom:%s (Thread is from %s) \n", cpu_id,
		      domain->domainName, icurrent[cpu_id]->domain->domainName);

	if (domain->Scheduler[cpu_id] == NULL)
		sys_panic("LLschedulerSupport_preemptCurrentThread: no Scheduler for Domain %d\n", domain->domainName);

	if (icurrent[cpu_id] == domain->Scheduler[cpu_id]->SchedActivateThread) {
		if (call_JAVA_method0
		    (domain->Scheduler[cpu_id]->SchedObj, domain->Scheduler[cpu_id]->SchedThread,
		     domain->Scheduler[cpu_id]->SchedPreempted_code) == JNI_FALSE) {
			/* Thread schould be resumed at the next CPU slot */
#ifdef DEBUG
			if (domain->Scheduler[cpu_id]->resume == JNI_TRUE)
				sys_panic("System Error in LLschedulerSupport_preemptCurrentThread: resume schould be false!");
#endif
			ASSERT(domain->Scheduler[cpu_id]->resume == JNI_FALSE);
			domain->Scheduler[cpu_id]->resume = JNI_TRUE;
			return thread2CPUState(icurrent[cpu_id]);
		}
		/* else:  everythig is ok (the Scheduler has handled the problem ) */
	}
//#ifdef VISIBLE_PORTALS
//    icurrent[cpu_id]->depSwitchBack=JNI_FALSE;
//#endif
	call_JAVA_method1(domain->Scheduler[cpu_id]->SchedObj, domain->Scheduler[cpu_id]->SchedThread,
			  domain->Scheduler[cpu_id]->preempted_code, thread2CPUState(icurrent[cpu_id]));
	return thread2CPUState(icurrent[cpu_id]);
#else
	sys_panic("compiled without JAVASCHEDULER defined!!");
#endif
}

/* ONLY called from IRQ-Handler (IRQs disabled) */
ObjectDesc *LLschedulerSupport_interruptCurrentThread(ObjectDesc * self)
{
#ifdef JAVASCHEDULER
	/* may only be called in interrupt handler */
	int cpu_id = get_processor_id();
	DomainDesc *domain = icurrent[cpu_id]->schedulingDomain;
	ASSERTCLI;
	sched_dprintf(2, "CPU%d: LLschedulerSupport_interruptCurrentThread called for dom:%s (Thread is from %s) \n", cpu_id,
		      domain->domainName, icurrent[cpu_id]->domain->domainName);

#ifdef DEBUG
	if (domain->Scheduler[cpu_id] == NULL)
		sys_panic("bug detected in JAVAschedulerSupport_saveCurrentThread\n");
#endif

	if (icurrent[cpu_id] == domain->Scheduler[cpu_id]->SchedActivateThread) {
		if (call_JAVA_method0
		    (domain->Scheduler[cpu_id]->SchedObj, domain->Scheduler[cpu_id]->SchedThread,
		     domain->Scheduler[cpu_id]->SchedInterrupted_code) == JNI_FALSE) {
			/* Thread schould be resumed at the next CPU slot */
#ifdef DEBUG
			if (domain->Scheduler[cpu_id]->resume == JNI_TRUE)
				sys_panic("System Error in LLschedulerSupport_interruptCurrentThread: resume schould be false!");
#endif
			ASSERT(domain->Scheduler[cpu_id]->resume == JNI_FALSE);
			domain->Scheduler[cpu_id]->resume = JNI_TRUE;
			return thread2CPUState(icurrent[cpu_id]);
		}
		/* else:  everythig is ok (the Scheduler has handled the problem ) */
	}
//#ifdef VISIBLE_PORTALS
//    icurrent[cpu_id]->depSwitchBack=JNI_FALSE;
//#endif

	call_JAVA_method1(domain->Scheduler[cpu_id]->SchedObj, domain->Scheduler[cpu_id]->SchedThread,
			  domain->Scheduler[cpu_id]->interrupted_code, thread2CPUState(icurrent[cpu_id]));
	return thread2CPUState(icurrent[cpu_id]);
#else
	sys_panic("compiled without JAVASCHEDULER defined!!");
#endif

}

// called from IRQ-Handler and ... (IRQs disabled)
static void LLschedulerSupport_activateDomain(ObjectDesc * self, ObjectDesc * domainObj)
{
#ifdef JAVASCHEDULER
	DomainDesc *domain;
	int cpu_id;
	ThreadDesc *newThread;
	ASSERTCLI;
	ASSERTOBJECT(domainObj);
	domain = obj2domainDesc(domainObj);
	ASSERTDOMAIN(domain);
	cpu_id = get_processor_id();
	/*printf("CPU%d: activate Dom: %s\n",cpu_id, domain->domainName); */
	//cli();
	if (domain == NULL)
		sys_panic("LLschedulerSupport_activateDomain called with NULL Domain\n");

	/* this thread is available soon */
	curthr()->state = STATE_AVAILABLE;


	if (domain == domainZero) {
		sched_dprintf(1, "CPU%d: activate Dom: %s\n", cpu_id, domain->domainName);
		DomZero_activated(cpu_id);	//ttt
		sys_panic("should not return\n");
	} else {
		if (domain->Scheduler[cpu_id] == NULL)
			sys_panic("LLschedulerSupport_activateDomain: Domain %s has no HLScheduler\n", domain->domainName);
		if (domain->Scheduler[cpu_id]->resume == JNI_TRUE) {
			sched_dprintf(1, "CPU%d: resuming Dom: %s\n", cpu_id, domain->domainName);
			domain->Scheduler[cpu_id]->resume = JNI_FALSE;
			destroy_switch_to(curthrP(), domain->Scheduler[cpu_id]->SchedActivateThread);
			sys_panic("schould not return\n");
		}
		sched_dprintf(1, "CPU%d: activate Dom: %s\n", cpu_id, domain->domainName);

		curthr()->state = STATE_AVAILABLE;
		destroy_call_JAVA_function(domain->Scheduler[cpu_id]->SchedObj, domain->Scheduler[cpu_id]->SchedActivateThread,
					   domain->Scheduler[cpu_id]->activated_code, CALL_WITH_ENABLED_IRQS);
		sys_panic("schould not return\n");
	}
	sys_panic("never reached\n");
#else
	sys_panic("SCHEDULING is not supported by this kernel");
#endif
}

// called from IRQ-Handler and ... (IRQs disabled)
static void LLschedulerSupport_activateIdleThread(ObjectDesc * self)
{
#ifdef JAVASCHEDULER
	ASSERTCLI;
	sched_dprintf(1, "CPU%d: activate IdleThread\n", get_processor_id());
	curthr()->state = STATE_AVAILABLE;
	destroy_switch_to(curthrP(), idle_thread);
	sys_panic("never reached\n");
#else
	sys_panic("JAVASCHEDULING is not supported by this kernel");
#endif
}

static jboolean LLschedulerSupport_isRunnable(ObjectDesc * self, ObjectDesc * domainObj)
{
#ifdef JAVASCHEDULER
	DomainDesc *domain;
	int cpu_id;
	ASSERTOBJECT(domainObj);
	domain = obj2domainDesc(domainObj);
	ASSERTDOMAIN(domain);
	cpu_id = get_processor_id();

	if (domain->Scheduler[cpu_id] == NULL)
		sys_panic("LLschedulerSupport_isRunnable: Domain %s has no HLScheduler\n", domain->domainName);
	return domain->Scheduler[cpu_id]->maybeRunnable;

#endif
}

jint LLschedulerSupport_getDomainTimeslice(ObjectDesc * self)
{
#ifdef KERNEL
	return schedule_counter_init;
#else
	/* in msec */
	struct itimerval value;
	getitimer(ITIMER_REAL, &value);
	return (value.it_interval.tv_sec * 1000) + (value.it_interval.tv_usec / 1000);
#endif
}

static jint LLschedulerSupport_readTimeslice_oddtime(ObjectDesc * self)
{
#ifdef JAVASCHEDULER
#   ifdef KERNEL
#      ifdef APIC
	if (apic_found)
		return read_APIC_clock();
	else
		return schedule_counter;
#      else
	return schedule_counter;
#      endif
#   else
	/* in msec */
	struct itimerval value;
	getitimer(ITIMER_REAL, &value);
	return (value.it_value.tv_sec * 1000) + (value.it_value.tv_usec / 1000);
#   endif
#else
	sys_panic("LLschedulerSupport::readTimeslice_oddtime: JAVASCHEDULER not defined!!\n");
#endif

}

void LLschedulerSupport_dumpHLSched(ObjectDesc * self, ObjectDesc * domainObj)
{
#ifdef JAVASCHEDULER
	DomainDesc *domain = obj2domainDesc(domainObj);
	DISABLE_IRQ;

	sched_dprintf(2, "dumping Dom: %s       (Thr1:%p, Thr2:%p)\n", domain->domainName,
		      domain->Scheduler[get_processor_id()]->SchedThread,
		      domain->Scheduler[get_processor_id()]->SchedActivateThread);
	call_JAVA_method0(domain->Scheduler[get_processor_id()]->SchedObj, domain->Scheduler[get_processor_id()]->SchedThread,
			  domain->Scheduler[get_processor_id()]->dump_code);
	RESTORE_IRQ;
#endif
}

static void LLschedulerSupport_dumpDomain(ObjectDesc * self, ObjectDesc * domainObj)
{
	DomainDesc *domain = obj2domainDesc(domainObj);
	printf("  %s at 0x%lx: \n", domain->domainName, domain);
}

static void LLschedulerSupport_printDomainName(ObjectDesc * self, ObjectDesc * domainObj)
{
	DomainDesc *domain = obj2domainDesc(domainObj);
	printf("%s", domain->domainName);
}

void JAVAschedulerSupport_setTimeslice(ObjectDesc * self, int time);

MethodInfoDesc LLschedulerSupportMethods[] = {
	{"preemptCurrentThread", "",
	 (code_t) LLschedulerSupport_preemptCurrentThread},
	{"interruptCurrentThread", "",
	 (code_t) LLschedulerSupport_interruptCurrentThread},
	{"activateDomain", "", (code_t) LLschedulerSupport_activateDomain},
	{"activateIdleThread", "",
	 (code_t) LLschedulerSupport_activateIdleThread},
	{"isRunnable", "", (code_t) LLschedulerSupport_isRunnable},
	{"getDomainTimeslice", "",
	 (code_t) LLschedulerSupport_getDomainTimeslice},
	{"readTimer", "",
	 (code_t) LLschedulerSupport_readTimeslice_oddtime},
	{"dumpDomain", "", (code_t) LLschedulerSupport_dumpDomain},
	{"dumpHLSched", "", (code_t) LLschedulerSupport_dumpHLSched},
	{"printDomainName", "",
	 (code_t) LLschedulerSupport_printDomainName},

	{"tuneTimer", "", (code_t) JAVAschedulerSupport_setTimeslice},
};

void init_llschedulersupport_portal()
{
	init_zero_dep_without_thread("jx/zero/LLSchedulerSupport", "LLSchedulerSupport", LLschedulerSupportMethods,
				     sizeof(LLschedulerSupportMethods), "<jx/zero/LLSchedulerSupport>");
}
