/********************************************************************************
 * Interrupt controller
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#include "all.h"

#ifdef SMP
#include "io_apic.h"
#endif

#include "gc_common.h"
#include "gc_impl.h"

void int_dispatch(int irq, struct irqcontext ctx);

/* Interrupt vectors defined/reserved by processor. */
#define DIVIDE_VECTOR      0	/* divide error */
#define DEBUG_VECTOR       1	/* single step (trace) */
#define NMI_VECTOR         2	/* non-maskable interrupt */
#define BREAKPOINT_VECTOR  3	/* software breakpoint */
#define OVERFLOW_VECTOR    4	/* from INTO */

/* 286 Exception vector numbers. */
#define BOUNDS_VECTOR       5	/* bounds check failed */
#define INVAL_OP_VECTOR     6	/* invalid opcode */
#define COPROC_NOT_VECTOR   7	/* coprocessor not available */
#define DOUBLE_FAULT_VECTOR 8
#define COPROC_SEG_VECTOR   9	/* coprocessor segment overrun */
#define INVAL_TSS_VECTOR   10	/* invalid TSS */
#define SEG_NOT_VECTOR     11	/* segment not present */
#define STACK_FAULT_VECTOR 12	/* stack exception */
#define PROTECTION_VECTOR  13	/* general protection */
/* extra 386 Exception vector numbers. */
#define PAGE_FAULT_VECTOR   14
#define COPROC_ERR_VECTOR   16	/* coprocessor error */

#define IRQ0_VECTOR     40
#define IRQ8_VECTOR     48

void divide_error(void);
void debug_exception();
void nmi();
void breakpoint_exception();
void overflow();
void bounds_check();
void inval_opcode();
void copr_not_available();
void double_fault();
void copr_seg_overrun();
void inval_tss();
void segment_not_present();
void stack_exception();
void general_protection();
void page_fault();
void copr_error();

void notDefined(void);

void hwint00(void);
void hwint01(void);
void hwint02(void);
void hwint03(void);
void hwint04(void);
void hwint05(void);
void hwint06(void);
void hwint07(void);
void hwint08(void);
void hwint09(void);
void hwint10(void);
void hwint11(void);
void hwint12(void);
void hwint13(void);
void hwint14(void);
void hwint15(void);

struct gate_table_s {
	void (*gate) (void);
	unsigned char vec_nr;
};

#ifdef APIC
#define NINT 0xff
#else
#define NINT 0x31
#endif
#define NINT_PREDEFINED 0x30
static void (*irqhandler[NINT]) () = {
	divide_error, debug_exception, nmi, breakpoint_exception, overflow, bounds_check, inval_opcode, copr_not_available, double_fault, copr_seg_overrun, inval_tss, segment_not_present, stack_exception, general_protection, page_fault, notDefined,	/* 0F */
	    copr_error,		/* 10 */
	    notDefined,		/* 11 */
	    notDefined,		/* 12 */
	    notDefined,		/* 13 */
	    notDefined,		/* 14 */
	    notDefined,		/* 15 */
	    notDefined,		/* 16 */
	    notDefined,		/* 17 */
	    notDefined,		/* 18 */
	    notDefined,		/* 19 */
	    notDefined,		/* 1A */
	    notDefined,		/* 1B */
	    notDefined,		/* 1C */
	    notDefined,		/* 1D */
	    notDefined,		/* 1E */
	    notDefined,		/* 1F */
	    hwint00,		/* 20 */
	    hwint01, hwint02, hwint03, hwint04, hwint05, hwint06, hwint07, hwint08, hwint09, hwint10, hwint11, hwint12, hwint13, hwint14, hwint15,	/* 2F */
};


#define OFFSET_HIGH_SHIFT   16	/* shift for (gate) offset --> offset_high */
#define KERNEL_CS	0x10	/* Kernel's PL0 code segment */


/*
 * Trap, interrupt, or call gate.
 */
