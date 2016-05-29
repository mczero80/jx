#ifndef __smp_detect_h
#define __smp_detect_h

#include "smp.h"

#ifndef NULL
#define NULL 0
#endif

#define LOCAL_APIC_DEFAULT_PHYS_BASE 0xfee00000
#define IO_APIC_DEFAULT_PHYS_BASE    0xfec00000

//Processors
extern unsigned int num_processors_present;	// # of processors present 
extern unsigned char BSP_id;	// ID of BootStrapProcessor
extern unsigned char present_cpu_ID[MAX_NR_CPUS];	// the CPUs local APIC ID of all CPUs
extern int compatibility_mode;	// see defines COMPATIBILITY_.. 
#define  COMPATIBILITY_PIC      1	// = 1 -> IMCR present (PIC comp. mode)
#define  COMPATIBILITY_VIRTWIRE 0	// = 0 -> no IMCR (Virtual Wire comp. mode)
// local APICs
/* moved to lapic.h 
extern unsigned long lapic_addr;     // addr. of the local APIC
extern int lapic_version[MAX_NR_CPUS];  // version of local APIC  
#define APIC_VER_82489DX     0x00    // 0Xh = 82489DX  (see IA sw Dev Man V3 sec 7.5.15))
#define APIC_VER_INTEGRATED  0x10    // 1Xh = local (2Xh-ffH = reserved) 
*/

// Bus
extern int bus_id_to_type[MAX_MP_BUSSES];	// maps bus IDs to their type (see BUSTYPE_... defines)

// IO APICs
extern int num_io_apic_entries;	// # of I/O APIC entries
extern struct mpt_config_ioapic io_apics[MAX_IO_APICS];	// I/O APIC entries 
// IRQs
extern int num_mp_irq_entries;	// # of MP IRQ source entries
extern struct mpt_config_int mp_irqs[MAX_IRQ_SOURCES];	// MP IRQ source entries 


struct mp_floating_pointer_structure {
	char signature[4];	/* "_MP_"  (ID signature)                 */
	unsigned long physptr;	/* Configuration table address (0 = none) */
	unsigned char length;	/* length in paragraphs (16-Byte units)   */
	unsigned char spec_rev;	/* MP version (0x01 = 1.1; 0x04 = 1.4)    */
	unsigned char checksum;	/* Checksum (sum of all must be  0)       */
	unsigned char feature1;	/* 0 = MP conf tbl; (!=0) = default config */
	unsigned char feature2;	/* Bit7 set = IMCR|PIC else virt. wire    */
	unsigned char feature3;	/* reserved (0)                           */
	unsigned char feature4;	/* reserved (0)                           */
	unsigned char feature5;	/* reserved (0)                           */
};
#define MPFPS_ID_SIGNATURE (('_'<<24)|('P'<<16)|('M'<<8)|'_')
#define MPFPS_SIZE sizeof(struct mp_floating_pointer_structure)

struct mp_config_table_header {
	char signature[4];	/* always "PCMP" */
	unsigned short length;	/* Size of table */
	char spec_rev;		/* version 1=v1.1, 4=v1.4 */
	char checksum;		/* makes the sum zero */
	char oemid[8];		/* id of sys manufacturer */
	char productid[12];	/* product family */
	unsigned long oemptr;	/* ptr to OEM config tbl (0 if not present) */
	unsigned short oemsize;	/* size of OEM cfg tbl (in bytes) */
	unsigned short count;	/* # entries in base table */
	unsigned long lapic;	/* address of local APIC */
	unsigned short exttbllen;	/* extended table length (in bytes) */
	char exttblchksum;	/* checksum of the extended table */
	char reserved;		/* ... */
};
#define MPCT_SIGNATURE "PCMP"

/* Entry Types */
#define	MPT_PROCESSOR	0
#define	MPT_BUS		1
#define	MPT_IOAPIC	2
#define	MPT_IOINT	3
#define	MPT_LINT	4

