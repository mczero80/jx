#ifdef KERNEL
#ifdef SMP

// search for "mf?" and "ttt" and fix it

#include "all.h"
#include "smp_detect.h"
#include "lapic.h"
#include "io_apic.h"
#include "misc.h"		// outb
#include "minic.h"
#include "timer8254.h"
#include "lowlevel.h"
#include "irq.h"
#include "lapic.h"

/********************/
static void smp_delay(int time)
{				/*delays time * 10 msec */
	int i;
	for (i = 0; i < time; i++)
		wait_8254_wraparound();
	return;
}
static inline void sti(void)
{
	asm volatile ("sti");
}

unsigned int num_processors_online = 0;	// # of processors online
unsigned char online_cpu_ID[MAX_NR_CPUS];	// the local APIC ID (only) of the used CPUs

static volatile unsigned long cpu_callout[MAX_NR_CPUS];	// set by BSP to activate an APs
static volatile unsigned long cpu_callin[MAX_NR_CPUS];	// is set by active APs so the BSP knows it's running


/*********************************************************************/
// copy entry point for the APs in lower mem
extern unsigned char smp_startup_begin[];
extern unsigned char smp_startup_end[];

static unsigned long setup_smp_startup_entry(void)
{
	// there is no low memory management yet
	// therefor almost all memory below 1M is free. 
	// so copy it anywhere in the low memory
	unsigned char *smp_startup_base = (unsigned char *) 0x1000;
	memcpy(smp_startup_base, smp_startup_begin, smp_startup_end - smp_startup_begin);
	return (unsigned long) smp_startup_base;
}

/* activates a CPU
return 1 on success otherwise 0 */
static int do_boot_cpu(int id)
{
	int result = 0;
	unsigned long send_status, accept_status;
	int timeout, j;		// in STARTUP loop
	unsigned long start_eip;

	start_eip = setup_smp_startup_entry();
	smp_debug_printf(SMP_DEBUG_STATE, "Booting processor #%d at EIP 0x%lx\n", id, start_eip);

	// run the startup process for the targeted processor.
	smp_debug_printf(SMP_DEBUG_ALL, "Setting warm reset code and vector.\n");
	/* set BIOS shutdown code to warm start (*CMOS0xf=0xa) */
	outb(0xf, 0x70);	// the CMOS is controlled by the RTC (port 70)
	outb(0xa, 0x71);
	/* set reset vector */
	*((volatile unsigned short *) 0x469) = start_eip >> 4;
	*((volatile unsigned short *) 0x467) = start_eip & 0xf;


	//Be paranoid about clearing APIC errors.
	if ((lapic_version[id] & 0xF0) != APIC_VER_82489DX) {
		apic_write(APIC_ESR, 0);
		accept_status = (apic_read(APIC_ESR) & 0xEF);
	}
//---------- let's go -------   
	// Status is now clean
	send_status = 0;
	accept_status = 0;
	cpu_callout[id] = 0;

	// Starting actual IPI sequence...
	smp_debug_printf(SMP_DEBUG_ALL, "Asserting INIT.\n");

	// Turn INIT on
	setIRQdest(id);
	set_APIC_ICR(APIC_DEST_LEVELTRIG | APIC_DEST_ASSERT | APIC_DEST_DM_INIT);

	smp_delay(2);
	smp_debug_printf(SMP_DEBUG_ALL, "Deasserting INIT.\n");

	setIRQdest(id);
	set_APIC_ICR(APIC_DEST_LEVELTRIG | APIC_DEST_DM_INIT);

	// Should we send STARTUP IPIs ?
	// if we have a 82489DX, wo don't need STARTUP IPIs
	if ((lapic_version[id] & 0xF0) != APIC_VER_82489DX) {
		// Run STARTUP IPI loop.
		for (j = 0; (j < 2); j++) {
			smp_debug_printf(SMP_DEBUG_ALL, "Sending STARTUP #%d.\n", j + 1);
			apic_write(APIC_ESR, 0);
			smp_debug_printf(SMP_DEBUG_ALL, "APIC Error Status Reg. cleared.\n");

			// STARTUP IPI
			setIRQdest(id);
			set_APIC_ICR(APIC_DEST_DM_STARTUP | (start_eip >> 12));

			timeout = 0;
			smp_debug_printf(SMP_DEBUG_ALL, "Waiting for send to finish...");
			do {
				smp_debug_printf(SMP_DEBUG_ALL, "+");
				smp_delay(1);
				timeout++;
				send_status = apic_read(APIC_ICR) & 0x1000;
			} while (send_status != 0 && (timeout < 10));

			if (timeout == 10)
				smp_debug_printf(SMP_DEBUG_ALL, "timed out\n");
			else
				smp_debug_printf(SMP_DEBUG_ALL, "send OK\n");

			// Give the other CPU some time to accept the IPI.
			smp_delay(1);
			accept_status = (apic_read(APIC_ESR) & 0xEF);

			if (send_status == 0 && accept_status == 0)
				break;	// everything went OK
		}		// for
		smp_debug_printf(SMP_DEBUG_ALL, "After Startup.\n");

		if (send_status != 0)	// send error
			smp_debug_printf(SMP_DEBUG_STATE, "APIC never delivered???\n");
		if (accept_status != 0)	// accept error
			smp_debug_printf(SMP_DEBUG_STATE, "APIC delivery error (%lx).\n", accept_status);
	} else
		smp_debug_printf(SMP_DEBUG_ALL, "No STARTUP IPI required\n");

	if ((send_status == 0) && (accept_status == 0)) {
		smp_debug_printf(SMP_DEBUG_ALL, "STARTUP IPI OK with CPU #%d!\n", id);
		// allow APs to start initializing.
		smp_debug_printf(SMP_DEBUG_ALL, "Before Callout CPU #%d.\n", id);
		cpu_callin[id] = 0;
		cpu_callout[id] = 1;
		smp_debug_printf(SMP_DEBUG_ALL, "After Callout CPU #%d.\n", id);

		for (timeout = 0; timeout < 500; timeout++) {
			if (cpu_callin[id])
				break;	// cpu has booted 
			smp_delay(1);
		}
		if (cpu_callin[id]) {
			smp_debug_printf(SMP_DEBUG_ALL, "OK.\n");
			result = 1;
			smp_debug_printf(SMP_DEBUG_STATE, "CPU #%d has booted.\n", id);
		} else		// Processor not responding
			smp_debug_printf(SMP_DEBUG_STATE, "CPU #%d Not responding.\n", id);
	} else			// STARTUP IPI failed
		smp_debug_printf(SMP_DEBUG_ALL, "STARTUP IPI failed with CPU #%d!\n", id);
	return result;
}



