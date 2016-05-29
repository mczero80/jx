/********************************************************************************
 * Copying garbage collector
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Joerg Baumann
 *******************************************************************************/

// FIXME
#if defined (ENABLE_GC)
//#if !defined( GC_USE_NEW ) && defined (ENABLE_GC)
#include "all.h"

//#include "gc_memcpy.h"
#include "gc_move.h"
#include "gc_pa.h"
#include "gc_pgc.h"
#include "gc_org.h"
#include "gc_common.h"
#include "gc_impl.h"
#include "gc_thread.h"
#include "gc_stack.h"
#include "gc_org_int.h"
#include "gc_memcpy.h"

/*
#define DBG_GCSTATIC 1
#define DBG_SCAN_HEAP2 1
#define CHECK_HEAP_BEFORE_ALLOC 1
#define CHECK_HEAP_AFTER_ALLOC 1
*/

/* FIXME prototypes */
int eip_in_last_stackframe(u4_t eip);
ObjectHandle(*registerObject) (DomainDesc * domain, ObjectDesc * o);
extern unsigned char callnative_special_end[], callnative_special_portal_end[], callnative_static_end[], thread_exit_end[];
void return_from_java0(ThreadDesc * next, ContextDesc * restore);
void return_from_java1(long param, ContextDesc * restore, ThreadDesc * next);
void return_from_java2(long param1, long param2, ThreadDesc * next, ContextDesc * restore);
extern unsigned char return_from_javaX_end[], never_return_end[];
void never_return(void);
jint cpuManager_receive(ObjectDesc * self, Proxy * portal);
extern unsigned char cpuManager_receive_end[];
/* FIXME prototypes */
void profile_sample_heapusage_alloc(DomainDesc * domain, u4_t objSize);

#ifdef JAVASCHEDULER
void gc_org_moveSchedulerObject(DomainDesc * domain)
{
	u4_t i;
	for (i = 0; i < MAX_NR_CPUS; i++) {
		if (domain->Scheduler[i] != NULL)
			if (domain->Scheduler[i]->SchedObj != NULL)
				if (domain == domain->Scheduler[i]->SchedThread->domain) {
					gc_dprintf("move HLScheduler %d\n", i);
					gc_org_move_reference(domain, &(domain->Scheduler[i]->SchedObj));
				}
	}
}
#endif

inline u4_t *gc_org_allocHeap2(DomainDesc * domain, u4_t size, u4_t flags)
{
	u4_t *data;
	ObjectDesc *obj;
	data = GCM_ORG(domain).heapTop2;
	if (data > (u4_t *) GCM_ORG(domain).heapBorder2) {
		sys_panic("target heap too small????");
	}
	GCM_ORG(domain).heapTop2 += size;
	obj = ptr2ObjectDesc(data);
	setObjFlags(obj, flags);
#ifdef USE_QMAGIC
	setObjMagic(obj, MAGIC_OBJECT);
#endif
#ifdef DBG_GC
	printf("target heap size: %ld bytes\n", (u4_t) GCM_ORG(domain).heapTop2 - (u4_t) GCM_ORG(domain).heap2);
#endif				/* DBG_GC */
	return data;
}

/* @param rec copy recursive */
u4_t *gc_org_move_object(DomainDesc * domain, ObjectDesc ** refPtr, jboolean onlyContents)
{
	ObjectDesc *targetObj = NULL;
	ClassDesc *cl;
	int objSize;
	u4_t *data;
	u4_t k;
	u1_t b = 0;
	u1_t *addr;
	ObjectDesc *obj = *refPtr;
	ASSERTOBJECT(obj);
	cl = obj2ClassDesc(obj);
	objSize = OBJSIZE_OBJECT(cl->instanceSize);

#ifdef DBG_GC
	printf("   MOVE OBJECT %p %s\n", obj, cl->name);
#endif

	if (!onlyContents) {
		// FIXME jgbauman
		if ((obj >= (ObjectDesc *) GCM_ORG(domain).heap) && (obj < (ObjectDesc *) GCM_ORG(domain).heapBorder)) {	//||
			//((obj>=GCM_ORG(domain).heap2) && (obj<GCM_ORG(domain).heapBorder2))) { 
			data = gc_org_allocHeap2(domain, objSize, OBJFLAGS_OBJECT);

			targetObj = ptr2ObjectDesc(data);
#ifdef USE_QMAGIC
			setObjMagic(targetObj, MAGIC_OBJECT);
#endif
#ifdef DBG_GC
			printf("     moved to %p\n", targetObj);
#endif

			targetObj->vtable = obj->vtable;
		} else {
			printf("%p\n", obj);
			printf("%s\n", cl->name);
			sys_panic("OBJECT OUTSIDE HEAP");
		}
	} else {
		targetObj = obj;
	}

	if (!onlyContents) {
		// copy data to target object
		PGCB(MOVE);
		gc_memcpy(targetObj->data, obj->data, objSize * 4);
		PGCE(MOVE);
	}

	if (onlyContents) {
		addr = cl->map;
		for (k = 0; k < cl->instanceSize; k++) {
			if (k % 8 == 0)
				b = *addr++;
			if (b & 1) {
				/* reference slot */
				//printf("   REFSLOT %p[%d] (%s) =%p\n", targetObj,  k, cl->name, targetObj->data[k]);
				gc_org_move_reference(domain, (ObjectDesc **) & (targetObj->data[k]));
#ifdef DBG_GC
				printf("   REFSLOT %p[%ld]=%p\n", targetObj, k, (void *) (targetObj->data[k]));
#endif
			} else {
				targetObj->data[k] = obj->data[k];
#ifdef DBG_GC
				printf("   NUMSLOT %p[%ld]=%d\n", targetObj, k, (void *) (targetObj->data[k]));
#endif
			}
			b >>= 1;
		}
	}
#ifdef PROFILE_AGING
	paMove(OBJECT, (jint *) obj, (jint *) targetObj);
#endif
	return (u4_t *) targetObj;
}

extern Proxy *initialNamingProxy;

u4_t *gc_org_move_portal(DomainDesc * domain, ObjectDesc ** refPtr)
{
	Proxy *targetObj;
	//ClassDesc *cl;
	int objSize;
	u4_t *data;
	Proxy *obj = (Proxy *) * refPtr;

	if (obj == initialNamingProxy) {
#ifdef PROFILE_AGING
		paMove(PROXY, (jint *) obj, (jint *) obj);
#endif
		return (u4_t *) obj;	/* no need to move this */
	}

	objSize = OBJSIZE_PORTAL;
	data = gc_org_allocHeap2(domain, objSize, OBJFLAGS_PORTAL);

	targetObj = (Proxy *) ptr2ObjectDesc(data);
#ifdef USE_QMAGIC
	setObjMagic(targetObj, MAGIC_OBJECT);
#endif
	targetObj->vtable = obj->vtable;
	targetObj->targetDomain = obj->targetDomain;
	targetObj->targetDomainID = obj->targetDomainID;
	targetObj->index = obj->index;
#ifdef PROFILE_AGING
	paMove(PROXY, (jint *) obj, (jint *) targetObj);
#endif
	return (u4_t *) targetObj;
}

/**
 * @param onlyContents array is already on new heap, move only array contents
 */
u4_t *gc_org_move_array(DomainDesc * domain, ArrayDesc ** refPtr, jboolean onlyContents)
{
	ArrayDesc *targetObj = NULL;
	//ClassDesc *cl;
	ArrayClassDesc *c;
	int objSize;
	u4_t *data;
	ArrayDesc *obj = *refPtr;

	if (!onlyContents) {
		// FIXME jgbauman
		if ((obj >= (ArrayDesc *) GCM_ORG(domain).heap) && (obj < (ArrayDesc *) GCM_ORG(domain).heapBorder)) {	//||
			//((obj>=GCM_ORG(domain).heap2) && (obj<GCM_ORG(domain).heapBorder2))) {
			objSize = gc_objSize((ObjectDesc *) obj);
			data = gc_org_allocHeap2(domain, objSize, OBJFLAGS_ARRAY);

			targetObj = (ArrayDesc *) ptr2ObjectDesc(data);
			targetObj->vtable = obj->vtable;
			c = (ArrayClassDesc *) (targetObj->arrayClass = obj->arrayClass);
			targetObj->size = obj->size;
			//printf("%s\n",obj->arrayClass->name);
			PGCB(MOVE);
#ifdef ALL_ARRAYS_32BIT
			gc_memcpy(targetObj->data, obj->data, obj->size * 4);
#else
			if (ARRAY_8BIT(c)) {
				gc_memcpy(targetObj->data, obj->data, obj->size);
			} else if (ARRAY_16BIT(c)) {
				gc_memcpy(targetObj->data, obj->data, obj->size * 2);
			} else {
				gc_memcpy(targetObj->data, obj->data, obj->size * 4);
			}
#endif
			PGCE(MOVE);
		} else {
			printf("%p\n", obj);
			//printf("%s\n", cl->name);
			sys_panic("ARRAY OUTSIDE HEAP");
		}
	} else {
		targetObj = obj;
	}
	if (obj->arrayClass->name[1] == 'L' || obj->arrayClass->name[1] == '[') {
		/* reference array */
		u4_t i;
		if (onlyContents) {
			for (i = 0; i < obj->size; i++) {
				gc_org_move_reference(domain, (ObjectDesc **) & (targetObj->data[i]));
			}
#ifdef DBG_GC
			printf("     copied array contents %p %s\n", targetObj, obj->arrayClass->name);
#endif
		}
	} else {
#ifdef DBG_GC
		printf("     don't copy primitive array %p %s\n", targetObj, obj->arrayClass->name);
#endif
	}
#ifdef PROFILE_AGING
	paMove(ARRAY, (jint *) obj, (jint *) targetObj);
#endif
	return (u4_t *) targetObj;
}

