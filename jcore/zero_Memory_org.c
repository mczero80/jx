#include "all.h"
#include "gc_impl.h"


#ifdef PROFILE_EVENT_MEMORY_ALLOC
static u4_t event_createMemoryIn, event_createMemoryOut, event_createMemoryMalloc, event_createMemoryMemset, event_createMemoryDZ,
    event_allocProxyIN;
#endif

#ifdef PROFILE_EVENT_MEMORY_ALLOC_AMOUNT
u4_t event_memory_alloc;
#endif				/* PROFILE_EVENT_MEMORY_ALLOC_AMOUNT */


// TODO: refcount, valid, deviceMem zusammenfassen
typedef struct DZMemoryProxy_s {
	u4_t refcount;
	u4_t valid;
	char *mem;
	u4_t size;
	jboolean deviceMem;
	struct DZMemoryProxy_s *prev;
	struct DZMemoryProxy_s *next;
	u4_t prevOwner;
	u4_t nextOwner;
#ifdef DEBUG_MEMORY_CREATION
	code_t createdAt;
	struct DomainDesc_s *createdBy;
	char *createdUsing;
#endif
} DZMemoryProxy;

typedef struct MemoryProxy_s {
	code_t *vtable;
	u4_t size;
	char *mem;
	DZMemoryProxy *dz;
	u4_t dummy0;
	u4_t dummy1;
#ifdef SMP
	u4_t busy;		/* indicates an ongoing revocation check */
#endif
} MemoryProxy;

DZMemoryProxy *createDZMemory(u4_t size, char *mem);
MemoryProxyHandle allocMemoryProxyInDomainDZM(DomainDesc * domain, ClassDesc * c, DZMemoryProxy * dzm);
MemoryProxyHandle allocMemoryProxyInDomain(DomainDesc * domain, ClassDesc * c, jint start, jint size);

/* test if memory object is valid */
#define ISVALID(_x_) memoryIsValid(_x_)
#ifdef REVOCATION_CHECK
#define CHECKVALID(self) {MemoryProxy *_x_ = (MemoryProxy*)(self); if (! ISVALID(_x_)) {extern_invalid_mem(_x_);}}
#else
#define CHECKVALID(self)
#endif				/* REVOCATION_CHECK */
/*TODO: define checkvalid using handles */


#undef CHECKVALID
#define CHECKVALID(self) {MemoryProxy *_x_ = (MemoryProxy*)(self); if (_x_->dz->valid == 0) { invalid_mem(_x_); }}
/*
#define CHECKVALID(self) {\
  MemoryProxy *_x_ = (MemoryProxy*)(self);\
  if (_x_->dz->valid==0) {invalid_mem(_x_);}\
  if (_x_->dz->refcount==0) {\
       printf("use old memory\n");\
  }\
}
*/
/* #define CHECKVALID(self) */







#ifndef NUMBER_DZMEM
#define NUMBER_DZMEM 2048
#endif

#define REDUCE_SPLITS 1
#define SHORT_GC_RUN  1

/*
#define DBG_SPLIT 1
#define DBG_REVOKE 1
*/

#ifdef REVOKE_USING_SPINLOCK
#include "spinlock.h"
static spinlock_t memory_revoke_lock = SPIN_LOCK_UNLOCKED;
#endif

#define debugm(x)
#define debugdm(x)
#define debugz(x)

#ifdef DEBUG_MEMORY_CREATION
#define INVALIDATE_DZ(__info__ , __dz__) {\
  (__dz__)->createdUsing=(__info__);\
  (__dz__)->valid=0;\
}
#define CHECK_DZ(_dz_) {\
 ASSERT((_dz_)->valid==1);\
 if ((_dz_)->prev) ASSERT(((_dz_)->prev->next==(_dz_)));\
 if ((_dz_)->prev) ASSERT(((_dz_)->prev->mem==NULL)||((_dz_)->prev->mem <= (_dz_)->mem));\
 if ((_dz_)->next) ASSERT(((_dz_)->next->mem==NULL)||((_dz_)->next->mem >= (_dz_)->mem));\
}
#else
#define INVALIDATE_DZ(__info__ , __dz__) (__dz__)->valid=0;
#define CHECK_DZ(_dz_)
#endif

#define FREE_DZMEM( __txt__,__mem__) {\
   INVALIDATE_DZMEM( __txt__,__mem__);\
   (*(__mem__))->dz->mem=NULL;\
}

#define FREE_DZ(__txt__,__mem__) {\
   INVALIDATE_DZ(__txt__,__mem__);\
   (__mem__)->mem=NULL;\
}

#ifdef REDIRECT_INVALID_DZ
#define INVALIDATE_DZMEM(__txt__,__mem__) INVALIDATE_DZ(__txt__,(*(__mem__))->dz);dzmemory_redirect_invalid_dz(__mem__);
#else
#define INVALIDATE_DZMEM(__txt__,__mem__) INVALIDATE_DZ(__txt__,(*(__mem__))->dz);
#endif

#ifdef ENFORCE_FMA
#define CP_ENFORCE_FMA domain_panic(curdom(),"memory method should not be called!")
#else
#define CP_ENFORCE_FMA
#endif				/* ENFORCE_FMA */

ClassDesc *readonlyMemoryClass = NULL;

/* prototypes */
static void dzmemory_free_chunk(DZMemoryProxy * dzm);
static DZMemoryProxy *dzmemory_alloc();
static DZMemoryProxy *dzmemory_collector();

/*
 * Memory Portal 
 */
static ClassDesc *memoryClass = NULL;
ClassDesc *deviceMemoryClass = NULL;

void invalid_mem(MemoryProxy * _x_)
{
	printf("INVALMEM: %p %p\n", _x_->dz, _x_->mem);
	cpuManager_dump(NULL, NULL, _x_);

	//  sys_panic("INVAL"); 
	exceptionHandler(THROW_InvalidMemory);
}

code_t extern_invalid_mem = invalid_mem;

static void dump_chunk_visual(DZMemoryProxy * dzm)
{
	DZMemoryProxy *nextObj;

	printf(" v obj  \tprev\t\tnext\t\tref \n");
	for (nextObj = dzm; nextObj != NULL; nextObj = nextObj->next) {
		printf(" %1d %p\t%p\t\t%p\t\t%4d ", nextObj->valid, nextObj, nextObj->prev, nextObj->next, nextObj->refcount);
#ifdef DEBUG_MEMORY_CREATION
		printf(" %s", nextObj->createdUsing);
#endif
		printf("\n");
	}
}

DZMemoryProxy *dzMemory;
DZMemoryProxy *dzMemoryBorder;
DZMemoryProxy *dzInvalid;
DZMemoryProxy *dzMemoryFreeList;


#ifdef DEBUG_MEMORY_CREATION
static int dzmemcount = 0;
#endif

static void dzmemory_gcdomain(DomainDesc * domain)
{
	if (domain->id == 0)
		return;
#ifdef ENABLE_GC
#ifdef SHORT_GC_RUN
	if (domain->id == 1)
		return;
	if (dzMemoryFreeList != NULL && dzMemoryFreeList->next != NULL)
		return;
#endif
	if (domain->gc.gcThread == NULL) {
		printf("Domain %d\n", (int) domain->id);
		domain_panic(domain, "GC but no GC thread available");
	}
	start_thread_using_code1(domain->gc.gcObject, domain->gc.gcThread, domain->gc.gcCode, (u4_t) domain);
#endif
}

void memory_deleted(struct MemoryProxy_s *obj)
{
	dzmemory_decRefcount(obj);
}

static void dzmemory_decRefcount(MemoryProxy * mp)
{
	DZMemoryProxy *m = mp->dz;
	u4_t refcount;
	do {
		refcount = m->refcount;
	} while (!cas(&m->refcount, refcount, refcount - 1));
	if (m->refcount == 0)
		dzmemory_free_chunk(m);
}

static void dzmemory_incRefcount(MemoryProxy * mp)
{
	DZMemoryProxy *m = mp->dz;
	u4_t refcount;
	do {
		refcount = m->refcount;
	} while (!cas(&m->refcount, refcount, refcount + 1));
}

static void dzmemory_init()
{
	DZMemoryProxy *dzm;

	dzMemory = jxmalloc(NUMBER_DZMEM * sizeof(DZMemoryProxy) MEMTYPE_OTHER);
	dzMemoryBorder = dzMemory + NUMBER_DZMEM;

	dzMemoryFreeList = NULL;
	for (dzm = dzMemory; dzm < dzMemoryBorder; dzm++) {
		dzm->refcount = 0;
		dzm->valid = 2;
		dzm->mem = NULL;
		dzm->prev = NULL;
		dzm->next = dzMemoryFreeList;
		dzMemoryFreeList = dzm;
#ifdef DEBUG_MEMORY_CREATION
		dzmemcount++;
#endif
	}

	dzm = dzMemory;
	dzInvalid = dzmemory_alloc();
	dzInvalid->valid = 0;
#ifdef DEBUG_MEMORY_CREATION
	dzInvalid->createdAt = getCaller(1);
	dzInvalid->createdBy = curdom();
	dzInvalid->deviceMem = JNI_FALSE;
#endif
}


