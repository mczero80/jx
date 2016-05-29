#include "all.h"

#ifdef PROFILE_EVENT_THREADSWITCH
#if 0
struct profile_event_threadswitch_s *profile_event_threadswitch_samples = NULL;
u4_t profile_event_threadswitch_n_samples = 0;
#endif
static int event_threadswitch = 0;
#endif

#ifdef PROFILE_SAMPLE_PMC

#ifdef PROFILE_SAMPLE_PMC0
jint n_pmc0_samples = 0;
jlong *pmc0_samples = NULL;
#endif

#ifdef PROFILE_SAMPLE_PMC1
jint n_pmc1_samples = 0;
jlong *pmc1_samples = NULL;
#endif

union event_selector_u {
	struct {
		unsigned int event_mask:8;
		unsigned int unit_mask:8;
		unsigned int user_mode:1;
		unsigned int os_mode:1;
		unsigned int edge_flag:1;
		unsigned int pin_control:1;
		unsigned int apic_irq:1;
		unsigned int reserved:1;
		unsigned int enable:1;
		unsigned int invert:1;
		unsigned int counter_mask:8;
	} f;
	u8_t l;
};
typedef union event_selector_u evt_sel;

#define MSR_PMC0 0xC1
#define MSR_PMC1 0xC2
#define MSR_PMC_ES0 0x186
#define MSR_PMC_ES1 0x187

#define pmc_read(counter, val) \
   __asm__ __volatile__("rdpmc" \
                        : "=A" (val) \
                        : "c" (counter))

