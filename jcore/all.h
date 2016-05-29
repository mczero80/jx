#ifndef ALL_H
#define ALL_H

#include "config.h"
#ifndef KERNEL
#include <stdio.h>
#include <signal.h>
#include <unistd.h>
#include <sys/time.h>
#include <stdlib.h>
#include <asm/sigcontext.h>
#include <termios.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdarg.h>
#else
#include "minic.h"
#include "intr.h"
#endif

#include "malloc.h"
#include "load.h"
#include "context.h"
#include "lowlevel.h"
#include "thread.h"
#include "portal.h"
#include "domain.h"
#include "types.h"
#include "config.h"
#include "interrupt.h"
#include "gc.h"
#include "vmsupport.h"
#include "execJAVA.h"
#include "monitor.h"
#include "zero.h"
#include "zero_Profiler.h"
#include "malloc_proto.h"
#include "portal_proto.h"
#include "atomic.h"
#include "exception_handler.h"



#include "config.h"
#include "thread.h"
#include "misc.h"
#include "monitor.h"
#include "lapic.h"
#include "smp.h"
#include "serialdbg.h"
#include "spinlock.h"
#include "symfind.h"
#include "interrupt.h"



#include "irq.h"
#include "smp.h"
#include "zero.h"
#include "lapic.h"
#ifdef JAVASCHEDULER
#include "javascheduler.h"
#include "execJAVA.h"
#endif
//#include "mutex.h"
#include "spinlock.h"
#include "libcache.h"
#include "exception_handler.h"
#include "memfs.h"





#include "irq.h"
#include "zero.h"
#include "smp.h"
#include "monitor.h"
#include "thread.h"
#include "multiboot.h"
#include "symfind.h"





#ifndef KERNEL
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdarg.h>
#include <sys/time.h>
#include <unistd.h>
#include <string.h>
#include <signal.h>
#include <asm/sigcontext.h>
#else
#include "minic.h"
#endif				/* KERNEL */



#include "misc.h"
#include "zero.h"
#include "memfs.h"
#include "libcache.h"
#include "zip.h"
#include "thread.h"
#include "malloc.h"



#include "types.h"
#include "intr.h"
#include "config.h"
#include "zero.h"
#include "serialdbg.h"
#include "monitor.h"
#include "irq.h"
#include "debug_reg.h"



#include "object.h"
#include "bench.h"
#include "ekhz.h"

#include "sched.h"


#ifndef INATOMICFN
#define ATOMICFN(_r_, _n_, _s_) extern _r_ (* _n_) _s_;
#define ATOMICFN0(_r_, _n_, _s_) extern _r_ (* _n_) _s_;
#include "atomicfn.h"
#endif


#endif				/* ALL_H */
