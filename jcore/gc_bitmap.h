#ifndef GC_BITMAP_H
#define GC_BITMAP_H

#if defined( GC_BITMAP_IMPL ) && defined ( GC_USE_NEW )
void gc_bitmap_init(DomainDesc * domain, u4_t heap_bytes);

#ifdef GC_USE_ONLY_ONE
ObjectHandle gc_bitmap_allocDataInDomain(DomainDesc * domain,
					     int objSize, u4_t flags);
u4_t gc_bitmap_freeWords(DomainDesc * domain);
u4_t gc_bitmap_totalWords(struct DomainDesc_s *domain);
void gc_bitmap_printInfo(struct DomainDesc_s *domain);
void gc_bitmap_init(DomainDesc * domain, u4_t heap_bytes);
void gc_bitmap_done(DomainDesc * domain);
void gc_bitmap_gc(DomainDesc * domain);
int gc_bitmap_isInHeap(DomainDesc * domain, ObjectDesc * obj);
void gc_bitmap_walkHeap(DomainDesc * domain,
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

#endif				/*defined( GC_BITMAP_IMPL ) && defined ( GC_USE_NEW ) */

#endif				/* GC_BITMAP_H */
