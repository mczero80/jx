#include "config.h"
#ifdef APIC
#include "lapic.h"
#include "misc.h"
#include "irq.h"
#include "smp.h"
#include "config.h"
#include "timer8254.h"
#include "cpuid.h"
#include "lapic.h"

unsigned long lapic_addr = LOCAL_APIC_DEFAULT_PHYS_BASE;	// addr. of the local APIC
int lapic_version[MAX_NR_CPUS] = { APIC_VER_INTEGRATED, };	// version of local APIC 
int apic_found = 0;

void detect_lAPIC(void)
{				/* is a local APIC present?? */
	struct cpu_info out_id;
	apic_found = 0;
	cpuid(&out_id);
	if (out_id.family == CPU_FAMILY_386)
		printf("a 386 does not have an APIC, so please disable APIC support");
	else if (out_id.family == CPU_FAMILY_486)
		printf("a 486 does not have an APIC, so please disable APIC support");
	else if ((out_id.feature_flags & CPUF_LOCAL_APIC) == 0)
		printf("no APIC found!!");
	else
		apic_found = 1;
}

void enable_local_APIC(void)
{
	unsigned long value;

	// Clear the logical destination ID
	value = apic_read(APIC_LDR);
	value &= 0x00ffffff;	// Dest = 0
	apic_write(APIC_LDR, value);

	/* Set Task Priority to 'accept all' */
	value = apic_read(APIC_TASKPRI);
	value &= 0xffffff00;
	apic_write(APIC_TASKPRI, value);

	// bring the APIC into flat delivery mode.
	value = apic_read(APIC_DFR);
	value |= 0xf0000000;	// bits 28-31 = 1111 -> flat mode
	apic_write(APIC_DFR, value);

	/* now enable APIC */
	value = apic_read(APIC_SPIV);
	value |= (1 << 8);	/* Enable APIC (bit==1) */
//      value &= ~(1<<9);               /* Enable focus processor (bit==0) */
	value |= (1 << 9);	/* Disable focus processor (bit==1) */
	value |= SPURIOUS_APIC_VECTOR;	/* Set spurious IRQ vector to 0xff */
	apic_write(APIC_SPIV, value);

	/* setup LVTERR on integrated APICs */
	if ((lapic_version[get_processor_id()] & 0xF0) != APIC_VER_82489DX) {	/* !82489DX */
		unsigned int value, maxlvt;
		maxlvt = GET_APIC_MAXLVT(apic_read(APIC_VERSION));

		if (maxlvt > 3)
			apic_write(APIC_ESR, 0);
		value = apic_read(APIC_ESR);
		/*smp_debug_printf(SMP_DEBUG_ALL, "ESR value before enabling vector: %08lx\n", value); */

		value = ERROR_APIC_VECTOR;
		apic_write(APIC_LVERR, value);

		/* clear errors after enabling vector */
		if (maxlvt > 3)
			apic_write(APIC_ESR, 0);
		value = apic_read(APIC_ESR);
		/*smp_debug_printf(SMP_DEBUG_ALL, "ESR value after enabling vector: %08lx\n", value); */
	} else
		/*smp_debug_printf(SMP_DEBUG_ALL, "No ESR for 82489DX.\n"); */
		;
}

void ack_APIC_irq(void)
{
	/* Clear the IPI */

	/* Dummy read */
	apic_read(APIC_SPIV);

	/* Docs say use 0 for future compatibility */
	apic_write(APIC_EOI, 0);
}

#ifdef SMP
/*
inline int get_processor_id(void) {
     if (smp_found)
	  return  ((apic_read(APIC_ID)>>24)&0x0F);
     else       
	  return 0;
}
*/
inline void set_processor_id(int newID)
{
	apic_write(APIC_ID, (newID & 0x0F) << 24);
}

// set the destination for an IPI
void inline setIRQdest(int cpu_id)
{
	unsigned long tmp;

	tmp = apic_read(APIC_ICR2);
	tmp &= 0x00FFFFFF;	// clear Destination field
	tmp |= SET_APIC_DEST_FIELD(cpu_id);	// set the correct destination cpu
	apic_write(APIC_ICR2, tmp);	// Target chip
}