/*
 * dzmemory_alloc
 *
 * This method is allocating a new dzmemory.
 *
 * Warn: This method may invoke the gc!
 *
 */
static DZMemoryProxy *dzmemory_alloc()
{
	DZMemoryProxy *dzm;

#ifdef DEBUG_MEMORY_CREATION
#if 0
	DISABLE_IRQ;
	if (do_sampling) {
		printf("**COUNT: %d\n", dzmemcount);
		printStackTraceNew("DZMEM");
	}
	RESTORE_IRQ;
#endif
#endif

	do {
		while ((dzm = dzMemoryFreeList) == NULL) {
			if ((dzm = dzmemory_collector()) != NULL)
				goto leaf_alloc;
		}
	} while (!cas(&dzMemoryFreeList, dzm, dzMemoryFreeList->next));

      leaf_alloc:

#ifdef DEBUG_MEMORY_CREATION
	DISABLE_IRQ;
	dzmemcount--;
	RESTORE_IRQ;

	ASSERT(dzm->mem == NULL);
	ASSERT(dzm->valid == 2);
	ASSERT(dzm->prev == NULL);
	ASSERT(dzm->refcount == 0);
#endif
	dzm->valid = 0;
	dzm->next = NULL;
	dzm->refcount = 1;

	return dzm;
}

static void dzmemory_free(DZMemoryProxy * dzm)
{
#ifdef DEBUG_MEMORY_CREATION
	ASSERT(dzm);
	ASSERT(dzm->mem == NULL);
	ASSERT(dzm->prev == NULL);
	ASSERT(dzm->next == NULL);
	ASSERT(dzm->valid != 2);
#endif
	ASSERT(dzm->refcount == 0);
	dzm->valid = 2;
	do {
		dzm->next = dzMemoryFreeList;
	} while (!cas(&dzMemoryFreeList, dzm->next, dzm));
#ifdef DEBUG_MEMORY_CREATION
	DISABLE_IRQ;
	dzmemcount--;
	RESTORE_IRQ;
#endif
}

static void dzmemory_free_chunk(DZMemoryProxy * dzm)
{
	DZMemoryProxy *tmp, *nextObj;

	/* check refcounts for chunk tail */
	for (nextObj = dzm; nextObj != NULL; nextObj = nextObj->next)
		if (nextObj->refcount)
			return;

	ASSERT(dzm->refcount == 0);

	if (dzm->prev || dzm->next) {
		/* find top element in chunk and check refcounts */
		while (dzm->prev != NULL) {
#ifdef DEBUG_MEMORY_CREATION
			if (dzm->prev->next != dzm) {
				dump_chunk_visual(dzm);
				dump_chunk_visual(dzm->prev);
				ASSERT(dzm->prev->next == dzm);
			}
#else
			ASSERT(dzm->prev->next == dzm);
			ASSERT(dzm->prev->mem == NULL || dzm->mem == NULL || (dzm->prev->mem + dzm->prev->size) == dzm->mem);
#endif
			dzm = dzm->prev;
			if (dzm->refcount)
				return;
		}

		/* free elements */
		nextObj = dzm->next;
		while (nextObj != NULL) {

			ASSERT(nextObj->refcount == 0);
			ASSERT(dzm->mem == NULL || nextObj->mem == NULL || nextObj->mem == (dzm->mem + dzm->size));

			dzm->size += nextObj->size;	/* compute size */
			nextObj->prev = NULL;	/* break prev link */
			nextObj->mem = NULL;	/* top memory pointer is saved in dzm */
			nextObj->size = NULL;
#ifdef DEBUG_MEMORY_CREATION
			nextObj->createdUsing = "!dzmem_free_chunk1";
#endif
			tmp = nextObj->next;
			nextObj->next = NULL;
			dzmemory_free(nextObj);
			nextObj = tmp;
		}
		dzm->next = NULL;
	}

	ASSERT(dzm->prev == NULL);
	ASSERT(dzm->next == NULL);
	ASSERT(dzm->refcount == 0);

	if (dzm->mem) {
		ASSERT(dzm->size > 4);
#ifdef DEBUG_MEMORY_CREATION
		dzm->createdUsing = "!dzmem_free_chunk2";
#endif
		jxfree(dzm->mem, dzm->size MEMTYPE_OTHER);
		dzm->mem = NULL;
	}
	dzmemory_free(dzm);

	return;
}

static void dzmemory_alive(MemoryProxy * mp)
{
	DZMemoryProxy *dzm = mp->dz;
	dzm->valid = dzm->valid & 3;
}

static void dzmemory_mark()
{
	DZMemoryProxy *d, *m;
	for (d = dzMemory; d < dzMemoryBorder; d++) {
		m = (DZMemoryProxy *) (d);
		m->valid = m->valid | 16;
	}
}

static void dzmemory_check()
{
	DZMemoryProxy *d, *m;
	int count, refcount, lost, inchunck, bad;

	count = 0;
	refcount = 0;
	lost = 0;
	inchunck = 0;
	bad = 0;
	for (d = dzMemory; d < dzMemoryBorder; d++) {
		m = (DZMemoryProxy *) (d);

		if (m->valid & 16) {
			/* memobj looks dead */
			m->valid = m->valid & 3;
			if (m->refcount > 0) {
				bad++;
			}
		}
		refcount += (int) (m->refcount);
		if (m->valid != 2)
			count++;
		if (m->refcount == 0 && m->valid != 2) {
			dzmemory_free_chunk(m);
			if (m->valid == 2)
				lost++;
			else
				inchunck++;
		}
	}

	printf("dzmemory refs: %d used: %d/%d lost: %d:%d bad: %d\n", refcount, count, NUMBER_DZMEM, lost, inchunck, bad);
}

static DZMemoryProxy *dzmemory_collector()
{
	DZMemoryProxy *dzm;

	DISABLE_IRQ;

#ifdef DEBUG_MEMORY_CREATION
#if 0
	printf("\ndzmemory_collector called from domain %d:\n", curdom()->id);
	printStackTraceNew("DZMEMORY");
#endif
#endif

#ifdef DEBUG_MEMORY_REFCOUNT
	dzmemory_mark();
#endif
	foreachDomain(dzmemory_gcdomain);
#ifdef DEBUG_MEMORY_REFCOUNT
	dzmemory_check();
#endif

	if ((dzm = dzMemoryFreeList) != NULL)
		dzMemoryFreeList = dzm->next;

	if (dzm == NULL) {
		printf("out of free dzmemory!\n");
#ifdef MONITOR
		monitor(NULL);
#endif
		goto leaf_collector;
	}

	ASSERT(dzm);
	ASSERT(dzm != dzMemoryFreeList);
	ASSERT(dzm->refcount == 0);
	ASSERT(dzm->mem == 0);

      leaf_collector:

	RESTORE_IRQ;

	return dzm;
}

#ifdef REDIRECT_INVALID_DZ
static void dzmemory_redirect_invalid_dz(MemoryProxyHandle mem)
{
	DZMemoryProxy *dzm = (*mem)->dz;

	ASSERT(dzm->valid == 0);

	dzmemory_incRefcount(dzInvalid);
	(*mem)->dz = dzInvalid;
	dzmemory_decRefcount(dzm);
}
#endif

/*
 * dzmemory_split3
 *
 * Is splitting dzm into three new parts dzm[0-2].
 * Caller should invalidate old dzmemory.
 *
 * Warn: This method may invoke the gc!
 *
 */
static int dzmemory_split3(DZMemoryProxy * dzm, jint offset, jint len, DZMemoryProxy ** dzm0, DZMemoryProxy ** dzm1,
			   DZMemoryProxy ** dzm2)
{
	DomainDesc *domain;

	ASSERT(dzm != NULL);
	ASSERT(dzm->valid == 1);
	ASSERT(dzm->mem != NULL);

	if (offset < 0 || len < 0 || offset + len > dzm->size)
		return -1;

	domain = curdom();

	// split into three parts
	*dzm0 = createDZMemory(offset, dzm->mem);
	*dzm1 = createDZMemory(len, dzm->mem + offset);
	*dzm2 = createDZMemory(dzm->size - len - offset, dzm->mem + len + offset);

	if (dzm->prev)
		dzm->prev->next = *dzm0;
	(*dzm0)->prev = dzm->prev;
	(*dzm0)->next = *dzm1;
	(*dzm0)->nextOwner = domain->id;
	(*dzm0)->prevOwner = dzm->prevOwner;
	(*dzm1)->prev = *dzm0;
	(*dzm1)->next = *dzm2;
	(*dzm1)->nextOwner = domain->id;
	(*dzm1)->prevOwner = domain->id;
	(*dzm2)->prev = *dzm1;
	(*dzm2)->next = dzm->next;
	(*dzm2)->nextOwner = dzm->nextOwner;
	(*dzm2)->prevOwner = domain->id;
	if (dzm->next)
		dzm->next->prev = *dzm2;

	dzm->prev = NULL;
	dzm->next = NULL;
	dzm->valid = 0;
	dzm->mem = NULL;

#ifdef DEBUG_MEMORY_CREATION
	(*dzm0)->createdAt = getCaller(2);
	(*dzm1)->createdAt = getCaller(2);
	(*dzm2)->createdAt = getCaller(2);
	(*dzm0)->createdUsing = "split3";
	(*dzm1)->createdUsing = "split3";
	(*dzm2)->createdUsing = "split3";
#endif

	CHECK_DZ(*dzm0);
	CHECK_DZ(*dzm1);
	CHECK_DZ(*dzm2);

	return 0;
}

