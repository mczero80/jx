#include "all.h"

//#define DEBUG_IRQ 1

#define debugz(x)

char *irqThreadName[] = {
	"IRQThread0",
	"IRQThread1",
	"IRQThread2",
	"IRQThread3",
	"IRQThread4",
	"IRQThread5",
	"IRQThread6",
	"IRQThread7",
	"IRQThread8",
	"IRQThread9",
	"IRQThread10",
	"IRQThread11",
	"IRQThread12",
	"IRQThread13",
	"IRQThread14",
	"IRQThread15"
};

DEPDesc *ihandlers[NUM_IRQs];	//obsolete
DomainDesc *idomains[MAX_NR_CPUS][NUM_IRQs];
ThreadDesc *ithreads[MAX_NR_CPUS][NUM_IRQs];
u4_t imissed[NUM_IRQs];		//obsolete
u4_t idelayed[NUM_IRQs];	//obsolete
u4_t iprocessed[MAX_NR_CPUS][NUM_IRQs];
code_t ifirstlevel_code[MAX_NR_CPUS][NUM_IRQs];
ObjectDesc *ifirstlevel_object[MAX_NR_CPUS][NUM_IRQs];
u4_t ifirstlevel_happened[MAX_NR_CPUS][NUM_IRQs];
u4_t ifirstlevel_processed[MAX_NR_CPUS][NUM_IRQs];
struct irqInfos iInfos[NUM_IRQs];

#ifndef KERNEL
static void simIRQfunc(unsigned int irq)
{
	sys_panic("simIRQfunc: this should never be called");
}
static struct irqfunctions simIRQ = {
	"simulated",
	simIRQfunc,		//ack
	simIRQfunc,		//enable
	simIRQfunc,		//disable
};
#endif

static void undefIRQfunc(unsigned int irq)
{
	sys_panic("undefIRQfunc: this should never be called");
}
static struct irqfunctions undefinedIRQ = {
	"undefined",
	undefIRQfunc,		//ack
	undefIRQfunc,		//enable
	undefIRQfunc,		//disable
};

/* init the data defined above
   called by pic_init_pmode in irq.c */
void init_irq_data(void)
{
	int i, c;
	for (i = 0; i < NUM_IRQs; i++) {
		ihandlers[i] = NULL;
		imissed[i] = 0;
		idelayed[i] = 0;
		for (c = 0; c < MAX_NR_CPUS; c++) {
			idomains[c][i] = NULL;
			ithreads[c][i] = NULL;
			ifirstlevel_code[c][i] = NULL;
			ifirstlevel_object[c][i] = NULL;
			iprocessed[c][i] = 0;
			ifirstlevel_happened[c][i] = 0;
			ifirstlevel_processed[c][i] = 0;
		}
		if (i < 16) {
			iInfos[i].used = 1;
#ifdef KERNEL
			iInfos[i].functions = &legacyPIC;
			iInfos[i].vector_number = 0x20 + i;
#else
			iInfos[i].functions = &simIRQ;
			iInfos[i].vector_number = 0;
#endif
			iInfos[i].apic = -1;
		} else {
			iInfos[i].used = 0;
			iInfos[i].functions = &undefinedIRQ;
			iInfos[i].vector_number = 0;
			iInfos[i].apic = -1;
		}
	}			// next i
}

/*
 * IRQDEP
 */


static void start_irq_thread()
{
	sys_panic("SHOULD NOT BE CALLED");
}


void irq_installFirstLevelHandler(ObjectDesc * self, int irq, ObjectDesc * handler)
{
	int error = 0;
	DomainDesc *sourceDomain = curdom();
	jint index = findDEPMethodIndex(sourceDomain,
					"jx/zero/FirstLevelIrqHandler",
					"interrupt",
					"()V");
	int cpu_id = get_processor_id();
	if (irq > NUM_IRQs) {
		printf("irq::installFirstLevelHandle: irq %d out of range (0-%d)\n", irq, NUM_IRQs);
		exceptionHandlerMsg(THROW_RuntimeException, "irq::installFirstLevelHandle: irq out of range");
	}
	DISABLE_IRQ;
#ifdef SMP
	spinlock;
#endif
	if (ifirstlevel_object[cpu_id][irq] != NULL) {
		error = 1;
		goto finish;
	}
#ifdef DEBUG_IRQ
	printf("INSTALL %d for CPU %d, domain=%s\n", irq, cpu_id, sourceDomain->domainName);
	printStackTraceNew("IRQ INSTALL");
#endif
	ifirstlevel_object[cpu_id][irq] = handler;
	ifirstlevel_code[cpu_id][irq] = handler->vtable[index];

	idomains[cpu_id][irq] = sourceDomain;
	ithreads[cpu_id][irq] = createThread(sourceDomain, start_irq_thread, NULL, STATE_AVAILABLE, SCHED_CREATETHREAD_NORUNQ);
	ithreads[cpu_id][irq]->isInterruptHandlerThread = 1;
	setThreadName(ithreads[cpu_id][irq], irqThreadName[irq], NULL);
	printf("  Thread   %p   stack=%p, stackTop=%p\n", ithreads[cpu_id][irq], ithreads[cpu_id][irq]->stack,
	       ithreads[cpu_id][irq]->stackTop);

      finish:
#ifdef SMP
	spinunlock;
#endif
	RESTORE_IRQ;

	if (error)
		exceptionHandlerMsg(THROW_RuntimeException, "IRQ handler already there");


}

