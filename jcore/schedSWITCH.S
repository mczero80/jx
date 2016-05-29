#include "thread.h"
#include "segments.h"
	
/* functions used to execute a JAVA method in a worker Thread */
#ifdef JAVASCHEDULER

#if 0  //not maintained anymore
#define SAVE_CONTEXT \
	movl	0(%esp),%eax		/* get eip (return address) */ ; \
	movl	%eax,PCB_EIP(%ecx) ; \
	movl	%ebx,PCB_EBX(%ecx) ;\
	movl	%esp,%eax	; \
	addl	$4, %eax		/* clean up the Stack */ ; \
	movl	%eax, PCB_ESP(%ecx) ; \
	movl	%ebp,PCB_EBP(%ecx) ; \
	movl	%esi,PCB_ESI(%ecx) ; \
	movl	%edi,PCB_EDI(%ecx) ; \
#ifdef KERNEL			; \
/*	movl	%gs, PCB_GS(%ecx)*/ ; \
/*	movl	%fs, PCB_FS(%ecx)*/ ; \
#endif				; \
#ifdef KERNEL			; \
	/* save eflags */	; \
        pushf			; \
	popl    PCB_EFLAGS(%ecx); \
#else				; \
	call	get_currentthread_sigmask ; \
#endif			

	
#define SAVE_CONTEXT2CURRENT \
	call    get_processor_id ; \
	movl	__current(,%eax,4),%ecx	 /* ecx = current[cpu] */ ; \
	movl	THREAD_PPCB(%ecx),%ecx	 /* ecx = current[cpu].context */ ; \
	SAVE_CONTEXT

#endif
	
	
/********************** variants of switch_to *******************************/
#if 1  //unused code	
	/* saves the current context, and activates the next Scheduler
	   the current thread is not added to any Scheduler */
	/* Prototype:	 jint switch_to_nextThread(void);  */
	/* IRQs must be disables when called! */
	.align 8
	.globl	switch_to_nextThread
	.type	switch_to_nextThread,@function
switch_to_nextThread:	
	call save_context2current
	jmp destroy_activate_nextThread
	/* never reached */
	ret
.Lswitch_to_nextThred_end:		
	.size switch_to_nextThread, .Lswitch_to_nextThred_end-switch_to_nextThread
#endif
	
#if 1  //used code
	/* saves the current context, and activates the next Domain
	   the current thread is not added to any Scheduler */
	/* Prototype:	 jint switchfully_to_nextDomain(void);  */
	/* IRQs must be disables when called! */
	.align 8
	.globl	switch_to_nextDomain
	.type	switch_to_nextDomain,@function
switch_to_nextDomain:	
	call save_context2current
	call destroy_activate_nextDomain
	/* never reached */
	ret
.Lswitch_to_nextDomain_end:		
	.size switch_to_nextDomain, .Lswitch_to_nextDomain_end-switch_to_nextDomain
#endif
		
	/* saves the current context, and activates the given Thread */
	/* the current thread is not added tho any Scheduler */
	/* Prototype:	 int switch_to(ThreadDesc *to);  */
	/* IRQs must be disables when called! */
	.align 8
	.globl	Sched_switchTo
	.type	Sched_switchTo,@function
Sched_switchTo:	
	//SAVE_CONTEXT2CURRENT
	call save_context2current
	jmp Sched_destroy_switchTo
	/* never reached */
	ret
.LSched_switchTo_end:		
	.size Sched_switchTo, .LSched_switchTo_end-Sched_switchTo

#ifdef ENABLE_GC
	/* saves the current context, and activates the given Thread */
	/* Prototype:	 void switch_toGCThread(ThreadDesc *GCThread);  */
	/* IRQs must be disables when called! */
	.align 8
	.globl	Sched_switchToGCThread
	.type	Sched_switchToGCThread,@function
Sched_switchToGCThread:	
	//SAVE_CONTEXT2CURRENT
	call save_context2current
	jmp Sched_startGCThread
	/* never reached */
	ret
