/********************************************************************************
 * Chunk-based GC
 * Copyright 2002 Michael Golm
 *******************************************************************************/

#if defined( GC_CHUNKED_IMPL ) && defined ( GC_USE_NEW ) && defined ( ENABLE_GC )
#include "all.h"

#include "gc_chunked.h"
#include "gc_impl.h"
#include "gc_pgc.h"

#include "gc_move_common.h"

//#define VERBOSE_GC 1

typedef struct ChunkInfo_s {
	jint *current;
	jint *last;
	struct ChunkInfo_s *next;
} ChunkInfo;

typedef struct gc_chunked_mem_s {
	gc_move_common_mem_t move_common;
	/* accounting info */
	DomainDesc *accountTo;
	u4_t accountToID;
	u4_t consumed;
	u4_t limit;
	ThreadDesc *waiting;
	/* allocation info primary heap */
	ChunkInfo *firstChunk;	/* pointer to the first chunk */
	jint *current;		/* the place we are currently allocating */
	ChunkInfo *currentChunk;	/* the chunk we are currently allocating in */
	jint *endCurrentChunk;	/* the end of the current chunk */
	/* allocation info secondary heap */
	ChunkInfo *firstChunk2;	/* pointer to the first chunk */
	jint *current2;		/* the place we are currently allocating */
	ChunkInfo *currentChunk2;	/* the chunk we are currently allocating in */
	jint *endCurrentChunk2;	/* the end of the current chunk */
	/* statistics */
	jint totalSizeWords;	/* total size of reserved memory */
	jint freeSizeWords;	/* reserved memory that is not used */
	jint usedSizeWords;	/* used memory */
	/* gc config */
	jint collectAtSizeWords;	/* gc starts with number of total allocated words */
	jint chunkSizeWords;	/* chunk size in words */
	jint initialHeapWords;	/* chunk size in words */
#ifdef NEW_COPY
	jint *mark;
#endif
} gc_chunked_mem_t;


#define GCM_CHUNKED(domain) (*(gc_chunked_mem_t*)(&domain->gc.untypedMemory))

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
extern Proxy *initialNamingProxy;


jint *allocChunk(DomainDesc * domain, u4_t size)
{
	DomainDesc *acc;
	if ((acc = GCM_CHUNKED(domain).accountTo) != NULL) {
		while (GCM_CHUNKED(acc).consumed + size > GCM_CHUNKED(acc).limit) {
			curthr()->nextInGCQueue = GCM_CHUNKED(acc).waiting;
			GCM_CHUNKED(acc).waiting = curthr();
			threadblock();
		}
		GCM_CHUNKED(acc).consumed += size;
	}
	return (jint *) jxmalloc(size MEMTYPE_HEAP);
}

jint *tryAllocChunk(DomainDesc * domain, u4_t size)
{
	DomainDesc *acc;
	if ((acc = GCM_CHUNKED(domain).accountTo) != NULL) {
		if (GCM_CHUNKED(acc).consumed + size > GCM_CHUNKED(acc).limit - (GCM_CHUNKED(acc).collectAtSizeWords << 2)) {	/* reduce limit by initial chunk size to allow GC to proceed */
			return NULL;
		}
		GCM_CHUNKED(acc).consumed += size;
	}
	return (jint *) jxmalloc(size MEMTYPE_HEAP);
}

void freeChunk(DomainDesc * domain, char *ptr, u4_t size)
{
	DomainDesc *acc;
	jxfree(ptr, size MEMTYPE_HEAP);
	if ((acc = GCM_CHUNKED(domain).accountTo) != NULL) {
		ThreadDesc *t = GCM_CHUNKED(acc).waiting;
		GCM_CHUNKED(acc).consumed -= size;
		GCM_CHUNKED(acc).waiting = NULL;
		while (t) {
			threadunblock(t);
			t = t->nextInGCQueue;
			t->nextInGCQueue = NULL;
		}
	}
}