void irq_panic()
{
	sys_panic("IRQPANIC");	/* should never be called */
}

#if defined(KERNEL)

#if defined(KERNEL) || defined(JAVASCHEDULER)
ThreadDesc *icurrent[MAX_NR_CPUS];
int inumber[MAX_NR_CPUS];
#ifdef APIC
int iclock[MAX_NR_CPUS];
#endif
#endif

int check_current;
void irq_exit(int cpuID)
{
	ifirstlevel_processed[cpuID][inumber[cpuID]]++;
	ackIRQ(inumber[cpuID]);

#ifdef DEBUG
	check_current = 0;
#endif
	Sched_deactivate_interrupt_thread(icurrent[cpuID]);
	*curthrP() = icurrent[cpuID];
	ASSERT(ithreads[cpuID][inumber[cpuID]]->state == STATE_RUNNABLE);
	ithreads[cpuID][inumber[cpuID]]->state = STATE_AVAILABLE;
#ifdef DEBUG
	check_current = 1;
#endif

//ttt  if (read_APIC_ticks() == 0)  // not changed??
//ttt    set_APIC_ticks(iclock[cpuID]);   // restart scheduler

	returnfromirq();	/* will never return */
	/* this function must not return, because the return address (on the stack) is not valid!! */
	/* there is still the pointer to the "ifirstlevel_object" on the stack!! */
	asm(".global irq_exit_end;" " irq_exit_end:");
}

void irq_first_level_handler(unsigned int cpuID, ContextDesc ctx, u4_t irq)
{
	u4_t *sp;
	ThreadDesc *curt;
	code_t c = ifirstlevel_code[cpuID][irq];
	ObjectDesc *o = ifirstlevel_object[cpuID][irq];
#ifdef APIC
	if (apic_found) {
//ttt      iclock[cpuID] = read_APIC_ticks();  // save scheduler clock
//ttt      set_APIC_ticks(0);        // stop scheduler
	}
#endif
#ifdef IRQ_STATISTICS
	ifirstlevel_happened[cpuID][irq]++;
#endif
	if (o == NULL) {
		ackIRQ(irq);
		returnfromirq();
		sys_panic("never reached");
	}
	/*printf("*****CALLING FIRST LEVEL HANDLER* %p %p*****\n", o, c); */

	Sched_activate_interrupt_thread(ithreads[cpuID][irq]);

#ifdef DEBUG
	check_current = 0;
#endif
	ASSERT(ithreads[cpuID][irq]->state == STATE_AVAILABLE);
	ithreads[cpuID][irq]->state = STATE_RUNNABLE;

	curt = curthr();
#ifdef DEBUG
	/* check whether stack of interrupted thread is consistent */
	checkStackTrace(curt, (u4_t *) (curt->context[PCB_EBP]));
#endif
	if (c == NULL)
		sys_panic("No code for firstlevel-handler.");
	icurrent[cpuID] = curt;
	inumber[cpuID] = irq;
	*curthrP() = ithreads[cpuID][irq];
	curt = curthr();
#ifdef DEBUG
	check_current = 1;
#endif
	sp = curt->stackTop - 1;
	stack_push(&sp, (u4_t) cpuID);
	stack_push(&sp, (u4_t) o);
	stack_push(&sp, (u4_t) irq_exit);
	curt->context[PCB_ESP] = sp;
	curt->context[PCB_EBP] = NULL;
	curt->context[PCB_EIP] = c;
	/*printf("***->CPU%d, IRQ%d\n",cpuID,irq); */

	activate_irq(curt);
}

