#define ENTRY(x) 	.type	 x,@function ;.globl x	;  x:	 
#define DATA32 data32
#define CR0_PE_ON      0x1
#define CR0_PE_OFF     0xfffffffe

#define KERNEL_DS       0x18
#define KERNEL_16_DS    0x28
#define KERNEL_CS	0x10
#define KERNEL_16_CS	0x20

#define REALCODE   0x9000
#define STACKOFF   0x8000
#define SCRATCHMEM 0x7000
#define OTHERMEM   0x6000
	
#if 0
#define WRITE(c)\
	mov    $0x3fd,%dx	; \
123:	in     (%dx),%al	; \
	and    $0x20,%ax	; \
	je     123b		; \
	mov    $0x3f8,%dx	; \
	mov    $c,%al	; \
	out    %al,(%dx)
#endif
#define WRITE(c)
	
		
	.text
.p2align  2   , 0x90 ; .type       _start   ,@function ; .globl     _start     ;     _start:    
				

	.code16
ENTRY(real_to_prot)
	cli

	WRITE('0')
	
	/* load the GDT register */
	/*DATA32	ADDR32	lgdt	gdtdesc*/

	/* turn on protected mode */
	movl	%cr0, %eax
	orl	$CR0_PE_ON, %eax
	movl	%eax, %cr0

	WRITE('1')
	
	/* jump to relocation, flush prefetch queue, and reload %cs */
	DATA32	ljmp	$KERNEL_CS, $protcseg

	.code32

protcseg:
	/* reload other segment registers */
	movw	$KERNEL_DS, %ax
	movw	%ax, %ds
	movw	%ax, %es
	movw	%ax, %fs
	movw	%ax, %gs
	movw	%ax, %ss

	WRITE('2')
	
	/* put the return address in a known safe location */
	movl	(%esp), %eax
	movl	%eax, STACKOFF

	/* get protected mode stack */
	movl	protstack, %eax
	movl	%eax, %esp
	movl	%eax, %ebp

	/* get return address onto the right stack */
	movl	STACKOFF, %eax
	movl	%eax, (%esp)

	/* zero %eax */
	xorl	%eax, %eax

	/* return on the old stack! */
	ret

	.code32
prot_to_real:	

	/* save the protected mode stack */
	movl	%esp, %eax
	movl	%eax, protstack

	/* get the return address */
	movl	(%esp), %eax
	movl	%eax, STACKOFF

	/* set up new stack */
	movl	$STACKOFF, %eax
	movl	%eax, %esp
	movl	%eax, %ebp

	/* set up segment limits */
	movw	$KERNEL_16_DS, %ax
	movw	%ax, %ds
	movw	%ax, %es
	movw	%ax, %fs
	movw	%ax, %gs
	movw	%ax, %ss

	/* this might be an extra step */
	ljmp	$KERNEL_16_CS, $tmpcseg	/* jump to a 16 bit segment */

tmpcseg:	
	.code16

	/* clear the PE bit of CR0 */
	movl	%cr0, %eax
	andl 	$CR0_PE_OFF, %eax
	movl	%eax, %cr0

	/* flush prefetch queue, reload %cs */
	DATA32	ljmp	$0, $realcseg

realcseg:	
	/* we are in real mode now
	 * set up the real mode segment registers : DS, SS, ES
	 */

	/* zero %eax */
	xorl	%eax, %eax

	movw	%ax, %ds
	movw	%ax, %es
	movw	%ax, %fs
	movw	%ax, %gs
	movw	%ax, %ss

#ifdef ALLOW_IRQ	
	/* restore interrupts */
	sti
#endif
	
	/* return on new stack! */
	DATA32	ret

	
/* int vesa2_detect (void)
 *      Returns zero if VESA 2 or better is detected, or non-zero if
 *      not detected.
 */

	.code32
ENTRY(vesa2_detect)
        pushl   %ebp
        pushl   %ebx
        pushl   %edi

	WRITE('N')

        movl    %esp, %ebp

        /* Get the target segment */
        movl $SCRATCHMEM, %ebx
        shrl    $4, %ebx

        /* Enter real mode. */
        call   prot_to_real
        .code16

	WRITE('M')
	
        /* Call the 'GET SuperVGA Information' VESA function. */
        movw    %bx, %es
        xorw    %di, %di
        movw    $0x4F00, %ax
        int     $0x10

        movw    %ax, %bx

	WRITE('L')
		
        /* Return to protected mode. */
       DATA32  call    real_to_prot
       .code32

	WRITE('K')
	
        movl    $1, %eax

        /* First, make sure the call suceeded. */
        cmpb    $0x4F, %bl
        jne     1f

	WRITE('1')
	
        /* Okay, we have a VESA BIOS. Now, make sure the version
           number is high enough. */
        cmpw    $0x200, (SCRATCHMEM)
        jl      1f

        /* We've got it! */
        xorl    %eax, %eax

