#if defined( GC_BITMAP_IMPL ) && defined ( GC_USE_NEW )
#include "all.h"

#include "gc_bitmap.h"
#include "gc_memcpy.h"
#include "gc_impl.h"
#include "gc_pgc.h"

#define SLOT_SIZE  (4*sizeof(u4_t))
#define BYTES2SLOTS(_size_) (((_size_) + SLOT_SIZE - 1) / SLOT_SIZE)

#ifndef VERBOSE_GC
#undef  BM_VERBOSE_MARK
#endif

#ifdef HAVE_RDTSC

#ifdef VERBOSE_GC
#define BITMAP_TIMEOUT 10000	/* milli seconds */
#else
#define BITMAP_TIMEOUT 50000	/* milli seconds */
#endif

#define BITMAP_STARTTIME(_domain_) bitmap_starttime(_domain_)
#define BITMAP_CHECK_TOUT(_domain_) bitmap_checktimout(_domain_,__FILE__,__LINE__)
#else
#define BITMAP_STARTTIME(_domain_)
#define BITMAP_CHECK_TOUT(_domain_)
#endif

typedef struct gc_bitmap_mem_s {
	u1_t *map;
	u4_t current;
	u1_t *heap;		/* all objects life here */
	u4_t n_slots;
	u4_t free_slots;
	u1_t run;
	u8_t stime;
} gc_bitmap_mem_t;
#define GCM_BITMAP(domain) (*(gc_bitmap_mem_t*)(&domain->gc.untypedMemory))

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
extern Proxy *initialNamingProxy;

#ifdef HAVE_RDTSC
static void bitmap_starttime(DomainDesc * domain)
{
	u8_t st;
	asm volatile ("rdtsc":"=A" (st):);
	GCM_BITMAP(domain).stime = st;
}
static void bitmap_checktimout(DomainDesc * domain, const char *file, int line)
{
	u8_t st, time;
	asm volatile ("rdtsc":"=A" (st):);
	time = (st - GCM_BITMAP(domain).stime) / (CPU_MHZ * 1000);
	if (time > BITMAP_TIMEOUT) {
		printf("timout at %s line %d\n", file, line);
		monitor(0);
	}
}
#endif				/* HAVE_RDTSC */

static void bitmap_clear(DomainDesc * domain)
{
	memset(GCM_BITMAP(domain).map, 0, GCM_BITMAP(domain).heap - GCM_BITMAP(domain).map);
	GCM_BITMAP(domain).free_slots = GCM_BITMAP(domain).n_slots;
	GCM_BITMAP(domain).current = 0;
}

static void bitmap_dump(DomainDesc * domain)
{
	u4_t i = 0;
	u4_t e = GCM_BITMAP(domain).n_slots / 32;

	printf("\n");
	for (i = 0; i < e; i++) {
		u4_t d = (u4_t *) (GCM_BITMAP(domain).map)[i];
		switch (d) {
		case 0xffffffff:
			printf("#");
			break;
		case 0:
			printf(".");
			break;
		default:
			printf("o");
		}
		if ((i % 80) == 0)
			printf("\n");
	}
	printf("\n");
}

void bitmap_clear_heap(DomainDesc * domain)
{
	u1_t *addr;
	u4_t c, objSize;
	u4_t s = GCM_BITMAP(domain).n_slots;
	ObjectDesc *obj;
	ClassDesc *cl;
	jint flags;

	for (c = 0; c < s; c++) {
		if (GCM_BITMAP(domain).map[c >> 3] & 1 << (c & 0x7)) {
		} else {
			addr = GCM_BITMAP(domain).heap + (c * SLOT_SIZE);
			memset(addr, 0, SLOT_SIZE);
		}
	}
}

static int bitmap_next_free(DomainDesc * domain)
{
	u4_t c = GCM_BITMAP(domain).current;
	u4_t s = GCM_BITMAP(domain).n_slots;
	while (GCM_BITMAP(domain).map[c >> 3] & 1 << (c & 0x7)) {
		c++;
		c %= s;
		if (c == GCM_BITMAP(domain).current)
			return JNI_FALSE;
		BITMAP_CHECK_TOUT(domain);
	}
	GCM_BITMAP(domain).current = c;
	return JNI_TRUE;
}

