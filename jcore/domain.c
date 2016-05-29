/********************************************************************************
 * Domain management
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#include "all.h"
#include "runq.h"
#include "sched.h"

DomainDesc *domainZero;

/*
 * Domain data
 */

static int domainsys_inited = 0;
static u4_t numberOfDomains = 0;
static u4_t currentDomainID = 0;
static char *domainMem = NULL;
static char *domainMemBorder = NULL;
static char *domainMemCurrent = NULL;
#define DOMAINMEM_SIZEBYTES (OBJSIZE_DOMAINDESC*4)
#define DOMAINMEM_TOTALBYTES (DOMAINMEM_SIZEBYTES * MAX_NUMBER_DOMAINS)
#define DOMAINDESC_OFFSETBYTES (( 2 + XMOFF) *4)

#undef FASTER_METHOD_LOOKUP

void check_threadindomain(DomainDesc * domain);

void init_domainsys()
{
	DomainDesc *d;
	u4_t *domainMemStart;
	domainMem = jxmalloc(DOMAINMEM_TOTALBYTES MEMTYPE_DCB);
	// set state to defined value
	domainMemStart = domainMem;
	numberOfDomains = 0;
	domainMemCurrent = domainMem;
	domainMemBorder = domainMem + DOMAINMEM_TOTALBYTES;

	while (domainMemStart < domainMemBorder) {
		u4_t *mem = domainMemStart;
		d = (DomainDesc *) (((char *) mem) + DOMAINDESC_OFFSETBYTES);
		/*printf("INIT d=%p\n", d); */
		d->state = DOMAIN_STATE_FREE;
		domainMemStart += DOMAINMEM_SIZEBYTES;
	}

	domainsys_inited = 1;
	domainZero =
	    createDomain("DomainZero", HEAP_BYTES_DOMAINZERO, -1, -1, NULL, -1, CODE_BYTES_DOMAINZERO, GC_IMPLEMENTATION_DEFAULT,
			 NULL);
	if (domainZero == NULL)
		sys_panic("Cannot create domainzero");
#ifdef PORTAL_INTERCEPTOR
	domainZero->memberOfTCB = JNI_TRUE;
#endif
}

extern ClassDesc *domainClass;

DomainDesc *specialAllocDomainDesc()
{
	DomainDesc *d;
	ObjectDesc *obj;
	u4_t *domainMemStart;

	LOCK_DOMAINS;
	if (numberOfDomains == MAX_NUMBER_DOMAINS)
		return NULL;

	domainMemStart = domainMemCurrent;
	do {
		u4_t *mem = domainMemCurrent;
		d = (DomainDesc *) (((char *) mem) + DOMAINDESC_OFFSETBYTES);
		//printf("d=%p state=%d\n", d, d->state);
		domainMemCurrent += DOMAINMEM_SIZEBYTES;
		if (domainMemCurrent >= domainMemBorder)
			domainMemCurrent = domainMem;
		if (domainMemCurrent == domainMemStart) {
			printf("ERROR: NO MORE DOMAINS\n");
			return NULL;
		}
	} while (d->state != DOMAIN_STATE_FREE);

	memset(d, 0, sizeof(DomainDesc));
#ifdef USE_QMAGIC
	d->magic = MAGIC_DOMAIN;
#endif
	d->state = DOMAIN_STATE_CREATING;
	d->id = currentDomainID++;
	numberOfDomains++;

	UNLOCK_DOMAINS;
	return d;
}