/*
 * dzmemory_split2
 *
 * Splits dzm into two new parts (dzm[0-1]).
 * Caller should invalidate old dzmemory.
 *
 * Warn: This method may invoke the gc!
 *
 */
static int dzmemory_split2(DZMemoryProxy * dzm, jint offset, DZMemoryProxy ** dzm0, DZMemoryProxy ** dzm1)
{
	DomainDesc *domain;

	ASSERT(dzm != NULL);
	ASSERT(dzm->valid == 1);
	ASSERT(dzm->mem != NULL);

	if (offset < 0 || offset > dzm->size)
		return -1;

	domain = curdom();

	*dzm0 = createDZMemory(offset, dzm->mem);
	*dzm1 = createDZMemory(dzm->size - offset, dzm->mem + offset);

	if (dzm->prev)
		dzm->prev->next = *dzm0;
	(*dzm0)->prev = dzm->prev;
	(*dzm0)->prevOwner = dzm->prevOwner;
	(*dzm0)->next = *dzm1;
	(*dzm0)->nextOwner = domain->id, (*dzm1)->prev = *dzm0;
	(*dzm1)->prevOwner = domain->id, (*dzm1)->next = dzm->next;
	(*dzm1)->nextOwner = dzm->nextOwner;
	if (dzm->next)
		dzm->next->prev = *dzm1;

	dzm->prev = NULL;
	dzm->next = NULL;
	dzm->mem = NULL;
	dzm->valid = 0;

#ifdef DEBUG_MEMORY_CREATION
	(*dzm0)->createdAt = getCaller(2);
	(*dzm1)->createdAt = getCaller(2);
	(*dzm0)->createdUsing = "split2";
	(*dzm1)->createdUsing = "split2";
#endif

	CHECK_DZ(*dzm0);
	CHECK_DZ(*dzm1);

	return 0;
}

/*
 * dzmemory_join 
 *
 * Is joining dzm1 and dzm2 to a new dzmemory.
 * Caller should invalidate old dzmemorys.
 *
 * Warn: This method may invoke the gc!
 *
 */
static DZMemoryProxy *dzmemory_join(DZMemoryProxy * dzm1, DZMemoryProxy * dzm2)
{
	DZMemoryProxy *dzm;

	ASSERT(dzm1);
	ASSERT(dzm2);

	ASSERT(dzm1->valid == 1);
	ASSERT(dzm2->valid == 1);
	ASSERT(dzm1->mem);
	ASSERT(dzm2->mem);

	ASSERT(dzm1->mem <= dzm2->mem);
	ASSERT(dzm1->next == dzm2);
	ASSERT(dzm2->prev == dzm1);

	dzm = createDZMemory(dzm1->size + dzm2->size, dzm1->mem);

	if (dzm1->prev)
		dzm1->prev->next = dzm;
	dzm->prev = dzm1->prev;
	dzm->prevOwner = dzm1->prevOwner;
	dzm->next = dzm2->next;
	dzm->nextOwner = dzm2->nextOwner;
	if (dzm2->next)
		dzm->next->prev = dzm;

	dzm1->prev = NULL;
	dzm1->valid = 0;
	dzm1->mem = NULL;
	dzm2->next = NULL;
	dzm2->valid = 0;
	dzm2->mem = NULL;

	return dzm;
}

void print_memobj(jint domainID)
{
	DZMemoryProxy *d, *m;
	int count, refcount;

	printf("%1s %10s %10s %6s %4s", "v", "dz", "mem", "size", "refc");
#ifdef DEBUG_MEMORY_CREATION
	printf(" %3s %s", "did", "creator");
#endif
	printf("\n");
	count = 0;
	refcount = 0;
	for (d = dzMemory; d < dzMemoryBorder; d++) {
		m = (DZMemoryProxy *) (d);
		if (m->valid != 2)
			count++;
#ifdef DEBUG_MEMORY_CREATION
		if (m->prev && m->valid != 2) {
			if (m->prev->valid == 2) {
				printf("!! %1d 0x%08x 0x%08x %6d %4d", (int) m->valid, (void *) m, (void *) (m->mem),
				       (int) (m->size), (int) (m->refcount));
				if (m->createdBy) {
					printf(" %3d ", (int) (m->createdBy->id));
					printf(" ");
					print_eip_info(m->createdAt);
				}
				if (m->createdUsing)
					printf(" %s!!\n", m->createdUsing);
			}
		}
#endif
		if (m->refcount == 0)
			continue;
#ifdef DEBUG_MEMORY_CREATION
		if (domainID != -1 && m->createdBy && m->createdBy->id != domainID)
			continue;
#endif
		printf("%1d 0x%08x 0x%08x %6d %4d", (int) m->valid, (void *) m, (void *) (m->mem), (int) (m->size),
		       (int) (m->refcount));
		refcount += (int) (m->refcount);
#ifdef DEBUG_MEMORY_CREATION
		if (m->createdBy) {
			printf(" %3d ", (int) (m->createdBy->id));
			printf(" ");
			print_eip_info(m->createdAt);
		}
		if (m->createdUsing)
			printf(" %s ", m->createdUsing);
#endif				/*DEBUG_MEMORY_CREATION */
		printf("\n");
	}
	printf("dzmemory refs: %d used: %d max: %d\n", refcount, count, NUMBER_DZMEM);
}

void assert_memory(MemoryProxy * x)
{
	u4_t flags = getObjFlags(x);
	if ((flags & FORWARD_MASK) == GC_FORWARD)
		return;		/* forwarding pointer. do not check */
	if ((flags & FLAGS_MASK) != OBJFLAGS_MEMORY) {
		printf("FLAGS: obj=%p flags=%x flagword=%x\n", x, flags & FLAGS_MASK, flags);
		sys_panic("no mem flags");
	}
	//if (((struct MemoryProxy_s*)(x))->dz==NULL) sys_panic("no dzmem");
}


DZMemoryProxy *createDZMemory(u4_t size, char *mem)
{
	DZMemoryProxy *dzm = dzmemory_alloc();
	//if (size==42)  printStackTraceNew("DZM");
	dzm->size = size;
	dzm->mem = mem;
	// refcount is set in alloc //dzm->refcount = 1;
	dzm->valid = 1;
	dzm->deviceMem = JNI_FALSE;
	dzm->prev = NULL;
	dzm->next = NULL;
	dzm->prevOwner = NULL;
	dzm->nextOwner = NULL;
#ifdef DEBUG_MEMORY_CREATION
	dzm->createdAt = getCaller(2);
	dzm->createdBy = curdom();
#endif
	return dzm;
}

MemoryProxyHandle allocReadOnlyMemory(ObjectDesc * self, jint start, jint size)
{
	MemoryProxyHandle mem;

	//  DZMemoryProxy* dzm = createDZMemory(size, (code_t)start);
	mem = allocMemoryProxyInDomain(curdom(), readonlyMemoryClass, start, size);

	ASSERTHANDLE(mem);

#ifdef DEBUG_MEMORY_CREATION
	(*mem)->dz->createdAt = getCaller(1);
	(*mem)->dz->createdBy = curdom();
	(*mem)->dz->deviceMem = JNI_FALSE;
#endif

	return mem;
}

MemoryProxyHandle createMemoryInstance(DomainDesc * domain, jint size, jint bytes)
{
	MemoryProxyHandle m;
	jint rem;
	jint *d;
	DZMemoryProxy *dzm;

#ifdef PROFILE_EVENT_MEMORY_ALLOC
	RECORD_EVENT(event_createMemoryIn);
#endif

	/* TODO: check quota here */
	d = jxmalloc(size + bytes - 1 MEMTYPE_MEMOBJ);
	domain->memoryObjectBytes += size + bytes - 1;
#ifdef PROFILE_EVENT_MEMORY_ALLOC_AMOUNT
	RECORD_EVENT_INFO2(event_memory_alloc, domain->id, size + bytes - 1);
#endif				/* PROFILE_EVENT_MEMORY_ALLOC */

	if (bytes > 1) {
		rem = ((u4_t) d % bytes);
		if (rem > 0) {
			d = (jint *) (((u4_t) d) + bytes - rem);
		}
	}
#ifdef PROFILE_EVENT_MEMORY_ALLOC
	RECORD_EVENT(event_createMemoryMalloc);
#endif

	jxmemset(d, 0, size);

	//memset(d, 0, size);

#ifdef PROFILE_EVENT_MEMORY_ALLOC
	RECORD_EVENT(event_createMemoryMemset);
#endif

	//  dzm = createDZMemory(size, d);
	m = allocMemoryProxyInDomain(domain, memoryClass, d, size);

#ifdef DEBUG_MEMORY_CREATION
	(*m)->dz->createdUsing = "createMemoryInstance";
	(*m)->dz->createdAt = getCaller(2);
	(*m)->dz->createdBy = domain;
#endif

#ifdef PROFILE_EVENT_MEMORY_ALLOC
	RECORD_EVENT(event_createMemoryOut);
#endif

	return m;
}

