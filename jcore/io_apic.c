#ifdef KERNEL
#ifdef SMP

#include "smp_detect.h"
#include "misc.h"
#include "smp_detect.h"
#include "irq.h"
#include "lapic.h"
#include "io_apic.h"

// The registers of the IO APIC:
struct IO_APIC_ID_reg {
	int reserved_2:24, ID:4, reserved_1:4;
} __attribute__ ((packed));

struct IO_APIC_VER_reg {
	int version:8, reserved_2:8, entries:8, reserved_1:8;
} __attribute__ ((packed));

struct IO_APIC_ARB_reg {
	int reserved_2:24, arbitration:4, reserved_1:4;
} __attribute__ ((packed));


// # of IRQ routing registers
static int nr_ioapic_irq_pins[MAX_IO_APICS];

struct IO_APIC_route_entry {
	int vector:8, delivery_mode:3,	// see DELIVERY_MODE... defines
	 dest_mode:1,		// see DEST_MODE_... defines
	 delivery_status:1, polarity:1, irr:1, trigger:1,	/* see APICTRIGGER_.. defines */
	 mask:1,		/* 0: enabled, 1: disabled */
	 reserved_2:15;
	union {
		struct {
			long reserved_1:24, physical_dest:4, reserved_2:4;
		} physical;
		struct {
			long reserved_1:24, logical_dest:8;
		} logical;
	} dest;

} __attribute__ ((packed));

#define DELIVERY_MODE_FIXED     0
#define DELIVERY_MODE_LOWESTPRI 1
#define DELIVERY_MODE_SMI       2
#define DELIVERY_MODE_NMI       4
#define DELIVERY_MODE_INIT      5
#define DELIVERY_MODE_EXTINT    7

#define DEST_MODE_PHYSICAL 0
#define DEST_MODE_LOGICAL  1

#define APICPOL_LOW     1
#define APICPOL_HIGH    0

#define APICTRIGGER_EDGE  0
#define APICTRIGGER_LEVEL 1


#define IO_APIC_BASE(idx) ((volatile int *)(io_apics[idx].apicaddr))
#define IO_APIC_IDReg      0x00
#define IO_APIC_VERReg     0x01
#define IO_APIC_ARBReg     0x02
#define IO_APIC_REDTBLReg  0x10

static inline unsigned int io_apic_read(unsigned int apic, unsigned int reg)
{
	*IO_APIC_BASE(apic) = reg;	// write reg to IOREGSEL
	return *(IO_APIC_BASE(apic) + 4);	// read IOWIN
}

static inline void io_apic_write(unsigned int apic, unsigned int reg, unsigned int value)
{
	*IO_APIC_BASE(apic) = reg;	// write to IOREGSEL
	*(IO_APIC_BASE(apic) + 4) = value;	// read IOWIN
}

static inline void io_apic_write_route_entry(unsigned int apic, unsigned int pin, struct IO_APIC_route_entry entry)
{
	io_apic_write(apic, IO_APIC_REDTBLReg + 2 * pin, *(((int *) &entry) + 0));
	io_apic_write(apic, IO_APIC_REDTBLReg + 1 + 2 * pin, *(((int *) &entry) + 1));
}

/** returns the PINth routing table entry in ENTRY*/
static inline void io_apic_read_route_entry(unsigned int apic, unsigned int pin, struct IO_APIC_route_entry
					    *entry)
{
	*(((int *) entry) + 0) = io_apic_read(apic, IO_APIC_REDTBLReg + pin * 2);
	*(((int *) entry) + 1) = io_apic_read(apic, IO_APIC_REDTBLReg + 1 + pin * 2);
}

static void clear_IO_APIC_pin(unsigned int apic, unsigned int pin)
{
	struct IO_APIC_route_entry entry;

	/*
	 * Disable it in the IO-APIC irq-routing table:
	 */
	memset(&entry, 0, sizeof(entry));
	entry.mask = 1;
	io_apic_write_route_entry(apic, pin, entry);
}

static void clear_IO_APIC(void)
{
	int apic, pin;

	for (apic = 0; apic < num_io_apic_entries; apic++)
		for (pin = 0; pin < nr_ioapic_irq_pins[apic]; pin++)
			clear_IO_APIC_pin(apic, pin);
}


