
#include "config.h"

#ifdef PROFILE

#ifndef KERNEL

#include <stdlib.h>
#include <stdarg.h>
#include <stdio.h>
#include <unistd.h>

#else

#include "minic.h"
#define FILE void
int fprintf(void *fd, const char *fmt, ...)
{
	va_list args;
	int err;

	va_start(args, fmt);
	err = vprintf(fmt, args);
	va_end(args);

	return err;
}

#endif				/* KERNEL */

#include "profile.h"
#include "thread.h"
#include "load.h"

#ifndef NULL
#define NULL ((void*)0)
#endif

/* extra clocks per profile call, when compiled with gcc */
/* 83-111 clocks */
//#define STATIC_DRIFT 83
/* 7 - */
//#define STATIC_DRIFT_IN  12
/* x-75 */
//#define STATIC_DRIFT_OUT 50
//#define STATIC_DRIFT_OUT 10 

/* MHZ */
#ifdef CPU_MHZ
static long cpu_freq = CPU_MHZ;
#else
static long cpu_freq = 500;
#endif
static long stack_depth = 0;

/* extra clocks per profile call, when compiled with gcc */
/* 83-111 clocks */
/* 7 - */
long profile_drift_in = 12;
/* x-75 */
long profile_drift_out = 10;

/*
static spinlock_t mem_lock = 0;
static spinlock_t call_lock = 0;
#define DBG_LOCK(_lock_) spinlock(_lock_);
#define DBG_FREE(_lock_) spinfree(_lock_);
*/
#define DBG_LOCK(_lock_)
#define DBG_FREE(_lock_)
//#define DBG_MSG(_txt_) printf _txt_ ;
#define DBG_MSG(_txt_)

typedef struct method_s {
	MethodDesc *desc;
	ClassDesc *class;

	unsigned long calls;
	unsigned long long total;
	unsigned long long method;
	unsigned long long average;

#ifdef PROF_EXTRAS
	unsigned long long rtotal;
#endif

	int nr;
	struct method_s *ndx;
	struct method_s *next;
} MethodEntry;

typedef int (*cmp_fkt_t) (MethodEntry * a, MethodEntry * b);

int realloc_stack(ProfileDesc * p);
void clear_hash(ProfileDesc * p);

void free_entry_lst(ProfileEntry * lst);
ProfileEntry *create_entry_lst(ProfileEntry * lst, ProfileDesc * p);

MethodEntry *new_method_lst(ProfileEntry * lst);
MethodEntry *create_method_entry(ProfileEntry * p_lst, MethodEntry * m_lst, MethodDesc * method);
MethodEntry *new_method_entry(MethodDesc * method);
int cmp_method_time(MethodEntry * a, MethodEntry * b);
int cmp_average_time(MethodEntry * a, MethodEntry * b);
int cmp_total_time(MethodEntry * a, MethodEntry * b);
MethodEntry *all_methods(MethodEntry * lst);
MethodEntry *sort_methods(MethodEntry * lst, cmp_fkt_t cmp_fkt);
void free_method_lst(MethodEntry * lst);
void list_methods(MethodEntry * lst);
void show_method(ProfileEntry *, MethodEntry *, MethodEntry *, const char *);
void method_dump(ProfileEntry *, MethodEntry *, const char *);
void dump_digraph(ProfileEntry * p_lst, MethodEntry * m_lst, MethodEntry * i_lst, const char *);
MethodEntry *find_method(MethodEntry * lst, MethodDesc * desc);
char *method_name(MethodEntry * e);

#define entry_malloc(_desc_) jxmalloc(sizeof(ProfileEntry))
#define entry_free(_addr_)   jxfree(_addr_, (sizeof(ProfileEntry)  MEMTYPE_PROFILING)

long long *profile_register(unsigned long long t1, unsigned long long t2, void *caller, MethodDesc * callee);

void profile_init()
{

}

				    /*void profile_shell(DomainDesc *domain) */
