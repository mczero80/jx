#include "context.h"
#include "thread.h"
#include "intr.h"


#ifdef KERNEL

	/* save current context and activate PARTIAL next context 
	* next context must have been saved by a switch call and not
	* by a interrupt!
	*/
	/* Prototype:	 jint internal_switch_to(ThreadDesc **current, ThreadDesc *to) ;  */
	.text
	.align 8
	.globl	internal_switch_to
	.type	internal_switch_to,@function
internal_switch_to:	
	/* save eflags */
	cli

	movl    4(%esp),%ecx
	movl    0(%ecx),%ecx

	movl	THREAD_PPCB(%ecx),%ecx
	movl	0(%esp),%eax			/* get eip (switch_to return address) */
	movl	%eax,PCB_EIP(%ecx)
	movl	%ebx,PCB_EBX(%ecx)
	movl	%esp,%eax
	addl	$4, %eax
	movl	%eax, PCB_ESP(%ecx)
	movl	%ebp,PCB_EBP(%ecx)
	movl	%esi,PCB_ESI(%ecx)
	movl	%edi,PCB_EDI(%ecx)
#ifdef KERNEL	
	movl	%es, PCB_ES(%ecx)
	movl	%gs, PCB_GS(%ecx)
	movl	%fs, PCB_FS(%ecx) 
#endif /* KERNEL */
	pushf
	popl    PCB_EFLAGS(%ecx)

	
#ifdef PROFILE_EVENT_THREADSWITCH
	pushl	8(%esp)	      /* New thread */
	call	profile_event_threadswitch_to
	popl	%ecx /* correct esp */
#endif	/* PROFILE_EVENT_THREADSWITCH */
#ifdef CPU_USAGE_STATISTICS
	call	profile_cputime
#endif /* CPU_USAGE_STATISTICS */

	/*
	 * Save is done, switch to new thread.
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
#ifdef KERNEL
	movl	PCB_GS(%edx),%gs
	movl	PCB_FS(%edx), %fs 
	movl	PCB_ES(%edx), %es 
#endif /* KERNEL */

	movl	PCB_ESP(%edx),%esp
	pushl	PCB_EIP(%edx)  /* eip is loaded by the ret instruction */
	pushl	PCB_EFLAGS(%edx) /* eflags */
	movl	PCB_EDI(%edx),%edi
	movl	PCB_ESI(%edx),%esi
	movl	PCB_EBP(%edx),%ebp
	movl	PCB_EAX(%edx),%eax
	movl	PCB_ECX(%edx),%ecx 
	movl	PCB_EBX(%edx),%ebx 
	movl	PCB_EDX(%edx),%edx  /* invalidates edx!! (->last line)*/

	popf /* from here interrupts are possible */
	
	ret
.globl internal_switch_to_end	
internal_switch_to_end:		
	.size internal_switch_to, internal_switch_to_end-internal_switch_to
	nop
	

	/* activate next context WITHOUT saving current context
	  or even touching current thread struct (may be deallocated) */
	/* Prototype:	 jint destroy_switch_to(ThreadDesc **current, ThreadDesc *to) ;  */
	.text
	.align 8
	.globl	destroy_switch_to
	.type	destroy_switch_to,@function
destroy_switch_to:	
#ifdef PROFILE_EVENT_THREADSWITCH
	pushl	8(%esp)	      /* New thread */
	call	profile_event_threadswitch_to
	popl	%ecx
#endif	/* PROFILE_EVENT_THREADSWITCH */
#ifdef CPU_USAGE_STATISTICS
	call	profile_cputime
#endif /* CPU_USAGE_STATISTICS */
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
#ifdef KERNEL
	movl	PCB_ES(%edx),%es
	movl	PCB_GS(%edx),%gs
	movl	PCB_FS(%edx), %fs 
