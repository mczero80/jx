/********************************************************************************
 * Portal handling
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifndef PORTAL_H
#define PORTAL_H

#ifdef ASSEMBLER

#define DEPFLAG_NONE     $0
#define DEPFLAG_REDO     $1
#define DEPFLAG_NOT_REDO $0xfffffffe


/* these offsets must conform to the DEPDesc structure !! */
/* obviously not used anymore, deactivated and possibly borken by gc changes 
#define DEP_FLAGS          (0x0 + 4 * XMOFF)
#define DEP_FIRSTRECEIVER  (0x4 + 4 * XMOFF) 
*/

#else				/* ASSEMBLER */
#include "object.h"


#define DEBUG_ALL   1
#define DEBUG_NO  0

#define PORTAL_DEBUG_LEVEL DEBUG_NO


#if(PORTAL_DEBUG_LEVEL == DEBUG_NO)
#define portal_dprintf(args...)
#else
#ifdef SMP
#define portal_dprintf(args...)   do{printf("CPU%d: ",get_processor_id()); printf(args);}while (0);
#else
#define portal_dprintf(args...)   printf(args)
#endif
#endif



typedef jint(*dep_f) (jint * paramlist);


typedef struct {
	char *name;
	char *signature;
	code_t code;
} MethodInfoDesc;

struct ThreadDesc_s;
struct DomainDesc_s;
struct Proxy_s;

/* used during linking of DomainZero´s DEPs */
typedef struct DEPTypeDesc_s {
	char *type;
	jint numMethods;
	char **methods;
	code_t code;
} DEPTypeDesc;

typedef struct ServiceThreadPool_s {
#ifdef USE_QMAGIC
	u4_t magic;
#endif
	u4_t flags;
	u4_t refcount;
	struct ThreadDesc_s *firstReceiver;
	struct ThreadDesc_s *firstWaitingSender;	
	struct ThreadDesc_s *lastWaitingSender;
	u4_t index;
} ServiceThreadPool;

typedef struct DEPDesc_s {
#ifdef USE_QMAGIC
	u4_t magic;
#endif
	u4_t flags;
#ifdef NEW_PORTALCALL
	struct ServiceThreadPool_s *pool;
#else
	struct ThreadDesc_s *firstWaitingReceiver;
	struct ThreadDesc_s *firstWaitingSender;
	struct ThreadDesc_s *lastWaitingSender;
	struct ThreadDesc_s *lastWaitingReceiver;
	struct ThreadDesc_s *receiver;
#endif /* NEW_PORTALCALL */
	volatile u4_t lock;
	struct DomainDesc_s *domain;
	ObjectDesc *obj;
	struct Proxy_s *proxy; /* used as prototype for copy in other domain */

	ClassDesc *interface;
	volatile u4_t valid;
	volatile u4_t refcount;
	volatile u4_t serviceIndex;
	volatile u4_t abortFlag;
#ifdef PORTAL_STATISTICS
	u4_t statistics_no_receiver;
	u4_t statistics_handoff;
#endif
} DEPDesc;

typedef DEPDesc Portal;

#ifdef USE_QMAGIC
#define MAGIC_DEP 0xbabeface
#endif
#if defined (NORMAL_MAGIC) && defined(USE_QMAGIC)
#define ASSERTDEP(x) ASSERT(x->magic==MAGIC_DEP)
#else
#define ASSERTDEP(x)
#endif

/* same structure as ObjectDesc */
typedef struct Proxy_s {
	code_t *vtable;
	struct DomainDesc_s *targetDomain;
	u4_t targetDomainID;
	u4_t index;
} Proxy;



typedef struct CPUStateProxy_s {
	code_t *vtable;
	struct ThreadDesc_s *cpuState;
} CPUStateProxy;

typedef struct AtomicVariableProxy_s {
	code_t *vtable;
	ObjectDesc *value;
	struct ThreadDesc_s *blockedThread;
	int listMode;
} AtomicVariableProxy;

typedef struct CASProxy_s {
	code_t *vtable;
	u4_t index;
} CASProxy;

typedef struct VMObjectProxy_s {
	code_t *vtable;
	struct DomainDesc_s *domain;
	u4_t domain_id;
	u4_t epoch;
	int type;
	ObjectDesc *obj;
	int subObjectIndex;
} VMObjectProxy;