DomainDesc *createDomain(char *domainName, jint gcinfo0, jint gcinfo1, jint gcinfo2, char *gcinfo3, jint gcinfo4, u4_t code_bytes,
			 int gcImpl, ArrayDesc * schedinfo)
{
	u1_t *mem;
	DEPDesc *gcdep;
	DomainDesc *domain;
	u4_t libMemSize;

	//  printf("CREATEDOMAIN: %s heap=%ld\n", domainName, heap_bytes);

	domain = specialAllocDomainDesc();
	if (domain == NULL) {
		printf("ERROR: NO MORE DOMAINS\n");
		return NULL;
	}
	domain->maxNumberOfLibs = MAX_NUMBER_LIBS;
	domain->numberOfLibs = 0;
	domain->arrayClasses = NULL;

	domain->scratchMem = jxmalloc(DOMAIN_SCRATCHMEM_SIZE MEMTYPE_OTHER);
	domain->scratchMemSize = DOMAIN_SCRATCHMEM_SIZE;

	/* alloc code mem */
	domain->cur_code = -1;
	if (code_bytes != -1) {
		domain->code_bytes = code_bytes;
	} else {
		domain->code_bytes = CODE_BYTES;
	}

	libMemSize = sizeof(LibDesc *) * domain->maxNumberOfLibs + strlen(domainName) + 1
#ifdef USE_LIB_INDEX
	    + sizeof(LibDesc *) * domain->maxNumberOfLibs + sizeof(jint *) * domain->maxNumberOfLibs
#endif
	    + gc_mem();
	mem = malloc_code(domain, libMemSize);
	//printf("DOMAINMALLOC domain=%p libsize=%d codesize=%d, heapsize=%d, start=%p\n", domain, libMemSize, code_bytes, heap_bytes, mem);

#ifdef NOPREEMPT
	//  curdom()->atomic_code = jxmalloc(4096);
#endif

	domain->libMemSize = libMemSize;
	domain->libs = (LibDesc **) mem;
	mem += sizeof(LibDesc *) * domain->maxNumberOfLibs;
	domain->domainName = mem;
	mem += strlen(domainName) + 1;

#ifdef USE_LIB_INDEX
	domain->ndx_libs = (LibDesc **) mem;
	mem += sizeof(LibDesc *) * domain->maxNumberOfLibs;
	memset(domain->ndx_libs, 0, sizeof(LibDesc *) * domain->maxNumberOfLibs);
	domain->sfields = (jint **) mem;
	mem += sizeof(jint *) * domain->maxNumberOfLibs;
	memset(domain->sfields, 0, sizeof(jint *) * domain->maxNumberOfLibs);
#endif



	strcpy(domain->domainName, domainName);
	domain->threads = NULL;
	domain->services[0] = SERVICE_ENTRY_CHANGING;	/* reserve; index 0 can be used as invalid index */

	gc_init(domain, mem, gcinfo0, gcinfo1, gcinfo2, gcinfo3, gcinfo4, gcImpl);

#ifdef NEW_SCHED
	sched_local_init(domain, 0);
#endif

	domain->state = DOMAIN_STATE_ACTIVE;

	return domain;
}

void destroyDomain(DomainDesc * domain)
{
}

int findClassForMethod(MethodDesc * method, Class ** class)
{
	int d, l, c, m;
	DomainDesc *domain;
	char *mem;
	int ret = -1;

	if (method == NULL)
		return -1;

	LOCK_DOMAINS;

	for (mem = domainMem; mem < domainMemBorder; mem += DOMAINMEM_SIZEBYTES) {
		domain = (DomainDesc *) (mem + DOMAINDESC_OFFSETBYTES);
		for (l = 0; l < domain->numberOfLibs; l++) {
			LibDesc *lib = domain->libs[l];
			Class *allClasses = lib->allClasses;
			for (c = 0; c < lib->numberOfClasses; c++) {
				ClassDesc *classDesc = allClasses[c].classDesc;
				for (m = 0; m < classDesc->numberOfMethods; m++) {
					if (&classDesc->methods[m] == method) {
						*class = &allClasses[c];
						//*class = classDesc;
						ret = 0;
						goto finish;
					}
				}
			}
		}
	}

      finish:
	UNLOCK_DOMAINS;

	return ret;
}