/****
 * Memory proxy access functions.
 */

/** THIS FUNCTION MUST NOT BE INTERRUPTED BY A GC */
int nonatomic_memoryIsValid(MemoryProxyHandle handle)
{
	MemoryProxy *_x_ = *handle;
	return (_x_->dz->valid == 1);
}

/** THIS FUNCTION MUST NOT BE INTERRUPTED BY A GC */
char *nonatomic_memoryGetMem(MemoryProxyHandle handle)
{
	/*  ASSERTHANDLE(handle); */
	return (*handle)->mem;
}

extern code_t extern_panic;
extern code_t extern_printf;

/** THIS FUNCTION MUST NOT BE INTERRUPTED BY A GC */
jint nonatomic_memoryGetSize(MemoryProxyHandle handle)
{
	/*  ASSERTHANDLE(handle); */
	return (*handle)->size;
}

/** THIS FUNCTION MUST NOT BE INTERRUPTED BY A GC */
jint nonatomic_memoryGetValid(MemoryProxyHandle handle)
{
	/*  ASSERTHANDLE(handle); */
	return (*handle)->dz->valid;
}

/** THIS FUNCTION MUST NOT BE INTERRUPTED BY A GC */
void nonatomic_memorySetValid(MemoryProxyHandle handle, jint v)
{
	DZMemoryProxy *dz;
	DomainDesc *cdom;

	/*ASSERTHANDLE(handle); */
	if (!v) {
		cdom = extern_dom();
		dz = (*handle)->dz;
		while ((dz->prevOwner == cdom->id) && (dz = dz->prev))
			INVALIDATE_DZ("!SetValid", dz);
		dz = (*handle)->dz;
		while ((dz->nextOwner == cdom->id) && (dz = dz->next))
			INVALIDATE_DZ("!SetValid", dz);
		INVALIDATE_DZMEM("!SetValid", handle);
	} else {
		(*handle)->dz->valid = v;
	}
}

#define ISNULLHANDLE(h) ((h) == NULL || *(h) == NULL)

void memory_set8(MemoryProxy * self, jint where, jbyte what)
{
	jbyte *ptr;
	jint size;

	CP_ENFORCE_FMA;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);

#ifdef REVOKE_USING_CLI
	DISABLE_IRQ;
#endif				/* REVOKE_USING_CLI */

#ifdef REVOKE_USING_SPINLOCK
	spin_lock(&memory_revoke_lock);
#endif				/*  REVOKE_USING_SPINLOCK */
	CHECKVALID(self);

	ptr = (jbyte *) self->mem;
	ASSERT(ptr != NULL);
	size = self->size;

	/* debugz(("Memory set8 %lx pos=%ld value=%d\n", (jint)self, pos, value); */
	if (where >= size) {
		DZMemoryProxy *m = (DZMemoryProxy *) (self->dz);
		printf("dzm:%p mem:%p size:%d ref:%d valid:%d\n", m, m->mem, m->size, m->refcount, m->valid);
#ifdef DEBUG_MEMORY_CREATION
		printf(", created at: ");
		print_eip_info(m->createdAt);
		printf(", by: %s\n", m->createdBy->domainName);
#endif
		printf("where>=size: where=%d size=%d\n", where, size);
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	}
	ptr[where] = what;
#ifdef REVOKE_USING_SPINLOCK
	spin_unlock(&memory_revoke_lock);
#endif				/*  REVOKE_USING_SPINLOCK */
#ifdef REVOKE_USING_CLI
	RESTORE_IRQ;
#endif				/* REVOKE_USING_CLI */
}

jbyte memory_get8(MemoryProxy * self, jint where)
{
	jbyte *ptr;
	jint size;
	jbyte result;

	CP_ENFORCE_FMA;

	if (self == NULL)
		sys_panic("STRANGE NULL");

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);

#ifdef REVOKE_USING_CLI
	DISABLE_IRQ;
#endif				/* REVOKE_USING_CLI */
#ifdef REVOKE_USING_SPINLOCK
	spin_lock(&memory_revoke_lock);
#endif				/*  REVOKE_USING_SPINLOCK */
	CHECKVALID(self);

	ptr = (jbyte *) self->mem;
	size = self->size;

	/*debugz(("Memory get8 %lx pos=%ld\n", (jint)self, pos); */
	if (where >= size || where < 0)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	result = ptr[where];
#ifdef REVOKE_USING_SPINLOCK
	spin_unlock(&memory_revoke_lock);
#endif				/*  REVOKE_USING_SPINLOCK */
#ifdef REVOKE_USING_CLI
	RESTORE_IRQ;
#endif				/* REVOKE_USING_CLI */
	return result;
}

void memory_set16(ObjectDesc * self, jint where, jshort what)
{
	jshort *ptr;
	jint size;

#if 0
	CP_ENFORCE_FMA;
#endif
	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);

	ptr = (jshort *) self->data[1];
	size = self->data[0];

	/* debugz(("Memory set16 %lx pos=%ld value=%d\n", (jint)self, pos, value); */
	if (where >= size / sizeof(jshort))
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	ptr[where] = what;
}

jshort memory_get16(ObjectDesc * self, jint where)
{
	jshort *ptr;
	jint size;
#if 0
	CP_ENFORCE_FMA;
#endif
	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);

	ptr = (jshort *) self->data[1];
	size = self->data[0];

	/*debugz(("Memory get16 %lx pos=%ld\n", (jint)self, pos); */
	if (where >= size / sizeof(jshort))
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	return ptr[where];
}

#ifdef SMP
void memory_set32(ObjectDesc * self, jint where, jint what)
{
	sys_panic("MEMORY REVOCATION CHECK HAS TO BEE IMPLEMENTED FOR SMPxs");
	/*
	   MemoryProxy * ptr = (MemoryProxy *)self;
	   jint size = self->size;

	   // lock bus and set busy flag 
	   asm volatile(...);
	   CHECKVALID(self);
	   if (where >= (size >> 2)) extern_panic("Writing Memory access out of range: %ld, %ld", where, size);
	   ptr->data[where] = what;
	   // lock bus and clear busy flag 
	   asm volatile(...);
	 */
}
#else
void memory_set32(ObjectDesc * self, jint where, jint what)
{
	jint *ptr = (jint *) self->data[1];
	jint size = self->data[0];

	CP_ENFORCE_FMA;

	ASSERTMEMORY(self);

	CHECKVALID(self);
	if (where >= (size >> 2))
		exceptionHandlerMsg(THROW_RuntimeException, "Writing Memory access out of range");
	ptr[where] = what;
}
#endif

jint memory_get32(ObjectDesc * self, jint where)
{
	jint *ptr = (jint *) self->data[1];
	jint size = self->data[0];

	CP_ENFORCE_FMA;
	ASSERTMEMORY(self);
	CHECKVALID(self);

	if (where >= size / sizeof(jint))
		extern_panic("Reading Memory access out of range");
	return ptr[where];
}

extern void jxwmemset(void *, jshort, jint);

void memory_fill16(ObjectDesc * self, jshort what, jint where, jint length)
{
	jint size;
	jshort *ptr;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);

	size = self->data[0];
	ptr = (jshort *) self->data[1];
#ifdef SAMPLE_FASTPATH
	if (do_sampling)
		printStackTrace("SLOWOPERATION-FILL16 ", curthr(), &self - 2);
#endif
	debugdm(("Memory fill16 %lx where=%ld what=%d length=%ld\n", (jint) self, where, what, length));
	if ((where + length) > size / sizeof(jshort))
		exceptionHandler(THROW_MemoryIndexOutOfBounds);;
//    for(ptr += where; length > 0; ++ptr, --length)
//      *ptr = what;
	jxwmemset(ptr + where, what, length * 2);
}

void memory_fill32(ObjectDesc * self, jint what, jint where, jint length)
{
	jint size;
	jint *ptr;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);

	size = self->data[0];
	ptr = (jint *) self->data[1];
#ifdef SAMPLE_FASTPATH
	if (do_sampling)
		printStackTrace("SLOWOPERATION-FILL32 ", curthr(), &self - 2);
#endif
	debugdm(("Memory fill32 %lx where=%ld what=%ld length=%ld\n", (jint) self, where, what, length));
	if ((where + length) > size / sizeof(jint))
		exceptionHandler(THROW_MemoryIndexOutOfBounds);;
	for (ptr += where; length > 0; ++ptr, --length)
		*ptr = what;

}

void memory_clear(ObjectDesc * self)
{
	jint size;
	jint *ptr;
	int i;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);

	size = self->data[0] / sizeof(jint);
	ptr = (jint *) self->data[1];

#ifdef SAMPLE_FASTPATH
	//if (do_sampling) printStackTrace("SLOWOPERATION-CLEAR ", curthr(), &self-2);
#endif

	jxwmemset(ptr, 0, size);
	//for (i=0;i<size;i++) ptr[i]=0;
}

