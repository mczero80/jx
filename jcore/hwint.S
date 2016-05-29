#include "context.h"
#include "thread.h"
#include "portal.h"
#include "segments.h"
#include "intr.h"
	
	.text
	
.globl hwint01
.globl hwint02
.globl hwint03
.globl hwint04
.globl hwint05
.globl hwint06
.globl hwint07
.globl hwint08
.globl hwint09
.globl hwint10
.globl hwint11
.globl hwint12
.globl hwint13
.globl hwint14
.globl hwint15
.global local_timer_int
	
	/*
	*
	* Hardware interrupt
	*
	*/	
	.align 4
	.type	 hwint,@function
	.globl hwint
hwint:
	SAVE
	call get_processor_id
	movl	__current(,%eax,4),%ecx	 /* ecx = current[cpu] */
	movl	THREAD_PPCB(%ecx),%ecx	 /* ecx = current[cpu].context */

	popl	PCB_GS(%ecx)
	popl	PCB_FS(%ecx)
	popl	PCB_ES(%ecx)
	popl	PCB_EDI(%ecx) 
	popl	PCB_ESI(%ecx) 
	popl	PCB_EBP(%ecx) 
	popl	%edx		/* esp (throw away) */
	popl	PCB_EBX(%ecx) 
	popl    PCB_EDX(%ecx) 
	popl    PCB_ECX(%ecx) 
	popl	PCB_EAX(%ecx)	
	
	popl	%edx /* edx now contains the interrupt number */
	popl	PCB_EIP(%ecx)  /* pop eip off the stack */
	popl	%ebx  /* pop code segment off the stack, throw away */
	popl	PCB_EFLAGS(%ecx)  /* pop eflags off the stack */

	movl	%esp,PCB_ESP(%ecx) 
	/* save is done , %edx contains irq, %ecx contains context, %eax contains CPU id */
	

	pushl %edx	/* irq */
	pushl %ecx	/* context */
	pushl %eax	/* CPU id */

	call  irq_first_level_handler /* never returns */
	jmp irq_panic 

.Lhwint_end:		
	.size hwint, .Lhwint_end-hwint

	.globl returnfromirq
returnfromirq:		
	/* return from interrupt */
	/* all registers must be restored */

	/*
	*  Restore complete context
	*/
	call    get_processor_id
	movl	__current(,%eax,4),%eax	 /* eax = current[cpu] */
	movl    THREAD_PPCB(%eax),%ecx /* load ctx pointer of next thread */

	movl	PCB_ESP(%ecx),%esp

	movl	PCB_EFLAGS(%ecx),%eax
	pushl	%eax   /* eflags is loaded by the iret instruction */

	movl	$KERNEL_CS, %eax
	pushl	%eax   /* code segment is loaded by the iret instruction */
		
	movl	PCB_EIP(%ecx),%eax
	pushl	%eax   /* eip is loaded by the iret instruction */

	movl	PCB_GS(%ecx),%gs
	movl	PCB_FS(%ecx), %fs 
	movl	PCB_ES(%ecx), %es 
	movl	PCB_EDI(%ecx),%edi
	movl	PCB_ESI(%ecx),%esi
	movl	PCB_EBP(%ecx),%ebp
	movl	PCB_EDX(%ecx), %edx 
	movl	PCB_EAX(%ecx), %eax 
	movl	PCB_EBX(%ecx), %ebx 
	movl	PCB_ECX(%ecx),%ecx /* must be last line ! */
	
	iret
.Lreturnfromirq_end:		
	.size returnfromirq, .Lreturnfromirq_end-returnfromirq
	

#define HWINT_MASTER(irq)\
	pushl	$irq		; \
	jmp	hwint

	
#define HWINT_SLAVE(irq)\
	pushl	$irq		              ;\
	jmp	hwint

        .align 4
hwint01:
        HWINT_MASTER(1)
        .align 4
hwint02:
        HWINT_MASTER(2)
        .align 4
hwint03:
        HWINT_MASTER(3)
        .align 4

	
hwint05:
        HWINT_MASTER(5)
        .align 4
hwint06:
        HWINT_MASTER(6)
        .align 4
hwint07:
        SAVE  
	pushl $7   /* IRQ Nr. */
	call ackIRQ
	popl %eax
        RESTORE
	iret
        .align 4
hwint08:
        HWINT_SLAVE(8)
        .align 4
hwint09:
        HWINT_SLAVE(9)
        .align 4
hwint10:
        HWINT_SLAVE(10)
        .align 4
hwint11:
        HWINT_SLAVE(11)
        .align 4
hwint12:
        HWINT_SLAVE(12)
        .align 4
hwint13:
        HWINT_SLAVE(13)
        .align 4
hwint14:
        HWINT_SLAVE(14)
	.align 4
hwint15:
        HWINT_SLAVE(15)

#ifdef APIC
#include "irq.h"

	.align 4
local_timer_int:
	/* ack APIC */	
	#define		APIC_EOI	0xB0

#ifdef JAVASCHEDULER
 	pushl %eax
	movl lapic_addr,%eax
	movl $0,APIC_EOI(%eax)
	popl %eax
 	
	pushl	$LAPIC_TIMER_IRQNR
	jmp	hwint
#else
	jmp  hwint00

#endif
#endif	

	/* 
	* Prototype:	void activate_irq(ThreadDesc *thread)
	* Never returns. Discards current CPU state.
	* Switches to new thread.
	* Interrupts are locked.
	*/	
	.align 8
	.globl	activate_irq
activate_irq:	
	movl	4(%esp),%ecx			/* thread to switch to */

	/*
	*  Restore context
	*/
	movl	THREAD_PPCB(%ecx),%edx
	movl	PCB_EDI(%edx),%edi
	movl	PCB_ESI(%edx),%esi
	movl	PCB_EBP(%edx),%ebp
	movl	PCB_EAX(%edx),%eax

	movl	PCB_ESP(%edx),%esp
	movl	PCB_EIP(%edx),%ebx
	pushl	%ebx   /* eip is loaded by the ret instruction */

	movl	PCB_EDI(%edx),%edi
	movl	PCB_ESI(%edx),%esi
	movl	PCB_EBP(%edx),%ebp
	movl	PCB_EAX(%edx),%eax
	movl	PCB_ECX(%edx),%ecx 
	movl	PCB_EBX(%edx),%ebx 
	movl	PCB_EDX(%edx),%edx /* must be last line ! */

	ret
		
	
