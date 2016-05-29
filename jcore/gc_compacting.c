/********************************************************************************
 * Compacting garbage collector
 * Copyright 2001-2002 Joerg Baumann
 *******************************************************************************/

#if defined( GC_COMPACTING_IMPL ) && defined ( GC_USE_NEW ) && defined ( ENABLE_GC )
#include "all.h"

#include "gc_compacting.h"
#include "gc_memcpy.h"
#include "gc_impl.h"
#include "gc_pgc.h"

//FIXME
//#undef IF_DBG_GC
//#define IF_DBG_GC(n) n

typedef struct gc_compacting_mem_s {
	jint *heapBorder;	/* pointer to border of heap (last allocated word  + 1) */
	jint *heap;		/* all objects life here */
	jint *heapTop;		/* pointer to free heap space */
	u4_t depth;
	u4_t abort;
} gc_compacting_mem_t;

#define GCM_COMPACTING(domain) (*(gc_compacting_mem_t*)(&domain->gc.untypedMemory))

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
extern Proxy *initialNamingProxy;

ObjectHandle gc_compacting_allocDataInDomain(DomainDesc * domain, int objSize, u4_t flags)
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
	if (!gc_compacting_isInHeap(domain, JNI_FALSE) == JNI_TRUE)
		sys_panic("inconsistent heap");
#endif				/* CHECK_HEAP_BEFORE_ALLOC */

      try_alloc:
	nextObj = GCM_COMPACTING(domain).heapTop + objSize;
	if ((nextObj > GCM_COMPACTING(domain).heapBorder - HEAP_RESERVE)
#ifdef PROFILE_AGING
	    || (domain->gc.memTime > memTimeNext)
#endif
	    ) {
		printf("%p,%p,%p,%p\n", nextObj, GCM_COMPACTING(domain).heapBorder, (void *) HEAP_RESERVE,
		       (GCM_COMPACTING(domain).heapBorder - HEAP_RESERVE));
#ifdef PROFILE_AGING
		gc_dprintf("\nDomain %p (%s) reached memtime %lld (%lld). Starting GC...\n", domain, domain->domainName,
			   memTimeNext, domain->gc.memTime);
#endif

#if defined(KERNEL) || defined(JAVASCHEDULER)
		if (curthr()->isInterruptHandlerThread) {
			if (nextObj > GCM_COMPACTING(domain).heapBorder) {
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
		       (char *) GCM_COMPACTING(domain).heapTop - (char *) GCM_COMPACTING(domain).heap);
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
		printf("    Live bytes: %d\n", (char *) GCM_COMPACTING(domain).heapTop - (char *) GCM_COMPACTING(domain).heap);
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
	paNew(domain, objSize, ptr2ObjectDesc(GCM_COMPACTING(domain).heapTop));
#endif

#ifdef PROFILE_SAMPLE_HEAPUSAGE
	profile_sample_heapusage_alloc(domain objSize);
#endif				/* PROFILE_SAMPLE_HEAPUSGAE */

	data = (jint *) GCM_COMPACTING(domain).heapTop;
	GCM_COMPACTING(domain).heapTop = nextObj;

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
		u4_t *tmpptr = GCM_COMPACTING(domain).heapTop;
		GCM_COMPACTING(domain).heapTop -= objSize;
		if (!gc_compacting_isInHeap(domain, JNI_FALSE) == JNI_TRUE)
			sys_panic("inconsistent heap");
		GCM_COMPACTING(domain).heapTop = tmpptr;
	}
#endif				/* CHECK_HEAP_AFTER_ALLOC */

	GC_UNLOCK;

	return handle;
}

void gc_compacting_walkHeap(DomainDesc * domain, HandleObject_t handleObject, HandleObject_t handleArray,
			    HandleObject_t handlePortal, HandleObject_t handleMemory, HandleObject_t handleService,
			    HandleObject_t handleCAS, HandleObject_t handleAtomVar, HandleObject_t handleDomainProxy,
			    HandleObject_t handleCPUStateProxy)
{
	gc_walkContinuesBlock_Alt(domain, GCM_COMPACTING(domain).heap, (u4_t *) GCM_COMPACTING(domain).heapTop, handleObject,
				  handleArray, handlePortal, handleMemory, handleService, handleCAS, handleAtomVar,
				  handleDomainProxy);
}

inline int gc_compacting_isInHeap(DomainDesc * domain, ObjectDesc * obj)
{
	return (obj >= (ObjectDesc *) GCM_COMPACTING(domain).heap && obj < (ObjectDesc *) GCM_COMPACTING(domain).heapTop);
}

