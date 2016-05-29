/********************************************************************************
 * Base GC support
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#include "all.h"

#ifdef ENABLE_GC
#include "gc_org.h"
#include "gc_new.h"
#include "gc_compacting.h"


#include "gc_common.h"
#include "gc_memcpy.h"
#include "gc_move.h"
#include "gc_pa.h"
#include "gc_pgc.h"
#endif				/* ENABLE_GC */

/* FIXME 
#ifdef SMP
#include "spinlock.h"
static spinlock_t allocating_mem = SPIN_LOCK_UNLOCKED;
#endif
*/

#ifndef ENABLE_GC

/** THIS FUNCTION MUST NOT BE INTERRUPTED BY A GC */
ObjectHandle nonatomic_registerObject(DomainDesc * domain, ObjectDesc * obj)
{
	return (ObjectHandle) obj;
}

/** THIS FUNCTION MUST NOT BE INTERRUPTED BY A GC */
ObjectDesc *nonatomic_unregisterObject(DomainDesc * domain, ObjectHandle handle)
{
	return (ObjectDesc *) handle;
}

u4_t gc_mem()
{
	return 0;
}

void gc_init(DomainDesc * domain, u1_t * memu, jint gcinfo0, jint gcinfo1, jint gcinfo2, char *gcinfo3, jint gcinfo4, int gcImpl)
{
	sys_panic("no gc");
}

void gc_done(DomainDesc * domain)
{
	sys_panic("no gc");
}

/* see gc.h
u4_t gc_freeWords(DomainDesc *domain) {
  sys_panic("no gc");
}

u4_t gc_totalWords(DomainDesc *domain) {
  sys_panic("no gc");
}
*/

void gc_printInfo(DomainDesc * domain)
{
	sys_panic("no gc");
}

/*
ObjectHandle gc_allocDataInDomain(DomainDesc *domain, int objsize, u4_t flags) {
  sys_panic("no gc");
}
*/

//void gc_in(ObjectDesc *o, DomainDesc *domain) { }

jboolean isRef(jbyte * map, int total, int num)
{
	sys_panic("no gc");
}

// FIXME
void rswitches()
{
	sys_panic("no gc");
}

#ifdef DEBUG
jboolean gc_isValidHeapRef(DomainDesc * domain, ObjectDesc * ptr)
{
	sys_panic("no gc");
}
#endif

#else				/* ENABLE_GC */

#ifdef PROFILE_AGING

#ifdef PROFILE_AGING_CREATION_ONLY
jlong memTimeStep = 1024 * 1024 * 1024;
#else
//jlong memTimeStep = 13*1024;
jlong memTimeStep = 20 * 1024;
//jlong memTimeStep = 20*10000*1024;
#endif

#endif				/* PROFILE_AGING */

/** THIS FUNCTION MUST NOT BE INTERRUPTED BY A GC */
ObjectHandle nonatomic_registerObject(DomainDesc * domain, ObjectDesc * o)
{
	int i;
	/*
	   ASSERT(o != NULL);
	   ASSERTOBJECT(o);
	 */
	//  if (domain==NULL) asm volatile("movl $testf, %ebx; jmp *%ebx"); //for(;;);//extern_panic("domain==0");
	if (domain == NULL)
		extern_panic("domain==0");
	for (i = 0; i < MAX_REGISTERED; i++) {
		if (domain->gc.registeredObjects[i] == NULL) {
			domain->gc.registeredObjects[i] = o;
#ifdef DEBUG_HANDLE
#if 0
			domain->gc.registrationPoints[i] = (code_t) extern_getCaller(3);
#else
			domain->gc.registrationPoints[i] = (code_t) getCaller(3);
			domain->gc.registrationPoints2[i] = (code_t) getCaller(2);
			domain->gc.registrationPoints1[i] = (code_t) getCaller(1);
#endif
#endif				/* DEBUG */
			goto ok;
		}
	}

	/* error */
#ifdef DEBUG_HANDLE
	for (i = 0; i < MAX_REGISTERED; i++) {
		print_eip_info((char *) domain->gc.registrationPoints[i]);
		print_eip_info((char *) domain->gc.registrationPoints2[i]);
		print_eip_info((char *) domain->gc.registrationPoints1[i]);
		printf("\n");
	}
#endif				/* DEBUG */
	extern_panic("maximal number of registered objects reached for this domain");

      ok:
	return &(domain->gc.registeredObjects[i]);
}