struct gate_s {
	u4_t offset_low:16,	/* offset 0..15 */
	 selector:16, word_count:8, access:8, offset_high:16;	/* offset 16..31 */
};

struct descriptor_s {
	u4_t limit_low:16,	/* limit 0..15 */
	 base_low:16,		/* base  0..15 */
	 base_med:8,		/* base  16..23 */
	 access:8,		/* access byte */
	 limit_high:4,		/* limit 16..19 */
	 granularity:4,		/* granularity */
	 base_high:8;		/* base 24..31 */
};

/* Full-size 256-entry IDT.
   This is needed, for example, under VCPI or Intel MP Standard PCs.  */
#define IDT_SIZE	256
struct gate_s icore_idt[IDT_SIZE];
struct descriptor_s icore_gdt[3];

/* Build descriptor for an interrupt gate. */
void int_gate(unsigned vec_nr, u4_t base, unsigned dpl_type)
{
	register struct gate_s *idp;

	idp = &icore_idt[vec_nr];
	idp->offset_low = base & 0xffff;
	idp->selector = KERNEL_CS;
	idp->access = dpl_type;
	idp->offset_high = (base >> OFFSET_HIGH_SHIFT) & 0xffff;
}

#define	ACC_PL_K	0x00	/* kernel access only */
#define	ACC_P		0x80	/* segment present */

static void init_descriptor(struct descriptor_s *desc, u4_t base, u4_t limit, u1_t access, u1_t sizebits)
{
	if (limit > 0xfffff) {
		limit >>= 12;
		sizebits |= 0x8;	/* 4k limit field */
	}
	desc->limit_low = limit & 0xffff;
	desc->base_low = base & 0xffff;
	desc->base_med = (base >> 16) & 0xff;
	desc->access = access | ACC_P;
	desc->limit_high = limit >> 16;
	desc->granularity = sizebits;
	desc->base_high = base >> 24;
}



/* Fixed global descriptors.  1 to 7 are prescribed by the BIOS. */
#define GDT_INDEX        1	/* GDT descriptor */
#define IDT_INDEX        2	/* IDT descriptor */

#define GDT_SELECTOR      0x08	/* (GDT_INDEX * DESC_SIZE) bad for asld */
#define IDT_SELECTOR      0x10	/* (IDT_INDEX * DESC_SIZE) */


struct desctable_s {
	char limit[sizeof(u2_t)];
	char base[sizeof(u4_t)];	/* really u24_t + pad for 286 */
};

#define KCODESEL 0x10
#define DESCSIZE 8
#define IDTOFF_LO 0
#define IDT_SEL   2
#define IDT_ZERO  4
#define IDT_ACCES 5
#define IDTOFF_HI 6
/* segment descriptor access flags */
#define ACCE 0x01
#define REWR 0x02
#define COED 0x04
#define PRES 0x80
#define SYSR 0x00
#define USRR 0x60
#define DATS 0x10
#define CODS 0x18
#define IDTD 0x0E
#define TSSD 0x09

static u1_t IDTInfo[6];
static u4_t IDT;

void idt_load()
{
	static int initialized = 0;
	int i;
	IDT = (u4_t) icore_idt;
	/*    if (!initialized) { */
#ifdef APIC
	for (i = NINT_PREDEFINED; i < NINT; i++) {
		irqhandler[i] = notDefined;
	}
#endif
	for (i = 0; i < NINT; i++) {
		*(u2_t *) (IDT + i * DESCSIZE + IDTOFF_LO) = (u4_t) irqhandler[i] & 0xFFFF;
		*(u2_t *) (IDT + i * DESCSIZE + IDT_SEL) = 0x10;	/* kernel code segment */
		*(u1_t *) (IDT + i * DESCSIZE + IDT_ZERO) = 0;
		*(u1_t *) (IDT + i * DESCSIZE + IDT_ACCES) = PRES /* present */  | IDTD /* interrupt gate */ ;
		*(u2_t *) (IDT + i * DESCSIZE + IDTOFF_HI) = (u4_t) irqhandler[i] >> 16;
	}

	*((u2_t *) IDTInfo + 0) = NINT * DESCSIZE - 1;
	*((u2_t *) IDTInfo + 1) = (u4_t) IDT & 0xFFFF;
	*((u2_t *) IDTInfo + 2) = (u4_t) IDT >> 16;

	initialized = 1;
	/*} */
	asm("lidt IDTInfo");
}