ObjectHandle gc_chunked_allocDataInDomain(DomainDesc * domain, int objSize, u4_t flags)
{
	ObjectDesc *obj;
	jint *nextObj;
	jint *data;
	jint *chunk;
	ChunkInfo *chunkInfo;
	ObjectHandle handle;
#ifdef KERNEL
	volatile u4_t irqflags __attribute__ ((unused)) = getEFlags();
#endif
#ifdef GC_USE_MMX
	objSize++;
	objSize &= ~1;
#endif

	GC_LOCK;

	/* first check if it is time for a GC */
	if (GCM_CHUNKED(domain).usedSizeWords >= GCM_CHUNKED(domain).collectAtSizeWords) {
	      do_collect:
#ifdef NOTICE_GC
		printf("\n GC(chunked) in %p (%s) [Thread: %d.%d (%s) caller=", domain, domain->domainName, TID(curthr()),
		       curthr()->name);
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
		printf("\nDomain %p (%s) consumed %d bytes of heap space, %d bytes are free. Starting GC...\n", domain,
		       domain->domainName, GCM_CHUNKED(domain).usedSizeWords << 2, GCM_CHUNKED(domain).freeSizeWords << 2);
#endif
		if (domain->gc.gcThread == NULL)
			domain_panic(curdom(), "GC but no GC thread availabke");
		start_thread_using_code1(NULL /*domain->gc.gcObject */ , domain->gc.gcThread, domain->gc.gcCode, (u4_t) domain);
#ifdef VERBOSE_GC
		printf("\nAfter GC: domain %p (%s) consumed %d bytes of heap space, %d bytes are free.\n", domain,
		       domain->domainName, GCM_CHUNKED(domain).usedSizeWords << 2, GCM_CHUNKED(domain).freeSizeWords << 2);
#endif
	}


      try_alloc:
	if (objSize > GCM_CHUNKED(domain).chunkSizeWords - (sizeof(ChunkInfo) >> 2)) {
		printf("large object (size=%d) is larger than chunk (%d)\n", objSize,
		       GCM_CHUNKED(domain).chunkSizeWords - (sizeof(ChunkInfo) >> 2));
		sys_panic("not yet implemented");
	}
	nextObj = GCM_CHUNKED(domain).current + objSize;
	if (nextObj >= GCM_CHUNKED(domain).endCurrentChunk) {
		/* update chunkinfo of current chunk */
		GCM_CHUNKED(domain).currentChunk->current = GCM_CHUNKED(domain).current;
		/* allocate new chunk */
		chunk = tryAllocChunk(domain, GCM_CHUNKED(domain).chunkSizeWords << 2);
		if (chunk == NULL) {
			/* got no chunk -> gc and try again */
			goto do_collect;
		}
		chunkInfo = (ChunkInfo *) chunk;
		/* link new chunk */
		GCM_CHUNKED(domain).currentChunk->next = chunkInfo;
		GCM_CHUNKED(domain).currentChunk = chunkInfo;
		GCM_CHUNKED(domain).endCurrentChunk = chunk + GCM_CHUNKED(domain).chunkSizeWords;
		GCM_CHUNKED(domain).current = (char *) (GCM_CHUNKED(domain).currentChunk) + sizeof(ChunkInfo);
		GCM_CHUNKED(domain).totalSizeWords += GCM_CHUNKED(domain).chunkSizeWords;
		GCM_CHUNKED(domain).freeSizeWords += GCM_CHUNKED(domain).chunkSizeWords - (sizeof(ChunkInfo) >> 2);
		chunkInfo->current = NULL;	/* current pointer inside current chunk is not valid */
		chunkInfo->last = GCM_CHUNKED(domain).endCurrentChunk;
		chunkInfo->next = NULL;
		/*printf("total: %d\n", GCM_CHUNKED(domain).totalSizeWords); */
		goto try_alloc;
	}


	data = (jint *) GCM_CHUNKED(domain).current;
	GCM_CHUNKED(domain).current = nextObj;

	/* update statistics */
	GCM_CHUNKED(domain).freeSizeWords -= objSize;
	GCM_CHUNKED(domain).usedSizeWords += objSize;


	/* data is now allocated; initialize it */
	ASSERT(data != NULL);
	memset(data, 0, objSize * 4);
	obj = ptr2ObjectDesc(data);
	setObjFlags(obj, flags);
#ifdef USE_QMAGIC
	setObjMagic(obj, MAGIC_OBJECT);
#endif

	handle = registerObject(domain, obj);

	GC_UNLOCK;
	return handle;
}

