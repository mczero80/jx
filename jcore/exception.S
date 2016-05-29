/***************************************/
/***************************************/
/***************************************/
/*
* this file contains the assembler code for the interrupt handlers
*
*/

#include "context.h"
#include "thread.h"
#include "intr.h"
	
.globl divide_error
.globl debug_exception
.globl nmi
.globl breakpoint_exception
.globl overflow
.globl bounds_check
.globl inval_opcode
.globl copr_not_available
.globl double_fault
.globl copr_seg_overrun
.globl inval_tss
.globl segment_not_present
.globl stack_exception
.globl general_protection
.globl page_fault
.globl copr_error

.globl notDefined

.globl current_mthread

.text

	
/* Interrupt vectors defined/reserved by processor. */
#define DIVIDE_VECTOR      $0    /* divide error */
#define DEBUG_VECTOR       $1    /* single step (trace) */
#define NMI_VECTOR         $2    /* non-maskable interrupt */
#define BREAKPOINT_VECTOR  $3    /* software breakpoint */
#define OVERFLOW_VECTOR    $4    /* from INTO */

	
/* 286 Exception vector numbers. */
#define BOUNDS_VECTOR       $5   /* bounds check failed */
#define INVAL_OP_VECTOR     $6   /* invalid opcode */
#define COPROC_NOT_VECTOR   $7   /* coprocessor not available */
#define DOUBLE_FAULT_VECTOR $8
#define COPROC_SEG_VECTOR   $9   /* coprocessor segment overrun */
#define INVAL_TSS_VECTOR   $10   /* invalid TSS */
#define SEG_NOT_VECTOR     $11   /* segment not present */
#define STACK_FAULT_VECTOR $12   /* stack exception */
#define PROTECTION_VECTOR  $13   /* general protection */
/* extra 386 Exception vector numbers. */
#define PAGE_FAULT_VECTOR   $14
#define COPROC_ERR_VECTOR   $16  /* coprocessor error */

#define UNKNOWN_EXCEPTION      $0xff    /* divide error */

	
divide_error:
        pushl    DIVIDE_VECTOR
        jmp     exception

	.type	debug_exception,@function
debug_exception:
	SAVE
        call    debug_ex
	RESTORE
	iret
.Ldebug_exception_end:		
	.size debug_exception, .Ldebug_exception_end-debug_exception

nmi:
        pushl    NMI_VECTOR
        jmp     exception

	.type	breakpoint_exception,@function
breakpoint_exception:
	SAVE
        call    breakpoint_ex
	RESTORE
	iret
.Lbreakpoint_exception_end:		
	.size breakpoint_exception, .Lbreakpoint_exception_end-breakpoint_exception

        .align 4
	.globl hwint04
hwint04:
	SAVE
        call    ser_breakpoint_ex
	RESTORE
	iret
	
overflow:
        pushl    OVERFLOW_VECTOR
        jmp     exception

bounds_check:
        pushl    BOUNDS_VECTOR
        jmp     exception

inval_opcode:
        pushl    INVAL_OP_VECTOR
        jmp     exception

copr_not_available:
        pushl    COPROC_NOT_VECTOR
        jmp     exception


copr_seg_overrun:
        pushl    COPROC_SEG_VECTOR
        jmp     exception

double_fault:
        pushl    DOUBLE_FAULT_VECTOR
        jmp     errexception

inval_tss:
        pushl    INVAL_TSS_VECTOR
        jmp     errexception

segment_not_present:
        pushl    SEG_NOT_VECTOR
        jmp     errexception

stack_exception:
        pushl    STACK_FAULT_VECTOR
        jmp     errexception

general_protection:
        pushl    PROTECTION_VECTOR
        jmp     errexception

page_fault:
        pushl    PAGE_FAULT_VECTOR
        jmp     errexception

copr_error:
        pushl    COPROC_ERR_VECTOR
        jmp     exception


        .align 4
	.type	exception,@function
exception:
	SAVE
	
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
	
	popl	%edx  /* pop own error code off the stack */
	popl	PCB_EIP(%ecx)  /* pop eip off the stack */
	popl	%ebx  /* pop code segment off the stack, throw away */
	popl	PCB_EFLAGS(%ecx)  /* pop eflags off the stack */

	movl	%esp,PCB_ESP(%ecx) 
	/* save is done 
		%eax contais cpu id
		%edx contains error code
		%ecx contains thread */
	/* context is now completely saved */
	
	pushl	%eax  /* cpu id */
	pushl	%edx  /* error cod */  
	call	hw_exception
	iret
.Lexception_end:		
	.size exception, .Lexception_end-exception

        .align 4
	.type	errexception,@function 
errexception:
	SAVE
	
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
	
	popl	%edx  /* pop own error code off the stack */
	popl	%ebx  /* pop system error code off the stack */
	popl	PCB_EIP(%ecx)  /* pop eip off the stack */
	popl	%edi  /* pop code segment off the stack, throw away */
	popl	PCB_EFLAGS(%ecx)  /* pop eflags off the stack */

	movl	%esp,PCB_ESP(%ecx) 
	/* context is now completely saved */

	pushl	%eax	/* cpu id */
	pushl	%ebx	/* system error code */
	pushl	%edx	/* own error code */
	call	hw_errexception
	iret
.Lerrexception_end:		
	.size errexception, .Lerrexception_end-errexception

.align  4
exceptF:
except11:
except12:
except13:
except14:
except15:
except16:
except17:
except18:
except19:
except1A:
except1B:
except1C:
except1D:
except1E:
except1F:
notDefined:
        pushl    UNKNOWN_EXCEPTION
	jmp exception

#ifdef APIC
#include "irq.h"
.global spurious_apic
.global error_apic
	
spurious_apic:
        pushl    SPURIOUS_APIC_VECTOR
        jmp     exception
error_apic:
	SAVE
        call    APIC_error_interrupt
	RESTORE
	iret

#endif