void profile_shell(void *d)
{
	DomainDesc *domain;
	ThreadDesc *thr;
	ProfileEntry *p_lst;
	MethodEntry *m_lst;
	MethodEntry *i_lst;
	char line[80];
	int i;

	domain = d;

	printf("scan profile data of domain %s\n", domain->domainName);

	p_lst = NULL;
	for (thr = domain->threads; thr != NULL; thr = thr->nextInDomain) {
#ifdef USE_QMAGIC
		if (thr->magic != MAGIC_THREAD)
			break;
#endif
		if (thr->profile == NULL) {
			printf("Thread %d.%d has no profiling data\n", (long) thr->domain->id, thr->id);
			continue;
		}
		if ((p_lst = create_entry_lst(p_lst, thr->profile)) == NULL) {
			printf("Thread %d.%d no profiling data found\n", (long) thr->domain->id, thr->id);
			continue;
		}
	}

	printf("make method list\n");

	if ((m_lst = new_method_lst(p_lst)) == NULL) {
		printf("no data profiling not possible\n");
		return;
	}

	printf("sort method list\n");

	i_lst = NULL;
	i_lst = sort_methods(m_lst, cmp_method_time);

	while (1) {
		printf("\nProfiler (%s): ", domain->domainName);
#ifndef KERNEL
		fflush(stdout);
#endif
		readline(line, 80);
		if (strcmp("quit", line) == 0 || strcmp("cont", line) == 0) {
			free_entry_lst(p_lst);
			free_method_lst(m_lst);
			return;
#ifndef KERNEL
		} else if (strcmp("stop", line) == 0) {
			exit(0);
#endif
		} else if (strcmp("sort total", line) == 0) {
			i_lst = sort_methods(m_lst, cmp_total_time);
			list_methods(i_lst);
		} else if (strcmp("sort method", line) == 0) {
			i_lst = sort_methods(m_lst, cmp_method_time);
			list_methods(i_lst);
		} else if (strcmp("sort average", line) == 0) {
			i_lst = sort_methods(m_lst, cmp_average_time);
			list_methods(i_lst);
		} else if (strcmp("list", line) == 0) {
			list_methods(i_lst);
		} else if (strncmp("show", line, 4) == 0) {
			show_method(p_lst, m_lst, i_lst, line);
		} else if (strncmp("info", line, 4) == 0) {
			show_method(p_lst, m_lst, i_lst, line);
		} else if ((strcmp("n", line) == 0) || (strcmp("next", line) == 0)) {
			int i;
			for (i = 0; (i_lst != NULL) && (i < 10); i_lst = i_lst->ndx)
				i++;
			list_methods(i_lst);
#ifdef PROF_EXTRAS
		} else if (strcmp("rlist", line) == 0) {
			MethodEntry *ptr;
			printf("calls method   class    total    average    in method  signature\n");
			for (ptr = m_lst; ptr != NULL; ptr = ptr->next) {
				printf("%5lu 0x%lx 0x%lx ", ptr->calls, ptr->desc, ptr->class);
				printf("%12lu %12lu %12lu ", (unsigned long) ptr->total, (unsigned long) ptr->average,
				       (unsigned long) ptr->method);
				printf("%s\n", method_name(ptr));
			}
		} else if (strcmp("rdump", line) == 0) {
			ProfileEntry *ptr;
			for (ptr = p_lst; ptr != NULL; ptr = ptr->next) {
				printf("%lu 0x%lx 0x%lx %lu\n", ptr->count, ptr->callee, ptr->cmdesc, (unsigned long) ptr->time);
			}
#endif
		} else if (strncmp("digraph", line, 7) == 0) {
			dump_digraph(p_lst, m_lst, i_lst, line);
		} else if (strcmp("freq", line) == 0) {
			printf("freq = %lu MHz\n", cpu_freq);
		} else if (strncmp("freq ", line, 5) == 0) {
			cpu_freq = strtol(&line[4], NULL, 10);
			printf("freq = %lu MHz\n", cpu_freq);
		} else if (strncmp("dump ", line, 5) == 0) {
			method_dump(p_lst, m_lst, line);
		} else if (strcmp("dump", line) == 0) {
			method_dump(p_lst, m_lst, "dump 1");
		} else if (strcmp("dumpfile", line) == 0) {
			method_dump(p_lst, m_lst, "dump 1");
		} else if (strcmp("dumptty", line) == 0) {
			method_dump(p_lst, m_lst, "dump 1");
		} else if (strcmp("pdump", line) == 0) {
			profile_dump(curthr());
		} else if (strcmp("stat", line) == 0) {
			printf("\ncpu frequence : %i MHz\n", (int) cpu_freq);
			printf("static drift    in:%lu out:%lu clocks\n\n", profile_drift_in, profile_drift_out);
			for (thr = domain->threads; thr != NULL; thr = thr->nextInDomain) {
#ifdef USE_QMAGIC
				if (thr->magic != MAGIC_THREAD) {
					printf("Thread : %p ??? \n", thr);
					break;
				}
#endif
				printf("Thread : %d.%d %s\n", thr->domain->id, thr->id, thr->name);

				if (thr->profile == NULL) {
					printf(" no profiling aktiv!\n");
				} else {
					printf("profile entries : %10lu\n", thr->profile->ecount);
#ifdef PROF_EXTRAS
					if (cpu_freq == 0) {
						printf("time in other threads        : %10lu clk\n",
						       (long) (thr->profile->total_dtime));
					} else {
						printf("time in other threads        : %10lu us\n",
						       (long) (thr->profile->total_dtime / cpu_freq));
					}
					printf("number of errors (wrong dtime): %10lu ", thr->profile->errors);
					printf("number of t1>t2               : %10lu\n", thr->profile->exception_err);
					printf("number of drift overruns      : %10lu ", thr->profile->drift_overruns);
					printf("number of thread switches     : %10lu\n", thr->profile->nswitch);
					printf("max portal time               : %10lu ", thr->profile->total_dtime2);
					printf("number of portal calls        : %10lu\n", thr->profile->nportal);
					printf("number of exceptions          : %10lu ", thr->profile->exceptions);
					if (cpu_freq == 0) {
						printf("time in exception handler     : %10lu clk\n",
						       (long) (thr->profile->exception_time));
					} else {
						printf("time in exception handler     : %10lu us\n",
						       (long) (thr->profile->exception_time / cpu_freq));
					}
#endif
				}
				printf("\n");
			}
		} else if (strcmp("clear", line) == 0) {
			for (thr = domain->threads; thr != NULL; thr = thr->nextInDomain) {
#ifdef USE_QMAGIC
				if (thr->magic != MAGIC_THREAD)
					break;
#endif
				if (thr->profile != NULL)
					clear_hash(thr->profile);
			}
			printf("\nprofile data cleared");
			continue;
		} else if (strcmp("help", line) == 0) {
			printf("\nsort (total|method|average) \n");
			printf("dump <Teiler> \n");
			printf("list      \n");
#ifdef PROF_EXTRAS
			printf("rdump/rlist \n");
#endif
			printf("next \n");
			printf("show (<method name>|0x<addr>)\n");
			printf("digraph <method name>\n");
			printf("graphic <method name>\n");
			printf("freq <cpu freqence in MHZ>\n");
			printf("clear     \n");
			printf("quit      \n");
			continue;
		} else {
			printf("\ncommand not found!");
		}
	}
}

ProfileDesc *profile_new_desc()
{
	ProfileDesc *new_desc;

	/* alloc and init desc */

	if ((new_desc = jxmalloc(sizeof(ProfileDesc)) MEMTYPE_PROFILING) == NULL)
		return NULL;
	memset(new_desc, 0, sizeof(ProfileDesc));

	/* alloc and init drift stack */

	if ((new_desc->drift_stack = jxmalloc(sizeof(long long) * PROF_DRIFT_STACK_SIZE) MEMTYPE_PROFILING) == NULL) {
		printf("Profile out of memory!\n");
		jxfree(new_desc, sizeof(ProfileDesc) MEMTYPE_PROFILING);
		return NULL;
	}
	new_desc->drift_ptr = new_desc->drift_stack + 1;
	new_desc->drift_stack_end = new_desc->drift_stack + PROF_DRIFT_STACK_SIZE;
	memset(new_desc->drift_stack, 0, sizeof(long long) * PROF_DRIFT_STACK_SIZE);

	return new_desc;
}

