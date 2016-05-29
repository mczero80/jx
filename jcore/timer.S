/***************************************/
/***************************************/
/***************************************/
/*
* this file contains the assembler code for the interrupt handlers
*
*/


	
#include "config.h"
#include "context.h"
#include "thread.h"

#include "intr.h"
	
.globl hwint00

/* config check */	
#ifdef NO_TIMER_IRQ
#  ifdef SAMPLING_TIMER_IRQ
#    error ERROR NO_TIMER_IRQ with SAMPLING_TIMER_IRQ
#  endif
#  ifdef PROFILE_EVENT_THREADSWITCH_IPSAMPLING
#    error ERROR NO_TIMER_IRQ with PROFILE_EVENT_THREADSWITCH_IPSAMPLING
#  endif
#  ifdef TIMESLICING_TIMER_IRQ
#    error ERROR NO_TIMER_IRQ with TIMESLICING_TIMER_IRQ
#  endif
#endif

#ifdef TIMESLICING_TIMER_IRQ
#  ifdef SAMPLING_TIMER_IRQ
#    error ERROR TIMESLICING_TIMER_IRQ with SAMPLING_TIMER_IRQ
#  endif
#  ifdef PROFILE_EVENT_THREADSWITCH_IPSAMPLING
#    error ERROR TIMESLICING_TIMER_IRQ with PROFILE_EVENT_THREADSWITCH_IPSAMPLING
#  endif
#endif
	
#ifdef SAMPLING_TIMER_IRQ
#  ifdef PROFILE_EVENT_THREADSWITCH_IPSAMPLING
#    error ERROR SAMPLING_TIMER_IRQ with PROFILE_EVENT_THREADSWITCH_IPSAMPLING
#  endif
#endif

	
#ifdef APIC
	#include "irq.h"
	.global	hwint00apic
#endif
		.text

        .align 4
	.type	 hwint00,@function
#ifdef NO_TIMER_IRQ
hwint00:
#ifdef CHECK_SERIAL_IN_TIMER
        SAVE  
	call check_serial_timer
	pushl $0   /* IRQ Nr. */
	call ackIRQ
	popl %eax
        RESTORE
#endif
	iret
.Lhwint00_end:		
	.size hwint00, .Lhwint00_end-hwint00	
#endif

#ifdef SAMPLING_TIMER_IRQ
hwint00:
	SAVE
	call profiler_sample
	pushl $0   /* IRQ Nr. */
	call ackIRQ
	popl %eax
	RESTORE
	iret
#endif

#ifdef PROFILE_EVENT_THREADSWITCH_IPSAMPLING
hwint00:
	SAVE
	call profile_event_threadswitch_ip
	pushl $0   /* IRQ Nr. */
	call ackIRQ
	popl %eax
	RESTORE
	iret
#endif
		
#ifdef TIMESLICING_TIMER_IRQ

#ifdef APIC
hwint00apic:
	/* save used registers */
	pushl    %eax              
	/* increment global timestamp */
	movl	(currenttime), %eax
	incl	%eax
	movl	%eax, (currenttime)

	SAVE   /* todo:	 save only the used regs. */
	call check_serial
	/* reenable PIC  */
	push $0
	call ackIRQ
	pop %eax
RESTORE

	popl %eax
	iret
#endif

#ifdef JAVASCHEDULER
hwint00:
	/* save used registers */
	pushl    %eax 
	/* reenable PIC  */
	/* SMP:	 IRQ0 is not handled by the APIC */
        #define PIC1_CMD_STAT_REGISTER 0x20	
        #define ENABLEALL 0x20
        movb    $ENABLEALL, %al                                                  
	outb    %al, $PIC1_CMD_STAT_REGISTER
	
#ifndef PRODUCTION
	movl	timerticks, %eax
	addl	$4, %eax
	movl	%eax, timerticks
#endif
	
#ifdef CHECK_SERIAL_IN_TIMER
SAVE  
	call check_serial
RESTORE
#endif
	popl %eax

	pushl	$0  
	jmp	hwint
	
.Lhwint00_end:		
	.size hwint00, .Lhwint00_end-hwint00	

#else  /* JAVASCHEDULER */	
		
hwint00:
	/* save state of current thread */
	SAVE	/* this SAVE macro may be used without RESTORE!! */ 
	 /* reenable PIC  */
#ifdef APIC
	/* todo:  check if an APIC was found !! */
	pushl $LAPIC_TIMER_IRQNR   // IRQ Nr.
#else
  	pushl $0   // IRQ Nr.
#endif
	call ackIRQ
	popl %eax

	
	/* save current context */
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

	popl	PCB_EIP(%ecx)  /* pop eip off the stack */
	popl	%ebx  /* pop code segment off the stack, throw away */
	popl	PCB_EFLAGS(%ecx)  /* pop eflags off the stack */

	movl	%esp,PCB_ESP(%ecx) 

	/* context is now completely saved */

#ifndef PRODUCTION
	movl	timerticks, %eax
	addl	$4, %eax
	movl	%eax, timerticks
#endif
		
#ifdef CHECK_SERIAL_IN_TIMER
	call check_serial
#endif
	
	jmp irq_handler_new

/******/	
.Lhwint00_end:		
	.size hwint00, .Lhwint00_end-hwint00

#endif /* JAVASCHEDULER */
#endif /* TIMESLICING_TIMER_IRQ */

	.text
	.align 8
	.globl	irq_destroy_switch_to
	.type	irq_destroy_switch_to,@function
irq_destroy_switch_to:
	/*
	 * switch to new thread.
	 */
	movl	8(%esp),%ecx			/* New thread */
	/*
	 * Set curthreads pointer.
	 */
	movl    4(%esp),%edx		
	movl    %ecx,(%edx)

	/*
	*  Restore context
	*/
	movl	THREAD_PPCB(%ecx),%edx

	movl	PCB_ECX(%edx),%ecx 

	movl	PCB_GS(%edx),%gs
	movl	PCB_FS(%edx),%fs 

	movl	PCB_EDI(%edx),%edi
	movl	PCB_ESI(%edx),%esi
	movl	PCB_EBP(%edx),%ebp					
	movl	PCB_ESP(%edx),%esp

	
	movl	PCB_EFLAGS(%edx),%ebx
	pushl	%ebx   /* efalgs is loaded by the iret instruction */

	movl	$0x10, %ebx
	pushl	%ebx   /* code segment is loaded by the iret instruction */
		
	movl	PCB_EIP(%edx),%ebx
	pushl	%ebx   /* eip is loaded by the iret instruction */

	movl	PCB_EAX(%edx),%ebx
	pushl   %ebx
	movl	PCB_EBX(%edx),%ebx 
	pushl   %ebx
	movl	PCB_EDX(%edx),%edx
	pushl   %edx

			
	popl %edx
	popl %ebx
	popl %eax

	iret
	
	
	
#if 0
/* (bitte nicht loeschen)
   meiks IRQ test funktion:	
   gibt Zeichen am Bildschirm aus */
	
pos:	.long  753664 
color:	.value 0x0431 /* rot */
writetoscreen:
	pushl %eax
	pushl %edx
	
	movw color,%dx  
	movl pos,%eax
	movw %dx,(%eax)
	
	addl $2,pos
	cmpl $757504,%eax
	jbe endoffkt
	movl $753664,pos
	incw color
endoffkt:
	popl %edx
	popl %eax
	ret
#endif
	