void gc_compacting_done(DomainDesc * domain)
{
	u4_t size;

	/* free heap */
	size = (GCM_COMPACTING(domain).heapBorder - GCM_COMPACTING(domain).heap) * 4;
	jxfree(GCM_COMPACTING(domain).heap, size MEMTYPE_HEAP);
	GCM_COMPACTING(domain).heap = GCM_COMPACTING(domain).heapBorder = GCM_COMPACTING(domain).heapTop = NULL;

}

void gc_compacting_finalizeMemory(DomainDesc * domain)
{
	// FIXME
	sys_panic("gc_compacting_finalizeMemory");
}

void gc_compacting_finalizePortals(DomainDesc * domain)
{
	// FIXME
	sys_panic("gc_compacting_finalizePortals");
}

static void clearMarkCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	setObjFlags(obj, getObjFlags(obj) & FORWARD_PTR_MASK);
	IF_DBG_GC(printf("clearMarkCB %p 0x%lx\n", obj, flags));
}

static void calcAddrCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	if (flags & FORWARD_MASK) {
		setObjMagic(obj, (u4_t) domain->gc.data);
		((u4_t *) domain->gc.data) += objSize;
	}
}

static void moveCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	u4_t *src, *dst;

	src = ObjectDesc2ptr(obj);

	if (flags & FORWARD_MASK) {
		dst = (u4_t *) getObjMagic(obj);
		ASSERT(getObjMagic(obj) != MAGIC_OBJECT);
		setObjMagic(obj, MAGIC_OBJECT);
		ASSERT(getObjMagic(obj) == MAGIC_OBJECT);

		IF_DBG_GC(printf("move from %p to %p (%ld, 0x%lx)\n", src, dst, objSize * 4, flags));
		// FIXME
		if (src == dst)
			return;
		if (src == dst)
			return;
		gc_memcpy(dst, src, objSize * 4);

	} else {
		ASSERTOBJECT(obj);
		IF_DBG_GC(printf("skip %p (%ld, 0x%lx)\n", src, objSize * 4, flags));
		if ((flags & FLAGS_MASK) == OBJFLAGS_MEMORY) {
			IF_DBG_GC(printf("DEADMEMORY: \n"));
			memory_deleted(((MemoryProxy *) obj));
		}
	}
}

static u4_t *adjustRefCB(DomainDesc * domain, ObjectDesc ** refPtr)
{
	u4_t flags;

	if (*refPtr == NULL)
		return (u4_t *) * refPtr;
	if (!gc_compacting_isInHeap(domain, *refPtr))
		return (u4_t *) * refPtr;
	flags = getObjFlags(*refPtr);
	IF_DBG_GC(printf("adjustRefCB %p 0x%lx\n", *refPtr, flags));
	ASSERT(flags & FORWARD_MASK);
	*refPtr = ptr2ObjectDesc((u4_t *) getObjMagic(*refPtr));
	return (u4_t *) * refPtr;
}

static void adjustObjectCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	if (flags & FORWARD_MASK)
		gc_impl_walkContentObject(domain, obj, adjustRefCB);
}

static void adjustArrayCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	if (flags & FORWARD_MASK)
		gc_impl_walkContentArray(domain, (ArrayDesc *) obj, adjustRefCB);
}

static void adjustServiceCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	if (flags & FORWARD_MASK)
		gc_impl_walkContentService(domain, (DEPDesc *) obj, adjustRefCB);
}

static void adjustAtomVarCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	if (flags & FORWARD_MASK)
		gc_impl_walkContentAtomVar(domain, (AtomicVariableProxy *) obj, adjustRefCB);
}

static u4_t *markCB3(DomainDesc * domain, ObjectDesc ** refPtr)
{
	u4_t flags;

	if (*refPtr == NULL)
		return NULL;
	if (!gc_compacting_isInHeap(domain, *refPtr)) {
		IF_DBG_GC(printf("REF OUT OF HEAP3 %p 0x%lx\n", *refPtr, getObjFlags(*refPtr)));
		return NULL;
	}

	flags = getObjFlags(*refPtr);
	if (!(flags & FORWARD_MASK)) {
		IF_DBG_GC(printf("+markCB %p 0x%lx\n", *refPtr, flags));
		if (GCM_COMPACTING(domain).depth > 10) {
			GCM_COMPACTING(domain).abort++;
			IF_DBG_GC(printf("Depth: %ld %ld\n", GCM_COMPACTING(domain).depth, GCM_COMPACTING(domain).abort));
			return NULL;
		}
		setObjFlags(*refPtr, flags | FORWARD_MASK);
		GCM_COMPACTING(domain).depth++;
		gc_impl_walkContent(domain, *refPtr, markCB3);
		GCM_COMPACTING(domain).depth--;
		IF_DBG_GC(printf("-markCB %p\n", *refPtr));
	}
	return NULL;
}