.LSched_switchToGCThread_end:		
	.size Sched_switchToGCThread, .LSched_switchToGCThread_end-Sched_switchToGCThread
#endif	

/********************** tiny_switch_to  ******************************/

	/* save current context and activate PARTIAL next context */
	/* Prototype:	 jint switch_to(ThreadDesc *to) ;  */
	/* IRQs must be disables when called! */
	.text
	.align 8
	.globl	tiny_switch_to
	.type	tiny_switch_to,@function
tiny_switch_to:
	
	call save_context2current	
	/*
	 * Save is done, switch to new thread.
	 */
	/*	.
		run into tiny_destroy_switch_to 
		.
	*/

	
	/* activate next context WITHOUT saving current context
	  or even touching current thread struct (may be deallocated) */
	/* Prototype:	 void tiny_destroy_switch_to(ThreadDesc *to) ;  */
	.text
	.align 8
	.globl	tiny_destroy_switch_to
	.type	tiny_destroy_switch_to,@function
tiny_destroy_switch_to:	

#ifdef PROFILE_EVENT_THREADSWITCH
	pushl	4(%esp)		/* New thread */
	call	profile_event_threadswitch_to
	popl	%ecx
#endif	
	/*
	 * Set curthreads pointer.
	 */
	call    get_processor_id
	movl	4(%esp),%ecx			/* New thread */
	movl	%ecx,__current(,%eax,4)

	/*
	*  Restore context
	*/
	movl	THREAD_PPCB(%ecx),%edx

//	movl	PCB_ESP(%edx),%esp
//	pushl	PCB_EIP(%edx)  /* eip is loaded by the ret instruction */

	movl	PCB_ESP(%edx),%esp
	
	movl	PCB_EDI(%edx),%edi
	movl	PCB_ESI(%edx),%esi
	movl	PCB_EBP(%edx),%ebp
	movl	PCB_EAX(%edx),%eax    /* restore return value if any*/
	movl	PCB_EBX(%edx),%ebx

	pushl	PCB_EIP(%edx)  /* eip is loaded by the ret instruction */
#ifdef KERNEL	
	pushl	PCB_EFLAGS(%edx)
	popf
#else
	pushl %edi
	pushl %esi
	pushl %ebp
	pushl %eax
	pushl %ebx
	call	set_currentthread_sigmask			
	popl %ebx
	popl %eax
	popl %ebp
	popl %esi
	popl %edi
#endif

	ret
	
.Ltiny_destroy_switch_to_end:		
	.size tiny_switch_to, .Ltiny_destroy_switch_to_end-tiny_switch_to
.Ltiny_destroy_switch_to_end:		
	.size tiny_destroy_switch_to, .Ltiny_destroy_switch_to_end-tiny_destroy_switch_to

#endif  /* JAVASCHEDULER */	



	/* backups the current context and activate PARTIAL next context 
	 *  only the ESP an the IP are activated from the new context 
	 *  the EFlags are not touched ! */
	/* Prototype: if KERNEL	 jint call_java (ContextDesc *backup, ThreadDesc *to); */
	/* Prototype: not KERNEL jint call_java (ThreadDesc *backup, ThreadDesc *to); */
	.text
	.align 8
	.globl	call_java
	.type	call_java,@function
call_java:
#ifdef KERNEL
	movl	4(%esp),%ecx		/* ecx = backup */
#else
	movl	4(%esp),%edx		/* edx = backup */
	movl	THREAD_PPCB(%edx),%ecx	/* ecx = backup.context */
#endif
	call save_context	

	/* Save is done, switch to new thread. */
	/* Set current thread pointer. */
#ifdef PROFILE_EVENT_THREADSWITCH
	pushl	8(%esp)	/* New thread */
	call	profile_event_threadswitch_to
	popl	%ecx
