#ifndef MONITOR_H
#define MONITOR_H

#include "thread.h"
#include "context.h"

void checkStackTrace(ThreadDesc * thread, u4_t * base);
void printStackTrace(char *prefix, ThreadDesc * thread, u4_t * base);
void printNStackTrace(char *prefix, ThreadDesc * thread, u4_t * base,
		      int n);
#ifdef KERNEL
void printTraceFromCtx(char *prefix, ThreadDesc * thread,
		       struct irqcontext *ctx);
void printTraceFromStoredCtx(char *prefix, ThreadDesc * thread,
			     unsigned long *ctx);
void monitor(struct irqcontext_timer *ctx);
#else
void printTraceFromCtx(char *prefix, ThreadDesc * thread,
		       struct sigcontext *ctx);
void printTraceFromStoredCtx(char *prefix, ThreadDesc * thread,
			     unsigned long *ctx);
void monitor(struct sigcontext *ctx);
#endif
void dump_data(ObjectDesc * o);
void dumpThreadInfo(ThreadDesc * t);

void printHeapUsage(DomainDesc * d);

void register_command(char *name, ObjectHandle cmd, DomainDesc * domain);

#endif				/* MONITOR_H */
