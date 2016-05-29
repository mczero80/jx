#include "multiboot.h"
#include "misc.h"
#include "lock.h"
#include "debug_reg.h"
#define assert(x)

struct multiboot_info boot_info;

extern addr_t return_address;

extern int main(int argc, char *argv[], char *envp[]);
extern char **environ;

/*
 * Real segment descriptor.
 */
struct x86_desc {
	unsigned int limit_low:16,	/* limit 0..15 */
	 base_low:16,		/* base  0..15 */
	 base_med:8,		/* base  16..23 */
	 access:8,		/* access byte */
	 limit_high:4,		/* limit 16..19 */
	 granularity:4,		/* granularity */
	 base_high:8;		/* base 24..31 */
};

/*
 * Trap, interrupt, or call gate.
 */
struct x86_gate {
	unsigned int offset_low:16,	/* offset 0..15 */
	 selector:16, word_count:8, access:8, offset_high:16;	/* offset 16..31 */
};

/*
 *	Intel x86 32-bit Task State Segment
 */
struct x86_tss {
	int back_link;		/* segment number of previous task,
				   if nested */
	int esp0;		/* initial stack pointer ... */
	int ss0;		/* and segment for ring 0 */
	int esp1;		/* initial stack pointer ... */
	int ss1;		/* and segment for ring 1 */
	int esp2;		/* initial stack pointer ... */
	int ss2;		/* and segment for ring 2 */
	int cr3;		/* CR3 - page table directory
				   physical address */
	int eip;
	int eflags;
	int eax;
	int ecx;
	int edx;
	int ebx;
	int esp;		/* current stack pointer */
	int ebp;
	int esi;
	int edi;
	int es;
	int cs;
	int ss;			/* current stack segment */
	int ds;
	int fs;
	int gs;
	int ldt;		/* local descriptor table segment */
	unsigned short trace_trap;	/* trap on switch to this task */
	unsigned short io_bit_map_offset;
	/* offset to start of IO permission
	   bit map */
};


#define	SZ_32		0x4	/* 32-bit segment */
#define SZ_16		0x0	/* 16-bit segment */
#define	SZ_G		0x8	/* 4K limit field */

#define	ACC_A		0x01	/* accessed */
#define	ACC_TYPE	0x1e	/* type field: */

#define	ACC_TYPE_SYSTEM	0x00	/* system descriptors: */

#define	ACC_LDT		0x02	/* LDT */
#define	ACC_CALL_GATE_16 0x04	/* 16-bit call gate */
#define	ACC_TASK_GATE	0x05	/* task gate */
#define	ACC_TSS		0x09	/* task segment */
#define	ACC_CALL_GATE	0x0c	/* call gate */
#define	ACC_INTR_GATE	0x0e	/* interrupt gate */
#define	ACC_TRAP_GATE	0x0f	/* trap gate */

#define	ACC_TSS_BUSY	0x02	/* task busy */

#define	ACC_TYPE_USER	0x10	/* user descriptors */

#define	ACC_DATA	0x10	/* data */
#define	ACC_DATA_W	0x12	/* data, writable */
#define	ACC_DATA_E	0x14	/* data, expand-down */
#define	ACC_DATA_EW	0x16	/* data, expand-down,
				   writable */
#define	ACC_CODE	0x18	/* code */
#define	ACC_CODE_R	0x1a	/* code, readable */
#define	ACC_CODE_C	0x1c	/* code, conforming */
#define	ACC_CODE_CR	0x1e	/* code, conforming,
				   readable */
#define	ACC_PL		0x60	/* access rights: */
#define	ACC_PL_K	0x00	/* kernel access only */
#define	ACC_PL_U	0x60	/* user access */
#define	ACC_P		0x80	/* segment present */

/*
 * Components of a selector
 */
#define	SEL_LDT		0x04	/* local selector */
#define	SEL_PL		0x03	/* privilege level: */
#define	SEL_PL_K	0x00	/* kernel selector */
#define	SEL_PL_U	0x03	/* user selector */

/* linux segments ?? */
// moved to ..
#include "segments.h"

/*
 *	i386 flags register
 */
