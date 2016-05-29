/********************************************************************************
 * Garbage collector
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifdef ENABLE_GC
#include "all.h"

#include "gc_common.h"
#include "gc_impl.h"
#include "gc_stack.h"
#include "gc_pgc.h"

//#define DBG_SCAN_HEAP2 1

/* FIXME prototypes */
/*
int eip_in_last_stackframe(u4_t eip);
ObjectHandle (*registerObject)(DomainDesc *domain, ObjectDesc *o);
extern unsigned char callnative_special_end [], callnative_special_portal_end [], callnative_static_end[], thread_exit_end[];void return_from_java0(ThreadDesc* next, ContextDesc* restore);
void return_from_java1(long param, ContextDesc* restore, ThreadDesc* next);
void return_from_java2(long param1, long param2, ThreadDesc* next, ContextDesc* restore);
extern unsigned char return_from_javaX_end [], never_return_end [];
void never_return (void);
jint cpuManager_receive(ObjectDesc *self, Proxy *portal);
extern unsigned char cpuManager_receive_end[];
*/
#if 0
u4_t getObjFlags(ObjectDesc * ref)
{
	u4_t *mem = (u4_t *) ref;

	ASSERT(ref != NULL);

	mem -= XMOFF;		/* magic */
	mem--;			/* flags */
	//  ASSERT(mem > 10000 && mem <0xfffff000);
	return *mem & FLAGS_MASK;
}

void setObjFlags(ObjectDesc * ref, u4_t flags)
{
	u4_t *mem = (u4_t *) ref;

	mem -= XMOFF;		/* magic */
	mem--;			/* flags */
	*mem = (*mem & ~FLAGS_MASK) | flags;
}
#endif
//FIXME for speedup
jboolean isRef(jbyte * map, int total, int num)
{
	FORBITMAP(map, total, {
		  if (index == num) return JNI_TRUE;}
		  , {
		  }
	);
	return JNI_FALSE;
}

u4_t gc_objSize2(ObjectDesc * obj, jint flags)
{
	ClassDesc *c;
	//jint flags;

	ASSERTOBJECT(obj);

	//flags = getObjFlags(obj);

	switch (flags & FLAGS_MASK) {
	case OBJFLAGS_ARRAY:{
			ArrayDesc *a = ((ArrayDesc *) obj);
#ifndef ALL_ARRAYS_32BIT
			c = a->arrayClass;
			ASSERTCLASSDESC(c);
			if (ARRAY_8BIT(c)) {
				return OBJSIZE_ARRAY_8BIT(a->size);
			} else if (ARRAY_16BIT(c)) {
				return OBJSIZE_ARRAY_16BIT(a->size);
			}
#endif
			return OBJSIZE_ARRAY_32BIT(a->size);
		}
	case OBJFLAGS_EXTERNAL_STRING:
	case OBJFLAGS_OBJECT:
		c = obj2ClassDesc(obj);
		ASSERTCLASSDESC(c);
		return OBJSIZE_OBJECT(c->instanceSize);
	case OBJFLAGS_PORTAL:
		return OBJSIZE_PORTAL;
	case OBJFLAGS_MEMORY:
		return OBJSIZE_MEMORY;
	case OBJFLAGS_SERVICE:
		return OBJSIZE_SERVICEDESC;
	case OBJFLAGS_SERVICE_POOL:
		return OBJSIZE_SERVICEPOOL;
	case OBJFLAGS_ATOMVAR:
		return OBJSIZE_ATOMVAR;
	case OBJFLAGS_CAS:
		return OBJSIZE_CAS;
	case OBJFLAGS_DOMAIN:
		return OBJSIZE_DOMAIN;
	case OBJFLAGS_CPUSTATE:
		return OBJSIZE_THREADDESCPROXY;
	default:
		printf("FLAGS: %lx\n", flags);
		dump_data(obj);
		sys_panic("WRONG HEAP DATA");
	}
}

