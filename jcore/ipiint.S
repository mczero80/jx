#ifdef SMP
	
#include "irq.h"
#include "intr.h"
	
	.text


.global call_function_ipi
	
 
	/*
	*
	* Inter Processor Interrupt
	*
	*/	
	.align 4
	.type	 ipiint,@function
//	.globl ipiint
ipiint:
	/* SAVE	is done */

	popl %ecx /* ecx now contains the interrupt handler addr. */

	call  *%ecx		
	call ack_APIC_irq

	popl   %ebx /* ebx now contains the interrupt number */

#ifdef IRQ_STATISTICS	
	call get_processor_id
	
	/* ebx must contain irq */
	/* eax must contain cpu id*/
	/* iprocessed[eax][ebx]++; */
	leal 0(,%ebx,4),%ebx		/* irq *4 */
	sall $10,%eax			/* cpuID *256 *4 */
	incl iprocessed(%eax,%ebx)
#endif	 

	/* return from interrupt */
	RESTORE
	iret
	
.Lipiint_end:		
	.size ipiint, .Lipiint_end-ipiint



	
	.align 4
call_function_ipi:
     	SAVE	/* = push eax,ecx,edx,ebx,esp,ebp,esi,edi-es,fs,gs */
    	pushl	$CALL_FUNCTION_IRQNR	
	pushl	$call_function_ipi_handler	
	jmp	ipiint

#endif