#  ifdef KERNEL
#    define pmc_es_read(counter, val) \
   __asm__ __volatile__("rdmsr" \
                        : "=A"(val.l) \
                        : "c" (MSR_PMC_ES##counter))

#    define pmc_es_write(counter, val) \
   __asm__ __volatile__("wrmsr" \
                        : \
                        : "A"(val.l), "c" (MSR_PMC_ES##counter))

#    define pmc_write(counter, val) \
   __asm__ __volatile__("wrmsr" \
                        : \
                        : "A"(val), "c" (MSR_PMC##counter))

void pmc_init()
{
	evt_sel ev0, ev1;
	ev0.l = ev1.l = 0;

	// enable user mode perf mon count reading  
	set_cr4(get_cr4() | CR4_PCE);

#if defined(PROFILE_SAMPLE_PMC1) || defined(PROFILE_SAMPLE_PMC_DIFF)
	ev1.f.event_mask = 0x2e;
	ev1.f.unit_mask = 0x0f;
	ev1.f.user_mode = 1;
	ev1.f.os_mode = 1;
	ev1.f.enable = 1;
	ev1.f.invert = 0;
	ev1.f.counter_mask = 0;
	pmc_write(1, 10);
	pmc_es_write(1, ev1);
#endif

#ifdef PROFILE_SAMPLE_PMC0
	ev0.f.event_mask = 0x26;
	ev0.f.unit_mask = 0x0;
	ev0.f.user_mode = 1;
	ev0.f.os_mode = 1;
	ev0.f.enable = 1;
	ev0.f.invert = 0;
	ev0.f.counter_mask = 0;
	pmc_write(0, 10);
	pmc_es_write(0, ev0);
#endif

}

#  else				/* KERNEL */
void pmc_init()
{
	// FIXME 
}

#  endif			/* KERNEL */

void pmc_log()
{
#ifdef PROFILE_SAMPLE_PMC0
#  ifdef PROFILE_SAMPLE_PMC_DIFF
	u8_t a;
	if (n_pmc0_samples < MAX_PMC0_SAMPLES) {
		pmc_read(0, a);
		pmc_read(1, pmc0_samples[n_pmc0_samples]);
		pmc0_samples[n_pmc0_samples++] -= a;
	}
#  else
	if (n_pmc0_samples < MAX_PMC0_SAMPLES)
		pmc_read(0, pmc0_samples[n_pmc0_samples++]);
#  endif
#endif

#ifdef PROFILE_SAMPLE_PMC1
	if (n_pmc1_samples < MAX_PMC1_SAMPLES)
		pmc_read(1, pmc1_samples[n_pmc1_samples++]);
#endif
}

#else				/* PROFILE_SAMPLE_PMC */

void pmc_log()
{
}

#endif				/* PROFILE_SAMPLE_PMC */

#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE
jint *eip_samples = NULL;
jint n_eip_samples = 0;
#endif
#ifdef PREEMPTION_SAMPLE
jint *eip_caller_samples = NULL;
jint *preempted_samples = NULL;
#endif

#ifdef PROFILE_SAMPLE_HEAPUSAGE
jint *heap_samples = NULL;
jint n_heap_samples = 0;
#endif

#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE || defined SAMPLING_TIMER_IRQ
int do_sampling = 1;
#endif

#ifdef KERNEL
/* called by timer interrupt */
#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE
void profiler_sample(struct irqcontext_timer sc)
{
	if (do_sampling) {
#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE
		if (n_eip_samples < MAX_EIP_SAMPLES) {
			eip_samples[n_eip_samples] = sc.eip;
		}
#endif
#ifdef PREEMPTION_SAMPLE
		if (n_eip_samples < MAX_EIP_SAMPLES) {
			eip_caller_samples[n_eip_samples] = getCallerCallerIP();
			preempted_samples[n_eip_samples] = curthr();
		}
#endif
#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE
		if (n_eip_samples < MAX_EIP_SAMPLES)
			n_eip_samples++;
#endif

		pmc_log();

		/*printf("  SAMPLE eip=%p \n",sc.eip); */
	}
}
#endif				/* */
#endif				/* KERNEL */



#ifdef PROFILE_EVENT_THREADSWITCH
#if 0
void profile_event_threadswitch_from(ThreadDesc * from)
{
	if (profile_event_threadswitch_n_samples < MAX_EVENT_THREADSWITCH) {
		/*    unsigned long long st;
		   asm volatile("rdtsc" : "=A" (st) : );
		   profile_event_threadswitch_samples[profile_event_threadswitch_n_samples].timestamp = st; */
		profile_event_threadswitch_samples[profile_event_threadswitch_n_samples].from = from;
		profile_event_threadswitch_samples[profile_event_threadswitch_n_samples].ip_from = from->context[PCB_EIP];
	}
}
#endif


#if 0
#if 1
void profile_event_threadswitch_to(ThreadDesc * to)
{
	if (profile_event_threadswitch_n_samples < MAX_EVENT_THREADSWITCH) {
		/*
		   unsigned long long st;
		   asm volatile("rdtsc" : "=A" (st) : );
		 */
		profile_event_threadswitch_samples[profile_event_threadswitch_n_samples].timestamp = get_tsc();
		profile_event_threadswitch_samples[profile_event_threadswitch_n_samples].to = to;
		profile_event_threadswitch_samples[profile_event_threadswitch_n_samples].ip_to = to->context[PCB_EIP];
		profile_event_threadswitch_n_samples++;
	}
	pmc_log();
}
#else
/* use profile_event_threadswitch_samples as ringbuffer */
void profile_event_threadswitch_to(ThreadDesc * to)
{
	static int slot = 0;
	if (profile_event_threadswitch_n_samples < MAX_EVENT_THREADSWITCH)
		profile_event_threadswitch_n_samples++;
	profile_event_threadswitch_samples[slot].timestamp = get_tsc();
	profile_event_threadswitch_samples[slot].to = to;
	profile_event_threadswitch_samples[slot].ip_to = to->context[PCB_EIP];
	if (slot++ == MAX_EVENT_THREADSWITCH)
		slot = 0;
	pmc_log();
}
#endif
#endif				/*0 */
void profile_event_threadswitch_to(ThreadDesc * to)
{
//      RECORD_EVENT_INFO2(event_threadswitch, (to->domain->id << 16) | (to->id & 0xffff), to->context[PCB_EIP]);
}


#ifdef KERNEL
void profile_event_threadswitch_ip(struct irqcontext sc)
{
#if 0
	if (profile_event_threadswitch_n_samples < MAX_EVENT_THREADSWITCH) {
		/*
		   unsigned long long st;
		   asm volatile("rdtsc" : "=A" (st) : );
		 */
		profile_event_threadswitch_samples[profile_event_threadswitch_n_samples].timestamp = get_tsc();
		profile_event_threadswitch_samples[profile_event_threadswitch_n_samples].to = curthr();
		profile_event_threadswitch_samples[profile_event_threadswitch_n_samples].ip_to = sc.eip;
		profile_event_threadswitch_n_samples++;
	}
	pmc_log();
#endif
}
#endif				/* KERNEL */
#endif






void thread_profile_irq(u4_t eip)
{

#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE
	if (n_eip_samples < MAX_EIP_SAMPLES) {
		eip_samples[n_eip_samples] = eip;
	}
#endif
#ifdef PREEMPTION_SAMPLE
	if (n_eip_samples < MAX_EIP_SAMPLES) {
		eip_caller_samples[n_eip_samples] = getCallerCallerCallerIP();
		preempted_samples[n_eip_samples] = curthr();
		//      printStackTraceNew();
	}
#endif
#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE
	if (n_eip_samples < MAX_EIP_SAMPLES)
		n_eip_samples++;
#endif

#ifdef PROFILE_SAMPLE_PMC0
	if (n_pmc0_samples < MAX_PMC0_SAMPLES)
		pmc_read(0, pmc0_samples[n_pmc0_samples++]);
#endif

#ifdef PROFILE_SAMPLE_PMC1
	if (n_pmc1_samples < MAX_PMC1_SAMPLES)
		pmc_read(1, pmc1_samples[n_pmc1_samples++]);
#endif

}





void threads_profile_init()
{
	printf("init threads system - profiler part\n");

#ifdef PROFILE_EVENT_THREADSWITCH
	event_threadswitch = createNewEvent("THREADSWITCH");
#endif

#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE
	eip_samples = jxmalloc(MAX_EIP_SAMPLES * sizeof(jint) MEMTYPE_PROFILING);
#endif
#ifdef PREEMPTION_SAMPLE
	eip_caller_samples = jxmalloc(MAX_EIP_SAMPLES * sizeof(jint) MEMTYPE_PROFILING);
	preempted_samples = jxmalloc(MAX_EIP_SAMPLES * sizeof(jint) MEMTYPE_PROFILING);
#endif
#ifdef PROFILE_SAMPLE_HEAPUSAGE
	heap_samples = jxmalloc(MAX_HEAP_SAMPLES * sizeof(struct heapsample_s) MEMTYPE_PROFILING);
#endif

#ifdef PROFILE_SAMPLE_PMC
	pmc_init();
#endif

#ifdef PROFILE_SAMPLE_PMC0
	pmc0_samples = jxmalloc(MAX_PMC0_SAMPLES * sizeof(jlong) MEMTYPE_PROFILING);
#endif

#ifdef PROFILE_SAMPLE_PMC1
	pmc1_samples = jxmalloc(MAX_PMC1_SAMPLES * sizeof(jlong) MEMTYPE_PROFILING);
#endif

#if 0
#ifdef PROFILE_EVENT_THREADSWITCH
	do_event_threadswitch = 1;
	profile_event_threadswitch_n_samples = 0;
	profile_event_threadswitch_samples =
	    jxmalloc(MAX_EVENT_THREADSWITCH * sizeof(struct profile_event_threadswitch_s) MEMTYPE_PROFILING);
#endif
#endif				/*0 */
}