int findMethodAtFramePointer(u4_t * ebp, MethodDesc ** method, ClassDesc ** classInfo)
{
#ifdef USE_PUSHED_METHODDESC
	MethodDesc *md;
	md = (MethodDesc *) ebp[-1];
#ifdef USE_QMAGIC
	if (md == NULL || md->classDesc == NULL || md->classDesc->magic != MAGIC_CLASSDESC) {
		printf("FAULT: methodDesc not found on stack! 0x%lx  0x%lx md=0x%lx\n", ebp, ebp[-1]);
		return -1;
	}
#endif
	*method = md;
	*classInfo = md->classDesc;
#else
	sys_panic("Compile with -DUSE_PUSHED_METHODDESC to use findMethodAtFramePointer.\n");
#endif
	return 0;
}

int findByteCodePosition(MethodDesc * method, u1_t * addr)
{
	ByteCodeDesc *table;
	int i, offset, bPos;

	table = method->bytecodeTable;
	offset = addr - (u1_t *) (method->code);

	bPos = -1;
	for (i = 0; i < method->numberOfByteCodes; i++) {
		if (table[i].start <= offset && table[i].end >= offset) {
			bPos = table[i].bytecodePos;
			break;
		}
	}

	return bPos;
}

int findMethodAtAddrInDomain(DomainDesc * domain, u1_t * addr, MethodDesc ** method, ClassDesc ** classInfo, jint * bytecodePos,
			     jint * lineNumber)
{
	int g, h, i, j, k, l, m;
	int ret = -1;
	SourceLineDesc *stable;
	if (addr == NULL)
		return -1;

	LOCK_DOMAINS;
#ifndef FASTER_METHOD_LOOKUP
	// printf("Looking for %p\n", addr);
	for (h = 0; h < domain->numberOfLibs; h++) {
		LibDesc *lib = domain->libs[h];
		Class *allClasses = lib->allClasses;
		for (i = 0; i < lib->numberOfClasses; i++) {
			ClassDesc *classDesc = allClasses[i].classDesc;
			for (j = 0; j < classDesc->numberOfMethods; j++) {
				//printf("      %p %p %s %s\n", classDesc->methods[j].code, classDesc->methods[j].code + classDesc->methods[j].numberOfCodeBytes, classDesc->name, classDesc->methods[j].name);
				if ((u1_t *) (classDesc->methods[j].code) <= addr
				    && (u1_t *) (classDesc->methods[j].code) + classDesc->methods[j].numberOfCodeBytes > addr) {
					*method = &classDesc->methods[j];
					*classInfo = classDesc;

					*bytecodePos = findByteCodePosition(&(classDesc->methods[j]), addr);

					*lineNumber = -1;
					//      printf("BC=%ld NS=%d\n", *bytecodePos, classDesc->methods[j].numberOfSourceLines);
					if (*bytecodePos != -1) {
						/* find source code line */
						stable = classDesc->methods[j].sourceLineTable;
						for (k = 0; k < classDesc->methods[j].numberOfSourceLines - 1; k++) {
							//printf("LINE: BC=%ld   STARTBC=%ld\n", *bytecodePos, stable[k].startBytecode);
							if ((stable[k].startBytecode <= *bytecodePos)
							    && (stable[k + 1].startBytecode > *bytecodePos)) {
								*lineNumber = stable[k].lineNumber;
								break;
							}
						}
					}
					ret = 0;
					goto finish;
				}
			}
		}
	}
#else				/* FASTER_METHOD_LOOKUP */
	for (h = 0; h < domain->numberOfLibs; h++) {
		LibDesc *lib = domain->libs[h];
		Class *allClasses = lib->allClasses;
		ClassDesc *classDesc;
		u1_t *lower, *upper;
		// neccessary?
		if (lib->numberOfClasses <= 0) {
			printf("problem in domain %d\n", domain->id);
			sys_panic("empty lib %p %d", domain, h);
		}
		if (lib->hasNoImplementations)
			continue;
		for (i = 0; i < lib->numberOfClasses; i++) {
			classDesc = lib->allClasses[i].classDesc;
			if ((classDesc->classType & CLASSTYPE_INTERFACE) == CLASSTYPE_INTERFACE)
				continue;
			for (j = 0; j < classDesc->numberOfMethods; j++) {
				lower = (u1_t *) (classDesc->methods[j].code);
				if (lower)
					goto low_class;
			}
		}
		if (i == lib->numberOfClasses) {
			lib->hasNoImplementations = JNI_TRUE;
			continue;
		}
	      low_class:
		for (g = lib->numberOfClasses - 1; g >= i; g--) {
			classDesc = lib->allClasses[g].classDesc;
			if ((classDesc->classType & CLASSTYPE_INTERFACE) == CLASSTYPE_INTERFACE)
				continue;
			for (j = classDesc->numberOfMethods - 1; j >= 0; j--) {
				upper = ((u1_t *) classDesc->methods[j].code) + classDesc->methods[j].numberOfCodeBytes;
				if (upper)
					goto upp_class;
			}
		}
	      upp_class:
		/*
		   printf("lib: %2d: %p<=%p<%p: %d (%d,%d)\n", h, lower, addr, upper, 
		   ((addr < lower) || (addr >= upper)), i, j);
		 */
		if ((addr < lower) || (addr >= upper))
			continue;
		for (; i <= g; i++) {
			classDesc = allClasses[i].classDesc;
			if ((classDesc->classType & CLASSTYPE_INTERFACE) == CLASSTYPE_INTERFACE)
				continue;
			for (l = 0; l < classDesc->numberOfMethods; l++) {
				lower = (u1_t *) (classDesc->methods[l].code);
				if (lower)
					break;
			}
			for (m = classDesc->numberOfMethods - 1; m >= l; m--) {
				upper = ((u1_t *) classDesc->methods[m].code) + classDesc->methods[m].numberOfCodeBytes;
				if (upper)
					break;
			}
			if ((addr < lower) || (addr >= upper))
				continue;
			l = 0;
			m = classDesc->numberOfMethods - 1;
			for (j = l; j <= m; j++) {
				//printf("      %p %p %s %s\n", classDesc->methods[j].code, classDesc->methods[j].code + classDesc->methods[j].numberOfCodeBytes, classDesc->name, classDesc->methods[j].name);
				if (classDesc->methods[j].code <= addr
				    && classDesc->methods[j].code + classDesc->methods[j].numberOfCodeBytes > addr) {
					*method = &classDesc->methods[j];
					*classInfo = classDesc;

					*bytecodePos = findByteCodePosition(&(classDesc->methods[j]), addr);

					*lineNumber = -1;
					if (*bytecodePos != -1) {
						/* find source code line */
						stable = classDesc->methods[j].sourceLineTable;
						for (k = 0; k < classDesc->methods[j].numberOfSourceLines - 1; k++) {
							/*printf("LINE: BC=%ld   STARTBC=%ld\n", *bytecodePos, stable[k].startBytecode); */
							if ((stable[k].startBytecode <= *bytecodePos)
							    && (stable[k + 1].startBytecode > *bytecodePos)) {
								*lineNumber = stable[k].lineNumber;
								break;
							}
						}
					}
					ret = 0;
					goto finish;
				}
			}
		}
	}
#endif				/* FASTER_METHOD_LOOKUP */

      finish:
	UNLOCK_DOMAINS;

	return ret;
}


