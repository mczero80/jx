/********************************************************************************
 * Low-level memory management
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#include "all.h"
#include "malloc.h"

#ifndef KERNEL
#   define LOCK_MALLOC           DISABLE_IRQ
#   define UNLOCK_MALLOC         RESTORE_IRQ
#else
#   ifdef SMP
static spinlock_t allocating_mem_lock = SPIN_LOCK_UNLOCKED;
#      define LOCK_MALLOC           spin_lock(&allocating_mem_lock);
#      define UNLOCK_MALLOC         spin_unlock(&allocating_mem_lock);
#   else
#      define LOCK_MALLOC           DISABLE_IRQ
#      define UNLOCK_MALLOC         RESTORE_IRQ
#   endif			/* SMP */
#endif				/* KERNEL */

#ifndef KERNEL
u4_t total_ram = 0;
#endif

#if 1
#define mem_dprintf(args...)
#else
#define mem_dprintf(args...)   printf(args)
#endif

/*
#define VERBOSE_FREE 1
#define VERBOSE_MALLOC 1
*/

/* check if freed block was allocated */
//#define CHECK_FREE 1


/*
 * The global memory management.
 */

static void malloc_dump();
static void test();



#define BLOCKADDR_MASK (~(BLOCKSIZE-1))
#define ALIGN_NEXT_BLOCK(a) ((((u4_t)(a))+(BLOCKSIZE-1)) & BLOCKADDR_MASK)
#define ALIGN_PREV_BLOCK(a) (((u4_t)(a)) & BLOCKADDR_MASK)


static char *jxmem_start = 0;
static char *jxmem_end = 0;
static u4_t n_blocks = 0;
static u4_t n_free;
static u4_t current_block = 0;	/* rotating pointer that marks the start of the free block search */
static u1_t *bitmap;
static u4_t bitmap_size;
static u4_t bitmap_size_blocks;

static u4_t unusedMem = 0;
static u4_t allocedMem = 0;

#ifdef MALLOC_STAT
static u4_t peak_alloced = 0;
#endif

static char *markUsed(u4_t nblocks);
static void markCurrentUsed();
static void test_mem();

#ifdef KERNEL
#include "multiboot.h"
extern struct multiboot_info boot_info;
#endif

#ifdef PROFILE_EVENT_JXMALLOC
u4_t event_jxmalloc, event_jxfree;
#endif

static int inited = 0;
void jxmalloc_init()
{
	if (inited)
		sys_panic("jxmalloc already initialized!");
	mem_dprintf("jxmalloc_init\n");
#ifdef KERNEL
	{
		struct multiboot_module *m = (struct multiboot_module *) boot_info.mods_addr;
		jxmem_end = (char *) ALIGN_PREV_BLOCK(0x100000 + boot_info.mem_upper * 1024);
		if (boot_info.mods_count == 1) {
			jxmem_start = (char *) ALIGN_NEXT_BLOCK(m[0].mod_end + 1);
		} else if (boot_info.mods_count == 2) {
			jxmem_start = (char *) ALIGN_NEXT_BLOCK(m[1].mod_end + 1);
		} else {
			sys_panic("max 2 modules expected");
		}
	}
#else
	{
		char *s = getenv("JX_RAM");
		total_ram = GLOBAL_MEMSIZE;
		if (s != NULL) {
			total_ram = strtol(s, NULL, 10);
			if (total_ram == 0)
				total_ram = GLOBAL_MEMSIZE;
		}
		jxmem_start = (char *) malloc(total_ram);
		if (jxmem_start == NULL)
			sys_panic("could not malloc");
		jxmem_start = (char *) ALIGN_NEXT_BLOCK(jxmem_start);
		jxmem_end = jxmem_start + total_ram;
		jxmem_end = (char *) ALIGN_PREV_BLOCK(jxmem_end);
	}
#endif

#ifdef KERNEL
	//    test_mem();
#endif

	n_blocks = jxmem_end - jxmem_start;
	n_blocks /= BLOCKSIZE;
	n_free = n_blocks;
	bitmap_size = n_blocks >> 3;	/* div 8 */
	bitmap_size_blocks = (ALIGN_NEXT_BLOCK(bitmap_size)) / BLOCKSIZE;
	bitmap = jxmem_start;
	current_block = 0;
	memset(bitmap, 0, bitmap_size);
	markUsed(bitmap_size_blocks);
	// malloc_dump();
	/*test();
	   exit(0); */
	inited = 1;

#ifdef PROFILE_EVENT_JXMALLOC
	event_jxmalloc = createNewEvent("JXMALLOC");
	event_jxfree = createNewEvent("JXFREE");
#endif				/* PROFILE_EVENT_MEMORY_ALLOC */

}

