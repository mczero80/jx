#ifdef KERNEL
#ifdef SMP

#include "config.h"
#include "smp_detect.h"
#include "minic.h"
#include "misc.h"
#include "lapic.h"

// search for "mf?" and fix it

// variables 
int smp_found = 0;		/* Have we found an SMP System */

//Processors
unsigned int num_processors_present = 0;	// # of processors present 
unsigned char BSP_id = 0;	// ID of BootStrapProcessor
unsigned char present_cpu_ID[MAX_NR_CPUS];	// the CPUs local APIC ID of all CPUs
int compatibility_mode;		// see defines COMPATIBILITY_..(in smp_detect.h)

// local APICs
/* moved to lapic.c
unsigned long lapic_addr = 0;     // addr. of the local APIC
int lapic_version[MAX_NR_CPUS];   // version of local APIC 
*/

// IO APICs
int num_io_apic_entries;	// # of I/O APIC entries
struct mpt_config_ioapic io_apics[MAX_IO_APICS];	// I/O APIC entries 

// IRQs
int num_mp_irq_entries;		// # of MP IRQ source entries
struct mpt_config_int mp_irqs[MAX_IRQ_SOURCES];	// MP IRQ source entries 

// Bus
int bus_id_to_type[MAX_MP_BUSSES] = { -1, };	// maps bus IDs to their type


/*********** functions ******************/
/*
void smp_debug_printf(int dbg_level, const char *fmt, ...)
{ 
     va_list args;
//     printf("CPU%d: ",get_processor_id());
     va_start(args, fmt);
     if (dbg_level <= SMP_DEBUG_LEVEL)
	  vprintf(fmt, args);
     va_end(args);
     return;
}
*/

/*
static void print_on_screen(int x, int y, const char * str)
{
 unsigned short *screen_start = (unsigned short *)0xb8000; 
 unsigned short *screen_end = screen_start + 80*24;
 unsigned short *s, i=0;

  for (s = screen_start+(y*80+x); s < screen_end; s++){
    if (str[i] == '\0')
      break;
    *s=0x0f00+str[i];
    i++;
  }
  return;
}

void print_screen(const char * str)
{
  static int x=0,y=0;
  print_on_screen(x,y,str);
  y++;
  if (y > 80)
    return;
  return;
}
*/

// Calc the checksum of an MP block.
//   *mp = start addr of MP block
//   len = length of MP block
//   result = 0 if OK, else !=0
static int mpb_checksum(unsigned char *mp, int len)
{
	int sum = 0;
	while (len--)
		sum += *mp++;
	return sum & 0xFF;
}

// returns the name of a processor
static char *mpc_family(int family, int model)
{
	static char *model_defs[] = {
		"80486DX", "80486DX",
		"80486SX", "80486DX/2 or 80487",
		"80486SL", "Intel5X2(tm)",
		"Unknown", "Unknown",
		"80486DX/4"
	};
	if (family == 0x04 && model < 9)	//100
		return model_defs[model];
	else if (family == 0x5) {	//101
		if (model == 4)
			return ("Pentium with MMX technology");
		else
			return ("Pentium");
	} else if (family == 0x6) {	//110
		if (model == 1)
			return ("Pentium Pro");
		else if (model == 3)
			return ("Pentium II (model 3)");
		else if (model == 5)
			return ("Pentium II (model 5) or Celeron");
		else if (model == 6)
			return ("Celeron");
		else if (model == 7)
			return ("Pentium III (model 7)");
		else if (model == 8)
			return ("Pentium III (model 8) or Celeron");
		else if (model == 10)
			return ("Pentium III Xeon (model A)");
		else
			return ("P6 family");
	} else if (family == 0x0F && model == 0x0F)	//111
		return ("Special controller");
	else
		return ("Unknown CPU");
}

