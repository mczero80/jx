/********************************************************************************
 * Garbage collector
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifndef GC_H
#define GC_H

/****************************/
// FIXME
//public part

#include "gc_alloc.h"
#include "gc_org.h"
#include "gc_new.h"
#include "gc_compacting.h"
#include "gc_bitmap.h"

#define GC_IMPLEMENTATION_NEW        0
#define GC_IMPLEMENTATION_COMPACTING 1
#define GC_IMPLEMENTATION_BITMAP     2
#define GC_IMPLEMENTATION_CHUNKED    3

#define GC_IMPLEMENTATION_DEFAULT    GC_IMPLEMENTATION_NEW 

#ifdef GC_USE_ONLY_ONE
# define GC_FUNC_NAME_H3(A, B) gc_##A##_##B
# define GC_FUNC_NAME_H2 GC_FUNC_NAME_H3
# define GC_FUNC_NAME_H1(A, B) GC_FUNC_NAME_H2 (A, B)
# define GC_FUNC_NAME(NAME) GC_FUNC_NAME_H1 ( GC_USE_ONLY_ONE, NAME)
#else
# define GC_FUNC_NAME(NAME) domain->gc. NAME
#endif				/* GC_USE_ONLY_ONE */

#ifndef GC_SWAP_MAGIC_WITH_FLAGS
# define XMONE XMOFF
# define XMZERO 0
#else
# define XMONE 0
# define XMZERO XMOFF
#endif

void gc_init(DomainDesc * domain, u1_t * memu, jint gcinfo0, jint gcinfo1, jint gcinfo2, char *gcinfo3, jint gcinfo4, int gcImpl);
void gc_done(DomainDesc * domain);

static inline u4_t *ObjectDesc2ptr(ObjectDesc * ref)
{
	return ((u4_t *) ref) - 1 - XMOFF;
}

static inline ObjectDesc *ptr2ObjectDesc(u4_t * ptr)
{
	return (ObjectDesc *) (ptr + 1 + XMOFF);
}

/*
static inline ObjectDesc* DomainDesc2ObjectDesc(DomainDesc *domain) {
  return (ObjectDesc*)(((u4_t*)domain) - 1 );
}

static inline ObjectDesc* CPUDesc2ObjectDesc(CPUDesc *cpu) {
  return (ObjectDesc*)(((u4_t*)cpu) - 1 );
}

static inline ObjectDesc* ThreadDesc2ObjectDesc(ThreadDesc *thread) {
  return (ObjectDesc*)(((u4_t*)thread) - 1 );
}
*/
static inline ObjectDesc *DomainDesc2ObjectDesc(DomainDesc * domain)
{
	return (ObjectDesc *) (((u4_t *) domain) - 1);
}

static inline ObjectDesc *CPUDesc2ObjectDesc(CPUDesc * cpu)
{
	return (ObjectDesc *) (((u4_t *) cpu) - 1);
}

static inline ObjectDesc *ThreadDesc2ObjectDesc(ThreadDesc * thread)
{
	return (ObjectDesc *) (((u4_t *) thread) - 1);
}

static inline u4_t getObjFlags(ObjectDesc * ref)
{
	return *(((u4_t *) ref) - 1 - XMONE);
}

static inline void setObjFlags(ObjectDesc * ref, u4_t flags)
{
	*(((u4_t *) ref) - 1 - XMONE) = flags;
}

#ifdef USE_QMAGIC
/*
static inline u4_t getObjMagic(ObjectDesc *ref) {
  return *(((u4_t*)ref) - 1 - XMZERO);
}
*/

#define getObjMagic(ref) (*(((u4_t*)ref) - 1 - XMZERO))

static inline void setObjMagic(ObjectDesc * ref, u4_t magic)
{
	*(((u4_t *) ref) - 1 - XMZERO) = magic;
}
#endif

static inline u4_t gc_freeWords(DomainDesc * domain)
{
#ifdef ENABLE_GC
	return GC_FUNC_NAME(freeWords) (domain);
#else
	sys_panic("no gc defined");
	return -1;
#endif
}

static inline u4_t gc_totalWords(DomainDesc * domain)
{
#ifdef ENABLE_GC
	return GC_FUNC_NAME(totalWords) (domain);
#else
	sys_panic("no gc defined");
	return -1;
#endif
}

void gc_printInfo(DomainDesc * domain);

