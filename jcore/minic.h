#ifndef MINIC_H
#define MINIC_H

#include "types.h"

struct _fpreg {
	unsigned short significand[4];
	unsigned short exponent;
};

struct _fpstate {
	unsigned long cw, sw, tag, ipoff, cssel, dataoff, datasel;
	struct _fpreg _st[8];
	unsigned long status;
};

struct sigcontext {
	unsigned short gs, __gsh;
	unsigned short fs, __fsh;
	unsigned short es, __esh;
	unsigned short ds, __dsh;
	unsigned long edi;
	unsigned long esi;
	unsigned long ebp;
	unsigned long esp;
	unsigned long ebx;
	unsigned long edx;
	unsigned long ecx;
	unsigned long eax;
	unsigned long trapno;
	unsigned long err;
	unsigned long eip;
	unsigned short cs, __csh;
	unsigned long eflags;
	unsigned long esp_at_signal;
	unsigned short ss, __ssh;
	struct _fpstate *fpstate;
	unsigned long oldmask;
	unsigned long cr2;
};


typedef int bool_t;


#define __va_size(type) ((sizeof(type)+3) & ~0x3)

typedef char *va_list;

#define	va_start(pvar, lastarg)		   	((pvar) = (char*)(void*)&(lastarg) + __va_size(lastarg))
#define	va_end(pvar)
#define	va_arg(pvar,type)			\
	((pvar) += __va_size(type),		\
	 *((type *)((pvar) - __va_size(type))))

#ifdef LOG_PRINTF
extern int printf2mem;
#endif

// Prototypes
struct multiboot_module *multiboot_get_module();
int strncmp(const char *s1, const char *s2, size_t n);
int vprintf(const char *fmt, va_list args);
char *strncpy(char *to, const char *from, size_t count);

#ifdef NO_PRINTF
static inline int printf(const char *fmt, ...)
{
}
#else
int printf(const char *fmt, ...);
#endif

#endif