u4_t *gc_org_move_memory(DomainDesc * domain, ObjectDesc ** refPtr, jboolean onlyContents)
{
	sys_panic("DZMem only accessible from within zero_Memory");
#if 0
	u4_t *data;
	MemoryProxy *targetObj;
	MemoryProxy *obj = (MemoryProxy *) * refPtr;
	if (!onlyContents) {
		u4_t objSize = OBJSIZE_MEMORY;
		data = gc_org_allocHeap2(domain, objSize, OBJFLAGS_MEMORY);
		targetObj = (MemoryProxy *) ptr2ObjectDesc(data);
		targetObj->vtable = obj->vtable;
		targetObj->size = obj->size;
#ifdef DEBUG_MEMORY_REFCOUNT
		dzmemory_alive(obj);
#endif
#ifdef REDIRECT_INVALID_DZ
		if (obj->dz->valid == 0)
			dzmemory_redirect_invalid_dz(&obj);
#endif
		targetObj->dz = obj->dz;
		ASSERT(targetObj->dz != NULL);
		ASSERT(targetObj->dz->refcount > 0);
		targetObj->mem = obj->mem;
		/* ASSERT(targetObj->mem != NULL); */
		if (targetObj->mem == NULL && targetObj->dz->valid)
			printf("warn: valid obj has null pointer!\n");
#ifdef DEBUG_MEMORY_CREATION
		if (targetObj->dz->valid) {
			if (targetObj->mem == NULL) {
				printf("bad memory object! created at:\n");
				print_eip_info((char *) targetObj->dz->createdAt);
				if (targetObj->dz->createdUsing)
					printf(" %s\n", targetObj->dz->createdUsing);
				domain_panic(curdom(), "valid && mem==NULL && refcount>0");
			}
		}
#endif
#ifdef DBG_GC
		printf("  copied memory = %p \n", targetObj);
#endif
		//printf("  copied memory = %p \n", targetObj);
	} else {
		targetObj = obj;
	}
	if (onlyContents) {
		//printf("  attempt to copy memory (%p)\n", targetObj);
	}
#ifdef PROFILE_AGING
	paMove(MEMORY, (jint *) obj, (jint *) targetObj);
#endif
	return (u4_t *) targetObj;
#endif
}

static u4_t *gc_org_move_cpustate(DomainDesc * domain, ObjectDesc ** refPtr)
{
	sys_panic("not impl");
}

u4_t *gc_org_move_service(DomainDesc * domain, ObjectDesc ** refPtr, jboolean onlyContents)
{
	u4_t *data;
	DEPDesc *targetObj;
	DEPDesc *obj = (DEPDesc *) * refPtr;
	u4_t objSize = OBJSIZE_SERVICEDESC;

	if (!onlyContents) {
		data = gc_org_allocHeap2(domain, objSize, OBJFLAGS_SERVICE);
		targetObj = (DEPDesc *) ptr2ObjectDesc(data);
		PGCB(MOVE);
		gc_memcpy(targetObj, obj, sizeof(DEPDesc));
		PGCE(MOVE);
	} else {
		targetObj = obj;
	}
	if (targetObj->obj != NULL) {
		if (onlyContents) {
#ifdef DBG_GC
			printf("  going to copy service.(obj=%p,proxy=%p) \n", targetObj->obj, targetObj->proxy);
#endif
			gc_org_move_reference(domain, &(targetObj->obj));
			gc_org_move_reference(domain, (ObjectDesc **) & (targetObj->proxy));
#ifdef DBG_GC
			printf("  copied service.(obj=%p,proxy=%p) \n", targetObj->obj, targetObj->proxy);
#endif
		}
	}
#ifdef DBG_GC
	printf("  copied service = %p \n", targetObj);
#endif
#ifdef PROFILE_AGING
	paMove(SERVICE, (jint *) obj, (jint *) targetObj);
#endif
	return (u4_t *) targetObj;
}

u4_t *gc_org_move_atomvar(DomainDesc * domain, ObjectDesc ** refPtr, jboolean onlyContents)
{
	u4_t *data;
	AtomicVariableProxy *targetObj;
	AtomicVariableProxy *obj = (AtomicVariableProxy *) * refPtr;
	u4_t objSize = OBJSIZE_ATOMVAR;

	if (!onlyContents) {
		data = gc_org_allocHeap2(domain, objSize, OBJFLAGS_ATOMVAR);
		targetObj = (AtomicVariableProxy *) ptr2ObjectDesc(data);
		targetObj->vtable = obj->vtable;
		targetObj->value = obj->value;
		targetObj->blockedThread = obj->blockedThread;
		//printf("  created atomvar = %p oldvalue = %p\n", targetObj, targetObj->value);
	} else {
		targetObj = obj;
	}
	if (targetObj->value != NULL) {
		if (onlyContents) {
			//ObjectDesc *o = targetObj->value;
			gc_org_move_reference(domain, &(targetObj->value));
			//printf("  copied atomvar %p updated contents from %p to %p \n", targetObj, o, targetObj->value);
		}
	}
#ifdef DBG_GC
	printf("  copied atomvar = %p \n", targetObj);
#endif
#ifdef PROFILE_AGING
	paMove(ATOM, (jint *) obj, (jint *) targetObj);
#endif
	return (u4_t *) targetObj;
}


u4_t *gc_org_move_cas(DomainDesc * domain, ObjectDesc ** refPtr, jboolean onlyContents)
{
	u4_t *data;
	CASProxy *targetObj;
	CASProxy *obj = (CASProxy *) * refPtr;
	u4_t objSize = OBJSIZE_CAS;

	if (!onlyContents) {
		data = gc_org_allocHeap2(domain, objSize, OBJFLAGS_CAS);
		targetObj = (CASProxy *) ptr2ObjectDesc(data);
		targetObj->vtable = obj->vtable;
		targetObj->index = obj->index;
	} else {
		targetObj = obj;
	}
#ifdef DBG_GC
	printf("  copied cas = %p \n", targetObj);
#endif
#ifdef PROFILE_AGING
	//paMove(ATOM, (jint*)obj, (jint*)targetObj);
#endif
	return (u4_t *) targetObj;
}

u4_t *gc_org_move_reference(DomainDesc * domain, ObjectDesc ** refPtr)
{
	u4_t *forward = NULL;
	ClassDesc *refcl;
	u4_t flags;
	ObjectDesc *ref = *refPtr;

	if (ref == NULL)
		return NULL;

#ifdef DBG_GC
	printf("MOVE REF   %p -> ", ref);
#endif
	if (ref > (ObjectDesc *) 0 && ref < (ObjectDesc *) 100) {
		printf("** WARNING ** : strange pointer %p not touched\n", ref);
		return (u4_t *) ref;
		//    sys_panic("strange pointer");
	}
	if (ref != NULL) {
		if (!((ref >= (ObjectDesc *) GCM_ORG(domain).heap)
		      && (ref < (ObjectDesc *) GCM_ORG(domain).heapTop))) {
			/* another heap -- not our business */
			// FIXME jgbauman: return *refPtr;
			//printf("outside heap: %p\n", ref);
#ifdef PROFILE_AGING
			paMove(FOREIGN, (jint *) ref, (jint *) ref);
#endif
		}
		flags = getObjFlags(ref);
		if ((flags & FORWARD_MASK) == GC_FORWARD) {
			/* object already copied, this is a forward reference */
			*refPtr = (ObjectDesc *) (flags & FORWARD_PTR_MASK);
#ifdef DBG_GC
			printf("      FORWARD PTR %p\n", *refPtr);
#endif
			return (u4_t *) * refPtr;
		}
		flags &= FLAGS_MASK;
		switch (flags) {
		case OBJFLAGS_FOREIGN_CPUSTATE:
		case OBJFLAGS_OBJECT:
			refcl = obj2ClassDesc(ref);
#ifdef DBG_GC
			printf("   OBJSLOT %p %s\n", ref, refcl->name);
#endif
			forward = gc_org_move_object(domain, refPtr, JNI_FALSE);
			break;
		case OBJFLAGS_PORTAL:
			refcl = obj2ClassDesc(ref);
#ifdef DBG_GC
			printf("   PORTALSLOT %p %s\n", ref, refcl->name);
#endif
			forward = gc_org_move_portal(domain, refPtr);
			break;
		case OBJFLAGS_MEMORY:
			refcl = obj2ClassDesc(ref);
#ifdef DBG_GC
			printf("   MEMSLOT %p %s\n", ref, refcl->name);
#endif
			forward = gc_org_move_memory(domain, refPtr, JNI_FALSE);
			break;
		case OBJFLAGS_ARRAY:
			refcl = obj2ClassDesc(ref);
#ifdef DBG_GC
			printf("   ARRSLOT %p %s\n", ref, ((ArrayDesc *) ref)->arrayClass->name);
#endif
			forward = gc_org_move_array(domain, (ArrayDesc **) refPtr, JNI_FALSE);
			break;
		case OBJFLAGS_SERVICE:
			forward = gc_org_move_service(domain, refPtr, JNI_FALSE);
			break;
		case OBJFLAGS_ATOMVAR:
			forward = gc_org_move_atomvar(domain, refPtr, JNI_FALSE);
			break;
		case OBJFLAGS_CAS:
			forward = gc_org_move_cas(domain, refPtr, JNI_FALSE);
			break;
		case OBJFLAGS_CPUSTATE:
			forward = gc_org_move_cpustate(domain, refPtr);
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
		default:
			printf("FLAGS: 0x%lx ref=%p", flags, *refPtr);
			sys_panic("UNKNOWN OBJECT TYPE");
		}
		/* create forward pointer */
		ASSERT(((u4_t) forward & FORWARD_MASK) == 0);
		setObjFlags(ref, (u4_t) forward | GC_FORWARD);
		*refPtr = (ObjectDesc *) forward;
		return forward;
	} else {
		return NULL;
	}
}