// reads the MP Configuration Table
//  mpct = pointer to the table
//  result = # of processors
static int smp_read_mpct(struct mp_config_table_header *mpcth)
{
	char str[16];
	int count = sizeof(*mpcth);
	unsigned char *mpt = ((unsigned char *) mpcth) + count;	//ptr to the table
	char *bus_type_strings[] = {	// table to map strings to numeric values
		"EISA", "ISA", "INTERN", "MCA", "VL", "PCI", "PCMCIA",
		"CBUS", "CBUSII", "FUTURE", "MBI", "MBII", "MPSA",
		"NUBUS", "TC", "VME", "XPRESS"
	};

	int i;

	for (i = 0; i < 4; i++)
		if ((mpcth->signature)[i] != MPCT_SIGNATURE[i]) {
			smp_debug_printf(SMP_DEBUG_NO, "PANIC:SMP mptable: bad signature [%c%c%c%c]!\n", mpcth->signature[0],
					 mpcth->signature[1], mpcth->signature[2], mpcth->signature[3]);
			return 1;
		}
	if (mpb_checksum((unsigned char *) mpcth, mpcth->length)) {
		smp_debug_printf(SMP_DEBUG_NO, "PANIC:SMP mptable: checksum error!\n");
		return 1;
	}
	smp_debug_printf(SMP_DEBUG_ALL, "MP Config Table version 1.%d\n", mpcth->spec_rev);
	if (mpcth->spec_rev != 0x01 && mpcth->spec_rev != 0x04) {
		smp_debug_printf(SMP_DEBUG_STATE, "Bad Config Table version (%d)!!\n", mpcth->spec_rev);
		return 1;
	}
	memcpy(str, mpcth->oemid, 8);
	str[8] = 0;
	smp_debug_printf(SMP_DEBUG_ALL, "OEM ID: %s ", str);

	memcpy(str, mpcth->productid, 12);
	str[12] = 0;
	smp_debug_printf(SMP_DEBUG_ALL, "Product ID: %s ", str);

	smp_debug_printf(SMP_DEBUG_ALL, "OEM Table Pointer: 0x%lX ", mpcth->oemptr);
	smp_debug_printf(SMP_DEBUG_ALL, "EXTENDED TABLE LENGTH: %d\n", mpcth->exttbllen);

	smp_debug_printf(SMP_DEBUG_ALL, "local APIC at: 0x%lX\n", mpcth->lapic);
	lapic_addr = mpcth->lapic;

	smp_debug_printf(SMP_DEBUG_ALL, "# of table Entries: %d\n", mpcth->count);
	// Now process the configuration blocks.
	while (count < mpcth->length) {
		switch (*mpt) {
		case MPT_PROCESSOR:
			{
				struct mpt_config_processor *m = (struct mpt_config_processor *) mpt;
				if (m->cpuflag & CPU_ENABLED) {
					num_processors_present++;
					smp_debug_printf(SMP_DEBUG_ALL, "Processor #%d(ID:%d) %s APIC version %d\n",
							 num_processors_present, m->lapicid,
							 mpc_family((m->cpusignature & CPU_FAMILY_MASK)
								    >> 8, (m->cpusignature & CPU_MODEL_MASK)
								    >> 4), m->lapicver);
					// see CPPUID
					if (m->featureflags & (1 << 0))
						smp_debug_printf(SMP_DEBUG_ALL, "    Floating point unit present.\n");
					if (m->featureflags & (1 << 7))
						smp_debug_printf(SMP_DEBUG_ALL, "    Machine Exception supported.\n");
					if (m->featureflags & (1 << 8))
						smp_debug_printf(SMP_DEBUG_ALL, "    64 bit compare & exchange supported.\n");
					if (m->featureflags & (1 << 9))
						smp_debug_printf(SMP_DEBUG_ALL, "    Internal APIC present.\n");
					else
						smp_debug_printf(SMP_DEBUG_ALL, "    no Internal APIC present.\n");
					if (m->cpuflag & CPU_BOOTPROCESSOR) {
						smp_debug_printf(SMP_DEBUG_ALL, "    Bootup CPU\n");
						BSP_id = m->lapicid;
					}

					if (num_processors_present > MAX_NR_CPUS)
						smp_debug_printf(SMP_DEBUG_STATE,
								 "Processor ID:%d unused. (Max %d processors).\n", m->lapicid,
								 MAX_NR_CPUS);
					else {
						present_cpu_ID[num_processors_present - 1] = m->lapicid;
						lapic_version[m->lapicid] = m->lapicver;
					}
				}
				mpt += sizeof(*m);
				count += sizeof(*m);
				break;
			}
		case MPT_BUS:
			{
				struct mpt_config_bus *m = (struct mpt_config_bus *) mpt;
				memcpy(str, m->bustype, 6);
				str[6] = 0;
				smp_debug_printf(SMP_DEBUG_ALL, "Bus #%d is %s\n", m->busid, str);


				for (i = 0; i < sizeof(bus_type_strings) / sizeof(char *); i++)
					if (strncmp(m->bustype, bus_type_strings[i], strlen(bus_type_strings[i]))
					    == 0) {
						bus_id_to_type[m->busid] = i;
						break;
					}
				if (i == sizeof(bus_type_strings) / sizeof(char *))
					smp_debug_printf(SMP_DEBUG_STATE, "unrecognized Bus #%d is %s\n", m->busid, str);

				mpt += sizeof(*m);
				count += sizeof(*m);
				break;
			}
		case MPT_IOAPIC:
			{
				struct mpt_config_ioapic *m = (struct mpt_config_ioapic *) mpt;
				if (m->flags & IOAPIC_ENABLED) {
					smp_debug_printf(SMP_DEBUG_ALL, "I/O APIC #%d Version %d at 0x%lX.\n", m->apicid,
							 m->apicver, m->apicaddr);
					io_apics[num_io_apic_entries] = *m;
					if (++num_io_apic_entries > MAX_IO_APICS) {
						smp_debug_printf(SMP_DEBUG_STATE, "Warning: Max I/O APICs exceeded (max %d).\n",
								 MAX_IO_APICS);
//                       smp_debug_printf(SMP_DEBUG_STATE,"Warning: switching to non APIC mode.\n");  // mf?
//                       skip_ioapic_setup=1;
						--num_io_apic_entries;
					}
				} else
					smp_debug_printf(SMP_DEBUG_STATE, "I/O APIC #%d Version %d at 0x%lX is not usable.\n",
							 m->apicid, m->apicver, m->apicaddr);
				mpt += sizeof(*m);
				count += sizeof(*m);
				break;
			}
		case MPT_IOINT:
			{
				struct mpt_config_int *m = (struct mpt_config_int *) mpt;
/*
	       smp_debug_printf(SMP_DEBUG_ALL," Nr. Type Pol. Trig. SrcBusID SrcBusIRQ DestIOAPIC DestIOAPICINTIN#\n");
	       smp_debug_printf(SMP_DEBUG_ALL,"%3d  %3d %3d   %3d    %3d      %3d       %3d        %3d\n",
				num_mp_irq_entries,
				m->irqtype,
				m->irqflag & (2),
				m->irqflag & (2<<2),
				m->srcbus,
				m->srcbusirq,
				m->dstapic,
				m->dstirq);
*/
				mp_irqs[num_mp_irq_entries] = *m;
				if (++num_mp_irq_entries == MAX_IRQ_SOURCES) {
					smp_debug_printf(SMP_DEBUG_STATE, "Max irq sources exceeded!!\n");
					smp_debug_printf(SMP_DEBUG_STATE, "Skipping remaining sources.\n");
					--num_mp_irq_entries;
				}
				mpt += sizeof(*m);
				count += sizeof(*m);
				break;
			}
		case MPT_LINT:
			{
				struct mpt_config_int *m = (struct mpt_config_int *) mpt;

/*	       smp_debug_printf(SMP_DEBUG_ALL," Nr.Type Pol. Trig. SrcBusID SrcBusIRQ DestlAPIC DestlAPICINTIN#\n");
	       smp_debug_printf(SMP_DEBUG_ALL,"%3d  %3d %3d   %3d    %3d      %3d       %3d        %3d\n",
				0,
				m->irqtype,
				m->irqflag & (3),
				m->irqflag & (3<<2),
				m->srcbus,
				m->srcbusirq,
				m->dstapic, 
				m->dstirq); 
	*/
				mpt += sizeof(*m);
				count += sizeof(*m);
				break;
			}
		}
	}
	return num_processors_present;
}

