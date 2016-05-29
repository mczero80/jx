/********************************************************************************
 * Portal invocation and data copy between domains
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/
#include "all.h"
#include "runq.h"
#include "scheduler_inlined.h"
#include "gc_common.h"
#include "gc_impl.h"

#ifdef PROFILE_EVENT_PORTAL
static int event_send_start, event_send_block, event_send_end, event_send_return, event_send_handoff_to_recv,
    event_send_returned_from_recv, event_receive_start, event_receive_handoff_sender, event_receive_finished,
    event_receive_start_exec, event_receive_end_exec;
#define PORTALEVENT_SEND_END       RECORD_EVENT(event_send_end)
#define PORTALEVENT_SEND_START     RECORD_EVENT(event_send_start)
#define PORTALEVENT_SEND_RETURN    RECORD_EVENT(event_send_return)
#define PORTALEVENT_SEND_HANDOFF_TO_RECV    RECORD_EVENT(event_send_handoff_to_recv)
#define PORTALEVENT_SEND_RETURNED_FROM_RECV RECORD_EVENT(event_send_returned_from_recv)
#define PORTALEVENT_SEND_BLOCK     RECORD_EVENT(event_send_block)
#define PORTALEVENT_RECEIVE_START  RECORD_EVENT(event_receive_start)
#define PORTALEVENT_RECEIVE_HANDOFF_SENDER  RECORD_EVENT(event_receive_handoff_sender)
#define PORTALEVENT_RECEIVE_FINISHED  RECORD_EVENT(event_receive_finished)
#define PORTALEVENT_RECEIVE_START_EXEC  RECORD_EVENT(event_receive_start_exec)
#define PORTALEVENT_RECEIVE_END_EXEC  RECORD_EVENT(event_receive_end_exec)
#else
#define PORTALEVENT_SEND_END
#define PORTALEVENT_SEND_START
#define PORTALEVENT_SEND_RETURN
#define PORTALEVENT_SEND_HANDOFF_TO_RECV
#define PORTALEVENT_SEND_RETURNED_FROM_RECV
#define PORTALEVENT_SEND_BLOCK
#define PORTALEVENT_RECEIVE_START
#define PORTALEVENT_RECEIVE_HANDOFF_SENDER
#define PORTALEVENT_RECEIVE_FINISHED
#define PORTALEVENT_RECEIVE_START_EXEC
#define PORTALEVENT_RECEIVE_END_EXEC
#endif

#define LOCK_PORTALS   DISABLE_IRQ
#define UNLOCK_PORTALS RESTORE_IRQ

#ifdef DEBUG
#define INLINE
#else
#define INLINE static inline
#endif

//#define VERBOSE_PORTAL_PARAM_COPY 1
//#define CHECK_DEPPARAMS 1

//#define VERBOSE_SENDER_QUEUE 1

/*
#define DBG_DEP 1
#define DBG_AUTO_PORTAL_PROMO 1
*/

//#define pprintf printf
static inline void pprintf(char *s, ...)
{
}

#define pprintf1(a)
#define pprintf2(a,b)
#define pprintf3(a,b,c)
#define pprintf4(a,b,c,d)
#define pprintf5(a,b,c,d,e)

//#define MULTI_PORTAL_HACK 1

ObjectDesc *copy_reference(DomainDesc * src, DomainDesc * dst, ObjectDesc * ref, u4_t * quota);

ObjectDesc *memoryManager_alloc(ObjectDesc * self, jint size);
ObjectDesc *memoryManager_allocAligned(ObjectDesc * self, jint size, jint bytes);
ObjectDesc *bootfs_getFile(ObjectDesc * self, ObjectDesc * filename);

/* returns index into domains service table */
#ifdef NEW_PORTALCALL
u4_t createService(DomainDesc * domain, ObjectDesc * depObj, ClassDesc * interface, ServiceThreadPool * pool)
#else
u4_t createService(DomainDesc * domain, ObjectDesc * depObj, ClassDesc * interface)
#endif
{
	DEPDesc *dep;
	u4_t index;

	LOCK_SERVICETABLE;
	for (index = 0; index < MAX_SERVICES; index++) {
		if (domain->services[index] == SERVICE_ENTRY_FREE) {
			domain->services[index] = SERVICE_ENTRY_CHANGING;
			break;
		}
	}
	UNLOCK_SERVICETABLE;
	if (index == MAX_SERVICES)
		sys_panic("domain can not create more services");

	dep = allocServiceDescInDomain(domain);
	dep->domain = domain;
	dep->obj = depObj;
	ASSERTCLASSDESC(interface);
	dep->interface = interface;
#ifdef NEW_PORTALCALL
	if (pool)
		connectServiceToPool(dep, pool);
	else
		dep->pool = NULL;
#else
	dep->firstWaitingSender = NULL;
	dep->lastWaitingSender = NULL;
	dep->firstWaitingReceiver = NULL;
	dep->lastWaitingReceiver = NULL;
#endif
	dep->flags = DEPFLAG_NONE;
	dep->valid = 1;
	dep->serviceIndex = index;
	pprintf("Created DEP 0x%lx in domain 0x%lx\n", dep, domain);
	domain->services[index] = dep;
	return index;
}

#ifdef NEW_PORTALCALL
ThreadDesc *createServiceThread(DomainDesc * domain, int poolIndex, char *name)
{
	ServiceThreadPool *pool = domain->pools[poolIndex];
	ThreadDesc *thread = createThread(domain, receive_dep, (void *) poolIndex, STATE_RUNNABLE, SCHED_CREATETHREAD_DEFAULT);
	thread->nextInReceiveQueue = pool->firstReceiver;
	pool->firstReceiver = thread;
	setThreadName(thread, "SVCPool", name);
	return thread;
}
#endif

#ifndef DIRECT_SEND_PORTAL

char proxycode[] = { /* pushl  %ebp                     */ 0x55,
	/* movl   %esp,%ebp                */ 0x89, 0xe5,
	/* leal   0x8(%ebp),%ecx           */ 0x8d, 0x4d, 0x08,
	/* pushl  %ecx                     */ 0x51,
	/* pushl  $0x2                     */ 0x6a, 0x02,
	/* pushl  $0x04030201              */ 0x68, 0x01, 0x02, 0x03, 0x04,
	/* call   0x8053cd0 <send_portalcall> */ 0xe8, 0x84, 0xfd, 0xff,
	0xff,
	/* movl   %ebp,%esp                */ 0x89, 0xec,
	/* popl   %ebp                     */ 0x5d,
	/* ret                             */ 0xc3
};
#define PROXYC_METHODINDEX   10
#define PROXYC_NUMPARAMS     8
#define PROXYC_ADDRESS       15
#endif

void abstract_method_error(ObjectDesc * self)
{
	ClassDesc *oclass;
	printf("ABSTRACT METHOD CALLED at object %p\n", self);
	printf("** PLEASE RECOMPILE ALL CLASSES **\n");
	oclass = obj2ClassDesc(self);
	ASSERTCLASSDESC(oclass);
	printf("  ClassName: %s\n", oclass->name);

	asm("int $3");
	sys_panic("THIS METHOD IS ABSTRACT");
}
static void abstract_method_error_proxy()
{
	sys_panic("THIS PROXY METHOD IS ABSTRACT");
}

extern DEPDesc *domainZeroDEP;

#ifdef DIRECT_SEND_PORTAL
u4_t direct_send_portal(Proxy * proxy, ...);
#endif

Proxy *createPortalInDomain(DomainDesc * domain, ClassDesc * depClass, DomainDesc * targetDomain, u4_t targetDomainID,
			    u4_t depIndex)
{
	code_t *vtable;
	jint j;
	code_t code;
	char *addr;
	Proxy *proxy;
	ASSERTCLASSDESC(depClass);

#ifdef SAMPLE_FASTPATH
	if (do_sampling) {
		printStackTrace("SLOWOPT-CREATE-PORTAL ", curthr(), &domain - 2);
	}
#endif

	if (depClass->proxyVtable == NULL) {
		vtable = malloc_vtable(domain, depClass->vtableSize + 1);
		vtable[0] = (code_t) depClass;
		vtable++;
		for (j = 0; j < depClass->vtableSize; j++) {
			if (depClass->vtableSym[j * 3][0] == '\0') {
				/* hole */
				vtable[j] = (code_t) abstract_method_error_proxy;
			} else {
#ifndef DIRECT_SEND_PORTAL
				code_t c;
				if (strcmp(depClass->vtableSym[j * 3], "jx/zero/Memory") == 0) {
					sys_panic("SHOULD NOT BE USED");
				} else {
					/*c = dep_send_msg; */
					c = (code_t) send_portalcall;
				}
				/* method */
				code = (code_t) malloc_proxycode(domain, sizeof(proxycode) + 3);
				code = (code_t) (((u4_t) code + 3) & 0xfffffffc);
				memcpy(code, proxycode, sizeof(proxycode));
				*((u4_t *) (code + PROXYC_METHODINDEX)) = j;
				((char *) code)[PROXYC_NUMPARAMS] = 10;
				/*addr = (char*)dep_send_msg-(code+PROXYC_ADDRESS+4); */
				addr = (char *) ((char *) send_portalcall - ((char *) code + PROXYC_ADDRESS + 4));
				*(char **) ((char *) code + PROXYC_ADDRESS) = addr;
				/*printf("CODE %s %d %p\n", depClass->name, j, code); */
				vtable[j] = code;
#else				/* DIRECT_SEND_PORTAL */
				vtable[j] = direct_send_portal;
#endif				/* DIRECT_SEND_PORTAL */
			}
		}
		depClass->proxyVtable = vtable;
	}

	proxy = allocProxyInDomain(domain, depClass, targetDomain, targetDomainID, depIndex);
	return proxy;
}


/* cl==NULL means create table for java_lang_Object */
/* methods==NULL and numMethods==0 means create call to exception_handler */
void installVtables(DomainDesc * domain, ClassDesc * c, MethodInfoDesc * methods, int numMethods, ClassDesc * cl)
{
	int j, k;
	int failure = 0;
	ASSERTCLASSDESC(c);
	ASSERT(methods != NULL);

	for (j = 0; j < c->vtableSize; j++) {
		c->vtable[j] = abstract_method_error;
		c->methodVtable[j] = NULL;
	}

	//if (cl != NULL) {
	installObjectVtable(c);
	//}

	if (methods == NULL && numMethods == 0)
		return;

	ASSERT(c->methodVtable[0] != NULL);
	ASSERT(cl == NULL || cl->methodVtable[0] != NULL);

	for (j = 0; j < c->vtableSize; j++) {
		if (c->vtableSym[j * 3][0] == '\0')
			continue;	/* hole */
		//if (j> n_object_methods) c->vtable[j] = NULL;
		for (k = 0; k < numMethods; k++) {
			if (strcmp(methods[k].name, c->vtableSym[j * 3 + 1]) == 0) {
				c->vtable[j] = methods[k].code;
				if (cl != NULL) {
					c->methodVtable[j] = malloc_methoddesc(domain);
					memset(c->methodVtable[j], 0, sizeof(MethodDesc));
					c->methodVtable[j]->name = cl->methodVtable[j]->name;
					c->methodVtable[j]->signature = cl->methodVtable[j]->signature;
					c->methodVtable[j]->numberOfArgs = cl->methodVtable[j]->numberOfArgs;
					c->methodVtable[j]->argTypeMap = cl->methodVtable[j]->argTypeMap;
					c->methodVtable[j]->returnType = cl->methodVtable[j]->returnType;
				}
				/*
				   printf("  %d ", j);
				   if (c->methodVtable[j]) printf("  %s %s ", c->methodVtable[j]->name, c->methodVtable[j]->signature);
				   else printf("  ???");
				   printf("\n");
				 */
				break;
			}
		}
		if (c->vtable[j] == abstract_method_error) {
			printf(" NOT FOUND: class %s contains no %s %s\n", c->name, cl->methodVtable[j]->name,
			       cl->methodVtable[j]->signature);
			failure = 1;
		}
	}
	if (failure) {
		sys_panic("Some DomainZero Portal methods are not implemented.");
	}
}





/*********
 *  Portal invocation 
 ****/

#ifdef SMP
static spinlock_t lock = SPIN_LOCK_UNLOCKED;
#endif