static int bitmap_has_space(DomainDesc * domain, u4_t nslots)
{
	u4_t i;
	u4_t c = GCM_BITMAP(domain).current;
	u4_t s = GCM_BITMAP(domain).n_slots;

	if (c + nslots > s)
		return JNI_FALSE;

	for (i = 0; i < nslots; i++) {
		if (GCM_BITMAP(domain).map[(c + i) >> 3] & 1 << ((c + i) & 0x7))
			return JNI_FALSE;
		BITMAP_CHECK_TOUT(domain);
	}
	return JNI_TRUE;
}

#if 0
static u4_t bitmap_mark1(DomainDesc * domain, u4_t nslots)
{
	u4_t i, pos;
	u1_t *addr;
	u4_t c = GCM_BITMAP(domain).current;
	u4_t s = GCM_BITMAP(domain).n_slots;

	if ((c + nslots) <= s) {
		nslots = s - c - 1;
	}

	for (i = 0; i < nslots; i++) {
		pos = c + i;
		GCM_BITMAP(domain).map[pos >> 3] |= 1 << (pos & 0x7);
		BITMAP_CHECK_TOUT(domain);
	}

	GCM_BITMAP(domain).free_slots -= nslots;
}
static u1_t *bitmap_mark(DomainDesc * domain, u4_t nslots)
{
	u1_t *addr;
	u4_t c = GCM_BITMAP(domain).current;
	u4_t s = GCM_BITMAP(domain).n_slots;

	ASSERT((c + nslots) <= s);

	bitmap_mark1(domain, nslots);

	addr = GCM_BITMAP(domain).heap + (c * SLOT_SIZE);
	GCM_BITMAP(domain).current = c + nslots;

	return addr;
}
#else
static u1_t *bitmap_mark(DomainDesc * domain, u4_t nslots)
{
	u4_t i, pos;
	u1_t *addr;
	u4_t c = GCM_BITMAP(domain).current;
	u4_t s = GCM_BITMAP(domain).n_slots;

	ASSERT((c + nslots) <= s);

	for (i = 0; i < nslots; i++) {
		pos = c + i;
		GCM_BITMAP(domain).map[pos >> 3] |= 1 << (pos & 0x7);
		BITMAP_CHECK_TOUT(domain);
	}

	GCM_BITMAP(domain).free_slots -= nslots;

	addr = GCM_BITMAP(domain).heap + (c * SLOT_SIZE);
	GCM_BITMAP(domain).current = c + nslots;

	return addr;
}
#endif

static jint *bitmap_alloc(DomainDesc * domain, u4_t size)
{
	u4_t nslots;
	u4_t i;

	nslots = BYTES2SLOTS(size);

#ifdef VERBOSE_GC
	if (nslots > 50) {
		printf("big alloc %d bytes\n", size);
	}
#endif

	for (i = 0; i < (GCM_BITMAP(domain).n_slots / 2); i++) {
		if (bitmap_has_space(domain, nslots))
			return (jint *) bitmap_mark(domain, nslots);
		GCM_BITMAP(domain).current += nslots;
		if (!bitmap_next_free(domain))
			break;
		BITMAP_CHECK_TOUT(domain);
	}

	return NULL;
}

static jint bitmap_check_reserve(DomainDesc * domain, u4_t size)
{
	u4_t nslots = BYTES2SLOTS(size);
	if (GCM_BITMAP(domain).free_slots < (nslots + (HEAP_RESERVE / SLOT_SIZE)))
		return JNI_FALSE;
	return JNI_TRUE;
}

#define bitmap_objSize(_D_,_O_) gc_objSize(_O_)

static void bitmap_mark_thread(DomainDesc * domain, ThreadDesc * thr)
{
	ThreadDescProxy *tpr;
	if (thr) {
		tpr = thread2CPUState(thr);
		bitmap_mark_ref(domain, (ObjectDesc **) & (tpr));
	}
}

static void bitmap_scan_object(DomainDesc * domain, ObjectDesc * obj)
{
	u4_t i;
	u4_t *addr;
	jint objSize;

	if (obj == NULL)
		return;

	if ((objSize = bitmap_objSize(domain, obj)) == -1) {
#ifdef VERBOSE_GC
		printf("unknown object size %p 0x%lx\n", obj, getObjFlags(obj));
		sys_panic("");
#endif
		return;
	}
#ifdef VERBOSE_GC
	printf("scan unknown object at %p flags 0x%lx\n", obj, getObjFlags(obj));
#endif
	addr = obj;
	for (i = 0; i < objSize; i++) {
		bitmap_mark_ref(domain, &addr);
		addr++;
	}
}

