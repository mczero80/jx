#ifdef SMP

#include "segments.h"
	
/* the memory below 1Meg is free, so let's use anything */	
#define SMP_START_STACK 8000
	
.data

.code16
	
.p2align 4,0x90			/* fill with NOP */
.global smp_startup_begin
smp_startup_begin:	
	
r_base = .

	mov	%cs, %ax	/* Code and data in the same place */
	mov	%ax, %ds

	cli			/* We should be safe anyway */

	lidt	dummy_idt - r_base	/* load idt with 0, 0 */
	lgdt	dummy_gdt - r_base	/* load gdt with ... */

	xor	%ax, %ax
	inc	%ax		/* protected mode (PE) bit */
	lmsw	%ax		/* go into protected mode */

	jmp	1f		/* flush the prefetch-queue */
1:
	ljmpl	$(KERNEL_CS), $setup_stack	

dummy_idt:
	.word	0		/* idt limit = 0 */
	.word	0, 0		/* idt base = 0L */

dummy_gdt:
	.word	0x7F		/* sizeof(base_gdt) - 1 =127 */	
	.long	base_gdt

.global smp_startup_end
smp_startup_end:	


	
.code32	
.p2align 4,0x90			/* fill with NOP */
setup_stack:	
	movl $KERNEL_DS,%eax	/* Set segments to known values */
	movw %ax,%ds
	movw %ax,%es
	movw %ax,%fs
	movw %ax,%gs

	jmp 1f			/* flush the prefetch-queue */
1:	movl $1f,%eax
	jmp *%eax		/* make sure eip is relocated */
1:
	lss stack_start	,%esp
	jmp	1f		/* flush the prefetch-queue */
1:
	ljmpl	$(KERNEL_CS), $start_secondary	/* jump to start_secondary */

	
.p2align 4,0x90			/* fill with NOP */
stack_start:		
	.long SMP_START_STACK
	.long KERNEL_DS
#endif
	
