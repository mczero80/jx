#ifdef KERNEL
#ifdef SMP

#include "config.h"
#include "smp_detect.h"
#include "lapic.h"
#include "minic.h"
#include "misc.h"
#include "irq.h"
#include "spinlock.h"
#include "smp.h"

// Structure and data for smp_call_function().
static volatile struct call_data_struct {
	int (*func) (void *info);
	void *info;
	volatile int started;
	volatile int finished;
	int wait;
	int result;
} *call_data = NULL;

#if 0
int smp_call_function(int dest, int (*func) (void *info), void *info, int wait, int *result)
{
	while (_smp_call_function(dest, func, info, wait, result) == 1);
}

// this function sends a 'generic call function' IPI to all other CPUs in the system.
int _smp_call_function(int dest, int (*func) (void *info), void *info, int wait, int *result)
/*
 * [SUMMARY] Run a function on other CPU(s).
 * <dest> The destination CPU ID (or APIC_DEST_SELF, ..._ALLINC or ..._ALLBUT  )
 * <func> The function to run. This must be fast and non-blocking.
 * <info> An arbitrary pointer to pass to the function.
 * <wait> If true, wait (atomically) until function has completed on other CPU(s).
 * <result> (if wait == true) pointer to an int for the result of the function (may be NULL)
 * [RETURNS] 1 if another CPU has the "lock";  0 on success
 * on success: Does not return until remote CPUs are nearly 
 * ready to execute <<func>>.
 *
 * You should not call this function with disabled interrupts or from a
 * hardware interrupt handler.
 * But if you do, check the result and do NOT loop until success
 */
{
	struct call_data_struct data;
	int cpus;

	printf("CPU%d: smp_call_function called\n", get_processor_id());

	if (dest == APIC_DEST_ALLINC)
		cpus = num_processors_online;
	else if (dest == APIC_DEST_ALLBUT)
		cpus = num_processors_online - 1;
	else			/*if (dest < 0x10 || dest == APIC_DEST_SELF) */
		cpus = 1;

	if (cpus == 0)
		return 0;

	data.func = func;
	data.info = info;
	data.started = 0;
	data.finished = 0;
	data.wait = wait;
	data.result = 0;

	/* set call_data to &data: (with CAS)
	   if (call_data == NULL); call_data = &data; */
	if (!cas(&call_data, NULL, &data))
		return 1;

	/* Send a message to all other CPUs and wait for them to respond */
	send_IPI(dest, CALL_FUNCTION_VECTOR);

	/* Wait for response */
	while (data.started != cpus);	//todo: timeout

	if (wait) {
		while (data.finished != cpus);
		if (result != NULL)
			*result = data.result;
	}
	return 0;
}

void call_function_ipi_handler(void)
{
	struct call_data_struct data, *dataPtr;

	ASSERT(call_data != NULL);

	dataPrt = call_data;
	data = *call_data;

	call_data = NULL;	//ttt: error works only with one other CPU

	ack_APIC_irq();

	/* Notify initiating CPU that I've grabbed the data and am about to execute the function */
	atomic_inc(&dataPtr->started);

	/* At this point dataPtr may be invalid unless wait==1 */
	data.result = (*data.func) (data.info);

	if (data->wait) {	/* dataPtr is still valid */
		dataPtr->result = data.result;
		atomic_inc(&dataPtr->finished);
	}
}
#else				/* test */
// this function sends a 'generic call function' IPI to all other CPUs in the system.
int smp_call_function(int dest, int (*func) (void *info), void *info, int wait, int *result)
/*
 * [SUMMARY] Run a function on other CPU(s).
 * <dest> The destination CPU ID (or APIC_DEST_SELF, ..._ALLINC or ..._ALLBUT  )
 * <func> The function to run. This must be fast and non-blocking.
 * <info> An arbitrary pointer to pass to the function.
 * <wait> If true, wait  until function has completed on other CPU(s).
 * <result> (if wait == true) pointer to an int for the result of the function (may be NULL)
 * [RETURNS] 0 on success(always), else a negative status code. Does not return until
 * remote CPUs are nearly ready to execute <<func>> or are or have executed.
 *
 * You must not call this function with disabled interrupts or from a
 * hardware interrupt handler.
 */
{
	struct call_data_struct data;
	int cpus;
	static spinlock_t lock = SPIN_LOCK_UNLOCKED;
	printf("CPU%d: smp_call_function called\n", get_processor_id());

	if (dest == APIC_DEST_ALLINC)
		cpus = num_processors_online;
	else if (dest == APIC_DEST_ALLBUT)
		cpus = num_processors_online - 1;
	else			/*if (dest < 0x10 || dest == APIC_DEST_SELF) */
		cpus = 1;
	if (cpus == 0)
		return 0;

	data.func = func;
	data.info = info;
	data.started = 0;
	data.finished = 0;
	data.wait = wait;
	data.result = 0;

	spin_lock(&lock);
	call_data = &data;
	/* Send a message to all other CPUs and wait for them to respond */
	send_IPI(dest, CALL_FUNCTION_VECTOR);

	/* Wait for response */
	while (data.started != cpus);	//todo: timeout

	if (wait) {
		while (data.finished != cpus);
		if (result != NULL)
			*result = data.result;
	}
	spin_unlock(&lock);
	return 0;
}

void call_function_ipi_handler(void)
{
	int (*func) (void *info) = call_data->func;
	void *info = call_data->info;
	int wait = call_data->wait;
	int result;

	unsigned int clock = read_APIC_ticks();	// save scheduler clock
	set_APIC_ticks(0);	// stop scheduler

	ack_APIC_irq();

	// Notify initiating CPU that I've grabbed the data and am about to execute the function
	atomic_inc(&call_data->started);

	// At this point the info structure may be out of scope unless wait==1
	result = (*func) (info);
	if (wait) {
		call_data->result = result;
		atomic_inc(&call_data->finished);
	}
	if (read_APIC_ticks() == 0)	// not changed??
		set_APIC_ticks(clock);	// restart scheduler
}
#endif				/* test */

#endif
#endif