static int bitmap_guess_type(DomainDesc * domain, ObjectDesc * obj, u1_t ** addr)
{
	ObjectDesc *eobj;

	if (obj == NULL)
		return -1;
	if (((int) obj % 4) != 0)
		return -1;
	if (!gc_bitmap_isInHeap(domain, obj))
		return -1;

#ifdef USE_MAGIC
	if (getObjMagic(obj) == MAGIC_OBJECT) {
		*addr = (u1_t *) ObjectDesc2ptr(obj);
		return bitmap_objSize(domain, obj);
	}
	/* handle embeded object e.g. threads */
	eobj = (ObjectDesc *) (((u4_t *) obj) - 1);
	if (getObjMagic((ObjectDesc *) eobj) == MAGIC_OBJECT) {
		*addr = (u1_t *) ObjectDesc2ptr(eobj);
		return bitmap_objSize(domain, obj);
	}
#else
	{
		u4_t size;
		size = bitmap_objSize(domain, obj);
		if (size != -1) {
			*addr = (u1_t *) ObjectDesc2ptr(obj);
			return size;
		}
		eobj = (ObjectDesc *) (((u4_t *) obj) - 1);
		size = bitmap_objSize(domain, eobj);
		if (size != -1) {
			*addr = (u1_t *) ObjectDesc2ptr(eobj);
			return size;
		}
	}
#endif				/* USE_MAGIC */
	return -1;
}

static jint bitmap_mark_addr(DomainDesc * domain, u1_t * addr, u4_t size)
{
	u4_t nslots;

	GCM_BITMAP(domain).current = (addr - GCM_BITMAP(domain).heap) / SLOT_SIZE;
	nslots = BYTES2SLOTS(size * sizeof(u4_t));

	if (!bitmap_has_space(domain, nslots))
		return JNI_FALSE;

	if (addr != bitmap_mark(domain, nslots))
		ASSERT(0);
}

static u4_t *bitmap_mark_ref(DomainDesc * domain, ObjectDesc ** refPtr)
{
	u1_t *addr;
	u4_t i;
	jint objSize;
	ObjectDesc *object;

	CHECK_STACK_SIZE(domain, 80);

	if (refPtr == NULL)
		return NULL;

	if (!gc_bitmap_isInHeap(domain, *refPtr))
		return NULL;

	object = *refPtr;

	if ((objSize = bitmap_guess_type(domain, object, &addr)) == -1)
		return NULL;
	object = ptr2ObjectDesc(addr);

	if (!bitmap_mark_addr(domain, addr, objSize))
		return NULL;

	/*  now walk content */
#ifdef USE_IMPL_WALK
	gc_impl_walkContent2(domain, object, bitmap_mark_ref);
#else
	switch (getObjFlags(object) & FLAGS_MASK) {
	case OBJFLAGS_ARRAY:
		{
			ArrayDesc *obj = (ArrayDesc *) object;
			if (obj->arrayClass->name[1] == 'L' || obj->arrayClass->name[1] == '[') {
				for (i = 0; i < obj->size; i++)
					bitmap_mark_ref(domain, (ObjectDesc **) & (obj->data[i]));
			}
		}
		break;
	case OBJFLAGS_OBJECT:
		{
			ObjectDesc *obj = (ObjectDesc *) object;
			ClassDesc *cl;
			cl = obj2ClassDesc(obj);
			ASSERTCLASSDESC(cl);
			FORBITMAP(cl->map, cl->instanceSize, bitmap_mark_ref(domain, (ObjectDesc **) & (obj->data[index]));,
				  /* nothing */ );
		}
		break;
	case OBJFLAGS_SERVICE:
		{
			DEPDesc *obj = (DEPDesc *) object;
			bitmap_mark_ref(domain, &(obj->obj));
			bitmap_mark_ref(domain, (ObjectDesc **) & (obj->proxy));
#ifdef NEW_PORTALCALL
			bitmap_mark_ref(domain, (ObjectDesc **) & (obj->pool));
#else
			bitmap_mark_thread(domain, obj->receiver);
			if (obj->firstWaitingSender) {
				sys_panic("WalkService: fws not yet impl");
			}
#endif				/* NEW_PORTALCALL */
		}
		break;
	case OBJFLAGS_SERVICE_POOL:
		{
			ServiceThreadPool *obj = (ServiceThreadPool *) obj;
			bitmap_mark_thread(domain, obj->firstReceiver);
		}
		break;
	case OBJFLAGS_ATOMVAR:
		{
			AtomicVariableProxy *obj = (AtomicVariableProxy *) object;
			bitmap_mark_thread(domain, obj->blockedThread);
			bitmap_mark_ref(domain, &(obj->value));
		}
		break;
	case OBJFLAGS_FOREIGN_CPUSTATE:
		{
			ThreadDescForeignProxy *obj = (ThreadDescForeignProxy *) object;
			bitmap_mark_ref(domain, (ObjectDesc **) & (obj->domain));
		}
		break;
	case OBJFLAGS_CPUSTATE:
		{
			ThreadDescProxy *obj = (ThreadDescProxy *) object;
			ThreadDesc *t = cpuState2thread(obj);

			/* pointer to threads in our domain */
			bitmap_mark_thread(domain, t->nextInDomain);
			bitmap_mark_thread(domain, t->prevInDomain);
			bitmap_mark_thread(domain, t->nextInRunQueue);

			/* pointer to threads in other domains and pointer to this thread in other domains */
			/* if (t->blockedInServiceThread) MOVETCB(t->blockedInServiceThread->mostRecentlyCalledBy); */

#ifdef NEW_PORTALCALL
			bitmap_mark_thread(domain, t->nextInReceiveQueue);
#endif
			bitmap_scan_object(domain, obj);

		}
		break;
	case OBJFLAGS_EXTERNAL_STRING:
		ASSERT(domain == domainZero);
	case OBJFLAGS_PORTAL:
	case OBJFLAGS_MEMORY:
	case OBJFLAGS_CAS:
	case OBJFLAGS_DOMAIN:
	default:
		bitmap_scan_object(domain, object);
	}
#endif

	return NULL;
}