#ifdef KERNEL
#include "minic.h"
#include "load.h"
#include "context.h"
#include "lowlevel.h"
#include "thread.h"


/*
 * IRQ 
 */
#define  MASTER_PORT  0x20
#define  SLAVE_PORT   0xa0
#define  MASTER_ICW  (MASTER_PORT + 0)
#define  MASTER_OCW  (MASTER_PORT + 1)
#define  SLAVES_ICW  (SLAVE_PORT + 0)
#define  SLAVES_OCW  (SLAVE_PORT + 1)


#define MASTER_VECTOR 0x20
#define SLAVE_VECTOR 0x28

/*
   ICW1
*/

#define ICW_TEMPLATE            0x10

#define LEVL_TRIGGER            0x08
#define EDGE_TRIGGER            0x00
#define ADDR_INTRVL4            0x04
#define ADDR_INTRVL8            0x00
#define SINGLE__MODE            0x02
#define CASCADE_MODE            0x00
#define ICW4__NEEDED            0x01
#define NO_ICW4_NEED            0x00

/*
**      ICW2 is the programmable interrupt vector base, not defined here.
*/

/*
**      ICW3
*/

#define SLAVE_ON_IR0            0x01
#define SLAVE_ON_IR1            0x02
#define SLAVE_ON_IR2            0x04
#define SLAVE_ON_IR3            0x08
#define SLAVE_ON_IR4            0x10
#define SLAVE_ON_IR5            0x20
#define SLAVE_ON_IR6            0x40
#define SLAVE_ON_IR7            0x80


#define I_AM_SLAVE_0            0x00
#define I_AM_SLAVE_1            0x01
#define I_AM_SLAVE_2            0x02
#define I_AM_SLAVE_3            0x03
#define I_AM_SLAVE_4            0x04
#define I_AM_SLAVE_5            0x05
#define I_AM_SLAVE_6            0x06
#define I_AM_SLAVE_7            0x07

/*
**      ICW4
*/

#define SNF_MODE_ENA            0x10
#define SNF_MODE_DIS            0x00
#define BUFFERD_MODE            0x08
#define NONBUFD_MODE            0x00
#define AUTO_EOI_MOD            0x02
#define NRML_EOI_MOD            0x00
#define I8086_EMM_MOD           0x01
#define SET_MCS_MODE            0x00

/*
**      OCW1
*/

#define PICM_MASK               0xFF
#define PICS_MASK               0xFF

/*
**      OCW2
*/

#define NON_SPEC_EOI            0x20
#define SPECIFIC_EOI            0x60
#define ROT_NON_SPEC            0xa0
#define SET_ROT_AEOI            0x80
#define RSET_ROTAEOI            0x00
#define ROT_SPEC_EOI            0xe0
#define SET_PRIORITY            0xc0
#define NO_OPERATION            0x40


#define SEND_EOI_IR0            0x00
#define SEND_EOI_IR1            0x01
#define SEND_EOI_IR2            0x02
#define SEND_EOI_IR3            0x03
#define SEND_EOI_IR4            0x04
#define SEND_EOI_IR5            0x05
#define SEND_EOI_IR6            0x06
#define SEND_EOI_IR7            0x07

/*
**      OCW3
*/

#define OCW_TEMPLATE            0x08
#define SPECIAL_MASK            0x40
#define MASK_MDE_SET            0x20
#define MASK_MDE_RST            0x00
#define POLL_COMMAND            0x04
#define NO_POLL_CMND            0x00
#define READ_NEXT_RD            0x02
#define READ_IR_ONRD            0x00
#define READ_IS_ONRD            0x01