// Find the IRQ entry number of a certain pin.
static int find_irq_entry(int apic, int pin, int type)
{
	int i;
	for (i = 0; i < num_mp_irq_entries; i++)
		if ((mp_irqs[i].irqtype == type) && (mp_irqs[i].dstapic == io_apics[apic].apicid) && (mp_irqs[i].dstirq == pin))
			return i;
	return -1;
}

static int irq_polarity(int idx)
{
	// Determine IRQ line polarity (high active or low active):
	switch (mp_irqs[idx].irqflag & 3) {
	case IRQPOL_DEFAULT:	// conforms, ie. bus-type dependent polarity 

		switch (bus_id_to_type[mp_irqs[idx].srcbus]) {
		case BUSTYPE_ISA:
			return APICPOL_HIGH;	// ISA interrupts are always high polarity
		case BUSTYPE_PCI:
			return APICPOL_LOW;	// PCI interrupts are always low polarity 
		default:
			smp_debug_printf(SMP_DEBUG_STATE, "unsupported bus type\n");
			return APICPOL_LOW;
		}
	case IRQPOL_HIGH:
		return APICPOL_HIGH;
	case IRQPOL_RESERVED:
		smp_debug_printf(SMP_DEBUG_STATE, "unsupported polarity (reserved)\n");
		return APICPOL_LOW;
	case IRQPOL_LOW:
		return APICPOL_LOW;
	default:		/* invalid */
		smp_debug_printf(SMP_DEBUG_STATE, "invalid polarity \n");
		return APICPOL_LOW;
	}
}

static int irq_trigger(int idx)
{
	// Determine IRQ trigger mode (edge or level sensitive):
	switch ((mp_irqs[idx].irqflag >> 2) & 3) {
	case IRQTRIGER_DEFAULT:	// conforms, ie. bus-type dependent 

		switch (bus_id_to_type[mp_irqs[idx].srcbus]) {
		case BUSTYPE_ISA:
			return APICTRIGGER_EDGE;	// ISA interrupts are always edge triggered
		case BUSTYPE_PCI:
			return APICTRIGGER_LEVEL;	// PCI interrupts are always level triggered
		default:
			smp_debug_printf(SMP_DEBUG_STATE, "unsupported bus type\n");
			return APICTRIGGER_LEVEL;
		}
	case IRQTRIGER_EDGE:
		return APICTRIGGER_EDGE;
	case IRQTRIGER_RESERVED:
		smp_debug_printf(SMP_DEBUG_STATE, "unsupported Trigger mode (reserved)\n");
		return APICTRIGGER_LEVEL;
	case IRQTRIGER_LEVEL:
		return APICTRIGGER_LEVEL;
	default:		/* invalid */
		smp_debug_printf(SMP_DEBUG_STATE, "invalid trigger mode \n");
		return APICTRIGGER_EDGE;
	}
}


static int pin_2_irq(int idx, int apic, int pin)
{
	int irq, i;

	switch (bus_id_to_type[mp_irqs[idx].srcbus]) {
	case BUSTYPE_ISA:
	case BUSTYPE_EISA:
		irq = mp_irqs[idx].srcbusirq;
		break;
	case BUSTYPE_PCI:
		// PCI IRQs are mapped in order
		i = irq = 0;
		while (i < apic)
			irq += nr_ioapic_irq_pins[i++];
		irq += pin;
		break;
	default:
		smp_debug_printf(SMP_DEBUG_STATE, "unsupported bus type: bus#%d\n", mp_irqs[idx].srcbus);
		irq = 0;
		break;
	}
	return irq;
}

// APIC vector available to drivers: (vectors 0x31-0xfe)
#define FIRST_DEVICE_VECTOR    0x31
#define FIRST_SYSTEM_VECTOR    0xef

static int assign_irq_vector(int irq)
{
	// NOTE! The local APIC isn't very good at handling multiple interrupts at the same interrupt level.
	// As the interrupt level is determined by taking the vector number and shifting that right by 4, (divide by 16)
	// we want to spread these out a bit so that they don't all fall in the same interrupt level. (max. 2 at the same level)
	static int current_vector = FIRST_DEVICE_VECTOR, offset = 0;
	if (current_vector > FIRST_SYSTEM_VECTOR) {
		offset++;
		current_vector = FIRST_DEVICE_VECTOR + offset;
		smp_debug_printf(SMP_DEBUG_ALL, "WARNING: ASSIGN_IRQ_VECTOR wrapped back to %02X\n", current_vector);
	}
	if (current_vector == FIRST_SYSTEM_VECTOR)
		sys_panic("ran out of interrupt sources!");

	current_vector += 8;

	return current_vector;
}



