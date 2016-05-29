#ifndef LOCK_H
#define LOCK_H

#ifndef ASSEMBLER

#include "types.h"

#ifdef DEBUG
struct DomainDesc_s;
void check_runnable(struct DomainDesc_s *domain);
void check_threadindomain(struct DomainDesc_s *domain);
#endif

#ifdef KERNEL
#  define SETEFLAGS(eflags) asm volatile("pushl %0 ; popfl" : : "r" (eflags))
#  ifdef CHECK_RUNNABLE_IN_RUNQ
#    define DISABLE_IRQ    { volatile u4_t oldflags = getEFlags();  asm volatile("cli"); if ((oldflags&0x00000200)!=0) foreachDomainRUNQ(check_runnable);
#    define RESTORE_IRQ    if ((oldflags&0x00000200)!=0) foreachDomainRUNQ(check_runnable);   foreachDomainRUNQ(check_threadindomain); SETEFLAGS(oldflags); }
#  else				/* CHECK_RUNNABLE_IN_RUNQ */
#    define DISABLE_IRQ    { volatile u4_t oldflags = getEFlags();  asm volatile("cli");
#    define RESTORE_IRQ    SETEFLAGS(oldflags); }
#  endif			/* CHECK_RUNNABLE_IN_RUNQ */

#  define ASSERTCLI ASSERT((getEFlags() & 0x00000200) == 0)
#  define ASSERTSTI ASSERT((getEFlags() & 0x00000200) != 0)
#  define CLI  asm volatile("cli");
#else				/* KERNEL */
#  include <signal.h>
#  ifdef CHECK_RUNNABLE_IN_RUNQ
#    define DISABLE_IRQ   {   sigset_t set, oldset; disable_irq(&set, &oldset);  if (!sigismember(&oldset,SIGALRM)) foreachDomainRUNQ(check_runnable);
#    define RESTORE_IRQ     if (!sigismember(&oldset,SIGALRM)) foreachDomainRUNQ(check_runnable); restore_irq(&oldset);}
#  else				/* CHECK_RUNNABLE_IN_RUNQ */
#    define DISABLE_IRQ   {   sigset_t set, oldset; disable_irq(&set, &oldset);
#    define RESTORE_IRQ     restore_irq(&oldset);}
#  endif			/* CHECK_RUNNABLE_IN_RUNQ */
#  define ASSERTCLI {sigset_t set, oldset; sigemptyset(&set); sigprocmask(SIG_BLOCK, &set, &oldset); if (!sigismember(&oldset,SIGALRM)) sys_panic("ALARM NOT BLOCKED"); }
#  define CLI  {sigset_t set; disable_irq(&set, (void*)0); }
void restore_irq(const sigset_t * set);
#endif

#ifndef DEBUG
#undef ASSERTCLI
#define ASSERTCLI
#undef ASSERTSTI
#define ASSERTSTI
#endif				/* DEBUG */

#endif				/* ASSEMBLER */

#endif				/* LOCK_H */
