/**************************************************
 *   This implementation allows sharing of memory *
 *   objects between domains                      *
 **************************************************/
#include "all.h"

static ClassDesc *memoryClass = NULL;
static ClassDesc *deviceMemoryClass = NULL;
static ClassDesc *readonlyMemoryClass = NULL;


#ifdef PROFILE_EVENT_MEMORY_ALLOC
static u4_t event_createMemoryIn, event_createMemoryOut, event_createMemoryMalloc, event_createMemoryMemset;
#endif

#define DZMEM_FLAGS_VALID      0x00000001
#define DZMEM_FLAGS_DEVICEMEM  0x00000002

typedef struct DZMem_s {
	u4_t refcount;
	u4_t flags;
	char *mem;
	u4_t size;
} DZMem;

typedef struct MemoryProxy_s {
	code_t *vtable;
	u4_t size;
	char *mem;
	DZMem *dz;
} MemoryProxy;

#define CHECK_BEFORE { int error=0; DISABLE_IRQ; if (self->dz && ((self->dz->flags & DZMEM_FLAGS_VALID) == 0)) {error=1;} else {

#define CHECK_BEFORE_SIZE1  CHECK_BEFORE_EXTERN(where >= self->size || where < 0)
#define CHECK_BEFORE_SIZE2  CHECK_BEFORE_EXTERN(where >= self->size >> 1 || where < 0)
#define CHECK_BEFORE_SIZE3  CHECK_BEFORE_EXTERN(where >= self->size >> 2 || where < 0)

#define CHECK_BEFORE_RANGE2 CHECK_BEFORE_EXTERN(where + length > self->size >> 1 || where < 0 || length < 0)
#define CHECK_BEFORE_RANGE3 CHECK_BEFORE_EXTERN(where + length > self->size >> 2 || where < 0 || length < 0)
#define CHECK_BEFORE_EXTERN(stmt) { int error=0; DISABLE_IRQ; if (self->dz && ((self->dz->flags & DZMEM_FLAGS_VALID) == 0)) {error=1;} else if (stmt) { error=2; } else {


#define CHECK_AFTER  } RESTORE_IRQ; if (error==1) exceptionHandlerMsg(THROW_RuntimeException,"Invalid memory"); if (error==2) exceptionHandler(THROW_MemoryIndexOutOfBounds); }

#include "zero_Memory_simple.c"

/*******************************************
 *            DZ management
 *******************************************/

static DZMem *dzmemory_alloc()
{
	sys_panic("dzalloc");
}

/*******************************************
 *            COPY BETWEEN HEAPS OF SAME DOMAIN
 *******************************************/

void memory_copy_intradomain(struct MemoryProxy_s *dstObj, struct MemoryProxy_s *srcObj)
{
	dstObj->vtable = srcObj->vtable;
	dstObj->size = srcObj->size;
	dstObj->mem = srcObj->mem;
}


/*******************************************
 *            COPY BETWEEN DOMAINS
 *******************************************/

ObjectDesc *copy_memory(DomainDesc * src, DomainDesc * dst, struct MemoryProxy_s *obj, u4_t * quota)
{
	u4_t objsize = OBJSIZE_MEMORY;
	MemoryProxyHandle handle;
	MemoryProxy *o;

	if (obj == NULL)
		return NULL;
	if (src == dst)
		return obj;

	printf("Copy memory %d -> %d\n", src->id, dst->id);

	if (*quota < objsize)
		exceptionHandlerMsg(THROW_RuntimeException, "Too much data copied during portal call");
	*quota -= objsize;

	if (!obj->dz) {		/* not yet a full memory object */
		obj->dz = createDZMemory(obj->size, obj->mem);
	}

	handle = allocMemoryProxyInDomain(dst, obj2ClassDesc(obj), obj->mem, obj->size);

	o = unregisterObject(dst, handle);
	o->dz = obj->dz;
	dzmemory_incRefcount(obj);

	addToRefTable(obj, o);

	return (ObjectDesc *) o;
}

/*******************************************
 *            REFCOUNTS
 *******************************************/

void memory_deleted(struct MemoryProxy_s *obj)
{
	DZMem *m = obj->dz;
	u4_t refcount;
	if (!m)
		return;		/* not yet a full memory object */
	do {
		refcount = m->refcount;
	} while (!cas(&m->refcount, refcount, refcount - 1));
	if (m->refcount == 0)
		sys_panic("FREE DZMEM");	//dzmemory_free_chunk(m);
}

void dzmemory_incRefcount(struct MemoryProxy_s *obj)
{
	DZMem *m = obj->dz;
	u4_t refcount;
	do {
		refcount = m->refcount;
	} while (!cas(&m->refcount, refcount, refcount + 1));

}

/*******************************************
 *            ALLOC
 *******************************************/

static DZMem *createDZMemory(u4_t size, char *mem)
{
	DZMem *dzm = dzmemory_alloc();
	dzm->size = size;
	dzm->mem = mem;
	dzm->flags |= DZMEM_FLAGS_VALID;
	dzm->flags &= ~DZMEM_FLAGS_DEVICEMEM;
	return dzm;
}

MemoryProxyHandle allocMemoryProxyInDomain(DomainDesc * domain, ClassDesc * c, jint start, jint size)
{
	MemoryProxyHandle obj;
	jint objSize = OBJSIZE_MEMORY;

	obj = (MemoryProxyHandle) gc_allocDataInDomain(domain, objSize, OBJFLAGS_MEMORY);
	if (obj == NULL || *obj == NULL) {
		exceptionHandler((void *) -1);
		sys_panic("should not be reached");
	}
	(*obj)->vtable = c->vtable;
	(*obj)->size = size;
	(*obj)->mem = start;

#ifdef PROFILE_AGING
	paMemProxy((jint *) obj, c);
#endif

	return obj;
}

