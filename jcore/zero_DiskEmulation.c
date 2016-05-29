#include "config.h"
#ifdef DISK_EMULATION

#include "all.h"

#define CHECKVALID(x)

#define DISK_SECTORSIZE 512
#define DISK_NAME "EMULATED_DISK"

static int fd = -1;
static int capacity = 0;

int diskemulation_getCapacity(ObjectDesc * self)
{
	return capacity;
}

int diskemulation_getSectorSize(ObjectDesc * self)
{
	return DISK_SECTORSIZE;
}


void diskemulation_readSectors(ObjectDesc * self, int startSector, int numberOfSectors, struct MemoryProxy_s *buf,
			       jboolean synchronous)
{
	CHECK_NULL_PTR(buf);
	CHECKVALID(buf);
	if (numberOfSectors * DISK_SECTORSIZE > memory_size(buf))
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	if (lseek(fd, startSector * DISK_SECTORSIZE, SEEK_SET) == -1) {
		perror("diskemulation_readSectors");
		sys_panic("lseek");
	}
	if (read(fd, memory_getMem(buf), numberOfSectors * DISK_SECTORSIZE) == -1)
		sys_panic("read");
}

void diskemulation_writeSectors(ObjectDesc * self, int startSector, int numberOfSectors, struct MemoryProxy_s *buf,
				jboolean synchronous)
{
	CHECK_NULL_PTR(buf);
	CHECKVALID(buf);
	if (numberOfSectors * DISK_SECTORSIZE > memory_size(buf))
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	if (lseek(fd, startSector * DISK_SECTORSIZE, SEEK_SET) == -1) {
		perror("diskemulation_writeSectors");
		sys_panic("lseek");
	}
	if (write(fd, memory_getMem(buf), numberOfSectors * DISK_SECTORSIZE) == -1) {
		perror("diskemulation_writeSectors");
		sys_panic("write");
	}
}


MethodInfoDesc diskemulationMethods[] = {
	{"getCapacity", "", diskemulation_getCapacity}
	,
	{"getSectorSize", "", diskemulation_getSectorSize}
	,
	{"readSectors", "", diskemulation_readSectors}
	,
	{"writeSectors", "", diskemulation_writeSectors}
	,
};

void init_disk_emulation_portal()
{
	struct stat buf;
	fd = open(DISK_NAME, O_SYNC | O_RDWR);
	if (fd == -1)
		sys_panic("could not open emulated disk");
	if (fstat(fd, &buf) == -1)
		sys_panic("fstat");
	capacity = buf.st_size / DISK_SECTORSIZE;
	init_zero_dep("jx/zero/DiskEmulation", "DiskEmulation", diskemulationMethods, sizeof(diskemulationMethods),
		      "<jx/zero/DiskEmulation>");
}

#endif				/* DISK_EMULATION */