#define PICM_ICW1       (ICW_TEMPLATE | EDGE_TRIGGER | ADDR_INTRVL8 \
                         | CASCADE_MODE | ICW4__NEEDED)
#define PICM_ICW3       (SLAVE_ON_IR2)
#define PICM_ICW4       (SNF_MODE_DIS | NONBUFD_MODE | NRML_EOI_MOD \
                         | I8086_EMM_MOD)

#define PICS_ICW1       (ICW_TEMPLATE | EDGE_TRIGGER | ADDR_INTRVL8 \
                         | CASCADE_MODE | ICW4__NEEDED)
#define PICS_ICW3       (I_AM_SLAVE_2)
#define PICS_ICW4       (SNF_MODE_DIS | NONBUFD_MODE | NRML_EOI_MOD \
                         | I8086_EMM_MOD)

static void enable8259IRQ(unsigned int irq)
{
	DISABLE_IRQ;

	if (irq < 8) {
		outb(MASTER_OCW, (inb(MASTER_OCW) & ~(1 << irq)));
	} else {
		outb(MASTER_OCW, (inb(MASTER_OCW) & ~(1 << 2)));
		irq -= 8;
		outb(SLAVES_OCW, (inb(SLAVES_OCW) & ~(1 << irq)));
	}

	RESTORE_IRQ;
}

static void disable8259IRQ(unsigned int irq)
{
	DISABLE_IRQ;

	if (irq < 8) {
		outb(MASTER_OCW, (inb(MASTER_OCW) | (1 << irq)));
	} else {
		/*outb(MASTER_OCW, (inb(MASTER_OCW) | (1 << 2))); */
		irq -= 8;
		outb(SLAVES_OCW, (inb(SLAVES_OCW) | (1 << irq)));
	}

	RESTORE_IRQ;
}

static void ack8259IRQ(unsigned int nr)
{
#define PIC1_CMD_STAT_REGISTER MASTER_PORT
#define PIC2_CMD_STAT_REGISTER SLAVE_PORT
#define ENABLEALL 0x20
#define ENABLEPIC2 0x62

	DISABLE_IRQ;

	if (nr < 8) {
		outb(PIC1_CMD_STAT_REGISTER, ENABLEALL);
	} else {
		outb(PIC1_CMD_STAT_REGISTER, ENABLEPIC2);
		outb(PIC2_CMD_STAT_REGISTER, ENABLEALL);
	}
	RESTORE_IRQ;
}

int getISR8259IRQ(unsigned int irq)
{
#define READ_ISR  0x0b
	if (irq < 8) {
		outb(MASTER_PORT, READ_ISR);
		return (inb(MASTER_PORT) >> irq) & 0x1;
	} else if (irq < 16) {
		irq -= 8;
		outb(SLAVE_PORT, READ_ISR);
		return (inb(SLAVE_PORT) >> irq) & 0x1;
	} else
		return 2;
}

int getIRR8259IRQ(unsigned int irq)
{
#define READ_IRR  0x0a
	if (irq < 8) {
		outb(MASTER_PORT, READ_IRR);
		return (inb(MASTER_PORT) >> irq) & 0x1;
	} else if (irq < 16) {
		irq -= 8;
		outb(SLAVE_PORT, READ_IRR);
		return (inb(SLAVE_PORT) >> irq) & 0x1;
	} else
		return 2;
}

int getIMR8259IRQ(unsigned int irq)
{
	if (irq < 8) {
		return (inb(MASTER_OCW) >> irq) & 0x1;
	} else if (irq < 16) {
		irq -= 8;
		return (inb(SLAVES_OCW) >> irq) & 0x1;
	} else
		return 2;
}

/* send EIO to the correct Interrupt Controller*/
void ackIRQ(unsigned int irq)
{
	iInfos[irq].functions->ack(irq);
}
void enableIRQ(unsigned int irq)
{
	iInfos[irq].functions->enable(irq);
	iInfos[irq].enabled = 1;
	ackIRQ(irq);
}
void disableIRQ(unsigned int irq)
{
	iInfos[irq].functions->disable(irq);
	iInfos[irq].enabled = 0;
}

