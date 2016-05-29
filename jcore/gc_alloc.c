/********************************************************************************
 * Garbage collector object allocation
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#include "all.h"

#include "gc_impl.h"

Proxy *allocProxyInDomain(DomainDesc * domain, ClassDesc * c, DomainDesc * targetDomain, u4_t targetDomainID, u4_t depIndex)
{
	ObjectHandle handle;
	Proxy *proxy;
	jint objSize = OBJSIZE_PORTAL;
#if 0
#ifdef DEBUG
	if (domain != curdom() && curdom() != domainZero /* mem proxy */ ) {
		printf("domain %d allocs in %d: \n", curdom()->id, domain->id);
		if (c != NULL)
			printf(" class=%s\n", c->name);
	}
#endif
#endif
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_PORTAL);
	proxy = (Proxy *) unregisterObject(domain, handle);
	if (c != NULL)
		proxy->vtable = c->proxyVtable;
	else
		proxy->vtable = NULL;
	proxy->targetDomain = targetDomain;
	proxy->targetDomainID = targetDomainID;
	proxy->index = depIndex;
	if (targetDomain && (targetDomain->id == targetDomainID)) {
		/* valid portal */
		service_incRefcount(targetDomain->services[depIndex]);
	}
#ifdef PROFILE_AGING
	paProxy((jint *) proxy, c, targetDomain, depIndex);
#endif
	return proxy;
}


AtomicVariableProxy *allocAtomicVariableProxyInDomain(DomainDesc * domain, ClassDesc * c)
{
	ObjectHandle handle;
	AtomicVariableProxy *obj;
	jint objSize = OBJSIZE_ATOMVAR;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_ATOMVAR);
	obj = (AtomicVariableProxy *) unregisterObject(domain, handle);
	obj->vtable = c->vtable;
	obj->value = NULL;
	obj->blockedThread = NULL;
	obj->listMode = 0;
#ifdef PROFILE_AGING
	paAVP((jint *) obj, c);
#endif
	return obj;
}

CASProxy *allocCASProxyInDomain(DomainDesc * domain, ClassDesc * c, u4_t index)
{
	ObjectHandle handle;
	CASProxy *obj;
	jint objSize = OBJSIZE_CAS;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_CAS);
	obj = (CASProxy *) unregisterObject(domain, handle);
	obj->vtable = c->vtable;
	obj->index = index;
	return obj;
}

extern ClassDesc *vmobjectClass;
VMObjectProxy *allocVMObjectProxyInDomain(DomainDesc * domain)
{
	ObjectHandle handle;
	VMObjectProxy *obj;
	jint objSize = OBJSIZE_VMOBJECT;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_VMOBJECT);
	obj = (VMObjectProxy *) unregisterObject(domain, handle);
	obj->vtable = vmobjectClass->vtable;
	obj->domain = NULL;
	obj->domain_id = 0;
	obj->epoch = 0;
	obj->type = 0;
	obj->obj = NULL;
	obj->subObjectIndex = 0;
	return (VMObjectProxy *) obj;
}

CredentialProxy *allocCredentialProxyInDomain(DomainDesc * domain, ClassDesc * c, u4_t signerDomainID)
{
	ObjectHandle handle;
	CredentialProxy *obj;
	jint objSize = OBJSIZE_CREDENTIAL;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_CREDENTIAL);
	obj = (CredentialProxy *) unregisterObject(domain, handle);
	obj->vtable = c->vtable;
	obj->value = NULL;
	obj->signerDomainID = signerDomainID;
#ifdef PROFILE_AGING
	// ...
#endif
	return obj;
}

extern ClassDesc *domainClass;

DomainProxy *allocDomainProxyInDomain(DomainDesc * domain, DomainDesc * domainValue, u4_t domainID)
{
	ObjectHandle handle;
	DomainProxy *obj;
	ClassDesc *c = domainClass;
	jint objSize = OBJSIZE_DOMAIN;
	ASSERTCLI;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_DOMAIN);
	obj = (DomainProxy *) unregisterObject(domain, handle);
	obj->vtable = c->vtable;
	obj->domain = domainValue;
	obj->domainID = domainID;
#ifdef PROFILE_AGING
	// ...
#endif
	return obj;
}