char *get_flagname(u4_t flags)
{
	switch (flags) {
	case OBJFLAGS_ARRAY:
		return "OBJFLAGS_ARRAY";
	case OBJFLAGS_EXTERNAL_STRING:
		return "OBJFLAGS_EXTERNAL_STRING";
	case OBJFLAGS_FOREIGN_CPUSTATE:
		return "OBJFLAGS_FOREIGN_CPUSTATE";
	case OBJFLAGS_OBJECT:
		return "OBJFLAGS_OBJECT";
	case OBJFLAGS_PORTAL:
		return "OBJFLAGS_PORTAL";
	case OBJFLAGS_MEMORY:
		return "OBJFLAGS_MEMORY";
	case OBJFLAGS_SERVICE:
		return "OBJFLAGS_SERVICE";
	case OBJFLAGS_SERVICE_POOL:
		return "OBJFLAGS_SERVICE_POOL";
	case OBJFLAGS_ATOMVAR:
		return "OBJFLAGS_ATOMVAR";
	case OBJFLAGS_CAS:
		return "OBJFLAGS_CAS";
	case OBJFLAGS_DOMAIN:
		return "OBJFLAGS_DOMAIN";
	case OBJFLAGS_CPUSTATE:
		return "OBJFLAGS_CPUSTATE";
	case OBJFLAGS_STACK:
		return "OBJFLAGS_STACK";
	}
	return "UNKNOWN";
}

/*#define DBG_SCAN_HEAP 1*/

 /*
    * Scan heap
  */
