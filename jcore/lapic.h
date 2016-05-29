#ifndef __lAPIC_H
#define __lAPIC_H

#include "config.h"
#ifdef APIC

#include "smp.h"

#define LOCAL_APIC_DEFAULT_PHYS_BASE 0xfee00000
extern unsigned long lapic_addr;	// addr. of the local APIC
extern int lapic_version[MAX_NR_CPUS];	// version of local APIC  
#define APIC_VER_82489DX     0x00	// 0Xh = 82489DX  (see IA sw Dev Man V3 sec 7.5.15))
#define APIC_VER_INTEGRATED  0x10	// 1Xh = local (2Xh-ffH = reserved)
extern int apic_found;		/* Have we found an APIC */

// the local APICs registers
#define		APIC_ID		0x20	// local APIC ID Register        (r/w)
#define		APIC_VERSION	0x30	// local APIC Version Register   (r)
#define		APIC_TASKPRI	0x80	// Task Priority Register        (r/w)
#define		APIC_ARBPRI	0x90	// Arbitration Priority Register (r)
#define		APIC_PROCPRI	0xA0	// Proccessor Priority Register  (r)
#define		APIC_EOI	0xB0	// EOI Register                  (w)
#define		APIC_LDR	0xD0	// Local Destination Register    (r/w)
#define		APIC_DFR	0xE0	// Destination Format Register   [0-27(r)][28-31(r/w)]
#define		APIC_SPIV	0xF0	// Spurious-Interrupt Vector Reg.[0-3(r)][4-9(r/w)]
#define		APIC_ISR	0x100	// ISR 0-255                     (r)
#define		APIC_TMR	0x180	// TMR 0-255                     (r)
#define 	APIC_IRR	0x200	// IRR 0-255                     (r)
#define 	APIC_ESR	0x280	// Error Status Register         (r)
#define		APIC_ICR	0x300	// Interrupt Command Reg. 0-31   (r/w )
#define		APIC_ICR2	0x310	// Interrupt Command Reg. 32-63  (r/w)
#define		APIC_LVTT	0x320	// Local Vector Table (Timer)
#define		APIC_PCLVT      0x340	// Performance Counter LVT       (r/w)  {P6 fam.}
#define		APIC_LVT0	0x350	// Local Vector Table (LINT0)    (r/w)
#define 	APIC_LVT1	0x360	// Local Vector Table (LINT1)    (r/w)
#define		APIC_LVERR	0x370	// Local Vector Table (Error)    (r/w)  {P5 fam.}
#define		APIC_TMICT	0x380	// Initial Count Reg. for Timer  (r/w)
#define		APIC_TMCCT	0x390	// Current Count Reg. for Timer  (r)
#define		APIC_TDCR	0x3E0	// Timer Divide Config. Reg.     (r/w)


// for ICR
#define	APIC_DEST_DEST		0x00000	// Destination Shorthand
#define	APIC_DEST_SELF		0x40000
#define	APIC_DEST_ALLINC	0x80000
#define	APIC_DEST_ALLBUT	0xC0000
//#define       APIC_DEST_RR_MASK       0x30000  // reserved
//#define       APIC_DEST_RR_INVALID    0x00000
//#define       APIC_DEST_RR_INPROG     0x10000
//#define       APIC_DEST_RR_VALID      0x20000
#define	APIC_DEST_LEVELTRIG	0x08000	// Trigger Mode
#define	APIC_DEST_ASSERT	0x04000	// Level
#define	APIC_DEST_BUSY		0x01000	// Delivery Status
#define	APIC_DEST_LOGICAL	0x00800	// Destination Mode
#define	APIC_DEST_DM_FIXED	0x00000	// Delivery Mode
#define	APIC_DEST_DM_LOWEST	0x00100
#define APIC_DEST_DM_SMI	0x00200
#define	APIC_DEST_DM_REMRD	0x00300
#define	APIC_DEST_DM_NMI	0x00400
#define	APIC_DEST_DM_INIT	0x00500
#define	APIC_DEST_DM_STARTUP	0x00600

#define	APIC_DEST_VECTOR_MASK	0x000FF
// for ICR2
#define	GET_APIC_DEST_FIELD(x)	(((x)>>24)&0xFF)
#define	SET_APIC_DEST_FIELD(x)	((x)<<24)
// for TDCR
#define			APIC_TDR_DIV_1		0xB
#define			APIC_TDR_DIV_2		0x0
#define			APIC_TDR_DIV_4		0x1
#define			APIC_TDR_DIV_8		0x2
#define			APIC_TDR_DIV_16		0x3
#define			APIC_TDR_DIV_32		0x8
#define			APIC_TDR_DIV_64		0x9
#define			APIC_TDR_DIV_128	0xA
// for LVT (LVTT PCLVT LVT0 LVT1 LVERR)
#define                 GET_APIC_MAXLVT(x)      (((x)>>16)&0xFF)

#define			APIC_LVT_TIMER_PERIODIC		(1<<17)
#define			APIC_LVT_MASKED			(1<<16)
#define			APIC_LVT_LEVEL_TRIGGER		(1<<15)
#define			APIC_LVT_REMOTE_IRR		(1<<14)
#define			APIC_INPUT_POLARITY		(1<<13)
#define			APIC_SEND_PENDING		(1<<12)

#define APIC_BASE lapic_addr	/* if modified see lowlevel.S */


static inline void apic_write(unsigned long reg, unsigned long v)
{
	*((volatile unsigned long *) (APIC_BASE + reg)) = v;
}

static inline unsigned long apic_read(unsigned long reg)
{
	return *((volatile unsigned long *) (APIC_BASE + reg));
}

inline void apic_write(unsigned long reg, unsigned long v);
inline unsigned long apic_read(unsigned long reg);

void enable_local_APIC(void);
void ack_APIC_irq(void);

/* local timer*/
void calibrate_APIC_clock(void);
void set_APIC_clock(unsigned int time);	/* sets the time (in 10msec) for the current CPU timer */
unsigned int read_APIC_clock(void);	/* returns the time (in 10msec) of the current CPU timer */
void set_APIC_ticks(unsigned int ticks);	/* sets the ticks for the current CPU timer */
unsigned int read_APIC_ticks(void);	/* returns the ticks of the current CPU timer */
extern struct irqfunctions local_timer_IRQ;

void install_apicIRQs(void);

#ifdef SMP
inline void setIRQdest(int cpu_id);
inline void set_APIC_ICR(unsigned long data);

inline int get_processor_id(void);
inline void set_processor_id(int newID);

void send_IPI(int dest, int vector);
#else				/* no SMP */
#define get_processor_id()  0
#endif				/* SMP */
#else				/* no APIC */
#define get_processor_id()  0
#endif				/* APIC */
#endif