/** THIS FUNCTION MUST NOT BE INTERRUPTED BY A GC */
ObjectDesc *nonatomic_unregisterObject(DomainDesc * domain, ObjectHandle o)
{
	ObjectDesc *obj;
	/*
	   ASSERT(o!=NULL);
	   ASSERT((char*)o >= (char*)domain->gc.registeredObjects && (char*)o < (char*)(domain->gc.registeredObjects+MAX_REGISTERED));
	 */
	if (domain == NULL)
		extern_panic("domain==0");
	if (o == NULL)
		extern_panic("o==0");
	obj = *o;
	*o = NULL;
	/*
	   ASSERTOBJECT(obj);
	 */
	return obj;
}


/*
#ifdef HEAP_STATISTICS
static void init_class_statistics(Class *c) {
    c->numberOfInstances=0;
}
static void print_class_statistics(Class *c) {
    printf("%05d   %s\n", (int)c->numberOfInstances, c->classDesc->name);
}
#endif
*/

/*
extern code_t extern_panic;
extern code_t extern_printf;
extern int_code_t extern_getCaller;
*/


#ifdef PROFILE_SAMPLE_HEAPUSAGE
jint n_heap_samples;
struct heapsample_s *heap_samples;

void profile_sample_heapusage_alloc(DomainDesc * domain, u4_t objsize)
{
	if (n_heap_samples < MAX_HEAP_SAMPLES) {
		jint i;
		u4_t *sp = (u4_t *) ((u4_t *) & domain - 2);
		//u4_t *ebp = (u4_t*)*sp++;
		u4_t *eip = (u4_t *) * sp++;
		//print_eip_info(eip);

		for (i = 0; i < 10; i++) {
			heap_samples[n_heap_samples].eip[i] = 0;
		}
		heap_samples[n_heap_samples].eip[0] = (char *) eip;
		// FIXME jgbauman: which c
		//heap_samples[n_heap_samples].cl = c;
		heap_samples[n_heap_samples].cl = NULL;
		heap_samples[n_heap_samples].size = objsize * 4;
		n_heap_samples++;
	}
}
#endif				/* PROFILE_HEAPUSAGE */

#ifdef PROFILE_HEAPUSAGE
/* 
 * callback for counting instances
 */
static void gc_countInstancesCB(DomainDesc * domain, ObjectDesc * obj, u4_t objsize, jint flags)
{
	ClassDesc *c;
	InstanceCounts_t *counts = (InstanceCounts_t *) domain->gc.data;

	switch (flags & FLAGS_MASK) {
	case OBJFLAGS_ARRAY:{
			counts->arrbytes += objsize * 4;
			((ArrayDesc *) obj)->arrayClass->n_arrayelements += objsize;
			//printf("ARR: %p %s bytes=%ld sum=%ld\n", ar->arrayClass, ar->arrayClass->name, objsize * 4, ar->arrayClass->n_arrayelements*4);
			break;
		}
	case OBJFLAGS_EXTERNAL_STRING:
		ASSERT(domain == domainZero);	/* only the heap of DomainZero contains these objects */
	case OBJFLAGS_OBJECT:{
			ASSERTOBJECT(obj);
			c = obj2ClassDesc(obj);
			ASSERTCLASSDESC(c);
			c->n_instances++;
			counts->objbytes += objsize * 4;
			break;
		}
	case OBJFLAGS_PORTAL:{
			counts->portalbytes += objsize * 4;
			break;
		}
	case OBJFLAGS_MEMORY:{
			counts->memproxybytes += objsize * 4;
			break;
		}
	case OBJFLAGS_ATOMVAR:{
			counts->atomvarbytes += objsize * 4;
			break;
		}
	case OBJFLAGS_CAS:{
			counts->casbytes += objsize * 4;
			break;
		}
	case OBJFLAGS_SERVICE:{
			counts->servicebytes += objsize * 4;
			break;
		}
	case OBJFLAGS_SERVICE_POOL:{
			counts->servicepoolbytes += objsize * 4;
			break;
		}
	case OBJFLAGS_STACK:
		counts->stackbytes += objsize * 4;
		break;
	case OBJFLAGS_CPUSTATE:
		counts->tcbbytes += objsize * 4;
		break;
	default:
		sys_panic("Unknown flag %d\n", flags & FLAGS_MASK);
	}
}

#ifndef GC_USE_ONLY_ONE
/*
 * Count number of instances of all classes
 */