ObjectHandle gc_bitmap_allocDataInDomain(DomainDesc * domain, int objSize, u4_t flags)
{
	ObjectDesc *obj;
	jint *nextObj = NULL;
	ObjectHandle handle;
#ifdef KERNEL
	volatile u4_t irqflags __attribute__ ((unused)) = getEFlags();
#endif

	GC_LOCK;

	BITMAP_STARTTIME(domain);

	objSize = objSize * 4;

	do {
#if defined(KERNEL) || defined(JAVASCHEDULER)
		if (curthr()->isInterruptHandlerThread) {
			nextObj = bitmap_alloc(domain, objSize);
			if (nextObj == NULL) {
				sys_panic("no GC in interrupt handler possible!");
			}
		} else {
#endif
			if (bitmap_check_reserve(domain, objSize)) {
				nextObj = bitmap_alloc(domain, objSize);
			}

			if (nextObj == NULL) {
				if (domain->gc.gcThread == NULL)
					domain_panic(domain, "GC but no GC thread available");
				start_thread_using_code1(domain->gc.gcObject, domain->gc.gcThread, domain->gc.gcCode,
							 (u4_t) domain);
#ifdef VERBOSE_GC
				printf("    Live bytes: %d\n",
				       (char *) GCM_BITMAP(domain).n_slots - (char *) GCM_BITMAP(domain).free_slots);
#endif
				bitmap_next_free(domain);
				nextObj = bitmap_alloc(domain, objSize);
			}
#if defined(KERNEL) || defined(JAVASCHEDULER)
		}
#endif
		BITMAP_CHECK_TOUT(domain);
	} while (nextObj == NULL);


	ASSERT(nextObj != NULL);
	memset(nextObj, 0, objSize);

	obj = ptr2ObjectDesc((jint *) nextObj);
	setObjFlags(obj, flags);

#ifdef DEBUG
	if (!gc_bitmap_isInHeap(domain, obj))
		sys_panic("assertion failed");
#endif

#ifdef USE_QMAGIC
	setObjMagic(obj, MAGIC_OBJECT);
#endif

#if 0
	if (GCM_BITMAP(domain).run > 0)
		printf(" alloc %d %d free:%d %p\n", domain->id, objSize, GCM_BITMAP(domain).free_slots, obj);
#endif
	handle = registerObject(domain, obj);

	GC_UNLOCK;

	return handle;
}

