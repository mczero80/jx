#include "all.h"

ClassDesc *vmmethodClass = NULL;


/*
 * VMMethod DEP
 */

ObjectDesc *vmmethod_getName(ObjectDesc * self)
{
	MethodDesc *m = obj2method(self);
	return newString(curdom(), m->name);
}

ObjectDesc *vmmethod_getSignature(ObjectDesc * self)
{
	MethodDesc *m = obj2method(self);
	return newString(curdom(), m->signature);
}

ObjectDesc *vmmethod_invoke(ObjectDesc * self, ObjectDesc * obj, ArrayDesc * params)
{
	MethodDesc *m = obj2method(self);
	ClassDesc *cd = m->classDesc;
	if (IS_STATIC(m)) {
		// check if object is instanceof class of this vmmethod
		if (!vm_instanceof(obj, m->classDesc)) {
			exceptionHandler(THROW_RuntimeException);
		}
		printf("INVOKE STATIC\n");
		if (params) {
			if (params->size != m->numberOfArgs)
				exceptionHandler(THROW_RuntimeException);
			return executeStatic(curdom(), cd->name, m->name, m->signature, params->data, params->size);	// TODO: check whether this is GC safe
		} else {
			if (m->numberOfArgs > 0)
				exceptionHandler(THROW_RuntimeException);
			return executeStatic(curdom(), cd->name, m->name, m->signature, NULL, 0);	// TODO: check whether this is GC safe
		}
	} else {
		if (obj == NULL)
			exceptionHandler(THROW_NullPointerException);
		printf("INVOKE VIRTUAL\n");
		return executeVirtual(curdom(), cd->name, m->name, m->signature, obj, NULL, 0);
	}
}

MethodInfoDesc vmmethodMethods[] = {
	{"getName", "()Ljava/lang/String;", (code_t) vmmethod_getName}
	,
	{"getSignature", "()Ljava/lang/String;",
	 (code_t) vmmethod_getSignature}
	,
	{"invoke",
	 "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
	 (code_t) vmmethod_invoke}
	,
};

static jbyte vmmethodTypeMap[] = { 0, 0 };

void init_vmmethod_portal()
{
	vmmethodClass =
	    init_zero_class("jx/zero/VMMethod", vmmethodMethods, sizeof(vmmethodMethods), 1, vmmethodTypeMap,
			    "<jx/zero/VMMethod>");
}
