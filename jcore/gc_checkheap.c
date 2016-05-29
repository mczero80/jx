/********************************************************************************
 * Garbage collector integrity checks
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifdef ENABLE_GC
#include "all.h"

#include "gc_impl.h"

jboolean gc_isValidHeapRef(DomainDesc * domain, ObjectDesc * ptr)
{
	jboolean ok = JNI_TRUE;
	if (!(ptr == NULL || GC_FUNC_NAME(isInHeap) (domain, ptr))) {
		u4_t flags = getObjFlags(ptr) & FLAGS_MASK;
		if (flags == OBJFLAGS_EXTERNAL_CPUDESC) {
			/* OK */
		} else if (flags == OBJFLAGS_EXTERNAL_STRING) {
			/* OK */
		} else if (flags == OBJFLAGS_EXTERNAL_CLASS) {
			/* OK */
		} else if (flags == OBJFLAGS_EXTERNAL_METHOD) {
			/* OK */
		} else {
			ClassDesc *oc;
			ClassDesc *strClass;
			//printf("Strange data on heap at %p, flags=%ld heap=%p..%p\n", ptr, flags, GCM_NEW(domain).heap, GCM_NEW(domain).heapTop);
			//sys_panic("");
			oc = obj2ClassDesc(ptr);
			strClass = findClassDesc("java/lang/String");
			if (oc != NULL && oc->name != NULL) {
				if (oc == strClass) {
					if (!(ptr == NULL || GC_FUNC_NAME(isInHeap)
					      (domain, ptr))) {
						char value[128];
						stringToChar(ptr, value, sizeof(value));
						printf("    STRING NOT IN HEAP AND NOT IN DOMAINZERO: \"%s\"\n", value);
						ok = JNI_FALSE;
					}
				} else {
					printf(" %p (%s) NOT IN HEAP \n", ptr, oc->name);
					ok = JNI_FALSE;
				}
			} else {
				printf(" %p (? %p ?) NOT IN HEAP \n", ptr, oc);
				ok = JNI_FALSE;
			}
			if (!ok)
				sys_panic("strange heap");
		}
	}
	return ok;
}

/*
 * Check all objects on this heap whether they contain invalid references (references that
 * point outside this heap) 
 */

struct CheckHeap_scrap_s {
	jboolean ok;
	jboolean invalidate;
	char *name;
};

typedef struct CheckHeap_scrap_s CheckHeap_Data_t;

#define CHECKHEAP_SCRAP_OK (((CheckHeap_Data_t*)domain->gc.data)->ok)
#define CHECKHEAP_SCRAP_INVAL (((CheckHeap_Data_t*)domain->gc.data)->invalidate)
#define CHECKHEAP_SCRAP_NAME (((CheckHeap_Data_t*)domain->gc.data)->name)

#ifdef USE_FMAGIC
# define MAGIC_CHECK \
{ \
  if (!CHECKHEAP_SCRAP_INVAL) { \
    if (getObjMagic(obj)!=MAGIC_OBJECT) {\
      printf("NOMAGIC at %p 0x%0x 0x%0x\n", /* FIXME (char*)data-(char*)GCM_NEW(domain).heap);*/ \
             obj, getObjMagic(obj), getObjFlags(obj)); \
      sys_panic("");\
    } \
  } else { \
    setObjMagic(obj, MAGIC_INVALID); \
  } \
}

#else
# define MAGIC_CHECK
#endif

#ifdef GC_USE_NEW

static u4_t *gc_checkHeap_isValidCB(DomainDesc * domain, ObjectDesc ** ref)
{
	if (!gc_isValidHeapRef(domain, *ref)) {
		printf("Object type: %s\n", CHECKHEAP_SCRAP_NAME);
		sys_panic("Not a reference in here!");
	}
	return (u4_t *) * ref;
}

static void gc_checkHeap_ObjectCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef CHECKHEAP_VERBOSE
	// FIXME printf("Object %p %p %p\n",obj,GCM_NEW(domain).heap,GCM_NEW(domain).heapTop);
	printf("Object %p\n");
#endif
	MAGIC_CHECK;
	ASSERT(obj->vtable != NULL);

	if ((flags & FLAGS_MASK) == OBJFLAGS_EXTERNAL_STRING) {
		if (domain != domainZero) {
			printf("External string on heap of domain %ld (%s)\n", domain->id, domain->domainName);
			sys_panic("");
		}
	}
	/* FIXME        
	   #ifdef CHECKHEAP_VERBOSE
	   printf("    of class %s\n", c->name);
	   #endif
	 */
	if (!CHECKHEAP_SCRAP_INVAL) {
		CHECKHEAP_SCRAP_NAME = "Object";
//              gc_impl_walkContentObject(domain, obj, gc_checkHeap_isValidCB);
	}
}