void gc_chunked_walkHeap(DomainDesc * domain, HandleObject_t handleObject, HandleObject_t handleArray,
			 HandleObject_t handlePortal, HandleObject_t handleMemory, HandleObject_t handleService,
			 HandleObject_t handleCAS, HandleObject_t handleAtomVar, HandleObject_t handleDomainProxy,
			 HandleObject_t handleCPUStateProxy, HandleObject_t handleServicePool, HandleObject_t handleStack)
{
	ChunkInfo *chunkInfo;
	chunkInfo = GCM_CHUNKED(domain).firstChunk;
	/* update chunkinfo of current chunk */
	GCM_CHUNKED(domain).currentChunk->current = GCM_CHUNKED(domain).current;
	while (chunkInfo) {
/*		printf("   WALK %p..%p..%p (%8d)\n", chunkInfo, chunkInfo->current, chunkInfo->last, (chunkInfo->last-(jint*)chunkInfo)<<2);*/
		gc_walkContinuesBlock(domain, (char *) (chunkInfo) + sizeof(ChunkInfo), (u4_t **) & (chunkInfo->current),
				      handleObject, handleArray, handlePortal, handleMemory, handleService, handleCAS,
				      handleAtomVar, handleDomainProxy, handleCPUStateProxy, handleServicePool, handleStack);
		chunkInfo = chunkInfo->next;
	}
}

static void gc_chunked_walkHeap2(DomainDesc * domain, HandleObject_t handleObject, HandleObject_t handleArray,
				 HandleObject_t handlePortal, HandleObject_t handleMemory, HandleObject_t handleService,
				 HandleObject_t handleCAS, HandleObject_t handleAtomVar, HandleObject_t handleDomainProxy,
				 HandleObject_t handleCPUStateProxy, HandleObject_t handleServicePool, HandleObject_t handleStack)
{
	ChunkInfo *chunkInfo;
	chunkInfo = GCM_CHUNKED(domain).firstChunk2;
	/* update chunkinfo of current chunk */
	GCM_CHUNKED(domain).currentChunk2->current = GCM_CHUNKED(domain).current2;
	while (chunkInfo) {
		/*printf("   WALK2 %p..%p (%8d)\n", chunkInfo, chunkInfo->last, (chunkInfo->last-(jint*)chunkInfo)<<2); */
		gc_walkContinuesBlock(domain, (char *) (chunkInfo) + sizeof(ChunkInfo), (u4_t **) & (chunkInfo->current),
				      handleObject, handleArray, handlePortal, handleMemory, handleService, handleCAS,
				      handleAtomVar, handleDomainProxy, handleCPUStateProxy, handleServicePool, handleStack);
		chunkInfo = chunkInfo->next;
	}
}

inline int gc_chunked_isInHeap(DomainDesc * domain, ObjectDesc * obj)
{
	ChunkInfo *chunkInfo;
	chunkInfo = GCM_CHUNKED(domain).firstChunk;
	while (chunkInfo) {
		if (obj >= (char *) chunkInfo
		    && obj < (chunkInfo->current == NULL ? GCM_CHUNKED(domain).current : chunkInfo->current))
			return 1;
		chunkInfo = chunkInfo->next;
	}
	return 0;
}

static inline int gc_chunked_ensureInHeap(DomainDesc * domain, ObjectDesc * obj)
{
	if (!gc_chunked_isInHeap(domain, obj)) {
		ClassDesc *cl = obj2ClassDesc(obj);

		printf("%p\n", obj);
		printf("%s\n", cl->name);
		printf("OBJECT OUTSIDE HEAP");
		gc_chunked_printInfo(domain);
		sys_panic("OBJECT OUTSIDE HEAP");
		return 0;
	}
	return 1;
}