static void mark2CB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	GCM_COMPACTING(domain).depth = 0;
	if (flags & FORWARD_MASK) {
		gc_impl_walkContent(domain, obj, markCB3);
	}
}

static u4_t *markCB(DomainDesc * domain, ObjectDesc ** refPtr)
{
	u4_t flags;

	if (*refPtr == NULL)
		return NULL;
	if (!gc_compacting_isInHeap(domain, *refPtr)) {
		IF_DBG_GC(printf("REF OUT OF HEAP %p 0x%lx\n", *refPtr, getObjFlags(*refPtr)));
		return NULL;
	}

	flags = getObjFlags(*refPtr);
	if (!(flags & FORWARD_MASK)) {
		IF_DBG_GC(printf("+markCB %p 0x%lx\n", *refPtr, flags));
		setObjFlags(*refPtr, flags | FORWARD_MASK);
		IF_DBG_GC(printf("-markCB %p\n", *refPtr));

	}
	return NULL;
}

void gc_compacting_gc(DomainDesc * domain)
{
#ifdef VERBOSE_GC
	printf("GARBAGE COLLECTOR started for domain %p (%s)\n", domain, domain->domainName);
#endif
	printf("COMPACTING HEAP %p - %p (%p)\n", GCM_COMPACTING(domain).heap, GCM_COMPACTING(domain).heapTop,
	       GCM_COMPACTING(domain).heapBorder);

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

	/* 
	 * Init
	 */
#ifdef CHECK_HEAPUSAGE
# ifdef DBG_GC
	/* check whether heap is consistent */
	printf("Checking Heap...");
	if (gc_checkHeap(domain, JNI_FALSE) == JNI_TRUE)
		printf("OK\n");
	else
		printf("FAILED\n");
# else
	gc_checkHeap(domain, JNI_FALSE);
# endif
#endif				/*CHECK_HEAPUSAGE */

	freezeThreads(domain);
	IF_DBG_GC( {
		  printf("    Test map...\n");
		  walkStacks(domain, NULL);	// FIXME
		  }
	);

	/* clear marks on heap */
	IF_DBG_GC(printf("PHASE: clear marks on heap\n"));
	gc_compacting_walkHeap(domain, clearMarkCB, clearMarkCB, clearMarkCB, clearMarkCB, clearMarkCB, clearMarkCB, clearMarkCB);

	/* mark active objects */
	IF_DBG_GC(printf("PHASE: mark active objects (plain rootset)\n"));
	walkRootSet(domain, markCB, markCB, markCB, markCB, markCB, markCB);
	{
		int i = 0;

		do {
			IF_DBG_GC(printf("PHASE: mark active objects (limit rec heap): # %d\n", ++i));
			GCM_COMPACTING(domain).abort = 0;
			gc_compacting_walkHeap(domain, mark2CB, mark2CB, mark2CB, mark2CB, mark2CB, mark2CB, mark2CB);
		} while (GCM_COMPACTING(domain).abort > 0);
	}

	/* calculate new addresses */
	IF_DBG_GC(printf("PHASE: calculate new addresses\n"));
	(u4_t) domain->gc.data = GCM_COMPACTING(domain).heap;
	gc_compacting_walkHeap(domain, calcAddrCB, calcAddrCB, calcAddrCB, calcAddrCB, calcAddrCB, calcAddrCB, calcAddrCB);

	/* adjust references */
	IF_DBG_GC(printf("PHASE: adjust references (plain rootset)\n"));
	walkRootSet(domain, adjustRefCB, adjustRefCB, adjustRefCB, adjustRefCB, adjustRefCB, adjustRefCB);

	IF_DBG_GC(printf("PHASE: adjust references (heap)\n"));
	gc_compacting_walkHeap(domain, adjustObjectCB, adjustArrayCB, NULL, NULL, adjustServiceCB, NULL, adjustAtomVarCB);

	/* move objects */
	IF_DBG_GC(printf("PHASE: move objects\n"));
	gc_compacting_walkHeap(domain, moveCB, moveCB, moveCB, moveCB, moveCB, moveCB, moveCB);
	GCM_COMPACTING(domain).heapTop = (jint *) domain->gc.data;

	memset(GCM_COMPACTING(domain).heapTop, 0xFF, 4 * (GCM_COMPACTING(domain).heapBorder - GCM_COMPACTING(domain).heapTop));
	PGCE(PROTECT);

#ifdef DEBUG
#ifdef DBG_GC
	printf("Checking new heap\n");
	if (gc_compacting_isInHeap(domain, JNI_FALSE) == JNI_TRUE) {	/* check all objects */
		printf("    heap check passed\n");
	} else {
		sys_panic("ERROR");
	}
	printf("Checking corrected stacks...\n");
	walkStacks(domain, NULL);	// FIXME
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
}