static void test_mem()
{
#ifdef DEBUG
	volatile unsigned char *i;
	printf("Testing memory (%p-%p)...\n", jxmem_start, jxmem_end);
	for (i = jxmem_start; i < jxmem_end; i++) {
		*i = ((u4_t) i & 0xff);
		if (((u4_t) i & 0xff) != *i)
			sys_panic("Memory corruption!");
	}
	memset(jxmem_start, 0xcc, jxmem_end - jxmem_start);
	printf("done.\n");
#endif
}


#define GETADDR(block) (jxmem_start + block * BLOCKSIZE)

static char *markUsed(u4_t nblocks)
{
	u4_t i;
	char *addr = GETADDR(current_block);

	for (i = 0; i < nblocks; i++) {
		markCurrentUsed();
		current_block++;
		current_block %= n_blocks;
	}
	return addr;
}

static void markCurrentUsed()
{
	u4_t pos = current_block >> 3;
	u1_t o = current_block & 0x7;
	bitmap[pos] |= 1 << o;
	n_free--;
}


static void markUnused(u4_t blocknr)
{
	u4_t pos = blocknr >> 3;
	u1_t o = blocknr & 0x7;
	o = 1 << o;
#ifdef CHECK_FREE
	if ((bitmap[pos] & o) == 0) {
		sys_panic("freed block was not marked as used!");
	}
#endif				/* CHECK_FREE */
	o = ~o;
	bitmap[pos] &= o;
	n_free++;
}

static void nextFree(u4_t dont_cross_this_block)
{
	while (bitmap[current_block >> 3] & 1 << (current_block & 0x7)) {
		current_block++;
		current_block %= n_blocks;
		if (current_block == dont_cross_this_block) {
			printf("ERROR: NO MORE MEM\n");
//                      exceptionHandler(THROW_MemoryExhaustedException);
			sys_panic("Out of memory");
#if 0
			jxmalloc_stat();
#ifndef KERNEL
			printf("Out of memory. You should increase JX_RAM.\n");
#endif
			sys_panic("Out of memory");
#endif
		}
	}
}

static int isFree(u4_t nblocks, u4_t dont_cross_this_block)
{
	u4_t i;
	if (current_block + nblocks > n_blocks) {
		return JNI_FALSE;
	}
	for (i = 0; i < nblocks; i++) {
		if (bitmap[(current_block + i) >> 3] & 1 << ((current_block + i) & 0x7)) {
			return JNI_FALSE;
		}
	}
	return JNI_TRUE;
}

#ifdef MALLOC_STAT
struct alloc_stat_s {
	u4_t heap;
	u4_t code;
	u4_t stack;
	u4_t memobj;
	u4_t profiling;
	u4_t emulation;
	u4_t tmp;
	u4_t dcb;
	u4_t other;
};
static struct alloc_stat_s alloc_stat;
#endif