ThreadDescProxy *allocThreadDescProxyInDomain(DomainDesc * domain, ClassDesc * c)
{
	ObjectHandle handle;
	ObjectDesc *obj;
	jint objSize = OBJSIZE_THREADDESCPROXY;
	ASSERTCLI;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_CPUSTATE);
	obj = (ObjectDesc *) unregisterObject(domain, handle);
	if (c != NULL)
		obj->vtable = c->vtable;
	else
		obj->vtable = NULL;	/* bootstrap of DomainZero */

#ifdef PROFILE_AGING
	// ...
#endif
	return obj;
}


ThreadDescForeignProxy *allocThreadDescForeignProxyInDomain(DomainDesc * domain, ThreadDescProxy * src)
{
	ObjectHandle handle;
	ThreadDescForeignProxy *obj;
	ClassDesc *c = obj2ClassDesc(src);
	jint objSize = OBJSIZE_FOREIGN_THREADDESC;
	ASSERTCLI;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_FOREIGN_CPUSTATE);
	obj = (ObjectDesc *) unregisterObject(domain, handle);
	if (c != NULL)
		obj->vtable = c->vtable;
	else
		obj->vtable = NULL;	/* bootstrap of DomainZero */

	obj->thread = &(src->desc);
	obj->threadID = src->desc.id;
	obj->gcEpoch = src->desc.domain->gc.epoch;
	obj->domain = allocDomainProxyInDomain(domain, src->desc.domain, src->desc.domain->id);
	// ... domain gc epoch id

#ifdef PROFILE_AGING
	// ...
#endif
	return obj;
}

#ifdef STACK_ON_HEAP
StackProxy *allocStackInDomain(DomainDesc * domain, ClassDesc * c, u4_t stacksize)
{
	ObjectHandle handle;
	StackProxy *obj;
	jint objSize = OBJSIZE_STACK(stacksize);
	ASSERTCLI;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_STACK);
	obj = (StackProxy *) unregisterObject(domain, handle);
	obj->size = stacksize;
	if (c != NULL)
		obj->vtable = c->vtable;
	else
		obj->vtable = NULL;	/* bootstrap of DomainZero */

#ifdef PROFILE_AGING
	// ...
#endif
	return obj;
}
#endif

#ifdef ENABLE_MAPPING
MappedMemoryProxy *allocMappedMemoryProxyInDomain(DomainDesc * domain, char *mem, ClassDesc * cl)
{
	ObjectHandle handle;
	MappedMemoryProxy *obj;
	jint objSize = OBJSIZE_MAPPED_MEMORY;
	ASSERTCLI;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_MAPPED_MEMORY);
	obj = (ObjectDesc *) unregisterObject(domain, handle);
	if (cl != NULL)
		obj->vtable = cl->vtable;
	else
		obj->vtable = NULL;	/* bootstrap of DomainZero */

	obj->mem = mem;

#ifdef PROFILE_AGING
	// ...
#endif
	return obj;
}
#endif				/* ENABLE_MAPPING */

#if defined(PORTAL_INTERCEPTOR) || defined(PORTAL_TRANSFER_INTERCEPTOR)
extern ClassDesc *interceptInboundInfoClass;
extern ClassDesc *interceptPortalInfoClass;
InterceptInboundInfoProxy *allocInterceptInboundInfoProxyInDomain(DomainDesc * domain)
{
	ObjectHandle handle;
	InterceptInboundInfoProxy *obj;
	jint objSize = OBJSIZE_INTERCEPTINBOUNDINFO;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_INTERCEPTINBOUNDINFO);
	obj = (InterceptInboundInfoProxy *) unregisterObject(domain, handle);
	obj->vtable = interceptInboundInfoClass->vtable;
	obj->source = NULL;
	obj->target = NULL;
	obj->method = NULL;
	obj->obj = NULL;
	obj->paramlist = NULL;
	obj->index = 1;
	return obj;
}

InterceptPortalInfoProxy *allocInterceptPortalInfoProxyInDomain(DomainDesc * domain)
{
	ObjectHandle handle;
	InterceptPortalInfoProxy *obj;
	jint objSize = OBJSIZE_INTERCEPTPORTALINFO;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_INTERCEPTPORTALINFO);
	obj = (InterceptPortalInfoProxy *) unregisterObject(domain, handle);
	obj->vtable = interceptPortalInfoClass->vtable;
	/*  obj->source=NULL;
	   obj->target = NULL;
	   obj->method = NULL;
	   obj->obj = NULL;
	   obj->paramlist = NULL;
	   obj->index = 1;
	 */
	obj->domain = NULL;
	obj->index = 0;
	return obj;
}

