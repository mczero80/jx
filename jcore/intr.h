#ifndef INTR_H
#define INTR_H

#ifndef ASSEMBLER
#include "types.h"

/* must be consistent with assenmbler code in debug_ex in irq.c ! */
struct irqcontext {
	u4_t gs;
	u4_t fs;
	u4_t es;

	u4_t edi;
	u4_t esi;
	u4_t ebp;
	u4_t esp;
	u4_t ebx;
	u4_t edx;
	u4_t ecx;
	u4_t eax;

	u4_t irq_nr;

	u4_t eip;
	u4_t cs;
	u4_t eflags;
};

struct irqcontext_timer {
	u4_t gs;
	u4_t fs;
	u4_t es;

	u4_t edi;
	u4_t esi;
	u4_t ebp;
	u4_t esp;
	u4_t ebx;
	u4_t edx;
	u4_t ecx;
	u4_t eax;

	u4_t eip;
	u4_t cs;
	u4_t eflags;
};

#else

/* the SAVE macro is used without the RESTORE macro */
/*	so DO NOT MODIFY it */
#define SAVE \
	pusha			  ;\
	pushl    %es              ;\
	pushl    %fs              ;\
	pushl    %gs              ;\

#define RESTORE \
	popl     %gs;\
        popl     %fs;\
        popl     %es;\
        popa	;\

#endif				/* !ASSEMBLER */

#endif				/* INTR_H */