/* -1 failure, 
   0 success */
int findMethodAtAddr(u1_t * addr, MethodDesc ** method, ClassDesc ** classInfo, jint * bytecodePos, jint * lineNumber)
{
	int g, h, i, j, k;
	jint offset;
	ByteCodeDesc *table;
	SourceLineDesc *stable;

	DomainDesc *domain;
	char *mem;
	int ret = -1;

	if (addr == NULL)
		return -1;

	LOCK_DOMAINS;

	for (mem = domainMem; mem < domainMemBorder; mem += DOMAINMEM_SIZEBYTES) {
		domain = (DomainDesc *) (mem + DOMAINDESC_OFFSETBYTES);
		if (findMethodAtAddrInDomain(domain, addr, method, classInfo, bytecodePos, lineNumber) == 0) {
			ret = 0;
			goto finish;
		}
	}

      finish:
	UNLOCK_DOMAINS;
	return ret;
}

void check_threads()
{
	DomainDesc *domain;
	char *mem;

	LOCK_DOMAINS;

	for (mem = domainMem; mem < domainMemBorder; mem += DOMAINMEM_SIZEBYTES) {
		domain = (DomainDesc *) (mem + DOMAINDESC_OFFSETBYTES);
		checkThreadsOfDomain(domain);
	}

	UNLOCK_DOMAINS;
}

