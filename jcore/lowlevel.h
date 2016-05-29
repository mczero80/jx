#ifndef LOWLEVEL_H
#define LOWLEVEL_H

#include "thread.h"

#ifndef ASSEMBLER

jint callnative_special(jint * params, ObjectDesc * obj, code_t f,
			jint params_size);
jint callnative_special_portal(jint * params, ObjectDesc * obj, code_t f,
			       jint params_size);
jint callnative_static(jint * params, code_t f, jint params_size);
void callnative_handler(u4_t * ebp, u4_t * sp, char *addr);

void activate_thread_desc(jint number);
jint get_thread_desc();

/*
void switchAndUnlock(ContextDesc *context, spinlock_t *lock);
void switchTo(ContextDesc *context);
*/

void yield(ThreadDesc ** current);
jint destroy_switch_to(ThreadDesc ** current, ThreadDesc * to);
void softint();

/* compare and swap */
#if 1
static inline u4_t cas(u4_t * var, u4_t old, u4_t new)
{
	int result;
	asm volatile ("   lock;"
		      "   cmpxchg %%edx, (%%ecx);"
		      "   jne 2f;"
		      "1: mov $1, %%eax;"
		      "   jmp 3f;"
		      "2: xor %%eax,%%eax;" "3:":"=a" ((result))
		      :"c"((var)), "a"((old)), "d"((new))
		      :"cc", "memory");
	return result;
}
#else
u4_t cas(u4_t * var, u4_t old, u4_t new);
#endif

static inline int fetchAndStore(int *var, int new)
{
	int result = *var;
	while (!cas((u4_t *) var, (u4_t) result, (u4_t) new))
		result = *var;
	return result;
}

/* get instruction pointer of caller */
#define getCallerIP() \
    ({ \
	register unsigned int _temp__; \
	asm volatile("movl %%ebp, %%eax; addl $4, %%eax; movl (%%eax), %0" : "=r" (_temp__)); \
	_temp__; \
    })

/* get instruction pointer of caller of caller */
#define getCallerCallerIP() \
    ({ \
	register unsigned int _temp__; \
	asm volatile("movl %%ebp, %%eax; movl (%%eax), %%eax; addl $4, %%eax; movl (%%eax), %0" : "=r" (_temp__)); \
	_temp__; \
    })

/* get instruction pointer of caller of caller of caller */
#define getCallerCallerCallerIP() \
    ({ \
	register unsigned int _temp__; \
	asm volatile("movl %%ebp, %%eax; movl (%%eax), %%eax; movl (%%eax), %%eax; addl $4, %%eax; movl (%%eax), %0" : "=r" (_temp__)); \
	_temp__; \
    })



#define ALIGN4(x) ((((u4_t)x)+3)&~3)

#endif				/* ASSEMBLER */

#endif				/* LOWLEVEL_H */
