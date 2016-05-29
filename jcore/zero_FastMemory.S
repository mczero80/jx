
#include "exception_handler.h"
	
#define MEM_ADDR  8
#define MEM_SIZE  4
#define MEM_VALID 4
#define MEM_DZMEM 12

#ifdef REVOKE_USING_CLI
#ifdef KERNEL	
#define FMC_LOCK \
	pushfl	; \
	cli
#define FMC_UNLOCK popfl
#else 
#define FMC_LOCK
#define FMC_UNLOCK		
#endif	
#else
#ifdef REVOKE_USING_SPINLOCK
	#error "not impl"
#else
#define FMC_LOCK
#define FMC_UNLOCK		
#endif
#endif	

.globl vm_get8
.globl vm_get32
.globl vm_getLittleEndian32	

.align 8	
vm_get8:

	FMC_LOCK

#ifndef KERNEL
	/* null pointer check */
	test %eax,%eax
	je mem_throw_null_ptr_exception_get8
#endif

#ifdef MEMORY_REVOKE
	/* valid check */
	movl MEM_DZMEM(%eax), %edi
	cmpl $0, MEM_VALID(%edi)
	je   mem_throw_null_ptr_exception_get8
#endif	
	
	/* range check */
	cmpl MEM_SIZE(%eax),%esi
	jae mem_throw_memory_out_of_range_exception_get8

	movl MEM_ADDR(%eax),%eax
	movsbl 0(%eax,%esi,1), %eax

	FMC_UNLOCK

	ret	

.align 8	
vm_get32:

	FMC_LOCK

#ifndef KERNEL
	/* null pointer check */
	test %eax,%eax
	je mem_throw_null_ptr_exception_get32
#endif

#ifdef MEMORY_REVOKE	
	/* valid check */
	movl MEM_DZMEM(%eax), %edi
	cmpl $0, MEM_VALID(%edi)
	je   mem_throw_null_ptr_exception_get32
#endif	
	
	/* range check */
	movl MEM_SIZE(%eax), %edi
	shrl $2, %edi
	cmpl %edi, %esi
	jae mem_throw_memory_out_of_range_exception_get32

	movl MEM_ADDR(%eax),%eax
	movl 0(%eax,%esi,4), %eax

	FMC_UNLOCK

	ret
	
.align 8	
vm_getLittleEndian32:

	FMC_LOCK

#ifndef KERNEL
	/* null pointer check */
	test %eax,%eax
	je mem_throw_null_ptr_exception_getLE32
#endif

#ifdef MEMORY_REVOKE
	/* valid check */
	movl MEM_DZMEM(%eax), %edi
	cmpl $0, MEM_VALID(%edi)
	je   mem_throw_null_ptr_exception_getLE32
#endif	
	
	/* range check */
	movl MEM_SIZE(%eax), %edi
	addl $4, %edi
	cmpl %edi, %esi
	jae mem_throw_memory_out_of_range_exception_getLE32

	movl MEM_ADDR(%eax),%eax
	movl 0(%eax,%esi,1), %eax

	FMC_UNLOCK

	ret
.align 8
vm_setLittleEndian32:

	FMC_LOCK

#ifndef KERNEL
	/* null pointer check */
	test %eax,%eax
	je mem_throw_null_ptr_exception_setLE32
#endif

#ifdef MEMORY_REVOKE
	/* valid check */
	movl MEM_DZMEM(%eax), %edi
	cmpl $0, MEM_VALID(%edi)
	je   mem_throw_null_ptr_exception_setLE32
#endif	
	
	/* range check */
	movl MEM_SIZE(%eax), %edi
	addl $4, %edi
	cmpl %edi, %esi
	jae mem_throw_memory_out_of_range_exception_setLE32

	movl MEM_ADDR(%eax),%eax
	movl 0(%eax,%esi,1), %eax

	FMC_UNLOCK

	ret

#ifndef KERNEL
mem_throw_null_ptr_exception_get8:	
	/*pushl THROW_NullPointerException*/
	pushl -2
	call exceptionHandler
mem_throw_null_ptr_exception_get32:	
	/*pushl THROW_NullPointerException*/
	pushl -2
	call exceptionHandler
mem_throw_null_ptr_exception_getLE32:	
	/*pushl THROW_NullPointerException*/
	pushl -2
	call exceptionHandler
mem_throw_null_ptr_exception_setLE32:	
	/*pushl THROW_NullPointerException*/
	pushl -2
	call exceptionHandler
#endif
mem_throw_memory_out_of_range_exception_get8:
	/*pushl THROW_MemoryIndexOutOfBounds*/
	pushl -4
	call exceptionHandler
mem_throw_memory_out_of_range_exception_get32:
	/*pushl THROW_MemoryIndexOutOfBounds*/
	pushl -4
	call exceptionHandler
mem_throw_memory_out_of_range_exception_getLE32:
	/*pushl THROW_MemoryIndexOutOfBounds*/
	pushl -4
	call exceptionHandler
mem_throw_memory_out_of_range_exception_setLE32:
	/*pushl THROW_MemoryIndexOutOfBounds*/
	pushl -4
	call exceptionHandler