void memory_move(ObjectDesc * self, jint __dst, jint __src, jint count)
{
	jint size = self->data[0];
	jbyte *dst = (jbyte *) self->data[1] + __dst;
	jbyte *src = (jbyte *) self->data[1] + __src;
	int d0, d1, d2, d3;

	if ((__dst + count) > size || (__src + count) > size)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);

	if (dst < src) {
		__asm__ __volatile__("cld\n\t" "shrl $1,%%ecx\n\t" "jnc 1f\n\t" "movsb\n" "1:\tshrl $1,%%ecx\n\t" "jnc 2f\n\t"
				     "movsw\n" "2:\trep\n\t" "movsl":"=&c"(d0), "=&D"(d1), "=&S"(d2)
				     :"0"(count), "1"((long) dst), "2"((long) src)
				     :"memory");
	} else {
		__asm__ __volatile__("std\n\t" "shrl $1,%%ecx\n\t" "jnc 1f\n\t" "movb 3(%%esi),%%al\n\t" "movb %%al,3(%%edi)\n\t"
				     "decl %%esi\n\t" "decl %%edi\n" "1:\tshrl $1,%%ecx\n\t" "jnc 2f\n\t" "movw 2(%%esi),%%ax\n\t"
				     "movw %%ax,2(%%edi)\n\t" "decl %%esi\n\t" "decl %%edi\n\t" "decl %%esi\n\t" "decl %%edi\n"
				     "2:\trep\n\t" "movsl\n\t" "cld":"=&c"(d0), "=&D"(d1), "=&S"(d2), "=&a"(d3)
				     :"0"(count), "1"(count - 4 + (long) dst), "2"(count - 4 + (long) src)
				     :"memory");
	}
	//return dst;
}

jint memory_getStartAddress(ObjectDesc * self)
{
	char *ptr;
	jint size;

	CP_ENFORCE_FMA;

	CHECKVALID(self);

	ptr = self->data[1];
	size = self->data[0];

	return (jint) ptr;
}

jint memory_size(ObjectDesc * self)
{
	jint size = self->data[0];
	CP_ENFORCE_FMA;
	return size;
}

void memory_copy(ObjectDesc * self, jint from, jint to, jint len)
{
	jint size, i;
	char *mem;
	CHECK_NULL_PTR(self);
	CHECKVALID(self);
	size = self->data[0];
	if (to + len > size) {
		sys_panic("to+len out of range");
	}
	if (from + len > size) {
		sys_panic("from+len out of range");
	}
	mem = (jbyte *) self->data[1];
	for (i = 0; i < len; i++) {
		mem[to + i] = mem[from + i];
	}
}

void memory_copyToByteArray(ObjectDesc * self, ArrayDesc * bytes, jint array_offset, jint mem_offset, jint len)
{
	jint dstSize, srcSize;
	char *src;
	jint *dst;		/* element of byte array is 32 bits ! */
	jint i;

	CHECK_NULL_PTR(self);
	CHECK_NULL_PTR(bytes);

	CHECKVALID(self);
	srcSize = self->data[0];
	dstSize = bytes->size;

	if (mem_offset + len > srcSize)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	if (array_offset + len > dstSize)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);

	src = self->data[1];
	dst = bytes->data;
	src += mem_offset;
	dst += array_offset;
	for (i = 0; i < len; i++) {
		*dst++ = *src++;
	}
}

void memory_copyFromByteArray(ObjectDesc * self, ArrayDesc * bytes, jint array_offset, jint mem_offset, jint len)
{
	jint dstSize, srcSize;
	char *dst;
	jint *src;		/* element of byte array is 32 bits ! */
	jint i;

	CHECK_NULL_PTR(self);
	CHECK_NULL_PTR(bytes);

	CHECKVALID(self);
	dstSize = self->data[0];
	srcSize = bytes->size;

	if (mem_offset + len > dstSize) {
#ifdef DEBUG
		printf("mem_offset+len > dstSize : %d+%d>%d\n", mem_offset, len, dstSize);
#endif
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	}

	if (array_offset + len > srcSize) {
#ifdef DEBUG
		printf("array_offset+len > srcSize : %d+%d>%d\n", array_offset, len, srcSize);
#endif
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	}

	dst = self->data[1];
	src = bytes->data;
	dst += mem_offset;
	src += array_offset;
	for (i = 0; i < len; i++) {
		*dst++ = *src++;
	}
}

void memory_copyToMemory(MemoryProxy * self, MemoryProxy * dst, jint srcOffset, jint dstOffset, jint len)
{
	jbyte *mem, *smem;
	jint dstSize, srcSize;
	jint i;

	debugz(("Memory copyToMem %lx \n", (jint) self));

	CHECK_NULL_PTR(self);
	CHECK_NULL_PTR(dst);

	ASSERTMEMORY(self);
	ASSERTMEMORY(dst);
	CHECKVALID(self);

	dstSize = dst->size;
	srcSize = self->size;
	if ((dstOffset + len > dstSize) || (srcOffset + len > srcSize))
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	mem = (jbyte *) dst->mem + dstOffset;
	smem = (jbyte *) self->mem + srcOffset;
	/*printf("COPYTOMEM: %p %p %ld\n", smem, mem, len); */
#if 0
	for (i = 0; i < len; i++) {
		mem[dstOffset + i] = smem[srcOffset + i];
	}
#else
	//  memcpy(mem + dstOffset, smem + srcOffset,  len);
	if ((((u4_t) smem | (u4_t) mem | (u4_t) len) & 0x3) == 0) {
		jxwordcpy(smem, mem, len >> 2);
	} else {
		jxbytecpy(smem, mem, len);
	}
#endif
}

void memory_copyFromMemory(MemoryProxy * self, MemoryProxy * src, jint srcOffset, jint dstOffset, jint len)
{
	jbyte *mem, *smem;
	jint i;
	jint dstSize, srcSize;

	debugz(("Memory copyFromMem %lx \n", (jint) self));

	CHECK_NULL_PTR(self);
	CHECK_NULL_PTR(src);

	ASSERTMEMORY(self);
	ASSERTMEMORY(src);
	CHECKVALID(self);
	CHECKVALID(src);
	smem = (jbyte *) src->mem + srcOffset;
	mem = (jbyte *) self->mem + dstOffset;
	dstSize = self->size;
	srcSize = src->size;
	if ((dstOffset + len > dstSize) || (srcOffset + len > srcSize))
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
#if 0
	for (i = 0; i < len; i++) {
		mem[dstOffset + i] = smem[srcOffset + i];
	}
#else
	/*printf("COPYFROMMEM: %p %p %ld\n", smem, mem, len); */
	if ((((u4_t) smem | (u4_t) mem | (u4_t) len) & 0x3) == 0) {
		jxwordcpy(smem, mem, len >> 2);
	} else {
		jxbytecpy(smem, mem, len);
	}
#endif

}

ObjectDesc *memory_getSubRange(MemoryProxy * self, jint offset, jint len)
{
	MemoryProxyHandle new_mem;
	DZMemoryProxy *dzm0, *dzm1, *dzm2;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);

	DISABLE_IRQ;

	ASSERT(self->size == self->dz->size);
	ASSERT(self->mem == self->dz->mem);

#ifdef REDUCE_SPLITS
	if (offset + len == self->dz->size) {
		/* this call may invoke the gc !! */
		if (dzmemory_split2(self->dz, offset, &dzm0, &dzm1))
			exceptionHandler(THROW_MemoryIndexOutOfBounds);
		/* ============================== */
		dzmemory_decRefcount(dzm0);
	} else {
#endif
		/* this call may invoke the gc !! */
		if (dzmemory_split3(self->dz, offset, len, &dzm0, &dzm1, &dzm2))
			exceptionHandler(THROW_MemoryIndexOutOfBounds);
		/* ============================== */
		dzmemory_decRefcount(dzm0);
		dzmemory_decRefcount(dzm2);
#ifdef REDUCE_SPLITS
	}
#endif

	FREE_DZMEM("!getSubRange", (&self));

	new_mem = allocMemoryProxyInDomainDZM(curdom(), memoryClass, dzm1);
#ifdef DEBUG_MEMORY_CREATION
	(*new_mem)->dz->createdUsing = "getSubRange";
#endif

	RESTORE_IRQ;

	RETURN_UNREGHANDLE(new_mem);;
}

void memory_split2(MemoryProxy * self, jint offset, ArrayDesc * arr)
{
	DZMemoryProxy *dzm0, *dzm1;
	MemoryProxyHandle mem0, mem1;
	DomainDesc *domain;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	ASSERTOBJECT(arr);

	CHECKVALID(self);

	if (arr == NULL || arr->size < 2)
		exceptionHandler(THROW_ArrayIndexOutOfBounds);

	DISABLE_IRQ;

	domain = curdom();

	/* this call may invoke the gc !! */
	if (dzmemory_split2(self->dz, offset, &dzm0, &dzm1))
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	/* ============================== */

	mem0 = allocMemoryProxyInDomainDZM(domain, memoryClass, dzm0);
	mem1 = allocMemoryProxyInDomainDZM(domain, memoryClass, dzm1);

	arr->data[0] = *mem0;
	arr->data[1] = *mem1;

	UNREGHANDLE(mem0);
	UNREGHANDLE(mem1);

	FREE_DZMEM("!split2", (&self));

	RESTORE_IRQ;
}

