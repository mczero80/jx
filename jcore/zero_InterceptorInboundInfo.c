#include "all.h"

ClassDesc *interceptInboundInfoClass = NULL;
ClassDesc *interceptPortalInfoClass = NULL;


/*
 * InterceptInboundInfo DEP
 */

ObjectDesc *interceptInboundInfo_getSourceDomain(InterceptInboundInfoProxy * self)
{
	sys_panic("use plugin for InterceptInboundInfo");
	return domainDesc2Obj(self->source);
}

ObjectDesc *interceptInboundInfo_getTargetDomain(InterceptInboundInfoProxy * self)
{
	sys_panic("use plugin for InterceptInboundInfo");
	return domainDesc2Obj(self->target);
}

ObjectDesc *interceptInboundInfo_getMethod(InterceptInboundInfoProxy * self)
{
	sys_panic("use plugin for InterceptInboundInfo");
	return self->method;
}

ObjectDesc *interceptInboundInfo_getServiceObject(InterceptInboundInfoProxy * self)
{
//     sys_panic("use plugin for InterceptInboundInfo");
	ASSERT(self->obj != NULL);
	return self->obj;
}

static jboolean getParameter(InterceptInboundInfoProxy * self, VMObjectProxy * obj, int index)
{
	jint numberArgs = obj2method(self->method)->numberOfArgs;
	jbyte *argTypeMap = obj2method(self->method)->argTypeMap;

	if (index > numberArgs) {
		obj->obj = NULL;
		return JNI_FALSE;
	}

	obj->domain = self->target;
	obj->domain_id = self->target->id;
	obj->type = isRef(argTypeMap, numberArgs, self->index - 1);
	obj->obj = (ObjectDesc *) (self->paramlist[self->index]);
	return JNI_TRUE;
}

jboolean interceptInboundInfo_getFirstParameter(InterceptInboundInfoProxy * self, VMObjectProxy * obj)
{
	self->index = 1;
	return getParameter(self, obj, self->index);
}

jboolean interceptInboundInfo_getNextParameter(InterceptInboundInfoProxy * self, VMObjectProxy * obj)
{
	self->index++;
	return getParameter(self, obj, self->index);
}

MethodInfoDesc interceptInboundInfoMethods[] = {
	{"getSourceDomain", "",
	 (code_t) interceptInboundInfo_getSourceDomain}
	,
	{"getTargetDomain", "",
	 (code_t) interceptInboundInfo_getTargetDomain}
	,
	{"getMethod", "", (code_t) interceptInboundInfo_getMethod}
	,
	{"getFirstParameter", "",
	 (code_t) interceptInboundInfo_getFirstParameter}
	,
	{"getNextParameter", "",
	 (code_t) interceptInboundInfo_getNextParameter}
	,
	{"getServiceObject", "",
	 (code_t) interceptInboundInfo_getServiceObject}
	,
};


/* FIXME*/
static jbyte interceptInboundInfoTypeMap[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

/*
 * InterceptPortalInfo DEP
 */

jint interceptPortalInfo_getServiceID(InterceptPortalInfoProxy * self)
{
	//sys_panic("use plugin for InterceptPortalInfo");
	return domainDesc2Obj(self->index);
}

ObjectDesc *interceptPortalInfo_getTargetDomain(InterceptPortalInfoProxy * self)
{

	//   sys_panic("use plugin for InterceptPortalInfo");
	return domainDesc2Obj(self->domain);
}

MethodInfoDesc interceptPortalInfoMethods[] = {
	{"getTargetDomain", "",
	 (code_t) interceptPortalInfo_getTargetDomain}
	,
	{"getServiceID", "", (code_t) interceptPortalInfo_getServiceID}
	,
};


/* FIXME*/
static jbyte interceptPortalInfoTypeMap[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

void init_interceptInboundInfo_portal()
{
	interceptInboundInfoClass =
	    init_zero_class("jx/zero/InterceptInboundInfo", interceptInboundInfoMethods, sizeof(interceptInboundInfoMethods), 1,
			    interceptInboundInfoTypeMap, "<jx/zero/InterceptInboundInfo>");
	interceptPortalInfoClass =
	    init_zero_class("jx/zero/PortalInfo", interceptPortalInfoMethods, sizeof(interceptPortalInfoMethods), 1,
			    interceptPortalInfoTypeMap, "<jx/zero/InterceptPortalInfo>");
}
