/********************************************************************************
 * Garbage collector
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Joerg Baumann
 *******************************************************************************/

#ifndef GC_IMP_H
#define GC_IMP_H

#ifdef ENABLE_GC
#include "gc_move.h"

void freezeThreads(DomainDesc * domain);

void walkStacks(DomainDesc * domain, HandleReference_t handler);
void walkStatics(DomainDesc * domain, HandleReference_t handler);
void walkPortals(DomainDesc * domain, HandleReference_t handler);
void walkRegistered(DomainDesc * domain, HandleReference_t handler);
void walkSpecial(DomainDesc * domain, HandleReference_t handler);
void walkInterrupHandlers(DomainDesc * domain, HandleReference_t handler);

void walkRootSet(DomainDesc * domain,
		 HandleReference_t stacksHandler,
		 HandleReference_t staticsHandler,
		 HandleReference_t portalsHandler,
		 HandleReference_t registeredHandler,
		 HandleReference_t specialHandler,
		 HandleReference_t interruptHandlersHandler);

/************************************************/
//FIXME
#define FORWARD_MASK            0x00000001
#define FORWARD_PTR_MASK        0xfffffffe

/* flag word is used as forwarding pointer
   flags must be at high position 
*/

#define GC_FORWARD 0x00000001
#define GC_WHITE   0x00000000


#define DEBUG_ALL   1
#define DEBUG_NO    0

#ifdef DBG_GC
#define GC_DEBUG_LEVEL DEBUG_ALL
#else
#define GC_DEBUG_LEVEL DEBUG_NO
#endif


#if(GC_DEBUG_LEVEL == DEBUG_NO)
#define gc_dprintf(args...) while(0){printf(args);}
#else
#ifdef SMP
#define gc_dprintf(args...)   do{printf("CPU%d: ",get_processor_id()); printf(args);}while (0);
#else
#define gc_dprintf(args...)   printf(args)
#endif
#endif



#ifdef DBG_GC
# define IF_DBG_GC(n) n;
//# define IF_DBG_GC(n) do{n}while(0);
#else
# define IF_DBG_GC(n) while(0){n;}
#endif

#define FORBITMAP(map, mapsize, isset, notset) \
{ \
  u1_t * addr = map; \
  u4_t index; \
  u1_t bits = 0; \
  for(index = 0;index < mapsize; index++) { \
    if (index % 8 == 0) bits = *addr++; \
    if (bits&1) { \
      isset; \
    } else { \
      notset; \
    } \
    bits >>= 1; \
  } \
}

#endif				/* ENABLE_GC */

#define MOVETCB(x) if (x) {tpr = thread2CPUState(x); handler(domain, (ObjectDesc **) & (tpr)); x = cpuState2thread(tpr);}

#endif				/* GC_IMP_H */
