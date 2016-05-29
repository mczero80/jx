/********************************************************************************
 * DomainZero Memory objects
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

/**************************************************
 *   This implementation does not allow sharing   *
 *   of memory objects between domains            *
 **************************************************/
#include "all.h"
#include "gc_impl.h"

#define ALLOW_SHARING 1

static ClassDesc *memoryClass = NULL;
static ClassDesc *deviceMemoryClass = NULL;
static ClassDesc *readonlyMemoryClass = NULL;

typedef struct MemoryProxy_s {
	code_t *vtable;
	u4_t size;
	char *mem;
} MemoryProxy;

#define CHECK_BEFORE
#define CHECK_AFTER
#define CHECK_BEFORE_SIZE1 if (where >= self->size || where < 0) {exceptionHandler(THROW_MemoryIndexOutOfBounds); }
#define CHECK_BEFORE_SIZE2 if (where >= self->size >> 1 || where < 0) {exceptionHandler(THROW_MemoryIndexOutOfBounds); }
#define CHECK_BEFORE_SIZE3 if (where >= self->size >> 2 || where < 0) {exceptionHandler(THROW_MemoryIndexOutOfBounds); }

#define CHECK_BEFORE_RANGE2 if (where + length > self->size >> 1 || where < 0 || length < 0) {exceptionHandler(THROW_MemoryIndexOutOfBounds); }
#define CHECK_BEFORE_RANGE3 if (where + length > self->size >> 2 || where < 0 || length < 0) {exceptionHandler(THROW_MemoryIndexOutOfBounds); }
#define CHECK_BEFORE_EXTERN(stmt) if (stmt) {exceptionHandler(THROW_MemoryIndexOutOfBounds); }

#include "zero_Memory_simple.c"


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
	if (obj == NULL)
		return NULL;
	if (src == dst)
		return obj;

	if (src == domainZero) {
		MemoryProxyHandle handle = allocMemoryProxyInDomain(dst, obj2ClassDesc(obj),
								    obj->mem, obj->size);
		ObjectDesc *o = unregisterObject(dst, handle);
		//printf("MEMADDR2: %p\n", o);
		addToRefTable(obj, o);
		return (ObjectDesc *) o;
	}
#ifdef ALLOW_SHARING
	{
		MemoryProxyHandle handle = allocMemoryProxyInDomain(dst, obj2ClassDesc(obj),
								    obj->mem, obj->size);
		ObjectDesc *o = unregisterObject(dst, handle);
		//printf("MEMADDR2: %p\n", o);
		addToRefTable(obj, o);
		return (ObjectDesc *) o;
	}
#else
	exceptionHandlerMsg(THROW_RuntimeException,
			    "Memory objects cannot be copied between domains in the current configuration");
#endif
}

/*******************************************
 *            REFCOUNTS
 *******************************************/

void memory_deleted(struct MemoryProxy_s *obj)
{
/* do nothing. we are called by the garbage collector. we should not try to create an exception object */
/*	exceptionHandlerMsg(THROW_RuntimeException,
	"Memory objects cannot be copied between domains in the current configuration");*/
}

void dzmemory_incRefcount(struct MemoryProxy_s *obj)
{
	exceptionHandlerMsg(THROW_RuntimeException,
			    "Memory objects cannot be copied between domains in the current configuration");
}

/*******************************************
 *            ALLOC
 *******************************************/

MemoryProxyHandle allocMemoryProxyInDomain(DomainDesc * domain, ClassDesc * c, jint start, jint size)
{
	MemoryProxyHandle obj;
	jint objSize = OBJSIZE_MEMORY;

	obj = (MemoryProxyHandle) gc_allocDataInDomain(domain, objSize, OBJFLAGS_MEMORY);
	if (obj == NULL || *obj == NULL) {
		exceptionHandler(THROW_RuntimeException);
	}
	//printf("MEMADDR: %p\n", obj);
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

	memset(d, 0, size);

	m = allocMemoryProxyInDomain(domain, memoryClass, d, size);

	return m;
}

/*******************************************
 *            MISC (Intra-Core)
 *******************************************/

void print_memobj(jint domainID)
{
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
	printf("warning: joinAll not supported\n");
//      exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
	return self;
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
	printf("warning: revoke not supported");
//      exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
	return self;
}

jint memory_getOffset(MemoryProxy * self)
{
	exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}

jboolean memory_isValid(MemoryProxy * self)
{
	//printf("warning: isValid not supported");
	return JNI_TRUE;
	//exceptionHandlerMsg(THROW_RuntimeException, "NOT SUPPORTED");
}







/*******************************************
 *            INIT
 *******************************************/

MethodInfoDesc memoryMethods[] = {
	{"set8", "", (code_t) memory_set8}
	,
	{"get8", "", (code_t) memory_get8}
	,
	{"set16", "", memory_set16}
	,
	{"get16", "", memory_get16}
	,
	{"set32", "", memory_set32}
	,
	{"get32", "", memory_get32}
	,
	{"fill16", "", memory_fill16}
	,
	{"fill32", "", memory_fill32}
	,
	{"clear", "", memory_clear}
	,
	{"getStartAddress", "", memory_getStartAddress}
	,
	{"size", "", memory_size}
	,
	{"copy", "", memory_copy}
	,
	{"move", "", memory_move}
	,
	{"copyToByteArray", "", memory_copyToByteArray}
	,
	{"copyFromByteArray", "", memory_copyFromByteArray}
	,
	{"copyToMemory", "", memory_copyToMemory}
	,
	{"copyFromMemory", "", memory_copyFromMemory}
	,
	{"getLittleEndian32", "", memory_getLittleEndian32}
	,
	{"setLittleEndian32", "", memory_setLittleEndian32}
	,
	{"getLittleEndian16", "", memory_getLittleEndian16}
	,
	{"setLittleEndian16", "", memory_setLittleEndian16}
	,
	{"getBigEndian32", "", memory_getBigEndian32}
	,
	{"setBigEndian32", "", memory_setBigEndian32}
	,
	{"getBigEndian16", "", memory_getBigEndian16}
	,
	{"setBigEndian16", "", memory_setBigEndian16}
	,
	{"revoke", "", memory_revoke}
	,
	{"isValid", "", memory_isValid}
	,
	{"split2", "", memory_split2}
	,
	{"split3", "", memory_split3}
	,
	{"getSubRange", "", memory_getSubRange}
	,
	{"joinPrevious", "", memory_joinPrevious}
	,
	{"joinNext", "", memory_joinNext}
	,
	{"joinAll", "", memory_joinAll}
	,
	{"getReadOnlySubRange", "", memory_getReadOnlySubRange}
	,
	{"map", "", memory_map}
};


static jbyte memoryTypeMap[] = { 8 };	/* data[3] */

void init_memory_portal()
{
	memoryClass =
	    init_zero_class("jx/zero/Memory", memoryMethods, sizeof(memoryMethods), 4, memoryTypeMap, "<jx/zero/Memory>");
	deviceMemoryClass =
	    init_zero_class("jx/zero/DeviceMemory", memoryMethods, sizeof(memoryMethods), 4, memoryTypeMap,
			    "<jx/zero/DeviceMemory>");
	readonlyMemoryClass =
	    init_zero_class("jx/zero/ReadOnlyMemory", memoryMethods, sizeof(memoryMethods), 4, memoryTypeMap,
			    "<jx/zero/ReadOnlyMemory>");

}
