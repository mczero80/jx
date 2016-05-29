#ifndef MALLOC_H
#define MALLOC_H
#include "types.h"

typedef struct TempMemory_s {
	u4_t size;
	char *free;
	char *start;
	char *border;
} TempMemory;


//#define BLOCKSIZE  4096
//#define BLOCKADDR_N_NULLBITS 12

#define BLOCKSIZE  1024
#define BLOCKADDR_N_NULLBITS 10
//FIXME jgbauman: unused
//#define BLOCKADDR_MASK 0xfffffc00

//#define BLOCKSIZE  512
//#define BLOCKADDR_N_NULLBITS 9


#ifdef MALLOC_STAT
#define MEMTYPE_OTHER  ,0
#define MEMTYPE_HEAP   ,1
#define MEMTYPE_STACK  ,2
#define MEMTYPE_MEMOBJ ,3
#define MEMTYPE_CODE   ,4
#define MEMTYPE_PROFILING   ,5
#define MEMTYPE_EMULATION   ,6
#define MEMTYPE_TMP   ,7
#define MEMTYPE_DCB   ,8
#define MEMTYPE_INFO , int memtype
/* internal use */
#define MEMTYPE_OTHER_PARAM  0
#define MEMTYPE_HEAP_PARAM   1
#define MEMTYPE_STACK_PARAM 2
#define MEMTYPE_MEMOBJ_PARAM 3
#define MEMTYPE_CODE_PARAM   4
#define MEMTYPE_PROFILING_PARAM   5
#define MEMTYPE_EMULATION_PARAM   6
#define MEMTYPE_TMP_PARAM   7
#define MEMTYPE_DCB_PARAM   8
#else
#define MEMTYPE_OTHER
#define MEMTYPE_HEAP 
#define MEMTYPE_STACK
#define MEMTYPE_MEMOBJ
#define MEMTYPE_CODE  
#define MEMTYPE_PROFILING
#define MEMTYPE_EMULATION
#define MEMTYPE_TMP
#define MEMTYPE_DCB
#define MEMTYPE_INFO
#endif

void *jxmalloc(u4_t size MEMTYPE_INFO);
/* start contains unaligned start address of reserved memory which must be passed to jxfree*/
void *jxmalloc_align(u4_t size_blk, u4_t align, u4_t ** start MEMTYPE_INFO);	
void jxfree(void *addr, u4_t size MEMTYPE_INFO);


/* allocate temporary memory (for internal core use) */
TempMemory *jxmalloc_tmp(u4_t size);
void jxfree_tmp(TempMemory * m);

/* print statistics */
void jxmalloc_stat();

struct DomainDesc_s;
u4_t *malloc_threadstack(struct DomainDesc_s *domain, u4_t size,
			 u4_t align);
void free_threadstack(struct DomainDesc_s *domain, u4_t * m, u4_t size);


char *malloc_code(struct DomainDesc_s *domain, u4_t size);

#endif				/* MALLOC_H */