ObjectHandle gc_org_allocDataInDomain(DomainDesc * domain, int objSize, u4_t flags)
{
	ObjectDesc *obj;
	jint *nextObj;
	jint *data;
	ObjectHandle handle;
#ifdef KERNEL
	volatile u4_t irqflags __attribute__ ((unused)) = getEFlags();
#endif
#ifdef GC_USE_MMX
	objSize++;
	objSize &= ~1;
#endif

#ifdef PROFILE_AGING
	jlong memTimeNext;
	memTimeNext = ((domain->gc.memTime / memTimeStep) + 1) * memTimeStep;
	domain->gc.memTime += sizeof(jint) * objSize;
	// FIXME jgbauman: don´t profile Zero
	if (domain->domainName != NULL && (strcmp(domain->domainName, "DomainZero") == 0))
		memTimeNext = domain->gc.memTime + 10;
#endif

	GC_LOCK;

#ifdef CHECK_HEAP_BEFORE_ALLOC
	if (!gc_org_checkHeap(domain, JNI_FALSE) == JNI_TRUE)
		sys_panic("inconsistent heap");
#endif				/* CHECK_HEAP_BEFORE_ALLOC */

      try_alloc:
	nextObj = GCM_ORG(domain).heapTop + objSize;
	if ((nextObj > GCM_ORG(domain).heapBorder - HEAP_RESERVE)
#ifdef PROFILE_AGING
	    || (domain->gc.memTime > memTimeNext)
#endif
	    ) {
#if DEBUG
		printf("org %p,%p,%p,%p\n", nextObj, GCM_ORG(domain).heapBorder, (void *) HEAP_RESERVE,
		       (GCM_ORG(domain).heapBorder - HEAP_RESERVE));
#endif
#ifdef PROFILE_AGING
		gc_dprintf("\nDomain %p (%s) reached memtime %lld (%lld). Starting GC...\n", domain, domain->domainName,
			   memTimeNext, domain->gc.memTime);
#endif

#if defined(KERNEL) || defined(JAVASCHEDULER)
		if (curthr()->isInterruptHandlerThread) {
			if (nextObj > GCM_ORG(domain).heapBorder) {
				sys_panic("no GC in interrupt handler possible!");
			}
#ifdef ENABLE_GC
			goto do_alloc;	/* we are in the interrupt handler but have still enough heap reserve */
#else
			sys_panic("GC is disabled");
#endif
		}
		/* not in interrupt handler -> GC possible */
#endif				/* KERNEL || JAVASCHEDULER */

#ifdef NOTICE_GC
		printf("\n GC in %p (%s) [Thread: %p (%s) caller=", domain, domain->domainName, curthr(), curthr()->name);
#if 0
		{
			code_t ip = getCallerCallerIP();
			print_eip_info(ip);
		}
		printf("]\n");
		printStackTraceNew("GC");
#endif				/*0 */
#endif
#ifndef ENABLE_GC
		sys_panic("Attempt to GC with GC disabled");
#endif

#ifdef VERBOSE_GC
		printf("\nDomain %p (%s) consumed %d bytes of heap space. Starting GC...\n", domain, domain->domainName,
		       (char *) GCM_ORG(domain).heapTop - (char *) GCM_ORG(domain).heap);
#endif
#ifdef PROFILE_AGING
		paGCStart(domain, memTimeNext);
#endif
		/*GC_UNLOCK; */
#ifdef ENABLE_GC
		if (domain->gc.gcThread == NULL)
			domain_panic(curdom(), "GC but no GC thread availabke");
		start_thread_using_code1(domain->gc.gcObject, domain->gc.gcThread, domain->gc.gcCode, (u4_t) domain);
#endif

		//executeInterface(domain, "jx/zero/GarbageCollector", "gc", "()V", domain->gcProxy, 0, 0);
		/*GC_LOCK; */

#ifdef VERBOSE_GC
		printf("    Live bytes: %d\n", (char *) GCM_ORG(domain).heapTop - (char *) GCM_ORG(domain).heap);
#endif
#ifdef PROFILE_AGING
		// prevent gc from running to often while profiling aging
		memTimeNext = domain->gc.memTime + 1;
#endif
		goto try_alloc;
	}
#if defined(KERNEL) || defined(JAVASCHEDULER)
      do_alloc:
#endif

#ifdef PROFILE_AGING
	paNew(domain, objSize, ptr2ObjectDesc(GCM_ORG(domain).heapTop));
#endif

#ifdef PROFILE_SAMPLE_HEAPUSAGE
	profile_sample_heapusage_alloc(domain objSize);
#endif				/* PROFILE_SAMPLE_HEAPUSGAE */

	data = (jint *) GCM_ORG(domain).heapTop;
	GCM_ORG(domain).heapTop = nextObj;

	ASSERT(data != NULL);
	memset(data, 0, objSize * 4);

	obj = ptr2ObjectDesc(data);
	setObjFlags(obj, flags);
#ifdef USE_QMAGIC
	setObjMagic(obj, MAGIC_OBJECT);
#endif

	handle = registerObject(domain, obj);

#ifdef CHECK_HEAP_AFTER_ALLOC
	{
		// last object on heap is not properly initialized (no vtable)
		u4_t *tmpptr = GCM_ORG(domain).heapTop;
		GCM_ORG(domain).heapTop -= objSize;
		if (!gc_org_checkHeap(domain, JNI_FALSE) == JNI_TRUE)
			sys_panic("inconsistent heap");
		GCM_ORG(domain).heapTop = tmpptr;
	}
#endif				/* CHECK_HEAP_AFTER_ALLOC */

	GC_UNLOCK;

	return handle;
}

void gc_org_scan_stack(DomainDesc * domain, ThreadDesc * thread, jboolean testOnly)
{
	int i, k;		//j,ret;
	u4_t *ebp, *eip, *sp, *s, *prevSP;
	ClassDesc *classInfo;
	MethodDesc *method, *prevMethod;
	char *sig;
	jint bytecodePos, lineNumber;
	//char *proxyMethod, *proxySig;
	//Class *proxyClass;
	//SymbolDescStackMap *sym;
	//jbyte b;
	//jbyte *addr;
	//char *symip;
	jint numSlots;
	jbyte stackmap[128];
	extern char _start[], end[];


	//  printStackTrace("XX", thread, thread->context[PCB_EBP]);

	prevMethod = NULL;
	prevSP = NULL;

	ebp = (u4_t *) thread->context[PCB_EBP];
	sp = (u4_t *) thread->context[PCB_ESP];
	eip = (u4_t *) thread->context[PCB_EIP];

//  sp = thread->context[PCB_EBP];
//  ebp = (u4_t*)*sp;
//  eip = (u4_t*)*(sp+1);
	while (sp != NULL && sp < thread->stackTop) {

		prevMethod = method;
		//printf("start=%p end=%p\n", _start, end);
		if (eip >= (u4_t *) _start && eip <= (u4_t *) end) {
			PGCB(STACK1);
			/* our own text segment */
			/*printf("NATIVE "); print_eip_info(eip); printf("\n"); */
#ifdef DBG_STACKMAP
			printf("NATIVE\n");
#endif
			if (eip_in_last_stackframe((u4_t) eip)) {
				PGCE(STACK1);
				if (*(eip - 1) == 0xfb)
					sys_panic("FIXME: interrupted before call in callnative_special_portal");
#ifdef DBG_STACKMAP
				printf("  Last interesting frame on this stack\n");
#endif
				break;	// no more stack frames
			}
			if ((eip >= (u4_t *) callnative_special && eip <= (u4_t *) callnative_special_end)
			    || (eip >= (u4_t *) callnative_static && eip <= (u4_t *) callnative_static_end)
			    || (eip >= (u4_t *) callnative_special_portal && eip <= (u4_t *) callnative_special_portal_end)
//#ifdef JAVASCHEDULER
			    || (eip >= (u4_t *) return_from_java0 && eip <= (u4_t *) return_from_javaX_end)
			    || (eip >= (u4_t *) return_from_java1 && eip <= (u4_t *) return_from_javaX_end)
			    || (eip >= (u4_t *) return_from_java2 && eip <= (u4_t *) return_from_javaX_end)
			    || (eip >= (u4_t *) never_return && eip <= (u4_t *) never_return_end)
//#endif
			    ) {
				/* C -> Java */
				/*  scan parameters */

#ifdef DBG_STACKMAP
				if (prevMethod)
					printf("  C -> Java control transfer; previous method %s %s  numArgs=%d\n",
					       prevMethod->name, prevMethod->signature, (int) prevMethod->numberOfArgs);
				else
					printf("  C -> Java control transfer; unknown previous method \n");

				printf("  %p:  x %p RET\n", ebp + 1, (void *) *(ebp + 1));
				printf("  %p:  x %p EBP\n", ebp, (void *) *ebp);
#endif

				if (prevMethod != NULL) {
					u4_t *s = sp;
					s += 2;
					/* callnative_static  does not put an ObjectDesc onto the stack */
					if (!(eip >= (u4_t *) callnative_static && eip <= (u4_t *)
					      callnative_static_end)) {
#ifdef DBG_STACKMAP
						printf("  self:   %p %p ", s, (void *) *s);
#endif
						if (!testOnly) {
							gc_org_move_reference(domain, (ObjectDesc **) s);
#ifdef DBG_STACKMAP
							printf("  moved to %p", (void *) *s);
#endif
						}
#ifdef DBG_STACKMAP
						printf("\n");
#endif
						s++;
					}
					for (i = 1; i < prevMethod->numberOfArgs + 1; i++) {
						if (isRef(prevMethod->argTypeMap, prevMethod->numberOfArgs, i - 1)) {
#ifdef DBG_STACKMAP
							printf("  %d:   1  %p %p ", i, s, (void *) *s);
#endif
							if (!testOnly) {
								gc_org_move_reference(domain, (ObjectDesc **)
										      s);
#ifdef DBG_STACKMAP
								printf("  moved to %p", (void *)
								       *s);
#endif
							}
						} else {
#ifdef DBG_STACKMAP
							if ((*s >= GCM_ORG(domain).heap)
							    && (*s < GCM_ORG(domain).heapBorder)) {
								printf(" maybe reference");
							}
							printf("  %d:   0  %p %p ", i, s, (void *) *s);
							printf("  not moved");
#endif
						}
						s++;
#ifdef DBG_STACKMAP
						printf("\n");
#endif
					}

#ifdef DBG_STACKMAP
					for (s = ebp - 1, k = 0; s > (sp + 1); s--, k++) {
						char *ptr = (char *) *s;
						printf("  %p:   %p   (ebp=%p)\n", s, ptr, ebp);
					}
#endif
				}
				// break; /* no more Java stack frames; NOT TRUE: newString, executeSpecial, ... */
//#ifdef JAVASCHEDULER
				/* no more Java stack frames for these functions: */
				if ((eip >= (u4_t *) return_from_java0 && eip <= (u4_t *) return_from_javaX_end)
				    || (eip >= (u4_t *) return_from_java1 && eip <= (u4_t *) return_from_javaX_end)
				    || (eip >= (u4_t *) return_from_java2 && eip <= (u4_t *) return_from_javaX_end)
				    || (eip >= (u4_t *) never_return && eip <= (u4_t *) never_return_end)
				    )
					break;
//#endif

			} else {
#ifdef DBG_STACKMAP
				char *cname = findCoreSymbol((jint) eip);
				if (cname != NULL) {
					printf("Skipping core frame:%s\n", cname);
					if (!(eip >= (u4_t *) thread_exit && eip <= (u4_t *) (char *) thread_exit + 48)) {	/* last frame; ebp and ret are invalid */
						printf("  %p:  x %p RET\n", ebp + 1, (void *) *(ebp + 1));
						printf("  %p:  x %p EBP\n", ebp, (void *) *ebp);
						for (s = ebp - 1, k = 0; s > (sp + 1); s--, k++) {
							char *ptr = (char *) *s;
							printf("  %p:   %p\n", s, ptr);
						}
					}
				} else {
					printf("Unknown code at address 0x%lx\n", (u4_t) eip);
					printf("  %p:  x %p RET\n", ebp + 1, (void *) *(ebp + 1));
					printf("  %p:  x %p EBP\n", ebp, (void *) *ebp);
					for (s = (u4_t *) (ebp - 1), k = 0; s > (sp + 1); s--, k++) {
						char *ptr = (char *) *s;
						printf("  %p:   %p\n", s, ptr);
					}
					sys_panic("");
				}
#endif
			}


			PGCE(STACK1);
		} else {
			int q;
#ifdef PROFILE_GC
			PGCB(STACK3);
#endif
			q = findMethodAtAddrInDomain(domain, (char *) eip, &method, &classInfo, &bytecodePos, &lineNumber);
#ifdef PROFILE_GC
			PGCE(STACK3);
#endif
			if (q == 0) {
				PGCB(STACK2);
#ifdef DBG_STACKMAP
				printf("%s::%s%s at BC=%ld LINE=%d (codestart=%p:eip=%p)\n", classInfo->name, method->name,
				       method->signature, bytecodePos, (int) lineNumber, method->code, eip);
#endif

				if (!find_stackmap(method, eip, ebp, stackmap, sizeof(stackmap), &numSlots)) {
					printf("No stackmap for this frame! at %p; thread=%p\n", eip, thread);
					list_stackmaps(method);
					sys_panic("No stackmap for this frame!");
				}
#ifdef DBG_STACKMAP
				printf("  %p:  x %8p RET\n", ebp + 1, (void *) *(ebp + 1));
				printf("  %p:  x %8p EBP\n", ebp, (void *) *ebp);
#endif
				for (s = ebp - 1, k = 0; s > (sp + 1); s--, k++) {
					//char* sm;
#ifdef DBG_STACKMAP
					char *ptr = (char *) *s;
#endif
					if (k >= numSlots) {
#ifdef DBG_STACKMAP
						printf("  %p:  ? %8p", s, ptr);
#endif
					} else {
#ifdef DBG_STACKMAP
						printf("  %p:  %d %8p", s, stackmap[k], ptr);
						if (!stackmap[k]) {
							printf(" %d", (int) ptr);
						}
#endif
					}
#ifdef DBG_STACKMAP
					if (ptr >= (char *) GCM_ORG(domain).heap && ptr <= (char *) GCM_ORG(domain).heapTop) {
						printf(" HEAP ");

#ifdef USE_QMAGIC
						if (getObjMagic(ptr) == MAGIC_OBJECT) {
							ClassDesc *oclass = obj2ClassDesc(ptr);
							printf("%s", oclass->name);
						}
#endif
					} else {
						if (!stackmap[k]) {
							printf(" %d", (int) ptr);
						} else {
							if (ptr == (char *)
							    getInitialNaming()) {
								printf(" InitialNaming");
							}
						}
					}

#endif				/* DBG_STACKMAP */
					if (!testOnly) {
						if (stackmap[k]) {	/* found reference */
							gc_org_move_reference(domain, (ObjectDesc **) s);
#ifdef DBG_STACKMAP
							printf("  moved to %p", (void *) *s);
#endif
						} else {
#ifdef DBG_STACKMAP
							if ((*s >= GCM_ORG(domain).heap)
							    && (*s < GCM_ORG(domain).heapBorder)) {
								printf(" maybe reference");
							}
							printf("  not moved");
#endif
						}
					}
#ifdef DBG_STACKMAP
					printf("\n");
#endif
				}
				PGCE(STACK2);
			} else {
				int q;
#ifdef PROFILE_GC
				PGCB(STACK4);
#endif
				q = findProxyCodeInDomain(domain, eip, &method, &sig, &classInfo);
#ifdef PROFILE_GC
				PGCE(STACK4);
#endif
				if (q == 0) {
#ifdef DBG_STACKMAP
					printf("PROXY\n");
					printf("  %p:  x %p RET\n", ebp + 1, (void *) *(ebp + 1));
					printf("  %p:  x %p EBP\n", ebp, (void *) *ebp);
#endif
				} else {
					printf("Warning: Strange eip thread=%ld.%ld (%p) eip=%p\n", TID(thread), thread, eip);
					//sys_panic("Strange instruction pointer");
				}
			}
		}
		prevSP = sp;

		sp = ebp;
		if (sp == NULL)
			break;
		ebp = (u4_t *) * sp;
		eip = (u4_t *) * (sp + 1);
	}
}