// modify the ICR (e.g. for IPIs)
void inline set_APIC_ICR(unsigned long data)
{
	unsigned long tmp;

	tmp = apic_read(APIC_ICR);
	tmp &= ~0xCDFFF;	// clear all bits
	tmp |= data;
	// Send the IPI. The write to APIC_ICR fires this off.
	apic_write(APIC_ICR, tmp);	// Send IPI 
}


/* send an IPI to the specified destination
   if  the destination is > 0xF it should be 
   APIC_DEST_SELF, APIC_DEST_ALLINC or APIC_DEST_ALLBUT */
void send_IPI(int dest, int vector)
{
	while (apic_read(APIC_ICR) & APIC_DEST_BUSY);	/* while APIC is busy */

	if (dest < 0x10) {	/*  dest specifies a CPU ID */
		setIRQdest(dest);
		set_APIC_ICR(APIC_DEST_DM_FIXED | APIC_DEST_DEST | vector);

	} else			/* dest is APIC_DEST_SELF, ..._ALLINC or ..._ALLBUT */
		set_APIC_ICR(APIC_DEST_DM_FIXED | dest | vector);

}

static void ipIRQfunc(unsigned int irq)
{
	sys_panic("this should never be called");
}
static struct irqfunctions ipIRQ = {
	"IPI",
	ipIRQfunc,		//ack
	ipIRQfunc,		//enable
	ipIRQfunc,		//disable
};

#endif

/************ setup the APIC internal clock  *************/
 /* time in 10 msec */
static unsigned int calibration_result;	/* amount of ticks for a msec */
#define APIC_DIVISOR 16		/* change also  APIC_TDR_DIV_16 in setup_APIC_LVTT */
#define HZ 100			/* timer is already setup to 100 Hz (see pic_init_default (irq.c)) */

static void ack_APIC_clock(unsigned int irq)
{
	ack_APIC_irq();
}

static void enable_APIC_clock(unsigned int irq)
{
	unsigned int lvtt_value;
	printf("enable_APIC_clock called\n");
	lvtt_value = apic_read(APIC_LVTT);
	lvtt_value &= ~APIC_LVT_MASKED;
	apic_write(APIC_LVTT, lvtt_value);
}
static void disable_APIC_clock(unsigned int irq)
{
	unsigned int lvtt_value;
	lvtt_value = apic_read(APIC_LVTT);
	lvtt_value |= APIC_LVT_MASKED;
	apic_write(APIC_LVTT, lvtt_value);
}

struct irqfunctions local_timer_IRQ = {
	"l Timer",
	ack_APIC_clock,		//ack
	enable_APIC_clock,	//enable
	disable_APIC_clock,	//disable
};

static void setup_APIC_LVTT(unsigned int clocks)
{
	unsigned int lvtt_value, tmp_value;

	lvtt_value = apic_read(APIC_LVTT);
	lvtt_value &= ~0xFF;	/* clear vector */
#ifdef JAVASCHEDULER
	lvtt_value |= LAPIC_TIMER_VECTOR;
#else
	lvtt_value |= APIC_LVT_TIMER_PERIODIC | LAPIC_TIMER_VECTOR;
#endif
	apic_write(APIC_LVTT, lvtt_value);

	/* Divide PICLK by 16 */
	tmp_value = apic_read(APIC_TDCR);
	apic_write(APIC_TDCR, (tmp_value & ~APIC_TDR_DIV_1)	/* clear all */
		   |APIC_TDR_DIV_16);	/* set "16" */

	apic_write(APIC_TMICT, clocks);
}

void calibrate_APIC_clock(void)
{

	long tt1, tt2;
	int i;

	const int LOOPS = HZ / 10;

	dprintf("calibrating APIC timer ...\n");

	setup_APIC_LVTT(0xffffffff);
	disable_APIC_clock(0 /*dummy */ );	/* disable timer IRQ */

	/* Let's wait for a wraparound to start exact measurement */
	wait_8254_wraparound();

	/* We wrapped around just now. Let's start */
	tt1 = apic_read(APIC_TMCCT);

	/* Let's wait LOOPS wraprounds */
	for (i = 0; i < LOOPS; i++)
		wait_8254_wraparound();

	tt2 = apic_read(APIC_TMCCT);

	calibration_result = ((tt1 - tt2)	/* clocks for LOOPS */
			      /LOOPS);	/* clocks per LOOP */

	/* init timer to std timeslice */
	set_APIC_clock(XXX);
#ifdef SMP
	wait_8254_wraparound();	//ttt
	smp_call_function(APIC_DEST_ALLBUT, setup_APIC_LVTT, (void *) (XXX * calibration_result), 1, NULL);
#endif
	dprintf("..... host bus clock speed is %ld.%04ld MHz.\n", (calibration_result * APIC_DIVISOR) / (1000000 / HZ),
		(calibration_result * APIC_DIVISOR) % (1000000 / HZ));
}