void jxmalloc_stat()
{
	printf("Memory start=%p end=%p size=%ld\n", jxmem_start, jxmem_end, (u4_t) (jxmem_end - jxmem_start));
	printf("Block size: %ld\n", BLOCKSIZE);
	printf("Bitmap size (bytes): %ld\n", bitmap_size);
	printf("Bitmap size (blocks): %ld\n", bitmap_size_blocks);
	printf("Number of blocks: %ld\n", n_blocks);
	printf("Free blocks: %ld (%d%%)\n", n_free, (n_free * 100) / n_blocks);
	printf("Current block: %ld\n", current_block);
	printf("Allocated memory: %ld\n", (n_blocks - n_free) * BLOCKSIZE);
#ifdef MALLOC_STAT
	printf("Peak allocated memory: %ld\n", peak_alloced);
#endif
	printf("Available memory: %ld\n", (n_free) * BLOCKSIZE);
	if (allocedMem > 0) {
#ifdef KERNEL
		printf("Fragmentation: %ld/%ld (%d%%)\n", unusedMem, allocedMem, (int) (unusedMem * 100 / allocedMem));
#else
		printf("Fragmentation: %ld/%ld (%3.2f%%)\n", unusedMem, allocedMem, (unusedMem * 100.0 / allocedMem));
#endif
	}
#ifdef MALLOC_STAT
	printf(" Used for:\n");
	printf("   Heap      %9ld\n", alloc_stat.heap);
	printf("   Code      %9ld\n", alloc_stat.code);
	printf("   Stack     %9ld\n", alloc_stat.stack);
	printf("   Memobj    %9ld\n", alloc_stat.memobj);
	printf("   Profiling %9ld\n", alloc_stat.profiling);
	printf("   Emulation %9ld\n", alloc_stat.emulation);
	printf("   Tmp       %9ld\n", alloc_stat.tmp);
	printf("   DCB       %9ld\n", alloc_stat.dcb);
	printf("   Other     %9ld\n", alloc_stat.other);
#endif
}

u4_t jxmalloc_getTotalMemory()
{
	return (u4_t) (jxmem_end - jxmem_start);
}

u4_t jxmalloc_getTotalFreeMemory()
{
	return (u4_t) (n_free * BLOCKSIZE);
}

static void malloc_dump()
{
	u4_t i;
	jxmalloc_stat();
	printf("Memory block bitmap:\n");
	for (i = 0; i < n_blocks; i++) {
		if (i % 8 == 0)
			printf("\n%p ", jxmem_start + i * 8 * BLOCKSIZE);
		printf("%02x ", bitmap[i]);
	}
	printf("\n");
}

static void *jxmalloc_internal(u4_t size_blk, u4_t align, u4_t ** start MEMTYPE_INFO);
#ifdef MALLOC_STAT
#define MEMTYPE_PARAM , memtype
#else
#define MEMTYPE_PARAM
#endif

void *jxmalloc(u4_t size MEMTYPE_INFO)
{
	u4_t *start;
	u4_t size_blk;
#if 0
	if (__current[0]) {
		code_t ip;
		printf("jxmalloc%d %12d ", curdom()->id, size);
		ip = getCaller(2);
		print_eip_info(ip);
		ip = getCaller(3);
		print_eip_info(ip);
		printf("\n");
		curdom()->memusage += size;
	} else {
		printf("jxmalloc? %12d ", size);
	}
#endif
	size = ALIGN_NEXT_BLOCK(size);
	size_blk = size >> BLOCKADDR_N_NULLBITS;
	jxmalloc_internal(size_blk, 1, &start MEMTYPE_PARAM);
}

/* align at address: addr % align == 0 */
void *jxmalloc_align(u4_t size_blk, u4_t align, u4_t ** start MEMTYPE_INFO)
{
	return jxmalloc_internal(size_blk, align, start MEMTYPE_PARAM);
}

/*#define ZERO_MALLOCED_MEM 1*/
/*
 * size_blk: number of blocks to allocate
 * align: alignment in bytes
 * start: contains start address on return
 */