/*
 * Scan heap
 */
void gc_org_scan_heap2(DomainDesc * domain)
{
	ObjectDesc *obj;
	ArrayDesc *ar;
	//jbyte *addr;
	//jint k;
	jint *data;
	ClassDesc *c;
	//Class *cl;
	jint flags;
	//jbyte b;
	u4_t objSize;
	//ObjectDesc *ptr;
	//u4_t *olddata;

	data = GCM_ORG(domain).heap2;
	while (data < GCM_ORG(domain).heapTop2) {
		flags = getObjFlags(ptr2ObjectDesc(data));
		flags &= FLAGS_MASK;

#ifdef DBG_SCAN_HEAP2
#ifdef USE_FMAGIC
		printf("addr=%p flags=0x%lx magic=0x%lx\n", data + 2, flags, data[1]);
#else
		printf("addr=%p flags=0x%lx\n", data + 1, flags);
#endif
#endif
		switch (flags) {
		case OBJFLAGS_ARRAY:{
				ar = (ArrayDesc *) ptr2ObjectDesc(data);
				gc_org_move_array(domain, &ar, JNI_TRUE);
#ifdef DBG_SCAN_HEAP2
				printf("  arr size=%d\n", ar->size);
#endif
				objSize = gc_objSize2(ptr2ObjectDesc(data), flags);
				data += objSize;
				break;
			}
		case OBJFLAGS_OBJECT:{
				obj = ptr2ObjectDesc(data);
				c = obj2ClassDesc(obj);
				ASSERTCLASSDESC(c);
#ifdef DBG_GC
				printf("Object %p  %s\n", obj, c->name);
#endif
				gc_org_move_object(domain, &obj, JNI_TRUE);
				objSize = OBJSIZE_OBJECT(c->instanceSize);
				data += objSize;
				break;
			}
		case OBJFLAGS_PORTAL:{
				objSize = OBJSIZE_PORTAL;
				data += objSize;
				break;
			}
		case OBJFLAGS_MEMORY:{
				obj = ptr2ObjectDesc(data);
				gc_org_move_memory(domain, &obj, JNI_TRUE);
				objSize = OBJSIZE_MEMORY;
				data += objSize;
				break;
			}
		case OBJFLAGS_SERVICE:{
				obj = ptr2ObjectDesc(data);
				gc_org_move_service(domain, &obj, JNI_TRUE);
				objSize = OBJSIZE_SERVICEDESC;
				data += objSize;
				break;
			}
		case OBJFLAGS_ATOMVAR:{
				obj = ptr2ObjectDesc(data);
				gc_org_move_atomvar(domain, &obj, JNI_TRUE);
				objSize = OBJSIZE_ATOMVAR;
				data += objSize;
				break;
			}
		case OBJFLAGS_CAS:{
				obj = ptr2ObjectDesc(data);
				gc_org_move_cas(domain, &obj, JNI_TRUE);
				objSize = OBJSIZE_CAS;
				data += objSize;
				break;
			}
		default:
			printf("FLAGS: %lx\n", flags);
			obj = ptr2ObjectDesc(data);
			dump_data(obj);
			sys_panic("WRONG HEAP DATA");
		}
	}
}				/*
				   * Scan statics of one class
				 */
//static DomainDesc *gc_domain;
static void scan_class_org(DomainDesc * domain, Class * cl)
{
	jbyte *addr;
	jbyte b = 0;
	jint k;
	ClassDesc *c = cl->classDesc;
#ifdef DBG_GCSTATIC
	if (c->staticFieldsSize) {
		printf("   scan class %s\n", cl->classDesc->name);
	}
#endif				/*DBG_GCSTATIC */
	addr = c->staticsMap;
	for (k = 0; k < c->staticFieldsSize; k++) {
		if (k % 8 == 0)
			b = *addr++;
		if (b & 1) {
			/* reference slot */
#ifdef DBG_GCSTATIC
			printf("      %d found ref %p\n", k, cl->staticFields[k]);
#endif
			gc_org_move_reference(domain, (ObjectDesc **) & (cl->staticFields[k]));
		} else {
#ifdef DBG_GCSTATIC
			printf("      %d numeric\n", k);
#endif
		}
		b >>= 1;
	}

}

#ifdef TIMER_EMULATION
extern AtomicVariableProxy *atomic;
#endif

