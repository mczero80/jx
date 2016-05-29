/********************************************************************************
 * Support for non-preemptable code.
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifdef NOPREEMPT

#include "all.h"

static u4_t global_atomic_code_original = 0;
static u4_t global_atomic_code_original_end = 0;
static u4_t global_atomic_code_original_recent = 0;
static u4_t global_atomic_code_original_realstart = 0;
static u4_t global_atomic_code_shadow_recent = 0;
static u4_t global_atomic_code_shadow = 0;
static u4_t global_atomic_code_shadow_end = 0;
static u4_t global_atomic_code_shadow_realstart = 0;

void nopreempt_init()
{
	global_atomic_code_original = (u4_t) jxmalloc_align(4096, 1024, &global_atomic_code_original_realstart MEMTYPE_OTHER);
	global_atomic_code_original_end = global_atomic_code_original + 4096;
	global_atomic_code_original_recent = global_atomic_code_original;
	global_atomic_code_shadow = (u4_t) jxmalloc_align(4096, 1024, &global_atomic_code_shadow_realstart MEMTYPE_OTHER);
	global_atomic_code_shadow_end = global_atomic_code_shadow + 4096;
	global_atomic_code_shadow_recent = global_atomic_code_shadow;
}

extern void commit_atomic_section();

/*
 * Requirements for atomic code:
 * - no relative jumps to the outside world 
 * - return at end of code (no returns inside code block)
 * This function is not reentrant!
 */
code_t nopreempt_register(code_t original, u4_t size)
{
	u1_t *c;
	ASSERT(global_atomic_code_original != NULL);

#ifdef DEBUG
	if (size == -1)
		sys_panic("invalid symbols.h! recompile with make clean ; make ...");
#endif

	ASSERT(global_atomic_code_original_end - global_atomic_code_original_recent > size + 4);	/* ret -> jmp */
	memcpy(global_atomic_code_original_recent, original, size);
	memcpy(global_atomic_code_shadow_recent, original, size);
	printf("Copied code %p ... %p (%ld bytes) with shadow at %p .. %p\n", original, global_atomic_code_original_recent, size,
	       global_atomic_code_shadow_recent, global_atomic_code_shadow_recent + size);
	c = global_atomic_code_shadow_recent + size - 1;
	*c++ = 0xe9;
	*((u4_t *) c) = (u1_t *) softint - (c + 4);
	c = global_atomic_code_original_recent;

	size = ALIGN4(size);

	global_atomic_code_original_recent += size + 4;
	global_atomic_code_shadow_recent += size + 4;
	return c;
}

int nopreempt_check(u4_t eip)
{
	return ((eip >= (u4_t) global_atomic_code_original)
		&& (eip < (u4_t) global_atomic_code_original_end));
}

u4_t nopreempt_adjust(u4_t eip)
{
	u4_t ret = eip - (u4_t) global_atomic_code_original + (u4_t) global_atomic_code_shadow;
	ASSERT((ret >= (u4_t) global_atomic_code_shadow)
	       && (ret < (u4_t) global_atomic_code_shadow_end));
	return ret;
}

#endif