int findProxyCode(DomainDesc * domain, char *addr, char **method, char **sig, ClassDesc ** classInfo)
{
	DomainDesc *domain;
	char *mem;
	int ret = -1;

	LOCK_DOMAINS;

	for (mem = domainMem; mem < domainMemBorder; mem += DOMAINMEM_SIZEBYTES) {
		domain = (DomainDesc *) (mem + DOMAINDESC_OFFSETBYTES);
		if (findProxyCodeInDomain(domain, addr, method, sig, classInfo) == 0) {
			ret = 0;
			break;
		}
	}

      finish:
	UNLOCK_DOMAINS;
	return ret;
}

DomainDesc *findDomain(u4_t id)
{
	DomainDesc *domain;
	char *mem;
	DomainDesc *ret = NULL;

	LOCK_DOMAINS;

	for (mem = domainMem; mem < domainMemBorder; mem += DOMAINMEM_SIZEBYTES) {
		domain = (DomainDesc *) (mem + DOMAINDESC_OFFSETBYTES);
		if (domain->state == DOMAIN_STATE_FREE)
			continue;
		if (domain->id == id) {
			ret = domain;
			break;
		}
	}

	UNLOCK_DOMAINS;
	return ret;
}

DomainDesc *findDomainByName(char *name)
{				/* HACK!!!! */
	DomainDesc *domain;
	char *mem;
	DomainDesc *ret = NULL;

	LOCK_DOMAINS;

	for (mem = domainMem; mem < domainMemBorder; mem += DOMAINMEM_SIZEBYTES) {
		domain = (DomainDesc *) (mem + DOMAINDESC_OFFSETBYTES);
		if (domain->state == DOMAIN_STATE_FREE)
			continue;
		if (domain->domainName && strcmp(domain->domainName, name) == 0) {
			ret = domain;
			break;
		}
	}

	UNLOCK_DOMAINS;
	return ret;
}

static u4_t in_foreach = 0;
static u4_t runnable_in_runq_check = 1;

void foreachDomain(domain_f func)
{
	DomainDesc *domain;
	char *mem;

	if (!domainsys_inited) {
		printf("DOMAINSYS NOT INITIED\n");
		return;
	}

	LOCK_DOMAINS;

	for (mem = domainMem; mem < domainMemBorder; mem += DOMAINMEM_SIZEBYTES) {
		domain = (DomainDesc *) (mem + DOMAINDESC_OFFSETBYTES);
		if (domain->state != DOMAIN_STATE_ACTIVE)
			continue;
		func(domain);
	}

	UNLOCK_DOMAINS;
}

