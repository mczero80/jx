#ifndef GC_CHUNKED_H
#define GC_CHUNKED_H

#if defined( GC_CHUNKED_IMPL ) && defined ( GC_USE_NEW )
void gc_chunked_init(DomainDesc * domain, jint initialHeap, jint chunkSize, jint startGCatTotalSize, char *principalDomainName, jint limit);

#ifdef GC_USE_ONLY_ONE
ObjectHandle gc_chunked_allocDataInDomain(DomainDesc * domain, int objSize,
				      u4_t flags);
u4_t gc_chunked_freeWords(DomainDesc * domain);
u4_t gc_chunked_totalWords(struct DomainDesc_s *domain);
void gc_chunked_printInfo(struct DomainDesc_s *domain);
void gc_chunked_init(DomainDesc * domain, u4_t heap_bytes);
void gc_chunked_done(DomainDesc * domain);
void gc_chunked_gc(DomainDesc * domain);
int gc_chunked_isInHeap(DomainDesc * domain, ObjectDesc * obj);
void gc_chunked_walkHeap(DomainDesc * domain,
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

#endif				/*( GC_CHUNKED_IMPL ) && defined ( GC_USE_NEW ) */

#endif				/* GC_CHUNKED_H */
