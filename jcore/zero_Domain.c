#include "all.h"

extern ClassDesc *domainClass;

/*
 * Domain DEP
 */
#if 0
jint domain_getID(ObjectDesc * self)
{
	/*
	   DomainDesc *domain = obj2domainDesc(self);
	   return domain->id;
	 */
	sys_panic("getID deprecated");
}
#endif

void domain_clearTCBflag(ObjectDesc * self)
{
#ifdef PORTAL_INTERCEPTOR
	obj2domainDesc(self)->memberOfTCB = JNI_FALSE;
#endif
}

jboolean domain_isActive(DomainProxy * self)
{
	jboolean ret = JNI_FALSE;
	DISABLE_IRQ;
	if ((self->domain->id == self->domainID)
	    && (self->domain->state == DOMAIN_STATE_ACTIVE))
		ret = JNI_TRUE;
	RESTORE_IRQ;
	return ret;
}

jboolean domain_isTerminated(DomainProxy * self)
{
	jboolean ret = JNI_FALSE;
	DISABLE_IRQ;
	if (self->domain->id != self->domainID)
		ret = JNI_TRUE;
	else if (self->domain->state == DOMAIN_STATE_FREE)
		ret = JNI_TRUE;
	RESTORE_IRQ;
	return ret;
}

ObjectDesc *domain_getName(DomainProxy * self)
{
	return NULL;
}

ObjectDesc *domain_getID(DomainProxy * self)
{
	return self->domainID;
}

MethodInfoDesc domainMethods[] = {
	{"clearTCBflag", "()I", (code_t) domain_clearTCBflag}
	,
	{"isActive", "()Z", (code_t) domain_isActive}
	,
	{"isTerminated", "()Z", (code_t) domain_isTerminated}
	,
	{"getName", "()Ljava/lang/String;", (code_t) domain_getName}
	,
	{"getID", "()I", (code_t) domain_getID}
	,
};

static jbyte domainTypeMap[] = { 0 };

void init_domain_portal()
{
	domainClass =
	    init_zero_class("jx/zero/Domain", domainMethods, sizeof(domainMethods), 1, domainTypeMap, "<jx/zero/Domain>");
}