#ifndef PRODUCTION
#ifdef CHECK_RUNNABLE_IN_RUNQ
void foreachDomainRUNQ(domain_f func)
{
	DomainDesc *domain;
	char *mem;

	if (!domainsys_inited) {	/*printf("DOMAINSYS NOT INITIED\n"); */
		return;
	}
	if (!runnable_in_runq_check)
		return;
	if (!cas(&in_foreach, 0, 1))
		return;		/* foreachDomain is called in DISABLE_IRQ */

	LOCK_DOMAINS;

	for (mem = domainMem; mem < domainMemBorder; mem += DOMAINMEM_SIZEBYTES) {
		domain = (DomainDesc *) (mem + DOMAINDESC_OFFSETBYTES);
		if (domain->state != DOMAIN_STATE_ACTIVE)
			continue;
		func(domain);
	}

	UNLOCK_DOMAINS;
	in_foreach = 0;
}
#endif				/* CHECK_RUNNABLE_IN_RUNQ */

#endif				/* PRODUCTION */

#ifdef MONITOR
void foreachDomain1(domain1_f func, void *arg)
{
	DomainDesc *domain;
	char *mem;

	LOCK_DOMAINS;

	for (mem = domainMem; mem < domainMemBorder; mem += DOMAINMEM_SIZEBYTES) {
		domain = (DomainDesc *) (mem + DOMAINDESC_OFFSETBYTES);
		if (domain->state == DOMAIN_STATE_FREE)
			continue;
		func(domain, arg);
	}

	UNLOCK_DOMAINS;
}
#endif

 /* in case foreachDomain was terminated by panic */
void clean_domainsys()
{
	runnable_in_runq_check = 0;
	in_foreach = 0;
}

/* BE CAREFUL WHEN CALLING THESE FUNCTIONS.
 * YOU DID NOT OBTAIN A LOCK.
 */
jint getNumberOfDomains()
{
	return numberOfDomains;
}

void domain_panic(DomainDesc * domain, char *msg, ...)
{
	u4_t *base;
	va_list args;

	printf("DOMAIN PANIC ");
	if (domain != NULL)
		printf("in domain %d", domain->id);
	printf("\n");
	va_start(args, msg);
	vprintf(msg, args);
	va_end(args);
	printf("\n");

	base = &domain - 2;
	printStackTrace("DOMAINPANIC ", curthr(), base);

	terminateDomain(domain);
}

/* only a thread outside the domain can terminate the domain */
#ifdef JAVASCHEDULER
void terminateDomain(DomainDesc * domain)
{
	sys_panic("termination of domains not (yet) supported by SCHEDULER");
}
#else
void terminateDomain(DomainDesc * domain)
{
	u4_t index, j, size;

	ThreadDesc *t;

#ifdef SMP
	sys_panic("terminateDomain not supported for SMP");
#endif
	DISABLE_IRQ;
#ifdef DEBUG
	printf("TERMINATE: %d\n", domain->id);

	foreachDomain(check_threadindomain);
#endif	 /*DEBUG*/
	    ASSERT(curdom() != domain);

	domain->state = DOMAIN_STATE_TERMINATING;

	/* deactivate services */
	LOCK_SERVICETABLE;
	for (index = 0; index < MAX_SERVICES; index++) {
		if (domain->services[index] != SERVICE_ENTRY_FREE) {
			domain->services[index] = SERVICE_ENTRY_CHANGING;
			break;
		}
	}
	UNLOCK_SERVICETABLE;

	/* stop all threads */
	/* TODO: on an SMP system threads may currently run on another processor. stop them. */

	/* terminate all pending portal calls */
	/* and return from all running service executions with an exception */

	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		if (t->blockedInDomain != NULL) {
			DEPDesc *svc = t->blockedInDomain->services[t->blockedInServiceIndex];
			if (t->state == STATE_PORTAL_WAIT_FOR_RCV) {
				//printf("termthread waiting for recv -> unqueue\n");
				portal_remove_sender(svc, t);
			} else if (t->state == STATE_PORTAL_WAIT_FOR_RET) {
				//printf("termthread executing svc -> abort call\n");
				portal_abort_current_call(svc, t);
			} else {
				sys_panic("terminate with pending call not yet supported");
			}
		}
		if (t->mostRecentlyCalledBy == (ThreadDesc *) 0xffffffff) {
			/* called by IRQ */
			sys_panic("this should not happen on a uniprocessor");
		} else if (t->mostRecentlyCalledBy != NULL) {
			/* called by domain */
			/* throw exception */
			ThreadDesc *source = t->mostRecentlyCalledBy;
			source->portalReturn = THROW_DomainTerminatedException;
			source->portalReturnType = PORTAL_RETURN_TYPE_EXCEPTION;
			Sched_portal_unblock_sender(source);
		}
	}