static void setup_IO_APIC_irqs(void)
{
	struct IO_APIC_route_entry entry;
	int apic, pin, idx, irq;

	smp_debug_printf(SMP_DEBUG_ALL, "init IO_APIC IRQs\n");

	for (apic = 0; apic < num_io_apic_entries; apic++)
		for (pin = 0; pin < nr_ioapic_irq_pins[apic]; pin++) {

			// add it to the IO-APIC irq-routing table:
			memset(&entry, 0, sizeof(entry));

			entry.delivery_mode = DELIVERY_MODE_FIXED;
			entry.dest_mode = DEST_MODE_PHYSICAL;
			entry.mask = 1;	/* disable IRQ by default */
			entry.dest.physical.physical_dest = BSP_id;	// to the BSP

			idx = find_irq_entry(apic, pin, INTTYPE_INT);
			if (idx == -1) {
				smp_debug_printf(SMP_DEBUG_ALL, " IO-APIC #%d Pin #%d not connected\n", io_apics[apic].apicid,
						 pin);
				continue;
			}

			entry.trigger = irq_trigger(idx);
			entry.polarity = irq_polarity(idx);

			irq = pin_2_irq(idx, apic, pin);

/*		  
#define  SYSTEMTIMER_VECTOR 0xf2
		  if (irq == 0)   // the timer IRQ schould be high prior
		       entry.vector = SYSTEMTIMER_VECTOR;
		  else
*/ entry.vector = assign_irq_vector(irq);

			iInfos[irq].used = 1;
			iInfos[irq].apic = apic;
			iInfos[irq].pinNr = pin;	//only the last pin is saved  (if an IRQ is connected to more than 1 pin)
			iInfos[irq].vector_number = entry.vector;

			io_apic_write_route_entry(apic, pin, entry);
		}
}

void print_IO_APIC(void)
{
	int apic, i;
	struct IO_APIC_ID_reg id_reg;
	struct IO_APIC_VER_reg ver_reg;
	struct IO_APIC_ARB_reg arb_reg;

	smp_debug_printf(SMP_DEBUG_ALL, "number of IRQ sources: %d.\n", num_mp_irq_entries);
	for (i = 0; i < num_io_apic_entries; i++)
		smp_debug_printf(SMP_DEBUG_ALL, "number of pins on IO-APIC #%d: %d.\n", io_apics[i].apicid,
				 nr_ioapic_irq_pins[i]);

	for (apic = 0; apic < num_io_apic_entries; apic++) {
		*(int *) &id_reg = io_apic_read(apic, IO_APIC_IDReg);
		*(int *) &ver_reg = io_apic_read(apic, IO_APIC_VERReg);
		*(int *) &arb_reg = io_apic_read(apic, IO_APIC_ARBReg);
		smp_debug_printf(SMP_DEBUG_ALL, "\nIO APIC #%d......\n", io_apics[apic].apicid);
		smp_debug_printf(SMP_DEBUG_ALL, ".... ID register: %08X\n", *(int *) &id_reg);
		smp_debug_printf(SMP_DEBUG_ALL, ".......    : physical APIC id: %02X\n", id_reg.ID);
		if (id_reg.reserved_1 || id_reg.reserved_2)
			smp_debug_printf(SMP_DEBUG_ALL, " WARNING: reserved bits are not 0\n");

		smp_debug_printf(SMP_DEBUG_ALL, ".... VER register: %08X\n", *(int *) &ver_reg);
		smp_debug_printf(SMP_DEBUG_ALL, ".......     : max redirection entries: %04X\n", ver_reg.entries);
		if ((ver_reg.entries != 0x0f) &&	/* older (Neptune) boards */
		    (ver_reg.entries != 0x17) &&	/* typical ISA+PCI boards */
		    (ver_reg.entries != 0x1b) &&	/* Compaq Proliant boards */
		    (ver_reg.entries != 0x1f) &&	/* dual Xeon boards */
		    (ver_reg.entries != 0x22) &&	/* bigger Xeon boards */
		    (ver_reg.entries != 0x2E) && (ver_reg.entries != 0x3F)
		    )
			smp_debug_printf(SMP_DEBUG_ALL, " WARNING: unexpected # of entries\n");

		smp_debug_printf(SMP_DEBUG_ALL, ".......     : IO APIC version: %04X\n", ver_reg.version);
		if ((ver_reg.version != 0x10) &&	/* oldest IO-APICs */
		    (ver_reg.version != 0x11) &&	/* Pentium/Pro IO-APICs */
		    (ver_reg.version != 0x13)	/* Xeon IO-APICs */
		    )
			smp_debug_printf(SMP_DEBUG_ALL, " WARNING: unexpected version\n");

		if (ver_reg.reserved_1 || ver_reg.reserved_2)
			smp_debug_printf(SMP_DEBUG_ALL, " WARNING: reserved bits are not 0\n");

		smp_debug_printf(SMP_DEBUG_ALL, ".... ARB register: %08X\n", *(int *) &arb_reg);
		smp_debug_printf(SMP_DEBUG_ALL, ".......     : arbitration: %02X\n", arb_reg.arbitration);
		if (arb_reg.reserved_1 || arb_reg.reserved_2)
			smp_debug_printf(SMP_DEBUG_ALL, " WARNING: reserved bits are not 0\n");


		smp_debug_printf(SMP_DEBUG_ALL, ".... IRQ redirection table:\n");

		smp_debug_printf(SMP_DEBUG_ALL, "Pin Log Phy ");
		smp_debug_printf(SMP_DEBUG_ALL, "Mask Trig IRR Pol Stat Dest Deli Vect:   \n");

		for (i = 0; i <= ver_reg.entries; i++) {
			struct IO_APIC_route_entry entry;
			io_apic_read_route_entry(apic, i, &entry);

			smp_debug_printf(SMP_DEBUG_ALL, " %2d %03X %02X  ", i, entry.dest.logical.logical_dest & 0xfff,
					 entry.dest.physical.physical_dest & 0xff);

			smp_debug_printf(SMP_DEBUG_ALL, " %2d   %2d  %2d  %2d   %2d   %2d   %2d   %02Xh\n", entry.mask,
					 entry.trigger, entry.irr, entry.polarity, entry.delivery_status, entry.dest_mode,
					 entry.delivery_mode, entry.vector & 0xff);
		}
	}
	smp_debug_printf(SMP_DEBUG_ALL, "IRQ to pin mappings:\n");
	for (i = 0; i < NUM_IRQs; i++) {
		if (iInfos[i].apic < 0 || iInfos[i].used == 0)
			continue;
		smp_debug_printf(SMP_DEBUG_ALL, "IRQ%d -> %d\n", i, iInfos[i].pinNr);
	}

	smp_debug_printf(SMP_DEBUG_ALL, ".................................... done.\n");

	return;
}