struct irqfunctions legacyPIC = {
	"legacy",
	ack8259IRQ,
	enable8259IRQ,
	disable8259IRQ
};

int pic_init_pmode()
{
	int i;
	dprintf("init system\n");
	/*
	 * IRQ 
	 */
	outb(MASTER_PORT, 0x11);	// ICW1
	outb(SLAVE_PORT, 0x11);	// ICW1

	outb(MASTER_PORT + 1, MASTER_VECTOR);	// ICW2 PIC1 vector offset
	outb(SLAVE_PORT + 1, SLAVE_VECTOR);	// ICW2 PIC2 vector offset

	outb(MASTER_PORT + 1, 0x04);	// ICW3  bit 2=1: irq2 is cascaded irq from slave
	outb(SLAVE_PORT + 1, 0x02);	// ICW3  slave id = 2

	outb(MASTER_PORT + 1, 0x01);	// ICW4  bit 0=1: 80x86 mode bit 1=1: automatic EOI
	outb(SLAVE_PORT + 1, 0x01);	// ICW4  bit 0=1: 80x86 mode bit 1=1: automatic EOI

	/* disable all irqs */
	outb(MASTER_OCW, 0xff);
	outb(SLAVES_OCW, 0xff);

#if 0
	/* change interrupt priorities, to allow serial line
	   to interrupt
	   commented out because timer has low priority */
	outb(0x20, 0xc2);
#endif

	for (i = 0; i < NUM_IRQs; i++) {
		printf("IRQ %d ", i);
		if (iInfos[i].enabled == 1) {
			enableIRQ(i);
			printf("enabled\n");
		} else {
			printf("disabled\n");
		}
	}

	dprintf("finished irq init\n");

}


/* called by exception handler */
void breakpoint_ex(struct irqcontext_timer sc)
{
	printf("  BREAKPOINT eip=%p ", sc.eip);
	print_eip_info((char *) sc.eip);
	printf("\n");
	printStackTrace("BREAK", curthr(), (u4_t *) (sc.ebp));
	monitor(&sc);
}

/* called by exception handler */
void debug_ex(struct irqcontext_timer sc)
{
	unsigned dr6 = get_dr6();
#ifdef DEBUG
	check_current = 0;
#endif
	if ((dr6 & DR6_BS) != 0)
		printf("SINGLESTEP ");
/*    else if ((dr6 & DR6_B0)  !=0 ) 
	 printf("BREAKPOINT0 ");
    else if ((dr6 & DR6_B1)  !=0 ) 
	 printf("BREAKPOINT1 ");
*/
	else if ((dr6 & DR6_B2) != 0)
		printf("BREAKPOINT2 ");
	else if ((dr6 & DR6_B3) != 0)
		printf("BREAKPOINT3 ");
	if ((dr6 & (DR6_B0 | DR6_B1)) != 0) {
#if 0
		printf("NULL POINTER EXCEPTION  eip=%p\n", sc.eip);
		printf(" eip=%p ", sc.eip);
		print_eip_info((char *) sc.eip);
		printf("\n");
		printf("eax: %p, ebx: %p, ecx: %p, edx: %p, esi: %p, edi: %p\n", sc.eax, sc.ebx, sc.ecx, sc.edx, sc.esi, sc.edi);
		printf("esp: %p, ebp: %p, eip: %p, eflags: %p\n", sc.esp, sc.ebp, sc.eip, sc.eflags);
		printStackTrace("STACK", curthr(), (u4_t *) (sc.ebp));

		monitor(&sc);
#endif
		asm("movl %ebp, %esp;" "popl %ebp;" "addl $16,%esp;  /* popl return_addr,gs,fs,es */" "addl $32,%esp;  /* popa */"
		    "popl %ecx;    /* eip */" "popl %eax;    /* cs */" "popl %eax;    /* eflags*/" "pushl $-2; "
		    "pushl %ecx;    /* eip (new return_addr)*/" "jmp exceptionHandler;");
		sys_panic("never reached");
	} else {
		printf("  DEBUG");
		printf(" eip=%p ", sc.eip);
		print_eip_info((char *) sc.eip);
		printf("\n");
		printf("eax: %p, ebx: %p, ecx: %p, edx: %p, esi: %p, edi: %p\n", sc.eax, sc.ebx, sc.ecx, sc.edx, sc.esi, sc.edi);
		printf("esp: %p, ebp: %p, eip: %p, eflags: %p\n", sc.esp, sc.ebp, sc.eip, sc.eflags);
		printStackTrace("STACK", curthr(), (u4_t *) (sc.ebp));

		sc.eflags &= 0xfffffeff;	/* unset single step flag */
		monitor(&sc);
	}
	set_dr6(0xFFFF0FF0);
}

