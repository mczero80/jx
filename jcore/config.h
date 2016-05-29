/********************************************************************************
 * Kernel configuration
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifndef CONFIG_H
#define CONFIG_H

#define FIND_OBJECTS_BY_CLASS  1

#ifndef STACK_TRACE_LIMIT
#define STACK_TRACE_LIMIT 100
#endif

#ifdef PROFILE
#define MONITOR 1
#define MEASURE_GC_TIME 1
#endif

#ifdef TIMESLICING_TIMER_IRQ 
#undef NO_TIMER_IRQ
#else
#define NO_TIMER_IRQ
#endif


//#define DISABLE_SIGINT 1
//#define ASSERT_PORTALCALL_NOT_IN_IRQHANDLER 1

#ifndef KERNEL			/* only print pointers when running in emulation mode */
//#define PRINT_POINTERS 1
#endif

#if defined(PROFILE) || defined(DEBUG)
#define MONITOR 1
#endif

#ifdef MONITOR
#define INCLUDE_VPRINTF 1
#endif

#ifdef PRODUCTION
#undef DEBUG
#undef USE_MAGIC
#undef IRQ_STATISTICS
#endif

#ifdef DEBUG
#define MONITOR 1
#endif

#ifndef DEBUG
#undef CHECK_SERIAL
#undef CHECK_SERIAL_IN_PORTAL
#undef CHECK_SERIAL_IN_TIMER
#undef CHECK_SERIAL_IN_YIELD
#undef CHECK_HEAPUSAGE
#undef DBG_DEP
#undef DBG_AUTO_PORTAL_PROMO
#undef DBG_GC
#undef FIND_OBJECTS_BY_CLASS
#undef CHECK_STACKTRACE
#undef CHECK_RUNNABLE_IN_RUNQ
#undef HEAP_STATISTICS
#undef IRQ_STATISTICS
#undef PORTAL_STATISTICS
#undef USE_MAGIC
#undef DEBUG_REVOCATION
#undef DEBUG_MEMORY_CREATION
#undef DEBUG_MEMORY_REFCOUNT
#undef DEBUG_HANDLE
#undef DEBUG_IRQ
#endif


#ifdef SAMPLE_FASTPATH
#define PROFILE
#define PROFILE_SAMPLE
#undef TIMER_EMULATION		/* time emulation needs alarm clock */
#endif

/********************************/
//FIXME jgbauman
#ifndef GC_USE_NEW
#undef GC_COMPACTING_IMPL
#undef GC_NEW_IMPL
#define GC_USE_ONLY_ONE org
#endif

#ifdef GC_COMPACTING_IMPL
#ifndef USE_MAGIC
#define USE_MAGIC
#endif
#endif

#ifdef USE_MAGIC
# define USE_XMAGIC		// only magic space
# define USE_QMAGIC		// use of magic space
# define USE_FMAGIC		// needs clearance
# define NORMAL_MAGIC
#endif

#ifdef GC_COMPACTING_IMPL
//#ifdef PRODUCTION
#undef NORMAL_MAGIC
//#endif
#endif

#ifdef USE_XMAGIC
# define XMOFF 1
#else
# define XMOFF 0
#endif				/* USE_XMAGIC */
/**********************************/

/* 
 * Timeslice of the round-robin scheduler
 */
#ifdef PROFILE_SAMPLE
#define SAMPLING_TIMER_IRQ 1
#undef NO_TIMER_IRQ
#undef TIMESLICING_TIMER_IRQ
#undef APIC
#endif

#if defined PROFILE_SAMPLE || defined PREEMPTION_SAMPLE
#define MAX_EIP_SAMPLES 60000
#endif


#ifdef PROFILE_EVENT_THREADSWITCH_IPSAMPLING
#undef NO_TIMER_IRQ
#undef TIMESLICING_TIMER_IRQ
#undef SAMPLING_TIMER_IRQ
#undef APIC
#endif

#ifndef ENABLE_GC
#undef PROFILE_AGING
#undef MEASURE_GC_TIME
#undef CHECK_HEAPUSAGE
#undef FIND_OBJECTS_BY_CLASS
#undef PROFILE_HEAPUSAGE
#endif

#ifdef PROFILE_AGING
#define MINILZO 1
#endif


#ifdef PROFILE_SAMPLE_HEAPUSAGE
#define MAX_HEAP_SAMPLES 60000
#endif