#define	EFL_CF		0x00000001	/* carry */
#define	EFL_PF		0x00000004	/* parity of low 8 bits */
#define	EFL_AF		0x00000010	/* carry out of bit 3 */
#define	EFL_ZF		0x00000040	/* zero */
#define	EFL_SF		0x00000080	/* sign */
#define	EFL_TF		0x00000100	/* trace trap */
#define	EFL_IF		0x00000200	/* interrupt enable */
#define	EFL_DF		0x00000400	/* direction */
#define	EFL_OF		0x00000800	/* overflow */
#define	EFL_IOPL	0x00003000	/* IO privilege level: */
#define	EFL_IOPL_KERNEL	0x00000000	/* kernel */
#define	EFL_IOPL_USER	0x00003000	/* user */
#define	EFL_NT		0x00004000	/* nested task */
#define	EFL_RF		0x00010000	/* resume without tracing */
#define	EFL_VM		0x00020000	/* virtual 8086 mode */
#define	EFL_AC		0x00040000	/* alignment check */
#define	EFL_VIF		0x00080000	/* virtual interrupt flag */
#define	EFL_VIP		0x00100000	/* virtual interrupt pending */
#define	EFL_ID		0x00200000	/* CPUID instruction support */

/*
 * Convert selector to descriptor table index.
 */
#define	sel_idx(sel)	((sel)>>3)

/* One entry in the list of gates to initialized.
   Terminate with an entry with a null entrypoint.  */
struct gate_init_entry {
	unsigned entrypoint;
	unsigned short vector;
	unsigned short type;
};

/* Format of a "pseudo-descriptor", used for loading the IDT and GDT.  */
struct pseudo_descriptor {
	short pad;
	unsigned short limit;
	unsigned long linear_base;
};



/* On normal PCs, there are always 16 IRQ lines.  */
#define BASE_IRQ_COUNT		0x10

/* This is the default location in the IDT at which we program the PICs. */
#define BASE_IRQ_MASTER_BASE	0x20
#define BASE_IRQ_SLAVE_BASE	0x28


#define MASTER_PIC_BASE		0x20
#define SLAVES_PIC_BASE		0xa0
#define OFF_ICW			0x00
#define OFF_OCW			0x01

#define MASTER_ICW		(MASTER_PIC_BASE + OFF_ICW)
#define MASTER_OCW		(MASTER_PIC_BASE + OFF_OCW)
#define SLAVES_ICW		(SLAVES_PIC_BASE + OFF_ICW)
#define SLAVES_OCW		(SLAVES_PIC_BASE + OFF_OCW)


/*
** The following banks of definitions ICW1, ICW2, ICW3, and ICW4 are used
** to define the fields of the various ICWs for initialisation of the PICs 
*/

/*
**	ICW1				
*/

#define ICW_TEMPLATE		0x10

#define LEVL_TRIGGER		0x08
#define EDGE_TRIGGER		0x00
#define ADDR_INTRVL4		0x04
#define ADDR_INTRVL8		0x00
#define SINGLE__MODE		0x02
#define CASCADE_MODE		0x00
#define ICW4__NEEDED		0x01
#define NO_ICW4_NEED		0x00

/*
**	ICW2 is the programmable interrupt vector base, not defined here.
*/

/*
**	ICW3				
*/

#define SLAVE_ON_IR0		0x01
#define SLAVE_ON_IR1		0x02
#define SLAVE_ON_IR2		0x04
#define SLAVE_ON_IR3		0x08
#define SLAVE_ON_IR4		0x10
#define SLAVE_ON_IR5		0x20
#define SLAVE_ON_IR6		0x40
#define SLAVE_ON_IR7		0x80

#define I_AM_SLAVE_0		0x00
#define I_AM_SLAVE_1		0x01
#define I_AM_SLAVE_2		0x02
#define I_AM_SLAVE_3		0x03
#define I_AM_SLAVE_4		0x04
#define I_AM_SLAVE_5		0x05
#define I_AM_SLAVE_6		0x06
#define I_AM_SLAVE_7		0x07

/*
**	ICW4				
*/

#define SNF_MODE_ENA		0x10
#define SNF_MODE_DIS		0x00
#define BUFFERD_MODE		0x08
#define NONBUFD_MODE		0x00
#define AUTO_EOI_MOD		0x02
#define NRML_EOI_MOD		0x00
#define I8086_EMM_MOD		0x01
#define SET_MCS_MODE		0x00

/*
**	OCW1				
*/

#define PICM_MASK		0xFF
#define	PICS_MASK		0xFF

/*
**	OCW2				
*/

#define NON_SPEC_EOI		0x20
#define SPECIFIC_EOI		0x60
#define ROT_NON_SPEC		0xa0
#define SET_ROT_AEOI		0x80
#define RSET_ROTAEOI		0x00
#define ROT_SPEC_EOI		0xe0
#define SET_PRIORITY		0xc0
#define NO_OPERATION		0x40