ProfileEntry *profile_new_entry()
{

	/* alloc and init memory for profile entry */

	ProfileEntry *new_entry;
	if ((new_entry = jxmalloc(sizeof(ProfileEntry)) MEMTYPE_PROFILING) != NULL) {
		memset(new_entry, 0, sizeof(ProfileEntry));
	}
	return new_entry;
}

void profile_dump(struct ThreadDesc_s *thr)
{
	ProfileEntry *entry;
	ProfileEntry *lst;
	MethodDesc *m_method;
	Class *m_class;
	MethodDesc *c_method;
	Class *c_class;
	FILE *f_out;
	long count_entries = 0;

	ASSERTTHREAD(thr);

	if (thr->profile == NULL) {
		printf("no profile data!\n");
		return;
	}

	if ((lst = create_entry_lst(NULL, thr->profile)) == NULL)
		return;

#ifndef KERNEL
	if ((f_out = fopen("jx.prof", "w")) == NULL) {
		printf("can't open jx.prof\n");
		return;
	}
#endif
	fprintf(f_out, "count callee caller clocks\n");

	for (entry = lst; entry != NULL; entry = entry->next) {

		m_method = (MethodDesc *) entry->callee;
		c_method = (MethodDesc *) entry->cmdesc;

		if (findClassForMethod(m_method, &m_class) != -1) {
			fprintf(f_out, "%lu JCORE.%s%s ", entry->count, m_method->name, m_method->signature);
		} else {
			fprintf(f_out, "%lu %s.%s%s ", entry->count, m_class->classDesc->name, m_method->name,
				m_method->signature);
		}

		if (findClassForMethod(c_method, &c_class) != -1) {
#ifndef KERNEL
			fprintf(f_out, "JCORE %llu", entry->time);
#else
			fprintf(f_out, "JCORE %lu", (long) entry->time);
#endif
		} else {
#ifndef KERNEL
			fprintf(f_out, "%s.%s%s %llu", c_class->classDesc->name, c_method->name, c_method->signature,
				entry->time);
#else
			fprintf(f_out, "%s.%s%s %lu", c_class->classDesc->name, c_method->name, c_method->signature,
				(long) entry->time);
#endif
		}

		fprintf(f_out, "\n");

		count_entries++;
	}

#ifdef PROF_EXTRAS
	if (count_entries > 0) {
		printf("count: %ld entries: %ld/%ld\n", thr->profile->count, thr->profile->ecount, count_entries);
	}
#endif

	free_entry_lst(lst);
#ifndef KERNEL
	fclose(f_out);
#endif				/* NO KERNEL */
}

void profile_free(ThreadDesc * thr)
{
	ProfileDesc *p;

	if (thr == NULL)
		thr = curthr();

	printf("profile_free called!\n");
	if ((p = thr->profile) == NULL)
		return;
	//thr->profile=NULL;

	clear_hash(p);

	if (p->drift_stack != NULL) {
		jxfree(p->drift_stack, p->drift_stack_end - p->drift_stack MEMTYPE_PROFILING);
		p->drift_stack = NULL;
	}
	//jxfree(p);      
}

void profile_stop_block(ThreadDesc * thr)
{
	unsigned long long st;
	ProfileDesc *p;
	unsigned char *eip;

	ASSERT_PROF_THREAD(thr);

	DBG_MSG(("profile stop called 0x%lx %s\n", (long) thr, thr->domain->domainName));

	if ((p = thr->profile) == NULL)
		return;

	if (p->stop > 0)
		return;

	p->stop = 1;
	asm volatile ("rdtsc":"=A" (st):);
	p->stimer = st;
}

void profile_cont_block(ThreadDesc * thr)
{
	unsigned long long st;
	ProfileDesc *p;

	ASSERT_PROF_THREAD(thr);

	DBG_MSG(("profile cont called 0x%lx %s\n", (long) thr, thr->domain->domainName));

	if ((p = thr->profile) == NULL)
		return;

	if (p->stop != 1)
		return;

	asm volatile ("rdtsc":"=A" (st):);
	p->dtime += st - p->stimer;
	p->stop = 0;
}

void profile_stop(ThreadDesc * thr)
{
	unsigned long long st;
	ProfileDesc *p;
	unsigned char *eip;

	ASSERT_PROF_THREAD(thr);

	DBG_MSG(("profile stop called 0x%lx %s\n", (long) thr, thr->domain->domainName));

	if ((p = thr->profile) == NULL)
		return;

	eip = (char *) thr->context[PCB_EIP] - 2;

#ifdef PROF_EXTRAS
	p->nswitch++;
#endif

	if (p->stop == 2) {
		/* see epilog of java method */
		/* rdtsc */
		/* movl 0,... */
		if ((eip[0] != 0x0f) && (eip[1] != 0x31)
		    && (eip[2] != 0xc7))
			return;
	} else {
		/* see epilog of java method */
		/* rdtsc */
		/* movl 2,... */
		if ((eip[0] == 0x0f) && (eip[1] == 0x31)
		    && (eip[2] == 0xc7))
			return;
	}

	p->stop = 1;
	asm volatile ("rdtsc":"=A" (st):);
	p->stimer = st;
}

void profile_stop_portal(ThreadDesc * thr)
{
	unsigned long long st;
	ProfileDesc *p;

	DBG_MSG(("profile stop portal called 0x%lx %s\n", (long) thr, thr->domain->domainName));

	if ((p = thr->profile) == NULL)
		return;

	if (p->stop == 0) {
		p->stop = 1;
		asm volatile ("rdtsc":"=A" (st):);
		p->stimer = st;
	}
#ifdef PROF_EXTRAS
	p->nportal++;
#endif
}

void profile_cont(ThreadDesc * thr)
{
	unsigned long long st;
	ProfileDesc *p;

	ASSERT_PROF_THREAD(thr);

	DBG_MSG(("profile cont called 0x%lx %s\n", (long) thr, thr->domain->domainName));

	if ((p = thr->profile) == NULL)
		return;

	if (p->stop != 1)
		return;

	asm volatile ("rdtsc":"=A" (st):);
	p->dtime += st - p->stimer;
	p->stop = 0;
}