static void construct_default_mpct(unsigned char type)
{
	int i, pos;

	// local APIC 
	lapic_addr = LOCAL_APIC_DEFAULT_PHYS_BASE;
	smp_debug_printf(SMP_DEBUG_ALL, "    local APIC at default Addr: %ld.\n", lapic_addr);
	// Processors
	num_processors_present = 2;
	BSP_id = get_processor_id();
	present_cpu_ID[0] = BSP_id;	//   1   or   2
	present_cpu_ID[1] = 3 - BSP_id;	// 3-1=2 or 3-2=1
	smp_debug_printf(SMP_DEBUG_ALL, "    two processors present.\n");
	// IO APICs
	num_io_apic_entries = 1;
	io_apics[0].apicaddr = IO_APIC_DEFAULT_PHYS_BASE;
	io_apics[0].apicid = 2;
	smp_debug_printf(SMP_DEBUG_ALL, "    I/O APIC at default Addr: %ld.\n", IO_APIC_DEFAULT_PHYS_BASE);


	for (i = 0, pos = 0; i < 16; i++) {
		if (i == 0 || i == 2)
			continue;
		if (type == 2 && (i == 13 || i == 0))
			continue;	// IRQ0 and IRQ13 not connected to IO APIC
		mp_irqs[pos].irqtype = INTTYPE_INT;
		mp_irqs[pos].irqflag = 0;	// default 
		mp_irqs[pos].srcbus = 0;
		mp_irqs[pos].srcbusirq = i;
		mp_irqs[pos].dstapic = 0;
		if (i == 0)
			mp_irqs[pos].dstirq = 2;
		else
			mp_irqs[pos].dstirq = i;
		pos++;
	}
	num_mp_irq_entries = pos;

	smp_debug_printf(SMP_DEBUG_ALL, "Bus #0 is ");
	switch (type) {
	case 1:
	case 5:
		smp_debug_printf(SMP_DEBUG_ALL, "ISA\n");
		bus_id_to_type[0] = BUSTYPE_ISA;
		break;
	case 2:
		smp_debug_printf(SMP_DEBUG_ALL, "(EISA with no IRQ8 chaining =) ");
		// no break;
	case 6:
	case 3:
		smp_debug_printf(SMP_DEBUG_ALL, "EISA\n");
		bus_id_to_type[0] = BUSTYPE_EISA;
		break;
	case 4:
	case 7:
		smp_debug_printf(SMP_DEBUG_ALL, "MCA\n");
		bus_id_to_type[0] = BUSTYPE_MCA;
		break;
	default:
		sys_panic("???\nUnknown standard configuration %d\n", type);
	}
	if (type > 4) {
		smp_debug_printf(SMP_DEBUG_ALL, "Bus #1 is PCI\n");
		lapic_version[0] = APIC_VER_INTEGRATED;
		lapic_version[1] = APIC_VER_INTEGRATED;
		bus_id_to_type[1] = BUSTYPE_PCI;
	} else {
		lapic_version[0] = APIC_VER_82489DX;
		lapic_version[1] = APIC_VER_82489DX;
	}
}