void check_servicequeue(DEPDesc * dep)
{
#ifndef NEW_PORTALCALL
#ifdef DEBUG
	int i = 0;
	ThreadDesc *t, *s;
	ASSERTCLI;
	for (t = dep->firstWaitingSender; t != NULL; t = t->nextInDEPQueue) {
		for (s = t->nextInDEPQueue; s != NULL; s = s->nextInDEPQueue) {
			if (s == t)
				sys_panic("Thread %p multiple times in depqueue %p\n", t, dep);
			i++;
			if (i == 1000000) {
				ClassDesc *c = obj2ClassDesc(dep->obj);
				sys_panic("endless loop in portal waitqueue domain %d service %s (waiting domain=%d)",
					  dep->domain->id, c->name, t->domain->id);
			}
		}
	}
#endif
#endif
}

void check_notin_servicequeue(DEPDesc * dep, ThreadDesc * thread)
{
#ifndef NEW_PORTALCALL
#ifdef DEBUG
	ThreadDesc *t;
	for (t = dep->firstWaitingSender; t != NULL; t = t->nextInDEPQueue) {
		if (thread == t)
			sys_panic("Thread %p already in depqueue %p\n", thread, dep);
	}
#endif
#endif
}



#ifndef NEW_PORTALCALL
INLINE void portal_set_recv(DEPDesc * dep, ThreadDesc * thread)
{
	ASSERT(thread != NULL);
	ASSERT(dep != NULL);
	dep->receiver = thread;
}
#endif

INLINE void portal_add_sender(DEPDesc * dep, ThreadDesc * thread)
{
#ifdef VERBOSE_SENDER_QUEUE
	printf("ENQUEUE %d.%d\n", TID(thread));
#endif				/* VERBOSE_SENDER_QUEUE */
#ifndef NEW_PORTALCALL
	// called with disabled IRQs
	ASSERT(thread != NULL);
	ASSERT(dep != NULL);
#ifdef SMP
	spin_lock(&lock);
#endif
	//printf("ENQUEUE %p\n", thread);

#ifdef DEBUG
	check_servicequeue(dep);
	check_notin_servicequeue(dep, thread);
#endif

	thread->nextInDEPQueue = NULL;
	if (dep->lastWaitingSender == NULL) {
		dep->lastWaitingSender = dep->firstWaitingSender = thread;
	} else {
		dep->lastWaitingSender->nextInDEPQueue = thread;
		dep->lastWaitingSender = thread;
	}

#ifdef DEBUG
	check_servicequeue(dep);
#endif

#ifdef SMP
	spin_unlock(&lock);
#endif

#else				/*NEW_PORTALCALL */
	ASSERTCLI;
	thread->nextInDEPQueue = NULL;
	if (dep->pool->lastWaitingSender == NULL) {
		ASSERT(dep->pool->firstWaitingSender == NULL);
		dep->pool->lastWaitingSender = dep->pool->firstWaitingSender = thread;
	} else {
		dep->pool->lastWaitingSender->nextInDEPQueue = thread;
		dep->pool->lastWaitingSender = thread;
	}
#endif				/*NEW_PORTALCALL */
}


