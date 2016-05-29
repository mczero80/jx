#ifndef GC_COMPACTING_H
#define GC_COMPACTING_H

#if defined( GC_COMPACTING_IMPL ) && defined ( GC_USE_NEW )
void gc_compacting_init(DomainDesc * domain, u4_t heap_bytes);

#ifdef GC_USE_ONLY_ONE
ObjectHandle gc_compacting_allocDataInDomain(DomainDesc * domain,
					     int objSize, u4_t flags);
u4_t gc_compacting_freeWords(DomainDesc * domain);
u4_t gc_compacting_totalWords(struct DomainDesc_s *domain);
void gc_compacting_printInfo(struct DomainDesc_s *domain);
void gc_compacting_init(DomainDesc * domain, u4_t heap_bytes);
void gc_compacting_done(DomainDesc * domain);
void gc_compacting_gc(DomainDesc * domain);
int gc_compacting_isInHeap(DomainDesc * domain, ObjectDesc * obj);
void gc_compacting_walkHeap(DomainDesc * domain,
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

#endif				/*defined( GC_COMPACTING_IMPL ) && defined ( GC_USE_NEW ) */

#endif				/* GC_COMPACTING_H */