#endif				/* KERNEL */

#if 0
#if defined(JAVASCHEDULER) && !defined(KERNEL)
void sim_timer_irq()
{
	ThreadDesc *curt;
	jint *sp;
	int irq = 0;
	int cpuID = get_processor_id();
	code_t c = ifirstlevel_code[cpuID][irq];
	ObjectDesc *o = ifirstlevel_object[cpuID][irq];
#ifdef IRQ_STATISTICS
	ifirstlevel_happened[cpuID][irq]++;
#endif
	if (o == NULL)
		return;
#ifdef DEBUG
	check_current = 0;
#endif
	printf("***->CPU%d, IRQ%d\n", cpuID, irq);
	/* switch state from AVAILABLE to RUNNING */
	if (!cas((u4_t *) (&ithreads[cpuID][irq]->state), (u4_t) STATE_AVAILABLE, (u4_t) STATE_RUNNABLE)) {
		sys_panic("the interrupt service thread (%p) (IRQ %d) is not AVAILABLE\n", ithreads[cpuID][irq], irq);
	}
	if (c == NULL)
		sys_panic("No code for firstlevel-handler.");
	icurrent[cpuID] = curthr();
	inumber[cpuID] = irq;
	*curthrP() = ithreads[cpuID][irq];	/* TODO: set esp */
	curt = curthr();
#ifdef DEBUG
	check_current = 1;
#endif
	sp = curt->stackTop;
	stack_push(&sp, (jint) cpuID);
	stack_push(&sp, o);
	stack_push(&sp, irq_panic);
	curt->context[PCB_ESP] = sp;
	curt->context[PCB_EIP] = c;

	/*printf("    sw to %p (%s)  from %p, (%s)\n",curt,curt->name,icurrent[cpuID],icurrent[cpuID]->name); */

	destroy_switch_to(curthrP(), curt);

	/* not necessary for timer IRQ and SCHEUDLER */
/*
  ifirstlevel_processed[cpuID][inumber[cpuID]]++;
#ifdef DEBUG    
  check_current = 0;
#endif
  *curthrP() = icurrent[cpuID];
    printf("back from %p (%s)   in %p (%s)\n",curt,curt->name,curthr(),curthr()->name);
    
  ASSERT(ithreads[cpuID][inumber[cpuID]]->state == STATE_RUNNABLE);
  ithreads[cpuID][inumber[cpuID]]->state = STATE_AVAILABLE;
#ifdef DEBUG    
  check_current = 1;
#endif
*/
}
#endif				/* JAVASCHEDULER && not KERNEL */
#endif

void irq_enableIRQ(ObjectDesc * self, jint irq)
{
	debugz(("IRQ enableIRQ %ld\n", irq));
#ifdef KERNEL
#   ifdef NO_TIMER_IRQ
	if (irq == 0) {		/* do not enable IRQ 0 (from JAVA level) if timer is not needed */
		printf("Thread %d.%d wants to enable IRQ 0 but NO_TIMER_IRQ is defined\n", TID(curthr()));
		return;
	}
#   endif
	enableIRQ(irq);
#endif
}

void irq_disableIRQ(ObjectDesc * self, jint irq)
{
	debugz(("IRQ disableIRQ %ld\n", irq));
}

void irq_enableAll(ObjectDesc * self)
{
	debugz(("IRQ enableAll\n"));
}

void irq_disableAll(ObjectDesc * self)
{
	debugz(("IRQ disableAll\n"));
}

void irq_set_destination(ObjectDesc * self, jint irq, jint new_dest)
{
#ifdef SMP
	if (smp_found)
		set_irq_destination(irq, new_dest);
	else
		debugz(("IRQ Destination can not be changed %d\n", irq));
#else
	debugz(("IRQ Destiantion can not be changed %d\n", irq));
#endif
}

MethodInfoDesc irqMethods[] = {
	{"installFirstLevelHandler", "", irq_installFirstLevelHandler},
	{"enableIRQ", "", irq_enableIRQ},
	{"disableIRQ", "", irq_disableIRQ},
	{"enableAll", "", irq_enableAll},
	{"disableAll", "", irq_disableAll},
	{"set_destination", "", irq_set_destination},
};

void init_irq_portal()
{
	init_zero_dep_without_thread("jx/zero/IRQ", "IRQ", irqMethods, sizeof(irqMethods), "<jx/zero/IRQ>");
}