inline void set_APIC_ticks(unsigned int ticks)
{
	apic_write(APIC_TMICT, ticks);
}

inline unsigned int read_APIC_ticks(void)
{
	return apic_read(APIC_TMCCT);
}

void set_APIC_clock(unsigned int time)
{
	set_APIC_ticks(time * calibration_result);
}

unsigned int read_APIC_clock(void)
{
	/* always round up */
	return ((read_APIC_ticks() + (calibration_result - 1))
		/ calibration_result);
}





void call_function_ipi(void);	// in ipiint.S
void hwint00apic(void);		// in timer.S
void breakpoint_ex(void);	// in timer.S
void local_timer_int(void);	// in hwint.S
void spurious_apic(void);	// in exception.S
void error_apic(void);		// in exception.S
void APIC_error_interrupt(void)	// called from "error_apic"
{
	unsigned long v, v1;

	v = apic_read(APIC_ESR);
	apic_write(APIC_ESR, 0);
	v1 = apic_read(APIC_ESR);
	ack_APIC_irq();

	/* Here is what the APIC error bits mean:
	   0: Send CS error
	   1: Receive CS error
	   2: Send accept error
	   3: Receive accept error
	   4: Reserved
	   5: Send illegal vector
	   6: Received illegal vector
	   7: Illegal register address
	 */
	printf("APIC error: %02lx(%02lx): \"", v, v1);
	if (v1 & 0x01)
		printf("Send CS error");
	if (v1 & 0x02)
		printf("Receive CS error");
	if (v1 & 0x04)
		printf("Send accept error");
	if (v1 & 0x08)
		printf("Receive accept error");
	if (v1 & 0x10)
		printf("Reserved");
	if (v1 & 0x20)
		printf("Send illegal vector");
	if (v1 & 0x40)
		printf("Received illegal vector");
	if (v1 & 0x80)
		printf("Illegal register addres");
	printf("\" on CPU%d\n", get_processor_id());
	sys_panic("APIC error on CPU%d\n", get_processor_id());
}

void install_apicIRQs(void)
{				/* install all additional IRQs */

#ifdef SMP
	iInfos[CALL_FUNCTION_IRQNR].used = 1;
	iInfos[CALL_FUNCTION_IRQNR].vector_number = CALL_FUNCTION_VECTOR;
	iInfos[CALL_FUNCTION_IRQNR].functions = &ipIRQ;

/* install the IPI-Handlers*/
	int_gate(CALL_FUNCTION_VECTOR, &call_function_ipi, 0x80 /*present bit */  | 14 /*irq gate */ );
#endif

	iInfos[LAPIC_TIMER_IRQNR].used = 1;
	iInfos[LAPIC_TIMER_IRQNR].vector_number = LAPIC_TIMER_VECTOR;
	iInfos[LAPIC_TIMER_IRQNR].functions = &local_timer_IRQ;

#ifdef TIMESLICING_TIMER_IRQ
/* the system timer IRQ0 is set to hwint00apic ->"=disabled" */
	iInfos[0].used = 0;
	int_gate(iInfos[0].vector_number, hwint00apic, 0x80 /*present bit */  | 14 /*irq gate */ );
#endif
/* install the local APIC timer-Handlers*/
	int_gate(LAPIC_TIMER_VECTOR, local_timer_int, 0x80 /*present bit */  | 14 /*irq gate */ );
/* install the error and spurious Handlers*/
	int_gate(SPURIOUS_APIC_VECTOR, spurious_apic, 0x80 /*present bit */  | 14 /*irq gate */ );
	int_gate(ERROR_APIC_VECTOR, error_apic, 0x80 /*present bit */  | 14 /*irq gate */ );
}


#endif				/* APIC */
