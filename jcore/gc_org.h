#ifndef GC_ORG_H
#define GC_ORG_H

#if defined( ENABLE_GC ) && !defined ( GC_USE_NEW )
void gc_org_init(DomainDesc * domain, u4_t heap_bytes);

#ifdef GC_USE_ONLY_ONE
ObjectHandle gc_org_allocDataInDomain(DomainDesc * domain, int objSize,
				      u4_t flags);
u4_t gc_org_freeWords(DomainDesc * domain);
u4_t gc_org_totalWords(struct DomainDesc_s *domain);
void gc_org_printInfo(struct DomainDesc_s *domain);
void gc_org_init(DomainDesc * domain, u4_t heap_bytes);
void gc_org_done(DomainDesc * domain);
void gc_org_finalizeMemory(DomainDesc * domain);
void gc_org_gc(DomainDesc * domain);
int gc_org_isInHeap(DomainDesc * domain, ObjectDesc * obj);

// special cases for org
void gc_org_findOnHeap(DomainDesc * domain, char *classname);
jboolean gc_org_checkHeap(DomainDesc * domain, jboolean invalidate);
#endif				/* GC_USE_ONLY_ONE */

#endif				/* defined( ENABLE_GC ) && !defined ( GC_USE_NEW ) */

#endif				/* GC_ORG_H */