void gc_countInstances(DomainDesc * domain, InstanceCounts_t * counts)
{
	counts->objbytes = 0;
	counts->arrbytes = 0;
	counts->portalbytes = 0;
	counts->memproxybytes = 0;
	counts->cpustatebytes = 0;
	counts->atomvarbytes = 0;
	counts->servicebytes = 0;
	counts->casbytes = 0;
	counts->tcbbytes = 0;
	counts->stackbytes = 0;

	domain->gc.data = (void *) counts;
	domain->gc.walkHeap(domain, gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB,
			    gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB,
			    gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB);
}
#else
void gc_countInstances(DomainDesc * domain, InstanceCounts_t * counts)
{
	counts->objbytes = 0;
	counts->arrbytes = 0;
	counts->portalbytes = 0;
	counts->memproxybytes = 0;
	counts->cpustatebytes = 0;
	counts->atomvarbytes = 0;
	counts->servicebytes = 0;
	counts->casbytes = 0;
/*
	gc_walkHeap (domain, gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB,
			gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB);
*/
	GC_FUNC_NAME(walkHeap) (domain, gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB,
				gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB, gc_countInstancesCB,
				gc_countInstancesCB);

}
#endif				/* GC_USE_ONLY_ONE */

#endif

#ifdef FIND_OBJECTS_BY_CLASS

#ifdef GC_USE_NEW
/*
 * Find objects given a class name
 *  TODO: use a GENERIC way to walk the heap (scan_heap,finalize_memory)
 */
static void gc_findOnHeapCB(DomainDesc * domain, ObjectDesc * obj, u4_t objSize, u4_t flags)
{
	ClassDesc *c = obj2ClassDesc(obj);
	if (strcmp(domain->gc.data, c->name) == 0) {
		u4_t k = 2;
		u4_t offs;
		printf("OBJECT: %p\n", obj);
#  ifdef USE_FMAGIC
		k++;
#endif
		offs = k;
		for (; k < objSize - 1; k++) {	/* -1, bacause OBJSIZE_OBJECT contains unused size field */
			int l;
			char *fieldname = "???";
			char *fieldtype = "???";
			for (l = 0; l < c->numberFields; l++) {
				if (c->fields[l].fieldOffset == k - offs) {
					fieldname = c->fields[l].fieldName;
					fieldtype = c->fields[l].fieldType;
					break;
				}
			}
			printf("%s %s  0x%lx\n", fieldtype, fieldname, obj->data[k - offs]);
		}
	}
}
#endif				/* GC_USE_NEW */