#endif
	movl	PCB_EDI(%edx),%edi
	movl	PCB_ESI(%edx),%esi
	movl	PCB_EBP(%edx),%ebp
	movl	PCB_EAX(%edx),%eax

	movl	PCB_ESP(%edx),%esp
	movl	PCB_EIP(%edx),%ebx
	pushl	%ebx   /* eip is loaded by the ret instruction */

	movl	PCB_EDI(%edx),%ebx
	pushl   %ebx
	movl	PCB_ESI(%edx),%ebx
	pushl   %ebx
	movl	PCB_EBP(%edx),%ebx
	pushl   %ebx
	movl	PCB_EAX(%edx),%ebx
	pushl   %ebx
	
	movl	PCB_ECX(%edx),%ebx 
	pushl   %ebx
	movl	PCB_EBX(%edx),%ebx 
	pushl   %ebx
	movl	PCB_EFLAGS(%edx),%ebx
	movl	PCB_EDX(%edx),%edx
	pushl   %edx
	pushl	%ebx   /* eflags */
			
	popf

	popl %edx
	popl %ebx
	popl %ecx

	popl %eax
	popl %ebp
	popl %esi
	popl %edi

	
	ret
.Ldestroy_switch_to_end:		
	.size destroy_switch_to, .Ldestroy_switch_to_end-switch_to
#else /* KERNEL */
	/***********************
	* EMULATION ONTOP OF UNIX
	*/
	
	/* save current context and activate PARTIAL next context 
	* next context must have been saved by a switch call and not
	* by a interrupt!
	*/
	/* Prototype:	 jint internal_switch_to(ThreadDesc **current, ThreadDesc *to) ;  */
	.text
	.align 8
	.globl	internal_switch_to
	.type	internal_switch_to,@function
internal_switch_to:	
	movl    4(%esp),%ecx

	test	%ecx, %ecx
	je	skip_save

	movl    0(%ecx),%ecx

	movl	THREAD_PPCB(%ecx),%ecx
	movl	(%esp),%eax			/* get eip (switch_to return address) */
	movl	%eax,PCB_EIP(%ecx)
	movl	%ebx,PCB_EBX(%ecx)
	movl	%esp,%eax
	addl	$4, %eax
	movl	%eax, PCB_ESP(%ecx)
	movl	%ebp,PCB_EBP(%ecx)
	movl	%esi,PCB_ESI(%ecx)
	movl	%edi,PCB_EDI(%ecx)

	call	get_currentthread_sigmask			
	
#ifdef PROFILE_EVENT_THREADSWITCH
	pushl	8(%esp)	      /* New thread */
	call	profile_event_threadswitch_to
	popl	%ecx
#endif
#ifdef CPU_USAGE_STATISTICS
	call	profile_cputime
#endif /* CPU_USAGE_STATISTICS */
	
skip_save:	
	/*
	 * Save is done, switch to new thread.
	 */
	movl	8(%esp),%ecx			/* New thread */
	/*
	 * Set curthreads pointer.
	 */
	movl    4(%esp),%edx		
	test	%edx, %edx
	je	skip_save1
	movl    %ecx,(%edx)
skip_save1:	
	/*
	*  Restore context
	*/
	movl	THREAD_PPCB(%ecx),%edx

	movl	PCB_ESP(%edx),%esp
	movl	PCB_EBP(%edx),%ebp
	movl	PCB_EIP(%edx),%ebx
	pushl	%ebx   /* eip is loaded by the ret instruction */

	movl	PCB_EFLAGS(%edx),%ebx
	pushl   %ebx
	movl	PCB_EDI(%edx),%ebx
	pushl   %ebx
	movl	PCB_ESI(%edx),%ebx
	pushl   %ebx
	movl	PCB_EAX(%edx),%ebx
	pushl   %ebx
	
	movl	PCB_ECX(%edx),%ebx 
	pushl   %ebx
	movl	PCB_EBX(%edx),%ebx 
	pushl   %ebx
	movl	PCB_EDX(%edx),%edx
	pushl   %edx

	
	call	set_currentthread_sigmask			

	popl %edx
	popl %ebx
	popl %ecx

	popl %eax
	popl %esi
	popl %edi

	popf /* restore flags */
		
	ret