void memory_split3(MemoryProxy * self, jint offset, jint len, ArrayDesc * arr)
{
	DZMemoryProxy *dzm0, *dzm1, *dzm2;
	MemoryProxyHandle mem0, mem1, mem2;
	DomainDesc *domain;

	ArrayDesc *tarr = arr;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);

	CHECKVALID(self);

	if (arr == NULL || arr->size < 3)
		exceptionHandler(THROW_ArrayIndexOutOfBounds);

	DISABLE_IRQ;

	ASSERT(self->size == self->dz->size);
	ASSERT(self->mem == self->dz->mem);

	domain = curdom();

	/* this call may invoke the gc !! */
	if (dzmemory_split3(self->dz, offset, len, &dzm0, &dzm1, &dzm2))
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	/* ============================== */

	mem0 = allocMemoryProxyInDomainDZM(domain, memoryClass, dzm0);
	mem1 = allocMemoryProxyInDomainDZM(domain, memoryClass, dzm1);
	mem2 = allocMemoryProxyInDomainDZM(domain, memoryClass, dzm2);

	arr->data[0] = *mem0;
	arr->data[1] = *mem1;
	arr->data[2] = *mem2;

	UNREGHANDLE(mem0);
	UNREGHANDLE(mem1);
	UNREGHANDLE(mem2);

	FREE_DZMEM("!split3", (&self));

	RESTORE_IRQ;
}

MemoryProxy *memory_joinPrevious(MemoryProxy * self)
{
	MemoryProxyHandle new_mem;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);

	DISABLE_IRQ;
	{
		MemoryProxyHandle mem = REF2HANDLE(self);

		DZMemoryProxy *dzmPrev;
		DZMemoryProxy *dzmThis;
		DZMemoryProxy *dzmNew;

		if ((dzmThis = (*mem)->dz) == NULL)
			sys_panic("join: NO DZ");

		/* join allowed? */
		if (dzmThis->prevOwner != curdom()->id) {
			if (dzmThis->prevOwner == NULL) {
				printf("prevOwner==NULL\n");
			} else {
				printf("prevOwner: %d current: %d (%s)\n", dzmThis->prevOwner, curdom()->id,
				       curdom()->domainName);
			}
			sys_panic("join not allowed");
		}

		if ((dzmPrev = dzmThis->prev) == NULL)
			sys_panic("join: NO prev");

		dzmNew = dzmemory_join(dzmPrev, dzmThis);

		FREE_DZ("!joinPrev", dzmPrev);
		FREE_DZ("!joinPrev", dzmThis);

		new_mem = allocMemoryProxyInDomainDZM(curdom(), memoryClass, dzmNew);

#ifdef DEBUG_MEMORY_CREATION
		(*new_mem)->dz->createdUsing = "joinPrevious";
#endif

		CHECK_DZ((*new_mem)->dz);

#ifdef REDIRECT_INVALID_DZ
		dzmemory_redirect_invalid_dz(mem);
#endif
	}
	RESTORE_IRQ;

	RETURN_UNREGHANDLE(new_mem);
}

/* returns NULL if join not possible. In this case the memory is not invalidated */
MemoryProxy *memory_joinNext(ObjectDesc * self)
{
	MemoryProxyHandle new_mem;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);

	DISABLE_IRQ;
	{
		MemoryProxyHandle mem = REF2HANDLE(self);
		DZMemoryProxy *dzmNext;
		DZMemoryProxy *dzmThis;
		DZMemoryProxy *dzmNew;

		if ((dzmThis = (*mem)->dz) == NULL)
			sys_panic("join: NO DZ");

		/* join allowed? */
		if (dzmThis->nextOwner != curdom()->id)
			return NULL;	/*sys_panic("join not allowed"); */

		if ((dzmNext = (*mem)->dz->next) == NULL)
			sys_panic("join: NO next");

		dzmNew = dzmemory_join(dzmThis, dzmNext);

		FREE_DZ("!joinPrev", dzmNext);
		FREE_DZ("!joinPrev", dzmThis);

		new_mem = allocMemoryProxyInDomainDZM(curdom(), memoryClass, dzmNew);

#ifdef DEBUG_MEMORY_CREATION
		(*new_mem)->dz->createdUsing = "joinNext";
#endif

		CHECK_DZ((*new_mem)->dz);

#ifdef REDIRECT_INVALID_DZ
		dzmemory_redirect_invalid_dz(mem);
#endif
	}
	RESTORE_IRQ;
	RETURN_UNREGHANDLE(new_mem);
}

/*
 *
 */
MemoryProxy *memory_joinAll(ObjectDesc * self)
{
	MemoryProxyHandle newMem;
	MemoryProxyHandle oldMem;
	DZMemoryProxy *dzmTop;
	DZMemoryProxy *dzmNew;
	DZMemoryProxy *dzmLast;
	DZMemoryProxy *dzmRef;
	DomainDesc *cdom;
	u4_t size;
	u4_t *mem;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);

	cdom = curdom();

	DISABLE_IRQ;
	{
		oldMem = REF2HANDLE(self);

		/* find top in dzm-chain */
		if ((dzmRef = (*oldMem)->dz) == NULL)
			sys_panic("joinAll: no dz");
		for (dzmTop = dzmRef; dzmTop->prev != NULL && dzmTop->prevOwner == cdom->id; dzmTop = dzmTop->prev) {
			ASSERT(dzmTop->prev->valid == 1);
			ASSERT(dzmTop->prev->mem == (dzmTop->mem - dzmTop->prev->size));
		}

		/* find last in dzm-chain and compute memory size */
		size = 0;
		mem = dzmTop->mem;
		dzmLast = dzmTop;
		do {
			ASSERT(dzmLast->valid == 1);
			if (dzmLast->next) {
				ASSERT(dzmLast->next->prev == dzmLast);
				ASSERT((dzmLast->mem + dzmLast->size) == dzmLast->next->mem);
			}
			size += dzmLast->size;
			FREE_DZ("!joinAll", dzmLast);
		} while (dzmLast->nextOwner == cdom->id && (dzmLast = dzmLast->next) != NULL);

		/* create new dz instance */
		dzmNew = createDZMemory(size, mem);
		ASSERT(dzmNew->mem);

		/* chain new dz */
		if (dzmTop->prev)
			dzmTop->prev->next = dzmNew;
		dzmNew->prev = dzmTop->prev;
		dzmNew->prevOwner = dzmTop->prevOwner;

		dzmTop->prev = NULL;	/* break old prev link */

		if (dzmLast) {
			dzmNew->next = dzmLast->next;
			dzmNew->nextOwner = dzmLast->nextOwner;
			if (dzmLast->next)
				dzmLast->next->prev = dzmNew;
			dzmLast->next = NULL;	/*break old next link */
		} else {
			dzmNew->next = NULL;
			dzmNew->nextOwner = cdom->id;
		}

#ifdef REDIRECT_INVALID_DZ
		dzmemory_redirect_invalid_dz(oldMem);
#endif

		CHECK_DZ(dzmNew);

		newMem = allocMemoryProxyInDomainDZM(cdom, memoryClass, dzmNew);

#ifdef DEBUG_MEMORY_CREATION
		(*newMem)->dz->createdUsing = "joinAll";
#endif
	}
	RESTORE_IRQ;
	RETURN_UNREGHANDLE(newMem);
}

/* FIXME: don't copy */
ObjectDesc *memory_getReadOnlySubRange(ObjectDesc * self, jint offset, jint len)
{
	sys_panic("NO LONGER SUPPORTED");
}

/* direct portal -> refs on Java frame */
ObjectDesc *memory_extendRange(ObjectDesc * self, jint atbegin, jint atend)
{
	sys_panic("NO LONGER SUPPORTED");
}

// two requirements: valid check must be atomic and handle return must be atomic
MemoryProxy *memory_extendFullRange(ObjectDesc * self)
{
	sys_panic("NO LONGER SUPPORTED");
}

jint *memory_map(MemoryProxy * self, ObjectDesc * vmclass)
{
#ifdef ENABLE_MAPPING
	MappedMemoryProxy *map = NULL;
	Class *cl;
	DISABLE_IRQ;
	cl = obj2class(vmclass);
/* check MAPPED instance size
	if (cl->instanceSize) {
		//throw();
	}
*/
	map = allocMappedMemoryProxyInDomain(curdom(), self->mem, cl->classDesc);
/*	printf("MEM %p\n", self->mem);*/
	RESTORE_IRQ;
	return map;
#else
	sys_panic("map not supported");
#endif
}

jint memory_getLittleEndian32(ObjectDesc * self, jint offset)
{
	jbyte *mem;
	jint size;

	CP_ENFORCE_FMA;

	CHECK_NULL_PTR(self);
	CHECKVALID(self);
	size = self->data[0];
	if (offset + 4 > size)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	mem = (jbyte *) self->data[1];
	return *(jint *) (mem + offset);
}

void memory_setLittleEndian32(ObjectDesc * self, jint offset, jint value)
{
	jbyte *mem;
	jint size;

	CP_ENFORCE_FMA;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);
	size = self->data[0];
	if (offset + 4 > size)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	mem = (jbyte *) self->data[1];
	*(jint *) (mem + offset) = value;
}