char *exception_msg(jint exc)
{
	char *msg;
	switch (exc) {
	case 0:
		msg = "Divide Error";
		break;
	case 1:
		msg = "Debug";
		break;
	case 2:
		msg = "Non-Maskable Interrupt";
		break;
	case 3:
		msg = "Software Breakpoint";
		break;
	case 4:
		msg = "Overflow";
		break;
	case 5:
		msg = "bounds check failed";
		break;
	case 6:
		msg = "Invalid Opcode";
		break;
	case 7:
		msg = "Coporcessor not available";
		break;
	case 8:
		msg = "Double Fault";
		break;
	case 9:
		msg = "Coprocessor segment overrun";
		break;
	case 10:
		msg = "Invalid TSS";
		break;
	case 11:
		msg = "Segment not present";
		break;
	case 12:
		msg = "Stack Exception";
		break;
	case 13:
		msg = "General Protection Fault";
		break;
	case 14:
		msg = "Page Fault";
		break;
	case 16:
		msg = "Coprocessor Error";
		break;
#ifdef APIC
	case SPURIOUS_APIC_VECTOR:
		msg = "spurious Interrupt from local APIC";
		break;
#endif
	default:
		msg = "Unknown exception code";
		break;
	}
	return msg;
}

void hw_exception(int exc, unsigned int cpu)
{
	int i;
	u4_t *sp;
#ifdef DEBUG
	check_current = 0;
#endif
#ifdef LOG_PRINTF
	printf2mem = 0;
#endif
	sp = (u4_t *) & exc;
	for (i = 0; i < 30; i++) {
		printf("STACK: %p: %p\n", sp, *sp);
		sp++;
	}
	printf("%p ", (char *) curthr()->context[PCB_EIP]);
	print_eip_info((char *) curthr()->context[PCB_EIP]);
	printf("\n");
	dprintf("Current Thread: \n");
	print_threadinfo(curthr());
	printStackTrace(" ", curthr(), (u4_t *) (curthr()->context[PCB_EBP]));
#ifdef SMP
	dprintf("CPU%d: HARDWARE EXCEPTION 0x%lx: %s\n", cpu, exc, exception_msg(exc));
#else
	dprintf("HARDWARE EXCEPTION 0x%lx: %s\n", exc, exception_msg(exc));
#endif
	monitor(NULL);
}

void hw_errexception(int exc, int errorcode, unsigned int cpu)
{
#ifdef SMP
	dprintf("HARDWARE ERROR EXCEPTION 0x%lx 0x%lx!!!!\n", exc, errorcode);
#else
	dprintf("CPU%d: HARDWARE ERROR EXCEPTION 0x%lx 0x%lx!!!!\n", cpu, exc, errorcode);
#endif
	hw_exception(exc, cpu);
}