void gc_findOnHeap(DomainDesc * domain, char *classname)
{
#ifndef GC_USE_NEW
	GC_FUNC_NAME(findOnHeap) (domain, classname);
#else
	{
		int l;
		ClassDesc *c = findClass(domain, classname)->classDesc;
		for (l = 0; l < c->numberFields; l++) {
			printf("%2d %s\n", c->fields[l].fieldOffset, c->fields[l].fieldName);
		}
	}
	domain->gc.data = classname;
	GC_FUNC_NAME(walkHeap) (domain, gc_findOnHeapCB, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
#endif				/* GC_USE_NEW */
}
#endif				/* FIND_OBJECTS_BY_CLASS */


void gc_in(ObjectDesc * o, DomainDesc * domain)
{

	ASSERTCLI;

	if (domain->inhibitGCFlag)
		sys_panic("GC CURRENTLY INHIBITED IN DOMAIN %d", domain->id);

	//sys_panic("GC DISABLED");
#ifdef NOTICE_GC
	printf("\nGC IN  %ld (%s) started\n", domain->id, domain->domainName);
#endif

#ifdef MEASURE_GC_TIME
	{
		u8_t gcStartTime, gcEndTime;
		u4_t heapBytesBefore;
		gcStartTime = get_tsc();
		heapBytesBefore = gc_freeWords(domain) * 4;
#endif

		GC_FUNC_NAME(gc) (domain);

#ifdef MEASURE_GC_TIME
		gcEndTime = get_tsc();
		domain->gc.gcTime += CYCL2MICROS(gcEndTime - gcStartTime);
		domain->gc.gcRuns++;
		domain->gc.gcBytesCollected += gc_freeWords(domain) * 4 - heapBytesBefore;
	}
#endif

	/* start a new GC epoch for this domain */
	domain->gc.epoch++;

#ifdef NOTICE_GC
	printf("\nGC IN  %ld finished\n", domain->id);
#ifdef VERBOSE_GC
	gc_printInfo(domain);
#endif				/* VERBOSE_GC */
#endif
}

void gc_printInfo(DomainDesc * domain)
{
	printf("   GC Info for %s (%3ld)\n", domain->domainName, domain->id);
	GC_FUNC_NAME(printInfo) (domain);
#ifdef MEASURE_GC_TIME
	printf("      Runs: %3ld, Time: %10ld, Collected Bytes: %ld\n", domain->gc.gcRuns, (u4_t) domain->gc.gcTime,
	       (u4_t) domain->gc.gcBytesCollected);
#endif
}

#ifdef GC_USE_NEW
static void gc_zero_panic(DomainDesc * domain)
{
	sys_panic("gc_zero_panic called");
}
#endif				/* GC_USE_NEW */

u4_t gc_mem()
{
	return MAX_REGISTERED * sizeof(ObjectDesc *)
#ifdef DEBUG_HANDLE
	    + MAX_REGISTERED * sizeof(code_t)
#endif
	    ;
}

void gc_zero_init(DomainDesc * domain)
{
#ifndef GC_USE_ONLY_ONE
	domain->gc.allocDataInDomain = (ObjectHandle(*)(DomainDesc *, int, u4_t)) gc_zero_panic;
	domain->gc.gc = (void (*)(DomainDesc *)) gc_zero_panic;
	domain->gc.done = (void (*)(DomainDesc *)) gc_zero_panic;
	domain->gc.freeWords = (u4_t(*)(DomainDesc *)) gc_zero_panic;
	domain->gc.totalWords = (u4_t(*)(DomainDesc *)) gc_zero_panic;
	domain->gc.printInfo = (void (*)(DomainDesc *)) gc_zero_panic;
	domain->gc.finalizeMemory = (void (*)(DomainDesc *)) gc_zero_panic;
	domain->gc.finalizePortals = (void (*)(DomainDesc *)) gc_zero_panic;
	domain->gc.isInHeap = (jboolean(*)(DomainDesc *, ObjectDesc *)) gc_zero_panic;
	domain->gc.walkHeap = (void (*)
			       (DomainDesc *, HandleObject_t, HandleObject_t, HandleObject_t, HandleObject_t, HandleObject_t,
				HandleObject_t, HandleObject_t)) gc_zero_panic;
#endif				/* GC_USE_ONLY_ONE */
}

void gc_init(DomainDesc * domain, u1_t * mem, jint gcinfo0, jint gcinfo1, jint gcinfo2, char *gcinfo3, jint gcinfo4, int gcImpl)
{
#ifdef PROFILE_AGING
	domain->gc.memTime = 0;
#endif
	domain->gc.registeredObjects = (ObjectDesc **) mem;
	memset(domain->gc.registeredObjects, 0, MAX_REGISTERED * sizeof(ObjectDesc *));
	mem += MAX_REGISTERED * sizeof(ObjectDesc *);

#ifdef DEBUG_HANDLE
	domain->gc.registrationPoints = (ObjectDesc **) jxmalloc(MAX_REGISTERED * 4);
	domain->gc.registrationPoints1 = (ObjectDesc **) jxmalloc(MAX_REGISTERED * 4);
	domain->gc.registrationPoints2 = (ObjectDesc **) jxmalloc(MAX_REGISTERED * 4);
	memset(domain->gc.registrationPoints, 0, MAX_REGISTERED * sizeof(code_t));
	memset(domain->gc.registrationPoints1, 0, MAX_REGISTERED * sizeof(code_t));
	memset(domain->gc.registrationPoints2, 0, MAX_REGISTERED * sizeof(code_t));
	mem += MAX_REGISTERED * sizeof(code_t);
#endif				/* DEBUG */

	//domain->gc.numberOfRegistered = 0;

	// FIXME: just for better error messages, could be removed
	gc_zero_init(domain);

#ifdef GC_USE_NEW
	switch (gcImpl) {
#ifdef GC_NEW_IMPL
	case GC_IMPLEMENTATION_NEW:
		gc_new_init(domain, gcinfo0);
		break;
#endif
#ifdef GC_COMPACTING_IMPL
	case GC_IMPLEMENTATION_COMPACTING:
		gc_compacting_init(domain, gcinfo0);
		break;
#endif
#ifdef GC_BITMAP_IMPL
	case GC_IMPLEMENTATION_BITMAP:
		gc_bitmap_init(domain, gcinfo0);
		break;
#endif
#ifdef GC_CHUNKED_IMPL
	case GC_IMPLEMENTATION_CHUNKED:
		gc_chunked_init(domain, gcinfo0, gcinfo1, gcinfo2, gcinfo3, gcinfo4);
		break;
#endif
	default:
		printf("GCImpl=%d\n", gcImpl);
		exceptionHandlerMsg(THROW_RuntimeException, "unknown gc implementation");
	}
#else				/* GC_USE_NEW */
	gc_org_init(domain, gcinfo0);
#endif				/* GC_NEW */

}

void gc_done(DomainDesc * domain)
{
	/* finalize memory objects */
	// FIXME
#ifdef CHECK_HEAP_BEFORE_GC
	gc_checkHeap(domain, JNI_FALSE);
	printf("HEAP OK\n");
#endif				/* CHECK_HEAP_BEFORE_GC */
	GC_FUNC_NAME(finalizeMemory) (domain);
	GC_FUNC_NAME(finalizePortals) (domain);
	GC_FUNC_NAME(done) (domain);
}
#endif				/* ENABLE_GC */