.globl internal_switch_to_end	
internal_switch_to_end:		
	.size internal_switch_to, internal_switch_to_end-internal_switch_to
	nop


	
	/* activate next context WITHOUT saving current context
	  or even touching current thread struct (may be deallocated) */
	/* Prototype:	 jint destroy_switch_to(ThreadDesc **current, ThreadDesc *to) ;  */
	.text
	.align 8
	.globl	destroy_switch_to
	.type	destroy_switch_to,@function
destroy_switch_to:

#if 0		
#ifdef PROFILE_EVENT_THREADSWITCH
	pushl	8(%esp)	      /* New thread */
	call	profile_event_threadswitch_to
	popl	%ecx
#endif
#endif
#ifdef CPU_USAGE_STATISTICS
	call	profile_cputime
#endif /* CPU_USAGE_STATISTICS */
	
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
	movl	PCB_EDI(%edx),%edi
	movl	PCB_ESI(%edx),%esi
	movl	PCB_EBP(%edx),%ebp
	movl	PCB_EAX(%edx),%eax

	movl	PCB_ESP(%edx),%esp
	movl	PCB_EIP(%edx),%ebx
	pushl	%ebx   /* eip is loaded by the ret instruction */

	movl	PCB_EFLAGS(%edx),%ebx
	pushl   %ebx /* loaded by popf */
	movl	PCB_EDI(%edx),%ebx
	pushl   %ebx
	movl	PCB_ESI(%edx),%ebx
	pushl   %ebx
	movl	PCB_EBP(%edx),%ebx
	pushl   %ebx
	movl	PCB_EAX(%edx),%ebx
	pushl   %ebx
	
	movl	PCB_ECX(%edx),%ebx 
	pushl   %ebx
	movl	PCB_EBX(%edx),%ebx 
	pushl   %ebx
	movl	PCB_EDX(%edx),%edx
	pushl   %edx

	call	set_currentthread_sigmask			

	popl %edx
	popl %ebx
	popl %ecx

	popl %eax
	popl %ebp
	popl %esi
	popl %edi

	popf
		
	ret
	.globl	destroy_switch_to_end
destroy_switch_to_end:		
	.size destroy_switch_to, destroy_switch_to_end-destroy_switch_to
		

#endif


#if 0
	/* save current context:	 context continues at caller frame!!!  
	*/
	/* Prototype:	 jint save_state(ThreadDesc *current) ;  */
	.text
	.align 8
	.globl	save_state
	.type	save_state,@function
save_state:	
	movl    4(%esp),%ecx
	movl	THREAD_PPCB(%ecx),%ecx
	movl	0(%esp),%eax			/* get eip (save_state return address) */
	movl	%eax,PCB_EIP(%ecx)
	movl	%ebx,PCB_EBX(%ecx)
	movl	%esp,%eax
	addl	$4, %eax
	movl	%eax, PCB_ESP(%ecx)
	movl	%ebp,PCB_EBP(%ecx)
	movl	%esi,PCB_ESI(%ecx)
	movl	%edi,PCB_EDI(%ecx)
#ifdef KERNEL	
	movl	%es, PCB_ES(%ecx)
	movl	%gs, PCB_GS(%ecx)
	movl	%fs, PCB_FS(%ecx) 
#endif /* KERNEL */
	pushf
	popl    PCB_EFLAGS(%ecx)
	ret
#endif		
	
	

#ifdef NOPREEMPT	

	/*
	*
	* Software interrupt
	* Called at the end of an atomic section that delayed an interrupt.
	* All callee-save registered are already restored.
	*/	
	.align 4
	.type	 softint,@function
	.globl softint
softint:
	pusha	/* = push eax,ecx,edx,ebx,esp,ebp,esi,edi */
	pushl    %es
	pushl    %fs
	pushl    %gs

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
	
	popl	PCB_EIP(%ecx)  /* pop eip off the stack (return address) */
	movl	%esp,PCB_ESP(%ecx) 
	/* save is done */
	
	jmp  softint_handler
#endif
	