void gc_bitmap_walkHeap(DomainDesc * domain, HandleObject_t handleObject, HandleObject_t handleArray, HandleObject_t handlePortal,
			HandleObject_t handleMemory, HandleObject_t handleService, HandleObject_t handleCAS,
			HandleObject_t handleAtomVar, HandleObject_t handleDomainProxy, HandleObject_t handleCPUStateProxy)
{
	/* we do nothing until now */
}

int gc_bitmap_isInHeap(DomainDesc * domain, ObjectDesc * obj)
{
	int slot = ((int) obj - (int) GCM_BITMAP(domain).heap) / SLOT_SIZE;
	return (slot >= 0 && slot < GCM_BITMAP(domain).n_slots);
}

void gc_bitmap_done(DomainDesc * domain)
{
	/* free heap (todo finalize) */
}

void gc_bitmap_finalizeMemory(DomainDesc * domain)
{
	// FIXME
	sys_panic("gc_bitmap_finalizeMemory");
}

void gc_bitmap_finalizePortals(DomainDesc * domain)
{
	// FIXME
	sys_panic("gc_bitmap_finalizePortals");
}

void walkUnknownStack(DomainDesc * domain, ThreadDesc * thread, HandleReference_t handleReference)
{
	u4_t *ebp, *sp, *s;

	ebp = (u4_t *) thread->context[PCB_EBP];
	sp = (u4_t *) thread->context[PCB_ESP];

	handleReference(domain, (ObjectDesc **) & thread->context[PCB_EAX]);
	handleReference(domain, (ObjectDesc **) & thread->context[PCB_ECX]);
	handleReference(domain, (ObjectDesc **) & thread->context[PCB_ESI]);
	handleReference(domain, (ObjectDesc **) & thread->context[PCB_EDI]);
	handleReference(domain, (ObjectDesc **) & thread->context[PCB_EBX]);
	handleReference(domain, (ObjectDesc **) & thread->context[PCB_EDX]);

#ifdef VERBOSE_GC
	printf("stack of thread %p fp:%p sp:%p top:%p\n", thread, ebp, sp, thread->stackTop);
#endif

	while (sp != NULL && ebp != NULL && sp < thread->stackTop) {
		for (s = ebp - 1; s > (sp + 1); s--) {
			handleReference(domain, (ObjectDesc **) s);
		}
		sp = ebp;
		if (sp == NULL)
			break;
		ebp = (u4_t *) * sp;
	}
}


void gc_bitmap_gc(DomainDesc * domain)
{
	ThreadDesc *thread;

#ifdef VERBOSE_GC
	printf("BITMAP GARBAGE COLLECTOR started for domain %p (%s)\n", domain, domain->domainName);
#endif

	GCM_BITMAP(domain).run++;

	/* 
	 * Init
	 */

	freezeThreads(domain);

	/* clear bitmap */
	bitmap_clear(domain);

	/* realloc objects from root set */
	/*
	   walkRootSet(domain, bitmap_mark_ref, bitmap_mark_ref, bitmap_mark_ref,
	   bitmap_mark_ref, bitmap_mark_ref, bitmap_mark_ref);
	 */

#ifdef VERBOSE_GC
	printf("\nScanning stacks...\n");
#endif
	for (thread = domain->threads; thread != NULL; thread = thread->nextInDomain) {
		if (thread == domain->gc.gcThread)
			continue;	/* don't scan my own stack */
		if (thread->isInterruptHandlerThread)
			continue;	/* don't scan interrupt stacks */
		if (thread->state == STATE_AVAILABLE)
			continue;	/* don't scan stack of available threads */

		walkUnknownStack(domain, thread, bitmap_mark_ref);
	}

	walkStatics(domain, bitmap_mark_ref);
	walkPortals(domain, bitmap_mark_ref);
	walkRegistereds(domain, bitmap_mark_ref);
	walkSpecials(domain, bitmap_mark_ref);
	walkInterrupHandlers(domain, bitmap_mark_ref);

	bitmap_clear_heap(domain);
	//bitmap_dump(domain);

#ifdef VERBOSE_GC
	printf("GC finished\n");
#endif				/* VERBOSE_GC */
}

u4_t gc_bitmap_freeWords(DomainDesc * domain)
{
	return (u4_t) GCM_BITMAP(domain).free_slots * SLOT_SIZE / sizeof(int);
}

