#ifndef __SEGMENTS_H
#define __SEGMENTS_H

#define BASE_TSS	0x08
#define KERNEL_CS	0x10	/* Kernel's PL0 code segment */
#define KERNEL_DS	0x18	/* Kernel's PL0 data segment */
#define KERNEL_16_CS	0x20	/* 16-bit version of KERNEL_CS */
#define KERNEL_16_DS	0x28	/* 16-bit version of KERNEL_DS */
#define LINEAR_CS	0x30	/* PL0 code segment mapping to linear space */
#define LINEAR_DS	0x38	/* PL0 data segment mapping to linear space */
#define USER_CS		0x43	/* User-defined descriptor, RPL=3 */
#define USER_DS		0x4b	/* User-defined descriptor, RPL=3 */
#define KERNEL_TRAP_TSS 0x50	/* Used by stack-overflow detection code */

#endif