void profile_cont_portal(ThreadDesc * thr)
{
	unsigned long long st;
	ProfileDesc *p;

	DBG_MSG(("profile stop portal called 0x%lx %s\n", (long) thr, thr->domain->domainName));

	if ((p = thr->profile) == NULL)
		return;

	if (p->stop != 1)
		return;

	asm volatile ("rdtsc":"=A" (st):);
	p->dtime += st - p->stimer;
	p->stop = 0;
#ifdef PROF_EXTRAS
	if ((st - p->stimer) > p->total_dtime2)
		p->total_dtime2 = (st - p->stimer);
#endif
}

void profile_trace(int kind, void *caller, MethodDesc * callee)
{
#ifndef KERNEL
	Class *m_class;
	u4_t *base, *eip;
	int mask, frame;
	SharedLibDesc *slib;
	jint **ptr;
	jint ***data;

	//if (1) return;

#ifdef PROF_FAST_TRACE
	if (kind == 1)
		return;
#endif

	eip = (u4_t *) & eip - 1;
	base = (u4_t *) & kind - 2;

	mask = 0xffffffff >> (32 - STACK_CHUNK);
	frame = STACK_CHUNK_SIZE - ((int) (&m_class) & mask) - 20;

	if (kind > 1) {
		printf("trace call ");
		printf("%p p1=%p p2=%p ebp=%p eip=%p\n", kind, caller, callee, *base, *eip);
#if 0
		//printf("\ncl %p off %d\n",kind,caller);
		//printf("field %p",callee);
		ASSERTCLASSDESC(kind);
		slib = ((ClassDesc *) kind)->definingLib;
		ptr = (curdom()->sfields[slib->ndx]) + (int) caller / 4;
		//printf("==%p d%d %p:%d %s\n",ptr,(int)callee - (int)ptr,*ptr,*ptr,((ClassDesc*)kind)->name);
		data = (int) base & (0xffffffff << STACK_CHUNK);
		//printf("lib-index %d frame %p frame[0] %p\n",slib->ndx,data,*data);
		m_class = classDesc2Class(curdom(), (ClassDesc *) kind);
		//printf("class->staticFields(%p) == curdom()->sfields[%d](%p) == frame[0][%d](%p)\n",
		//m_class->staticFields,
		//slib->ndx,(curdom()->sfields[slib->ndx]),
		//slib->ndx,(data[0][slib->ndx]));

		test_static((ClassDesc *) kind, (jint *) callee, (int) caller);
		printf("get %s %d/%d %p=%d\n", ((ClassDesc *) kind)->name, ((int) caller) / 4, caller, callee, *callee);

		if (ptr != callee)
			monitor(NULL);
#endif
		return;
	}

	if (kind == 0) {
		printf("0x%lx -> 0x%lx %4d/%4d", caller, callee->code, frame, STACK_CHUNK_SIZE);
	} else {
		printf("0x%lx <- 0x%lx %4d/%4d", caller, callee->code, frame, STACK_CHUNK_SIZE);
	}

#ifndef PROF_FAST_TRACE
	printf(" %s%s\n", callee->name, callee->signature);
#else
	if (!findClassForMethod(callee, &m_class)) {
		printf(" <unknown>.%s%s\n", callee->name, callee->signature);
	} else {
		printf(" %s.%s%s\n", m_class->classDesc->name, callee->name, callee->signature);
	}
#endif
#endif
	return;
}

long *profile_call2(MethodDesc * cmethod, long *sp, unsigned long long te)
{
	/* callee , caller , edx , eax , thr->profile(->drift_ptr) */
	unsigned long long t1;
	unsigned long long t2;
	unsigned long long t3;
	MethodDesc *callee;
	void *caller;
	ProfileDesc *p;
	unsigned long dummy;
	int i;

	asm volatile ("rdtsc":"=A" (t2):);

	i = 0;
#ifdef PROF_SAFE
	p = curthr()->profile;
	if (((ProfileDesc *) sp[i] != p)
	    || ((MethodDesc *) sp[i + 4] != cmethod)) {
		for (i = 10; i > -20; i--)
			if (((ProfileDesc *) sp[i] == p)
			    && ((MethodDesc *) sp[i + 4] == cmethod))
				break;
		printf("p!=profileDesc i=%d\n", i);
	}
#else
	p = (ProfileDesc *) sp[i];
#endif
	p->stop = 2;

	t1 = (unsigned long long) (sp[i + 1]);
	//t1     = ((unsigned long long)(sp[i+2]) << 32 ) | (unsigned long long)sp[i+1];
	caller = sp[i + 3];
	callee = sp[i + 4];

#ifdef PROF_SAFE
	if (t1 > t2) {
#ifdef PROF_EXTRAS
		if (p->exception_err == 0)
			printf("!! t1>t2 see stat for details !!\n");
		p->exception_err++;
#else
		printf("t1>t2\n");
#endif
		return sp -= 5;
	}
	if (callee != cmethod) {
		printf(" %s != %s \n", callee->name, cmethod->name);
	}
#endif

	profile_call(p, t2, dummy, t2, t1, caller, callee);

#ifdef PROF_EXTRAS
	p->exceptions++;
	p->exception_time += (t2 - te);
#endif

	asm volatile ("rdtsc":"=A" (t3):);
	p->stop = 0;
	p->drift_ptr[0] += (t3 - t2);

	return sp -= 5;
}

