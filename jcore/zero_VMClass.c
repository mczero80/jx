#include "all.h"

/*
 * VMClass DEP
 */
ClassDesc *vmclassClass = NULL;
extern ClassDesc *vmmethodClass;

ObjectDesc *vmclass_getName(ObjectDesc * self)
{
	Class *cl = obj2class(self);
	return string_replace_char(newString(curdom(), cl->classDesc->name), '/', '.');
}

jboolean vmclass_isPrimitive(ObjectDesc * self)
{
	return JNI_FALSE;
}

jboolean vmclass_equals(ObjectDesc * self, ObjectDesc * cmp)
{
	return self == cmp;
}

jint vmclass_getInstanceSize(ObjectDesc * self)
{
	Class *cl = obj2class(self);
	return cl->classDesc->instanceSize;
}

ObjectDesc *vmclass_newInstance(ObjectDesc * self)
{
	DomainDesc *sourceDomain = curdom();
	Class *cla = obj2class(self);
	ClassDesc *cl = cla->classDesc;
	ObjectDesc *obj = allocObjectInDomain(sourceDomain, cl);
	executeSpecial(sourceDomain, cl->name, "<init>", "()V", obj, NULL, 0);
	printf("newInstance %s -> %p\n", cl->name, obj);
	return obj;
}

ObjectDesc *vmclass_getMethods(ObjectDesc * self)
{
	u4_t i;
	ArrayDesc *obj;
	DomainDesc *sourceDomain = curdom();
	Class *cla = obj2class(self);
	ClassDesc *cl = cla->classDesc;
	obj = allocArray(vmmethodClass, cl->numberOfMethods);
	for (i = 0; i < cl->numberOfMethods; i++) {
		obj->data[i] = method2Obj(&(cl->methods[i]));
	}
	printf("getmethods %s -> %p\n", cl->name, obj);
	return obj;
}


MethodInfoDesc vmclassMethods[] = {
	{"getName", "()Ljava/lang/String;", (code_t) vmclass_getName}
	,
	{"isPrimitive", "()Z", (code_t) vmclass_isPrimitive}
	,
	{"getInstanceSize", "()I", (code_t) vmclass_getInstanceSize}
	,
	{"newInstance", "()Ljava/lang/Object;",
	 (code_t) vmclass_newInstance}
	,
	{"getMethods", "()[Ljx/zero/VMMethod;", (code_t) vmclass_getMethods}
	,
	{"equals", "", (code_t) vmclass_equals}
	,
};


static jbyte vmclassTypeMap[] = { 0, 0 };

void init_vmclass_portal()
{
	vmclassClass =
	    init_zero_class("jx/zero/VMClass", vmclassMethods, sizeof(vmclassMethods), 1, vmclassTypeMap, "<jx/zero/VMClass>");
}
