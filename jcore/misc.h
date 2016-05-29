#ifndef MISC_H
#define MISC_H

#ifndef ASSEMBLER


#ifdef DEBUG
#define ASSERT(x) if (!(x)) {printf("\"%s\", line %d: Assertion failed in file: %s.\n", __FILE__, __LINE__, #x); asm("int $3");}
#else
#define ASSERT(x)
#endif

void sys_panic(char *msg, ...);

#define MAX(a,b) (((a)<(b))?(b):(a))

#ifdef KERNEL
int dprintf(const char *fmt, ...);
#else
#define dprintf printf
#endif
static inline unsigned getEFlags()
{
	unsigned eflags;
	asm volatile ("pushfl ; popl %0":"=r" (eflags));
	return eflags;
}

/*
static inline void setEFlags(unsigned eflags)
{
	asm volatile("
		pushl %0
		popfl" : : "r" (eflags));
}
*/

#define setEFlags(eflags) asm volatile("pushl %0 ; popfl" : : "r" (eflags))


static inline int test_and_set_bit(int nr, volatile long *lock)
{
	int oldbit;
	asm volatile ("lock;"
		      "tsl %2,%1;" "bbl %0,%0;":"=r" (oldbit), "=m"(*lock)
		      :"Ir"(nr));
	return oldbit;
}
static inline void atomic_inc(volatile int *v)
{
	asm volatile ("lock;" "incl %0;":"=m" (*v)
		      :"m"(*v));
}

#define get_cr4() \
    ({ \
	register unsigned int _temp__; \
	asm volatile("mov %%cr4, %0" : "=r" (_temp__)); \
	_temp__; \
    })
#define set_cr4(value) \
    ({ \
	register unsigned int _temp__ = (value); \
	asm volatile("mov %0, %%cr4" : : "r" (_temp__)); \
     })

#define set_idt(pseudo_desc) \
    ({ \
	asm volatile("lidt %0" : : "m" ((pseudo_desc)->limit)); \
    })

#define SLOW_DOWN_IO __asm__ __volatile__("outb %al,$0x80")

static inline unsigned int inb(unsigned short int port)
{
	unsigned char ret;
	asm volatile ("inb %1,%0": "=a" (ret): "d"(port));
	return ret;
}

static inline void outb(unsigned short int port, unsigned char val)
{
	asm volatile ("outb %0,%1"::"a" (val), "d"(port));
}

#define get_esp() \
    ({ \
	register unsigned int _temp__; \
	asm("movl %%esp, %0" : "=r" (_temp__)); \
	_temp__; \
    })

#define get_eflags() \
    ({ \
	register unsigned int _temp__; \
	asm volatile("pushfl; popl %0" : "=r" (_temp__)); \
	_temp__; \
    })



/*
 * CR0
 */
#define	CR0_PG	0x80000000	/*       enable paging */
#define	CR0_CD	0x40000000	/* i486: cache disable */
#define	CR0_NW	0x20000000	/* i486: no write-through */
#define	CR0_AM	0x00040000	/* i486: alignment check mask */
#define	CR0_WP	0x00010000	/* i486: write-protect kernel access */
#define	CR0_NE	0x00000020	/* i486: handle numeric exceptions */
#define	CR0_ET	0x00000010	/*       extension type is 80387 */
					/*       (not official) */
#define	CR0_TS	0x00000008	/*       task switch */
#define	CR0_EM	0x00000004	/*       emulate coprocessor */
#define	CR0_MP	0x00000002	/*       monitor coprocessor */
#define	CR0_PE	0x00000001	/*       enable protected mode */

/*
 * CR4
 */
#define CR4_VME	0x00000001	/* enable virtual intrs in v86 mode */
#define CR4_PVI	0x00000002	/* enable virtual intrs in pmode */
#define CR4_TSD	0x00000004	/* disable RDTSC in user mode */
#define CR4_DE	0x00000008	/* debug extensions (I/O breakpoints) */
#define CR4_PSE	0x00000010	/* page size extensions */
#define CR4_PGE	0x00000020	/* page global extensions */
#define CR4_MCE	0x00000040	/* machine check exception */
#define CR4_PCE	0x00000100	/* enable read perf counter instr */


#include "types.h"


char *strcat(char *dest, const char *src);
void sprintnum(char *s, u4_t u, int base);


void setTimer();

#define CYCL2NANOS(c)  ((u4_t)((((c)*(u8_t)1000))/(u8_t)CPU_MHZ))
#define CYCL2MICROS(c)  ((u4_t)((c)/(u8_t)CPU_MHZ))
#define CYCL2MILLIS(c)  ((u4_t)((c)/(u8_t)CPU_MHZ)/(u8_t)1000)

#endif				/* ASSEMBLER */

#endif				/* MISC_H */