/* perfmormance memory counter */
#ifdef PROFILE_SAMPLE_PMC_DIFF
#  undef PROFILE_SAMPLE_PMC0
#  define PROFILE_SAMPLE_PMC0
#  undef PROFILE_SAMPLE_PMC1
#endif

#if defined(PROFILE_SAMPLE_PMC0) || defined(PROFILE_SAMPLE_PMC0)
#  define PROFILE_SAMPLE_PMC
#endif

#ifdef PROFILE_SAMPLE_PMC0
#define MAX_PMC0_SAMPLES 60000
#endif

#ifdef PROFILE_SAMPLE_PMC1
#define MAX_PMC1_SAMPLES 60000
#endif
/********/

#ifdef KERNEL
#undef DISK_EMULATION
#undef NET_EMULATION
#undef TIMER_EMULATION
#endif

#ifndef KERNEL
#undef CHECK_SERIAL
#undef CHECK_SERIAL_IN_PORTAL
#undef CHECK_SERIAL_IN_TIMER
#endif

#ifdef KERNEL

#  ifdef SMP
#    define APIC
#  endif

#endif				/* KERNEL */

#define MYPARAMS_SIZE 1024

#define HEAP_BYTES_OTHER (30 * 1024 * 1024)
#define CODE_BYTES (16 * 40 * 1024)
#define CODE_FRAGMENTS   30
#define HEAP_RESERVE (10 * 1024)

//#ifndef STACK_ON_HEAP
// 10 =  1024 byte stack size (tiny)
// 11 =  2048 byte stack size (small)
// 12 =  4096 byte stack size (normal)
// 13 =  8192 byte stack size (large)
// 14 = 16384 byte stack size (huge)
#define STACK_CHUNK      13
#define STACK_GROW       1
#define STACK_CHUNK_SIZE (1 << STACK_CHUNK)
//#endif

#define CHECK_STACK_SIZE(_a_,_s_) \
  {\
   u4_t sp=(u4_t)&(_a_);\
   if(sp < ((u4_t)curthr()->stack + (_s_<<2))) {printf("SP=%p\n", sp); throw_StackOverflowError();}\
  }

#define MAX_NUMBER_LIBS 100
#define MAX_DEP_INSTANCES 300

//#define MAX_REGISTERED 100
#define MAX_REGISTERED 50
#define MAX_SERVICES 1500

#ifdef EVENT_LOG
/* maximal number of events that should be logged */
#define MAX_EVENTS           5000000
#define MAX_EVENT_TYPE_SIZE  50
#define MAX_EVENT_TYPES      50
#define ZSTORE
#endif


#ifdef ZSTORE
#ifndef MINILZO
#define MINILZO
#endif
#endif

#define LL_SCHEDULER      "jx/scheduler/LLRRobin"
#define HL_SCHEDULER_INIT "jx/scheduler/HLRRobin_mini"

/* need to disable interrupts when spinning */
#ifdef REVOKE_USING_SPINLOCK
#ifndef REVOKE_USING_CLI
#define REVOKE_USING_CLI
#endif
#endif

#ifdef SAMPLE_FASTPATH
#define SAMPLEPOINT_FASTPATH if (do_sampling) { \
printf("SLOWOPT at %s:%d\n");\
printStackTrace("SLOWOPT ", curthr(), &domain-2); }
#else
#define SAMPLEPOINT_FASTPATH
#endif

#ifndef INIT_LIB
#define INIT_LIB "init.jll"
#endif

#define THREAD_NAME_MAXLEN 40

#ifdef DEBUG
#define CHECK_FREE 1
#endif

#ifdef KERNEL
#undef MPROTECT_HEAP
#endif

#ifdef ALL_ARRAYS_32BIT
#define ARRAY_8BIT(_class_) (0) 
#define ARRAY_16BIT(_class_) (0)
#else
#ifdef CHARS_8BIT
#define ARRAY_8BIT(_class_) ((_class_)->name[1]=='B' || (_class_)->name[1]=='C') 
#define ARRAY_16BIT(_class_) ((_class_)->name[1]=='S')
#else
#define ARRAY_8BIT(_class_) ((_class_)->name[1]=='B') 
#define ARRAY_16BIT(_class_) ((_class_)->name[1]=='S' || (_class_)->name[1]=='C')
#endif
#endif


/* defines for assembler code */
#define ENTRY(x) 	.type	 x,@function ;.globl x	;  x:	 
#define ENDENTRY(x)	.L##x##_end:	; .size	 x, .L##x##_end-x


#endif				/* CONFIG_H */