#ifdef SMP
/** sets the destination CPU of an IRQ (-1 = all CPUs) */
void set_irq_destination(unsigned int irq, char new_dest)
{
	struct IO_APIC_route_entry entry;
	if (irq >= NUM_IRQs)
		return;

	io_apic_read_route_entry(iInfos[irq].apic, iInfos[irq].pinNr, &entry);

	entry.delivery_mode = DELIVERY_MODE_FIXED;
	entry.dest_mode = DEST_MODE_PHYSICAL;
	entry.dest.physical.physical_dest = new_dest;

	io_apic_write_route_entry(iInfos[irq].apic, iInfos[irq].pinNr, entry);
}
#endif

static void enable_IO_APIC_irq(unsigned int irq)
{
	struct IO_APIC_route_entry entry;
	/*printf("enable_IO_APIC_irq %d\n", irq); */
	if (irq >= NUM_IRQs)
		return;
	io_apic_read_route_entry(iInfos[irq].apic, iInfos[irq].pinNr, &entry);
	entry.mask = 0;		// unmask it
	io_apic_write_route_entry(iInfos[irq].apic, iInfos[irq].pinNr, entry);
}

static void disable_IO_APIC_irq(unsigned int irq)
{
	struct IO_APIC_route_entry entry;
	/*printf("disable_IO_APIC_irq %d\n", irq); */
	if (irq >= NUM_IRQs)
		return;
	io_apic_read_route_entry(iInfos[irq].apic, iInfos[irq].pinNr, &entry);
	entry.mask = 1;		// unmask it
	io_apic_write_route_entry(iInfos[irq].apic, iInfos[irq].pinNr, entry);
}

static void ack_IO_APIC_irq(unsigned int irq)
{
	ack_APIC_irq();		// in lapic.c
}


static struct irqfunctions advancedPIC = {
	"IO-APIC",
	ack_IO_APIC_irq,
	enable_IO_APIC_irq,
	disable_IO_APIC_irq
};


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