void profile_call(void *profileDesc, unsigned long long t2, unsigned long eflags, unsigned long long rval, unsigned long long t1,
		  void *caller, MethodDesc * callee)
{
	unsigned long long *drift;
	ProfileDesc *p;
	ProfileEntry *entry;
	unsigned long long td;
	u4_t i;

	DBG_LOCK(&call_lock);

	p = curthr()->profile;

#ifdef PROF_SAFE
	if (p != profileDesc)
		printf("\np!=profileDesc method:0x%lx\n", (long) callee);
	if (p->stop != 2)
		printf("\np->stop=%i\n", p->stop);
#endif

	if (realloc_stack(p)) {
		printf("Drift stack overrun in profile call! Profiling aborted\n");
		profile_free(NULL);
		return;
	}

	/* find entry */

#ifdef PROF_EXTRAS
	p->count++;
#endif

	i = (((u4_t) caller + (u4_t) callee) >> PROF_HASH_OFFSET) % PROF_HASH_SIZE;
	ASSERT(i >= 0 && i < PROF_HASH_SIZE);
	if ((entry = p->entries[i]) == NULL) {
		if ((entry = p->entries[i] = profile_new_entry()) == NULL) {
			printf("Out of memory in profile_call! Profiling aborted\n");
			profile_free(NULL);
			return;
		}
		entry->callee = callee;
		entry->caller = caller;
		p->ecount++;
	} else {
		while ((entry->caller != caller)
		       || (entry->callee != callee)) {
			if (entry->next != NULL)
				entry = entry->next;
			else {
				if ((entry->next = profile_new_entry()) == NULL) {
					printf("Out of memory in profile call! Profiling aborted\n");
					profile_free(NULL);
					return;
				}
				entry = entry->next;
				entry->callee = callee;
				entry->caller = caller;
				p->ecount++;
				break;
			}
		}
	}

	/* calc raw time */
	td = t2 - t1;

	if (p->dtime > td) {	// FIXME !!!
#ifdef PROF_EXTRAS
		p->errors++;
#endif
		p->dtime = 0;
	}

	drift = --(p->drift_ptr);
	//printf(" %lu %lu %lu %s\n",(long)drift[0],(long)drift[1],(long)drift[2],callee->name);
#ifdef PROF_SAFE
	if (drift < p->drift_stack || drift >= p->drift_stack_end) {
		printf("\n !!!! drift pointer out of range !!!! %d \n\n", drift - p->drift_stack);
	}
#endif

	drift[1] += p->dtime + profile_drift_in;
#ifdef PROF_EXTRAS
	p->total_dtime += p->dtime;
#endif

#ifdef PROF_EXTRAS
	entry->rtime += td;
#endif
	if (drift[1] > td) {
		long drift_err = drift[1] - td;
#ifdef PROF_EXTRAS
		p->drift_overruns++;
		if (p->drift_overruns < 5) {
			printf("\n!!! dirft > td  %lu0 > %lu0 dtime=%lu0 !!! method: 0x%lx %s\n", (long) (drift[1] / 10),
			       (long) (td / 10), (long) (p->dtime / 10), callee, callee->name);
			printf(" in:%lu out:%lu err:%d\n", profile_drift_in, profile_drift_out, drift_err);
			//profile_shell("Test","");
		}
#endif
#if 0
		if (drift_err < 10) {
			if (profile_drift_in > 1)
				profile_drift_in--;
			if (profile_drift_out > 2)
				profile_drift_out -= 2;
		} else {
			/*
			   long delta_drift = (drift_err*100) / (profile_drift_in + profile_drift_out);
			   profile_drift_in  -= (delta_drift * profile_drift_in) / 100;
			   profile_drift_out -= (delta_drift * profile_drift_out) / 100;
			 */
			profile_drift_out -= drift_err / 2;
		}
#endif
		drift[1] = (td - 1);
	}
	entry->time += (td - drift[1]);
	p->dtime = 0;
	entry->count++;

	/* propaged drift upwards */
	drift[0] += drift[1] + profile_drift_out;
	drift[1] = 0;

	DBG_FREE(&call_lock);
}

/*================================================================================================
 * helper fkt.
 */

MethodEntry *new_method_lst(ProfileEntry * lst)
{
	ProfileEntry *ptr;
	MethodEntry *new_lst = NULL;

	/* insert all methods */

	for (ptr = lst; ptr != NULL; ptr = ptr->next) {
		if ((new_lst = create_method_entry(lst, new_lst, ptr->callee)) == NULL)
			return NULL;
		if ((new_lst = create_method_entry(lst, new_lst, ptr->cmdesc)) == NULL)
			return NULL;
	}

	return new_lst;
}

MethodEntry *create_method_entry(ProfileEntry * p_lst, MethodEntry * m_lst, MethodDesc * method)
{
	MethodEntry *entry;
	ProfileEntry *ptr;
	unsigned long long sub;

	/* create and find method entry */

	if (m_lst == NULL) {
		if ((entry = m_lst = new_method_entry(method)) == NULL) {
			printf("profile: ouf of memory!\n");
			return NULL;
		}
	} else {
		entry = m_lst;
		while (1) {
			if (entry->desc == method) {
				/* entry found */
				return m_lst;
			}
			if (entry->next == NULL) {
				/* entry not found */
				if ((entry->next = new_method_entry(method)) == NULL) {
					printf("profile: out of memory\n");
					// free_entry_lst(lst);
					return NULL;
				}
				entry = entry->next;
				break;
			}
			entry = entry->next;
		}
	}

	/* calc profile values */

	sub = 0;
	for (ptr = p_lst; ptr != NULL; ptr = ptr->next) {
		if (ptr->callee == method) {
			entry->calls += ptr->count;
			entry->total += ptr->time;
#ifdef PROF_EXTRAS
			entry->rtotal += ptr->rtime;
#endif
		} else if (ptr->cmdesc == method) {
			sub += ptr->time;
		}
	}

	if (entry->calls == 0) {
		entry->total = entry->method = entry->average = 1;
	} else {
#ifdef PROF_SAFE
		if (sub > entry->total) {
			if (cpu_freq == 0) {
				printf("sub>total (%i > %i) : %s 0x%lx \n", (int) sub, (int) entry->total, entry->desc->name,
				       (long) method);
			} else {
				printf("sub>total (%i > %i) us : %s 0x%lx \n", (int) (sub / cpu_freq),
				       (int) (entry->total / cpu_freq), entry->desc->name, (long) method);
			}
			entry->total = sub + entry->calls;
		}
#endif
		entry->average = entry->total / entry->calls;
		entry->method = entry->total - sub;
	}

	return m_lst;
}

