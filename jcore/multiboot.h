#ifndef MULTIBOOT_H
#define MULTIBOOT_H

#include "types.h"

/* The entire multiboot_header must be contained
   within the first MULTIBOOT_SEARCH bytes of the kernel image.  */
#define MULTIBOOT_SEARCH	8192

/* Magic value identifying the multiboot_header.  */
#define MULTIBOOT_MAGIC		0x1badb002

/* Features flags for 'flags'.
   If a boot loader sees a flag in MULTIBOOT_MUSTKNOW set
   and it doesn't understand it, it must fail.  */
#define MULTIBOOT_MUSTKNOW	0x0000ffff

/* Align all boot modules on page (4KB) boundaries.  */
#define MULTIBOOT_PAGE_ALIGN	0x00000001

/* Must be provided memory information in multiboot_info structure */
#define MULTIBOOT_MEMORY_INFO	0x00000002

/* Use the load address fields above instead of the ones in the a.out header
   to figure out what to load where, and what to do afterwards.
   This should only be needed for a.out kernel images
   (ELF and other formats can generally provide the needed information).  */
#define MULTIBOOT_AOUT_KLUDGE	0x00010000

/* The boot loader passes this value in register EAX to signal the kernel
   that the multiboot method is being used */
#define MULTIBOOT_VALID         0x2badb002


#define MULTIBOOT_MEMORY	(1L<<0)
#define MULTIBOOT_BOOT_DEVICE	(1L<<1)
#define MULTIBOOT_CMDLINE	(1L<<2)
#define MULTIBOOT_MODS		(1L<<3)
#define MULTIBOOT_AOUT_SYMS	(1L<<4)
#define MULTIBOOT_ELF_SHDR	(1L<<5)
#define MULTIBOOT_MEM_MAP	(1L<<6)

/* For use with printf's %b format. */
#define MULTIBOOT_FLAGS_FORMAT \
  "\20\1MEMORY\2BOOT_DEVICE\3CMDLINE\4MODS\5AOUT_SYMS\6ELF_SHDR\7MEM_MAP"

/* The mods_addr field above contains the physical address of the first
   of 'mods_count' multiboot_module structures.  */


/* The mmap_addr field above contains the physical address of the first
   of the AddrRangeDesc structure.  "size" represents the size of the
   rest of the structure and optional padding.  The offset to the beginning
   of the next structure is therefore "size + 4".  */
struct AddrRangeDesc {
	unsigned long size;
	unsigned long BaseAddrLow;
	unsigned long BaseAddrHigh;
	unsigned long LengthLow;
	unsigned long LengthHigh;
	unsigned long Type;

	/* unspecified optional padding... */
};

/* usable memory "Type", all others are reserved.  */
#define MB_ARD_MEMORY       1


struct multiboot_module {
	/* Physical start and end addresses of the module data itself.  */
	char *mod_start;
	char *mod_end;

	/* Arbitrary ASCII string associated with the module.  */
	char *string;

	/* Boot loader must set to 0; OS must ignore.  */
	unsigned reserved;
};


/* The boot loader passes this data structure to the kernel in
   register EBX on entry.  */
struct multiboot_info {
	/* These flags indicate which parts of the multiboot_info are valid;
	   see below for the actual flag bit definitions.  */
	unsigned flags;

	/* Lower/Upper memory installed in the machine.
	   Valid only if MULTIBOOT_MEMORY is set in flags word above.  */
	size_t mem_lower;
	size_t mem_upper;

	/* BIOS disk device the kernel was loaded from.
	   Valid only if MULTIBOOT_BOOT_DEVICE is set in flags word above.  */
	unsigned char boot_device[4];

	/* Command-line for the OS kernel: a null-terminated ASCII string.
	   Valid only if MULTIBOOT_CMDLINE is set in flags word above.  */
	addr_t cmdline;

	/* List of boot modules loaded with the kernel.
	   Valid only if MULTIBOOT_MODS is set in flags word above.  */
	unsigned mods_count;
	addr_t mods_addr;

	/* Symbol information for a.out or ELF executables. */
	union {
		struct {
			/* a.out symbol information valid only if MULTIBOOT_AOUT_SYMS
			   is set in flags word above.  */
			size_t tabsize;
			size_t strsize;
			addr_t addr;
			unsigned reserved;
		} a;

		struct {
			/* ELF section header information valid only if
			   MULTIBOOT_ELF_SHDR is set in flags word above.  */
			unsigned num;
			size_t size;
			addr_t addr;
			unsigned shndx;
		} e;
	} syms;

	/* Memory map buffer.
	   Valid only if MULTIBOOT_MEM_MAP is set in flags word above.  */
	size_t mmap_count;
	addr_t mmap_addr;
};


extern struct multiboot_info boot_info;

#endif				/* MULTIBOOT_H */