#endif

ObjectDesc *nonatomic_allocObjectInDomain(DomainDesc * domain, ClassDesc * c)
{
	ObjectDesc *obj;
	ObjectHandle handle;
	jint objSize;

	//ASSERT(domain->state == DOMAIN_STATE_ACTIVE);
	ASSERT(c != NULL);
	ASSERTCLASSDESC(c);

	objSize = OBJSIZE_OBJECT(c->instanceSize);

#if 0
#ifdef DEBUG
	// if (domain != domainZero) classDesc2Class(domain,c);

	if (domain != curdom() && curdom() != domainZero /* mem proxy */  && domain != domainZero /* string constants */ ) {
		printf("domain %d allocs in %d: class=%s\n", curdom()->id, domain->id, c->name);
	}
#endif
#endif

	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_OBJECT);
	obj = unregisterObject(domain, handle);
	obj->vtable = c->vtable;
#ifdef PROFILE_AGING
	paObj((jint *) obj, c);
#endif
	return obj;
}

DEPDesc *allocServiceDescInDomain(DomainDesc * domain)
{
	ObjectHandle handle;
	DEPDesc *dep;
	jint objSize = OBJSIZE_SERVICEDESC;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_SERVICE);
	dep = (DEPDesc *) unregisterObject(domain, handle);
	memset(dep, 0, sizeof(DEPDesc));
#ifdef USE_QMAGIC
	dep->magic = MAGIC_DEP;
#endif
#ifdef PROFILE_AGING
	paDEP((jint *) dep);
#endif
	return dep;
}

#ifdef NEW_PORTALCALL
ServiceThreadPool *allocServicePoolInDomain(DomainDesc * domain)
{
	ObjectHandle handle;
	ServiceThreadPool *dep;
	jint objSize = OBJSIZE_SERVICEPOOL;
	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_SERVICE_POOL);
	dep = (DEPDesc *) unregisterObject(domain, handle);
	memset(dep, 0, sizeof(ServiceThreadPool));
#ifdef USE_QMAGIC
	dep->magic = MAGIC_DEP;
#endif
#ifdef PROFILE_AGING
	paDEP((jint *) dep);
#endif
	return dep;
}
#endif

ClassDesc *cpuStateClass = NULL;
ClassDesc *stackClass = NULL;
ClassDesc *domainClass = NULL;
ClassDesc *cpuClass = NULL;

//FIXME jgbauman FLAGS
CPUDesc *specialAllocCPUDesc()
{
	CPUDesc *c;
	ObjectDesc *obj;

	u4_t *mem = (u4_t *) malloc_cpudesc(domainZero, OBJSIZE_CPUDESC * 4);
	c = (CPUDesc *) (mem + 2 + XMOFF);
	obj = CPUDesc2ObjectDesc(c);
	setObjFlags(obj, OBJFLAGS_EXTERNAL_CPUDESC);	/* flags */
#  ifdef USE_QMAGIC
	setObjMagic(obj, MAGIC_OBJECT);
#  endif
	if (cpuClass != NULL)
		obj->vtable = (u4_t) cpuClass->vtable;	/* vtable */
	else
		obj->vtable = (u4_t) NULL;	/* during bootstrap of DomainZero! */
#ifdef USE_QMAGIC
	c->magic = MAGIC_CPU;
#endif
	return c;
}


ObjectDesc *specialAllocObject(ClassDesc * c)
{
	return allocObjectInDomain(curdom(), c);
}

ArrayDesc *specialAllocArray(ClassDesc * elemClass0, jint size)
{
	//DomainDesc *domain;
	//ClassDesc *elemClass=elemClass0;
	//domain = curdom();
	ASSERTCLASSDESC(elemClass0);
	if (size < 0)
		exceptionHandler(THROW_RuntimeException);
	return allocArrayInDomain(curdom(), elemClass0, size);
#if 0
	if (domain->memoryManager == NULL) {
	} else {
		jint params[] = { domain->memoryManager, elemClass, size };
		/*printf("  Calling MemoryManager for domain %s\n", domain->domainName); */
		return send_portalcall(domain->memoryManager_allocArrayIndex, 3, params);
	}
#endif
}