MethodEntry *new_method_entry(MethodDesc * method)
{
	MethodEntry *mnew;
	Class *class;

	if ((mnew = jxmalloc(sizeof(MethodEntry)) MEMTYPE_PROFILING) == NULL)
		return NULL;
	memset(mnew, 0, sizeof(MethodEntry));

	mnew->desc = method;
	//if (!findClassForMethod(method,&class)) mnew->class=class->classDesc;
	class = NULL;
	if (!findClassForMethod(method, &class)) {
		ASSERTCLASS(class);
		mnew->class = class->classDesc;
	}

	return mnew;
}

MethodEntry *find_method(MethodEntry * lst, MethodDesc * desc)
{
	for (; lst != NULL; lst = lst->next)
		if (desc == lst->desc)
			return lst;
	return NULL;
}

int cmp_total_time(MethodEntry * a, MethodEntry * b)
{
	if (a->total > b->total)
		return 1;
	if (a->total < b->total)
		return -1;
	return 0;
}

int cmp_method_time(MethodEntry * a, MethodEntry * b)
{
	if (a->method > b->method)
		return 1;
	if (a->method < b->method)
		return -1;
	return 0;
}

int cmp_average_time(MethodEntry * a, MethodEntry * b)
{
	if (a->average > b->average)
		return 1;
	if (a->average < b->average)
		return -1;
	return 0;
}

MethodEntry *all_methods(MethodEntry * lst)
{
	MethodEntry *ptr = lst;
	for (; ptr != NULL; ptr = ptr->next)
		ptr->ndx = ptr->next;
	return lst;
}

MethodEntry *sort_methods(MethodEntry * lst, cmp_fkt_t cmp_fkt)
{
	MethodEntry *curr;
	MethodEntry *ptr;
	MethodEntry *top = NULL;
	MethodEntry **last;

	for (curr = lst; curr != NULL; curr = curr->next) {
		if (top == NULL) {
			curr->ndx = NULL;
			top = curr;
		} else {
			last = &top;
			ptr = top;
			while (1) {
				if (cmp_fkt(curr, ptr) > 0) {
					curr->ndx = ptr;
					*last = curr;
					break;
				}
				if (ptr->ndx == NULL) {
					/* not found */
					curr->ndx = NULL;
					ptr->ndx = curr;
					break;
				}
				last = &(ptr->ndx);
				ptr = ptr->ndx;
			}
		}
	}

	return top;
}

void free_method_lst(MethodEntry * lst)
{
	MethodEntry *ptr;

	for (; lst != NULL; lst = ptr) {
		ptr = lst->next;
		jxfree(lst, sizeof(MethodEntry) MEMTYPE_PROFILING);
	}
}

void list_methods(MethodEntry * lst)
{
	int i;
	printf("  calls        total      average    in method method\n");
	for (i = 1; (lst != NULL) && i <= 10; lst = lst->ndx) {
		if (lst->class != NULL) {
			if (cpu_freq > 0) {
				printf("%7lu %10luus ", lst->calls, (unsigned long) (lst->total / cpu_freq));
				if (lst->calls > 0)
					printf("%10luus %10luus ", (unsigned long) (lst->average / cpu_freq),
					       (unsigned long) (lst->method / cpu_freq));
				else
					printf("------------ ------------ ");
			} else {
				printf("%7lu %12lu ", lst->calls, (unsigned long) lst->total);
				if (lst->calls > 0)
					printf("%12lu %12lu ", (unsigned long) lst->average, (unsigned long) lst->method);
				else
					printf("------------ ------------ ");
			}
			printf("%s\n", method_name(lst));
		}
		i++;
	}
}

/*
 * this function is *not* thread save! and the return value
 * is overwriten at the next call.
 */
char *method_name(MethodEntry * e)
{
	static char txt[1024];
	return methodName2str(e->class, e->desc, txt, 1024);
}

void method_dump(ProfileEntry * p_lst, MethodEntry * m_lst, const char *cmd)
{
	MethodEntry *m;
	ProfileEntry *p;
	long l;
	int i;

	if ((l = strtol(cmd, NULL, 10)) == 0)
		l = cpu_freq;
	if (l == 0)
		l = 1;

	printf("# method list\n");
	printf("# index calls total average method  name\n");
#define MEM_INFO 1
#ifdef MEM_INFO
	printf("%%VERSION 0.2\n");
#endif
	printf("%%CLOCK %d\n", l);
	i = 0;
	for (m = m_lst; m != NULL; m = m->next)
		i++;
	printf("%%BOL %d\n", i);
	i = 1;
	for (m = m_lst; m != NULL; m = m->next) {
#ifdef MEM_INFO
		if (m->desc != NULL) {
			printf("%5d %5lu %9lu %9lu %lu %3d ", i, m->calls, (long) (m->total / l), (long) (m->method / l),
			       (long) (m->desc->code), m->desc->numberOfCodeBytes);
		} else {
			printf("%5d %5lu %9lu %9lu 0   0 ", i, m->calls, (long) (m->total / l), (long) (m->method / l));
		}
#else
		if (l > 99) {
			printf("%5d %7lu %8lu %8lu %8lu ", i, m->calls, (long) (m->total / l), (long) (m->average / l),
			       (long) (m->method / l));
		} else {
			printf("%5d %7lu %10lu %10lu %10lu ", i, m->calls, (long) (m->total / l), (long) (m->average / l),
			       (long) (m->method / l));
		}
#endif
		if (m->desc != NULL) {
			printf("%s\n", method_name(m));
		} else {
			printf("<core>\n");
		}
		m->nr = i++;
	}
	printf("%%EOL\n");

	printf("# count caller callee total\n", l);
	i = 0;
	for (p = p_lst; p != NULL; p = p->next)
		i++;
	printf("%%BOL %d\n", i);
	for (; p_lst != NULL; p_lst = p_lst->next) {
		printf("%5lu ", p_lst->count);
		if ((m = find_method(m_lst, p_lst->cmdesc)) == NULL) {
			printf("    0 ");
		} else {
			printf("%5d ", m->nr);
		}
		if ((m = find_method(m_lst, p_lst->callee)) == NULL) {
			printf("    0 ");
		} else {
			printf("%5d ", m->nr);
		}
		if (l > 99) {
			printf("%8lu\n", (long) p_lst->time / l);
		} else {
			printf("%10lu\n", (long) p_lst->time / l);
		}
	}
	printf("%%EOL\n");
}