/*
// map of the names of the IRQ traps 
#define IRQ_NAME_2(x,y) hwint##x##y
#define IRQ_NAME_1(x)  IRQ_NAME_2(x,0), IRQ_NAME_2(x,1), IRQ_NAME_2(x,2), IRQ_NAME_2(x,3), IRQ_NAME_2(x,4), \
                       IRQ_NAME_2(x,5), IRQ_NAME_2(x,6), IRQ_NAME_2(x,7), IRQ_NAME_2(x,8), IRQ_NAME_2(x,9) 
static void (*interrupt_trap_name[NUM_IRQs])(void) = {IRQ_NAME_1(0), IRQ_NAME_1(1), };
*/
static void (*interrupt_trap_name[NUM_IRQs]) (void) = {
hwint00, hwint01, hwint02, hwint03, hwint04, hwint05, hwint06, hwint07, hwint08, hwint09, hwint10, hwint11, hwint12,
	    hwint13, hwint14, hwint15, /*... */ };

static inline void init_IO_APIC_traps(void)
{
	int i;
	for (i = 0; i < NUM_IRQs; i++)
		if (iInfos[i].apic >= 0) {	// IRQ handled by APIC
			// disable it in the 8259A 
			if (i < 16)
				disableIRQ(i);	//irqfunctions point to the old values, so this should work

			// install new irqfunctions
			iInfos[i].functions = &advancedPIC;
			// install IDT Entry
			int_gate(iInfos[i].vector_number, (u4_t) interrupt_trap_name[i], 0x80 /*present bit */  | 14	/*irq gate */
			    );
		}
}

// Set the IO-APIC physical IDs based on the values stored in the MPC table 
static void setup_ioapic_IDs_from_mpt(void)
{
	struct IO_APIC_ID_reg ID_reg;

	// Set the IOAPIC ID to the value stored in the MPC table.
	int apic;
	for (apic = 0; apic < num_io_apic_entries; apic++) {
		/* Read the ID register value */
		*(int *) &ID_reg = io_apic_read(apic, IO_APIC_IDReg);

		if (ID_reg.ID != io_apics[apic].apicid) {
			if (io_apics[apic].apicid > 15)
				smp_debug_printf(SMP_DEBUG_ALL,
						 "BIOS wants to set APIC ID to %d, ignoring and praying the BIOS setup is ok\n",
						 io_apics[apic].apicid);
			else {
				/* Change the value */
				smp_debug_printf(SMP_DEBUG_ALL, "...changing IO-APIC physical APIC ID to %d\n",
						 io_apics[apic].apicid);
				ID_reg.ID = io_apics[apic].apicid;
				io_apic_write(apic, IO_APIC_IDReg, *(int *) &ID_reg);

				// Sanity check
				*(int *) &ID_reg = io_apic_read(apic, IO_APIC_IDReg);
				if (ID_reg.ID != io_apics[apic].apicid)
					sys_panic("could not set IO-APIC ID");
			}
		}
	}
}

void setup_IO_APIC(void)
{
	struct IO_APIC_VER_reg ver_reg;
	int i;

	// no IRQ ist handled by the APIC yet
	for (i = 0; i < NUM_IRQs; i++) {
		iInfos[i].apic = -1;
		iInfos[i].used = 0;
	}

	// save the number of IO-APIC IRQ pins
	for (i = 0; i < num_io_apic_entries; i++) {
		*(int *) &ver_reg = io_apic_read(i, IO_APIC_VERReg);
		nr_ioapic_irq_pins[i] = ver_reg.entries + 1;
	}

	//clear the IO-APIC Redirection Table
	clear_IO_APIC();

	// switch to symmetric IO
	if (compatibility_mode == COMPATIBILITY_PIC) {
		smp_debug_printf(SMP_DEBUG_ALL, "disabling PIC mode\n");
		outb(0x70, 0x22);	// write to IMCR   
		outb(0x01, 0x23);	// disable PIC Mode
	}

	smp_debug_printf(SMP_DEBUG_STATE, "ENABLING IO-APIC IRQs\n");

	setup_ioapic_IDs_from_mpt();	// setup IDs
	// Set up the IO-APIC IRQ routing table by parsing the MP-BIOS mptable:
	setup_IO_APIC_irqs();
	init_IO_APIC_traps();
//     print_IO_APIC();
}

#endif
#endif