ArrayDesc *vmSpecialAllocArray(ClassDesc * elemClass0, jint size)
{
	//u4_t *sp, *ebp, *eip;
	//    printf("ALLOC: %p\n", elemClass0);
	ASSERTCLASSDESC(elemClass0);
	//printf("ALLOCARRAY: %s\n", elemClass0->name);
	return (ArrayDesc *) specialAllocArray(elemClass0, size);
}


/*
 *
 * AllocMultiArray Hack !!! fixme !!!
 *
 * !!! recursiv and not gc save !!!
 *
 */

ArrayDesc *doAllocMultiArray(ClassDesc * elemClass, jint dim, jint * oprs);

ArrayDesc *vmSpecialAllocMultiArray(ClassDesc * elemClass, jint dim, jint sizes)
{
	ASSERTCLASSDESC(elemClass);
	return doAllocMultiArray(elemClass, dim, (jint *) (&sizes));
}

ArrayDesc *doAllocMultiArray(ClassDesc * arrayClass, jint dim, jint * oprs)
{
	ArrayDesc *array;
	//ClassDesc *c;
	ClassDesc *elemClass;
	int i;
	u4_t gcEpoch;
	DomainDesc *domain = curdom();

	CHECK_STACK_SIZE(elemClass, 16);
	elemClass = findClassOrPrimitive(domain, arrayClass->name + 1)->classDesc;

	if (dim == 1) {
		return (ArrayDesc *) specialAllocArray(elemClass, oprs[0]);
	}

      restart:
	gcEpoch = domain->gc.epoch;
	array = (ArrayDesc *) specialAllocArray(elemClass, oprs[0]);
	if (gcEpoch != domain->gc.epoch)
		goto restart;

	for (i = 0; i < oprs[0]; i++) {
		array->data[i] = (jint) doAllocMultiArray(elemClass, dim - 1, oprs + 1);
		if (gcEpoch != domain->gc.epoch)
			goto restart;
	}
	return array;
}

/*
 *
 * !!! end of AllocMultiArray Hack !!! 
 *
 */

u4_t *specialAllocStaticFields(DomainDesc * domain, int numberFields)
{
	return (u4_t *) malloc_staticfields(domain, numberFields);
}

ArrayDesc *allocByteArray(DomainDesc * domain, ClassDesc * elemClass, jint size)
{
	jint objSize;
	ObjectHandle handle;
	ArrayDesc *obj;
	ArrayClassDesc *arrayClass;
	char name[256];

	SAMPLEPOINT_FASTPATH;

	strcpy(name, "[");
	if (elemClass->classType == CLASSTYPE_CLASS || elemClass->classType == CLASSTYPE_INTERFACE) {
		strcat(name, "L");
	}
	strcat(name, elemClass->name);
	if (elemClass->classType == CLASSTYPE_CLASS || elemClass->classType == CLASSTYPE_INTERFACE) {
		strcat(name, ";");
	}

	if (elemClass->arrayClass == NULL) {
		arrayClass = findSharedArrayClassDescByElemClass(elemClass);
	} else {
		arrayClass = elemClass->arrayClass;
	}

#ifdef ALL_ARRAYS_32BIT
	objSize = OBJSIZE_ARRAY_32BIT(size);
#else
	if (ARRAY_8BIT(arrayClass)) {
		objSize = OBJSIZE_ARRAY_8BIT(size);
	} else if (ARRAY_16BIT(arrayClass)) {
		objSize = OBJSIZE_ARRAY_16BIT(size);
	} else {
		objSize = OBJSIZE_ARRAY_32BIT(size);
	}
#endif

	handle = gc_allocDataInDomain(domain, objSize, OBJFLAGS_ARRAY);
	obj = (ArrayDesc *) unregisterObject(domain, handle);
	obj->arrayClass = (ClassDesc *) arrayClass;
	obj->size = size;
	obj->vtable = arrayClass->vtable;
#ifdef PROFILE_AGING
	paArray((jint *) obj, elemClass, size);
#endif
	return obj;
}

ArrayDesc *allocArray(ClassDesc * elemClass, jint size)
{
	ASSERTCLASSDESC(elemClass);
	return allocArrayInDomain(curdom(), elemClass, size);
}

ArrayDesc *allocArrayInDomain(DomainDesc * domain, ClassDesc * elemClass, jint size)
{
	ASSERTCLASSDESC(elemClass);
	return allocByteArray(domain, elemClass, size);
}