u4_t gc_bitmap_totalWords(struct DomainDesc_s *domain)
{
	return (u4_t) GCM_BITMAP(domain).n_slots * SLOT_SIZE / sizeof(int);
}

void gc_bitmap_printInfo(struct DomainDesc_s *domain)
{
	u1_t *heap = GCM_BITMAP(domain).heap;
	u1_t *map = GCM_BITMAP(domain).map;
	u4_t n_slots = GCM_BITMAP(domain).n_slots;
	u4_t free_slots = GCM_BITMAP(domain).free_slots;
	u4_t bitmap_size = n_slots / 8;

	printf(" map   %p ... %p %d byte\n", map, map + bitmap_size, bitmap_size);
	printf(" heap  %p ... %p %d byte\n", heap, heap + n_slots * SLOT_SIZE, n_slots * SLOT_SIZE);
	printf(" total: %d (%d byte) free: %d (%d byte)\n", n_slots, n_slots * SLOT_SIZE, free_slots, free_slots * SLOT_SIZE);
}

/* align heap at page borders to use mprotect for debugging the GC */
#define HEAP_BLOCKSIZE            4096
#define HEAP_BLOCKADDR_N_NULLBITS 12
#define HEAP_BLOCKADDR_MASK       0xfffff000

void gc_bitmap_init(DomainDesc * domain, u4_t heap_bytes)
{
	u4_t heapSize;
	u4_t *start;
	u4_t n_obj;
	u4_t bitmap_size;

	ASSERT(sizeof(GCDescUntypedMemory_t) >= sizeof(gc_bitmap_mem_t));

	if (heap_bytes == 0)
		sys_panic("gc_bitmap is not suitable for domain Zero");

	if (heap_bytes < HEAP_RESERVE + HEAP_BLOCKSIZE)
		heap_bytes = HEAP_RESERVE + HEAP_BLOCKSIZE;

	n_obj = heap_bytes / SLOT_SIZE;
	bitmap_size = n_obj / 8;
	heap_bytes += bitmap_size;

	/* alloc heap mem */
	heapSize = (heap_bytes + HEAP_BLOCKSIZE - 1) & HEAP_BLOCKADDR_MASK;

	if (HEAP_BLOCKSIZE % BLOCKSIZE != 0)
		sys_panic("heapalign must be multiple of blocksize");

	GCM_BITMAP(domain).map = (u1_t *) jxmalloc_align(heapSize >> BLOCKADDR_N_NULLBITS, HEAP_BLOCKSIZE, &start MEMTYPE_HEAP);
	/* FIXME: Do not align block. Align to slot instead */
	GCM_BITMAP(domain).heap = (u1_t *) start + ((bitmap_size + HEAP_BLOCKSIZE - 1) & HEAP_BLOCKADDR_MASK);

	heapSize -= (GCM_BITMAP(domain).heap - GCM_BITMAP(domain).map);
#ifdef VERBOSE_GC
	printf(" real heap size for %s is %d byte\n", domain->domainName, heapSize);
#endif
	GCM_BITMAP(domain).n_slots = heapSize / SLOT_SIZE;
	GCM_BITMAP(domain).free_slots = GCM_BITMAP(domain).n_slots;
	GCM_BITMAP(domain).current = 0;
	GCM_BITMAP(domain).run = 0;

#ifdef VERBOSE_GC
	printf(" domain %s\n", domain->domainName);
	gc_bitmap_printInfo(domain);
#endif

#ifndef GC_USE_ONLY_ONE
	domain->gc.allocDataInDomain = gc_bitmap_allocDataInDomain;
	domain->gc.gc = gc_bitmap_gc;
	domain->gc.done = gc_bitmap_done;
	domain->gc.freeWords = gc_bitmap_freeWords;
	domain->gc.totalWords = gc_bitmap_totalWords;
	domain->gc.printInfo = gc_bitmap_printInfo;
	domain->gc.finalizeMemory = gc_bitmap_finalizeMemory;
	domain->gc.finalizePortals = gc_bitmap_finalizePortals;
	domain->gc.isInHeap = gc_bitmap_isInHeap;
	domain->gc.walkHeap = gc_bitmap_walkHeap;
#endif				/* GC_USE_ONLY_ONE */
}
#endif
