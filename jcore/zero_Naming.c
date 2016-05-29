#include "all.h"

#define CALLERDOMAIN (curthr()->mostRecentlyCalledBy->domain)

Proxy *initialNamingProxy = NULL;

struct nameValue_s *nameValue = NULL;
struct nameValue_s **lastNameValue = &nameValue;

Proxy *lookupPortal(char *name)
{
	struct nameValue_s *n;
	for (n = nameValue; n != NULL; n = n->next) {
		if (strcmp(name, n->name) == 0)
			return n->obj;
	}
	return NULL;
}

#if 0
/* list of waiting threads */
static ThreadDesc *waitqueue_first = NULL;
static ThreadDesc *waitqueue_last = NULL;
static void waitqueue_append(ThreadDesc * thread)
{
	thread->nextInRunQueue = NULL;
	if (waitqueue_first) {
		/* there are already threads in the queue */
		waitqueue_last->nextInRunQueue = thread;
		waitqueue_last = thread;
	} else {
		waitqueue_last = waitqueue_first = thread;
	}
}

void naming_listwaiters()
{
	ThreadDesc *t;
	DISABLE_IRQ;
	t = waitqueue_first;
	while (t) {
		printf("%d.%d\n", TID(t));
		t = t->nextInRunQueue;
	}
	RESTORE_IRQ;
}


/* THIS ONLY WORKS WHEN A THREAD POOL IS USED FOR SERVICES */
ObjectDesc *naming_lookupOrWait(ObjectDesc * self, ObjectDesc * name)
{
	for (;;) {
		ObjectDesc *ret = naming_lookup(self, name);
		if (ret != NULL)
			return ret;
		DISABLE_IRQ;
		waitqueue_append(curthr());
		threadblock();
		RESTORE_IRQ;
	}
}
#endif
ObjectDesc *naming_lookupOrWait(ObjectDesc * self, ObjectDesc * name)
{
	sys_panic("");
}

#ifdef PROFILE_SAMPLE
extern int do_sampling;
#endif

ObjectDesc *naming_lookup(ObjectDesc * self, ObjectDesc * name)
{
	Proxy *ret;
	char value[128];
	if (name == 0)
		return 0;
	stringToChar(name, value, sizeof(value));
	ret = lookupPortal(value);
	//  printf(" LOOKUP: %s -> %p\n", value, ret);

	//printf("LOOKUP: %p %s %x %d type:%s\n", ret, value, *(((jint*)ret)-1), *(((jint*)ret)-2), obj2ClassDesc(ret)->name);

	/*
	   #ifdef PROFILE_SAMPLE
	   if (do_sampling)  dumpThreadInfo(curthr()->mostRecentlyCalledBy);
	   #endif
	 */

	{
		u4_t quota = 1000;	/*  4 kB portal parameter quota , new quota for each new call */
		curthr()->n_copied = 0;
		//return copy_portal(domainZero, curdom(), ret, &quota, 0);
		return copy_reference(domainZero, curdom(), ret, &quota);
	}
}

void registerPortal(DomainDesc * domain, ObjectDesc * portalObject, char *name)
{
	struct nameValue_s *n = malloc_domainzero_namevalue(strlen(name) + 1);
	strcpy(n->name, name);
	//  printf("REGISTER %s -> %p\n", name, portalObject);
	if ((getObjFlags(portalObject) & FLAGS_MASK) != OBJFLAGS_PORTAL) {
		sys_panic("NOT A PORTAL");
	}
	n->obj = portalObject;
	n->next = NULL;
	DISABLE_IRQ;
	*lastNameValue = n;
	lastNameValue = &(n->next);
	RESTORE_IRQ;

#if 0
	/* wakeup all threads in waitqueue */
	{
		ThreadDesc *t, *next;
		DISABLE_IRQ;
		t = waitqueue_first;
		waitqueue_first = waitqueue_last = NULL;
		while (t) {
			next = t->nextInRunQueue;
			t->nextInRunQueue = NULL;
			threadunblock(t);
			t = next;
		}
		RESTORE_IRQ;
	}
#endif
}


void naming_registerPortal(ObjectDesc * self, Proxy * proxy, ObjectDesc * nameStr)
{
	char name[128];
	u4_t quota = 1000;
	//char *xname;
	/*
	   DEPDesc *dep=proxy->index;
	   ASSERT(dep!=NULL);
	   ASSERTDEP(dep);
	 */
	stringToChar(nameStr, name, sizeof(name));
	printf("registerPortal %s\n", name);
#ifdef COPY_TO_DOMAINZERO
	registerPortal(NULL, proxy, name);
#else
	registerPortal(NULL, copy_reference(CALLERDOMAIN, curdom(), (ObjectDesc *) proxy, &quota), name);
#endif
}




MethodInfoDesc namingMethods[] = {
	{"lookup", "(Ljava/lang/String;)Ljx/zero/Portal;",
	 (code_t) naming_lookup}
	,
	{"lookupOrWait", "(Ljava/lang/String;)Ljx/zero/Portal;",
	 (code_t) naming_lookupOrWait}
	,
	{"registerPortal", "", (code_t) naming_registerPortal}
	,
};


void init_naming_portal()
{
	initialNamingProxy = init_zero_dep("jx/zero/Naming", "Naming", namingMethods, sizeof(namingMethods), "<jx/zero/Naming>");
}
