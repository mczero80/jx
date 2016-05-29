#include "all.h"

void memfs_init()
{
	/* we do nothing yet */
}

#define MEM_MAX_STRING 128
static char char_buffer[MEM_MAX_STRING];
const char *memfs_str2chr(ObjectDesc * string)
{
	stringToChar(string, char_buffer, MEM_MAX_STRING);
	return (const char *) char_buffer;
}

#ifdef KERNEL
#define READFROMZIP
#endif
int memfs_lookup(const char *filename)
{
	unsigned long size;
#ifndef READFROMZIP
	int fd;
	struct stat statbuf;
	char path[128];
#else
	zipentry entry;
#endif

	if (libcache_lookup_jll(filename, &size) == NULL) {
#ifdef READFROMZIP
		zip_reset();
		for (;;) {
			if (zip_next_entry(&entry) == -1)
				return JNI_FALSE;
			if (strcmp(entry.filename, filename) == 0)
				break;
		}
#else
		fd = open(filename, O_RDONLY);
		if (fd == -1) {
			strcpy(path, "../libs/");
			strcat(path, filename);
			fd = open(path, O_RDONLY);
			if (fd == -1) {
				strcpy(path, "../domains/");
				strcat(path, filename);
				fd = open(path, O_RDONLY);
				if (fd == -1) {
					return JNI_FALSE;
				}
			}
		}

		if (fstat(fd, &statbuf) == -1) {
			close(fd);
			return JNI_FALSE;
		}
		close(fd);
#endif
	}

	return JNI_TRUE;
}

int memfs_link(const char *filename, u1_t * mem)
{
	return -1;
}

int memfs_unlink(const char *filename)
{
	return -1;
}

//MemoryProxy *memfs_mmap(FileDesc *fd, DomainDesc *domain, int flag) {
MemoryProxyHandle memfs_mmap(FileDesc * fd, DomainDesc * domain, int flag)
{
	ASSERTFD(fd);
	if (flag != MEMFS_RO) {
		exceptionHandlerMsg(THROW_RuntimeException, "BootFS can only create ReadOnlyMemory");
	}

	if (fd->mem == NULL) {
		fd->mem = allocReadOnlyMemory(domain, fd->data, fd->size);
	}
	return fd->mem;
}

FileDesc *memfs_open(DomainDesc * domain, const char *filename)
{
	FileDesc *desc;
	u1_t *codefile;
	u4_t size;
#ifndef READFROMZIP
	int fd;
	struct stat statbuf;
	char path[128];
#else
	zipentry entry;
#endif

#ifdef USE_FMAGIC
	ASSERTDOMAIN(domain);
#endif

	if ((codefile = libcache_lookup_jll(filename, &size)) == NULL) {
#ifdef READFROMZIP
		zip_reset();
		for (;;) {
			if (zip_next_entry(&entry) == -1)
				return NULL;
			if (strcmp(entry.filename, filename) == 0) {
				codefile = entry.data;
				size = entry.uncompressed_size;
				break;
			}
		}
#else
		fd = open(filename, O_RDONLY);
		if (fd == -1) {
			strcpy(path, "../libs/");
			strcat(path, filename);
			fd = open(path, O_RDONLY);
			if (fd == -1) {
				strcpy(path, "../domains/");
				strcat(path, filename);
				fd = open(path, O_RDONLY);
				if (fd == -1) {
					strcpy(path, "../");
					strcat(path, filename);
					fd = open(path, O_RDONLY);
					if (fd == -1) {
						return NULL;
					}
				}
			}
		}

		if (fstat(fd, &statbuf) == -1)
			return NULL;
		codefile = (char *) jxmalloc(statbuf.st_size MEMTYPE_OTHER);
		if (read(fd, codefile, statbuf.st_size) != statbuf.st_size)
			return NULL;
		close(fd);
		size = statbuf.st_size;
		/*return codefile; */
#endif
	}

	desc = jxmalloc(sizeof(FileDesc) MEMTYPE_OTHER);
	memset(desc, 0, sizeof(FileDesc));

#ifdef USE_QMAGIC
	desc->magic = MAGIC_FD;
#endif
	desc->pos = 0;
	desc->data = codefile;
	desc->size = size;
	desc->domain = domain;
	desc->filename = filename;

	return desc;
}

int memfs_readByte(FileDesc * desc, jbyte * value)
{
	ASSERTFD(desc);

	if (memfs_eof(desc))
		return -1;

	*value = *(jbyte *) (desc->data + desc->pos);
	desc->pos++;
	return 0;
}

int memfs_readInt(FileDesc * desc, jint * value)
{
	ASSERTFD(desc);

	if (memfs_eof(desc))
		return -1;

	*value = *(jint *) (desc->data + desc->pos);
	desc->pos += 4;
	return 0;
}

int memfs_readStringData(FileDesc * desc, u1_t * buf, jint length)
{
	ASSERTFD(desc);

	if (memfs_eof(desc))
		return -1;

	memcpy(buf, desc->data + desc->pos, length);
	buf[length] = 0;
	desc->pos += length;
	return 0;
}

int memfs_readString(FileDesc * desc, u1_t * buf, jint nbuf)
{
	jint length;

	ASSERTFD(desc);

	if (memfs_readInt(desc, &length))
		return -1;

	if (length >= nbuf) {
		domain_panic(curdom(), "buffer too small!\n");
		return -1;
	}

	return memfs_readStringData(desc, buf, length);
}

char *memfs_getString(FileDesc * desc)
{
	jint length;
	u1_t *strbuf;

	ASSERTFD(desc);

	if (memfs_readInt(desc, &length))
		return NULL;

	if (length > 10000) {
		domain_panic(curdom(), "buffer too small!\n");
		return NULL;
	}

	strbuf = (u1_t *) malloc_code(desc->domain, length + 1);

	if (memfs_readStringData(desc, strbuf, length))
		return NULL;

	return strbuf;
}

int memfs_readCode(FileDesc * desc, u1_t * buf, jint nbuf)
{
	ASSERTFD(desc);

	if (memfs_eof(desc))
		return -1;

	memcpy(buf, desc->data + desc->pos, nbuf);
	desc->pos += nbuf;

	return 0;
}

u4_t memfs_getPos(FileDesc * desc)
{
	ASSERTFD(desc);
	return desc->pos;
}

char *memfs_getFileName(FileDesc * desc)
{
	ASSERTFD(desc);
	return desc->filename;
}

u4_t memfs_getSize(FileDesc * desc)
{
	ASSERTFD(desc);
	return desc->size;
}

int memfs_seek(FileDesc * desc, unsigned long pos)
{
	ASSERTFD(desc);
	desc->pos = pos;
	return 0;
}

int memfs_testChecksum(FileDesc * desc)
{
	int i, checksum;
	ASSERTFD(desc);

	checksum = 0;
	for (i = 0; i < desc->size - 4; i++) {
		checksum = (checksum ^ (*(jbyte *) (desc->data + i))) & 0xff;
	}

	if (checksum != *(jint *) (desc->data + desc->size - 4))
		return -1;

	return 0;
}

jint memfs_eof(FileDesc * desc)
{
	ASSERTFD(desc);
	if (desc->pos < desc->size)
		return JNI_FALSE;
	return JNI_TRUE;
}

void memfs_close(FileDesc * desc)
{
	ASSERTFD(desc);
#ifdef USE_QMAGIC
	desc->magic = 0;
#endif
	desc->size = 0;
	jxfree(desc, sizeof(FileDesc) MEMTYPE_OTHER);
}
