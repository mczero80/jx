#ifndef CONTEXT_H
#define CONTEXT_H

#ifndef ASSEMBLER

/* 32 bit offsets */
#define PCB_GS  0
#define PCB_FS  1
#define PCB_ES  2
#define PCB_DS  3
#define PCB_EDI 4
#define PCB_ESI 5
#define PCB_EBP 6
#define PCB_ESP 7
#define PCB_EBX 8
#define PCB_EDX 9
#define PCB_ECX 10
#define PCB_EAX 11
#define PCB_EIP 12
#define PCB_EFLAGS 13

typedef unsigned long ContextDesc[14];

#else
/* byte offsets */
#define PCB_GS  0
#define PCB_FS  4
#define PCB_ES  8
#define PCB_DS  12
#define PCB_EDI 16
#define PCB_ESI 20
#define PCB_EBP 24
#define PCB_ESP 28
#define PCB_EBX 32
#define PCB_EDX 36
#define PCB_ECX 40
#define PCB_EAX 44
#define PCB_EIP 48
#define PCB_EFLAGS 52
#endif


/* linux segments */
#define KERNEL_CS	0x10
#define BASE_TSS	0x08
#define KERNEL_DS	0x18	/* Kernel's PL0 data segment */
#define KERNEL_16_CS	0x20	/* 16-bit version of KERNEL_CS */
#define KERNEL_16_DS	0x28	/* 16-bit version of KERNEL_DS */
#define LINEAR_CS	0x30	/* PL0 code segment mapping to linear space */
#define LINEAR_DS	0x38	/* PL0 data segment mapping to linear space */
#define USER_CS		0x43	/* User-defined descriptor, RPL=3 */
#define USER_DS		0x4b	/* User-defined descriptor, RPL=3 */
#define KERNEL_TRAP_TSS 0x50	/* Used by stack-overflow detection code */

#endif				/* CONTEXT_H */