#endif	
	call    get_processor_id
	movl	8(%esp),%ecx			/* New thread */
	movl	%ecx,__current(,%eax,4)

	
	/*  Install new context (only ESP and IP) */
	movl	THREAD_PPCB(%ecx),%edx  	/* ECX contains the new Thread */

	movl	$0,%ebp        /* begin of stack */ 
	movl	PCB_ESP(%edx),%esp
	pushl	PCB_EIP(%edx)  /* eip is loaded by the ret instruction */

	ret
.Lcall_java_end:		
	.size call_java, .Lcall_java_end-call_java

		
	
/*******************************************************************************/
/* functions used by the LowLevel-Scheduler */
		
	/* activates the next Thread WITHOUT saving current context */
	/* sets the current pointer to the activated Thread */
	/* from the new Thread only the ESP, IP and EFLAGS are restored */
	/*    Prototype:	 void destroy_call_java(ThreadDesc *to);  */
	/* IRQs must be disabled when called! */
	.text
	.align 8
	.globl	destroy_call_java
	.type	destroy_call_java,@function
destroy_call_java:	

#ifdef PROFILE_EVENT_THREADSWITCH
	pushl	4(%esp)		/* New thread */
	call	profile_event_threadswitch_to
	popl	%ebx
#endif	

	/* change current pointer */
	call    get_processor_id
	movl	4(%esp),%ebx			/* New thread */
	movl	%ebx, __current(,%eax,4)


	/* Install new context (only ESP, IP and EFlags) */
	movl	THREAD_PPCB(%ebx),%edx

	movl	$0,%ebp         /* begin of stack */ 
	movl	PCB_ESP(%edx),%esp
	pushl	PCB_EIP(%edx)	/* loaded by the ret instruction */
#if KERNEL
	pushl	PCB_EFLAGS(%edx)
	popf
#else
	call	set_currentthread_sigmask			
#endif
	ret
.Ldestroy_call_java_end:		
	.size destroy_call_java, .Ldestroy_call_java_end-destroy_call_java




	/* activates the context WITHOUT saving current context */
	/* #ifdef KERNEL		
		Prototype:	 private jint return_from_java(ThreadDesc *to, ContectDesc *restore) ;	
	   #else
		Prototype:	 private jint return_from_java(ThreadDesc *to, ThreadDesc *restore) ;  
	*/
	.text
	.align 8
	.type	return_form_java,@function
return_from_java:	
	/*
	*  Restore context
	*/
	/* save return value*/
#ifdef KERNEL
	movl	8(%esp),%edx		/* Context to restore */
#else
	movl	8(%esp),%edx		/* ThreadDesc restore */
	movl	THREAD_PPCB(%edx),%edx	/* edx = restore.context */
#endif
	movl	%eax, PCB_EAX(%edx)     /* save return value if any*/

#ifdef KERNEL
	cli
#else
	call disable_irq
#endif
	
#ifdef PROFILE_EVENT_THREADSWITCH
	pushl	4(%esp)	      /* New thread */
	call	profile_event_threadswitch_to
	popl	%ecx
#endif	

	/* Set current thread pointer. */
	call    get_processor_id
	movl	4(%esp),%ecx			/* New thread */
	movl	%ecx,__current(,%eax,4)

#ifdef KERNEL
	movl	8(%esp),%edx		/* Context to restore */
#else
	movl	8(%esp),%ecx		/* ThreadDesc restore */
	movl	THREAD_PPCB(%ecx),%edx	/* edx = restore.context */
#endif
#ifdef KERNEL
//	movl	PCB_GS(%edx),%gs
//	movl	PCB_FS(%edx), %fs 
#endif

	movl	PCB_ESP(%edx),%esp
	movl	PCB_EDI(%edx),%edi
	movl	PCB_ESI(%edx),%esi
	movl	PCB_EBP(%edx),%ebp
	movl	PCB_EAX(%edx),%eax    /* restore return value if any*/

	movl	PCB_EBX(%edx),%ebx

	pushl	PCB_EIP(%edx)  /* eip is loaded by the ret instruction */