static void *jxmalloc_internal(u4_t size_blk, u4_t align, u4_t ** start MEMTYPE_INFO)
{
	char *addr = NULL;
	u4_t start_block = current_block;
	u4_t alignmask;

	LOCK_MALLOC;

#ifdef MALLOC_STAT
	switch (memtype) {
	case MEMTYPE_HEAP_PARAM:
		alloc_stat.heap += size_blk * BLOCKSIZE;
		break;
	case MEMTYPE_CODE_PARAM:
		alloc_stat.code += size_blk * BLOCKSIZE;
		break;
	case MEMTYPE_MEMOBJ_PARAM:
		alloc_stat.memobj += size_blk * BLOCKSIZE;
		break;
	case MEMTYPE_PROFILING_PARAM:
		alloc_stat.profiling += size_blk * BLOCKSIZE;
		break;
	case MEMTYPE_STACK_PARAM:
		alloc_stat.stack += size_blk * BLOCKSIZE;
		break;
	case MEMTYPE_EMULATION_PARAM:
		alloc_stat.emulation += size_blk * BLOCKSIZE;
		break;
	case MEMTYPE_TMP_PARAM:
		alloc_stat.tmp += size_blk * BLOCKSIZE;
		break;
	case MEMTYPE_DCB_PARAM:
		alloc_stat.dcb += size_blk * BLOCKSIZE;
		break;
	case MEMTYPE_OTHER_PARAM:
		alloc_stat.other += size_blk * BLOCKSIZE;
		break;
	default:
		sys_panic("MEMTYPE unknown");
	}
/*
	if (memtype==MEMTYPE_OTHER_PARAM) {
		printf("***SIZE: %ld\n", size_blk*BLOCKSIZE);
		printStackTraceNew("MALLOC_OTHER");
	}
*/


#endif


#ifdef VERBOSE_MALLOC
	{
		code_t ip;
		//printf("free: %ld",jxmalloc_getTotalFreeMemory());
		printf("jxmalloc(%ld,%ld) ", size_blk * BLOCKSIZE, align);
		//if (curdom()) printf(" dom=%d ", curdom()->id);  
		ip = getCaller(1);
		print_eip_info(ip);
		ip = getCaller(2);
		print_eip_info(ip);
		ip = getCaller(3);
		print_eip_info(ip);
		ip = getCaller(4);
		print_eip_info(ip);
	}
#endif				/* VERBOSE_MALLOC */

	alignmask = align - 1;

	for (;;) {
		if (isFree(size_blk, start_block)) {
			mem_dprintf("jxmalloc_align: free %ld at addr %p\n", current_block, GETADDR(current_block));
			if (((u4_t) (GETADDR(current_block)) & alignmask)
			    == 0) {
				addr = markUsed(size_blk);
				goto finish;
			} else {
				current_block++;
				current_block %= n_blocks;
				if (start_block == current_block) {
					printf("Request for %d blocks at alignment %d cannot be satisfied.\n", size_blk, align);
					malloc_dump();
					sys_panic("NO MORE MEM BLOCKS");
				}
			}
		} else {
			current_block++;
			current_block %= n_blocks;
			if (start_block == current_block) {
				printf("Request for %d blocks at alignment %d cannot be satisfied.\n", size_blk, align);
				malloc_dump();
				sys_panic("NO MORE MEM BLOCKS");
			}
		}
		nextFree(start_block);
	}
      finish:

#ifdef ZERO_MALLOCED_MEM
	memset(addr, 0, size_blk * BLOCKSIZE);
#endif

#ifdef VERBOSE_MALLOC
	//printf("free after jxmalloc: %ld ",jxmalloc_getTotalFreeMemory());
	printf(" -> %p\n", addr);
#endif				/* VERBOSE_MALLOC */

#ifdef PROFILE_EVENT_JXMALLOC
	RECORD_EVENT_INFO(event_jxmalloc, jxmalloc_getTotalFreeMemory());
#endif

#ifdef MALLOC_STAT
	{
		u4_t peak = (n_blocks - n_free) * BLOCKSIZE;
		if (peak > peak_alloced) {
			peak_alloced = peak;
		}
	}
#endif

	UNLOCK_MALLOC;
	*start = addr;
	return addr;
}


