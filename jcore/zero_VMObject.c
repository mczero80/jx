#include "all.h"

ClassDesc *vmobjectClass = NULL;


/*
 * VMObject DEP
 */

ObjectDesc *vmobject_getVMClass(VMObjectProxy * self)
{
	ASSERTOBJECT(self);
	if (self->type) {
		ClassDesc *c = obj2ClassDesc(self->obj);
		Class *cl = classDesc2Class(self->domain, c);
		ObjectDesc *vmclassObj = class2Obj(cl);
#ifdef DEBUG
		{
			u4_t flags;
			flags = getObjFlags(vmclassObj) & FLAGS_MASK;
			ASSERT(flags == OBJFLAGS_EXTERNAL_CLASS);
		}
#endif
		return vmclassObj;
	} else
		return NULL;
}

int vmobject_getPrimitiveData(VMObjectProxy * self)
{
	if (self->type)
		return 0;
	else
		return (int) self->obj;
}

ObjectDesc *vmobject_getString(VMObjectProxy * self)
{
	ClassDesc *c;

	if (!self->type)
		return NULL;

	c = obj2ClassDesc(self->obj);
	if (strcmp(c->name, "java/lang/String") != 0)
		return NULL;

	/* evtl vorher kopieren???? */
	return self->obj;
}

static jboolean getSubObject(VMObjectProxy * self, VMObjectProxy * result, int index)
{
	ClassDesc *c;

	if (!self->type) {
		result->obj = NULL;
		return JNI_FALSE;
	}

	ASSERTOBJECT(self->obj);
	c = obj2ClassDesc(self->obj);

	if (index >= c->instanceSize) {
		result->obj = NULL;
		return JNI_FALSE;
	}
#if DEBUG
	if (gc_isValidHeapRef(self->domain, self->obj)) {
		printf("%p\n", self->obj);
		printf("%s\n", c->name);
		printf("%s\n", self->domain->domainName);
		sys_panic("OBJECT OUTSIDE HEAP");
	}
#endif
	result->domain = self->domain;
	result->domain_id = self->domain_id;
	result->type = isRef(c->map, c->instanceSize, self->subObjectIndex);
	result->obj = (ObjectDesc *) self->obj->data[index];
	return JNI_TRUE;
}

jboolean vmobject_getFirstSubObject(VMObjectProxy * self, VMObjectProxy * result)
{
	self->subObjectIndex = 0;
	return getSubObject(self, result, self->subObjectIndex);
}

jboolean vmobject_getNextSubObject(VMObjectProxy * self, VMObjectProxy * result)
{
	self->subObjectIndex++;
	return getSubObject(self, result, self->subObjectIndex);
}


MethodInfoDesc vmobjectMethods[] = {
	{"getVMClass", "()Ljava/lang/String;", (code_t) vmobject_getVMClass}
	,
	{"getPrimitiveData", "", (code_t) vmobject_getPrimitiveData}
	,
	{"getString", "", (code_t) vmobject_getString}
	,
	{"getFirstSubObject", "", (code_t) vmobject_getFirstSubObject}
	,
	{"getNextSubObject", "", (code_t) vmobject_getNextSubObject}
	,
};

static jbyte vmobjectTypeMap[] = { 0, 0 };

void init_vmobject_portal()
{
	vmobjectClass =
	    init_zero_class("jx/zero/VMObject", vmobjectMethods, sizeof(vmobjectMethods), 1, vmobjectTypeMap,
			    "<jx/zero/VMObject>");
}