static void gc_checkHeap_ArrayCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef CHECKHEAP_VERBOSE
	printf("Array %p %d\n", obj, ((ArrayDesc *) obj)->size);
#endif
	MAGIC_CHECK;
	CHECKHEAP_SCRAP_NAME = "Array";
//      gc_impl_walkContentArray(domain, (ArrayDesc *) obj, gc_checkHeap_isValidCB);
#ifdef CHECKHEAP_VERBOSE
	{
		ArrayDesc *ar = (ArrayDesc *) ar;
		// FIXME ar->arrayClass->n_arrayelements += objSize;
		printf("ARR: %p %s bytes=%ld sum=%ld\n", ar->arrayClass, ar->arrayClass->name, size * 4,
		       ar->arrayClass->n_arrayelements * 4);
	}
#endif
}

static void gc_checkHeap_PortalCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef CHECKHEAP_VERBOSE
	printf("Portal\n");
#endif
	MAGIC_CHECK;
}

static void gc_checkHeap_MemoryCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef CHECKHEAP_VERBOSE
	printf("Memory\n");
#endif
	MAGIC_CHECK;
	if (!CHECKHEAP_SCRAP_INVAL)
		ASSERTMEMORY(obj);
}

static void gc_checkHeap_ServiceCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef CHECKHEAP_VERBOSE
	printf("Service\n");
#endif
	MAGIC_CHECK;
	CHECKHEAP_SCRAP_NAME = "Service";
	//gc_impl_walkContentService(domain, (DEPDesc *) obj, gc_checkHeap_isValidCB);
}
static void gc_checkHeap_ServicePoolCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef CHECKHEAP_VERBOSE
	printf("ServicePool\n");
#endif
	MAGIC_CHECK;
	CHECKHEAP_SCRAP_NAME = "ServicePool";
	//gc_impl_walkContentServicePool(domain, obj, gc_checkHeap_isValidCB);
}

static void gc_checkHeap_StackCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef CHECKHEAP_VERBOSE
	printf("Stack\n");
#endif
	MAGIC_CHECK;
	CHECKHEAP_SCRAP_NAME = "Stack";
}

static void gc_checkHeap_CASCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef CHECKHEAP_VERBOSE
	printf("CAS\n");
#endif
	MAGIC_CHECK;
}

static void gc_checkHeap_AtomVarCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef CHECKHEAP_VERBOSE
	printf("Atomvar\n");
#endif
	MAGIC_CHECK;
	CHECKHEAP_SCRAP_NAME = "Atomvar";
	//gc_impl_walkContentAtomVar(domain, (AtomicVariableProxy *) obj, gc_checkHeap_isValidCB);
}

static void gc_checkHeap_DomainProxyCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef CHECKHEAP_VERBOSE
	printf("DomainProxy\n");
#endif
	MAGIC_CHECK;
	CHECKHEAP_SCRAP_NAME = "DomainProxy";
	//gc_impl_walkContentDomainProxy(domain, (DomainProxy *) obj, gc_checkHeap_isValidCB);
}
static void gc_checkHeap_CPUStateProxyCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef CHECKHEAP_VERBOSE
	printf("CPUStateProxy\n");
#endif
	MAGIC_CHECK;
	CHECKHEAP_SCRAP_NAME = "CPUStateProxy";
	// do not walk content when checking !
//      gc_impl_walkContentCPUState(domain, (CPUStateProxy *) obj, gc_checkHeap_isValidCB);
}
#endif				/* GC_USE_NEW */

jboolean gc_checkHeap(DomainDesc * domain, jboolean invalidate)
{
#ifndef GC_USE_NEW
	return GC_FUNC_NAME(checkHeap) (domain, invalidate);
#else
	CheckHeap_Data_t data;

	domain->gc.data = &data;
	CHECKHEAP_SCRAP_INVAL = invalidate;
	CHECKHEAP_SCRAP_OK = JNI_TRUE;
	CHECKHEAP_SCRAP_NAME = "not set";

	IF_DBG_GC(printf("Checking heap inval=%d\n", invalidate));

	GC_FUNC_NAME(walkHeap) (domain, gc_checkHeap_ObjectCB, gc_checkHeap_ArrayCB, gc_checkHeap_PortalCB, gc_checkHeap_MemoryCB,
				gc_checkHeap_ServiceCB, gc_checkHeap_CASCB, gc_checkHeap_AtomVarCB, gc_checkHeap_DomainProxyCB,
				gc_checkHeap_CPUStateProxyCB, gc_checkHeap_ServicePoolCB, gc_checkHeap_StackCB);
	return CHECKHEAP_SCRAP_OK;
#endif				/* GC_USE_NEW */
}
#endif				/* ENABLE_GC */
