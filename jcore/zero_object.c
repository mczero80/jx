#include "all.h"


/*
 * java/lang/Object
 */



void object_constructor(ObjectDesc * self)
{
}

ObjectDesc *object_getClass(ObjectDesc * self)
{
	jint params[2];
	ClassDesc *c = obj2ClassDesc(self);
	Class *cl = classDesc2Class(curdom(), c);
	ObjectDesc *vmclassObj = class2Obj(cl);
	ObjectDesc *classObj = (ObjectDesc *) allocObjectInDomain(curdom(),
								  findClassDesc("java/lang/Class"));
	//printf("getClass: %s\n", c->name);
#ifdef DEBUG
	{
		u4_t flags;
		flags = getObjFlags(vmclassObj) & FLAGS_MASK;
		ASSERT(flags == OBJFLAGS_EXTERNAL_CLASS);
	}
#endif
	params[0] = (jint) vmclassObj;
	executeSpecial(curdom(), "java/lang/Class", "<init>", "(Ljx/zero/VMClass;)V", classObj, params, 1);
	return classObj;
}

jint object_hashCode(ObjectDesc * self)
{
	return (jint) self;
}

ObjectDesc *object_clone(ObjectDesc * self)
{
	ObjectDesc *clone;
	ClassDesc *oclass;
	oclass = obj2ClassDesc(self);
	ASSERTCLASSDESC(oclass);
	clone = allocObject(oclass);
	ASSERTCLASSDESC(oclass);
	memcpy(&(clone->data[0]), &(self->data[0]), oclass->instanceSize * 4);
	return clone;
}

ObjectDesc *ohash;
ThreadDesc *twaiting;
/*wait()V*/
void object_wait0(ObjectDesc * self)
{
	sys_panic("not implemented");
}

/*wait(J)V*/
void object_wait1(ObjectDesc * self)
{
	sys_panic("not implemented");
}

/*wait(JI)V*/
void object_wait2(ObjectDesc * self)
{
	sys_panic("not implemented");
}

void object_notify(ObjectDesc * self)
{
	sys_panic("not implemented");
}

void object_notifyAll(ObjectDesc * self)
{
	sys_panic("not implemented");
}

ObjectDesc *object_toString(ObjectDesc * self)
{
	ObjectDesc *s;
	ClassDesc *oclass;

	printf("TOSTRING %p\n", self);
	if (self == NULL)
		return;
	oclass = obj2ClassDesc(self);
	s = newString(curdom(), oclass-> /*classDesc-> */ name);
	return s;
}

jboolean object_equals(ObjectDesc * self, ObjectDesc * other)
{
	return self == other;
}

void object_finalize(ObjectDesc * self)
{
	sys_panic("not implemented");
}

MethodInfoDesc objectMethods[] = {
	{"getClass", "()Ljava/lang/Class;", (code_t) object_getClass},
	{"hashCode", "()I", (code_t) object_hashCode},
	{"clone", "()Ljava/lang/Object;", (code_t) object_clone},
	{"wait", "()V", (code_t) object_wait0},
	{"wait", "(J)V", (code_t) object_wait1},
	{"wait", "(JI)V", (code_t) object_wait2},
	{"notify", "()V", (code_t) object_notify},
	{"notifyAll", "()V", (code_t) object_notifyAll},
	{"toString", "()Ljava/lang/String;", (code_t) object_toString},
	{"equals", "(Ljava/lang/Object;)Z", (code_t) object_equals},
	{"finalize", "()V", (code_t) object_finalize},
};

#ifdef USE_QMAGIC
# ifdef GC_SWAP_MAGIC_WITH_FLAGS
#  define MDHEADER MAGIC_OBJECT, OBJFLAGS_EXTERNAL_METHOD, NULL, MAGIC_METHODDESC
# else
#  define MDHEADER OBJFLAGS_EXTERNAL_METHOD, MAGIC_OBJECT, NULL, MAGIC_METHODDESC
# endif
#else				/* MAGIC */
# define MDHEADER OBJFLAGS_EXTERNAL_METHOD, NULL
#endif


MethodDesc object_constructor_methoddesc = {
	MDHEADER, "<init>", "()V",
	0, 0, 0, 0, 0, 0,
	(code_t) object_constructor
};