#define SEND_EOI_IR0		0x00
#define SEND_EOI_IR1		0x01
#define SEND_EOI_IR2		0x02
#define SEND_EOI_IR3		0x03
#define SEND_EOI_IR4		0x04
#define SEND_EOI_IR5		0x05
#define SEND_EOI_IR6		0x06
#define SEND_EOI_IR7		0x07

/*
**	OCW3				
*/

#define OCW_TEMPLATE		0x08
#define SPECIAL_MASK		0x40
#define MASK_MDE_SET		0x20
#define MASK_MDE_RST		0x00
#define POLL_COMMAND		0x04
#define NO_POLL_CMND		0x00
#define READ_NEXT_RD		0x02
#define READ_IR_ONRD		0x00
#define READ_IS_ONRD		0x01


/*
**	Standard PIC initialization values for PCs.
*/
#define PICM_ICW1	(ICW_TEMPLATE | EDGE_TRIGGER | ADDR_INTRVL8 \
			 | CASCADE_MODE | ICW4__NEEDED)
#define PICM_ICW3	(SLAVE_ON_IR2)
#define PICM_ICW4	(SNF_MODE_DIS | NONBUFD_MODE | NRML_EOI_MOD \
			 | I8086_EMM_MOD)

#define PICS_ICW1	(ICW_TEMPLATE | EDGE_TRIGGER | ADDR_INTRVL8 \
			 | CASCADE_MODE | ICW4__NEEDED)
#define PICS_ICW3	(I_AM_SLAVE_2)
#define PICS_ICW4	(SNF_MODE_DIS | NONBUFD_MODE | NRML_EOI_MOD \
			 | I8086_EMM_MOD)


/* Variables storing the current master and slave PIC interrupt vector base */
extern int irq_master_base, irq_slave_base;




/* tables */
/*static struct cpu_info base_cpuid;*/


/* Leave a little additional room beyond this for customization */
#define GDTSZ		(0x80/8)
struct x86_desc base_gdt[GDTSZ];

struct x86_tss base_tss;

/*
 * This gate initialization table is used by base_trap_init
 * to initialize the base IDT to the default trap entrypoint code,
 * which pushes the state frame described above
 * and calls the trap handler, below.
 */
extern struct gate_init_entry base_trap_inittab[];
/*
 * This gate initialization table is used by base_irq_init
 * to initialize the hardware interrupt vectors in the base IDT.
 */
extern struct gate_init_entry icore_irq_inittab[];

/* Fill a segment descriptor.  */
void fill_descriptor(struct x86_desc *desc, unsigned base, unsigned limit, unsigned char access, unsigned char sizebits)
{
	if (limit > 0xfffff) {
		limit >>= 12;
		sizebits |= SZ_G;
	}
	desc->limit_low = limit & 0xffff;
	desc->base_low = base & 0xffff;
	desc->base_med = (base >> 16) & 0xff;
	desc->access = access | ACC_P;
	desc->limit_high = limit >> 16;
	desc->granularity = sizebits;
	desc->base_high = base >> 24;
}

/* Fill a gate with particular values.  */
void fill_gate(struct x86_gate *gate, unsigned offset, unsigned short selector, unsigned char access, unsigned char word_count)
{
	gate->offset_low = offset & 0xffff;
	gate->selector = selector;
	gate->word_count = word_count;
	gate->access = access | ACC_P;
	gate->offset_high = (offset >> 16) & 0xffff;
}

void gate_init(struct x86_gate *dest, const struct gate_init_entry *src, unsigned entry_cs)
{
	while (src->entrypoint) {
		fill_gate(&dest[src->vector], src->entrypoint, entry_cs, src->type, 0);
		src++;
	}
}

#define pic_enable_all() ({		\
	outb(MASTER_OCW, 0);		\
	outb(SLAVES_OCW, 0);		\
})

void pic_disable_all()
{
	outb(MASTER_OCW, PICM_MASK);
	outb(SLAVES_OCW, PICS_MASK);
}

#define pic_ack(irq) ({				\
	outb(MASTER_ICW, NON_SPEC_EOI);		\
	if ((irq) >= 8)				\
		outb(SLAVES_ICW, NON_SPEC_EOI);	\
})

