#define SAVE_EFLAGS	1
#define MASK_IF_FLAG	0

/*
 * Low level code to:	
 *  - call native code 
 *  - switch stacks 
 */
#include "context.h"
#include "thread.h"
#include "intr.h"
	
	.text
	.align 16
.globl activate_thread_desc	
.globl get_thread_desc
.globl msetjmp
.globl mlongjmp
.globl send_msg1
.globl yield
.globl jxbcopy
	
.globl get_processor_id
.globl cas				
	

	/* Prototyp:	 jxbcopy(jint from, jint to, jint count) */
	   .align 4
ENTRY(jxbcopy)	
	pushl	%ebp
	movl	%esp,%ebp
	pushl	%edi
	pushl	%esi
	movl	14(%ebp),%edx
	movl	12(%ebp),%edi
	movl	8(%ebp),%esi
/* move longs */
0:	cld
	movl	%edx,%ecx
	sarl	$2,%ecx
	js	1f
	rep
	movsl
/* move bytes */
	movl	%edx,%ecx
	andl	$3,%ecx
	rep
	movsb
1:
	popl	%esi
	popl	%edi
	popl   %ebp
	ret	

	
	

	/* void jxbytecpy(char* source, char* target, jint nbytes) */
	.align 4
ENTRY(IN_jxbytecpy)
	pushl %ebp
	movl %esp,%ebp
        movl 8(%ebp),%esi /* source */
        movl 12(%ebp),%edi /* target */
        movl 16(%ebp),%ecx /* nbytes */
        cld
        rep
	movsb
	movl %ebp,%esp
	popl %ebp
	ret
.LIN_jxbytecpy_end:		
	.size IN_jxbytecpy, .LIN_jxbytecpy_end-IN_jxbytecpy

	/* void jxwordcpy(u4_t* source, u4_t* target, jint nwords) */
	.align 4
ENTRY(jxwordcpy)	
	pushl %ebp
	movl %esp,%ebp
        movl 8(%ebp),%esi /* source */
        movl 12(%ebp),%edi /* target */
        movl 16(%ebp),%ecx /* nwords */
        cld
        rep
	movsl
	movl %ebp,%esp
	popl %ebp
	ret
.Ljxwordcpy_end:		
	.size jxwordcpy, .Ljxwordcpy_end-jxwordcpy
		
	/* void jxmemset0(char *s, jbyte value, jint nbytes) */
	.align 4
ENTRY(jxmemset)	
	push   %ebp
	mov    %esp,%ebp
	push   %edi
	push   %esi
	mov    0x8(%ebp),%edi
	mov    0x10(%ebp),%esi
	movzbl 0xc(%ebp),%eax
	cld
	cmp    $0xb,%esi
	jbe    1f
	mov    %eax,%edx
	shl    $0x8,%edx
	or     %eax,%edx
	mov    %edi,%ecx
	mov    %edx,%eax
	neg    %ecx
	shl    $0x10,%edx
	or     %eax,%edx
	and    $0x3,%ecx
	mov    %edx,%eax
	sub    %ecx,%esi
	repz stos %al,%es:(%edi)
	mov    %esi,%ecx
	shr    $0x2,%ecx
	repz stos %eax,%es:(%edi)
	and    $0x3,%esi
1:	mov    %esi,%ecx
	repz stos %al,%es:(%edi)
	pop    %esi
	mov    0x8(%ebp),%eax
	pop    %edi
	pop    %ebp
	ret

	/* void jxmemset0(char *s, jbyte value, jint nbytes) */
	.align 4
ENTRY(jxwmemset)	
	push   %ebp
	mov    %esp,%ebp
	push   %ecx
	push	%edi
	movl   	0x8(%ebp),%edi
	movl 	0xc(%ebp),%eax
	movl	0xc(%ebp),%ecx
	andl	$0xffff, %eax
	andl	$0xffff, %ecx
	shll	$16, %ecx
	orl		%ecx, %eax	
	movl   	0x10(%ebp),%ecx
	
	cld
	shrl	$1, %ecx
	jnc		1f
	stosb	
1:
	shrl	$1, %ecx
	jnc		2f
	stosw	
2:
	rep stosl
	
	pop		%edi
	pop		%ecx
	pop		%ebp
	ret
		
	
	/* int get_processor_id(void) ;	only EAX is used!! */
ENTRY(get_processor_id)
#ifdef SMP
	cmpl $0,smp_found
	jne .read_id
	movl $0,%eax
	ret
	.p2align 4,,7
.read_id:
	movl lapic_addr,%eax
	movl 0x20(%eax),%eax
	shrl $24,%eax
	andl $0xf,%eax
	ret
#else
	movl $0,%eax
	ret
#endif
.Lget_processor_id_end:
	.size	 get_processor_id, .Lget_processor_id_end-get_processor_id

#if 0
/* Prototype:
 *	int cas(u4_t *var, u4_t old, u4_t new) ;
	returns 0 if failed 1 on success
*/
.align 4
cas:
	movl  4(%esp),%ecx	/* var */    
 	movl  8(%esp),%eax	/* old */
	movl 12(%esp),%edx	/* new */
        lock;
        cmpxchg %edx, (%ecx);
        jne .cas_failed
.cas_successful:	
	mov $1, %eax
        ret	
.cas_failed:	
	xorl %eax,%eax	
	ret
.cas_end:
	.size	 cas,.cas_end-cas
#endif


ENTRY(disable_cache)
        pushf                   
        pushl    %eax             
        cli                     
        movl     %cr0, %eax      
        /*orl      $0x40000000, %eax*/
        orl      $0x60000000, %eax
        movl     %eax, %cr0       
        wbinvd                  
	popl     %eax             
        popf                    
        ret                    
ENDENTRY(disable_cache)

ENTRY(enable_cache)
        pushf                   
        pushl    %eax             
        cli                     
        wbinvd                  
	movl  %cr0, %eax
        andl  $0x9fffffff, %eax
        movl  %eax, %cr0
        wbinvd                  
	popl     %eax             
        popf                    
        ret                    
ENDENTRY(enable_cache)