typedef struct CredentialProxy_s {
	code_t *vtable;
	u4_t signerDomainID;
	ObjectDesc *value;
} CredentialProxy;

typedef struct DomainProxy_s {
	code_t *vtable;
	struct DomainDesc_s *domain;
	u4_t domainID;
} DomainProxy;

typedef struct InterceptOutboundInfo_s {
	struct DomainDesc_s *source;
	struct DomainDesc_s *target;
	struct MethodDesc_s *method;
	ObjectDesc *obj;
	ArrayDesc *paramlist;
} InterceptOutboundInfo;

typedef struct InterceptInboundInfoProxy_s {
	code_t *vtable;
	struct DomainDesc_s *source;
	struct DomainDesc_s *target;
	ObjectDesc *method;
	ObjectDesc *obj;
	jint *paramlist;
	int index;
} InterceptInboundInfoProxy;

typedef struct InterceptPortalInfoProxy_s {
	code_t *vtable;
	/*  struct DomainDesc_s *source;
	   struct DomainDesc_s *target;
	   ObjectDesc *method;
	   ObjectDesc *obj;
	   jint *paramlist;
	   int index;
	 */
	struct DomainDesc_s *domain;
	u4_t index;
} InterceptPortalInfoProxy;

#define LOCK_PORTAL(portal)           DISABLE_IRQ
#define UNLOCK_PORTAL(portal)         RESTORE_IRQ
//#define PLAIN_UNLOCK_PORTAL(portal)   PLAIN_RESTORE_IRQ

#define LOCK_SERVICETABLE           DISABLE_IRQ
#define UNLOCK_SERVICETABLE         RESTORE_IRQ
//#define PLAIN_UNLOCK_SERVICETABLE   PLAIN_RESTORE_IRQ


#if DEBUG && ! KERNEL
#define ASSERTNOCLI(maskptr)  if (curdom() != domainZero && ! curdom()->advancingThreads && sigismember(maskptr, SIGALRM)) sys_panic("timer disabled?");
#define ASSERTNOCLI1  { sigset_t set, oldset; sigemptyset(&set); sigprocmask(SIG_BLOCK, &set, &oldset); if (curdom() != domainZero && ! curdom()->advancingThreads && sigismember(&oldset, SIGALRM)) sys_panic("timer disabled?");}
#else
#define ASSERTNOCLI(maskptr)
#define ASSERTNOCLI1
#endif

struct DomainDesc_s;

/* Prototypes */

void portals_init();
#ifdef NEW_PORTALCALL
u4_t createService(struct DomainDesc_s * domain, ObjectDesc * depObj, ClassDesc * interface, ServiceThreadPool *pool);
#else
u4_t createService(struct DomainDesc_s * domain, ObjectDesc * depObj, ClassDesc * interface);
#endif
Proxy *createPortalInDomain(struct DomainDesc_s *domain,
			    ClassDesc * depClass,
			    struct DomainDesc_s *targetDomain,
			    u4_t targetDomainID, u4_t depIndex);

ObjectDesc *copy_reference(struct DomainDesc_s *src,
			   struct DomainDesc_s *dst, ObjectDesc * ref,
			   u4_t * quota);
ObjectDesc *copy_object(struct DomainDesc_s *src, struct DomainDesc_s *dst,
			ObjectDesc * obj, u4_t * quota);
ObjectDesc *copy_portal(struct DomainDesc_s *src, struct DomainDesc_s *dst,
			Proxy * obj, u4_t * quota, jboolean addRef);
ObjectDesc *copy_array(struct DomainDesc_s *src, struct DomainDesc_s *dst,
		       ObjectDesc * obj, u4_t * quota);
struct ServiceThreadPool_s;
struct ThreadDesc_s* createServiceThread(struct DomainDesc_s* domain, int index, char* name);

//FIXME jgbauman
//int findProxyCodeInDomain(DomainDesc *domain, char *addr,  MethodDesc **method, ClassDesc **classInfo);
//FIXME jgbauman
jint send_portalcall(jint methodIndex, jint numParams, jint ** paramlist);

int findProxyCodeInDomain(struct DomainDesc_s *domain, char *addr,
			  char **method, char **sig,
			  struct ClassDesc_s **classInfo);

#endif				/*ASSEMBLER */




#endif