#ifndef NEW_PORTALCALL
INLINE ThreadDesc *portal_dequeue_sender(DEPDesc * dep)
{
	ThreadDesc *ret = dep->firstWaitingSender;
	if (ret == NULL)
		return NULL;
	dep->firstWaitingSender = dep->firstWaitingSender->nextInDEPQueue;
	if (dep->firstWaitingSender == NULL) {
		dep->lastWaitingSender = NULL;
	}
	ret->nextInDEPQueue = NULL;
	return ret;
#else				/*NEW_PORTALCALL */
INLINE ThreadDesc *portal_dequeue_sender(ServiceThreadPool * pool)
{
	ThreadDesc *ret = pool->firstWaitingSender;
	ASSERTCLI;
	if (ret == NULL)
		return NULL;
	pool->firstWaitingSender = pool->firstWaitingSender->nextInDEPQueue;
	if (pool->firstWaitingSender == NULL) {
		pool->lastWaitingSender = NULL;
	}
	ret->nextInDEPQueue = NULL;
#ifdef VERBOSE_SENDER_QUEUE
	printf("DEQUEUE %d.%d\n", TID(ret));
#endif				/* VERBOSE_SENDER_QUEUE */
	return ret;
#endif				/*NEW_PORTALCALL */
}

void portal_remove_sender(DEPDesc * dep, ThreadDesc * sender)
{
#ifndef NEW_PORTALCALL
	ThreadDesc **t;
	for (t = &(dep->firstWaitingSender); *t != NULL; t = &((*t)->nextInDEPQueue)) {
		if (*t == sender) {
			*t = (*t)->nextInDEPQueue;
			return;
		}
	}
	sys_panic("SENDER NOT IN QUEUE");
#else				/*NEW_PORTALCALL */
	ThreadDesc **t;
	ASSERTCLI;
	for (t = &(dep->pool->firstWaitingSender); *t != NULL; t = &((*t)->nextInDEPQueue)) {
		if (*t == sender) {
			*t = (*t)->nextInDEPQueue;
			return;
		}
	}
	sys_panic("SENDER NOT IN QUEUE");
#endif				/*NEW_PORTALCALL */
}

void portal_abort_current_call(DEPDesc * dep, ThreadDesc * sender)
{
	dep->abortFlag = 1;
}

ObjectDesc *copy_returnvalue(DomainDesc * src, DomainDesc * dst, ObjectDesc * srcObj, int *quota)
{
	return copy_reference(curdom(), dst, srcObj, quota);
}


typedef ObjectDesc *(*copy_returnvalue_t) (DomainDesc * src, DomainDesc * dst, ObjectDesc * srcObj, int *quota);

copy_returnvalue_t copy_returnvalue_ptr;

/* MACROS */

#ifdef SMP
#define PORTAL_SPINLOCK  spin_lock(&lock);	// IRQs are disabled!
#define PORTAL_SPINUNLOCK  spin_unlock(&lock);	// IRQs are disabled!
#else
#define PORTAL_SPINLOCK
#define PORTAL_SPINUNLOCK
#endif


#ifdef NEW_PORTALCALL
#define GETSVC svc = curdom()->services[serviceIndex]; ASSERTDEP(svc);
#define GETPOOL pool = curdom()->pools[poolIndex];
#else
#define GETSVC svc = curdom()->services[serviceIndex]; ASSERTDEP(svc);
#endif

// refresh svc (and pool), because it could be moved during GC
#ifdef NEW_PORTALCALL
#define REFRESH	GETPOOL;GETSVC;
#else
#define REFRESH	GETSVC;
#endif

#ifdef NEW_PORTALCALL
//void receive_portalcall(ServiceThreadPool * pool)
void receive_portalcall(u4_t poolIndex)
#else
void receive_portalcall(u4_t serviceIndex)
#endif
{
	ThreadDesc *source;
	jint methodIndex;
	jint ret;
	jint numParams;
	ObjectDesc *obj;
	code_t code;
	ClassDesc *targetClass;
	jint numberArgs;
	jbyte *argTypeMap;
	jint returnType;
	u4_t quota;
	u4_t *myparams;
	int i;
	DEPDesc *svc = NULL;
	MethodDesc *method;
#ifdef NEW_PORTALCALL
	u4_t serviceIndex;
	ServiceThreadPool *pool;
#endif
	DomainDesc *domain = curdom();
	copy_returnvalue_ptr = copy_returnvalue;

	CHECK_STACK_SIZE(source, 256);

	DISABLE_IRQ;

	curthr()->isPortalThread = JNI_TRUE;

	thread_prepare_to_copy();

	myparams = jxmalloc(MYPARAMS_SIZE MEMTYPE_OTHER);	/* FIXME: how do we choose a good size? no malloc! USE DIRECT POINTER TO NEXT STACKFRAME!!! */
	curthr()->myparams = myparams;

#ifdef DEBUG
	//  check_domain_not_in_runq(curdom());
#endif

#ifdef MULTI_PORTAL_HACK
	if (svc->receiver == NULL)
		portal_set_recv(svc, curthr());
#else
#endif

	for (;;) {		/* receive loop */


		curthr()->processingDEP = NULL;
		curthr()->mostRecentlyCalledBy = NULL;


    /**************
     *  Wait until there is a sender.
     **************/
#ifndef NEW_PORTALCALL
		GETSVC;
		while ((source = portal_dequeue_sender(svc)) == NULL) {
			PROFILE_STOP(curthr());
			SCHED_BLOCK_PORTAL_WAIT_FOR_SND;
			GETSVC;
			PROFILE_CONT(curthr());
		}
#else
		ASSERTCLI;
		GETPOOL;
		while ((source = portal_dequeue_sender(pool)) == NULL) {
			PROFILE_STOP(curthr());
			SCHED_BLOCK_PORTAL_WAIT_FOR_SND;
			GETPOOL;
			PROFILE_CONT(curthr());
		}
#endif
		//printf("Sender Thread: %d.%d (%p)\n", TID(source), source);

#ifdef DEBUG
		check_domain_not_in_runq(curdom());
#endif

    /**************
     *  There is a waiting sender.
     **************/
		PORTALEVENT_RECEIVE_START;

		source->state = STATE_PORTAL_WAIT_FOR_PARAMCOPY;
		serviceIndex = source->blockedInServiceIndex;
		GETSVC;

		ASSERTTHREAD(source);
		//printf("PROCESSING STARTED %s\n", svc->interface->name);
		quota = RECEIVE_PORTAL_QUOTA;	/*  4 kB portal parameter quota , new quota for each new call */
		curthr()->n_copied = 0;
#if defined(KERNEL) && defined(CHECK_SERIAL_IN_PORTAL)
		check_serial();
#endif				/* KERNEL && CHECK_SERIAL_IN_PORTAL */

		ASSERT(curthr()->state == STATE_RUNNABLE);

		obj = svc->obj;
		curthr()->processingDEP = svc;
		curthr()->mostRecentlyCalledBy = source;
		source->blockedInServiceThread = curthr();

		ASSERT(curthr()->state == STATE_RUNNABLE);

		methodIndex = source->depMethodIndex;
		obj = (ObjectDesc *) svc->obj;
		ASSERTOBJECT(obj);
		code = (code_t) obj->vtable[methodIndex];

		/* Copy parameters from caller domain */
		targetClass = obj2ClassDesc(obj);

		ASSERT(methodIndex < targetClass->vtableSize);

		method = targetClass->methodVtable[methodIndex];
		ASSERT(method);

#ifdef DBG_DEP
		printf(" %s %s %d\n", targetClass->methodVtable[methodIndex]->name,
		       targetClass->methodVtable[methodIndex]->signature, targetClass->methodVtable[methodIndex]->numberOfArgs);
#endif				/* DBG_DEP */

		numberArgs = method->numberOfArgs;
		argTypeMap = method->argTypeMap;
		returnType = method->returnType;

		/* COPY IMPLICIT PORTAL PARAMETER FROM CALLER */
		if (source->portalParameter != NULL) {
		      restart:
			curthr()->n_copied = 0;
			curthr()->portalParameter = copy_reference(source->domain, curdom(), source->portalParameter, &quota);
			if (curthr()->portalParameter == 0xffffffff) {
				source = curthr()->mostRecentlyCalledBy;
				goto restart;
			}
		}

		curthr()->myparams[0] = obj;
		for (i = 1; i < numberArgs + 1; i++) {
			ClassDesc *cl = NULL;
			if (source->depParams[i] == NULL) {
				curthr()->myparams[i] = NULL;
				continue;
			}
			if (isRef(argTypeMap, numberArgs, i - 1)) {
				if (source->depParams[i] != 0) {
					cl = obj2ClassDesc(source->depParams[i]);
					//printf("PARAM (object) %p %s\n", source->depParams[i], cl!=NULL?cl->name:"(null)");

					/* copy object to target domain */
#ifndef COPY_TO_DOMAINZERO
					if (curdom() == domainZero) {
						pprintf("  do not copy to DomainZero\n");
						curthr()->myparams[i] = (ObjectDesc *) source->depParams[i];
					} else {
					      restart0:
						curthr()->n_copied = 0;
						ASSERT(source->depParams[i] != 0xffffffff);
						curthr()->myparams[i] = (jint)
						    copy_reference(source->domain, curdom(), (ObjectDesc *)
								   source->depParams[i], &quota);
						if (curthr()->myparams[i] == 0xffffffff) {
							source = curthr()->mostRecentlyCalledBy;
							goto restart0;
						}
					}
#else
					curthr()->myparams[i] = (jint) copy_reference(source->domain, curdom(), (ObjectDesc *)
										      source->depParams[i], &quota);
					if (curthr()->myparams[i] == 0xffffffff)
						sys_panic("todo: implement restart");
#endif
				}
			} else {	/* no reference */
				curthr()->myparams[i] = source->depParams[i];
				//printf("PARAM (numeric) %d \n", curthr()->myparams[i]/*source->depParams[i]*/);
			}
		}
		source->depParams = NULL;	// do not need params any longer
		ASSERTOBJECT((ObjectDesc *) curthr()->myparams[0]);


		REFRESH;

#ifdef PORTAL_STATISTICS
		curdom()->portal_statistics_copyin_rcv += RECEIVE_PORTAL_QUOTA - quota;
#endif				/* PORTAL_STATISTICS */

#ifdef DBG_DEP
		for (i = 0; i < numberArgs + 1; i++) {
			printf(" arg[%d]=%p   (senderparam=%p)\n", i, curthr()->myparams[i], source->depParams[i]);
		}
#endif				/* DBG_DEP */

#ifdef DEBUG
		check_servicequeue(svc);
#endif
		check_notin_servicequeue(svc, source);

#ifdef PORTAL_INTERCEPTOR
		if (curthr() == curdom()->inboundInterceptorThread) {
			printf("inbound interceptor calls portal of same domain\n");
		}
		if (curthr() == curdom()->outboundInterceptorThread) {
			printf("outbound interceptor calls portal of same domain\n");
		}
		if (source->domain->memberOfTCB == JNI_FALSE && curdom()->inboundInterceptorCode) {
			ClassDesc *cl;
			ArrayDesc *paramArray;
			u4_t quota = RECEIVE_PORTAL_QUOTA;	/*  4 kB portal parameter quota , new quota for each new call */
			ThreadDesc *t = curthr();
			InterceptInboundInfoProxy *info =
			    allocInterceptInboundInfoProxyInDomain(curdom()->inboundInterceptorThread->domain);
			info->source = source->domain;
			info->target = curdom();
			info->method = method2Obj(method);
			ASSERT(source->depParams[0] != 0);
			ASSERT((getObjFlags(source->depParams[0]) & FLAGS_MASK) == OBJFLAGS_PORTAL);
			info->obj = copy_portal(source->domain, curdom(), source->depParams[0], &quota, 0);

			info->paramlist = curthr()->myparams;

#ifdef MULTI_PORTAL_HACK
			if (svc->receiver != curthr())
				sys_panic("an additional Thread was intercepted ( is this a problem?? )");
#endif
			ASSERTTHREAD(curdom()->inboundInterceptorThread);
			ASSERTOBJECT(curdom()->inboundInterceptorObject);

			//printf("start inbound Interceptor  (%s)\n", get_IDString(curthr()));

			ret =
			    start_thread_using_code1(curdom()->inboundInterceptorObject, curdom()->inboundInterceptorThread,
						     curdom()->inboundInterceptorCode, info);


			//printf("inbound Interceptor returned (%s)\n", get_IDString(curthr()));
			if (ret == JNI_FALSE) {
				printf("inbound Interceptor rejected the call\n");
				exceptionHandler(THROW_RuntimeException);
			}
		}
#endif				/*  PORTAL_INTERCEPTOR */

		if (!svc->valid) {
			/* service has been deactivated */
			exceptionHandler(THROW_RuntimeException);
		}

		/* remember caller domain to check if it still exists when we return from the service execution */
		curthr()->callerDomain = source->domain;
		curthr()->callerDomainID = curthr()->callerDomain->id;

		/* interrupts are enabled in callnative_special_portal */

		source->state = STATE_PORTAL_WAIT_FOR_RET;

		PORTALEVENT_RECEIVE_START_EXEC;

		ret = callnative_special_portal(&(curthr()->myparams[1]), (ObjectDesc *) curthr()->myparams[0], code, numberArgs);	/* irqs are enabled in this function */
#ifndef KERNEL
		irq_store_disable();	/* FIXME */
#endif
		PORTALEVENT_RECEIVE_END_EXEC;

		/* interrupts are disabled in callnative_special_portal */

		/* check if caller domain was terminated while we executed the service */
		if (curthr()->callerDomainID != curthr()->callerDomain->id
		    || curthr()->callerDomain->state != DOMAIN_STATE_ACTIVE) {
			/* caller disappeared, ignore all return values */
			goto finishedReturnHandling;
		}
		// refresh source, because it could be moved during GC
		source = curthr()->mostRecentlyCalledBy;
		ASSERTTHREAD(source);

		// refresh svc, because it could be moved during GC
		REFRESH;

		/* first check if the caller is still alive */
		if (svc->abortFlag == 1) {
			/* aborted: either caller disappeared or aborted call -> ignore all return values */
			svc->abortFlag = 0;
			goto finishedReturnHandling;
		}

		check_notin_servicequeue(svc, curthr());

		pprintf("PROCESSING FINISHED d:%p,src:%p  ret=%p\n", svc, source, ret);


		if (returnType == 1) {
			ASSERTOBJECT((ObjectDesc *) ret);
#ifdef CHECK_HEAPUSAGE
			if ((ObjectDesc *) ret != NULL && !gc_isValidHeapRef(curdom(), (ObjectDesc *) ret)) {
				printf("ret=%p domain=%p (%s)\n", (ObjectDesc *) ret, curdom(), curdom()->domainName);
				gc_printInfo(curdom());
				printf(" %s %s %d %p\n", method->name, method->signature, method->numberOfArgs, code);
				sys_panic("Portal return not a valid reference!");
			}
#endif
		}
		// refresh svc, because it could be moved during GC
		REFRESH;

		/* remove references to parameter objects -> allow GC to collect thgem */
		memset(myparams, 0, numberArgs * 4);

		/* Send reply to caller.
		 */

		PROFILE_STOP(curthr());

		quota = RECEIVE_PORTAL_QUOTA;

		/* copy return value to caller domain */
		source->state = STATE_PORTAL_WAIT_FOR_RETCOPY;
		if (returnType == 1) {
			ObjectDesc *ret0;
			ASSERTOBJECT(ret);
			if (code != memoryManager_alloc && code != memoryManager_allocAligned && code != bootfs_getFile) {	/* memory proxy has already been allocated in client heap */
				//printf("code %p\n", code);
			      restart1:
#ifdef NEVER
				if (ret != NULL) {
					ClassDesc *mc = obj2ClassDesc(ret);
					printf("RET %s\n", mc->name);
				}
#endif
				curthr()->n_copied = 0;
				ret0 = (jint) copy_reference(curdom(), source->domain, (ObjectDesc *)
							     ret, &quota);
				if (ret0 == 0xffffffff) {	/* restart */
					source = curthr()->mostRecentlyCalledBy;
					goto restart1;
				}
				ret = ret0;

			} else {
				ASSERTMEMORY(ret);
				//MemoryProxy *xx = (MemoryProxy*)ret;
				//printf("refcount %d\n", xx->dz->refcount);
			}
			source->portalReturnType = 1;
		} else {
			pprintf("RETURN (numeric) %d\n", ret);
			source->portalReturnType = 0;
		}
		source->portalReturn = (ObjectDesc *) ret;

#ifdef PORTAL_STATISTICS
		curdom()->portal_statistics_copyout_rcv += RECEIVE_PORTAL_QUOTA - quota;
#endif				/* PORTAL_STATISTICS */

		curthr()->processingDEP = NULL;
		curthr()->mostRecentlyCalledBy = NULL;

#ifdef CONT_PORTAL
		(curthr()->linkedDEPThr)->linkedDEPThr = source;
#endif


#ifdef SMP
		ASSERT(source->curCpuId == get_processor_id());
#endif

#ifdef MULTI_PORTAL_HACK
		if (svc->receiver != curthr()) {
			printf("MULTI_PORTAL_HACK: destroying Portalthread\n");
			curthr()->state = STATE_ZOMBIE;
			switch_to(curthrP(), source);
			thread_exit();
			sys_panic("schould not return");
		}
#endif

		/* If there is a next sender waiting there are two alternatives:
		 *  1. Set source runnable and handoff to source
		 *  2. Set source runnable and handoff to next sender
		 */
		//    if (svc->firstWaitingSender) { /* the next sender is already waiting */
		// ...
		//}

		PORTALEVENT_RECEIVE_HANDOFF_SENDER;

		/* currently we always handoff to sender */
#ifdef NEW_PORTALCALL
		if (!domain->gc.isInHeap(curdom(), svc))
			sys_panic("svc not in heap");
		if (!domain->gc.isInHeap(curdom(), svc->pool))
			sys_panic("svc->pool  not in heap");
		Sched_portal_handoff_to_sender(source, svc->pool->firstWaitingSender ? 1 : 0);
#else
		Sched_portal_handoff_to_sender(source, svc->firstWaitingSender ? 1 : 0);
#endif
		PROFILE_CONT(curthr());

	      finishedReturnHandling:
		PORTALEVENT_RECEIVE_FINISHED;
	}			/* end of receive loop */


	RESTORE_IRQ;		/* never executed but needed for DISABLE_IRQ macro */
}

#ifdef DIRECT_SEND_PORTAL
u4_t direct_send_portal(Proxy * proxy, ...)
{
	u4_t methodIndex;
	jint **paramlist;
#else				/* DIRECT_SEND_PORTAL */
jint send_portalcall(jint methodIndex, jint numParamsOLD, jint ** paramlist)
{
	Proxy *proxy;
#endif
	ThreadDesc *target;
	ClassDesc *oclass;
	DomainDesc *targetDomain;
	int handoff;
#ifdef KERNEL
	u4_t oldflags;
#else
	sigset_t signalmask;
#endif
#ifdef JAVASCHEDULER
	HLSchedDesc *HLS;
#endif
	DEPDesc *svc;

#ifdef DIRECT_SEND_PORTAL
	/* MUST BE THE FIRST INSTRUCTION IN THIS FUNCTION!! */
	asm volatile ("movl %%ecx, %0":"=r" (methodIndex));
	paramlist = &proxy;
#endif

	proxy = *(Proxy **) paramlist;
	//pprintf("SEND ##%p#%p\n", proxy, curdom());
	//gc_printInfo(curdom());

	CHECKINHEAP(curdom(), proxy);
	ASSERT(proxy != NULL);
	ASSERT(proxy > 10000 && proxy < 0xfffff000);
	ASSERTPROXY(proxy);

	ASSERT(methodIndex < obj2ClassDesc(proxy)->vtableSize);

	pprintf5("SEND: proxy=%p methodindex=%d targetdomain=%p svcindex=%d\n", proxy, methodIndex, proxy->targetDomain,
		 proxy->index);

	targetDomain = proxy->targetDomain;
	ASSERTDOMAIN(targetDomain);

#ifdef SAMPLE_FASTPATH
	if (do_sampling) {
		printf("SEND: proxy=%p methodindex=%d targetdomain=%p svcindex=%d\n", proxy, methodIndex, proxy->targetDomain,
		       proxy->index);
		printStackTraceNew("SLOWOPERATION-PORTAL ");
	}
#endif

#ifdef DEBUG
	{
		ClassDesc *c = obj2ClassDesc(proxy);
		if (!(methodIndex < c->vtableSize)) {
			printf("methodIndex =%d, c=%p\n", methodIndex, c);
		}

		ASSERT(methodIndex < c->vtableSize);
	}
#endif

	if (targetDomain == curdom()) {
		//if (targetDomain == curthr()) {
		/* short circuit portal call inside one domain */
		/* works when method tables in both domains are identical! */
		ObjectDesc *obj = targetDomain->services[proxy->index]->obj;
		code_t c;
		jint numParams;
		ASSERTOBJECT(obj);
		c = obj->vtable[methodIndex];
		if (proxy->targetDomainID != targetDomain->id) {
			exceptionHandler(-1);	/* domain was terminated */
		}
#ifdef NEVER
		{
			ClassDesc *cl = obj2ClassDesc(obj);
			printf("SENDLOCCAL: obj=%p class=%s methodindex=%d method=%s\n", obj, cl->name, methodIndex,
			       cl->vtableSym[methodIndex * 3 + 1]);
		}
#endif				/* DBG_DEP */
		numParams = obj2ClassDesc(obj)->methodVtable[methodIndex]->numberOfArgs;
		return callnative_special(paramlist + 1, obj, c, numParams);
	}

	PORTALEVENT_SEND_START;

#ifdef ASSERT_PORTALCALL_NOT_IN_IRQHANDLER
	//  ASSERTNOCLI;
#endif

	LOCK_PORTALS;

	if (proxy->targetDomainID != targetDomain->id) {
		/* target domain was terminated and DCB reused */
		PORTAL_RETURN_SET_EXCEPTION(curthr()->portalReturnType);
		goto finished;
	}

	if (targetDomain->state != DOMAIN_STATE_ACTIVE) {
#ifdef NEVER
		exceptionHandlerMsg(THROW_RuntimeException, "target domain not active");
#endif
		PORTAL_RETURN_SET_EXCEPTION(curthr()->portalReturnType);
		goto finished;
	}

	svc = targetDomain->services[proxy->index];
	if (svc == SERVICE_ENTRY_CHANGING) {
#ifdef NEVER
		exceptionHandler(THROW_RuntimeException);
#endif
		PORTAL_RETURN_SET_EXCEPTION(curthr()->portalReturnType);
		goto finished;
	}


	PROFILE_STOP_PORTAL(curthr());

#ifdef PORTAL_INTERCEPTOR
	{
		u4_t ret;
#ifdef DEBUG
		ThreadDesc *switchedFrom = curthr();
#endif
		if (curthr() == curdom()->inboundInterceptorThread) {
			printf("inbound interceptor calls portal of same domain\n");
		}
		if (cur == curdom()->outboundInterceptorThread) {
			printf("outbound interceptor calls portal of same domain\n");
		}

		if (curdom()->memberOfTCB == JNI_FALSE && curdom()->outboundInterceptorCode) {
			InterceptOutboundInfo info;
			ClassDesc *cl;
			ArrayDesc *paramArray;
			u4_t quota = 1000;	/*  4 kB portal parameter quota , new quota for each new call */
			ThreadDesc *t = curthr();

			/* to allow atomic state investigation and decision domains must be frozen */
			//      freezeDomain(curdom());
			//      freezeDomain(targetDomain);


			info.source = curdom();
			info.target = targetDomain;
			cl = obj2ClassDesc(proxy);
			info.method = method2Obj(cl->methodVtable[methodIndex]);
			//      ASSERTMETHODDESC(info.method);
			paramArray = allocArray(java_lang_Object, cl->methodVtable[methodIndex]->numberOfArgs);


			ASSERT(t == curthr());
			thread_prepare_to_copy();

			info.paramlist = copy_reference(curdom(), targetDomain, paramArray, &quota);

			ASSERTTHREAD(curdom()->outboundInterceptorThread);
			ASSERTOBJECT(curdom()->outboundInterceptorObject);

			ret =
			    start_thread_using_code1(curdom()->outboundInterceptorObject, curdom()->outboundInterceptorThread,
						     curdom()->outboundInterceptorCode, &info);

			//      thawDomain(curdom());
			//      thawDomain(targetDomain);

			//printf("RET:%p\n", ret);
			if (ret == JNI_FALSE) {
				printf("outbound Interceptor rejected the call\n");
				exceptionHandler(THROW_RuntimeException);
			}
			/*
			   if (call_JAVA_method1(curdom()->outboundInterceptorObject, 
			   curdom()->outboundInterceptorThread, 
			   curdom()->outboundInterceptorCode, &info)==JNI_FALSE) {
			   exceptionHandler(-1);
			   }
			 */

		}
#ifdef DEBUG
		if (switchedFrom != curthr())
			sys_panic("");
#endif
	}
#endif				/*  PORTAL_INTERCEPTOR */


	curthr()->blockedInDomain = targetDomain;
	curthr()->blockedInDomainID = targetDomain->id;
	curthr()->blockedInServiceIndex = proxy->index;

	/* make parameters available to receiver  */
	curthr()->depParams = (jint *) paramlist;
	curthr()->depMethodIndex = methodIndex;
#ifdef CHECK_DEPPARAMS
	{
		ThreadDesc *t = curthr();
		jint **paramlist = (jint **) t->depParams;
		u4_t i;
		Proxy *proxy;
		ClassDesc *oclass;
		u4_t methodIndex, numberArgs;
		jbyte *argTypeMap;

		//printf("SET depParams of thread %d.%d to %p\n", TID(curthr()), curthr()->depParams);
		proxy = *(Proxy **) paramlist;
		oclass = obj2ClassDesc((ObjectDesc *) proxy);
		methodIndex = t->depMethodIndex;
		numberArgs = oclass->methodVtable[methodIndex]->numberOfArgs;
		argTypeMap = oclass->methodVtable[methodIndex]->argTypeMap;
		//printf("%s %s\n", oclass->methodVtable[methodIndex]->name, oclass->methodVtable[methodIndex]->signature);
		for (i = 1; i < numberArgs + 1; i++) {
			if (isRef(argTypeMap, numberArgs, i - 1)) {
				//printf("   PORTALPARAM REF %p\n", t->depParams[i]);
				if (!gc_isValidHeapRef(curdom(), t->depParams[i])) {
					sys_panic("NOT A VALID HEAP REF IN depParams");
				}
			} else {
				//printf("   PORTALPARAM NUM %p\n", t->depParams[i]);
			}
		}
	}
#endif

	//printf("%d.%d INDEX: %d svcindex=%d\n", TID(curthr()), methodIndex, curthr()->blockedInServiceIndex);


	ASSERTDEP(svc);
	CHECKINHEAP(curdom(), proxy);
	oclass = obj2ClassDesc((ObjectDesc *) proxy);

	portal_add_sender(svc, curthr());	/* append this sender to the waiting threads */


#ifdef NEW_PORTALCALL
	ASSERTOBJECT(svc);
	ASSERTOBJECT(svc->pool);
	target = svc->pool->firstReceiver;	/* get receiver thread for this service */
	while (target) {
		ASSERTTHREAD(target);
		if (target->state == STATE_PORTAL_WAIT_FOR_SND)
			break;
		target = target->nextInReceiveQueue;
	}
#else
	target = svc->receiver;	/* get receiver thread for this service */
	if (target->state != STATE_PORTAL_WAIT_FOR_SND)
		target = NULL;
#endif

	if (target == NULL
#if defined(SMP)
	    //FIXME: ???? 
	    //|| svc->firstWaitingSender!=curthr()
#endif
	    ) {
    /**************
     *  There is no receiver currently available.
     **************/

#ifdef MULTI_PORTAL_HACK
#ifdef TIMESLICING_TIMER_IRQ
		sys_panic("MULTI_PORTAL_HACK should not be used when timeslicing is enabled");
#endif
#ifdef PORTAL_TRANSFER_INTERCEPTOR
		sys_panic("MULTI_PORTAL_HACK should not be used when PORTAL_TRANSFER_INTERCEPTOR is activated");
#endif
		if (target != NULL && target->state != STATE_WAITPORTAL1) {
			ThreadDesc *t;
			ThreadDesc *sender;
			ASSERTDEP(svc);
			printf("  Waiting Senders on Portal: ");
			if (svc->firstWaitingSender == NULL)
				printf("  none\n");
			else
				for (sender = svc->firstWaitingSender; sender != NULL; sender = sender->nextInDEPQueue)
					printf("  %s\n", get_IDString(sender));
			/*          printf("  currently processing:\n");
			   if (svc->receiver->mostRecentlyCalledBy == NULL)
			   printf("  none\n");
			   else
			   printf("  %s\n",get_IDString(dep->receiver->mostRecentlyCalledBy));
			 */

			printf("MULTI_PORTAL_HACK: (%s) generating new Portalthread", get_IDString(curthr()));
			t = createThread(targetDomain, receive_dep, (void *) (proxy->index));
			printf("(%s)\n", get_IDString(t));
		}
#endif				/* MULTI_PORTAL_HACK */

		/* block this thread and switch to the next runnable thread */
		/* TODO: handoff timeslot to target *domain* (i.e., domain of DEP) */

		//      PROFILE_STOP_PORTAL(curthr());
		//printf("NO RECEIVER      (svc:%p thr:%p)\n", svc, curthr());
		PORTALEVENT_SEND_BLOCK;
#ifdef PORTAL_STATISTICS
		svc->statistics_no_receiver++;
#endif				/*PORTAL_STATISTICS */


		SCHED_BLOCK_PORTAL_WAIT_FOR_RCV;
		/* when the Sched_block_portal_sender() function returns the request has been processed
		 * and this thread is no longer in the service wait queue */

		PROFILE_CONT_PORTAL(curthr());
		pprintf3("SENDING FINISHED (svc:%p thr:%p)\n", svc, curthr());

		// check consistency
		//      ASSERT(! (svc->firstWaitingSender && svc->receiver));
#ifdef DEBUG
		{
			DEPDesc *svc0 = targetDomain->services[proxy->index];
			ASSERTDEP(svc0);
			check_notin_servicequeue(svc0, curthr());
		}
#endif
	} else {		/* endif "no receiver available" */

    /**************
     *  This is the hopefully common case: A receiver thread is available.
     **************/

#ifdef DEBUG
		check_servicequeue(svc);
#endif

#ifdef PORTAL_STATISTICS
		svc->statistics_handoff++;
#endif				/*PORTAL_STATISTICS */

		ASSERTTHREAD(target);

		/* handoff to target thread  */
#ifdef CONT_PORTAL
		/* store information about the caller */
		if (curthr()->linkedDEPThr == curthr())	/* user called this portal */
			target->linkedDEPThr = curthr();
		else		/* called from an other portal */
			target->linkedDEPThr = curthr()->linkedDEPThr;
		(target->linkedDEPThr)->linkedDEPThr = target;
#endif

#ifdef SMP			/* check if a portal call is allowed on this CPU */
		printf("SMP portal check disabled\n");
#endif
#ifdef __SMP			/* check if a portal call is allowed on this CPU */
		if (target->domain->cpu[get_processor_id()] == NULL) {	/* portal not allowed on this CPU */
			printf("portal call not allowed on this CPU (svc:%p)\n", svc);
			printf("the following code was not tested in this context!!\n");
			monitor(NULL);
			pprintf2("remote unblocking Portal Receiver (svc:%p)\n", svc);
			//RECEIVER_STI;
			//smp_call_function(target->curCpuId, start_receiver, target, 0, NULL);
			//smp_call_function(target->curCpuId, start_receiver, target, 1, NULL);
			//RECEIVER_CLI;

			Sched_block_portal_sender();
		} else		/* this CPU is ok */
#endif				/* SMP */
		{
			pprintf3("SENDING STARTED  (svc:%p thr:%p)\n", svc, curthr());
			PORTALEVENT_SEND_HANDOFF_TO_RECV;
			Sched_portal_handoff_to_receiver(target);
			PORTALEVENT_SEND_RETURNED_FROM_RECV;
			pprintf3("SENDING FINISHED (svc:%p thr:%p)\n", svc, curthr());
		}

		PROFILE_CONT_PORTAL(curthr());

	}

  /********
   * Return from portal call
   ********/
      finished:
	curthr()->blockedInDomain = NULL;
	curthr()->blockedInServiceIndex = 0;
	curthr()->blockedInServiceThread = NULL;
	curthr()->depParams = NULL;

	PORTALEVENT_SEND_RETURN;
	UNLOCK_PORTALS;

	if (PORTAL_RETURN_IS_EXCEPTION(curthr()->portalReturnType)) {
		exceptionHandler(curthr()->portalReturn);
	} else {
		/* normal return with reference (already copied) or numeric */
		curthr()->portalReturnType = 0;
		return (jint) curthr()->portalReturn;	//ret;
	}

}


/* -1 failure, 
   0 success */
int findProxyCodeInDomain(DomainDesc * domain, char *addr, char **method, char **sig, ClassDesc ** classInfo)
{
	int g, h, i, j;

	for (h = 0; h < domain->numberOfLibs; h++) {
		LibDesc *lib = domain->libs[h];
		Class *allClasses = lib->allClasses;
		for (i = 0; i < lib->numberOfClasses; i++) {
			ClassDesc *classDesc = allClasses[i].classDesc;
			if (classDesc->proxyVtable == NULL)
				continue;
			for (j = 0; j < classDesc->vtableSize; j++) {
				if (classDesc->proxyVtable[j] == NULL)
					continue;
#ifndef DIRECT_SEND_PORTAL
				if ((char *) classDesc->proxyVtable[j] <= addr
				    && (char *) (classDesc->proxyVtable[j]) + sizeof(proxycode) > addr) {
					//*method = classDesc->vtableSym[j*3][1];
					//*sig = classDesc->vtableSym[j*3][2];
					*method = classDesc->methodVtable[j]->name;
					*sig = classDesc->methodVtable[j]->signature;
					if (*method == NULL)
						*method = "(null)";
					if (*sig == NULL)
						*sig = "(null)";
					*classInfo = classDesc;
					return 0;
				}
#endif				/* DIRECT_SEND_PORTAL */
			}
		}
	}
	return -1;
}

void addToRefTable(ObjectDesc * src, ObjectDesc * dst)
{
#ifndef NEW_COPY
	ThreadDesc *c = curthr();
	if (c->n_copied == c->max_copied)
		sys_panic("too many objects copied");
	c->copied[c->n_copied].src = src;
	c->copied[c->n_copied].dst = dst;
	c->n_copied++;
#else
	ThreadDesc *c = curthr();
	if (c->n_copied == c->max_copied)
		sys_panic("too many objects copied");
	c->copied[c->n_copied].src = src;
	c->copied[c->n_copied].flags = getObjFlags(src);
	/* set forward pointer to find all objects that are already copied */
	setObjFlags(src, (u4_t) dst | GC_FORWARD);
	c->n_copied++;
#endif
}

// copy either object, portal, or array

#ifdef NEW_COPY
static void correctFlags(DomainDesc * domain, struct copied_s *copied, u4_t n_copied)
{
	ObjectDesc *obj;
	u4_t i;
	for (i = 0; i < n_copied; i++) {
		obj = copied[i].src;
		setObjFlags(obj, copied[i].flags);
	}
}
static void correctFlags2(DomainDesc * domain, struct copied_s *copied, u4_t n_copied)
{
	ObjectDesc *obj;
	u4_t i;
#ifdef DEBUG
	printf("NUMBER FLAGS: %d\n", n_copied);
#endif
	for (i = 0; i < n_copied; i++) {
		obj = copied[i].src;
		setObjFlags(obj, copied[i].flags);
#ifdef DEBUG
		printf("%2d %p %s\n", i, obj, obj2ClassDesc(obj)->name);
#endif
	}
}
#endif

#ifdef NEW_COPY
static char *copy_content_reference_internal(DomainDesc * src, DomainDesc * dst, ObjectDesc * ref, u4_t * quota);
ObjectDesc *copy_reference(DomainDesc * src, DomainDesc * dst, ObjectDesc * ref, u4_t * quota)
{
	ObjectDesc *newobj, *ref0;

	CHECK_STACK_SIZE(src, 128);

	if (src == dst)
		return ref;
	/*restart: */
	curthr()->n_copied = 0;
	dst->gc.setMark(dst);
	ref0 = copy_shallow_reference_internal(src, dst, ref, quota);
	//printf("S: %p\n", ref);
	if (ref0 == 0xffffffff) {
		correctFlags2(dst, curthr()->copied, curthr()->n_copied);	/* correct flags after restarting */
		goto restart;
	}
	for (;;) {
		newobj = dst->gc.atMark(dst);
		if (newobj == NULL)
			break;
		//printf("C: %p\n", newobj);
		if (copy_content_reference_internal(src, dst, newobj, quota) == 0xffffffff) {
			correctFlags2(dst, curthr()->copied, curthr()->n_copied);	/* correct flags after restarting */
			goto restart;
		}
	}
	correctFlags(dst, curthr()->copied, curthr()->n_copied);
	curthr()->n_copied = 0;
	return ref0;
      restart:
	return 0xffffffff;
}
#else
ObjectDesc *copy_reference(DomainDesc * src, DomainDesc * dst, ObjectDesc * ref, u4_t * quota)
{
	CHECK_STACK_SIZE(src, 128);
	return copy_reference_internal(src, dst, ref, quota);
}
#endif

#ifndef NEW_COPY
static ObjectDesc *copy_reference_internal(DomainDesc * src, DomainDesc * dst, ObjectDesc * ref, u4_t * quota)
{
	ClassDesc *refcl;
	u4_t flags, i;
	struct copied_s *c;
	u4_t n;
	ObjectDesc *ret;
#ifdef CHECK_SERIAL_IN_PORTAL
	check_serial();
#endif				/* CHECK_SERIAL_IN_PORTAL */

	if (ref == NULL)
		return NULL;

	/* check if sufficient stack space exist to run this function */
	CHECK_STACK_SIZE(src, 128);

	if (src == dst)
		return ref;

#ifndef NEW_COPY
	c = curthr()->copied;
	n = curthr()->n_copied;
	for (i = 0; i < n; i++) {
		if (c[i].src == ref)
			return c[i].dst;
	}
#else
	flags = getObjFlags(ref);
	if ((flags & FORWARD_MASK) == GC_FORWARD) {
		/* object already copied, this is a forward reference */
		return (ObjectDesc *) (flags & FORWARD_PTR_MASK);
	}
#endif

/* HACK */
#ifndef JAVASCHEDULER
#ifdef DEBUG
	if (!gc_isValidHeapRef(src, ref)) {
		printf("INVALID Reference: %p\n", ref);
		sys_panic("invalref");
	}
#endif
#endif

	pprintf3(" COPY REF -> %p, %d\n", ref, *(((jint *) ref) - 2));
	if (ref != NULL) {
		flags = getObjFlags(ref) & FLAGS_MASK;
		switch (flags) {
		case OBJFLAGS_OBJECT:
			refcl = obj2ClassDesc(ref);
			ASSERTCLASSDESC(refcl);
#ifdef VERBOSE_PORTAL_PARAM_COPY
			printf("   COPY OBJECT %p %s\n", ref, refcl->name);
#endif				/* VERBOSE_PORTAL_PARAM_COPY */
#ifdef NEW_COPY
			return copy_shallow_object(src, dst, ref, quota);
#else
			return copy_object(src, dst, ref, quota);
#endif
		case OBJFLAGS_PORTAL:
#ifdef VERBOSE_PORTAL_PARAM_COPY
			refcl = obj2ClassDesc(ref);
			pprintf3("   COPY PORTAL %p %s\n", ref, refcl->name);
#endif
			return copy_portal(src, dst, (Proxy *) ref, quota, 1);
		case OBJFLAGS_MEMORY:
#ifdef VERBOSE_PORTAL_PARAM_COPY
			refcl = obj2ClassDesc(ref);
			pprintf3("   MEMSLOT %p %s\n", ref, refcl->name);
#endif
			ret = copy_memory(src, dst, (struct MemoryProxy_s *) ref, quota);
			ASSERT(gc_isValidHeapRef(dst, ret));
			return ret;
		case OBJFLAGS_DOMAIN:
#ifdef VERBOSE_PORTAL_PARAM_COPY
			refcl = obj2ClassDesc(ref);
			pprintf3("   DOMAINSLOT %p %s\n", ref, refcl->name);
#endif
			return copy_domainproxy(src, dst, (DomainProxy *) ref, quota);
		case OBJFLAGS_ARRAY:
#ifdef VERBOSE_PORTAL_PARAM_COPY
			refcl = obj2ClassDesc(ref);
			printf("   COPY ARRAY %p %s\n", ref, refcl->name);
#endif				/* VERBOSE_PORTAL_PARAM_COPY */
			return copy_array(src, dst, ref, quota);
		case OBJFLAGS_EXTERNAL_STRING:
		case OBJFLAGS_EXTERNAL_CPUDESC:
			return ref;
		case OBJFLAGS_CPUSTATE:
			return copy_cpustate(src, dst, ref, quota);
		case OBJFLAGS_FOREIGN_CPUSTATE:
			return copy_foreign_cpustate(src, dst, ref, quota);
		case OBJFLAGS_ATOMVAR:{
#ifdef NEVER
				DEPDesc *depObj = curthr()->processingDEP;
				ObjectDesc *obj = (ObjectDesc *) depObj->obj;
				ClassDesc *targetClass = obj2ClassDesc(obj);
				ThreadDesc *source = curthr()->mostRecentlyCalledBy;
				int methodIndex = source->depMethodIndex;
				printf(" %s %s %d %p\n", targetClass->methodVtable[methodIndex]->name,
				       targetClass->methodVtable[methodIndex]->signature,
				       targetClass->methodVtable[methodIndex]->numberOfArgs, c);
				sys_panic("CURRENTLY NO WAY TO COPY ATOMVAR");
#endif
				exceptionHandlerMsg(THROW_RuntimeException, "AtomicVariable cannot be copied between domains");
			}
		default:
#ifdef DEBUG
			{
				ThreadDesc *source;
				DEPDesc *dep;
				int methodIndex;
				ObjectDesc *obj;
				MethodDesc *meth;
				ClassDesc *cl;
				printf("Param copy failed (flags=%d):\n", flags);
				dep = curthr()->processingDEP;
				source = curthr()->mostRecentlyCalledBy;
				methodIndex = source->depMethodIndex;
				obj = (ObjectDesc *) dep->obj;
				printf("Object %p, FLAGS: 0x%x", ref, flags);
				cl = *(ClassDesc **) (((ObjectDesc *) (obj))->vtable - 1);
				meth = cl->methodVtable[methodIndex];
				printf("Method: %s %s\n", meth->name, meth->signature);
				printf("Class: %s\n", cl->name);


				refcl = (*(ClassDesc **)
					 (((ObjectDesc *) (ref))->vtable - 1));
				// printf("Class of failed OBJ: %s\n", refcl->name);
				printf("Class of failed OBJ: %p\n", refcl->name);

			}
#endif
			sys_panic("UNKNOWN OBJECT TYPE 0x%x for object %p", flags, ref);
		}
	} else {
		return NULL;
	}
	return NULL;
}
#else
static ObjectDesc *copy_shallow_reference_internal(DomainDesc * src, DomainDesc * dst, ObjectDesc * ref, u4_t * quota)
{
	ClassDesc *refcl;
	u4_t flags, i;
	struct copied_s *c;
	u4_t n;
	ObjectDesc *ret;
#ifdef CHECK_SERIAL_IN_PORTAL
	check_serial();
#endif				/* CHECK_SERIAL_IN_PORTAL */

	if (ref == NULL)
		return NULL;

	/* check if sufficient stack space exist to run this function */
	CHECK_STACK_SIZE(src, 128);

	if (src == dst)
		return ref;

	flags = getObjFlags(ref);
	if ((flags & FORWARD_MASK) == GC_FORWARD) {
		/* object already copied, this is a forward reference */
		return (ObjectDesc *) (flags & FORWARD_PTR_MASK);
	}

	pprintf3(" COPY REF -> %p, %d\n", ref, *(((jint *) ref) - 2));
	if (ref != NULL) {
		flags = getObjFlags(ref) & FLAGS_MASK;
		switch (flags) {
		case OBJFLAGS_OBJECT:
			refcl = obj2ClassDesc(ref);
#ifdef VERBOSE_PORTAL_PARAM_COPY
			printf("   COPY SHALLOW OBJECT %p %s\n", ref, refcl->name);
#endif				/* VERBOSE_PORTAL_PARAM_COPY */
			return copy_shallow_object(src, dst, ref, quota);
		case OBJFLAGS_PORTAL:
#ifdef VERBOSE_PORTAL_PARAM_COPY
			refcl = obj2ClassDesc(ref);
			printf("   COPY SHALLOW PORTAL %p %s\n", ref, refcl->name);
#endif
			return copy_shallow_portal(src, dst, (Proxy *) ref, quota, 1);
		case OBJFLAGS_MEMORY:
#ifdef VERBOSE_PORTAL_PARAM_COPY
			refcl = obj2ClassDesc(ref);
			printf("   COPY SHALLOW MEMORY %p %s\n", ref, refcl->name);
#endif
			ret = copy_shallow_memory(src, dst, (struct MemoryProxy_s *) ref, quota);
			ASSERT(gc_isValidHeapRef(dst, ret));
			return ret;
		case OBJFLAGS_DOMAIN:
#ifdef VERBOSE_PORTAL_PARAM_COPY
			refcl = obj2ClassDesc(ref);
			printf("   COPY SHALLOW DOMAIN %p %s\n", ref, refcl->name);
#endif
			return copy_shallow_domainproxy(src, dst, (DomainProxy *) ref, quota);
		case OBJFLAGS_ARRAY:
#ifdef VERBOSE_PORTAL_PARAM_COPY
			refcl = obj2ClassDesc(ref);
			printf("   COPY SHALLOW ARRAY %p %s\n", ref, refcl->name);
#endif				/* VERBOSE_PORTAL_PARAM_COPY */
			return copy_shallow_array(src, dst, ref, quota);
		case OBJFLAGS_EXTERNAL_STRING:
#ifdef VERBOSE_PORTAL_PARAM_COPY
			refcl = obj2ClassDesc(ref);
			printf("   NOCOPY SHALLOW  %p %s\n", ref, refcl->name);
#endif
			return ref;
		case OBJFLAGS_CPUSTATE:
#ifdef VERBOSE_PORTAL_PARAM_COPY
			refcl = obj2ClassDesc(ref);
			printf("   COPY SHALLOW CPUSTATE %p %s\n", ref, refcl->name);
#endif				/* VERBOSE_PORTAL_PARAM_COPY */
			return copy_shallow_cpustate(src, dst, ref, quota);
		case OBJFLAGS_FOREIGN_CPUSTATE:
			return copy_shallow_foreign_cpustate(src, dst, ref, quota);
		case OBJFLAGS_ATOMVAR:{
#ifdef NEVER
				DEPDesc *depObj = curthr()->processingDEP;
				ObjectDesc *obj = (ObjectDesc *) depObj->obj;
				ClassDesc *targetClass = obj2ClassDesc(obj);
				ThreadDesc *source = curthr()->mostRecentlyCalledBy;
				int methodIndex = source->depMethodIndex;
				printf(" %s %s %d %p\n", targetClass->methodVtable[methodIndex]->name,
				       targetClass->methodVtable[methodIndex]->signature,
				       targetClass->methodVtable[methodIndex]->numberOfArgs, c);
				sys_panic("CURRENTLY NO WAY TO COPY ATOMVAR");
#endif
				exceptionHandlerMsg(THROW_RuntimeException, "AtomicVariable cannot be copied between domains");
			}
		default:
#ifdef DEBUG
			{
				ThreadDesc *source;
				DEPDesc *dep;
				int methodIndex;
				ObjectDesc *obj;
				MethodDesc *meth;
				ClassDesc *cl;
				printf("Param copy failed (flags=%d):\n", flags);
				dep = curthr()->processingDEP;
				source = curthr()->mostRecentlyCalledBy;
				methodIndex = source->depMethodIndex;
				obj = (ObjectDesc *) dep->obj;
				printf("Object %p, FLAGS: 0x%x", ref, flags);
				cl = *(ClassDesc **) (((ObjectDesc *) (obj))->vtable - 1);
				meth = cl->methodVtable[methodIndex];
				printf("Method: %s %s\n", meth->name, meth->signature);
				printf("Class: %s\n", cl->name);


				refcl = (*(ClassDesc **)
					 (((ObjectDesc *) (ref))->vtable - 1));
				// printf("Class of failed OBJ: %s\n", refcl->name);
				printf("Class of failed OBJ: %p\n", refcl->name);

			}
#endif
			sys_panic("UNKNOWN OBJECT TYPE 0x%x for object %p", flags, ref);
		}
	} else {
		return NULL;
	}
	return NULL;
}
static char *copy_content_reference_internal(DomainDesc * src, DomainDesc * dst, ObjectDesc * ref, u4_t * quota)
{
	ClassDesc *refcl;
	u4_t flags, i;
	struct copied_s *c;
	u4_t n;
	ObjectDesc *ret;
#ifdef CHECK_SERIAL_IN_PORTAL
	check_serial();
#endif				/* CHECK_SERIAL_IN_PORTAL */

	ASSERT(ref != NULL)

	    /* check if sufficient stack space exist to run this function */
	    CHECK_STACK_SIZE(src, 128);

	flags = getObjFlags(ref);
	if ((flags & FORWARD_MASK) == GC_FORWARD) {
		sys_panic("ref should be on traget heap");
	}

	pprintf3(" COPY REF -> %p, %d\n", ref, *(((jint *) ref) - 2));
	switch (flags) {
	case OBJFLAGS_OBJECT:
		refcl = obj2ClassDesc(ref);
#ifdef VERBOSE_PORTAL_PARAM_COPY
		printf("   COPY CONTENT OBJECT %p %s\n", ref, refcl->name);
#endif				/* VERBOSE_PORTAL_PARAM_COPY */
		return copy_content_object(src, dst, ref, quota);
		break;
	case OBJFLAGS_ARRAY:
#ifdef VERBOSE_PORTAL_PARAM_COPY
		refcl = obj2ClassDesc(ref);
		printf("   COPY CONTENT ARRAY %p %s\n", ref, refcl->name);
#endif				/* VERBOSE_PORTAL_PARAM_COPY */
		return copy_content_array(src, dst, ref, quota);
		break;
	case OBJFLAGS_PORTAL:
#ifdef VERBOSE_PORTAL_PARAM_COPY
		refcl = obj2ClassDesc(ref);
		pprintf3("   COPY CONTENT PORTAL %p %s\n", ref, refcl->name);
#endif
		copy_content_portal(src, dst, (Proxy *) ref, quota, 1);
		break;
	case OBJFLAGS_MEMORY:
#ifdef VERBOSE_PORTAL_PARAM_COPY
		refcl = obj2ClassDesc(ref);
		pprintf3("   COPY CONTENT MEMORY %p %s\n", ref, refcl->name);
#endif
		copy_content_memory(src, dst, (struct MemoryProxy_s *) ref, quota);
		break;
	case OBJFLAGS_DOMAIN:
		break;
	case OBJFLAGS_FOREIGN_CPUSTATE:
		break;
	default:
#ifdef DEBUG
		{
			ThreadDesc *source;
			DEPDesc *dep;
			int methodIndex;
			ObjectDesc *obj;
			MethodDesc *meth;
			ClassDesc *cl;
			printf("Param copy failed (flags=%d):\n", flags);
			cl = *(ClassDesc **) (((ObjectDesc *) (obj))->vtable - 1);
			dep = curthr()->processingDEP;
			source = curthr()->mostRecentlyCalledBy;
			if (source) {
				methodIndex = source->depMethodIndex;
				obj = (ObjectDesc *) dep->obj;
				meth = cl->methodVtable[methodIndex];
				printf("Method: %s %s\n", meth->name, meth->signature);
				printf("Class: %s\n", cl->name);
			}
			printf("Object %p, FLAGS: 0x%x", ref, flags);


			refcl = (*(ClassDesc **)
				 (((ObjectDesc *) (ref))->vtable - 1));
			// printf("Class of failed OBJ: %s\n", refcl->name);
			printf("Class of failed OBJ: %p\n", refcl->name);

		}
#endif
		sys_panic("UNKNOWN OBJECT TYPE 0x%x for object %p", flags, ref);
	}
	return NULL;
}
#endif

ClassDesc *portalInterface;

Proxy *portal_auto_promo(DomainDesc * domain, ObjectDesc * obj)
{
	ClassDesc *cl;
	Proxy *proxy;

	ASSERTOBJECT(obj);
	cl = obj2ClassDesc(obj);

#ifdef DBG_AUTO_PORTAL_PROMO
	printf("Auto promotion of object %p of class %s in domain %d, thread %d.%d\n", obj, cl->name, domain->id, TID(curthr()));
	if (curthr()->mostRecentlyCalledBy != NULL) {
		printf("   This is a portal thread processing service %d %s (%s)\n", curthr()->processingDEP->serviceIndex,
		       curthr()->processingDEP->interface->name, obj2ClassDesc(curthr()->processingDEP->obj)->name);
		if (curthr()->processingDEP) {
			if (obj2ClassDesc(curthr()->processingDEP->obj)->inheritServiceThread) {
				printf("INHERIT!!!\n");
			}
		}
	}
#endif
	if (implements_interface(cl, portalInterface)) {
		ClassDesc *ifclass;
		ClassDesc *superclass;
		ThreadDesc *thread;
		Proxy *proxy;
		u4_t i;
		u4_t depIndex;
		DEPDesc *d = NULL;
		// try to find a service description for this object
		LOCK_SERVICETABLE;
		for (i = 0; i < MAX_SERVICES; i++) {
			d = domain->services[i];
			if (d == SERVICE_ENTRY_FREE || d == SERVICE_ENTRY_CHANGING) {
				d = NULL;
				continue;
			}
			if (d->obj == obj) {
#ifdef DBG_AUTO_PORTAL_PROMO
				printf("Reuse proxy: %p for service object %p and service desc %p\n", d->proxy, obj, d);
#endif
				break;
			}
		}
		UNLOCK_SERVICETABLE;
		if (d != NULL)
			return d->proxy;


		// find portal interface
		ifclass = NULL;
		superclass = cl;
		do {
#ifdef DBG_AUTO_PORTAL_PROMO
			printf("  CL: %s\n", superclass->name);
#endif
			for (i = 0; i < superclass->numberOfInterfaces; i++) {
#ifdef DBG_AUTO_PORTAL_PROMO
				printf("  IF: %s\n", superclass->interfaces[i]->name);
#endif
				if (implements_interface(superclass->interfaces[i], portalInterface)) {
					// found a portal interface
					if (ifclass != NULL && ifclass != superclass->interfaces[i]) {
						printf
						    ("Class %s [problem in superclass %s] implements more than one portal interface: %s and %s\n",
						     cl->name, superclass->name, ifclass->name, superclass->interfaces[i]->name);
						/* check whether the portal interfaces are in an inheritance relation */
						/* if yes use the subtype; otherwise throw exception */
						if (check_assign(ifclass, superclass->interfaces[i])) {
							ifclass = superclass->interfaces[i];
						} else if (check_assign(superclass->interfaces[i], ifclass)) {
							// do not change ifclass
						} else {
							exceptionHandlerMsg(THROW_RuntimeException,
									    "Class implements more than one portal interface");
						}
					} else {
						ifclass = superclass->interfaces[i];
					}
				}
			}
		} while ((superclass = superclass->superclass) != NULL);


		DISABLE_IRQ;
#ifdef NEW_PORTALCALL
		{
			ServiceThreadPool *pool;
			if (curthr()->processingDEP && obj2ClassDesc(curthr()->processingDEP->obj)->inheritServiceThread) {
				printf("INHERIT!!!\n");
				pool = curthr()->processingDEP->pool;
			} else {
				int i, index;
				pool = allocServicePoolInDomain(domain);
				index = -1;
				for (i = 0; i < MAX_SERVICES; i++) {
					if (domain->pools[i] == NULL) {
						domain->pools[i] = pool;
						index = i;
						break;
					}
				}
				if (index == -1)
					exceptionHandlerMsg(THROW_RuntimeException, "no free pool slot");
				pool->index = index;
				printf("INITPOOL %d %d=%p\n", domain->id, index, domain->pools[index]);
				createServiceThread(domain, index, ifclass->name);
			}
			depIndex = createService(domain, obj, ifclass, pool);
			proxy = createPortalInDomain(domain, ifclass, domain, domain->id, depIndex);
			domain->services[depIndex]->proxy = proxy;
		}
#else
		depIndex = createService(domain, obj, ifclass);
		proxy = createPortalInDomain(domain, ifclass, domain, domain->id, depIndex);
		domain->services[depIndex]->proxy = proxy;
		thread = createThread(domain, receive_dep, (void *) depIndex, STATE_RUNNABLE, SCHED_CREATETHREAD_DEFAULT);
		setThreadName(thread, "SVC-", ifclass->name);
		portal_set_recv(domain->services[depIndex], thread);
#endif
		RESTORE_IRQ;


#ifdef DBG_AUTO_PORTAL_PROMO
		printf("Created proxy: %p for service object %p and service %d\n", proxy, obj, depIndex);
#ifndef NEW_PORTALCALL
		printf("  and started thread %p\n", thread);
#endif
#endif
		proxy->targetDomain = domain;
		proxy->targetDomainID = domain->id;
		proxy->index = depIndex;
		return proxy;
	}
	sys_panic("DOES NOT IMPLEMENT PORTAL INTERFACE");
}

/* restart copy operation after GC */

void restartCopy0(u4_t * sp)
{
	u4_t *ebp, *eip;
	ThreadDesc *thread = curthr();
	while (sp > thread->stack && sp < thread->stackTop) {
		ebp = (u4_t *) * sp++;
		eip = (u4_t *) * sp++;
		printf("%p\n", eip);
		if (eip == 0x80555f5) {
			eip -= 4;
		}
/*
		if (in_copyreturn(eip)) {
			sys_panic("RESTART1");
		}
*/
		sp = ebp;
	}
	sys_panic("RESTART");
}

char *restartCopy()
{
/*
	register u4_t *sp;
	asm volatile ("movl %%ebp, %%eax; movl (%%eax), %0":"=r" (sp));
	restartCopy0(sp);
*/
#ifdef DEBUG
	printf("RESTART COPY\n");
	printStackTraceNew("RESTART");
#endif
	return 0xffffffff;
}


// copy object
#ifndef NEW_COPY
ObjectDesc *copy_object(DomainDesc * src, DomainDesc * dst, ObjectDesc * obj, u4_t * quota)
{
	jbyte *addr;
	jint k;
	jbyte b = 0;
	ClassDesc *cl = NULL;
	ObjectDesc *targetObj, *ref;
	u4_t depIndex;
	u4_t gcEpoch;

	/* check if sufficient stack space exist to run this function */
	CHECK_STACK_SIZE(src, 128);

	ASSERTOBJECT(obj);
	cl = obj2ClassDesc(obj);

	if (implements_interface(cl, portalInterface)) {
		Proxy *proxy = portal_auto_promo(src, obj);
		/*  check if proxy type can be substituted for obj! */
		// guarantee that destination domain does not contain service class
		if (findClass(dst, cl->name) != NULL) {
			printf("Target domain %d (%s) has loaded class %s\n", dst->id, dst->domainName, cl->name);
			exceptionHandlerMsg(THROW_RuntimeException, "Target domain has loaded service class");
		}
		return copy_portal(src, dst, proxy, quota, 1);
	}

	{
		u4_t objsize = OBJSIZE_OBJECT(cl->instanceSize);
		if (*quota < objsize)
			sys_panic("");
		*quota -= objsize;
	}

	gcEpoch = dst->gc.epoch;
	targetObj = allocObjectInDomain(dst, cl);
	if (gcEpoch != dst->gc.epoch)
		return restartCopy();
	addToRefTable(obj, targetObj);

#ifdef COPY_STATISTICS
	cl->copied++;
#endif				/* COPY_STATISTICS */

	addr = cl->map;
	for (k = 0; k < cl->instanceSize; k++) {
		if (k % 8 == 0)
			b = *addr++;
		if (b & 1) {
			/* reference slot */
			ref = copy_reference_internal(src, dst, (ObjectDesc *) obj->data[k], quota);
			if (ref == 0xffffffff) {
				return 0xffffffff;
			}
			targetObj->data[k] = (jint) ref;
		} else {
#ifdef DBG_DEP
			pprintf2("   NUMSLOT %d\n", obj->data[k]);
#endif
			targetObj->data[k] = obj->data[k];
		}
		b >>= 1;
	}
	return targetObj;
}
#else
ObjectDesc *copy_shallow_object(DomainDesc * src, DomainDesc * dst, ObjectDesc * obj, u4_t * quota)
{
	jbyte *addr;
	jint k;
	jbyte b = 0;
	ClassDesc *cl = NULL;
	ObjectDesc *targetObj, *ref;
	u4_t depIndex;
	u4_t gcEpoch;

	/* check if sufficient stack space exist to run this function */
	CHECK_STACK_SIZE(src, 128);

	ASSERTOBJECT(obj);
	cl = obj2ClassDesc(obj);

	if (implements_interface(cl, portalInterface)) {
		Proxy *proxy = portal_auto_promo(src, obj);
		/*  check if proxy type can be substituted for obj! */
		// guarantee that destination domain does not contain the service class
		if (findClass(dst, cl->name) != NULL) {
			printf("Target domain %d (%s) has loaded class %s\n", dst->id, dst->domainName, cl->name);
			exceptionHandlerMsg(THROW_RuntimeException, "Target domain has loaded service class");
		}
		return copy_shallow_portal(src, dst, proxy, quota, 1);
	}

	{
		u4_t objsize = OBJSIZE_OBJECT(cl->instanceSize);
		if (*quota < objsize)
			sys_panic("");
		*quota -= objsize;
	}

	gcEpoch = dst->gc.epoch;
	targetObj = allocObjectInDomain(dst, cl);
	if (gcEpoch != dst->gc.epoch)
		return restartCopy();
	addToRefTable(obj, targetObj);
	memcpy(targetObj->data, obj->data, cl->instanceSize * 4);
#ifdef COPY_STATISTICS
	cl->copied++;
#endif				/* COPY_STATISTICS */

	return targetObj;
}

char *copy_content_object(DomainDesc * src, DomainDesc * dst, ObjectDesc * obj, u4_t * quota)
{
	jbyte *addr;
	jint k;
	jbyte b = 0;
	ClassDesc *cl = NULL;
	ObjectDesc *ref;
	u4_t depIndex;
	u4_t gcEpoch;

	cl = obj2ClassDesc(obj);

	addr = cl->map;
	for (k = 0; k < cl->instanceSize; k++) {
		if (k % 8 == 0)
			b = *addr++;
		if (b & 1) {
			/* reference slot */
			ref = copy_shallow_reference_internal(src, dst, (ObjectDesc *) obj->data[k], quota);
			if (ref == 0xffffffff) {
				/*
				   for (k = 0; k < cl->instanceSize; k++) {
				   obj->data[k] = NULL;
				   }
				 */
				return 0xffffffff;
			}
			obj->data[k] = (jint) ref;
		} else {
#ifdef DBG_DEP
			pprintf2("   NUMSLOT %d\n", obj->data[k]);
#endif
			/* already copied in copy_shallow */
		}
		b >>= 1;
	}
	return NULL;
}
#endif

#ifndef NEW_COPY
ObjectDesc *copy_portal(DomainDesc * src, DomainDesc * dst, Proxy * obj, u4_t * quota, jboolean addRef)
{
	ClassDesc *c;
	Proxy *o;
	u4_t gcEpoch;

	if (obj == NULL)
		return NULL;

	if ((obj->targetDomainID == dst->id) && !(obj->targetDomainID == 0 && obj->targetDomain == NULL && obj->index == 0)) {
		ObjectDesc *tobj = dst->services[obj->index]->obj;
#ifdef DBG_DEP
		printf("COPYPORTALOBJ: %p (%s) -> %p (%s)\n", obj, obj2ClassDesc(obj)->name, tobj, obj2ClassDesc(tobj)->name);
#endif				/* DBG_DEP */
		return tobj;
	}

	c = obj2ClassDesc(obj);

#ifdef PORTAL_TRANSFER_INTERCEPTOR
	if (curdom()->portalInterceptorThread) {
		ClassDesc *cl;
		int ret;
		ArrayDesc *paramArray;
		u4_t quota = 1000;	/*  4 kB portal parameter quota , new quota for each new call */
		ThreadDesc *t = curthr();
		InterceptPortalInfoProxy *info = NULL;

		info = allocInterceptPortalInfoProxyInDomain(curdom()->portalInterceptorThread->domain);
		info->domain = obj->targetDomain;
		info->index = obj->index;
		ASSERTTHREAD(curdom()->portalInterceptorThread);
		ASSERTOBJECT(curdom()->portalInterceptorObject);
		ASSERT(curdom()->createPortalInterceptorCode != NULL);

		DISABLE_IRQ;
		ret =
		    start_thread_using_code1(curdom()->portalInterceptorObject, curdom()->portalInterceptorThread,
					     curdom()->createPortalInterceptorCode, info);

		RESTORE_IRQ;

		//     printf("return of inboundInterceptor: %d\n", ret);
		if (ret == JNI_FALSE) {
			exceptionHandler(-1);
		}

	}
#endif				/*  PORTAL_TRANSFER_INTERCEPTOR */

	{
		u4_t objsize = OBJSIZE_PORTAL;
		if (*quota < objsize)
			sys_panic("");
		*quota -= objsize;
	}
	gcEpoch = dst->gc.epoch;
	o = allocProxyInDomain(dst, NULL, obj->targetDomain, obj->targetDomainID, obj->index);
	if (gcEpoch != dst->gc.epoch)
		return restartCopy();
	if (addRef)
		addToRefTable(obj, o);
	o->vtable = obj->vtable;
#ifdef DBG_DEP
	pprintf3("  copied portal = %p (%s)\n", o, c->name);
#endif
	return (ObjectDesc *) o;
}
#else
ObjectDesc *copy_shallow_portal(DomainDesc * src, DomainDesc * dst, Proxy * obj, u4_t * quota, jboolean addRef)
{
	ClassDesc *c;
	Proxy *o;
	u4_t gcEpoch;

	if (obj == NULL)
		return NULL;

	if ((obj->targetDomainID == dst->id) && !(obj->targetDomainID == 0 && obj->targetDomain == NULL && obj->index == 0)) {
		ObjectDesc *tobj = dst->services[obj->index]->obj;
#ifdef DBG_DEP
		printf("COPYPORTALOBJ: %p (%s) -> %p (%s)\n", obj, obj2ClassDesc(obj)->name, tobj, obj2ClassDesc(tobj)->name);
#endif				/* DBG_DEP */
		return tobj;
	}

	c = obj2ClassDesc(obj);

#ifdef PORTAL_TRANSFER_INTERCEPTOR
	if (curdom()->portalInterceptorThread) {
		ClassDesc *cl;
		int ret;
		ArrayDesc *paramArray;
		u4_t quota = 1000;	/*  4 kB portal parameter quota , new quota for each new call */
		ThreadDesc *t = curthr();
		InterceptPortalInfoProxy *info = NULL;

		info = allocInterceptPortalInfoProxyInDomain(curdom()->portalInterceptorThread->domain);
		info->domain = obj->targetDomain;
		info->index = obj->index;
		ASSERTTHREAD(curdom()->portalInterceptorThread);
		ASSERTOBJECT(curdom()->portalInterceptorObject);
		ASSERT(curdom()->createPortalInterceptorCode != NULL);

		DISABLE_IRQ;
		ret =
		    start_thread_using_code1(curdom()->portalInterceptorObject, curdom()->portalInterceptorThread,
					     curdom()->createPortalInterceptorCode, info);

		RESTORE_IRQ;

		//     printf("return of inboundInterceptor: %d\n", ret);
		if (ret == JNI_FALSE) {
			exceptionHandler(-1);
		}

	}
#endif				/*  PORTAL_TRANSFER_INTERCEPTOR */

	{
		u4_t objsize = OBJSIZE_PORTAL;
		if (*quota < objsize)
			sys_panic("");
		*quota -= objsize;
	}
	gcEpoch = dst->gc.epoch;
	o = allocProxyInDomain(dst, NULL, obj->targetDomain, obj->targetDomainID, obj->index);
	if (gcEpoch != dst->gc.epoch)
		return restartCopy();
	if (addRef)
		addToRefTable(obj, o);
	o->vtable = obj->vtable;
#ifdef DBG_DEP
	pprintf3("  copied portal = %p (%s)\n", o, c->name);
#endif
	return (ObjectDesc *) o;
}

ObjectDesc *copy_content_portal(DomainDesc * src, DomainDesc * dst, Proxy * obj, u4_t * quota, jboolean addRef)
{
	return obj;
}

#endif

#ifndef NEW_COPY
ObjectDesc *copy_domainproxy(DomainDesc * src, DomainDesc * dst, DomainProxy * obj, u4_t * quota)
{
	DomainProxy *o;
	u4_t gcEpoch;

	if (obj == NULL)
		return NULL;
	if (src == dst)
		return obj;

	{
		u4_t objsize = OBJSIZE_DOMAIN;
		if (*quota < objsize)
			sys_panic("");
		*quota -= objsize;
	}

	gcEpoch = dst->gc.epoch;
	o = allocDomainProxyInDomain(dst, obj->domain, obj->domainID);
	if (gcEpoch != dst->gc.epoch)
		return restartCopy();
	addToRefTable(obj, o);
	o->vtable = obj->vtable;
#ifdef DBG_DEP
	pprintf2("  copied domain = %p \n", o);
#endif
	return (ObjectDesc *) o;
}
#else
ObjectDesc *copy_shallow_domainproxy(DomainDesc * src, DomainDesc * dst, DomainProxy * obj, u4_t * quota)
{
	DomainProxy *o;
	u4_t gcEpoch;

	if (obj == NULL)
		return NULL;
	if (src == dst)
		return obj;

	{
		u4_t objsize = OBJSIZE_DOMAIN;
		if (*quota < objsize)
			sys_panic("");
		*quota -= objsize;
	}

	gcEpoch = dst->gc.epoch;
	o = allocDomainProxyInDomain(dst, obj->domain, obj->domainID);
	if (gcEpoch != dst->gc.epoch)
		return restartCopy();
	addToRefTable(obj, o);
	o->vtable = obj->vtable;
#ifdef DBG_DEP
	pprintf2("  copied domain = %p \n", o);
#endif
	return (ObjectDesc *) o;
}

void copy_content_domainproxy(DomainDesc * src, DomainDesc * dst, DomainProxy * obj, u4_t * quota)
{
}
#endif

#ifndef NEW_COPY
ObjectDesc *copy_foreign_cpustate(DomainDesc * src, DomainDesc * dst, ThreadDescForeignProxy * obj, u4_t * quota)
{
	sys_panic("copy foreign cpustate not yet implemented");
}
#else
ObjectDesc *copy_shallow_foreign_cpustate(DomainDesc * src, DomainDesc * dst, ThreadDescForeignProxy * obj, u4_t * quota)
{
	sys_panic("copy foreign cpustate not yet implemented");
}

ObjectDesc *copy_content_foreign_cpustate(DomainDesc * src, DomainDesc * dst, ThreadDescForeignProxy * obj, u4_t * quota)
{
	sys_panic("copy foreign cpustate not yet implemented");
}
#endif

#ifndef NEW_COPY
ObjectDesc *copy_cpustate(DomainDesc * src, DomainDesc * dst, ThreadDescProxy * obj, u4_t * quota)
{
	ThreadDescForeignProxy *o;
	ClassDesc *c = obj2ClassDesc(obj);
	u4_t gcEpoch;

	if (obj == NULL)
		return NULL;
	if (src == dst)
		return obj;

	{
		u4_t objsize = OBJSIZE_FOREIGN_THREADDESC;
		if (*quota < objsize)
			sys_panic("");
		*quota -= objsize;
	}

	gcEpoch = dst->gc.epoch;
	o = allocThreadDescForeignProxyInDomain(dst, obj);
	if (gcEpoch != dst->gc.epoch)
		return restartCopy();

	addToRefTable(obj, o);
	o->vtable = obj->vtable;
#ifdef DBG_DEP
	pprintf2("  copied cpustate = %p \n", o);
#endif
	return (ObjectDesc *) o;
}
#else
ObjectDesc *copy_shallow_cpustate(DomainDesc * src, DomainDesc * dst, ThreadDescProxy * obj, u4_t * quota)
{
	ThreadDescForeignProxy *o;
	ClassDesc *c = obj2ClassDesc(obj);
	u4_t gcEpoch;

	if (obj == NULL)
		return NULL;
	if (src == dst)
		return obj;

	{
		u4_t objsize = OBJSIZE_FOREIGN_THREADDESC;
		if (*quota < objsize)
			sys_panic("");
		*quota -= objsize;
	}

	gcEpoch = dst->gc.epoch;
	o = allocThreadDescForeignProxyInDomain(dst, obj);
	if (gcEpoch != dst->gc.epoch)
		return restartCopy();

	addToRefTable(obj, o);
	o->vtable = obj->vtable;
#ifdef DBG_DEP
	pprintf2("  copied cpustate = %p \n", o);
#endif
	return (ObjectDesc *) o;
}

ObjectDesc *copy_content_cpustate(DomainDesc * src, DomainDesc * dst, ThreadDescProxy * obj, u4_t * quota)
{
	return obj;
}
#endif

#ifndef NEW_COPY
ObjectDesc *copy_array(DomainDesc * src, DomainDesc * dst, ObjectDesc * obj, u4_t * quota)
{
	ArrayClassDesc *c;
	ArrayDesc *o;
	ArrayDesc *ar;
	//c = (ArrayClassDesc*)obj2ClassDesc(obj);
	u4_t gcEpoch;

	ASSERT(obj != NULL);

	o = (ArrayDesc *) obj;
	c = (ArrayClassDesc *) o->arrayClass;
	{
		u4_t objsize = gc_objSize(obj);
		if (*quota < objsize)
			sys_panic("PER-CALL QUOTA REACHED: TOO MANY OBJECTS COPIED DURING PORTAL CALL");
		*quota -= objsize;
	}
	gcEpoch = dst->gc.epoch;
	ar = allocArrayInDomain(dst, c->elementClass, o->size);
	if (gcEpoch != dst->gc.epoch)
		return restartCopy();
	addToRefTable(obj, ar);

#ifdef COPY_STATISTICS
	c->copied++;
	c->copied_arrayelements += o->size;
#endif				/* COPY_STATISTICS */

	pprintf4("CP: %p \"%s\" \"%s\"  \n", o, c->name, c->elementClass->name);
	if (c->name[1] == 'L') {
		/* reference array */
		u4_t i;
		u4_t *ref;
		pprintf1("copy refarray\n");
		for (i = 0; i < o->size; i++) {
			pprintf2("    %p -> ", o->data[i]);
			ref = copy_reference_internal(src, dst, o->data[i], quota);
			if (ref == 0xffffffff) {
				for (i = 0; i < o->size; i++) {
					ar->data[i] = NULL;
				}
				return 0xffffffff;
			}
			ar->data[i] = ref;
			pprintf2("    %p \n ", ar->data[i]);
		}
#ifdef ALL_ARRAYS_32BIT
	} else if (ARRAY_8BIT(c)) {
		pprintf1("copy 8bit array\n");
		memcpy(ar->data, o->data, o->size);
	} else if (ARRAY_16BIT(c)) {
		pprintf1("copy 16bit array\n");
		memcpy(ar->data, o->data, o->size * 2);
#endif
	} else {
		pprintf1("copy 32bit array\n");
		memcpy(ar->data, o->data, o->size * 4);
	}
	pprintf3(" CP%p -> %p\n", o, ar);
	return (ObjectDesc *) ar;
}
#else
ObjectDesc *copy_shallow_array(DomainDesc * src, DomainDesc * dst, ObjectDesc * obj, u4_t * quota)
{
	ArrayClassDesc *c;
	ArrayDesc *o;
	ArrayDesc *ar;
	//c = (ArrayClassDesc*)obj2ClassDesc(obj);
	u4_t gcEpoch;

	ASSERT(obj != NULL);

	o = (ArrayDesc *) obj;
	c = (ArrayClassDesc *) o->arrayClass;
	{
		u4_t objsize = gc_objSize(obj);
		if (*quota < objsize)
			sys_panic("PER-CALL QUOTA REACHED: TOO MANY OBJECTS COPIED DURING PORTAL CALL");
		*quota -= objsize;
	}
	gcEpoch = dst->gc.epoch;
	ar = allocArrayInDomain(dst, c->elementClass, o->size);
	if (gcEpoch != dst->gc.epoch)
		return restartCopy();
	addToRefTable(obj, ar);

#ifdef COPY_STATISTICS
	c->copied++;
	c->copied_arrayelements += o->size;
#endif				/* COPY_STATISTICS */

#ifdef ALL_ARRAYS_32BIT
	memcpy(ar->data, o->data, o->size * 4);
#else
	sys_panic("shallow copy <32bit array not implemented");
#endif
	pprintf3(" CP%p -> %p\n", o, ar);
	return (ObjectDesc *) ar;
}

char *copy_content_array(DomainDesc * src, DomainDesc * dst, ObjectDesc * obj, u4_t * quota)
{
	ArrayClassDesc *c;
	ArrayDesc *o;

	o = (ArrayDesc *) obj;
	c = (ArrayClassDesc *) o->arrayClass;
	pprintf4("CP: %p \"%s\" \"%s\"  \n", o, c->name, c->elementClass->name);
	if (c->name[1] == 'L') {
		/* reference array */
		u4_t i;
		u4_t *ref;
		pprintf1("copy refarray\n");
		for (i = 0; i < o->size; i++) {
			pprintf2("    %p -> ", o->data[i]);
			ref = copy_shallow_reference_internal(src, dst, o->data[i], quota);
			if (ref == 0xffffffff) {
				return 0xffffffff;
			}
			o->data[i] = ref;
			pprintf2("    %p \n ", o->data[i]);
		}
	}
	return NULL;
}
#endif

void receive_portal_init(DEPDesc * svc)
{
	receive_portalcall(svc->serviceIndex);
}

void reinit_service_thread()
{
	jint *sp;
	DEPDesc *svc = curthr()->processingDEP;
	ThreadDesc *thread = curthr();

	sp = thread->stackTop;
	stack_push(&sp, svc);
	stack_push(&sp, thread_exit);
	thread->context[PCB_ESP] = sp;
	thread->context[PCB_EBP] = NULL;
	thread->context[PCB_EIP] = receive_portal_init;
#ifdef KERNEL
	thread->context[PCB_EFLAGS] &= ~0x00000200;
#else
	sigaddset(&(thread->sigmask), SIGALRM);
#endif

	/* finally activate ourself from the TCB and notify sender */
	Sched_portal_destroy_handoff_to_sender(curthr()->mostRecentlyCalledBy);
}

void service_incRefcount(DEPDesc * p)
{
#ifdef SMP
	u4_t refcount;
	do {
		refcount = p->refcount;
	} while (!cas(&p->refcount, refcount, refcount + 1));
	// FIXME: TODO: GC SERVICE!
#else
	DISABLE_IRQ;
	p->refcount++;
	RESTORE_IRQ;
#endif
}

#ifdef NOTIFY_SERVICE_CLEANUP
/* TODO: GC must trace parameters!!! */
void start_notify_thread(void *dummy)
{
	//ObjectHandle svcObj = dummy;
	ObjectDesc *obj = dummy;
	ClassDesc *svcClass;
	MethodDesc *m;

	DISABLE_IRQ;
	//obj = *svcObj;
	svcClass = obj2ClassDesc(obj);

	//unregisterObject(curdom(), svcObj);

	if ((m = findMethod(curdom(), svcClass->name, "serviceFinalizer", "()V")) != NULL) {
		executeSpecial(curdom(), svcClass->name, "serviceFinalizer", "()V", obj, NULL, 0);
	}

	RESTORE_IRQ;

}
#endif

void service_decRefcount(DomainDesc * domain, u4_t index)
{
	ObjectHandle svcObj;
#ifdef SMP
	u4_t refcount;
	do {
		refcount = p->refcount;
	} while (!cas(&p->refcount, refcount, refcount - 1));
	// FIXME: TODO: GC SERVICE!
#else
	DEPDesc *p = domain->services[index];
	DISABLE_IRQ;
	//svcObj = registerObject(domain, p->obj);

	p->refcount--;
#ifdef SERVICE_EAGER_CLEANUP
	if (p->refcount == 1) {
		printf("DELETE SERVICE %s\n", obj2ClassDesc(p->obj)->name);
		/* delete service (service object will stay on the heap as garbage) */
		p->refcount = 0;
#ifdef NEW_PORTALCALL
		disconnectServiceFromPool(p);
#else
		terminateThread(p->receiver);
#endif
		domain->services[index] = SERVICE_ENTRY_FREE;
	}
#ifdef NOTIFY_SERVICE_CLEANUP
	{
		ThreadDesc *thread;
		/* check whether there is a finalizer method */
		ClassDesc *svcClass = obj2ClassDesc(p->obj);
		if (findMethod(curdom(), svcClass->name, "serviceFinalizer", "()V") != NULL) {
			/* use a new thread to notify the domain that a service was deleted */
			thread = createThread(domain, start_notify_thread, p->obj /*svcObj */ , STATE_RUNNABLE,
					      SCHED_CREATETHREAD_DEFAULT);
			setThreadName(thread, "ServiceFinalizer", NULL);
		}
	}
#else
	//unregisterObject(curdom(), svcObj);
#endif				/* NOTIFY_SERVICE_CLEANUP */
#endif				/* SERVICE_EAGER_CLEANUP */
	RESTORE_IRQ;
#endif
}

#ifdef NEW_PORTALCALL
void connectServiceToPool(DEPDesc * svc, ServiceThreadPool * pool)
{
	ASSERTCLI;
	pool->refcount++;
	svc->pool = pool;
}

void disconnectServiceFromPool(DEPDesc * svc)
{
	ASSERTCLI;
	svc->pool->refcount--;
	if (svc->pool->refcount == 0) {
		ThreadDesc *t = svc->pool->firstReceiver;
		while (t) {
			ThreadDesc *next = t->nextInReceiveQueue;
			terminateThread(t);
			t = next;
		}
	}
}
#endif


void portals_init()
{
#ifdef PROFILE_EVENT_PORTAL
	event_send_start = createNewEvent("PORTAL_SEND_START");
	event_send_block = createNewEvent("PORTAL_SEND_BLOCK");
	event_send_end = createNewEvent("PORTAL_SEND_END");
	event_send_return = createNewEvent("PORTAL_SEND_RETURN");
	event_send_handoff_to_recv = createNewEvent("PORTAL_SEND_HANDOFF_TO_RECV");
	event_send_returned_from_recv = createNewEvent("PORTAL_SEND_RETURNED_FROM_RECV");
	event_receive_start = createNewEvent("PORTAL_RECEIVE_START");
	event_receive_handoff_sender = createNewEvent("PORTAL_RECEIVE_HANDOFF_SENDER");
	event_receive_finished = createNewEvent("PORTAL_RECEIVE_FINISHED");
	event_receive_start_exec = createNewEvent("PORTAL_RECEIVE_START_EXEC");
	event_receive_end_exec = createNewEvent("PORTAL_RECEIVE_END_EXEC");
#endif
}
