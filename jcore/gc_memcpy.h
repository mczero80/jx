#ifndef GC_MEMCPY_H
#define GC_MEMCPY_H

#ifdef GC_FAST_MEMCPY
#define gc_memcpy(a,b,c) gc_fast_memcpy(a,b,c)
void gc_fast_memcpy(void *b, void *a, unsigned int s);
#else				/*  GC_FAST_MEMCPY */
#define gc_memcpy(a,b,c) memcpy(a,b,c)
#endif				/*  GC_FAST_MEMCPY */

#endif				/* GC_MEMCPY_H */