MethodDesc objectMethodDescs[] = {
	{MDHEADER, "getClass", "()Ljava/lang/Class;", 0, 0, 0, 0, 0, 0,
	 (code_t) object_getClass},
	{MDHEADER, "hashCode", "()I", 0, 0, 0, 0, 0, 0,
	 (code_t) object_hashCode},
	{MDHEADER, "clone", "()Ljava/lang/Object;", 0, 0, 0, 0, 0, 0,
	 (code_t) object_clone},
	{MDHEADER, "wait", "()V", 0, 0, 0, 0, 0, 0, (code_t) object_wait0},
	{MDHEADER, "wait", "(J)V", 0, 0, 0, 0, 0, 0,
	 (code_t) object_wait1},
	{MDHEADER, "wait", "(JI)V", 0, 0, 0, 0, 0, 0,
	 (code_t) object_wait2},
	{MDHEADER, "notify", "()V", 0, 0, 0, 0, 0, 0,
	 (code_t) object_notify},
	{MDHEADER, "notifyAll", "()V", 0, 0, 0, 0, 0, 0,
	 (code_t) object_notifyAll},
	{MDHEADER, "toString", "()Ljava/lang/String;", 0, 0, 0, 0, 0, 0,
	 (code_t) object_toString},
	{MDHEADER, "equals", "(Ljava/lang/Object;)Z", 0, 0, 0, 0, 0, 0,
	 (code_t) object_equals},
	{MDHEADER, "finalize", "()V", 0, 0, 0, 0, 0, 0,
	 (code_t) object_finalize},
};


void abstract_method_error(ObjectDesc * self);

void installObjectVtable(ClassDesc * c)
{
	int j;
	int n_methods = sizeof(objectMethods) / sizeof(MethodInfoDesc);
	for (j = 0; j < n_methods; j++) {
		// printf("OVT %s:%s:%s\n", java_lang_Object->vtableSym[j*3], java_lang_Object->vtableSym[j*3+1], java_lang_Object->vtableSym[j*3+2]);
		c->methodVtable[j] = &(objectMethodDescs[j]);

		if (java_lang_Object->vtableSym[j * 3][0] == '\0') {
			sys_panic("OBJECT");
		} else {
			/*printf("%s:%s:%s\n", java_lang_Object->vtableSym[j*3], java_lang_Object->vtableSym[j*3+1], java_lang_Object->vtableSym[j*3+2]); */
		}

		c->vtable[j] = objectMethods[j].code;
	}
	for (j = n_methods; j < c->vtableSize; j++) {
		c->vtable[j] = abstract_method_error;
	}

}

/*
 * Arrays
 */

void array_constructor(ArrayDesc * self)
{
}
ObjectDesc *array_getClass(ArrayDesc * self)
{
	return object_getClass((ObjectDesc *) self);
}

jint array_hashCode(ArrayDesc * self)
{
	return (jint) self;
}

ArrayDesc *array_clone(ArrayDesc * self)
{
	sys_panic("not implemented");
	return NULL;
}

void array_wait0(ArrayDesc * self)
{
	sys_panic("not implemented");
}

/*wait(J)V*/
void array_wait1(ArrayDesc * self)
{
	sys_panic("not implemented");
}

/*wait(JI)V*/
void array_wait2(ArrayDesc * self)
{
	sys_panic("not implemented");
}

void array_notify(ArrayDesc * self)
{
	sys_panic("not implemented");
}

void array_notifyAll(ArrayDesc * self)
{
	sys_panic("not implemented");
}

ObjectDesc *array_toString(ArrayDesc * self)
{
	char s[100];
	//  ClassDesc* arrayClass;
	//  jint size;
	if (self == NULL)
		return newString(curdom(), "ARRAY: null");
	strcpy(s, "ARRAY");
	strcat(s, self->arrayClass->name);
	sprintnum(s + strlen(s), self->size, 10);
	return newString(curdom(), s);
	//    sys_panic("not implemented");
}

jboolean array_equals(ArrayDesc * self, ArrayDesc * other)
{
	return self == other;
}

void array_finalize(ArrayDesc * self)
{
	sys_panic("not implemented");
}

MethodInfoDesc arrayMethods[] = {
	{"getClass", "()Ljava/lang/Class;", (code_t) array_getClass},
	{"hashCode", "()I", (code_t) array_hashCode},
	{"clone", "()Ljava/lang/Object;", (code_t) array_clone},
	{"wait", "()V", (code_t) array_wait0},
	{"wait", "(J)V", (code_t) array_wait1},
	{"wait", "(JI)V", (code_t) array_wait2},
	{"notify", "()V", (code_t) array_notify},
	{"notifyAll", "()V", (code_t) array_notifyAll},
	{"toString", "()Ljava/lang/String;", (code_t) array_toString},
	{"equals", "(Ljava/lang/Object;)Z", (code_t) array_equals},
	{"finalize", "()V", (code_t) array_finalize},
};