void gc_org_gc(DomainDesc * domain)
{
#ifdef ENABLE_GC
	//ObjectDesc **s;
	ThreadDesc *t;
	//ClassDesc *classInfo;
	//MethodDesc *method;
	//jint bytecodePos;
	jint *htmp;
	DEPDesc *d;
	jint i;
	//jint heap_bytes;

#ifdef VERBOSE_GC
	printf("GARBAGE COLLECTOR started for domain %p (%s)\n", domain, domain->domainName);
#endif

	ASSERTCLI;

#if 0
#ifdef KERNEL
	DISABLE_IRQ;
#else
#ifdef DEBUG
	{
		sigset_t set, oldset;
		sigemptyset(&set);
		sigprocmask(SIG_BLOCK, &set, &oldset);
		if (!sigismember(&oldset, SIGALRM))
			sys_panic("signal must be blocked during GC");

	}
#endif	 /*DEBUG*/
#endif
#endif
#ifdef PROFILE_GC
	    pgcNewRun(domain);
#endif
	PGCB(GC);
#ifndef KERNEL
#ifdef MPROTECT_HEAP
	if (mprotect
	    (GCM_ORG(domain).heap2, (char *) (GCM_ORG(domain).heapBorder2) - (char *) (GCM_ORG(domain).heap2),
	     PROT_READ | PROT_WRITE) == -1) {
		perror("unprotecting  new heap semispace");
		sys_panic("");
	}
#endif				/* MPROTECT_HEAP */
#endif


	/* 
	 * Init
	 */
#ifdef DBG_GC
#ifdef CHECK_HEAPUSAGE
	/* check whether heap is consistent */
	printf("Checking Heap...");
	if (gc_org_checkHeap(domain, JNI_FALSE) == JNI_TRUE)
		printf("OK\n");
	else
		printf("FAILED\n");
#endif
#endif

#ifdef CHECK_HEAPUSAGE
	gc_checkHeap(domain, JNI_FALSE);
#endif

	/*
	 * Scan stacks
	 */
#ifdef DBG_GC
	printf("checking thread Positions ...\n");
#endif
	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		if (t == domain->gc.gcThread)
			continue;	/* don't scan my own stack */
		if (t->isInterruptHandlerThread)
			continue;	/* don't scan interrupt stacks, they can not be interrupted by a GC and so they are not active */
		if (t->state == STATE_AVAILABLE)
			continue;	/* don't scan stack of available threads */
		check_thread_position(domain, t);
	}

	PGCB(SCAN);
	PGCB(STACK);
#ifdef DBG_GC
	printf("Scanning stacks...\n");
#endif
	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		if (t == domain->gc.gcThread)
			continue;	/* don't scan my own stack */
		if (t->isInterruptHandlerThread)
			continue;	/* don't scan interrupt stacks, they can not be interrupted by a GC and so they are not active */
		if (t->state == STATE_AVAILABLE)
			continue;	/* don't scan stack of available threads */
#ifdef DBG_STACKMAP
		printf("\n  Thread %p (%s)\n", t, t->name);
#endif

#ifdef DBG_GC
		printf("    Test map...\n");
		gc_org_scan_stack(domain, t, JNI_TRUE);
		printf("    Scan...\n");
#endif
		gc_org_scan_stack(domain, t, JNI_FALSE);
	}
	PGCE(STACK);

	/*
	 * Scan statics 
	 */
	PGCB(STATIC);
#ifdef DBG_GC
	printf("Scanning classes...\n");
#endif
	{
		jint k;
		jint i;
		for (k = 0; k < domain->numberOfLibs; k++) {
			LibDesc *lib = domain->libs[k];
			for (i = 0; i < lib->numberOfClasses; i++) {
				scan_class_org(domain, &(lib->allClasses[i]));
			}
		}
	}
	PGCE(STATIC);
	/*
	 * Scan services
	 */
	PGCB(SERVICE);
#ifdef DBG_GC
	printf("Scanning portals...\n");
#endif
	/* TODO: perform GC on copy of service table and use locking only to reinstall table 
	 * all entries of original table must be marked as changing
	 */
	LOCK_SERVICETABLE;
	for (i = 0; i < MAX_SERVICES; i++) {
		d = domain->services[i];
		if (d == SERVICE_ENTRY_FREE || d == SERVICE_ENTRY_CHANGING)
			continue;
		gc_org_move_reference(domain, (ObjectDesc **) & (domain->services[i]));
	}
#ifdef NEW_PORTALCALL
	for (i = 0; i < MAX_SERVICES; i++) {
		if (domain->pools[i]) {
			gc_org_move_reference(domain, (ObjectDesc **) & (domain->pools[i]));
		}
	}
#endif				/* NEW_PORTALCALL */

	UNLOCK_SERVICETABLE;
	PGCE(SERVICE);

	/*
	 * Scan registered 
	 */
	PGCB(REGISTERED);
#ifdef DBG_GC
	printf("Scanning registered...\n");
#endif

	for (i = 0; i < MAX_REGISTERED; i++) {
		if (domain->gc.registeredObjects[i] != NULL) {
			printf("register obj in domain %ld object %p\n", domain->id, &(domain->gc.registeredObjects[i]));
			gc_org_move_reference(domain, &(domain->gc.registeredObjects[i]));
		}
	}

	PGCE(REGISTERED);


	/*
	 * Scan special 
	 */
	PGCB(SPECIAL)
#ifdef DBG_GC
	    printf("Scanning special...\n");
#endif
	if (domain->gc.gcObject != NULL)
		gc_org_move_reference(domain, (ObjectDesc **) & (domain->gc.gcObject));
	if (domain->startClassName)
		gc_org_move_reference(domain, (ObjectDesc **) & (domain->startClassName));
	if (domain->dcodeName != NULL)
		gc_org_move_reference(domain, (ObjectDesc **) & (domain->dcodeName));
	if (domain->libNames != NULL)
		gc_org_move_reference(domain, (ObjectDesc **) & (domain->libNames));
	if (domain->argv != NULL)
		gc_org_move_reference(domain, (ObjectDesc **) & (domain->argv));
	if (domain->initialPortals != NULL)
		gc_org_move_reference(domain, (ObjectDesc **) & (domain->initialPortals));
	if (domain->initialNamingProxy != NULL)
		gc_org_move_reference(domain, (ObjectDesc **) & (domain->initialNamingProxy));
#ifdef TIMER_EMULATION
	if ((atomic != NULL)
	    && gc_org_isInHeap(domain, (ObjectDesc *) atomic))
		gc_org_move_reference(domain, (ObjectDesc **) & atomic);
#endif
#ifdef JAVASCHEDULER
	/* move references to HLscheduler objects */
	/* FIXME: 1) domain-control-blocks of other domains are touched
	   2) very expensive (all domains are visited) */

	foreachDomain(gc_org_moveSchedulerObject);

	/* move references to LLscheduler objects */
	for (i = 0; i < MAX_NR_CPUS; i++)
		if (domain == CpuInfo[i]->LowLevel.SchedThread->domain)
			if (CpuInfo[i]->LowLevel.SchedObj != NULL) {
				gc_dprintf("move LLScheduler %d\n", i);
				gc_org_move_reference(domain, &(CpuInfo[i]->LowLevel.SchedObj));
			}
#endif

	/*
	 *  object references in domain control block 
	 */
	if (domain->naming) {
		gc_dprintf("move naming\n");
		gc_org_move_reference(domain, (ObjectDesc **) & (domain->naming));
	}
#ifdef PORTAL_INTERCEPTOR
	if (domain->outboundInterceptorObject) {
		gc_dprintf("move interceptor\n");
		gc_org_move_reference(domain, (ObjectDesc **) & (domain->outboundInterceptorObject));
	}
	if (domain->inboundInterceptorObject) {
		gc_dprintf("move interceptor\n");
		gc_org_move_reference(domain, (ObjectDesc **) & (domain->inboundInterceptorObject));
	}
#endif				/* PORTAL_INTERCEPTOR */


	/* object references in thread control block */
	/* portal call parameters in TCB */

	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		/* data for incoming call */
		if (t->processingDEP) {
			gc_dprintf("move processingDEP\n");
			gc_org_move_reference(domain, (ObjectDesc **) & (t->processingDEP));
		}
		if (t->portalParameter) {
			gc_dprintf("move portalParameter\n");
			gc_org_move_reference(domain, (ObjectDesc **) & (t->portalParameter));
		}
		if (t->entry) {
			gc_dprintf("move entry\n");
			gc_org_move_reference(domain, (ObjectDesc **) & (t->entry));
		}
		if (PORTAL_RETURN_IS_OBJECT(t->portalReturnType)
		    && t->portalReturn) {
			gc_dprintf("move portalReturn\n");
			gc_org_move_reference(domain, (ObjectDesc **) & (t->portalReturn));
		}

		/* reference table that is used to detect cycles during parameter copying */
#ifdef NO_RESTART
		{
			u4_t n, i;
			n = curthr()->n_copied;
			for (i = 0; i < n; i++) {
				gc_org_move_reference(domain, &(curthr()->copied[i].dst));
			}
		}
#endif
#if 0
		/* data for outgoing call */
		if (t->state == STATE_WAITPORTAL0 || t->state == STATE_WAITPORTAL2) {
			/* Situation: thread is blocked waiting for portal call and parameters are not yet copied
			 * to target domain */
#ifdef DBG_GC
			printf("Move portal call parameters of thread %p\n", t);
#endif
			{
				jint **paramlist = (jint **) t->depParams;
				u4_t i;
				Proxy *proxy = *(Proxy **) paramlist;
				ClassDesc *oclass = obj2ClassDesc((ObjectDesc *) proxy);
				u4_t methodIndex = t->depMethodIndex;
				u4_t numberArgs = oclass->methodVtable[methodIndex]->numberOfArgs;
				jbyte *argTypeMap = oclass->methodVtable[methodIndex]->argTypeMap;
#ifdef DBG_GC
				printf("%s %s\n", oclass->methodVtable[methodIndex]->name,
				       oclass->methodVtable[methodIndex]->signature);
#endif
				for (i = 1; i < numberArgs + 1; i++) {
					if (isRef(argTypeMap, numberArgs, i - 1)) {
						printf("   PORTALPARAM REF\n");
						if ((void *) t->depParams[i] == NULL)
							continue;
						gc_org_move_reference(domain, (ObjectDesc **) t->myparams + i);
					} else {
						printf("   PORTALPARAM NUM\n");
					}
				}
			}
		}