static inline u4_t *gc_chunked_allocHeap2(DomainDesc * domain, u4_t objSize)
{
	jint *nextObj, *data, *chunk;
	ChunkInfo *chunkInfo;

	if (!GCM_CHUNKED(domain).current2) {	/* init second semispace */
		/* heap2 */
		GCM_CHUNKED(domain).firstChunk2 = allocChunk(domain, GCM_CHUNKED(domain).initialHeapWords << 2);
		GCM_CHUNKED(domain).current2 = (char *) (GCM_CHUNKED(domain).firstChunk2) + sizeof(ChunkInfo);
		GCM_CHUNKED(domain).currentChunk2 = GCM_CHUNKED(domain).firstChunk2;
		GCM_CHUNKED(domain).endCurrentChunk2 =
		    ((char *) GCM_CHUNKED(domain).firstChunk2) + (GCM_CHUNKED(domain).initialHeapWords << 2);
		chunkInfo = (ChunkInfo *) GCM_CHUNKED(domain).firstChunk2;
		chunkInfo->current = NULL;	/* current pointer inside current chunk is not valid */
		chunkInfo->last = GCM_CHUNKED(domain).endCurrentChunk2;
		chunkInfo->next = NULL;
		/* stats */
		GCM_CHUNKED(domain).totalSizeWords = GCM_CHUNKED(domain).initialHeapWords;
		GCM_CHUNKED(domain).freeSizeWords = GCM_CHUNKED(domain).totalSizeWords - (sizeof(ChunkInfo) >> 2);
		GCM_CHUNKED(domain).usedSizeWords = GCM_CHUNKED(domain).totalSizeWords - GCM_CHUNKED(domain).freeSizeWords;
	}

      try_alloc:
	nextObj = GCM_CHUNKED(domain).current2 + objSize;
	if (nextObj >= GCM_CHUNKED(domain).endCurrentChunk2) {
		/* update chunkinfo of current chunk */
		GCM_CHUNKED(domain).currentChunk2->current = GCM_CHUNKED(domain).current2;
		/* allocate new chunk */
		chunk = allocChunk(domain, GCM_CHUNKED(domain).chunkSizeWords << 2);
		chunkInfo = (ChunkInfo *) chunk;
		/* link new chunk */
		GCM_CHUNKED(domain).currentChunk2->next = chunkInfo;
		GCM_CHUNKED(domain).currentChunk2 = chunkInfo;
		GCM_CHUNKED(domain).endCurrentChunk2 = chunk + GCM_CHUNKED(domain).chunkSizeWords;
		GCM_CHUNKED(domain).current2 = (char *) (GCM_CHUNKED(domain).currentChunk2) + sizeof(ChunkInfo);
		chunkInfo->current = NULL;	/* current pointer inside current chunk is not valid */
		chunkInfo->last = GCM_CHUNKED(domain).endCurrentChunk2;
		chunkInfo->next = NULL;
		/* stats */
		GCM_CHUNKED(domain).totalSizeWords += GCM_CHUNKED(domain).chunkSizeWords;
		GCM_CHUNKED(domain).freeSizeWords += GCM_CHUNKED(domain).chunkSizeWords - (sizeof(ChunkInfo) >> 2);

		goto try_alloc;
	}
	data = (jint *) GCM_CHUNKED(domain).current2;
	GCM_CHUNKED(domain).current2 = nextObj;
	/* update chunkinfo of current chunk */
	GCM_CHUNKED(domain).currentChunk2->current = GCM_CHUNKED(domain).current2;
	/* update statistics */
	GCM_CHUNKED(domain).freeSizeWords -= objSize;
	GCM_CHUNKED(domain).usedSizeWords += objSize;
	return data;
}




static void gc_chunked_finalizeMemoryCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	//MemoryProxy *memobj = (MemoryProxy*)obj;

	if ((flags & FORWARD_MASK) == GC_FORWARD) {
		//printf("LIVEMEM: %p %ld refcount=%ld %p %p \n", memobj->mem, memobj->size, memobj->dz->refcount, obj, ObjectDesc2ptr(obj)));
	} else {
		//printf("DEADMEM: %p %ld refcount=%ld\n", memobj->mem, memobj->size, memobj->dz->refcount);
		ASSERTMEMORY(obj);
		memory_deleted((struct MemoryProxy_s *) obj);
	}
}

static void gc_chunked_finalizePortalsCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	Proxy *p = (Proxy *) obj;
	if (!((flags & FORWARD_MASK) == GC_FORWARD)) {
		// decrement service refcount
		ASSERTCLI;
		if (p->targetDomain && (p->targetDomain->id == p->targetDomainID)) {
			if (p->targetDomain != domain) {	/* otherwise dummy portal in own domain */
				service_decRefcount(p->targetDomain, p->index);
			}
		}
	}
}