#ifdef DEBUG
jboolean gc_isValidHeapRef(DomainDesc * domain, ObjectDesc * ptr);
#endif

#ifdef CHECK_HEAPUSAGE
jboolean gc_checkHeap(DomainDesc * domain, jboolean invalidate);
#endif

#ifdef FIND_OBJECTS_BY_CLASS
void gc_findOnHeap(DomainDesc * domain, char *classname);
#endif

#ifdef PROFILE_HEAPUSAGE
void gc_countInstances(DomainDesc * domain, InstanceCounts_t * counts);
#endif

  /**********************************/
//FIXME
#define OBJFLAGS_OBJECT               0x00000002
#define OBJFLAGS_PORTAL               0x00000004
#define OBJFLAGS_MEMORY               0x00000006
#define OBJFLAGS_XXXXXXXXX            0x00000008
#define OBJFLAGS_ATOMVAR              0x0000000a
#define OBJFLAGS_SERVICE              0x0000000c
#define OBJFLAGS_CPUSTATE             0x0000000e
#define OBJFLAGS_FOREIGN_CPUSTATE     0x00000010
#define OBJFLAGS_XXXXXXXXXX           0x00000012
#define OBJFLAGS_EXTERNAL_CPUDESC     0x00000014
#define OBJFLAGS_ARRAY                0x00000016
#define OBJFLAGS_EXTERNAL_STRING      0x00000018
#define OBJFLAGS_CAS                  0x0000001a
#define OBJFLAGS_EXTERNAL_METHOD      0x0000001c
#define OBJFLAGS_EXTERNAL_CLASS       0x0000001e
#define OBJFLAGS_INTERCEPTINBOUNDINFO 0x00000020
#define OBJFLAGS_INTERCEPTPORTALINFO  0x00000022
#define OBJFLAGS_VMOBJECT             0x00000024
#define OBJFLAGS_CREDENTIAL           0x00000026
#define OBJFLAGS_DOMAIN               0x00000028
#define OBJFLAGS_SERVICE_POOL         0x0000002a
#define OBJFLAGS_MAPPED_MEMORY        0x0000002c
#define OBJFLAGS_STACK                0x0000002e

#define FLAGS_MASK        0x000000fe


#define OBJSIZE_ARRAY \
	  XMOFF /* magic */\
	  + 1 /* flags at neg index */\
	  + 1 /* vtable (arrays are objects!) */\
	  + 1 /* size */\
	  + 1 /* elemClass pointer */

#define OBJSIZE_ARRAY_8BIT(size) \
	  ((((size)+3)/4) \
	   + OBJSIZE_ARRAY)

#define OBJSIZE_ARRAY_16BIT(size) \
	  ((((size)+1)/2) \
	   + OBJSIZE_ARRAY)

#define OBJSIZE_ARRAY_32BIT(size) \
	  ((size) \
	   + OBJSIZE_ARRAY)

#define OBJSIZE_ARRAY_64BIT(size) \
	  (((size)*2) \
	   + OBJSIZE_ARRAY)

#define OBJSIZE_OBJECT(size) \
	  (size \
	  + XMOFF /* magic */\
	  + 1 /* flags at neg index */\
	  + 1 /* vtable  */\
	  + 1 /* size */)

#define OBJSIZE_STACK(size) \
	  (size \
	  + XMOFF /* magic */\
	  + 1 /* flags at neg index */\
	  + 2 /* size field, tcb field */\
	  + 1 /* vtable  */)

#define OBJSIZE_PORTAL \
	  ( XMOFF /* magic */\
          + 1 /* flags at neg */\
	  + 1 /* domain */\
	  + 1 /* domainID */\
	  + 1 /* index */\
	  + 1 /* vtable pointer */)

#define OBJSIZE_MEMORY \
	 ( XMOFF /* magic */\
         + 1 /* flags at negative index */\
	 + (memory_sizeof_proxy()>>2) /* vtable pointer + data */)

// FIXME jgbauman ?magic?
#define OBJSIZE_SERVICEDESC \
 (((sizeof(DEPDesc) + 4) >> 2) \
    + 1  /* OBJFLAGS */)

#define OBJSIZE_SERVICEPOOL \
 (((sizeof(ServiceThreadPool) + 4) >> 2) \
    + 1  /* OBJFLAGS */)


#define OBJSIZE_DOMAINDESC \
	  ( XMOFF /* magic */\
          + ((sizeof(DomainDesc)+4)>>2) \
          + 1 /* flags at index -1 */\
          + 1 /* vtable */)