ClassDesc *createObjectClassDesc()
{
	//ClassDesc *java_lang_Object;
	int n_methods;
	int j, i;
	char *name = "java/lang/Object";
	/* create and init java/lang/Object */
	n_methods = sizeof(objectMethods) / sizeof(MethodInfoDesc);
	java_lang_Object = malloc_classdesc(domainZero, strlen(name) + 1);
	java_lang_Object->classType = CLASSTYPE_CLASS;
#ifdef USE_QMAGIC
	java_lang_Object->magic = MAGIC_CLASSDESC;
#endif
	strcpy(java_lang_Object->name, name);
	java_lang_Object->definingLib = NULL;
	java_lang_Object->superclass = NULL;
	java_lang_Object->instanceSize = 0;
	java_lang_Object->vtableSize = n_methods;
	java_lang_Object->vtableSym = malloc_vtableSym(domainZero, java_lang_Object->vtableSize);
	i = 0;
	java_lang_Object->numberOfMethods = n_methods;
	java_lang_Object->methods = malloc_methods(domainZero, n_methods);
	java_lang_Object->methodVtable = malloc_methodVtable(domainZero, n_methods);
	memset(java_lang_Object->methods, 0, sizeof(MethodDesc) * n_methods);
	for (j = 0; j < java_lang_Object->vtableSize * 3; j += 3) {
		java_lang_Object->vtableSym[j] = "java/lang/Object";	/* class */
		java_lang_Object->vtableSym[j + 1] = objectMethods[i].name;	/* name */
		java_lang_Object->vtableSym[j + 2] = objectMethods[i].signature;	/* type */
		java_lang_Object->methods[i].name = objectMethods[i].name;
		java_lang_Object->methods[i].signature = objectMethods[i].signature;
		java_lang_Object->methodVtable[i] = &java_lang_Object->methods[i];
		//if (java_lang_Object->methodVtable[i]) printf(" XXX  %s %s ", java_lang_Object->methodVtable[i]->name, java_lang_Object->methodVtable[i]->signature);
		i++;
	}
	createVTable(domainZero, java_lang_Object);
	installVtables(domainZero, java_lang_Object, objectMethods, n_methods, NULL);
	return java_lang_Object;
}

Class *createObjectClass(ClassDesc * java_lang_Object)
{
	Class *c = malloc_class(domainZero);
	c->classDesc = java_lang_Object;
#ifdef USE_QMAGIC
	c->magic = MAGIC_CLASS;
#endif
	/* superclass */
	c->superclass = NULL;
	/* static fields */
	c->staticFields = 0;
	return c;
}

code_t *array_vtable = NULL;

void createArrayObjectVTableProto(DomainDesc * domain)
{
	ClassDesc *java_lang_Array;
	int n_methods;
	int j, i;
	char *name = "<Array>";
	/* create and init java/lang/Array */
	n_methods = sizeof(arrayMethods) / sizeof(MethodInfoDesc);
	java_lang_Array = malloc_classdesc(domain, strlen(name) + 1);
	java_lang_Array->classType = CLASSTYPE_CLASS;
#ifdef USE_QMAGIC
	java_lang_Array->magic = MAGIC_CLASSDESC;
#endif
	strcpy(java_lang_Array->name, name);
	java_lang_Array->definingLib = NULL;
	java_lang_Array->superclass = NULL;
	java_lang_Array->instanceSize = 0;
	java_lang_Array->vtableSize = n_methods;
	java_lang_Array->vtableSym = malloc_vtableSym(domain, java_lang_Array->vtableSize);
	i = 0;
	for (j = 0; j < java_lang_Array->vtableSize * 3; j += 3) {
		java_lang_Array->vtableSym[j] = "<Array>";	/* class */
		java_lang_Array->vtableSym[j + 1] = arrayMethods[i].name;	/* name */
		java_lang_Array->vtableSym[j + 2] = arrayMethods[i].signature;	/* type */
		i++;
	}

	createVTable(domain, java_lang_Array);
	installVtables(domain, java_lang_Array, arrayMethods, n_methods, java_lang_Object);
	array_vtable = java_lang_Array->vtable;
}

void findClassDescAndMethodInObject(SharedLibDesc * lib, char *classname, char *methodname, char *signature,
				    ClassDesc ** classFound, MethodDesc ** methodFound)
{
	int j;
	int n_methods = sizeof(objectMethods) / sizeof(MethodInfoDesc);
	if ((strcmp(methodname, "<init>") == 0)
	    && (strcmp(signature, "()V") == 0)) {
		*classFound = java_lang_Object;
		*methodFound = &object_constructor_methoddesc;
		return;
	}
	for (j = 0; j < n_methods; j++) {
		if (strcmp(methodname, objectMethods[j].name) == 0) {
			if (strcmp(signature, objectMethods[j].signature)
			    == 0) {
				*classFound = java_lang_Object;
				*methodFound = &objectMethodDescs[j];
				return;
			}
		}
	}

}


extern ClassDesc *vmmethodClass;
void init_object()
{
	u4_t j;
/* add vmmethodclass->vtable to objectmethoddesc */
	int n_methods = sizeof(objectMethods) / sizeof(MethodInfoDesc);
	for (j = 0; j < n_methods; j++) {
		objectMethodDescs[j].objectDesc_vtable = vmmethodClass->vtable;
	}
}