void print_digraph_label(MethodEntry * e);

void dump_digraph(ProfileEntry * p_lst, MethodEntry * m_lst, MethodEntry * i_lst, const char *line)
{
	MethodEntry *ptr = NULL;
	ProfileEntry *e, *e2;
	MethodEntry *p2;

	if (line[7] == 0) {
		ptr = i_lst;
	} else if (line[5] == '0' && line[6] == 'x') {
		long i = strtol(line + 10, NULL, 16);
		if ((ptr = find_method(m_lst, (MethodDesc *) i)) == NULL) {
			printf("method not found!\n");
			return;
		}
	} else {
		char *mn = &line[8];
		ptr = m_lst;
		while (strncmp(mn, method_name(ptr), strlen(mn)) != 0) {
			ptr = ptr->next;
			if (ptr == NULL) {
				ptr = m_lst;
				while (ptr->desc == NULL || ptr->class == NULL || (strncmp(mn, ptr->desc->name, strlen(mn)) != 0)) {
					ptr = ptr->next;
					if (ptr == NULL) {
						printf("%s not found!\n", mn);
						return;
					}
				}
				break;
			}
		}
	}

	printf("digraph G {\n");
	printf("   ratio=fill;\n");
	//printf("   node%d [shape=box,label=\"%s\"];\n",ptr,method_name(ptr));
	print_digraph_label(ptr);

	for (e = p_lst; e != NULL; e = e->next)
		if (e->callee == ptr->desc) {
			MethodEntry *p;

			if ((p = find_method(m_lst, e->cmdesc)) == NULL) {
				printf("   /* <undefined> -> %s */\n", method_name(ptr));
			} else {
				printf("   node%d -> node%d [label=\"%lu times\"];\n", p, ptr, e->count);
				//printf("   node%d [label=\"%s\"];\n",p,method_name(p));
				print_digraph_label(p);
			}
		}

	for (e = p_lst; e != NULL; e = e->next)
		if (e->cmdesc == ptr->desc) {
			MethodEntry *p;

			if ((p = find_method(m_lst, e->callee)) == NULL) {
				printf("   /* %s -> <undefined> */\n", method_name(ptr));
			} else {
				printf("   node%d -> node%d [label=\"%lu times\"];\n", ptr, p, e->count);
				//printf("   node%d [label=\"%s\"];\n",p,method_name(p)); 
				print_digraph_label(p);
				for (e2 = p_lst; e2 != NULL; e2 = e2->next)
					if (e2->cmdesc == p->desc) {
						if ((p2 = find_method(m_lst, e2->callee))
						    == NULL) {
							printf("   /* %s -> <undefined> */\n", method_name(ptr));
						} else {
							printf("   node%d -> node%d [label=\"%lu times\"];\n", p, p2, e2->count);
							//printf("   node%d [label=\"%s\"];\n",p,method_name(p)); 
							print_digraph_label(p2);

						}
					}
			}
		}

	printf("}\n");
}

void print_digraph_label(MethodEntry * e)
{
	char txt[1024];
	int i, p;
	char *src;

	if (e->class == NULL || (int) e->class < 2) {
		printf("   node%d [label=\"jxcore\"];\n", e);
		return;
	}

	src = e->class->name;
	p = 0;
	for (i = 0; p < 1020; i++) {
		if (src[i] == 0)
			break;
		if ((src[i] == '/') || (src[i] == '\\')) {
			txt[p++] = '.';
		} else {
			txt[p++] = src[i];
		}
	}
	txt[p++] = '\\';
	txt[p++] = 'n';
	src = e->desc->name;
	for (i = 0; p < 1023; i++) {
		if (src[i] == 0)
			break;
		txt[p++] = src[i];
	}
	src = e->desc->signature;
	for (i = 0; p < 1023; i++) {
		if (src[i] == 0)
			break;
		txt[p++] = src[i];
	}
	txt[p++] = 0;

	printf("   node%d [shape=box,label=\"%s\"];\n", e, txt);
}

void show_method(ProfileEntry * p_lst, MethodEntry * m_lst, MethodEntry * i_lst, const char *line)
{
	MethodEntry *ptr = NULL;

	if (line[4] == 0) {
		ptr = i_lst;
	} else if (line[5] == '0' && line[6] == 'x') {
		long i = strtol(line + 7, NULL, 16);
		if ((ptr = find_method(m_lst, (MethodDesc *) i)) == NULL) {
			printf("method not found!\n");
			return;
		}
	} else {
		char *mn = &line[5];
		ptr = m_lst;
		while (strncmp(mn, method_name(ptr), strlen(mn)) != 0) {
			ptr = ptr->next;
			if (ptr == NULL) {
				ptr = m_lst;
				while (ptr->desc == NULL || ptr->class == NULL || (strncmp(mn, ptr->desc->name, strlen(mn)) != 0)) {
					ptr = ptr->next;
					if (ptr == NULL) {
						printf("%s not found!\n", mn);
						return;
					}
				}
				break;
			}
		}
	}

	if (i_lst != NULL) {
		ProfileEntry *e;

		printf("Method    : %s\n", method_name(ptr));
#ifdef PROF_EXTRAS
		printf("MethodDesc: 0x%lx Code: 0x%lx \n", (long) ptr->desc, (long) ptr->desc->code);
		if (cpu_freq == 0) {
			printf("raw time total %8lu clk average %8lu clk \n", (long) (ptr->rtotal),
			       (long) (ptr->rtotal / ptr->calls));
		} else {
			printf("raw time total %8lu us average %8lu us \n", (long) (ptr->rtotal / cpu_freq),
			       (long) ((ptr->rtotal / ptr->calls) / cpu_freq));
		}
#endif
		if (cpu_freq > 0) {
			printf(" calls %lu total time %8lu us, average time %8lu us, time in method %8lu us =%4lu%%\n",
			       ptr->calls, (long) (ptr->total / cpu_freq), (long) (ptr->average / cpu_freq),
			       (long) (ptr->method / cpu_freq), (long) (ptr->method * 100 / ptr->average));
		} else {
			printf(" calls %lu total time %lu clk, average time %lu clk, time in method %lu clk =%4lu%%\n",
			       ptr->calls, (long) ptr->total, (long) ptr->average, (long) ptr->method,
			       (long) (ptr->method * 100 / ptr->average));
		}

		printf(" callers: \n");
		for (e = p_lst; e != NULL; e = e->next)
			if (e->callee == ptr->desc) {
				MethodEntry *p = NULL;
				printf("%7lu ", e->count);
				if (ptr->total != 0) {
					printf("%4lu%% ", (long) (e->time * 100 / ptr->total));
				} else {
					printf("----  ");
				}
/*
	if (((p=find_method(m_lst,e->cmdesc))!=NULL)&&p->total!=0) {
	    printf("%4lu%% ",(long)(e->time*100/p->total));
	} else {
 	    printf("----  ");
	}
*/
				if (cpu_freq > 0) {
					printf("%10lu us ", (long) e->time / cpu_freq);
				} else {
					printf("%10lu clk ", (long) e->time);
				}
				if (p != NULL || (p = find_method(m_lst, e->cmdesc)) != NULL)
					printf("%s\n", method_name(p));
			}
		printf(" callees: \n");
		for (e = p_lst; e != NULL; e = e->next)
			if (e->cmdesc == ptr->desc) {
				MethodEntry *p;
				if ((p = find_method(m_lst, e->callee)) == NULL) {
					printf("%7lu      <undefined>\n", e->count);
				} else {
					printf("%7lu ", e->count);
					if (ptr->total == 0) {
						printf("----  ");
					} else {
						printf("%4lu%% ", (long) (e->time * 100 / ptr->total));
					}
					if (cpu_freq > 0) {
						printf("%10lu us ", (long) (e->time / cpu_freq));
					} else {
						printf("%10lu clk ", (long) e->time);
					}
					printf("%s\n", method_name(p));
				}
			}
	}
}

