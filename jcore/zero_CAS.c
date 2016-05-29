#include "all.h"

/*
 * CAS
 */

ClassDesc *casClass = NULL;

jboolean cas_casObject(CASProxy * self, ObjectDesc * obj, ObjectDesc * compare, ObjectDesc * setTo)
{
	jboolean ret = JNI_TRUE;
#if 1
	DISABLE_IRQ;
	{
		u4_t o = obj->data[self->index];
		if (o != (u4_t) compare) {
			ret = JNI_FALSE;
		} else {
			obj->data[self->index] = (u4_t) setTo;
		}
	}
	RESTORE_IRQ;
#else
	return (cas(&obj->data[self->index], compare, setTo) != 0);
#endif

	return ret;
}

MethodInfoDesc casMethods[] = {
	{"casObject", "", cas_casObject}
	,
	{"casInt", "", cas_casObject}
	,
	{"casBoolean", "", cas_casObject}
	,
	{"casShort", "", cas_casObject}
	,
	{"casByte", "", cas_casObject}
	,
};


static jbyte casTypeMap[] = { 0 };

void init_cas_portal()
{
	casClass = init_zero_class("jx/zero/CAS", casMethods, sizeof(casMethods), 1, casTypeMap, "<jx/zero/CAS>");
}
