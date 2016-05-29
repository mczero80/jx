#include "all.h"

#define INCLIENT 1

#ifdef INCLIENT
#define CALLERDOMAIN (curdom())
#else
#define CALLERDOMAIN (curthr()->mostRecentlyCalledBy->domain)
#endif

extern ClassDesc *readonlyMemoryClass;

/*
 * MemoryManager DEP
 */

/* create a memory DEP with the requested size */
ObjectDesc *memoryManager_alloc(ObjectDesc * self, jint size)
{
	MemoryProxyHandle handle;
	DomainDesc *domain = CALLERDOMAIN;
	ASSERT(domain != domainZero)
	    handle = createMemoryInstance(domain, size, 1);
#ifdef DEBUG_MEMORY_CREATION
	(*handle)->dz->createdUsing = "alloc";
#endif
	RETURN_UNREGHANDLE(handle);
}

ObjectDesc *memoryManager_allocAligned(ObjectDesc * self, jint size, jint bytes)
{
	MemoryProxyHandle handle;
	DomainDesc *domain = CALLERDOMAIN;
	ASSERT(domain != domainZero)
	    if (bytes <= 0)
		return NULL;
	handle = createMemoryInstance(domain, size, bytes);
#ifdef DEBUG_MEMORY_CREATION
	(*handle)->dz->createdUsing = "allocAligned";
#endif
	RETURN_UNREGHANDLE(handle);
}


extern ClassDesc *deviceMemoryClass;

ObjectDesc *memoryManager_allocDeviceMemory(ObjectDesc * self, jint start, jint size)
{
	MemoryProxyHandle memoryInstance;
	DomainDesc *domain = CALLERDOMAIN;
	//ASSERT(domain != domainZero)
	memoryInstance = (ObjectDesc *) allocMemoryProxyInDomain(domain, mem_getDeviceMemoryClass(), start, size);

	RETURN_UNREGHANDLE(memoryInstance);
}

jint memoryManager_getTotalMemory(ObjectDesc * self)
{
	return jxmalloc_getTotalMemory();
}

jint memoryManager_getTotalFreeMemory()
{
	return jxmalloc_getTotalFreeMemory();
}

jint memoryManager_getFreeHeapMemory()
{
	DomainDesc *sourceDomain = CALLERDOMAIN;
	printf("Source: %s\n", sourceDomain->domainName);
	return gc_freeWords(sourceDomain) * 4;
}



MethodInfoDesc memoryManagerMethods[] = {
	{"alloc", "", memoryManager_alloc}
	,
	{"allocAligned", "", memoryManager_allocAligned}
	,
	{"allocDeviceMemory", "", memoryManager_allocDeviceMemory}
	,
	{"getTotalMemory", "", memoryManager_getTotalMemory}
	,
	{"getTotalFreeMemory", "", memoryManager_getTotalFreeMemory}
	,
	{"getFreeHeapMemory", "", memoryManager_getFreeHeapMemory}
	,
};


void init_memorymanager_portal()
{
#ifdef INCLIENT
	init_zero_dep_without_thread("jx/zero/MemoryManager", "MemoryManager", memoryManagerMethods, sizeof(memoryManagerMethods),
				     "<jx/zero/MemoryManager>");
#else
	init_zero_dep("jx/zero/MemoryManager", "MemoryManager", memoryManagerMethods, sizeof(memoryManagerMethods),
		      "<jx/zero/MemoryManager>");
#endif
}