jshort memory_getLittleEndian16(ObjectDesc * self, jint offset)
{
	jbyte *mem;
	jint size;
#if 0
	CP_ENFORCE_FMA;
#endif
	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);
	size = self->data[0];
	if (offset + 2 > size)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	mem = (jbyte *) self->data[1];
	return *(jshort *) (mem + offset);
}

void memory_setLittleEndian16(ObjectDesc * self, jint offset, jshort value)
{
	jbyte *mem;
	jint size;
#if 0
	CP_ENFORCE_FMA;
#endif
	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);
	size = self->data[0];
	if (offset + 2 > size)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	mem = (jbyte *) self->data[1];
	*(jshort *) (mem + offset) = value;
}

jint memory_getBigEndian32(ObjectDesc * self, jint offset)
{
	jbyte *mem;
	jint size;
	jint data;

	CP_ENFORCE_FMA;

	CHECK_NULL_PTR(self);
	CHECKVALID(self);
	size = self->data[0];
	if (offset + 4 > size)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	mem = (jbyte *) self->data[1];

	data = *(jint *) (mem + offset);
	asm volatile ("bswap %%eax":"=r" (data));

	return data;
}

void memory_setBigEndian32(ObjectDesc * self, jint offset, jint value)
{
	jbyte *mem;
	jint size;

	CP_ENFORCE_FMA;

	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);

	size = self->data[0];
	if (offset + 4 > size)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	mem = (jbyte *) self->data[1];
	asm volatile ("bswap %%eax":"=r" (value));
	*(jint *) (mem + offset) = value;
}

jshort memory_getBigEndian16(ObjectDesc * self, jint offset)
{
	jbyte *mem;
	jint size;
	jint value;
#if 0
	CP_ENFORCE_FMA;
#endif
	CHECK_NULL_PTR(self);
	CHECKVALID(self);
	size = self->data[0];
	if (offset + 2 > size)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	mem = (jbyte *) self->data[1];
	value = *(jshort *) (mem + offset);
	return ((value & 0x00ff) << 8) | ((value & 0xff00) >> 8);
}

void memory_setBigEndian16(ObjectDesc * self, jint offset, jshort value)
{
	jbyte *mem;
	jint size;
#if 0
	CP_ENFORCE_FMA;
#endif
	CHECK_NULL_PTR(self);
	ASSERTMEMORY(self);
	CHECKVALID(self);
	size = self->data[0];
	if (offset + 2 > size)
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	mem = (jbyte *) self->data[1];
	*(jshort *) (mem + offset) = ((value & 0x00ff) << 8) | ((value & 0xff00) >> 8);
}

MemoryProxy *memory_revoke(MemoryProxy * self)
{
	MemoryProxyHandle memoryInstance;
	MemoryProxyHandle selfHandle = REF2HANDLE(self);
	DomainDesc *domain = curdom();
	DZMemoryProxy *dzmOldThis;
	DZMemoryProxy *dzmNewThis;
	DZMemoryProxy *dzmOld;
	DZMemoryProxy *dzmNew;
	DZMemoryProxy *dzmCurNew, *dzmCurOld, *dzmLastNew;
	DZMemoryProxy *dz;
	DomainDesc *cdom = curdom();

	DISABLE_IRQ;

	CHECKVALID(self);

	ASSERTMEMORY(self);

	dzmOldThis = self->dz;
	ASSERT(dzmOldThis);

#ifdef DBG_REVOKE
	printf("***REVOKE\n");
	cpuManager_dump(NULL, NULL, self);
#endif				/* DBG_REVOKE */

	dzmNewThis = createDZMemory(memoryGetSize(selfHandle), memoryGetMem(selfHandle));

#ifdef DEBUG_MEMORY_CREATION
	dzmNewThis->createdAt = getCaller(1);
	dzmNewThis->createdBy = cdom;
	dzmNewThis->createdUsing = "revoke";
#endif				/*DEBUG_MEMORY_CREATION */

	dzmCurOld = dzmOldThis;
	dzmLastNew = dzmNewThis;
	while ((dzmCurOld->prevOwner == cdom->id)
	       && (dzmCurOld = dzmCurOld->prev)) {
		dzmCurNew = createDZMemory(dzmCurOld->size, dzmCurOld->mem);
		//allocMemoryProxyInDomain(cdom, memoryClass, dzmCurNew);
		dzmCurNew->next = dzmLastNew;
		dzmCurNew->nextOwner = cdom->id;
		dzmLastNew->prev = dzmCurNew;
		dzmLastNew->prevOwner = cdom->id;
		dzmLastNew = dzmCurNew;
		dzmCurOld->valid = 0;
		dzmCurOld->mem = NULL;
#ifdef DEBUG_MEMORY_CREATION
		dzmCurOld->createdUsing = "!revoke(p)";
		dzmCurNew->createdAt = getCaller(1);
		dzmCurNew->createdBy = cdom;
		dzmCurNew->createdUsing = "revoke(p)";
#endif				/*DEBUG_MEMORY_CREATION */
	}
	if (dzmCurOld != NULL && dzmCurOld->prev) {
		/* there is a prev link but we do not own it */
		dzmCurOld->prev->next = dzmCurNew;
		dzmCurNew->prev = dzmCurOld->prev;
		dzmCurNew->prevOwner = dzmCurOld->prevOwner;
	}

	dzmCurOld = dzmOldThis;
	dzmLastNew = dzmNewThis;
	while ((dzmCurOld->nextOwner == cdom->id)
	       && (dzmCurOld = dzmCurOld->next)) {

		dzmCurNew = createDZMemory(dzmCurOld->size, dzmCurOld->mem);
		//allocMemoryProxyInDomain(cdom, memoryClass, dzmCurNew);

		ASSERT(dzmCurNew != dzmLastNew);
		dzmCurNew->prev = dzmLastNew;
		dzmCurNew->prevOwner = cdom->id;
		dzmLastNew->next = dzmCurNew;
		dzmLastNew->nextOwner = cdom->id;
		dzmLastNew = dzmCurNew;
		dzmCurOld->valid = 0;
		dzmCurOld->mem = NULL;
#ifdef DEBUG_MEMORY_CREATION
		dzmCurOld->createdUsing = "!revoke(n)";
		dzmCurNew->createdAt = getCaller(1);
		dzmCurNew->createdBy = cdom;
		dzmCurNew->createdUsing = "revoke(n)";
#endif				/*DEBUG_MEMORY_CREATION */
	}
	if (dzmCurOld != NULL && dzmCurOld->next) {
		/* there is a next link but we do not own it */
		dzmCurOld->next->prev = dzmCurNew;
		dzmCurNew->next = dzmCurOld->next;
		dzmCurNew->nextOwner = dzmCurOld->nextOwner;
	}

	memoryInstance = allocMemoryProxyInDomainDZM(curdom(), memoryClass, dzmNewThis);

	FREE_DZMEM("!revoke", selfHandle);

	ASSERT(!ISVALID(selfHandle));

	ASSERTMEMORY(*memoryInstance);

	RESTORE_IRQ;

	RETURN_UNREGHANDLE(memoryInstance);
}

jint memory_getOffset(MemoryProxy * self)
{
	sys_panic("NOT SUPPORTED");
}

jboolean memory_isValid(MemoryProxy * self)
{
	MemoryProxyHandle selfHandle = REF2HANDLE(self);
	return ISVALID(selfHandle);
}

/*******************************************
 *            MISC
 *******************************************/

ClassDesc *mem_getDeviceMemoryClass()
{
	return deviceMemoryClass;
}

u4_t memory_sizeof_proxy()
{
	return sizeof(MemoryProxy);
}

/*******************************************
 *            INTRA-CORE INTERFACE 
 *******************************************/
u4_t memory_getSize(MemoryProxy * obj)
{
	return obj->size;
}

u4_t memory_getMem(MemoryProxy * obj)
{
	return obj->mem;
}

/*******************************************
 *            COPY
 *******************************************/