#endif
		/* data for incoming call */
		if (t->isPortalThread) {
#ifdef DBG_GC
			printf("Move portal call in-parameters of thread %p\n", t);
#endif
			{
				ThreadDesc *source = t->mostRecentlyCalledBy;
				if (source != NULL) {
					//u4_t numberArgs = (jint**)source->depNumParams;
					u4_t i;
					ObjectDesc *obj;
					ClassDesc *oclass;
					u4_t methodIndex, numberArgs;
					jbyte *argTypeMap;
					DEPDesc *dep;
					dep = t->processingDEP;
					obj = dep->obj;
					oclass = obj2ClassDesc(obj);
					methodIndex = source->depMethodIndex;
					numberArgs = oclass->methodVtable[methodIndex]->numberOfArgs;
					argTypeMap = oclass->methodVtable[methodIndex]->argTypeMap;
#ifdef DBG_GC
					printf("methodindex=%ld, numberargs: %ld\n", methodIndex, numberArgs);
					printf("source %p \n", source);
					printf("obj %p \n", obj);
					printf("class %p %s method %s\n", oclass, oclass->name,
					       oclass->methodVtable[methodIndex]->name
					       /*, oclass->methodVtable[methodIndex]->signature */
					    );
#endif
					gc_org_move_reference(domain, (ObjectDesc **) & (t->myparams[0]));
					if (t->depParams != NULL) {
						for (i = 1; i < numberArgs + 1; i++) {
							if (isRef(argTypeMap, numberArgs, i - 1)) {
								//printf("   INPORTALPARAM REF\n");
								if ((void *) t->depParams[i] == NULL)
									continue;
								gc_org_move_reference(domain, (ObjectDesc **) & (t->myparams[i]));
							} else {
								// printf("   INPORTALPARAM NUM\n");
							}
						}
					}
#ifdef DBG_GC
					printf("DONE inportal %p\n", (void *) (t->myparams[0]));
#endif
				}
			}
		}



	}
	PGCE(SPECIAL);
	/* Interrupt handler objects */
	PGCB(INTR);
	{
		u4_t i, j;
#ifdef SMP
		sys_panic("GC only tested for single CPU systems");
#endif
		for (i = 0; i < MAX_NR_CPUS; i++) {	/* FIXME: must synchronize with these CPUs!! */
			for (j = 0; j < NUM_IRQs; j++) {
				if (ifirstlevel_object[i][j] != NULL && idomains[i][j] == domain) {
					gc_dprintf("move interrupt handler\n");
					gc_org_move_reference(domain, &(ifirstlevel_object[i][j]));
				}
			}
		}
	}
	PGCE(INTR);

#if 0
	/* domainzero name server !! DO NOT GC IT. LOCAL OBJECTS OF DOMAINZERO! */
#ifdef DBG_GC
	printf("Scanning domainzero name server\n");
#endif
	{
		struct nameValue_s *n;
		for (n = nameValue; n != NULL; n = n->next) {
			if (n->domain == domain) {
				move_reference(domain, &n->obj);
			}
		}
	}
#endif




	/*
	 * All directly reachable objects are now on the new heap
	 * Scan new heap 
	 */
#ifdef DBG_GC
	printf("Scanning new heap ...\n");
#endif				/* DBG_GC */
	PGCB(HEAP);
	gc_org_scan_heap2(domain);
	PGCE(HEAP);

	/*
	 * Finish
	 */
#ifdef DBG_GC
	printf("Finalize memory objects ...\n");
#endif
	gc_org_finalizeMemory(domain);
	gc_org_finalizePortals(domain);


#ifdef DBG_GC
	printf("Invalidating all objects of old heap");
#endif				/* DBG_GC */
#ifdef CHECK_HEAPUSAGE
	gc_checkHeap(domain, JNI_TRUE);	/* invalidate all objects */
#endif

	htmp = GCM_ORG(domain).heap;
	GCM_ORG(domain).heap = GCM_ORG(domain).heap2;
	GCM_ORG(domain).heap2 = htmp;

	htmp = GCM_ORG(domain).heapBorder;
	GCM_ORG(domain).heapBorder = GCM_ORG(domain).heapBorder2;
	GCM_ORG(domain).heapBorder2 = htmp;

	GCM_ORG(domain).heapTop = GCM_ORG(domain).heapTop2;
	GCM_ORG(domain).heapTop2 = GCM_ORG(domain).heap2;	/* heap2 is empty */


#if 0
	/* TEST */
	htmp = GCM_ORG(domain).heap;
	GCM_ORG(domain).heap = GCM_ORG(domain).heap2;
	GCM_ORG(domain).heap2 = GCM_ORG(domain).heap3;
	GCM_ORG(domain).heap3 = htmp;

	htmp = GCM_ORG(domain).heapBorder;
	GCM_ORG(domain).heapBorder = GCM_ORG(domain).heapBorder2;
	GCM_ORG(domain).heapBorder2 = GCM_ORG(domain).heapBorder3;
	GCM_ORG(domain).heapBorder3 = htmp;

	GCM_ORG(domain).heapTop = GCM_ORG(domain).heapTop3;
	GCM_ORG(domain).heapTop3 = GCM_ORG(domain).heap3;	/* heap3 is empty */
#endif
	PGCE(SCAN);
#ifndef KERNEL
#ifdef DBG_GC
	printf("Protecting %p..%p\n", GCM_ORG(domain).heap2, GCM_ORG(domain).heapBorder2);
#endif				/* DBG_GC */
	PGCB(PROTECT);

#ifdef MPROTECT_HEAP
	if (mprotect(GCM_ORG(domain).heap2, (char *) (GCM_ORG(domain).heapBorder2) - (char *) (GCM_ORG(domain).heap2), PROT_NONE)
	    == -1) {
		perror("protecting old heap semispace");
		sys_panic("");
	}
#endif				/* MPROTECT_HEAP */

	PGCE(PROTECT);
#endif

#ifdef DEBUG
#ifdef DBG_GC
	printf("Checking new heap\n");
	if (gc_org_checkHeap(domain, JNI_FALSE) == JNI_TRUE) {	/* check all objects */
		printf("    heap check passed\n");
	} else {
		sys_panic("ERROR");
	}
	printf("Checking corrected stacks...\n");
	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		if (t == domain->gc.gcThread)
			continue;	/* don't scan my own stack */
		if (t->isInterruptHandlerThread)
			continue;	/* don't scan interrupt stacks, they can not be interrupted by a GC and so they are not active */
		if (t->state == STATE_AVAILABLE)
			continue;	/* don't scan stack of available threads */
		printf("  Thread %p (%s)\n", t, t->name);
		gc_org_scan_stack(domain, t, JNI_TRUE);
	}

#endif

#ifdef VERBOSE_GC
#ifdef PROFILE_HEAPUSAGE
	printHeapUsage(domain);
#endif
#endif

#endif				/* DEBUG */
	PGCE(GC);

#ifdef VERBOSE_GC
	printf("GC finished\n");
#endif				/* VERBOSE_GC */


	/* sys_panic("GC only partially implemented"); */
#endif
}

void gc_org_done(DomainDesc * domain)
{
	u4_t size;

	/* free heap */
	size = (GCM_ORG(domain).heapBorder - GCM_ORG(domain).heap) * 4;
	jxfree(GCM_ORG(domain).heap, size MEMTYPE_HEAP);
	GCM_ORG(domain).heap = GCM_ORG(domain).heapBorder = GCM_ORG(domain).heapTop = NULL;

#ifdef ENABLE_GC
	jxfree(GCM_ORG(domain).heap2, size MEMTYPE_HEAP);
	GCM_ORG(domain).heap2 = GCM_ORG(domain).heapBorder2 = GCM_ORG(domain).heapTop2 = NULL;
#endif				/* ENABLE_GC */

}

#ifdef PROFILE_HEAPUSAGE
/*
 * Count number of instances of all classes
 */
void gc_org_count_instances(DomainDesc * domain, InstanceCounts_t * counts)
{
	ObjectDesc *obj;
	ArrayDesc *ar;
	//jbyte *addr;
	//jint k;
	jint *data;
	ClassDesc *c;
	//Class *cl;
	jint flags;
	//jbyte b;
	u4_t objSize;

	counts->objbytes = 0;
	counts->arrbytes = 0;
	counts->portalbytes = 0;
	counts->memproxybytes = 0;
	counts->cpustatebytes = 0;
	counts->atomvarbytes = 0;
	counts->servicebytes = 0;
	counts->casbytes = 0;

	data = GCM_ORG(domain).heap;
	while (data < GCM_ORG(domain).heapTop) {
		//printf("DATA=%p ",data);
		flags = getObjFlags(ptr2ObjectDesc(data));
		flags &= FLAGS_MASK;
		//printf(" FLAGS=%p\n",flags);
		switch (flags) {
		case OBJFLAGS_ARRAY:{
				ar = (ArrayDesc *) ptr2ObjectDesc(data);
				//printf("%p %d\n",ar, ar->size);
				/* TODO */
				/* ... */
				objSize = gc_objSize2(ptr2ObjectDesc(data), flags);

				counts->arrbytes += objSize * 4;
				data += objSize;
#ifdef PROFILE_HEAPUSAGE
				ar->arrayClass->n_arrayelements += objSize;
#endif
				//printf("ARR: %p %s bytes=%ld sum=%ld\n", ar->arrayClass, ar->arrayClass->name, objSize * 4, ar->arrayClass->n_arrayelements*4);
				break;
			}
		case OBJFLAGS_EXTERNAL_STRING:
			ASSERT(domain == domainZero);	/* only the heap of DomainZero contains these objects */
		case OBJFLAGS_OBJECT:{
				//printf("%p %p %p\n",data,GCM_ORG(domain).heap,GCM_ORG(domain).heapTop);
#  ifdef USE_FMAGIC
				obj = ptr2ObjectDesc(data);
				if (getObjMagic(obj) != MAGIC_OBJECT) {
					printf("NOMAGIC at %d\n", (char *) data - (char *) GCM_ORG(domain).heap);
					sys_panic("");
					break;
				}
#else
				obj = ptr2ObjectDesc(data);
#endif
				ASSERTOBJECT(obj);
				c = obj2ClassDesc(obj);
				ASSERTCLASSDESC(c);
#ifdef PROFILE_HEAPUSAGE
				c->n_instances++;
#endif
				//printf("%s\n", c->name);
				objSize = OBJSIZE_OBJECT(c->instanceSize);
				data += objSize;
				counts->objbytes += objSize * 4;
				break;
			}
		case OBJFLAGS_PORTAL:{
				objSize = OBJSIZE_PORTAL;
				data += objSize;
				counts->portalbytes += objSize * 4;
				break;
			}
		case OBJFLAGS_MEMORY:{
				objSize = OBJSIZE_MEMORY;
				data += objSize;
				counts->memproxybytes += objSize * 4;
				break;
			}
		case OBJFLAGS_ATOMVAR:{
				objSize = OBJSIZE_ATOMVAR;
				data += objSize;
				counts->atomvarbytes += objSize * 4;
				break;
			}
		case OBJFLAGS_CAS:{
				objSize = OBJSIZE_CAS;
				data += objSize;
				counts->casbytes += objSize * 4;
				break;
			}
		case OBJFLAGS_SERVICE:{
				objSize = OBJSIZE_SERVICEDESC;
				data += objSize;
				counts->servicebytes += objSize * 4;
				break;
			}
		default:
			printf("WRONG HEAP DATA: %lx", flags);
			obj = ptr2ObjectDesc(data);
			dump_data(obj);
			sys_panic("WRONG HEAP DATA");
		}
	}
}
#endif				/* PROFILE_HEAPUSAGE */