void jxfree(void *addr, u4_t size MEMTYPE_INFO)
{
	LOCK_MALLOC;

#ifdef MALLOC_STAT
	switch (memtype) {
	case MEMTYPE_HEAP_PARAM:
		alloc_stat.heap -= size;
		break;
	case MEMTYPE_CODE_PARAM:
		alloc_stat.code -= size;
		break;
	case MEMTYPE_MEMOBJ_PARAM:
		alloc_stat.memobj -= size;
		break;
	case MEMTYPE_PROFILING_PARAM:
		alloc_stat.profiling -= size;
		break;
	case MEMTYPE_STACK_PARAM:
		alloc_stat.stack -= size;
		break;
	case MEMTYPE_EMULATION_PARAM:
		alloc_stat.emulation -= size;
		break;
	case MEMTYPE_TMP_PARAM:
		alloc_stat.tmp -= size;
		break;
	case MEMTYPE_DCB_PARAM:
		alloc_stat.dcb -= size;
		break;
	case MEMTYPE_OTHER_PARAM:
		alloc_stat.other -= size;
		break;
	default:
		sys_panic("MEMTYPE unknown");
	}
#endif

#ifdef VERBOSE_FREE
	{
		code_t ip;
		printf("jxfree(%p,%ld), free=%d ", addr, size, jxmalloc_getTotalFreeMemory());
		//if (curdom()) printf(" dom=%d ", curdom()->id);  
		ip = getCaller(1);
		print_eip_info(ip);
		ip = getCaller(2);
		print_eip_info(ip);
		ip = getCaller(3);
		print_eip_info(ip);
	}
#endif				/* VERBOSE_FREE */

	{
		u4_t b = ((char *) addr - jxmem_start) / BLOCKSIZE;
		u4_t nb, i;
		size = ALIGN_NEXT_BLOCK(size);
		nb = size / BLOCKSIZE;
		//printf("NB:%d (size=%d,blocksize=%d\n",nb,size,BLOCKSIZE);
		for (i = 0; i < nb; i++) {
			//printf("  markunused %ld\n",b+i);
			markUnused(b + i);
		}
	}
#ifdef VERBOSE_FREE
	printf("       free: %ld\n", jxmalloc_getTotalFreeMemory());
#endif				/* VERBOSE_FREE */
#ifdef PROFILE_EVENT_JXMALLOC
	RECORD_EVENT_INFO(event_jxfree, jxmalloc_getTotalFreeMemory());
#endif

	UNLOCK_MALLOC;
}

TempMemory *jxmalloc_tmp(u4_t size)
{
	TempMemory *t;
	char *m;
	size += sizeof(TempMemory);
	size = ALIGN_NEXT_BLOCK(size);
	m = jxmalloc(size MEMTYPE_TMP);
	t = (TempMemory *) m;
	t->size = size;
	t->start = m;
	t->free = m + (sizeof(TempMemory));
	t->border = m + size;
	return t;
}

void jxfree_tmp(TempMemory * m)
{
	jxfree(m->start, m->size MEMTYPE_TMP);
}

#if 0
static void test()
{
	char *a = jxmalloc(2400);
	char *b = jxmalloc(24000);
	char *c = jxmalloc(8000);
	char *d = jxmalloc(10000);

	malloc_dump();
	jxfree(a, 2400);
	malloc_dump();
}
#endif

char *malloc_code(DomainDesc * domain, u4_t size)
{
	char *data;
	char *nextObj;
	u4_t c;
	u4_t chunksize = domain->code_bytes;

	DISABLE_IRQ;

	if (domain->cur_code == -1) {
		//printf("Chunksize domain %d: %d\n", domain->id, chunksize);
		domain->code[0] = (char *) jxmalloc(chunksize MEMTYPE_CODE);
		domain->codeBorder[0] = domain->code[0] + chunksize;
		domain->codeTop[0] = domain->code[0];
		domain->cur_code = 0;
	}
	c = domain->cur_code;
	nextObj = domain->codeTop[c] + size;
	if (nextObj > domain->codeBorder[c]) {
		//printf("Domain: %s\n", domain->domainName);
		c++;
		if (c == CODE_FRAGMENTS)
			sys_panic("NO MORE CODE");
		domain->code[c] = (char *) jxmalloc(chunksize MEMTYPE_CODE);
		domain->codeBorder[c] = domain->code[c] + chunksize;
		domain->codeTop[c] = domain->code[c];
		domain->cur_code = c;
		nextObj = domain->codeTop[c] + size;
		if (nextObj > domain->codeBorder[c])
			sys_panic("can`t alloc %i byte", size);
	}
	data = domain->codeTop[c];
	domain->codeTop[c] = nextObj;

	RESTORE_IRQ;

	return data;
}

LibDesc *malloc_libdesc(DomainDesc * domain)
{
	return (LibDesc *) malloc_code(domain, sizeof(LibDesc));
}

ClassDesc *malloc_classdesc(DomainDesc * domain, u4_t namelen)
{
	char *m = malloc_code(domain, sizeof(ClassDesc) + namelen);
	ClassDesc *cd = (ClassDesc *) m;
	memset(m, 0, sizeof(ClassDesc) + namelen);
	cd->name = m + sizeof(ClassDesc);
	return cd;
}

PrimitiveClassDesc *malloc_primitiveclassdesc(DomainDesc * domain, u4_t namelen)
{
	char *m = malloc_code(domain, sizeof(PrimitiveClassDesc) + namelen);
	PrimitiveClassDesc *cd = (PrimitiveClassDesc *) m;
	memset(m, 0, sizeof(PrimitiveClassDesc) + namelen);
	cd->name = m + sizeof(PrimitiveClassDesc);
	return cd;
}

