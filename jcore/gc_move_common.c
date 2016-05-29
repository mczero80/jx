/********************************************************************************
 * Copying garbage collector
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Joerg Baumann
 *******************************************************************************/

#if (defined( GC_NEW_IMPL ) || defined( GC_CHUNKED_IMPL )) && defined ( ENABLE_GC )
#include "all.h"

#include "gc_impl.h"
#include "gc_pgc.h"

#include "gc_move_common.h"

#define ENSURE_INHEAP(obj)

/*
#define ENSURE_INHEAP(obj)	if (!domain->gc.ensureInHeap(domain, obj)) return (u4_t *) obj;

*/

static u4_t *gc_common_move_object(DomainDesc * domain, ObjectDesc ** refPtr)
{
	ObjectDesc *obj = *refPtr;
	u4_t *dst;

	ASSERTOBJECT(obj);
	IF_DBG_GC(printf("   GC_CHUNKED MOVE OBJECT %p \n", obj));
	ENSURE_INHEAP(obj);
#ifdef DEBUG
	if (!domain->gc.ensureInHeap(domain, obj))
		sys_panic("not in heap");
#endif
	dst = GCM_MOVE_COMMON(domain).allocHeap2(domain, OBJSIZE_OBJECT(obj2ClassDesc(obj)->instanceSize));
	return (u4_t *) gc_impl_shallowCopyObject(dst, obj);
}

static u4_t *gc_common_move_service(DomainDesc * domain, ObjectDesc ** refPtr)
{
	DEPDesc *obj = (DEPDesc *) * refPtr;
	u4_t *dst;

	ASSERTOBJECT((ObjectDesc *) obj);
	IF_DBG_GC(printf("   GC_CHUNKED MOVE SERVICE %p \n", obj));

	ASSERT(obj->refcount > 1);
	/*if (obj->refcount <= 1) sys_panic(""); */

	ENSURE_INHEAP(obj);

	dst = GCM_MOVE_COMMON(domain).allocHeap2(domain, OBJSIZE_SERVICEDESC);
	return (u4_t *) gc_impl_shallowCopyService(dst, obj);
}


static u4_t *gc_common_move_servicepool(DomainDesc * domain, ObjectDesc ** refPtr)
{
#ifdef NEW_PORTALCALL
	ServiceThreadPool *obj = (ServiceThreadPool *) * refPtr;
	u4_t *dst;

	ASSERTOBJECT((ObjectDesc *) obj);
	IF_DBG_GC(printf("   GC_CHUNKED MOVE SERVICEPOOL %p \n", obj));

	ENSURE_INHEAP(obj);

	dst = GCM_MOVE_COMMON(domain).allocHeap2(domain, OBJSIZE_SERVICEPOOL);
	return (u4_t *) gc_impl_shallowCopyServicePool(dst, obj);
#endif
}

static u4_t *gc_common_move_array(DomainDesc * domain, ArrayDesc ** refPtr)
{
	ArrayDesc *obj = (ArrayDesc *) * refPtr;
	u4_t *dst;

	ASSERTOBJECT((ObjectDesc *) obj);
	IF_DBG_GC(printf("   GC_CHUNKED MOVE ARRAY %p \n", obj));

	ENSURE_INHEAP(obj);

	dst = GCM_MOVE_COMMON(domain).allocHeap2(domain, gc_objSize(obj));
	return (u4_t *) gc_impl_shallowCopyArray(dst, obj);
}

static u4_t *gc_common_move_portal(DomainDesc * domain, ObjectDesc ** refPtr)
{
	Proxy *obj = (Proxy *) * refPtr;
	u4_t *dst;

	ASSERTOBJECT((ObjectDesc *) obj);
	IF_DBG_GC(printf("   GC_CHUNKED ATOMIC VARIABLE %p \n", obj));

	ENSURE_INHEAP(obj);

	dst = GCM_MOVE_COMMON(domain).allocHeap2(domain, OBJSIZE_PORTAL);
	return (u4_t *) gc_impl_shallowCopyPortal(dst, obj);
}

static u4_t *gc_common_move_memory(DomainDesc * domain, ObjectDesc ** refPtr)
{
	ObjectDesc *obj = (ObjectDesc *) * refPtr;
	u4_t *dst;

	ASSERTOBJECT((ObjectDesc *) obj);
	ASSERTMEMORY((ObjectDesc *) obj);
	IF_DBG_GC(printf("   GC_CHUNKED MEMORY %p \n", obj));

#ifdef DEBUG_MEMORY_REFCOUNT
	dzmemory_alive(obj);
#endif

	ENSURE_INHEAP(obj);

	dst = GCM_MOVE_COMMON(domain).allocHeap2(domain, OBJSIZE_MEMORY);
	return (u4_t *) gc_impl_shallowCopyMemory(dst, obj);
}