#ifdef KERNEL
	pushl	PCB_EFLAGS(%edx)
	popf
#else
	pushl %edi
	pushl %esi
	pushl %ebp
	pushl %eax
	pushl %ebx
	
	pushl %ecx	/* ThreadDesc */
	call	restore_sigmask_from
	popl  %ecx	/* dummy */
	
	popl  %ebx
	popl  %eax
	popl  %ebp
	popl  %esi
	popl  %edi	
#endif
	ret
.Lreturn_from_java_end:		
	.size return_from_java, .Lreturn_from_java_end-return_from_java
	

	/* Prototype:	void return_from_java0(ThreadDesc* next, ContextDesc* restore) ;		*/
	/*		void return_from_java1(long param, ContextDesc* restore, ThreadDesc* next,) ;*/
	/*		void return_from_java2(long param1, long param2, ThreadDesc* next, ContextDesc* restore) ;*/
	/*	
	void return_from_java0(ThreadDesc* next, ContextDesc* restore) {
		return_from_java(next, restore);
	}
	void return_from_java1(long param, ContextDesc* restore, ThreadDesc* next) {
	        return_from_java(next, restore);
	}
	void return_from_java2(long param1, long param2, ContextDesc* restore, ThreadDesc* next) {
	        return_from_java(next, restore);
	}
	!! if not defined(KERNEL):   restore is a ThreadDesc instead of ContextDesc !! 
	*/
	.global	return_from_java0
	.global	return_from_java1
	.global	return_from_java2

	.type	return_from_java0,@function
	.type	return_from_java1,@function
	.type	return_from_java2,@function

	.align 8
return_from_java2:  /* do not destroy EAX (return value)*/
	popl %edx		
return_from_java1:
	popl %edx		
return_from_java0:
	jmp return_from_java
.global return_from_javaX_end
return_from_javaX_end:
	.size return_from_java0, .Lreturn_from_javaX_end-return_from_java0
	.size return_from_java1, .Lreturn_from_javaX_end-return_from_java1
	.size return_from_java2, .Lreturn_from_javaX_end-return_from_java2

	

	
	/* a sub-routine to save the current context */
	/* it is assumed that the saved context should resume at
	   the return-address of the function which called
	   this sub-routine */
	/* Prototype:		void save_current_context(void);  */
	/* could also be	void save_current_context(int ret_addr);  */
	/* IRQs must be disables when called! */
	.align 8
	.type	save_context2current,@function
	.size save_context2current, .Lsave_context_end-save_context2current
.global save_context2current
save_context2current:	
	call    get_processor_id
	movl	__current(,%eax,4),%edx	 /* ecx = current[cpu] */
	movl	THREAD_PPCB(%edx),%ecx	 /* ecx = current[cpu].context */
	
	.type	save_context,@function
save_context:
	/* ECX contains the ContextDesc */
	/* EDX MAY contain the ThreadDesc (if called from save_context2current) */
	movl	4(%esp),%eax		/* get eip (return address) */
	movl	%eax,PCB_EIP(%ecx)
	movl	%ebx,PCB_EBX(%ecx)
	movl	%esp,%eax
	addl	$8, %eax		/* clean up the Stack */
	movl	%eax, PCB_ESP(%ecx)
	movl	%ebp,PCB_EBP(%ecx)
	movl	%esi,PCB_ESI(%ecx)
	movl	%edi,PCB_EDI(%ecx)
#ifdef KERNEL	
//	movl	%gs, PCB_GS(%ecx)
//	movl	%fs, PCB_FS(%ecx) 
#endif
#ifdef KERNEL
	/* save eflags */
        pushf 
	popl    PCB_EFLAGS(%ecx)
#else
	push	%edx
	call	save_sigmask_to
	popl	%edx
#endif	
	ret	
.Lsave_context_end:		
	.size save_context, .Lsave_context_end-save_context

					
