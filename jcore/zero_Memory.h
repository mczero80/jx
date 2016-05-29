#ifndef ZERO_MEMORY_H
#define ZERO_MEMORY_H


struct MemoryProxy_s;
typedef struct MemoryProxy_s **MemoryProxyHandle;


//FIXME
void dzmemory_decRefcount(struct MemoryProxy_s *m);
void dzmemory_alive(struct MemoryProxy_s *dzm);
void dzmemory_redirect_invalid_dz(MemoryProxyHandle mem);


ObjectDesc *copy_memory(struct DomainDesc_s *src, struct DomainDesc_s *dst,
			struct MemoryProxy_s *obj, u4_t * quota);


#if defined (USE_QMAGIC) && defined (NORMAL_MAGIC)
#define ASSERTDZMEM(x) assert_memory(x);
#define ASSERTMEMORY(x) {ASSERTOBJECT(x); ASSERTDZMEM(x); }
#else
#define ASSERTMEMORY(x)
#endif

MemoryProxyHandle allocMemoryProxyInDomain(DomainDesc * domain,
					   ClassDesc * c, jint start,
					   jint size);

struct MemoryProxy_s;
struct MemoryProxy_s *gc_impl_shallowCopyMemory(u4_t * dst,
						struct MemoryProxy_s
						*srcObj);

u4_t memory_sizeof_proxy();

#endif
