#ifndef GC_MOVE_H
#define GC_MOVE_H

#ifdef ENABLE_GC
#include "gc_common.h"

ObjectDesc *gc_impl_shallowCopyObject(u4_t * dst, ObjectDesc * srcObj);
void gc_impl_walkContentObject(DomainDesc * domain, ObjectDesc * obj,
			       HandleReference_t handleReference);

DEPDesc *gc_impl_shallowCopyService(u4_t * dst, DEPDesc * srcObj);
void gc_impl_walkContentService(DomainDesc * domain, DEPDesc * obj,
				HandleReference_t handleReference);

ArrayDesc *gc_impl_shallowCopyArray(u4_t * dst, ArrayDesc * srcObj);
void gc_impl_walkContentArray(DomainDesc * domain, ArrayDesc * obj,
			      HandleReference_t handleReference);

Proxy *gc_impl_shallowCopyPortal(u4_t * dst, Proxy * srcObj);

CASProxy *gc_impl_shallowCopyCAS(u4_t * dst, CASProxy * srcObj);

AtomicVariableProxy *gc_impl_shallowCopyAtomVar(u4_t * dst,
						AtomicVariableProxy *
						srcObj);
void gc_impl_walkContentAtomVar(DomainDesc * domain,
				AtomicVariableProxy * obj,
				HandleReference_t handleReference);

void gc_impl_walkContent(DomainDesc * domain, ObjectDesc * obj,
			 HandleReference_t handleReference);

#endif				/* ENABLE_GC */

#endif				/* GC_MOVE_H */
