#include "all.h"


#define CALLERDOMAIN (curthr()->mostRecentlyCalledBy->domain)

/*
 * BootFS DEP
 */

jint bootfs_lookup(ObjectDesc * self, ObjectDesc * filename)
{
	return memfs_lookup(memfs_str2chr(filename));
}

ObjectDesc *bootfs_getFile(ObjectDesc * self, ObjectDesc * filename)
{
	FileDesc *fd;
	char *data;
	/*ObjectDesc *mem; */
	MemoryProxyHandle mem;

	CHECK_NULL_PTR(filename);
	if ((fd = memfs_open(curdom(), memfs_str2chr(filename))) == NULL) {
		/*exceptionHandler(THROW_NullPointerException); */
		return NULL;
	}


	mem = memfs_mmap(fd, curdom(), MEMFS_RO);
	ASSERTHANDLE(mem);

	memfs_close(fd);

	//  printf("BM%p %p\n",mem, (*mem)->dz);
#ifdef DEBUG_MEMORY_CREATION
	(*mem)->dz->createdAt = getCaller(2);
	(*mem)->dz->createdBy = CALLERDOMAIN;
#endif

	/*return mem; */
	RETURN_FROMHANDLE(mem);
}

ObjectDesc *bootfs_getReadWriteFile(ObjectDesc * self, ObjectDesc * filename)
{
	FileDesc *fd;
	char *data;
	ObjectDesc *mem;

	CHECK_NULL_PTR(filename);
	if ((fd = memfs_open(curdom(), memfs_str2chr(filename))) == NULL) {
		exceptionHandler(THROW_NullPointerException);
	}

	mem = memfs_mmap(fd, curdom(), MEMFS_RW);

	memfs_close(fd);

	return mem;
}

MethodInfoDesc bootfsMethods[] = {
	{"lookup", "", bootfs_lookup}
	,
	{"getFile", "", bootfs_getFile}
	,
	{"getReadWriteFile", "", bootfs_getReadWriteFile}
	,
};


void init_bootfs_portal()
{
	init_zero_dep("jx/zero/BootFS", "BootFS", bootfsMethods, sizeof(bootfsMethods), "<jx/zero/BootFS>");
}
