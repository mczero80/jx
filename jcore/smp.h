#ifndef __smp_h
#define __smp_h

#ifdef SMP
#define MAX_NR_CPUS       15
#else
#define MAX_NR_CPUS        1
#endif

#ifdef SMP
#define MAX_IO_APICS       4
#define MAX_IRQ_SOURCES  128
#define MAX_MP_BUSSES     32

#ifndef ASSEMBLER
extern unsigned int num_processors_online;	// # of processors online
extern unsigned char online_cpu_ID[MAX_NR_CPUS];	// the local APIC ID (only) of the used CPUs
extern int smp_found;		/* Have we found an SMP System */

// search for MP Config Table
int smp_detect(void);
// init all Processors
void smp_init(void);

// this function sends a 'generic call function' IPI to all/one other CPUs in the system.
int smp_call_function(int dest, int (*func) (void *info), void *info,
		      int wait, int *result);
#endif

#else				/* noSMP */
#define num_processors_online (1)
#endif				/* SMP */

#endif