struct mpt_config_processor {
	unsigned char type;	/* Entry Type (=0) */
	unsigned char lapicid;	/* Local APIC ID  number */
	unsigned char lapicver;	/* APIC versions */
	unsigned char cpuflag;	/* Bit 0(EN):en-/disable; Bit 1(BP):BSP/AP */
	unsigned long cpusignature;	/* Processor Type (e.g. Pentium) */
	unsigned long featureflags;	/* CPUID feature value */
	unsigned long reserved[2];
};
#define CPU_ENABLED		1	/* Processor is available */
#define CPU_BOOTPROCESSOR	2	/* Processor is the BSP */

#define CPU_STEPPING_MASK 0x0F
#define CPU_MODEL_MASK	0xF0
#define CPU_FAMILY_MASK	0xF00

struct mpt_config_bus {
	unsigned char type;	/* Entry Type (=1) */
	unsigned char busid;	/* assigned by BIOS */
	unsigned char bustype[6];	/* ID String */
};

// the BUSTYPE defines have to match
//    the array index of the according 
//    bus_type strings[] (in smp_detect.c)
#define BUSTYPE_EISA	0
#define BUSTYPE_ISA	1
#define BUSTYPE_INTERN	2
#define BUSTYPE_MCA	3
#define BUSTYPE_VL	4
#define BUSTYPE_PCI	5
#define BUSTYPE_PCMCIA	6
#define BUSTYPE_CBUS	7
#define BUSTYPE_CBUSII	8
#define BUSTYPE_FUTURE	9
#define BUSTYPE_MBI	10
#define BUSTYPE_MBII	11
#define BUSTYPE_MPSA	12
#define BUSTYPE_NUBUS	13
#define BUSTYPE_TC	14
#define BUSTYPE_VME	16
#define BUSTYPE_XPRESS	17


struct mpt_config_ioapic {
	unsigned char type;	/* Entry Type (=2) */
	unsigned char apicid;	/* ID of this I/O APIC */
	unsigned char apicver;	/* version of this I/O APIC */
	unsigned char flags;	/* BIT 0(EN):en-/disable */
	unsigned long apicaddr;	/* base addr of this I/O APIC */
};
#define IOAPIC_ENABLED		0x01

struct mpt_config_int {
	unsigned char type;	/* Entry Type (=3(I/O) or =4(local)) */
	unsigned char irqtype;	/* see INTTYPE_??? defines */
	unsigned short irqflag;	/* Bit 0&1(PO): Polarity (see IRQPOL defines)
				   Bit 2&3(EL): Trigger mode (see defines) */
	unsigned char srcbus;	/* source bus ID */
	unsigned char srcbusirq;	/* source bus IRQ signal number */
	unsigned char dstapic;	/* ID of connected I/O APIC (0xff=all) */
	unsigned char dstirq;	/* INTINn pin to which the signal is connected */
};

#define INTTYPE_INT      	0
#define INTTYPE_NMI		1
#define INTTYPE_SMI		2
#define INTTYPE_EXTINT		3

#define IRQPOL_DEFAULT  	0
#define IRQPOL_HIGH		1
#define IRQPOL_RESERVED		2
#define IRQPOL_LOW		3

#define IRQTRIGER_DEFAULT  	0
#define IRQTRIGER_EDGE		1
#define IRQTRIGER_RESERVED	2
#define IRQTRIGER_LEVEL		3

#define APIC_ALL	0xFF


// functions

// Scan for Intel's MP floating pointer stucture
int smp_detect(void);		// returns 1 if MP else 0
//void smp_debug_printf(int dbg_level, const char *fmt, ...);
//void print_screen(const char * str);
void smp_init(void);

#define SMP_DEBUG_ALL   2
#define SMP_DEBUG_STATE 1
#define SMP_DEBUG_NO    0

#define SMP_DEBUG_LEVEL 2

#if(SMP_DEBUG_LEVEL == SMP_DEBUG_NO)
#define smp_debug_printf(args...)
#else
#define smp_debug_printf(dbg_level,args...)   do{if (dbg_level <= SMP_DEBUG_LEVEL) {printf (args);}}while(0)
#endif


#endif