void gc_org_finalizePortals(DomainDesc * domain)
{
	sys_panic("not implemented yet");
}

/*
 * Finalize all unreachable memory objects
 */
void gc_org_finalizeMemory(DomainDesc * domain)
{
	ObjectDesc *obj;
	//MemoryProxy *memobj;
	ArrayDesc *ar;
	jint *data;
	ClassDesc *c;
	jint flags;
	u4_t objSize;

	data = GCM_ORG(domain).heap;
	while (data < GCM_ORG(domain).heapTop) {
		flags = getObjFlags(ptr2ObjectDesc(data));
		if ((flags & FORWARD_MASK) == GC_FORWARD) {
			flags = getObjFlags((ObjectDesc *) (flags & FORWARD_PTR_MASK));
		}
		flags &= FLAGS_MASK;
		switch (flags) {
		case OBJFLAGS_ARRAY:{
				ar = (ArrayDesc *) ptr2ObjectDesc(data);
				objSize = gc_objSize2(ptr2ObjectDesc(data), flags);
				data += objSize;
				break;
			}
		case OBJFLAGS_OBJECT:{
				obj = ptr2ObjectDesc(data);
				c = obj2ClassDesc(obj);
				objSize = OBJSIZE_OBJECT(c->instanceSize);
				data += objSize;
				break;
			}
		case OBJFLAGS_PORTAL:{
				objSize = OBJSIZE_PORTAL;
				data += objSize;
				break;
			}
		case OBJFLAGS_MEMORY:{
				struct MemoryProxy_s *memobj = (struct MemoryProxy_s *) ptr2ObjectDesc(data);
				if ((getObjFlags((ObjectDesc *) memobj) & FORWARD_MASK) == GC_FORWARD) {
					//printf("LIVEMEM: %p %d refcount=%d", memobj->mem, memobj->size, memobj->dz->refcount);
				} else {
					//printf("DEADMEM: %p %d refcount=%d", memobj->mem, memobj->size, memobj->dz->refcount);
					ASSERTMEMORY((ObjectDesc *)
						     memobj);
					memory_deleted(memobj);
				}
				//printf(", created at: "); print_eip_info(memobj->dz->createdAt);printf("\n");
				objSize = OBJSIZE_MEMORY;
				data += objSize;
				break;
			}
		case OBJFLAGS_SERVICE:{
				objSize = OBJSIZE_SERVICEDESC;
				data += objSize;
				break;
			}
		case OBJFLAGS_ATOMVAR:{
				objSize = OBJSIZE_ATOMVAR;
				data += objSize;
				break;
			}
		default:
			printf("FLAGS: %lx\n", flags);
			sys_panic("WRONG HEAP DATA");
		}
	}
}


#ifdef FIND_OBJECTS_BY_CLASS
/*
 * Find objects given a class name
 *  TODO: use a GENERIC way to walk the heap (scan_heap,finalize_memory)
 */
void gc_org_findOnHeap(DomainDesc * domain, char *classname)
{
	ObjectDesc *obj;
	struct MemoryProxy_s *memobj;
	ArrayDesc *ar;
	//jbyte *addr;
	//jint k;
	jint *data;
	ClassDesc *c;
	//Class *cl;
	jint flags;
	//jbyte b;
	u4_t objSize;
	//ObjectDesc *ptr;
	//jboolean ok = JNI_TRUE;

	{
		int l;
		c = findClass(domain, classname)->classDesc;
		for (l = 0; l < c->numberFields; l++) {
			printf("%2d %s\n", c->fields[l].fieldOffset, c->fields[l].fieldName);
		}
	}

	data = GCM_ORG(domain).heap;
	while (data < GCM_ORG(domain).heapTop) {
		flags = getObjFlags(ptr2ObjectDesc(data));
		if ((flags & FORWARD_MASK) == GC_FORWARD) {
			flags = getObjFlags((ObjectDesc *) (flags & FORWARD_PTR_MASK));
		}
		flags &= FLAGS_MASK;
		switch (flags) {
		case OBJFLAGS_ARRAY:{
				// FIXME jgbauman magic error USE_FMAGIC
				ar = (ArrayDesc *) ptr2ObjectDesc(data);
				objSize = gc_objSize2(ptr2ObjectDesc(data), flags);
				data += objSize;
				break;
			}
		case OBJFLAGS_OBJECT:{
				obj = ptr2ObjectDesc(data);
				c = obj2ClassDesc(obj);
				objSize = OBJSIZE_OBJECT(c->instanceSize);
				if (strcmp(classname, c->name) == 0) {
					u4_t k = 2;
					u4_t offs;
					printf("OBJECT: %p\n", obj);
#  ifdef USE_FMAGIC
					k++;
#endif
					offs = k;
					for (; k < objSize - 1; k++) {	/* -1, bacause OBJSIZE_OBJECT contains unused size field */
						int l;
						char *fieldname = "???";
						char *fieldtype = "???";
						for (l = 0; l < c->numberFields; l++) {
							if (c->fields[l].fieldOffset == k - offs) {
								fieldname = c->fields[l].fieldName;
								fieldtype = c->fields[l].fieldType;
								break;
							}
						}
						printf("%s %s  0x%lx\n", fieldtype, fieldname, data[k]);
					}
				}
				data += objSize;
				break;
			}
		case OBJFLAGS_PORTAL:{
				objSize = OBJSIZE_PORTAL;
				data += objSize;
				break;
			}
		case OBJFLAGS_MEMORY:{
				memobj = (struct MemoryProxy_s *)
				    ptr2ObjectDesc(data);
				//printf(", created at: "); print_eip_info(memobj->dz->createdAt);printf("\n");
				objSize = OBJSIZE_MEMORY;
				data += objSize;
				break;
			}
		case OBJFLAGS_SERVICE:{
				objSize = OBJSIZE_SERVICEDESC;
				data += objSize;
				break;
			}
		case OBJFLAGS_ATOMVAR:{
				objSize = OBJSIZE_ATOMVAR;
				data += objSize;
				break;
			}
		default:
			printf("FLAGS: %lx\n", flags);
			sys_panic("WRONG HEAP DATA");
		}
	}
}
#endif				/* FIND_OBJECTS_BY_CLASS */