#ifndef NEW_SCHED
	sys_panic("");		//        Sched_DomainTerminated(domain);
#else
	Sched_domainLeave(domain);
#endif
	/* free all TCBs and stacks */
	for (t = domain->threads; t != NULL;) {
		ThreadDesc *tnext = t->nextInDomain;
		//printf("TERMTHREAD %p\n", t);
		freeThreadMem(t);
		t = tnext;
	}

	/* free libmem */
	//printf("free libmem\n");
	//jxfree(domain->libs,domain->libMemSize); // libmem now in code area

	/* free unshared code segments */
	for (j = 0; j < domain->cur_code + 1; j++) {
		size = (char *) (domain->codeBorder[j]) - (char *) (domain->code[j]);
		//printf("free unshared code\n");
		//printf("Chunksize domain %d: %d\n", domain->id, domain->code_bytes);
		jxfree(domain->code[j], domain->code_bytes /*size */ MEMTYPE_CODE);
		domain->code[j] = domain->codeBorder[j] = domain->codeTop[j] = NULL;
	}

	gc_done(domain);

	jxfree(domain->scratchMem, domain->scratchMemSize MEMTYPE_OTHER);

	domain->state = DOMAIN_STATE_FREE;	/* domain control block can be reused */
	numberOfDomains--;


#ifdef DEBUG
	foreachDomain(check_threadindomain);
#endif	 /*DEBUG*/
	    RESTORE_IRQ;
}
#endif				/* JAVASCHEDULER */




void freezeDomain(DomainDesc * domain)
{
	u4_t index, j, size;

	ThreadDesc *t;

	sys_panic("");
	// printf("TERMINATE: %s\n", domain->domainName);

#ifdef SMP
	sys_panic("freezeDomain not supported for SMP");
#endif
	DISABLE_IRQ;

	ASSERT(curdom() != domain);

	domain->state = DOMAIN_STATE_FREEZING;

	/* stop all threads */
	/* TODO: on an SMP system threads may currently run on another processor. stop them. */

	/* stop all pending portal calls */
	/* and return from all running service executions with an exception */
	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		if (t->blockedInDomain != NULL
		    && (t->state == STATE_PORTAL_WAIT_FOR_RCV || t->state == STATE_PORTAL_WAIT_FOR_RET)) {
			/* Currently waiting for service of domain t->blockedInDomain, t->blockedInServiceIndex */
		}
		if (t->mostRecentlyCalledBy == (ThreadDesc *) 0xffffffff) {
			/* called by IRQ */
			sys_panic("this should not happen on a uniprocessor");
		} else if (t->mostRecentlyCalledBy != NULL) {
			/* called by domain */
			/* suspend call == do nothing */
		}
	}

	Sched_domainLeave(domain);

	domain->state = DOMAIN_STATE_FROZEN;

	RESTORE_IRQ;
}


void thawDomain(DomainDesc * domain)
{
	sys_panic("");
}



#ifdef DEBUG

void check_threadindomain(DomainDesc * domain)
{
	ThreadDesc *t;

	for (t = domain->threads; t != NULL; t = t->nextInDomain) {
		if (t->domain != domain) {
			printf("THREAD %d.%d (%p) OF DOMAIN %s INCONSISTENT\n", t->domain->id, t->id, t, t->domain->domainName);
			sys_panic("NOTINDOMAIN");
		}
	}
}

#endif				/* DEBUG */