void base_gdt_init(void)
{
	/* Initialize the base TSS descriptor.  */
	fill_descriptor(&base_gdt[BASE_TSS / 8], (unsigned) &base_tss, sizeof(base_tss) - 1, ACC_PL_K | ACC_TSS | ACC_P, 0);

	/* Initialize the 32-bit kernel code and data segment descriptors
	   to point to the base of the kernel linear space region.  */
	fill_descriptor(&base_gdt[KERNEL_CS / 8], 0, 0xffffffff, ACC_PL_K | ACC_CODE_R, SZ_32);
	fill_descriptor(&base_gdt[KERNEL_DS / 8], 0, 0xffffffff, ACC_PL_K | ACC_DATA_W, SZ_32);

	/* Corresponding 16-bit code and data segment descriptors,
	   typically used when entering and leaving real mode.  */
	fill_descriptor(&base_gdt[KERNEL_16_CS / 8], 0, 0xffff, ACC_PL_K | ACC_CODE_R, SZ_16);
	fill_descriptor(&base_gdt[KERNEL_16_DS / 8], 0, 0xffff, ACC_PL_K | ACC_DATA_W, SZ_16);

	/* Descriptors that direct-map all linear space.  */
	fill_descriptor(&base_gdt[LINEAR_CS / 8], 0x00000000, 0xffffffff, ACC_PL_K | ACC_CODE_R, SZ_32);
	fill_descriptor(&base_gdt[LINEAR_DS / 8], 0x00000000, 0xffffffff, ACC_PL_K | ACC_DATA_W, SZ_32);
}
void base_tss_init(void)
{
	/* Only initialize once.  */
	if (!base_tss.ss0) {
		base_tss.ss0 = KERNEL_DS;
		base_tss.esp0 = get_esp();	/* only temporary */
		base_tss.io_bit_map_offset = sizeof(base_tss);
	}
}

/*
struct cpu_info base_cpuid;
void cpuid(struct cpu_info *out_id) {
}
*/
void icore_base_cpu_init(void)
{
	/* Detect the current processor.  */
	/*cpuid(&base_cpuid); */

	/* Initialize the processor tables.  */
/*init_intr_gates(); *//* in metaxa_os_IRQ.c */
	base_gdt_init();
	/*base_tss_init(); */
}


/* load tables */
void base_gdt_load(void)
{
	struct pseudo_descriptor pdesc;

	/* Create a pseudo-descriptor describing the GDT.  */
	pdesc.limit = sizeof(base_gdt) - 1;
	pdesc.linear_base = (unsigned long) &base_gdt;

	/* Load it into the CPU.  */
	asm volatile ("lgdt %0"::"m" ((&pdesc)->limit));

	/*
	 * Reload all the segment registers from the new GDT.
	 */
	asm volatile ("ljmp %0,$1f; " "1: nop"::"i" (KERNEL_CS));

	asm volatile ("movw %w0,%%ds"::"r" (KERNEL_DS));
	asm volatile ("movw %w0,%%es"::"r" (KERNEL_DS));
	asm volatile ("movw %w0,%%ss"::"r" (KERNEL_DS));
}

void base_tss_load(void)
{
	/* Make sure the TSS isn't marked busy.  */
	base_gdt[BASE_TSS / 8].access &= ~ACC_TSS_BUSY;

	/* Load the TSS.  */
	asm volatile ("ltr %0"::"rm" ((unsigned short) (BASE_TSS)));
}



void icore_base_cpu_load(void)
{
	base_gdt_load();
	idt_load();		/* in metaxa_os_IRQ.c */
	/*base_tss_load(); */
}

void multiboot_main(addr_t boot_info_pa)
{
	u2_t *screen_start = (u2_t *) 0xb8000;
	u2_t *screen_end = screen_start + 80 * 24;
	u2_t *s;
	int argc = 0;
	char **argv = 0;
	int port = 0;

	asm volatile ("cli");
	init_serial(port);

	/* Copy the multiboot_info structure into our pre-reserved area.
	   This avoids one loose fragment of memory that has to be avoided.  */
	boot_info = *(struct multiboot_info *) boot_info_pa;

	for (s = screen_start; s < screen_end; s++) {
		*s = 0x0f00;
	}

	/*dbg_sync(port); */
	/*dbg_print(port, "JX\n"); */
	/*
	   for(;;) {
	   dbg_print(port, "JX");
	   int c = ser_trygetchar(port);
	   if(c=='#') break; 
	   }
	 */
	/*printf("jxCore running\n"); */

	/* Identify the CPU and get the processor tables set up.  */


	base_gdt_init();
	base_gdt_load();
	idt_load();		/* in irq.c */
	set_dr6(0xFFFF0FF0);	/* clear debug status reg */
	set_b0(0, DR7_LEN_1, DR7_RW_INST);
	set_b1(0, DR7_LEN_4, DR7_RW_DATA);
	/* The  manual recommends executing an LGDT instruction after modifying breakpoint registers. */
	base_gdt_load();

	/*    printf("CPU OK\n"); */

	jxmalloc_init();

	jxmalloc_stat();

	/* Invoke the main program. */
	exit(main(argc, argv, NULL));
}