// Cycle through the processors sending APIC IPIs to boot each.
static void smp_boot_cpus(void)
{
	int i;

	enable_local_APIC();

	//      Now scan the CPU present map and fire up the other CPUs.
	for (i = 0; i < num_processors_present; i++) {
		// Don't start the boot CPU!
		if (present_cpu_ID[i] == BSP_id ||	// BSP
		    do_boot_cpu(present_cpu_ID[i]))	// or AP is online
		{
			num_processors_online++;
			online_cpu_ID[num_processors_online - 1] = present_cpu_ID[i];
		} else
			smp_debug_printf(SMP_DEBUG_STATE, "CPU #%d not responding.\n", present_cpu_ID[i]);
	}

	// Cleanup

	//Paranoid:  Set warm reset code and vector here back to default values.
	outb(0xf, 0x70);
	outb(0x0, 0x71);
	*((volatile long *) 0x467) = 0;

	ack_APIC_irq();
}

static volatile int smp_commenced = 0;

/* Called by boot processor to activate the rest. */
void smp_init(void)
{
	/* Get other processors into their bootup holding patterns. */
	smp_boot_cpus();
	setup_IO_APIC();
	install_apicIRQs();
	calibrate_APIC_clock();	/* APICs and IRQs must already be setup */
	smp_idle_threads_init();
	smp_commenced = 1;
}



/****************** functions for the APs ****************************/

static void stop_this_cpu(void)
{
	int i;
     /** update the CPU online infos */
	for (i = 0; i < num_processors_online; i++)
		if (online_cpu_ID[i] == get_processor_id()) {
			online_cpu_ID[i] = online_cpu_ID[num_processors_online - 1];
			num_processors_online--;
			break;
		}

	printf("CPU%d stopped!\n", get_processor_id());
	for (;;)
		asm("hlt");
}

static void smp_callin(void)
{
	int cpuid;
	unsigned long timeout = 0;

	// who am I?
	cpuid = get_processor_id();

	smp_debug_printf(SMP_DEBUG_STATE, "CPU #%d is waiting for CALLOUT\n", cpuid);

	/*
	 * STARTUP IPIs are fragile beasts as they might sometimes
	 * trigger some glue motherboard logic. Complete APIC bus
	 * silence for 1 second, this overestimates the time the
	 * boot CPU is spending to send the up to 2 STARTUP IPIs
	 * by a factor of two. This should be enough.
	 */
	while (timeout++ < 10000000) {
		// Has the boot CPU finished it's STARTUP sequence?
		if (cpu_callout[cpuid])
			break;
		smp_delay(1);
	}

	if (timeout++ > 10000000) {
		smp_debug_printf(SMP_DEBUG_STATE, "BUG: CPU%d started up but did not get a callout!\n", cpuid);
		stop_this_cpu();	// will never return!
	}

	smp_debug_printf(SMP_DEBUG_STATE, "CPU #%d called out!\n", cpuid);

	/*
	 * the boot CPU has finished the init stage and is spinning
	 * on cpu_callin until we finish. We are free to set up this
	 * CPU, first the APIC. (this is probably redundant on most
	 * boards)
	 */

	enable_local_APIC();

	idt_load();
	sti();

	//      Allow the master to continue.
	smp_debug_printf(SMP_DEBUG_ALL, "CPU #%d sets callin bit.\n", cpuid);
	cpu_callin[cpuid] = 1;
}


//Activate a secondary processor.
int start_secondary(void *unused)
{
	smp_callin();

	/* wait for BSP to setup the idle Threads */
	while (!smp_commenced);
//        set_current(createThread(domainZero, dummy_entry_point, NULL));
//        thread_exit();
#ifdef DEBUG
	check_current = 0;
#endif
	destroy_switch_to(curthrP(), idle_thread);	/* set_current */
	sys_panic("schould never reached\n");
	stop_this_cpu();
	return 0;		// never reached
}

#endif
#endif