#ifndef NEW_COPY
ObjectDesc *copy_memory(DomainDesc * src, DomainDesc * dst, MemoryProxy * obj, u4_t * quota)
{
	MemoryProxyHandle handle;
	MemoryProxy *o;
	ClassDesc *c = obj2ClassDesc(obj);

	if (obj == NULL)
		return NULL;
	if (src == dst)
		return obj;

	{
		u4_t objsize = OBJSIZE_MEMORY;
		if (*quota < objsize)
			sys_panic("");
		*quota -= objsize;
	}
	ASSERTMEMORY(obj);

	handle = allocMemoryProxyInDomainDZM(dst, c, obj->dz);
	o = unregisterObject(dst, handle);
	addToRefTable(obj, o);
	o->vtable = obj->vtable;
	o->dz = obj->dz;
	ASSERT(o->dz != NULL);
	o->dz->refcount++;
	//  ASSERT(o->mem != NULL);  mem is NULL if memory is invalidated
	//  printf("  copied memory = %p %d -> %d\n", o->dz, src->id, dst->id);

#ifdef DBG_DEP
	pprintf2("  copied memory = %p \n", o);
#endif
	return (ObjectDesc *) o;
}
#else
ObjectDesc *copy_shallow_memory(DomainDesc * src, DomainDesc * dst, MemoryProxy * obj, u4_t * quota)
{
	MemoryProxyHandle handle;
	MemoryProxy *o;
	ClassDesc *c = obj2ClassDesc(obj);

	if (obj == NULL)
		return NULL;
	if (src == dst)
		return obj;

	{
		u4_t objsize = OBJSIZE_MEMORY;
		if (*quota < objsize)
			sys_panic("");
		*quota -= objsize;
	}
	ASSERTMEMORY(obj);

	handle = allocMemoryProxyInDomainDZM(dst, c, obj->dz);
	o = unregisterObject(dst, handle);
	addToRefTable(obj, o);
	o->vtable = obj->vtable;
	o->dz = obj->dz;
	ASSERT(o->dz != NULL);
	o->dz->refcount++;
	//  ASSERT(o->mem != NULL);  mem is NULL if memory is invalidated
	//  printf("  copied memory = %p %d -> %d\n", o->dz, src->id, dst->id);

#ifdef DBG_DEP
	pprintf2("  copied memory = %p \n", o);
#endif
	return (ObjectDesc *) o;
}

void copy_content_memory(DomainDesc * src, DomainDesc * dst, MemoryProxy * obj, u4_t * quota)
{
}
#endif

/*******************************************
 *            SHALLOW COPY
 *******************************************/

void memory_copy_intradomain(struct MemoryProxy_s *dstObj, struct MemoryProxy_s *srcObj)
{
	dstObj->vtable = srcObj->vtable;
	dstObj->size = srcObj->size;
#ifdef REDIRECT_INVALID_DZ
	if (srcObj->dz->valid == 0)
		dzmemory_redirect_invalid_dz(&srcObj);
#endif
	dstObj->dz = srcObj->dz;
	ASSERT(dstObj->dz != NULL);
	ASSERT(dstObj->dz->refcount > 0);
	dstObj->mem = srcObj->mem;

	if (dstObj->mem == NULL && dstObj->dz->valid)
		printf("warn: valid obj has null pointer!\n");
#ifdef DEBUG_MEMORY_CREATION
	if (dstObj->dz->valid) {
		if (dstObj->mem == NULL) {
			printf("bad memory object! created at:\n");
			print_eip_info((char *) dstObj->dz->createdAt);
			if (dstObj->dz->createdUsing)
				printf(" %s\n", dstObj->dz->createdUsing);
			domain_panic(curdom(), "valid && mem==NULL && refcount>0");
		}
	}
#endif
}

/*******************************************
 *            ALLOC
 *******************************************/

/** THIS FUNCTION MUST NOT BE INTERRUPTED BY A GC */
MemoryProxyHandle allocMemoryProxyInDomainDZM(DomainDesc * domain, ClassDesc * c, DZMemoryProxy * dzm)
{
	return internalallocMemoryProxyInDomain(domain, c, dzm);
}

MemoryProxyHandle allocMemoryProxyInDomain(DomainDesc * domain, ClassDesc * c, jint start, jint size)
{
	DZMemoryProxy *dzm;
#ifdef PROFILE_EVENT_MEMORY_ALLOC
	RECORD_EVENT(event_allocProxyIN);
#endif
	dzm = createDZMemory(size, start);
#ifdef PROFILE_EVENT_MEMORY_ALLOC
	RECORD_EVENT(event_createMemoryDZ);
#endif
	return internalallocMemoryProxyInDomain(domain, c, dzm);
}

MemoryProxyHandle internalallocMemoryProxyInDomain(DomainDesc * domain, ClassDesc * c, DZMemoryProxy * dzm)
{
	MemoryProxyHandle obj;
	jint objSize = OBJSIZE_MEMORY;


	obj = (MemoryProxyHandle) gc_allocDataInDomain(domain, objSize, OBJFLAGS_MEMORY);
	if (obj == NULL || *obj == NULL) {
		exceptionHandler((void *) -1);
		sys_panic("should not be reached");
	}
	(*obj)->vtable = c->vtable;
	(*obj)->size = dzm->size;
	(*obj)->mem = dzm->mem;
	(*obj)->dz = dzm;

#ifdef PROFILE_AGING
	paMemProxy((jint *) obj, c);
#endif

#ifdef DEBUG_MEMORY_CREATION
	(*memoryInstance)->dz->createdUsing = "alloc";
	(*memoryInstance)->dz->createdAt = getCaller(1);
	(*memoryInstance)->dz->createdBy = curdom();
	(*memoryInstance)->dz->deviceMem = JNI_TRUE;
#endif				/*DEBUG_MEMORY_CREATION */

	return obj;
}




/*******************************************
 *            INIT
 *******************************************/

#define MEMORY_METHODS {\
    {"set8", "", memory_set8},\
    {"get8", "", memory_get8},\
    {"set16", "", memory_set16}, \
    {"get16", "", memory_get16},\
    {"set32", "", memory_set32},\
    {"get32", "", memory_get32},\
    {"fill16", "", memory_fill16},\
    {"fill32", "", memory_fill32},\
    {"clear", "", memory_clear},\
    {"getStartAddress", "", memory_getStartAddress},\
    {"size", "", memory_size},\
    {"copy", "", memory_copy},\
    {"move", "", memory_move}, \
    {"copyToByteArray", "", memory_copyToByteArray},\
    {"copyFromByteArray", "", memory_copyFromByteArray},\
    {"copyToMemory", "", memory_copyToMemory},\
    {"copyFromMemory", "", memory_copyFromMemory},\
    {"getLittleEndian32", "", memory_getLittleEndian32},\
    {"setLittleEndian32", "", memory_setLittleEndian32},\
    {"getLittleEndian16", "", memory_getLittleEndian16},\
    {"setLittleEndian16", "", memory_setLittleEndian16},\
    {"getBigEndian32", "", memory_getBigEndian32},\
    {"setBigEndian32", "", memory_setBigEndian32},\
    {"getBigEndian16", "", memory_getBigEndian16},\
    {"setBigEndian16", "", memory_setBigEndian16},\
    {"revoke", "", memory_revoke},\
    {"isValid", "", memory_isValid},\
    {"split2", "", memory_split2},\
    {"split3", "", memory_split3},\
    {"getSubRange", "", memory_getSubRange},\
    {"joinPrevious", "", memory_joinPrevious},\
    {"joinNext", "", memory_joinNext},\
    {"joinAll", "", memory_joinAll},\
    {"getReadOnlySubRange", "", memory_getReadOnlySubRange},\
    {"map", "", memory_map},\
}

/* DANGER: DO NOT CHANGE THE memoryMethods METHODS LIST WITHOUT ADJUSTING THIS DEFINE! */
#define SET32_INDEX 4
#define GET32_INDEX 5

MethodInfoDesc memoryMethods[] = MEMORY_METHODS;

static jbyte memoryTypeMap[] = { 8 };	/* data[3] */

void init_memory_portal()
{
	/*
	   #ifdef NOPREEMPT
	   memoryMethods[SET32_INDEX].code = memory_set32;
	   memoryMethods[GET32_INDEX].code = memory_get32;
	   #endif
	 */

#ifdef PROFILE_EVENT_MEMORY_ALLOC
	event_createMemoryIn = createNewEvent("createMemoryIn");
	event_createMemoryOut = createNewEvent("createMemoryOut");
	event_createMemoryMalloc = createNewEvent("createMemoryMalloc");
	event_createMemoryMemset = createNewEvent("createMemoryMemset");
	event_createMemoryDZ = createNewEvent("createMemoryDZ");
	event_allocProxyIN = createNewEvent("allocProxyIN");
#endif

	/* alloc mem for refcounts */

	dzmemory_init();

	memoryClass =
	    init_zero_class("jx/zero/Memory", memoryMethods, sizeof(memoryMethods), 4, memoryTypeMap, "<jx/zero/Memory>");
	deviceMemoryClass =
	    init_zero_class("jx/zero/DeviceMemory", memoryMethods, sizeof(memoryMethods), 4, memoryTypeMap,
			    "<jx/zero/DeviceMemory>");

#ifdef PROFILE_EVENT_MEMORY_ALLOC_AMOUNT
	event_memory_alloc = createNewEvent("MEMORY_ALLOC");
#endif				/* PROFILE_EVENT_MEMORY_ALLOC */

#if 0
	{
		ClassDesc *cd;
		Class *c = findClass(curdom(), "jx/zero/DeviceMemory");
		if (c != NULL) {
			cd = c->classDesc;
			printf("Class at %p ClassDesc at %p vtable at %p\n", c, cd, cd->vtable);
		}
		printf("DeviceMem %s ClassDesc at %p vtable at %p name=%p\n", deviceMemoryClass->name, deviceMemoryClass,
		       deviceMemoryClass->vtable);
	}
#endif
	readonlyMemoryClass =
	    init_zero_class("jx/zero/ReadOnlyMemory", memoryMethods, sizeof(memoryMethods), 4, memoryTypeMap,
			    "<jx/zero/ReadOnlyMemory>");

}
