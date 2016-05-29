/********************************************************************************
 * Garbage collector
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Joerg Baumann
 *******************************************************************************/

#ifndef GC_COMMON_H
#define GC_COMMON_H

#ifdef ENABLE_GC

#ifdef KERNEL
#   ifdef SMP
static spinlock_t gc_lock = SPIN_LOCK_UNLOCKED;
#      define GC_LOCK    spin_lock(&gc_lock);
#      define GC_UNLOCK  spin_unlock(&gc_lock);
#   else
#      define GC_LOCK   DISABLE_IRQ
#      define GC_UNLOCK RESTORE_IRQ
#   endif
#else				/* KERNEL */
#define GC_LOCK   DISABLE_IRQ
#define GC_UNLOCK RESTORE_IRQ
#endif				/* KERNEL */

#define gc_objSize(_o_) gc_objSize2(_o_, getObjFlags(_o_))
u4_t gc_objSize2(ObjectDesc* obj, jint flags); 

void gc_walkContinuesBlock(DomainDesc * domain, u4_t * start, u4_t ** top,
			   HandleObject_t handleObject,
			   HandleObject_t handleArray,
			   HandleObject_t handlePortal,
			   HandleObject_t handleMemory,
			   HandleObject_t handleService,
			   HandleObject_t handleCAS,
			   HandleObject_t handleAtomVar,
			   HandleObject_t handleDomainProxy,
			   HandleObject_t handleCPUStateProxy,
			   HandleObject_t handleServicePool,
			   HandleObject_t handleStackProxy);

void gc_walkContinuesBlock_Alt(DomainDesc * domain, u4_t * start,
			       u4_t * top, HandleObject_t handleObject,
			       HandleObject_t handleArray,
			       HandleObject_t handlePortal,
			       HandleObject_t handleMemory,
			       HandleObject_t handleService,
			       HandleObject_t handleCAS,
			       HandleObject_t handleAtomVar,
			       HandleObject_t handleDomainProxy);

#endif				/* ENABLE_GC */

#endif				/* GC_COMMON_H */
