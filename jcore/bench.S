#include "context.h"
#include "thread.h"
#include "intr.h"

#ifdef MICROBENCHMARKS

	
#define BENCH_PRO(name) \
	.text			; \
	.align 16		; \
	.type	name,@function	; \
	.globl	name		; \
name:			; \
	pushl	%ebp		; \
	movl	%esp,%ebp	; \
	pushl	%ebx		; \
	pushl	%esi		; \
	pushl	%edi		; \
	rdtsc			; \
	movl	%edx, %edi	; \
	movl	%eax, %esi

#define BENCH_EPI\
	rdtsc			; \
	mov    0x8(%ebp),%ecx	; \
	movl   %edi,0x0(%ecx)	; \
	movl   %esi,0x4(%ecx)	; \
	movl   %edx,0x8(%ecx)	; \
	movl   %eax,0xc(%ecx)	; \
	popl	%edi		; \
	popl	%esi		; \
	popl	%ebx		; \
	leave			; \
	ret
		

	/* these registers can be used:	 %eax, %ebx, %ecx, %edx */
	
BENCH_PRO(bench_empty)
BENCH_EPI

BENCH_PRO(bench_store)
	movl	$1, %eax
	movl	$1, %eax
	movl	$1, %eax
	movl	$1, %eax
	movl	$1, %eax
	movl	$1, %eax
	movl	$1, %eax
	movl	$1, %eax
	movl	$1, %eax
	movl	$1, %eax
BENCH_EPI

BENCH_PRO(bench_store1)
	movl	benchmem, %eax
	movl	$1, %ebx
	movl	%ebx, (%eax)
BENCH_EPI

BENCH_PRO(bench_load1)
	movl	benchmem, %eax
	movl	(%eax), %ebx
BENCH_EPI
			

#endif /* MICROBENCHMARKS	 */
	