MemoryProxyHandle allocReadOnlyMemory(MemoryProxy * self, jint start, jint size)
{
	return allocMemoryProxyInDomain(curdom(), readonlyMemoryClass, start, size);
}

MemoryProxyHandle createMemoryInstance(DomainDesc * domain, jint size, jint bytes)
{
	MemoryProxyHandle m;
	jint rem;
	jint *d;

#ifdef PROFILE_EVENT_MEMORY_ALLOC
	RECORD_EVENT(event_createMemoryIn);
#endif

	if (size < 0 || bytes <= 0) {
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	}


	d = jxmalloc(size + bytes - 1 MEMTYPE_MEMOBJ);
	domain->memoryObjectBytes += size + bytes - 1;

	if (bytes > 1) {
		rem = ((u4_t) d % bytes);
		if (rem > 0) {
			d = (jint *) (((u4_t) d) + bytes - rem);
		}
	}
#ifdef PROFILE_EVENT_MEMORY_ALLOC
	RECORD_EVENT(event_createMemoryMalloc);
#endif

	memset(d, 0, size);

#ifdef PROFILE_EVENT_MEMORY_ALLOC
	RECORD_EVENT(event_createMemoryMemset);
#endif

	m = allocMemoryProxyInDomain(domain, memoryClass, d, size);

#ifdef PROFILE_EVENT_MEMORY_ALLOC
	RECORD_EVENT(event_createMemoryOut);
#endif

	return m;
}

/*******************************************
 *            MISC (Intra-Core)
 *******************************************/

void print_memobj(jint domainID)
{
}



/*******************************************
 *            METHODS OF THE MEMORY PORTAL
 *******************************************/


ObjectDesc *memory_getSubRange(MemoryProxy * self, jint offset, jint len)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

void memory_split2(MemoryProxy * self, jint offset, ArrayDesc * arr)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

void memory_split3(MemoryProxy * self, jint offset, jint len, ArrayDesc * arr)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

MemoryProxy *memory_joinPrevious(MemoryProxy * self)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

MemoryProxy *memory_joinNext(MemoryProxy * self)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

MemoryProxy *memory_joinAll(MemoryProxy * self)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

ObjectDesc *memory_getReadOnlySubRange(MemoryProxy * self, jint offset, jint len)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

ObjectDesc *memory_extendRange(MemoryProxy * self, jint atbegin, jint atend)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

MemoryProxy *memory_extendFullRange(MemoryProxy * self)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

jint *memory_map(ThreadDesc * source)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}


MemoryProxy *memory_revoke(MemoryProxy * self)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

jint memory_getOffset(MemoryProxy * self)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

jboolean memory_isValid(MemoryProxy * self)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}






/*******************************************
 *            INIT
 *******************************************/

MethodInfoDesc memoryMethods[] = {
	{"set8", "", (code_t) memory_set8},
	{"get8", "", (code_t) memory_get8},
	{"set16", "", memory_set16},
	{"get16", "", memory_get16},
	{"set32", "", memory_set32},
	{"get32", "", memory_get32},
	{"fill16", "", memory_fill16},
	{"fill32", "", memory_fill32},
	{"clear", "", memory_clear},
	{"getStartAddress", "", memory_getStartAddress},
	{"size", "", memory_size},
	{"copy", "", memory_copy},
	{"move", "", memory_move},
	{"copyToByteArray", "", memory_copyToByteArray},
	{"copyFromByteArray", "", memory_copyFromByteArray},
	{"copyToMemory", "", memory_copyToMemory},
	{"copyFromMemory", "", memory_copyFromMemory},
	{"getLittleEndian32", "", memory_getLittleEndian32},
	{"setLittleEndian32", "", memory_setLittleEndian32},
	{"getLittleEndian16", "", memory_getLittleEndian16},
	{"setLittleEndian16", "", memory_setLittleEndian16},
	{"getBigEndian32", "", memory_getBigEndian32},
	{"setBigEndian32", "", memory_setBigEndian32},
	{"getBigEndian16", "", memory_getBigEndian16},
	{"setBigEndian16", "", memory_setBigEndian16},
	{"revoke", "", memory_revoke},
	{"isValid", "", memory_isValid},
	{"split2", "", memory_split2},
	{"split3", "", memory_split3},
	{"getSubRange", "", memory_getSubRange},
	{"joinPrevious", "", memory_joinPrevious},
	{"joinNext", "", memory_joinNext},
	{"joinAll", "", memory_joinAll},
	{"getReadOnlySubRange", "", memory_getReadOnlySubRange},
	{"map", "", memory_map}
};


static jbyte memoryTypeMap[] = { 8 };	/* data[3] */

void init_memory_portal()
{
#ifdef PROFILE_EVENT_MEMORY_ALLOC
	event_createMemoryIn = createNewEvent("createMemoryIn");
	event_createMemoryOut = createNewEvent("createMemoryOut");
	event_createMemoryMalloc = createNewEvent("createMemoryMalloc");
	event_createMemoryMemset = createNewEvent("createMemoryMemset");
#endif

	memoryClass =
	    init_zero_class("jx/zero/Memory", memoryMethods, sizeof(memoryMethods), 4, memoryTypeMap, "<jx/zero/Memory>");
	deviceMemoryClass =
	    init_zero_class("jx/zero/DeviceMemory", memoryMethods, sizeof(memoryMethods), 4, memoryTypeMap,
			    "<jx/zero/DeviceMemory>");
	readonlyMemoryClass =
	    init_zero_class("jx/zero/ReadOnlyMemory", memoryMethods, sizeof(memoryMethods), 4, memoryTypeMap,
			    "<jx/zero/ReadOnlyMemory>");

}
