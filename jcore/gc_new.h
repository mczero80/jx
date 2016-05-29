#ifndef GC_NEW_H
#define GC_NEW_H

#if defined( GC_NEW_IMPL ) && defined ( GC_USE_NEW )
void gc_new_init(DomainDesc * domain, u4_t heap_bytes);

#ifdef GC_USE_ONLY_ONE
ObjectHandle gc_new_allocDataInDomain(DomainDesc * domain, int objSize,
				      u4_t flags);
u4_t gc_new_freeWords(DomainDesc * domain);
u4_t gc_new_totalWords(struct DomainDesc_s *domain);
void gc_new_printInfo(struct DomainDesc_s *domain);
void gc_new_init(DomainDesc * domain, u4_t heap_bytes);
void gc_new_done(DomainDesc * domain);
void gc_new_gc(DomainDesc * domain);
int gc_new_isInHeap(DomainDesc * domain, ObjectDesc * obj);
void gc_new_walkHeap(DomainDesc * domain,
		     HandleObject_t handleObject,
		     HandleObject_t handleArray,
		     HandleObject_t handlePortal,
		     HandleObject_t handleMemory,
		     HandleObject_t handleService,
		     HandleObject_t handleCAS,
		     HandleObject_t handleAtomVar,
		     HandleObject_t handleDomainProxy,
		     HandleObject_t handleCPUStateProxy);
#endif				/* GC_USE_ONLY_ONE */

#endif				/*( GC_NEW_IMPL ) && defined ( GC_USE_NEW ) */

#endif				/* GC_NEW_H */
