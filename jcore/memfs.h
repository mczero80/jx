#ifndef MEMFS
#define MEMFS

#include "types.h"
#include "domain.h"

#ifdef USE_QMAGIC
#define MAGIC_FD 0x00932756
#endif
#if defined (NORMAL_MAGIC)  && defined (USE_QMAGIC)
#define ASSERTFD(_fd_) ASSERT(_fd_->magic==MAGIC_FD)
#else
#define ASSERTFD(_fd_)
#endif

#define MEMFS_RO  0
#define MEMFS_RW  1

typedef struct filedesc_s {
#ifdef USE_QMAGIC
	u4_t magic;
#endif
	u4_t pos;
	u1_t *data;
	u4_t size;
	char *filename;
	DomainDesc *domain;
	MemoryProxyHandle *mem;
} FileDesc;


void memfs_init();

/*
 * only use this fkt together with memfs !!
 */
const char *memfs_str2chr(ObjectDesc * string);

int memfs_lookup(const char *filename);
int memfs_link(const char *filename, u1_t * mem);
int memfs_unlink(const char *filename);
FileDesc *memfs_open(DomainDesc * domain, const char *filename);

MemoryProxyHandle memfs_mmap(FileDesc * desc, DomainDesc * domain,
			     int flag);
/* MemoryProxy *memfs_getReadOnlyMemory(FileDesc* desc); */

int memfs_readByte(FileDesc * desc, jbyte * value);
int memfs_readInt(FileDesc * desc, jint * value);
int memfs_readStringData(FileDesc * desc, u1_t * buf, jint length);
int memfs_readString(FileDesc * desc, u1_t * buf, jint nbuf);
char *memfs_getString(FileDesc * desc);
int memfs_readCode(FileDesc * desc, u1_t * buf, jint nbuf);

int memfs_testChecksum(FileDesc * desc);
u4_t memfs_getPos(FileDesc * desc);
u4_t memfs_getSize(FileDesc * desc);
char *memfs_getFileName(FileDesc * desc);
int memfs_seek(FileDesc * desc, u4_t pos);
jint memfs_eof(FileDesc * desc);

void memfs_close(FileDesc * desc);

#endif				/* MEMFS */
