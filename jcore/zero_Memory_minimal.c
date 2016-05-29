#include "all.h"

static ClassDesc *memoryClass = NULL;
static ClassDesc *deviceMemoryClass = NULL;
static ClassDesc *readonlyMemoryClass = NULL;

struct MemoryProxy_s *gc_impl_shallowCopyMemory(u4_t * dst, struct MemoryProxy_s
						*srcObj)
{
	sys_panic("shallow");
}

MemoryProxyHandle allocMemoryProxyInDomain(DomainDesc * domain, ClassDesc * c, jint start, jint size)
{
	sys_panic("alloc");
}

jint memory_size(ObjectDesc * self)
{
	sys_panic("size");
}

jint memory_getStartAddress(ObjectDesc * self)
{
	sys_panic("startaddr");
}

ObjectDesc *copy_memory(DomainDesc * src, DomainDesc * dst, struct MemoryProxy_s *obj, u4_t * quota)
{
	sys_panic("copy");
}

u4_t memory_sizeof_proxy()
{
	sys_panic("sizeof");
}

void memory_deleted(struct MemoryProxy_s *obj)
{
	sys_panic("incref");
}

void dzmemory_incRefcount(struct MemoryProxy_s *obj)
{
	sys_panic("decref");
}

MemoryProxyHandle createMemoryInstance(DomainDesc * domain, jint size, jint bytes)
{
	sys_panic("createinstance");
}

MemoryProxyHandle allocReadOnlyMemory(ObjectDesc * self, jint start, jint size)
{
	sys_panic("createRO");
}

ClassDesc *mem_getDeviceMemoryClass()
{
	return deviceMemoryClass;
}

void print_memobj(jint domainID)
{
}

/*******************************************
 *            INIT
 *******************************************/

static jbyte memoryTypeMap[] = { 8 };	/* data[3] */

void init_memory_portal()
{
	memoryClass = init_zero_class("jx/zero/Memory", NULL, 0, 4, memoryTypeMap, "<jx/zero/Memory>");
	deviceMemoryClass = init_zero_class("jx/zero/DeviceMemory", NULL, 0, 4, memoryTypeMap, "<jx/zero/DeviceMemory>");
	readonlyMemoryClass = init_zero_class("jx/zero/ReadOnlyMemory", NULL, 0, 4, memoryTypeMap, "<jx/zero/ReadOnlyMemory>");

}