void pic_init_rmode()
{
	unsigned char master_base = 0x08;
	unsigned char slave_base = 0x70;
	/* Initialize the master. */
	outb_p(MASTER_ICW, PICM_ICW1);
	outb_p(MASTER_OCW, master_base);
	outb_p(MASTER_OCW, PICM_ICW3);
	outb_p(MASTER_OCW, PICM_ICW4);

	/* Initialize the slave. */
	outb_p(SLAVES_ICW, PICS_ICW1);
	outb_p(SLAVES_OCW, slave_base);
	outb_p(SLAVES_OCW, PICS_ICW3);
	outb_p(SLAVES_OCW, PICS_ICW4);

	/* Ack any bogus intrs by setting the End Of Interrupt bit. */
	outb_p(MASTER_ICW, NON_SPEC_EOI);
	outb_p(SLAVES_ICW, NON_SPEC_EOI);
}

#ifdef SMP
#define FORALLCPU(cpu)	for (cpu = 0; cpu < num_processors_online; cpu++)
#else
#define FORALLCPU(cpu)	cpu = 0;
#endif

void dump_irqhandlers()
{
	int i, cpu;
	if ((getEFlags() & 0x00000200) == 0)
		printf("Interrupts are disabled (CLI)\n");
	else
		printf("Interrupts are enabled (STI)\n");
	printf("Installed handlers:\n");
	FORALLCPU(cpu) {
#ifdef SMP
		printf("\nCPU #%d\n\n", online_cpu_ID[cpu]);
#endif
		printf("IRQ    PIC   M R S Processed 1stHappen 1stProc  Handler\n\n");
		for (i = 0; i < NUM_IRQs; i++) {
			if (iInfos[i].used == 0)
				continue;	// irq-vector not used
			printf("%3d  %7.7s ", i, iInfos[i].functions->PICname);
			if (iInfos[i].functions == &legacyPIC)
				printf("%d %d %d ", getIMR8259IRQ(i), getIRR8259IRQ(i), getISR8259IRQ(i));
			else
				printf("- - - ");
#ifdef IRQ_STATISTICS
			printf("%8ld  %8ld  %8ld", iprocessed[cpu][i], ifirstlevel_happened[cpu]
			       [i], ifirstlevel_processed[cpu]
			       [i]);
#else
			printf("                 not available                  ");
#endif
			printf(" e=%d ", iInfos[i].enabled);

			if (ifirstlevel_object[cpu][i] != NULL) {
//                       printf(", 1stLevel Handler %p, thread=%p\n", ifirstlevel_object[cpu][i], ithreads[cpu][i]); 
				printf(", 1stLevel Handler %p in domain 0x%lx (%s)\n", ifirstlevel_object[cpu][i],
				       idomains[cpu][i], idomains[cpu][i]->domainName);
				printf("thread=%d.%d", ithreads[cpu][i]->domain->id, ithreads[cpu][i]->id);
				printf("                                           ");
				print_eip_info((char *)
					       ithreads[cpu][i]->context[PCB_EIP]);
				printf("\n");
			} else {
				printf(" \n");
			}
			printf("\n\n");
		}


	}			// end "for cpu"
}

/* t is pointer into old heap ! */
int gc_correct_irqHandlers(ThreadDesc * t, DomainDesc * domain, HandleReference_t handleReference)
{
	int i, cpu;
/*	printf("Seek %d.%d\n", TID(t));*/
	FORALLCPU(cpu) {
		for (i = 0; i < NUM_IRQs; i++) {
			if (iInfos[i].used == 0)
				continue;	// irq-vector not used
			if (ifirstlevel_object[cpu][i] != NULL) {
				/*printf("IRQ handler %d.%d\n", ithreads[cpu][i]->domain->id, ithreads[cpu][i]->id); */
				if (ithreads[cpu][i]->id == t->id && ithreads[cpu][i]->domain->id == t->domain->id) {
					ThreadDesc *tp;
					ThreadDescProxy *tpr;
					HandleReference_t handler = handleReference;
					MOVETCB(ithreads[cpu][i]);
					return 1;
				}
			}
		}
	}
	return 0;
}
#endif				/* KERNEL */