#define OBJSIZE_CPUDESC \
	  ( XMOFF /* magic */\
	  + ((sizeof(CPUDesc)+4)>>2) \
	  + 1 /* flags at index -1 */\
	  + 1 /* vtable */)


#define OBJSIZE_ATOMVAR \
    ( XMOFF /* magic */\
    + 1 /* flags at negative index */\
    + 1 /* vtable pointer */\
    + 3 /* data */)

#define OBJSIZE_CAS \
    ( XMOFF /* magic */\
    + 1 /* flags at negative index */\
    + 1 /* vtable pointer */\
    + 1 /* data */)

#define OBJSIZE_VMOBJECT \
      ( XMOFF /* magic */\
      + 1 /* flags at negative index */\
      + 1 /* vtable pointer */\
      + 6 /* data */)

#define OBJSIZE_CREDENTIAL \
    ( XMOFF /* magic */\
    + 1 /* flags at negative index */\
    + 1 /* vtable pointer */\
    + 2 /* data */)

#define OBJSIZE_DOMAIN \
    ( XMOFF /* magic */\
    + 1 /* flags at negative index */\
    + 1 /* vtable pointer */\
    + 2 /* data */)

#define OBJSIZE_THREADDESCPROXY \
	  ( XMOFF /* magic */\
	  + ((sizeof(ThreadDescProxy)+4)>>2) \
	  + 1 /* flags at index -1 */\
	  + 1 /* vtable */)

#define OBJSIZE_FOREIGN_THREADDESC \
	  ( XMOFF /* magic */\
	  + ((sizeof(ThreadDescForeignProxy)+4)>>2) \
	  + 1 /* flags at index -1 */\
	  + 1 /* vtable */)

#define OBJSIZE_MAPPED_MEMORY \
	  ( XMOFF /* magic */\
	  + ((sizeof(MappedMemoryProxy)+4)>>2) \
	  + 1 /* flags at index -1 */\
	  + 1 /* vtable */)


#if defined(PORTAL_INTERCEPTOR) || defined(PORTAL_TRANSFER_INTERCEPTOR)
#  define OBJSIZE_INTERCEPTINBOUNDINFO \
        ( XMOFF /* magic */\
        + 1 /* flags at negative index */\
        + 1 /* vtable pointer */\
        + 6 /* data */)

#  define OBJSIZE_INTERCEPTPORTALINFO \
        ( XMOFF /* magic */\
        + 1 /* flags at negative index */\
        + 1 /* vtable pointer */\
        + 2 /* data */)
#endif

#if 0
#define OBJSIZE_DZMEMORY (sizeof(DZMemoryProxy)>>2)
#endif

static inline ObjectHandle gc_allocDataInDomain(DomainDesc * domain,
						int objsize, u4_t flags)
{
#ifdef ENABLE_GC
	return GC_FUNC_NAME(allocDataInDomain) (domain, objsize, flags);
#else
	sys_panic("no gc defined");
	return -1;
#endif
}

/*
void setObjFlags(ObjectDesc *ref, u4_t flags);
u4_t getObjFlags(ObjectDesc *ref);
*/
void gc_in(ObjectDesc * o, DomainDesc * domain);
jboolean isRef(jbyte * map, int total, int num);

#ifdef DEBUG
#define CHECKINHEAP(domain, x) ASSERT(gc_isValidHeapRef(domain, (ObjectDesc*)x))
#else
#define CHECKINHEAP(domain, x)
#endif

#ifdef ENABLE_GC

#define REF2HANDLE(ref) (&(ref))
// JUMP to atomic code
#define RETURN_FROMHANDLE(handle)  ASSERTOBJECT(*handle); return *handle;
#define RETURN_UNREGHANDLE(handle) return unregisterObject(curdom(), handle);
#define UNREGHANDLE(handle) unregisterObject(curdom(), handle);

#else

#define REF2HANDLE(ref) ((ObjectHandle)(ref))
// JUMP to atomic code
#define RETURN_FROMHANDLE(handle)  ASSERTOBJECT((ObjectDesc*)(handle)); return (ObjectDesc*)(handle);
#define RETURN_UNREGHANDLE(handle) return unregisterObject(curdom(), handle);
#define UNREGHANDLE(handle) unregisterObject(curdom(), handle);

#endif				/* ENABLE_GC */

#endif