static u4_t *gc_common_move_atomvar(DomainDesc * domain, ObjectDesc ** refPtr)
{
	AtomicVariableProxy *obj = (AtomicVariableProxy *) * refPtr;
	u4_t *dst;

	ASSERTOBJECT((ObjectDesc *) obj);
	IF_DBG_GC(printf("   GC_CHUNKED ATOMIC VARIABLE %p \n", obj));

	ENSURE_INHEAP(obj);

	dst = GCM_MOVE_COMMON(domain).allocHeap2(domain, OBJSIZE_ATOMVAR);
	return (u4_t *) gc_impl_shallowCopyAtomVar(dst, obj);
}

static u4_t *gc_common_move_stack(DomainDesc * domain, ObjectDesc ** refPtr)
{
#ifdef STACK_ON_HEAP
	StackProxy *obj = (StackProxy *) * refPtr;
	u4_t *dst;

	ASSERTOBJECT((ObjectDesc *) obj);
	IF_DBG_GC(printf("   GC_CHUNKED STACK %p \n", obj));

	ENSURE_INHEAP(obj);

	dst = GCM_MOVE_COMMON(domain).allocHeap2(domain, OBJSIZE_STACK(obj->size));
	return (u4_t *) gc_impl_shallowCopyStack(dst, obj);
#endif				/* STACK_ON_HEAP */
}

static u4_t *gc_common_move_cas(DomainDesc * domain, ObjectDesc ** refPtr)
{
	CASProxy *obj = (CASProxy *) * refPtr;
	u4_t *dst;

	ASSERTOBJECT((ObjectDesc *) obj);
	IF_DBG_GC(printf("   GC_CHUNKED CAS %p \n", obj));

	ENSURE_INHEAP(obj);

	dst = GCM_MOVE_COMMON(domain).allocHeap2(domain, OBJSIZE_CAS);
	return (u4_t *) gc_impl_shallowCopyCAS(dst, obj);
}

static u4_t *gc_common_move_domain(DomainDesc * domain, ObjectDesc ** refPtr)
{
	DomainProxy *obj = (DomainProxy *) * refPtr;
	u4_t *dst;

	ASSERTOBJECT((ObjectDesc *) obj);
	IF_DBG_GC(printf("   GC_CHUNKED DOMAIN %p \n", obj));

	ENSURE_INHEAP(obj);

	dst = GCM_MOVE_COMMON(domain).allocHeap2(domain, OBJSIZE_DOMAIN);
	return (u4_t *) gc_impl_shallowCopyDomain(dst, obj);
}
static u4_t *gc_common_move_cpustate(DomainDesc * domain, ObjectDesc ** refPtr)
{
	ObjectDesc *obj = (ObjectDesc *) * refPtr;
	u4_t *dst;
	ThreadDesc *cpuState = cpuState2thread(obj);

	ASSERTOBJECT((ObjectDesc *) obj);
	IF_DBG_GC(printf("   GC_CHUNKED CPUState %p (%d.%d)\n", obj, TID(cpuState)));

	ENSURE_INHEAP(obj);

	dst = GCM_MOVE_COMMON(domain).allocHeap2(domain, OBJSIZE_THREADDESCPROXY);
	return (u4_t *) gc_impl_shallowCopyCpuState(dst, obj);
}