1:
	WRITE('0')
	
	popl    %edi
        popl    %ebx
        popl    %ebp
        ret

	
ENTRY(testreal)
        pushl   %ebp
        pushl   %ebx
        pushl   %edi

        movl    %esp, %ebp


	WRITE('N')
	
        /* Enter real mode. */
        call   prot_to_real
        .code16

	WRITE('M')

        /* Return to protected mode. */
       DATA32  call    real_to_prot
       .code32

	WRITE('L')
	
	
1:      popl    %edi
        popl    %ebx
        popl    %ebp
        ret
	

/* int vesa2_get_mode_info (int mode, struct vesa_mode_info *vesa_mode_info)
 *      Gets VESA mode information from the VESA BIOS.
 *      Returns zero if successful, or non-zero on error.
 */

ENTRY(vesa2_get_mode_info)
        pushl   %ebp

        movl    %esp, %ebp

        pushl   %ebx
        pushl   %edi

        /* Get the target address and convert to segmented form. */
        movl    12(%ebp), %ebx

       movl    %ebx, %edi
       shrl    $4, %ebx
       andl    $0xFFFF, %edi

        /* Store the mode number in CX. */
        movl    8(%ebp), %ecx

		
        /* Enter real mode. */
        call   prot_to_real
        .code16

        /* Call the 'GET SuperVGA Mode Information' VESA function. */

	/* load segment into %es */
        movw    $0, %ax
	movw	%ax, %es

	/* load offset into %di */
        movw    $0x6000, %ax
	movw	%ax, %di
	
        /*movw    %bx, %es*/
        movw    $0x4f01, %ax
        int     $0x10

        movw    %ax, %bx

        /* Return to protected mode. */
       DATA32  call    real_to_prot
       .code32

        movl    $1, %eax

        /* Make sure the call suceeded. */
        cmpw    $0x004F, %bx
        jne     1f

        /* We've got it! */
        xorl    %eax, %eax

1:      popl    %edi
        popl    %ebx
        popl    %ebp
        ret

/* int vesa2_set_mode (int mode)
 *      Returns zero if the given VESA mode could be set, or non-zero
 *      if not.
 */

ENTRY(vesa2_set_mode)
        pushl   %ebp
        movl    %esp, %ebp

        pushl   %ebx

        movl    8(%ebp), %ebx

        /* Enter real mode. */
        call   prot_to_real
        .code16

        /* Call the 'SET SuperVGA VIDEO MODE' VESA function. */
        movw    $0x4F02, %ax
        int     $0x10

        movw    %ax, %bx

        /* Return to protected mode. */
       DATA32  call   real_to_prot
       .code32

        movl    $1, %eax

        /* Check if the call suceeded. */
        cmpw    $0x004F, %bx
        jne     1f

        xorl    %eax, %eax
1:      popl    %ebx
        popl    %ebp
        ret



	.p2align	2	/* force 4-byte alignment */

	/* this is the place where the stack pointer is saved
	 * at the entry to prot_to_real and restored at exit from
	 * real_to_prot */
	
protstack:
	.long	0

	
/*
 * This is the Global Descriptor Table
 *
 *  An entry, a "Segment Descriptor", looks like this:
 *
 * 31          24         19   16                 7           0
 * ------------------------------------------------------------
 * |             | |B| |A|       | |   |1|0|E|W|A|            |
 * | BASE 31..24 |G|/|0|V| LIMIT |P|DPL|  TYPE   | BASE 23:16 |
 * |             | |D| |L| 19..16| |   |1|1|C|R|A|            |
 * ------------------------------------------------------------
 * |                             |                            |
 * |        BASE 15..0           |       LIMIT 15..0          |
 * |                             |                            |
 * ------------------------------------------------------------
 *
 *  Note the ordering of the data items is reversed from the above
 *  description.
 */

	.p2align	2	/* force 4-byte alignment */
gdt:
	.word	0, 0
	.byte	0, 0, 0, 0

	/* code segment */
	.word	0xFFFF, 0
	.byte	0, 0x9A, 0xCF, 0

	/* data segment */
	.word	0xFFFF, 0
	.byte	0, 0x92, 0xCF, 0

	/* 16 bit real mode CS */
	.word	0xFFFF, 0
	.byte	0, 0x9E, 0, 0

	/* 16 bit real mode DS */
	.word	0xFFFF, 0
	.byte	0, 0x92, 0, 0


/* this is the GDT descriptor */
gdtdesc:
	.word	0x27			/* limit */
	.long	gdt			/* addr */