ArrayClassDesc *malloc_arrayclassdesc(DomainDesc * domain, u4_t namelen)
{
	char *m = malloc_code(domain, sizeof(ArrayClassDesc) + namelen + (11 + 1) * 4);	// vtable
	ArrayClassDesc *cd = (ArrayClassDesc *) m;
	memset(m, 0, sizeof(ArrayClassDesc) + namelen);
	cd->name = m + sizeof(ArrayClassDesc);
	cd->vtable = m + sizeof(ArrayClassDesc) + namelen + 4 /* classptr at negative offset */ ;
	return cd;
}

Class *malloc_class(DomainDesc * domain)
{
	//return (ClassDesc*)jxmalloc(sizeof(ClassDesc));
	return (Class *) malloc_code(domain, sizeof(Class));
}

Class *malloc_classes(DomainDesc * domain, u4_t number)
{
	//return (ClassDesc*)jxmalloc(sizeof(ClassDesc));
	return (Class *) malloc_code(domain, sizeof(Class) * number);
}

MethodDesc *malloc_methoddesc(DomainDesc * domain)
{
	//return (ClassDesc*)jxmalloc(sizeof(ClassDesc));
	return (MethodDesc *) malloc_code(domain, sizeof(MethodDesc));
}

MethodDesc *malloc_methoddescs(DomainDesc * domain, u4_t number)
{
	//return (ClassDesc*)jxmalloc(sizeof(ClassDesc));
	return (MethodDesc *) malloc_code(domain, sizeof(MethodDesc) * number);
}

ExceptionDesc *malloc_exceptiondescs(DomainDesc * domain, u4_t number)
{
	char *m = malloc_code(domain, sizeof(ExceptionDesc) * number);
	//memset(m, 0, sizeof(ExceptionDesc)*number);
	return (ExceptionDesc *) m;
}

ClassDesc *malloc_classdescs(DomainDesc * domain, u4_t number)
{
	//return (ClassDesc*)jxmalloc(sizeof(ClassDesc));
	return (ClassDesc *) malloc_code(domain, sizeof(ClassDesc) * number);
}


char **malloc_vtableSym(DomainDesc * domain, u4_t vtablelen)
{
	return (char **) malloc_code(domain, vtablelen * 3 * sizeof(char *));
}

MethodDesc *malloc_methods(DomainDesc * domain, u4_t number)
{
	return (MethodDesc *) malloc_code(domain, number * sizeof(MethodDesc));
}

MethodDesc **malloc_methodVtable(DomainDesc * domain, u4_t number)
{
	return (MethodDesc **) malloc_code(domain, number * sizeof(MethodDesc *));
}

ClassDesc **malloc_classdesctable(DomainDesc * domain, u4_t number)
{
	return (ClassDesc **) malloc_code(domain, number * sizeof(ClassDesc *));
}

SharedLibDesc **malloc_sharedlibdesctable(DomainDesc * domain, u4_t number)
{
	return (SharedLibDesc **) malloc_code(domain, number * sizeof(SharedLibDesc *));
}

FieldDesc *malloc_fielddescs(DomainDesc * domain, u4_t number)
{
	return (FieldDesc *) malloc_code(domain, sizeof(FieldDesc) * number);
}

char **malloc_tmp_stringtable(DomainDesc * domain, TempMemory * mem, u4_t number)
{
	char *m;
	char *n;
	number *= 4;
	n = mem->free + number;
	if (n > mem->border)
		domain_panic(domain, "temp space not sufficient");
	m = mem->free;
	mem->free = n;
	return (char **) m;
}

code_t *malloc_vtable(DomainDesc * domain, u4_t number)
{
	return (code_t *) malloc_code(domain, number * sizeof(code_t));
}

static u4_t sharedLibID = 1;

SharedLibDesc *malloc_sharedlibdesc(DomainDesc * domain, u4_t namelen)
{
	char *m = malloc_code(domain, sizeof(SharedLibDesc) + namelen);
	SharedLibDesc *sl = (SharedLibDesc *) m;
	memset(m, 0, sizeof(SharedLibDesc) + namelen);
	sl->name = m + sizeof(SharedLibDesc);	/* name allocated immediately after LibDesc */
	DISABLE_IRQ;
	sl->id = sharedLibID++;
	RESTORE_IRQ;
	return sl;
}