/*
 * fkt. profile entry list 
 */

ProfileEntry *create_entry_lst(ProfileEntry * lst, ProfileDesc * p)
{

	ProfileEntry *entry;
	ProfileEntry *ptr;
	MethodDesc *c_method;
	ClassDesc *c_class;
	jint c_pos;
	jint c_line;
	int i;

	ptr = lst;

	for (i = 0; i < PROF_HASH_SIZE; i++) {

		for (entry = p->entries[i]; entry != NULL; entry = entry->next) {

			if (entry->cmdesc == NULL) {
				if (findMethodAtAddr(entry->caller, &c_method, &c_class, &c_pos, &c_line)) {
					//c_method = entry->caller;
					c_method = 0;
				}
				entry->cmdesc = c_method;
			}

			if (lst == NULL) {
				if ((lst = ptr = profile_new_entry()) == NULL) {
					printf("profile: out of memory\n");
					return NULL;
				}
				memcpy(ptr, entry, sizeof(ProfileEntry));
				ptr->next = NULL;
			} else {
				ptr = lst;
				while (1) {
					if (ptr->cmdesc == entry->cmdesc && ptr->callee == entry->callee) {
						ptr->time += entry->time;
						ptr->count += entry->count;
#ifdef PROF_EXTRAS
						ptr->rtime += entry->rtime;
#endif
						break;
					}
					if (ptr->next == NULL) {

						if ((ptr->next = profile_new_entry())
						    == NULL) {
							printf("profile: out of memory\n");
							free_entry_lst(lst);
							return NULL;
						}

						ptr = ptr->next;
						memcpy(ptr, entry, sizeof(ProfileEntry));
						ptr->next = NULL;

						break;
					}
					ptr = ptr->next;
				}
			}
		}
	}

	return lst;
}

void free_entry_lst(ProfileEntry * lst)
{
	ProfileEntry *ptr;
	while (lst != NULL) {
		ptr = lst;
		lst = lst->next;
		jxfree(ptr, sizeof(ProfileEntry) MEMTYPE_PROFILING);
	}
}

#ifdef PROF_FAST_MEM
void *entry_malloc(ProfileDesc * desc)
{
	void *chunk = NULL;

	if (desc->mem_curr < desc->mem_end) {
		chunk = desc->curr;
		desc->curr += sizeof(ProfileEntry);
		return chunk;
	}

	return jxmalloc(sizeof(ProfileEntry) MEMTYPE_PROFILING);
}

void entry_free(ProfileDesc * desc, void *addr)
{
	if ((addr > desc->mem_start) && (addr < desc->mem_end)) {
		/* TODO */
		return;
	}
	sys_panic("how many bytes to free?");
	jxfree(addr, /* FIXME */ 0 MEMTYPE_PROFILING);
}
#endif

/*
 * fkt for drift stack 
 */

int realloc_stack(ProfileDesc * p)
{
	unsigned long long *new;
	int old_size, new_size;

	DBG_LOCK(&mem_lock);

	if (p->drift_stack == NULL)
		return -1;

	if ((p->drift_ptr + 1) >= p->drift_stack_end) {

		old_size = p->drift_stack_end - p->drift_stack;
		new_size = (p->drift_ptr - p->drift_stack) + 16;

		if ((new = jxmalloc(sizeof(long long) * new_size) MEMTYPE_PROFILING) == NULL) {
			DBG_FREE(&mem_lock);
			return -1;
		}

		memset(new, 0, sizeof(long long) * new_size);
		memcpy(new, p->drift_stack, sizeof(long long) * old_size);

		p->drift_ptr = new + (p->drift_ptr - p->drift_stack);

		jxfree(p->drift_stack, old_size MEMTYPE_PROFILING);

		p->drift_stack = new;
		p->drift_stack_end = new + new_size;
	}

	DBG_FREE(&mem_lock);

	return 0;
}

void clear_hash(ProfileDesc * p)
{
	ProfileEntry *e;
	ProfileEntry *n;
	int i;

	for (i = 0; i < PROF_HASH_SIZE; i++) {
		e = p->entries[i];
		p->entries[i] = NULL;
		while (e != NULL) {
			n = e->next;
			jxfree(e, sizeof(ProfileEntry) MEMTYPE_PROFILING);
			e = n;
		}
	}

	p->ecount = 0;
}

#endif				/* PROFILE */