u4_t gc_compacting_freeWords(DomainDesc * domain)
{
	return (u4_t) GCM_COMPACTING(domain).heapBorder - (u4_t) GCM_COMPACTING(domain).heapTop;
}

u4_t gc_compacting_totalWords(struct DomainDesc_s * domain)
{
	return (u4_t) GCM_COMPACTING(domain).heapBorder - (u4_t) GCM_COMPACTING(domain).heap;
}

void gc_compacting_printInfo(struct DomainDesc_s *domain)
{
	printf("heap(used ): %p...%p (current=%p)\n", GCM_COMPACTING(domain).heap, GCM_COMPACTING(domain).heapBorder,
	       GCM_COMPACTING(domain).heapTop);
	printf("total: %ld, used: %ld, free: %ld\n", gc_totalWords(domain) * 4,
	       (gc_totalWords(domain) - gc_freeWords(domain)) * 4, (gc_freeWords(domain)) * 4);
}

/* align heap at page borders to use mprotect for debugging the GC */
#define HEAP_BLOCKSIZE            4096
#define HEAP_BLOCKADDR_N_NULLBITS 12
#define HEAP_BLOCKADDR_MASK       0xfffff000

void gc_compacting_init(DomainDesc * domain, u4_t heap_bytes)
{
	u4_t heapSize;
	u4_t *start;

	//FIXME
	//heap_bytes = heap_bytes * 2;

	ASSERT(sizeof(GCDescUntypedMemory_t) >= sizeof(gc_compacting_mem_t));

	if (heap_bytes == 0)
		sys_panic("gc_compacting is not suitable for domain Zero");

	/* alloc heap mem */
	heapSize = (heap_bytes + HEAP_BLOCKSIZE - 1) & HEAP_BLOCKADDR_MASK;

	if (heapSize <= HEAP_RESERVE + 1000)
		sys_panic("heap too small. need at least %d bytes ", HEAP_RESERVE);
	if (HEAP_BLOCKSIZE % BLOCKSIZE != 0)
		sys_panic("heapalign must be multiple of blocksize");

	GCM_COMPACTING(domain).heap =
	    (jint *) jxmalloc_align(heapSize >> BLOCKADDR_N_NULLBITS, HEAP_BLOCKSIZE, &start MEMTYPE_HEAP);
	GCM_COMPACTING(domain).heapFreePtr = start;
	GCM_COMPACTING(domain).heapBorder = GCM_COMPACTING(domain).heap + (heap_bytes >> 2);
	GCM_COMPACTING(domain).heapTop = GCM_COMPACTING(domain).heap;

	if (GCM_COMPACTING(domain).heapTop > GCM_COMPACTING(domain).heapBorder - HEAP_RESERVE) {
		sys_panic("HEAP TOO SMALL");
	}
#ifndef GC_USE_ONLY_ONE
	domain->gc.allocDataInDomain = gc_compacting_allocDataInDomain;
	domain->gc.gc = gc_compacting_gc;
	domain->gc.done = gc_compacting_done;
	domain->gc.freeWords = gc_compacting_freeWords;
	domain->gc.totalWords = gc_compacting_totalWords;
	domain->gc.printInfo = gc_compacting_printInfo;
	domain->gc.finalizeMemory = gc_compacting_finalizeMemory;
	domain->gc.finalizePortals = gc_compacting_finalizePortals;
	domain->gc.isInHeap = gc_compacting_isInHeap;
	domain->gc.ensureInHeap = gc_compacting_ensureInHeap;
	domain->gc.walkHeap = gc_compacting_walkHeap;
#endif				/* GC_USE_ONLY_ONE */
}

#endif				/* defined( GC_COMPACTING_IMPL ) && defined ( GC_USE_NEW ) && defined (ENABLE_GC) */
