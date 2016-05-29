#ifndef PROFILE_H
#define PROFILE_H

#include "config.h"
#include "code.h"

#ifndef PROFILE

#define PROFILE_STOP(_thr_)
#define PROFILE_STOP_PORTAL(_thr_)
#define PROFILE_STOP_BLOCK(_thr_)
#define PROFILE_CONT(_thr_)
#define PROFILE_CONT_PORTAL(_thr_)
#define PROFILE_CONT_BLOCK(_thr_)

#else				/* ifdef PROFILE */

#define PROF_EXTRAS 1
#define PROF_SAFE   1
#define PROF_DRIFT_STACK_SIZE 512
#define PROF_HASH_SIZE        1024
#define PROF_HASH_OFFSET      2
#define PROF_FAST_TRACE 0

#ifdef ASSEMBLER

#define PROFILE_STOP(_thr_) \
 pushl _thr_ \
 call profile_stop
#define PROFILE_CONT(_thr_) \
 pushl _thr_ \
 call profile_cont

#else				/* NO ASSEMBLER */

#ifdef PROF_SAFE
#define IF_NO_PROFILESUPPORT_RETURN(_thr_) if (_thr_->profile==NULL) return
#define ASSERT_PROF_THREAD(_thr_) ASSERTTHREAD(_thr_)
#define MALLOC(_ptr_,_size_,_msg_,_do_) if ((_ptr_=jxmalloc(_size_))==NULL) {printf _msg_ ; _do_ ;}
#define FREE(_ptr_) jxfree(_ptr_)
#else
#define IF_NO_PROFILESUPPORT_RETURN(_thr_) \/\/
#define ASSERT_PROF_THREAD(_thr_)
#define MALLOC(_ptr_,_size_) _ptr_=jxmalloc(_size_)
#define FREE(_ptr_) jxfree(_ptr_)
#endif

#define PROFILE_STOP(_thr_) profile_stop(_thr_)
#define PROFILE_STOP_PORTAL(_thr_) profile_stop_portal(_thr_)
#define PROFILE_STOP_BLOCK(_thr_) profile_stop_block(_thr_)

#define PROFILE_CONT(_thr_) profile_cont(_thr_)
#define PROFILE_CONT_PORTAL(_thr_) profile_cont_portal(_thr_)
#define PROFILE_CONT_BLOCK(_thr_) profile_cont_block(_thr_)

struct ThreadDesc_s;

typedef struct profile_entry_s {
	unsigned long long time;
	unsigned long count;
	void *callee;
	void *caller;
	void *cmdesc;
	struct profile_entry_s *next;
#ifdef PROF_EXTRAS
	unsigned long long rtime;
#endif
} ProfileEntry;

typedef struct profile_struct {
	unsigned long long *drift_ptr;	/* !! must be the first field in the struct !! */
	/* flag to show if timer is stopped */
	int stop;		/* !! must be the second field in the struct !! */

	unsigned long ecount;

	unsigned long long stimer;	/* stop timer thread switch */
	unsigned long long dtime;	/* total time spend in other threads */

	unsigned long long *drift_stack;
	unsigned long long *drift_stack_end;
	ProfileEntry *entries[PROF_HASH_SIZE];
#ifdef PROF_EXTRAS
	unsigned long count;
	unsigned long nportal;
	unsigned long nswitch;
	unsigned long errors;
	unsigned long exception_err;
	unsigned long drift_overruns;
	unsigned long long total_dtime;
	long long total_dtime2;
	unsigned long exceptions;
	unsigned long long exception_time;
#endif
} ProfileDesc;

extern long profile_drift_in;
extern long profile_drift_out;

void profile_init();

ProfileDesc *profile_new_desc();

ProfileEntry *profile_new_entry();

void profile_trace();
void profile_call();
long *profile_call2(MethodDesc * cmethod, long *sp, unsigned long long te);

/* void profile_shell(DomainDesc*); */
void profile_shell(void *);

void profile_stop(struct ThreadDesc_s *thr);
void profile_stop_portal(struct ThreadDesc_s *thr);
void profile_cont(struct ThreadDesc_s *thr);
void profile_cont_portal(struct ThreadDesc_s *thr);

void profile_dump(struct ThreadDesc_s *thr);

#endif				/* NO ASSEMBLER */

#endif				/* PROFILE */

#endif				/* PROFILE_H */