void gc_walkContinuesBlock(DomainDesc * domain, u4_t * start, u4_t ** top, HandleObject_t handleObject,
			   HandleObject_t handleArray, HandleObject_t handlePortal, HandleObject_t handleMemory,
			   HandleObject_t handleService, HandleObject_t handleCAS, HandleObject_t handleAtomVar,
			   HandleObject_t handleDomainProxy, HandleObject_t handleCPUStateProxy, HandleObject_t handleServicePool,
			   HandleObject_t handleStackProxy)
{
	ObjectDesc *obj;
	u4_t *data;
	ClassDesc *c;
	jint flags;
	u4_t objSize = 0;

	//printf("walk: %p %p\n", start, *top);

	data = start;
	while (data < *top) {
		obj = ptr2ObjectDesc(data);
		flags = getObjFlags(obj);
		if ((flags & FORWARD_MASK) == GC_FORWARD) {
			flags = getObjFlags((ObjectDesc *) (flags & FORWARD_PTR_MASK))
			    | GC_FORWARD;
		}
#ifdef DBG_SCAN_HEAP
#ifdef USE_FMAGIC
		printf("rel=%p addr=%p flags=%p (%s) magic=%p\n", (char *) data - (char *) start, data + 2, flags & FLAGS_MASK,
		       get_flagname(flags & FLAGS_MASK), data[1]);
#else
		printf("addr=%p flags=%p (%s)\n", data + 1, flags & FLAGS_MASK, get_flagname(flags & FLAGS_MASK));
#endif
#endif
		switch (flags & FLAGS_MASK) {
		case OBJFLAGS_ARRAY:{
				objSize = gc_objSize2(obj, flags);
				if (handleArray)
					handleArray(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_EXTERNAL_STRING:
			ASSERT(domain == domainZero);
		case OBJFLAGS_FOREIGN_CPUSTATE:
		case OBJFLAGS_OBJECT:{
				c = obj2ClassDesc(obj);
				ASSERTCLASSDESC(c);
#ifdef DBG_SCAN_HEAP
				printf("%s\n", c->name);
#endif
				objSize = OBJSIZE_OBJECT(c->instanceSize);
				if (handleObject)
					handleObject(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_PORTAL:{
				objSize = OBJSIZE_PORTAL;
				if (handlePortal)
					handlePortal(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_MEMORY:{
				objSize = OBJSIZE_MEMORY;
				if (handleMemory)
					handleMemory(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_SERVICE:{
				objSize = OBJSIZE_SERVICEDESC;
				if (handleService)
					handleService(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_SERVICE_POOL:{
				objSize = OBJSIZE_SERVICEPOOL;
				if (handleServicePool)
					handleServicePool(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_ATOMVAR:{
				objSize = OBJSIZE_ATOMVAR;
				if (handleAtomVar)
					handleAtomVar(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_CAS:{
				objSize = OBJSIZE_CAS;
				if (handleCAS)
					handleCAS(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_DOMAIN:{
				objSize = OBJSIZE_DOMAIN;
				if (handleDomainProxy)
					handleDomainProxy(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_CPUSTATE:{
				objSize = OBJSIZE_THREADDESCPROXY;
				if (handleCPUStateProxy)
					handleCPUStateProxy(domain, obj, objSize, flags);
				break;
			}
#ifdef STACK_ON_HEAP
		case OBJFLAGS_STACK:{
				objSize = OBJSIZE_STACK(((StackProxy *) obj)->size);
				if (handleStackProxy)
					handleStackProxy(domain, obj, objSize, flags);
				break;
			}
#endif				/* STACK_ON_HEAP */
		default:
			printf("FLAGS: %lx\n", flags & FLAGS_MASK);
#ifdef DEBUG
			flags = getObjFlags(obj);
			if ((flags & FORWARD_MASK) == GC_FORWARD) {
				printf("Forward to %p\n", flags & FORWARD_PTR_MASK);
				flags = getObjFlags((ObjectDesc *) (flags & FORWARD_PTR_MASK));
			}
			dump_data(obj);
#endif
			sys_panic("WRONG HEAP DATA");
		}
		data += objSize;
	}
}

 /*
    * Scan heap
  */
void gc_walkContinuesBlock_Alt(DomainDesc * domain, u4_t * start, u4_t * top, HandleObject_t handleObject,
			       HandleObject_t handleArray, HandleObject_t handlePortal, HandleObject_t handleMemory,
			       HandleObject_t handleService, HandleObject_t handleCAS, HandleObject_t handleAtomVar,
			       HandleObject_t handleDomainProxy)
{
	ObjectDesc *obj;
	u4_t *data;
	ClassDesc *c;
	jint flags;
	u4_t objSize = 0;

	data = start;
	//FIXME
	IF_DBG_GC(printf("walk: %p %p %p\n", data, start, top));

	while (data < top) {
		obj = ptr2ObjectDesc(data);
		flags = getObjFlags(obj);

#ifdef DBG_SCAN_HEAP2
#ifdef USE_FMAGIC
		printf("addr=%p flags=%p magic=%p\n", data + 2, flags, data[1]);
#else
		printf("addr=%p flags=%p\n", data + 1, flags);
#endif
#endif
		switch (flags & FLAGS_MASK) {
		case OBJFLAGS_ARRAY:{
				objSize = gc_objSize2(obj, flags);
				if (handleArray)
					handleArray(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_EXTERNAL_STRING:
			ASSERT(domain == domainZero);
		case OBJFLAGS_OBJECT:{
				// FIXME (((*(((u4_t*)(obj))-1)==MAGIC_OBJECT) && (((ObjectDesc*)(obj))->vtable != NULL))
				c = *(ClassDesc **) (((ObjectDesc *) (obj))->vtable - 1);
				// : (printf("\"%s\", line %d: Assertion failed.\n", __FILE__, __LINE__), sys_panic(""), (ClassDesc*)NULL))
				// c=obj2ClassDesc(obj);
				ASSERTCLASSDESC(c);
				objSize = OBJSIZE_OBJECT(c->instanceSize);
				if (handleObject)
					handleObject(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_PORTAL:{
				objSize = OBJSIZE_PORTAL;
				if (handlePortal)
					handlePortal(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_MEMORY:{
				objSize = OBJSIZE_MEMORY;
				if (handleMemory)
					handleMemory(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_SERVICE:{
				objSize = OBJSIZE_SERVICEDESC;
				if (handleService)
					handleService(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_SERVICE_POOL:{
				objSize = OBJSIZE_SERVICEPOOL;
				// TODO handle servicepool
				break;
			}
		case OBJFLAGS_ATOMVAR:{
				objSize = OBJSIZE_ATOMVAR;
				if (handleAtomVar)
					handleAtomVar(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_CAS:{
				objSize = OBJSIZE_CAS;
				if (handleCAS)
					handleCAS(domain, obj, objSize, flags);
				break;
			}
		case OBJFLAGS_DOMAIN:{
				objSize = OBJSIZE_DOMAIN;
				if (handleDomainProxy)
					handleDomainProxy(domain, obj, objSize, flags);
				break;
			}
		default:
			printf("FLAGS: %lx\n", flags);
			dump_data(obj);
			sys_panic("WRONG HEAP DATA");
		}
		data += objSize;
	}
}

#endif				/* ENABLE_GC */