char *malloc_string(DomainDesc * domain, u4_t len)
{
	return (char *) malloc_code(domain, (len + 1) * sizeof(char));
}

char *malloc_staticsmap(DomainDesc * domain, u4_t size)
{
	return (char *) malloc_code(domain, size);
}

char *malloc_objectmap(DomainDesc * domain, u4_t size)
{
	return (char *) malloc_code(domain, size);
}

char *malloc_argsmap(DomainDesc * domain, u4_t size)
{
	return (char *) malloc_code(domain, size);
}


SymbolDesc **malloc_symboltable(DomainDesc * domain, u4_t len)
{
	return (SymbolDesc **) malloc_code(domain, sizeof(SymbolDesc *) * len);
}

SymbolDesc *malloc_symbol(DomainDesc * domain, u4_t size)
{
	return (SymbolDesc *) malloc_code(domain, size);
}

char *malloc_stackmap(DomainDesc * domain, u4_t size)
{
	return (char *) malloc_code(domain, size);
}

char *malloc_proxycode(DomainDesc * domain, u4_t size)
{
	return (char *) malloc_code(domain, size);
}

char *malloc_cpudesc(DomainDesc * domain, u4_t size)
{				/* only in DomainZero ??? */
	return (char *) malloc_code(domain, size);
}

ByteCodeDesc *malloc_bytecodetable(DomainDesc * domain, u4_t len)
{
	return (ByteCodeDesc *) malloc_code(domain, sizeof(ByteCodeDesc) * len);
}

SourceLineDesc *malloc_sourcelinetable(DomainDesc * domain, u4_t len)
{
	return (SourceLineDesc *) malloc_code(domain, sizeof(SourceLineDesc) * len);
}

u4_t *malloc_staticfields(DomainDesc * domain, u4_t number)
{
	return (u4_t *) malloc_code(domain, number * 4);
}

struct meta_s *malloc_metatable(DomainDesc * domain, u4_t number)
{
	return (struct meta_s *) malloc_code(domain, number * sizeof(struct meta_s));
}

/*
 * malloc_nativecode
 *
 * alloc memory in code-segment of domain 16 byte alignd
 * because of the instruction buffer size of the PIII.
 *
 */
u1_t *malloc_nativecode(DomainDesc * domain, u4_t size)
{
#define ALIGN_CODE_TO_IBUFFERSIZE 1
#ifdef ALIGN_CODE_TO_IBUFFERSIZE
	/* align code to instruction buffer size (PIII = 16 byte) */
	return (u1_t *) (((u4_t) malloc_code(domain, size + 16) + 15) & (u4_t) ~ 0xf);
#else
	/* align code to 32 bit or 4 bytes */
	return (u1_t *) (((u4_t) malloc_code(domain, size + 4) + 3) & (u4_t) ~ 0x3);
#endif
}


struct nameValue_s *malloc_domainzero_namevalue(u4_t namelen)
{
	char *m = malloc_code(domainZero, sizeof(struct nameValue_s) + namelen);
	struct nameValue_s *s = (struct nameValue_s *) m;
	s->name = m + sizeof(struct nameValue_s);
	return s;
}

//#ifndef STACK_ON_HEAP
u4_t *malloc_threadstack(DomainDesc * domain, u4_t size, u4_t align)
{
	u4_t *m;
	u4_t start;
	if (size % BLOCKSIZE != 0)
		sys_panic("stacksize must be multiple of blocksize");
/* REMOVE
	if (align % BLOCKSIZE != 0)
		sys_panic("align must be multiple of blocksize");
*/

	m = jxmalloc_align(size >> BLOCKADDR_N_NULLBITS, align, &start MEMTYPE_STACK);
	ASSERT(start == m);	// FIXME: remember start

	ASSERT(((u4_t) m & (align - 1)) == 0);
	return m;
}

void free_threadstack(DomainDesc * domain, u4_t * m, u4_t size)
{
	jxfree(m, size MEMTYPE_STACK);
}

//#endif /* STACK_ON_HEAP */