jboolean gc_org_isValidHeapRef(DomainDesc * domain, ObjectDesc * ptr)
{
	jboolean ok = JNI_TRUE;

	// FIXME printf("Strange data on heap at %p, flags=%ld heap=%p..%p\n", ptr, 0, GCM_ORG(domain).heap, GCM_ORG(domain).heapTop);

	if (!(ptr == NULL || (ptr >= (ObjectDesc *) GCM_ORG(domain).heap && ptr <= (ObjectDesc *) GCM_ORG(domain).heapTop))) {
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
			printf("Strange data on heap at %p, flags=%ld heap=%p..%p\n", ptr, flags, GCM_ORG(domain).heap,
			       GCM_ORG(domain).heapTop);
			//sys_panic("");
			oc = obj2ClassDesc(ptr);
			strClass = findClassDesc("java/lang/String");
			if (oc != NULL && oc->name != NULL) {
				if (oc == strClass) {
					if (!(ptr == NULL || (ptr >= (ObjectDesc *)
							      GCM_ORG(domainZero).heap && ptr <= (ObjectDesc *)
							      GCM_ORG(domainZero).heapTop))) {
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
jboolean gc_org_checkHeap(DomainDesc * domain, jboolean invalidate)
{
#ifdef CHECK_HEAPUSAGE
	ObjectDesc *obj;
	ArrayDesc *ar;
	MemoryProxy *mem;
	jbyte *addr;
	jint k;
	jint *data;
	ClassDesc *c;
	//Class *cl;
	jint flags;
	jbyte b = 0;
	u4_t objSize;
	ObjectDesc *ptr;
#ifdef USE_QMAGIC
	u4_t *olddata;
#endif
	jboolean ok = JNI_TRUE;

#ifdef DEBUG
	//    printf("Checking heap inval=%d\n", invalidate);
#endif

	data = GCM_ORG(domain).heap;
	while (data < GCM_ORG(domain).heapTop) {
		flags = getObjFlags(ptr2ObjectDesc(data));
		// FIXME jgbauman
		if ((flags & FORWARD_MASK) == GC_FORWARD) {
#ifdef CHECKHEAP_VERBOSE
			printf("data=%p is forward pointer\n", data);
#endif
			flags = getObjFlags((ObjectDesc *) (flags & FORWARD_PTR_MASK));
		}
		flags &= FLAGS_MASK;
#ifdef USE_QMAGIC
		olddata = data;
#endif
#ifdef CHECKHEAP_VERBOSE
		printf("data=%p flags=%d\n", data, flags);
#endif
		switch (flags) {
		case OBJFLAGS_ARRAY:{
				ar = (ArrayDesc *) ptr2ObjectDesc(data);
#ifdef CHECKHEAP_VERBOSE
				printf("Array %p %d\n", ar, ar->size);
#endif
				/* TODO */
				/* ... */
				objSize = gc_objSize2(ptr2ObjectDesc(data), flags);
#ifdef DEBUG
				if (ar->arrayClass->name[1] == 'L' || ar->arrayClass->name[1] == '[') {
					u4_t i;
					for (i = 0; i < ar->size; i++) {
						if (!gc_isValidHeapRef(domain, (ObjectDesc *) ar->data[i])) {
							sys_panic("Not a reference in array!");
						}
					}
				}
#endif				/* DEBUG */

				// *arrbytes += objSize * 4;
				data += objSize;
#ifdef CHECKHEAP_VERBOSE
				ar->arrayClass->n_arrayelements += objSize;
				printf("ARR: %p %s bytes=%ld sum=%ld\n", ar->arrayClass, ar->arrayClass->name, objSize * 4,
				       ar->arrayClass->n_arrayelements * 4);
#endif
				break;
			}
		case OBJFLAGS_OBJECT:{
#ifdef CHECKHEAP_VERBOSE
				printf("Object %p %p %p\n", data, GCM_ORG(domain).heap, GCM_ORG(domain).heapTop);
#endif
				obj = (ObjectDesc *) ptr2ObjectDesc(data);
#  ifdef USE_FMAGIC
				if (!invalidate) {	/* invalidate mode overwrites MAGIC */
					if (getObjMagic(obj) != MAGIC_OBJECT) {
						printf("NOMAGIC at %d\n", (char *) data - (char *)
						       GCM_ORG(domain).heap);
						sys_panic("");
					}
				}
#endif
				ASSERT(obj->vtable != NULL);
				c = obj2ClassDescFAST(obj);
				ASSERTCLASSDESC(c);
#ifdef CHECKHEAP_VERBOSE
				printf("    of class %s\n", c->name);
#endif
				if (!invalidate) {
					/* scan object */
					addr = c->map;
					for (k = 0; k < c->instanceSize; k++) {
						if (k % 8 == 0)
							b = *addr++;
						//printf("%d", b&1);
						if (b & 1) {
							/* reference slot */
							ptr = (ObjectDesc *)
							    obj->data[k];
#ifdef CHECKHEAP_VERBOSE
							printf("        reference slot %d %p\n", k, ptr);
#endif

#ifdef DEBUG
							if (!gc_isValidHeapRef(domain, ptr)) {
								sys_panic("Not a reference in object!");
							}
#endif				/* DEBUG */

						}
						b >>= 1;
					}
				}
				objSize = OBJSIZE_OBJECT(c->instanceSize);
				data += objSize;
				break;
			}
		case OBJFLAGS_PORTAL:{
#ifdef CHECKHEAP_VERBOSE
				printf("Portal\n");
#endif
				objSize = OBJSIZE_PORTAL;

				obj = ptr2ObjectDesc(data);
#  ifdef USE_FMAGIC
				if (!invalidate) {	/* invalidate mode overwrites MAGIC */
					if (getObjMagic(obj) != MAGIC_OBJECT) {
						printf("NOMAGIC at %d\n", (char *) data - (char *)
						       GCM_ORG(domain).heap);
						sys_panic("");
					}
				}
#endif


				data += objSize;
				break;
			}
		case OBJFLAGS_MEMORY:{
#ifdef CHECKHEAP_VERBOSE
				printf("Memory\n");
#endif
				mem = (MemoryProxy *) ptr2ObjectDesc(data);
#  ifdef USE_FMAGIC
				if (!invalidate) {	/* invalidate mode overwrites MAGIC */
					if (getObjMagic(obj) != MAGIC_OBJECT) {
						printf("NOMAGIC at %d\n", (char *) data - (char *)
						       GCM_ORG(domain).heap);
						sys_panic("");
					}
				}
#endif
				ASSERTMEMORY((ObjectDesc *) mem);

				objSize = OBJSIZE_MEMORY;
				data += objSize;
				break;
			}
		case OBJFLAGS_SERVICE:{
#ifdef CHECKHEAP_VERBOSE
				printf("Service\n");
#endif
				objSize = OBJSIZE_SERVICEDESC;
				data += objSize;
				break;
			}
		case OBJFLAGS_ATOMVAR:{
#ifdef CHECKHEAP_VERBOSE
				printf("Atomvar\n");
#endif
				obj = ptr2ObjectDesc(data);
#  ifdef USE_FMAGIC
				if (!invalidate) {	/* invalidate mode overwrites MAGIC */
					if (getObjMagic(obj) != MAGIC_OBJECT) {
						printf("NOMAGIC at %d\n", (char *) data - (char *)
						       GCM_ORG(domain).heap);
						sys_panic("");
					}
				}
#endif
#ifdef DEBUG
				if (!gc_isValidHeapRef(domain, ((AtomicVariableProxy *) obj)->value)) {
					sys_panic("Not a reference in atomvar!");
				}
#endif
				objSize = OBJSIZE_ATOMVAR;

				data += objSize;
				break;
			}
		case OBJFLAGS_CAS:{
#ifdef CHECKHEAP_VERBOSE
				printf("CAS\n");
#endif
				obj = ptr2ObjectDesc(data);
#  ifdef USE_FMAGIC
				if (!invalidate) {	/* invalidate mode overwrites MAGIC */
					if (getObjMagic(obj) != MAGIC_OBJECT) {
						printf("NOMAGIC at %d\n", (char *) data - (char *)
						       GCM_ORG(domain).heap);
						sys_panic("");
					}
				}
#endif
				objSize = OBJSIZE_CAS;
				data += objSize;
				break;
			}
		case OBJFLAGS_EXTERNAL_STRING:{
				if (domain != domainZero) {
					printf("External string on heap of domain %ld (%s)\n", domain->id, domain->domainName);
					sys_panic("");
				}
				obj = ptr2ObjectDesc(data);
				c = obj2ClassDesc(obj);
				objSize = OBJSIZE_OBJECT(c->instanceSize);
				data += objSize;
				break;
			}
		default:
			printf("FLAGS: %lx\n", flags);
			sys_panic("WRONG HEAP DATA");
		}
#  ifdef USE_QMAGIC
		if (invalidate) {
			setObjMagic(ptr2ObjectDesc(olddata), MAGIC_INVALID);
			//printf("INVALIDATE %p\n", olddata + 2);
		}
#endif
	}
	return ok;
#else
	return JNI_TRUE;
#endif
}

int gc_org_isInHeap(DomainDesc * domain, ObjectDesc * obj)
{
	return ((obj >= ((ObjectDesc *) GCM_ORG(domain).heap))
		&& (obj < (ObjectDesc *) (GCM_ORG(domain).heapTop)));
}

u4_t gc_org_freeWords(DomainDesc * domain)
{
	return (u4_t) GCM_ORG(domain).heapBorder - (u4_t) GCM_ORG(domain).heapTop;
}

u4_t gc_org_totalWords(struct DomainDesc_s * domain)
{
	return (u4_t) GCM_ORG(domain).heapBorder - (u4_t) GCM_ORG(domain).heap;
}

void gc_org_printInfo(struct DomainDesc_s *domain)
{
	printf("heap(used ): %p...%p (current=%p)\n", GCM_ORG(domain).heap, GCM_ORG(domain).heapBorder, GCM_ORG(domain).heapTop);
	printf("heap(other): %p...%p (current=%p)\n", GCM_ORG(domain).heap2, GCM_ORG(domain).heapBorder2,
	       GCM_ORG(domain).heapTop2);
	printf("total: %ld, used: %ld, free: %ld\n", gc_totalWords(domain), gc_totalWords(domain) - gc_freeWords(domain),
	       gc_freeWords(domain));
}

/* align heap at page borders to use mprotect for debugging the GC */
#define HEAP_BLOCKSIZE            4096
#define HEAP_BLOCKADDR_N_NULLBITS 12
#define HEAP_BLOCKADDR_MASK       0xfffff000

void gc_org_init(DomainDesc * domain, u4_t heap_bytes)
{
	u4_t heapSize;
	u4_t *start;

	ASSERT(sizeof(GCDescUntypedMemory_t) >= sizeof(gc_org_mem_t));

	if (heap_bytes == 0)
		sys_panic("gc_org not suitable for domain zero");

	/* alloc heap mem */
	heapSize = heap_bytes;
	heapSize = (heapSize + HEAP_BLOCKSIZE - 1) & HEAP_BLOCKADDR_MASK;
	if (heapSize <= HEAP_RESERVE + 1000)
		sys_panic("heap too small. need at least %d bytes ", HEAP_RESERVE);
	if (HEAP_BLOCKSIZE % BLOCKSIZE != 0)
		sys_panic("heapalign must be multiple of blocksize");


	GCM_ORG(domain).heap = (jint *) jxmalloc_align(heapSize >> BLOCKADDR_N_NULLBITS, HEAP_BLOCKSIZE, &start MEMTYPE_HEAP);
	GCM_ORG(domain).heapFreePtr = start;
	GCM_ORG(domain).heapBorder = GCM_ORG(domain).heap + (heap_bytes >> 2);
	GCM_ORG(domain).heapTop = GCM_ORG(domain).heap;
#ifdef ENABLE_GC
	GCM_ORG(domain).heap2 = (jint *) jxmalloc_align(heapSize >> BLOCKADDR_N_NULLBITS, 4096, &start MEMTYPE_HEAP);
	GCM_ORG(domain).heap2FreePtr = start;
	GCM_ORG(domain).heapBorder2 = GCM_ORG(domain).heap2 + (heap_bytes >> 2);
	GCM_ORG(domain).heapTop2 = GCM_ORG(domain).heap2;
#endif

	if (GCM_ORG(domain).heapTop > GCM_ORG(domain).heapBorder - HEAP_RESERVE) {
		sys_panic("HEAP TOO SMALL");
	}

	/*
	   domain->gc.allocDataInDomain = gc_org_allocDataInDomain;
	   domain->gc.gc = gc_org_gc;
	   domain->gc.done = gc_org_done;
	   domain->gc.freeWords = gc_org_freeWords;
	   domain->gc.totalWords = gc_org_totalWords;
	   domain->gc.printInfo = gc_org_printInfo;
	   domain->gc.finalizeMemory = gc_org_finalizeMemory;
	   domain->gc.finalizePortals = gc_org_finalizePortals;
	   domain->gc.isInHeap = gc_org_isInHeap;
	 */
	// FIXME domain->gc.walkHeap = NULL;

}
#endif				/*  !defined( GC_USE_NEW ) && defined (ENABLE_GC) */