u4_t *gc_common_move_reference(DomainDesc * domain, ObjectDesc ** refPtr)
{
	u4_t *forward = NULL;
	ClassDesc *refcl;
	u4_t flags;
	ObjectDesc *ref = *refPtr;

	if (ref == NULL)
		return NULL;

	// FIXME
	if ((u4_t) ref <= 0xfff) {
		printf("MOVE STRANGE REF   %p  (%p)\n", ref, getCaller(1));
		print_eip_info(getCaller(1));
		printf("\n");
		return NULL;
	}
	IF_DBG_GC(printf("MOVE REF   %p -> ", ref));
	if (ref > (ObjectDesc *) 0 && ref < (ObjectDesc *) 100) {
		printf("** WARNING ** : strange pointer %p not touched\n", ref);
		return (u4_t *) ref;
		//    sys_panic("strange pointer");
	}

	if (ref != NULL) {
		flags = getObjFlags(ref);
		if ((flags & FORWARD_MASK) == GC_FORWARD) {
			/* object already copied, this is a forward reference */
			*refPtr = (ObjectDesc *) (flags & FORWARD_PTR_MASK);
			IF_DBG_GC(printf("      FORWARD PTR %p\n", *refPtr));
			return (u4_t *) * refPtr;
		}
		flags &= FLAGS_MASK;
		switch (flags) {
		case OBJFLAGS_FOREIGN_CPUSTATE:
		case OBJFLAGS_OBJECT:
			refcl = obj2ClassDesc(ref);
			IF_DBG_GC(printf("   OBJSLOT %p %s\n", ref, refcl->name));
			forward = gc_common_move_object(domain, refPtr);
			break;
		case OBJFLAGS_PORTAL:
			refcl = obj2ClassDesc(ref);
			IF_DBG_GC(printf("   PORTALSLOT %p %s\n", ref, refcl->name));
			forward = gc_common_move_portal(domain, refPtr);
			break;
		case OBJFLAGS_MEMORY:
			refcl = obj2ClassDesc(ref);
			IF_DBG_GC(printf("   MEMSLOT %p %s\n", ref, refcl->name));
			forward = gc_common_move_memory(domain, refPtr);
			break;
		case OBJFLAGS_ARRAY:
			refcl = obj2ClassDesc(ref);
			IF_DBG_GC(printf("   ARRSLOT %p %s\n", ref, ((ArrayDesc *) ref)->arrayClass->name));
			forward = gc_common_move_array(domain, (ArrayDesc **) refPtr);
			break;
		case OBJFLAGS_SERVICE:
			forward = gc_common_move_service(domain, refPtr);
			break;
		case OBJFLAGS_SERVICE_POOL:
			forward = gc_common_move_servicepool(domain, refPtr);
			break;
		case OBJFLAGS_ATOMVAR:
			forward = gc_common_move_atomvar(domain, refPtr);
			break;
		case OBJFLAGS_CAS:
			forward = gc_common_move_cas(domain, refPtr);
			break;
		case OBJFLAGS_DOMAIN:
			forward = gc_common_move_domain(domain, refPtr);
			break;
		case OBJFLAGS_CPUSTATE:
			forward = gc_common_move_cpustate(domain, refPtr);
			break;
		case OBJFLAGS_EXTERNAL_STRING:
			return (u4_t *) * refPtr;	/* don't move shared strings  */
		case OBJFLAGS_EXTERNAL_CPUDESC:
		case OBJFLAGS_EXTERNAL_CLASS:
		case OBJFLAGS_EXTERNAL_METHOD:
			/* don't move! */
			return (u4_t *) * refPtr;
		case OBJFLAGS_INTERCEPTINBOUNDINFO:	/* FIXME */
			sys_panic("Object Type is OBJFLAGS_INTERCEPTINBOUNDINFO or OBJFLAGS_VMOBJECT");
		case OBJFLAGS_VMOBJECT:	/* FIXME */
			sys_panic("Object Type is OBJFLAGS_VMOBJECT");
		case OBJFLAGS_STACK:
			forward = gc_common_move_stack(domain, refPtr);
			break;
		default:
			printf("UNKNOWN FLAGS: 0x%lx ref=%p\n", flags, *refPtr);
			refcl = obj2ClassDescFAST(ref);
			printf("Class: %s\n", refcl->name);
#ifdef PRODUCTION
			return (u4_t *) ref;
#else
			sys_panic("UNKNOWN OBJECT TYPE");
#endif
		}
		//printf("FORWARD %p: %p\n", refPtr, forward);
		/* create forward pointer */
		ASSERT(((u4_t) forward & FORWARD_MASK) == 0);
		setObjFlags(ref, (u4_t) forward | GC_FORWARD);
		*refPtr = (ObjectDesc *) forward;
		return forward;
	} else {
		return NULL;
	}
}



static void gc_common_move_scan_heap2_Object(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef STACK_ON_HEAP
	gc_impl_walkContentObject(domain, obj, gc_common_move_reference);
#endif
}

static void gc_common_move_scan_heap2_Array(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	gc_impl_walkContentArray(domain, (ArrayDesc *) obj, gc_common_move_reference);
}

static void gc_common_move_scan_heap2_Service(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	gc_impl_walkContentService(domain, (DEPDesc *) obj, gc_common_move_reference);
}

static void gc_common_move_scan_heap2_ServicePool(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	gc_impl_walkContentServicePool(domain, (DEPDesc *) obj, gc_common_move_reference);
}

static void gc_common_move_scan_heap2_AtomVar(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	gc_impl_walkContentAtomVar(domain, (AtomicVariableProxy *) obj, gc_common_move_reference);
}
static void gc_common_move_scan_heap2_CPUState(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	gc_impl_walkContentCPUState(domain, (ThreadDescProxy *) obj, gc_common_move_reference);
}
static void gc_common_move_scan_heap2_ForeignCPUState(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	gc_impl_walkContentForeignCPUState(domain, (ThreadDescForeignProxy *) obj, gc_common_move_reference);
}
static void gc_common_move_scan_heap2_Stack(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
#ifdef STACK_ON_HEAP
	gc_impl_walkContentStack(domain, (DEPDesc *) obj, gc_common_move_reference);
#endif
}

void gc_common_move_scan_heap2(DomainDesc * domain)
{
	GCM_MOVE_COMMON(domain).walkHeap2(domain, gc_common_move_scan_heap2_Object, gc_common_move_scan_heap2_Array, NULL, NULL,
					  gc_common_move_scan_heap2_Service, NULL, gc_common_move_scan_heap2_AtomVar, NULL,
					  gc_common_move_scan_heap2_CPUState, gc_common_move_scan_heap2_ServicePool,
					  gc_common_move_scan_heap2_Stack);
}
#endif