/*
 * Finalize all unreachable memory objects
 */
void gc_chunked_finalizeMemory(DomainDesc * domain)
{
	//  printf("FINALIZE MEM\n");
	gc_chunked_walkHeap(domain, NULL, NULL, NULL, gc_chunked_finalizeMemoryCB, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
}


/*
 * Finalize all garbage portals
 */
void gc_chunked_finalizePortals(DomainDesc * domain)
{
	//  printf("FINALIZE PORTALS\n");
	gc_chunked_walkHeap(domain, NULL, NULL, gc_chunked_finalizePortalsCB, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
}



void gc_chunked_checkHeap(DomainDesc * domain)
{
	gc_chunked_walkHeap(domain, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
}

void gc_chunked_gc(DomainDesc * domain)
{

#ifdef VERBOSE_GC
	printf("GARBAGE COLLECTOR started for domain %p (%s)\n", domain, domain->domainName);
#endif

	/* update chunkinfo of current chunk */
	GCM_CHUNKED(domain).currentChunk->current = GCM_CHUNKED(domain).current;

	/* 
	 * Init
	 */
	/*
	 * Move directly reachable objects onto chunked heap 
	 */
	walkRootSet(domain, gc_common_move_reference, gc_common_move_reference, gc_common_move_reference,
		    gc_common_move_reference, gc_common_move_reference, gc_common_move_reference);

	/*
	 * All directly reachable objects are now on the chunked heap
	 * Scan chunked heap 
	 */
	IF_DBG_GC(printf("Scanning chunked heap ...\n"));
	gc_common_move_scan_heap2(domain);

	/*
	 * Finish
	 */
	IF_DBG_GC(printf("Finalize memory objects ...\n"));
	gc_chunked_finalizeMemory(domain);
	gc_chunked_finalizePortals(domain);


	IF_DBG_GC(printf("Invalidating all objects of old heap"));
	{
		ChunkInfo *chunkInfo, *chunkInfo2;

		/* free secondary heap */
		chunkInfo = GCM_CHUNKED(domain).firstChunk;
		IF_DBG_GC(printf(" Free Heap chunks:\n"));
		while (chunkInfo) {
			chunkInfo2 = chunkInfo->next;
			IF_DBG_GC(printf
				  ("   %p..%p (%8d)\n", chunkInfo, chunkInfo->last,
				   ((char *) chunkInfo->last - (char *) chunkInfo)));
			freeChunk(domain, chunkInfo, (chunkInfo->last - (jint *) chunkInfo) << 2);
			chunkInfo = chunkInfo2;
		}
		GCM_CHUNKED(domain).firstChunk = GCM_CHUNKED(domain).firstChunk2;
		GCM_CHUNKED(domain).current = GCM_CHUNKED(domain).current2;
		GCM_CHUNKED(domain).currentChunk = GCM_CHUNKED(domain).currentChunk2;
		GCM_CHUNKED(domain).firstChunk2 = NULL;
		GCM_CHUNKED(domain).current2 = NULL;



	}
#ifdef VERBOSE_GC
	printf("GC finished\n");
#endif				/* VERBOSE_GC */
}

u4_t gc_chunked_freeWords(DomainDesc * domain)
{
	return GCM_CHUNKED(domain).freeSizeWords;
}

u4_t gc_chunked_totalWords(struct DomainDesc_s * domain)
{
	return GCM_CHUNKED(domain).totalSizeWords;
}

void gc_chunked_printInfo(struct DomainDesc_s *domain)
{
	ChunkInfo *chunkInfo;
	chunkInfo = GCM_CHUNKED(domain).firstChunk;
	printf("   total: %d bytes\n", GCM_CHUNKED(domain).totalSizeWords << 2);
	printf("   free: %d bytes\n", GCM_CHUNKED(domain).freeSizeWords << 2);
	printf("   used: %d bytes\n", GCM_CHUNKED(domain).usedSizeWords << 2);
	if (GCM_CHUNKED(domain).limit != -1) {
		printf("   resource principal info:\n");
		printf("      limit: %d bytes\n", GCM_CHUNKED(domain).limit);
		printf("      consumed: %d bytes\n", GCM_CHUNKED(domain).consumed);
	}
	if (GCM_CHUNKED(domain).accountTo) {
		printf("   prinicipal: domain %d\n", GCM_CHUNKED(domain).accountToID);
	}
	printf(" Heap chunks:\n");
	while (chunkInfo) {
		printf("   %p..%p (%8d)\n", chunkInfo, chunkInfo->last, (chunkInfo->last - (jint *) chunkInfo) << 2);
		chunkInfo = chunkInfo->next;
	}
}

#ifdef NEW_COPY
/* DEFUNCT!! */
void gc_chunked_setMark(struct DomainDesc_s *domain)
{
	GCM_CHUNKED(domain).mark = GCM_CHUNKED(domain).current;
}


ObjectDesc *gc_chunked_atMark(struct DomainDesc_s *domain)
{
#if 0
	u4_t *data = GCM_CHUNKED(domain).mark;
	ObjectDesc *obj;
	jint flags;
	u4_t objSize = 0;
	ClassDesc *c;

	if (data == NULL || data >= GCM_CHUNKED(domain).heapTop) {
		GCM_CHUNKED(domain).mark = NULL;
		return NULL;
	}

	ASSERT(data <= GCM_CHUNKED(domain).heapTop && data >= GCM_CHUNKED(domain).heap);

	obj = ptr2ObjectDesc(data);
	flags = getObjFlags(obj);
	switch (flags & FLAGS_MASK) {
	case OBJFLAGS_ARRAY:
		objSize = gc_objSize2(obj, flags);
		break;
	case OBJFLAGS_OBJECT:
		c = obj2ClassDesc(obj);
		ASSERTCLASSDESC(c);
		objSize = OBJSIZE_OBJECT(c->instanceSize);
		break;
	case OBJFLAGS_PORTAL:
		objSize = OBJSIZE_PORTAL;
		break;
	case OBJFLAGS_MEMORY:
		objSize = OBJSIZE_MEMORY;
		break;
	case OBJFLAGS_SERVICE:
		objSize = OBJSIZE_SERVICEDESC;
		break;
	case OBJFLAGS_SERVICE_POOL:
		objSize = OBJSIZE_SERVICEPOOL;
		break;
	case OBJFLAGS_ATOMVAR:
		objSize = OBJSIZE_ATOMVAR;
		break;
	case OBJFLAGS_CAS:
		objSize = OBJSIZE_CAS;
		break;
	case OBJFLAGS_DOMAIN:
		objSize = OBJSIZE_DOMAIN;
		break;
	case OBJFLAGS_CPUSTATE:
		objSize = OBJSIZE_THREADDESCPROXY;
		break;
	default:
		printf("OBJ=%p, FLAGS: %lx mark=%p, heap=%p heaptop=%p\n", obj, flags & FLAGS_MASK, GCM_CHUNKED(domain).mark,
		       GCM_CHUNKED(domain).heap, GCM_CHUNKED(domain).heapTop);
#ifdef DEBUG
		flags = getObjFlags(obj);
		if ((flags & FORWARD_MASK) == GC_FORWARD) {
			printf("Forward to %p\n", flags & FORWARD_PTR_MASK);
			flags = getObjFlags((ObjectDesc *) (flags & FORWARD_PTR_MASK));
		}
		dump_data(obj);
#endif
		sys_panic("WRONG HEAP DATA");
	}
	GCM_CHUNKED(domain).mark += objSize;
	return obj;
#endif
	sys_panic("gc_chunked currently does not support marking");
}
#endif


/* free resources */
void gc_chunked_done(DomainDesc * domain)
{
	u4_t size;
	ChunkInfo *chunkInfo, *chunkInfo2;

	/* free heap */
	printf("free heap of domain %d\n", domain->id);

	chunkInfo = GCM_CHUNKED(domain).firstChunk;
	IF_DBG_GC(printf(" Free Heap chunks:\n"));
	while (chunkInfo) {
		chunkInfo2 = chunkInfo->next;
		IF_DBG_GC(printf
			  ("   %p..%p (%8d)\n", chunkInfo, chunkInfo->last, ((char *) chunkInfo->last - (char *) chunkInfo)));
		freeChunk(domain, chunkInfo, (chunkInfo->last - (jint *) chunkInfo) << 2);
		chunkInfo = chunkInfo2;
	}
}


/* initialHeap: initial heap size in bytes */
/* chunkSize: size of additional heap chunks in bytes */
void gc_chunked_init(DomainDesc * domain, jint initialHeap, jint chunkSize, jint startGCatTotalSize, char *principalDomainName,
		     jint limit)
{
	u4_t heapSize;
	u4_t *start;
	ChunkInfo *chunkInfo;

	ASSERT(sizeof(GCDescUntypedMemory_t) >= sizeof(gc_chunked_mem_t));

	if (*principalDomainName != '0') {
		DomainDesc *accdomain = findDomainByName(principalDomainName);
		GCM_CHUNKED(domain).accountTo = accdomain;
		GCM_CHUNKED(domain).accountToID = accdomain->id;
	} else {
		GCM_CHUNKED(domain).accountTo = NULL;
		GCM_CHUNKED(domain).accountToID = 0;
	}
	GCM_CHUNKED(domain).limit = limit;

	/* data structures */
	heapSize = initialHeap;
	GCM_CHUNKED(domain).collectAtSizeWords = startGCatTotalSize >> 2;
	GCM_CHUNKED(domain).chunkSizeWords = chunkSize >> 2;
	GCM_CHUNKED(domain).initialHeapWords = initialHeap >> 2;

	/* heap1 */
	GCM_CHUNKED(domain).firstChunk = (ChunkInfo *) allocChunk(domain, GCM_CHUNKED(domain).initialHeapWords << 2);
	GCM_CHUNKED(domain).current = (char *) (GCM_CHUNKED(domain).firstChunk) + sizeof(ChunkInfo);
	GCM_CHUNKED(domain).currentChunk = GCM_CHUNKED(domain).firstChunk;
	GCM_CHUNKED(domain).endCurrentChunk =
	    (char *) (GCM_CHUNKED(domain).firstChunk) + (GCM_CHUNKED(domain).initialHeapWords << 2);
	chunkInfo = (ChunkInfo *) GCM_CHUNKED(domain).firstChunk;
	chunkInfo->current = NULL;	/* current pointer inside current chunk is not valid */
	chunkInfo->last = GCM_CHUNKED(domain).endCurrentChunk;
	chunkInfo->next = NULL;

	/* heap 2 */
	GCM_CHUNKED(domain).current2 = NULL;

	GCM_CHUNKED(domain).totalSizeWords = GCM_CHUNKED(domain).initialHeapWords;
	GCM_CHUNKED(domain).freeSizeWords = GCM_CHUNKED(domain).totalSizeWords - (sizeof(ChunkInfo) >> 2);
	GCM_CHUNKED(domain).usedSizeWords = GCM_CHUNKED(domain).totalSizeWords - GCM_CHUNKED(domain).freeSizeWords;


	/*
	 * functions
	 */

	/* moving  GC interface */
	GCM_MOVE_COMMON(domain).allocHeap2 = gc_chunked_allocHeap2;
	GCM_MOVE_COMMON(domain).walkHeap2 = gc_chunked_walkHeap2;

	/* general GC interface */
	domain->gc.allocDataInDomain = gc_chunked_allocDataInDomain;
	domain->gc.gc = gc_chunked_gc;
	domain->gc.done = gc_chunked_done;
	domain->gc.freeWords = gc_chunked_freeWords;
	domain->gc.totalWords = gc_chunked_totalWords;
	domain->gc.printInfo = gc_chunked_printInfo;
	domain->gc.finalizeMemory = gc_chunked_finalizeMemory;
	domain->gc.finalizePortals = gc_chunked_finalizePortals;
	domain->gc.isInHeap = gc_chunked_isInHeap;
	domain->gc.ensureInHeap = gc_chunked_ensureInHeap;
	domain->gc.walkHeap = gc_chunked_walkHeap;
#ifdef NEW_COPY
	domain->gc.setMark = gc_chunked_setMark;
	domain->gc.atMark = gc_chunked_atMark;
#endif
}


#endif				/* defined( GC_CHUNKED_IMPL ) && defined ( GC_USE_NEW ) && defined ( ENABLE_GC ) */