// Scan a memory block for an SMP floating pointer structure
//  base = start addr of mem. block
//  length = length of mem. block
static int smp_search_config(unsigned long base, unsigned long length)
{
	unsigned long *bp = (unsigned long *) base;
	struct mp_floating_pointer_structure *mpfps;

	smp_debug_printf(SMP_DEBUG_ALL, "Scan for MP Floating Prt. Table from %p for %ld bytes.\n", bp, length);

	while (length > 0) {
		if (*bp == MPFPS_ID_SIGNATURE) {
			smp_debug_printf(SMP_DEBUG_ALL, "_MP_ found at 0x%lX\n", bp);
			mpfps = (struct mp_floating_pointer_structure *) bp;
			if (mpfps->length == (MPFPS_SIZE / 16)
			    && !mpb_checksum((unsigned char *) bp, MPFPS_SIZE)
			    && (mpfps->spec_rev == 1 || mpfps->spec_rev == 4)) {
				smp_debug_printf(SMP_DEBUG_ALL, "Intel MultiProcessor Specification v1.%d\n", mpfps->spec_rev);

				if (mpfps->feature2 & (1 << 6))
					smp_debug_printf(SMP_DEBUG_ALL, "    processors have different clock sources).\n");
				else
					smp_debug_printf(SMP_DEBUG_ALL, "    all processors share a single clock source\n");
				compatibility_mode = mpfps->feature2 & (1 << 7);
				if (compatibility_mode == COMPATIBILITY_VIRTWIRE)
					smp_debug_printf(SMP_DEBUG_ALL, "    Virtual Wire compatibility mode.\n");
				else
					smp_debug_printf(SMP_DEBUG_ALL, "    PIC compatibility mode (IMCR present).\n");

				smp_found = 1;

				// check if this is a standard configuration
				if (mpfps->feature1 != 0) {	// this is a default config!!
					smp_debug_printf(SMP_DEBUG_ALL, "    System has a default configuration.\n");
					construct_default_mpct(mpfps->feature1);
				} else {
					if (mpfps->physptr != NULL)
						smp_read_mpct((void *)
							      mpfps->physptr);
				}
				//Only use the first configuration found.
				return 1;
			}	// if length == xxx && checksum == xx && spec_rev == xx
		}		// if "_MP_" found
		bp += 4;
		length -= 16;
	}			// while

	return 0;
}

// Scan for Intel's MP floating pointer stucture
int smp_detect(void)
{
	unsigned short memsize = 0;
	/*
	 * 1) Scan the first 1K of EBDA 
	 * 2) Scan the top 1K of base RAM (639K-640K or 511K-512K)
	 * 3) Scan the BIOS ROM (0x0f0000-0x0fffff) 
	 */
	memsize = (*(unsigned short *) (0x413));
	smp_debug_printf(SMP_DEBUG_ALL, "\nSystem base Memory: %d KBytes\n", memsize);

	if (smp_search_config((*(unsigned short *) (0x40E)), 1024) || smp_search_config(memsize * 1024, 1024) || smp_search_config(0x0F0000, 64 * 1024)	// scan BIOS ROM
	    ) {
//        smp_debug_printf(SMP_DEBUG_STATE,"MP Floating Pointer Structure found!\n  -->This is a multi processor machine! ");
		apic_found = 1;
		return 1;
	} else {
		smp_debug_printf(SMP_DEBUG_STATE,
				 "MP Floating Pointer Structure not found!\n  -->This must be a single processor machine!\n");
		num_processors_online = 1;
		return 0;
	}
}

#endif
#endif
