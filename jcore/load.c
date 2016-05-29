/********************************************************************************
 * Loader and linker
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Christian Wawersich
 *******************************************************************************/

#include "all.h"

#define debugf(x)
#define debugs(x)
#define debugp(x)
#define debugbt(x)
#ifdef DBG_LOAD
#define debugc(x) printf x
#else				/* DBG_LOAD */
#define debugc(x)
#endif				/* DBG_LOAD */


/*
#define DBG_CLINIT 1
*/

/*
#define debugf(x) printf x
#define debugs(x) printf x
#define debugp(x) printf x
#define debugs(x) { if (strcmp(lib->allClasses[i].methods[j].name,"perf_local_throw")==0) printf x ; }
*/

SharedLibDesc *sharedLibs = NULL;
static jint sharedLibsIndexNumber = 0;

ClassDesc *java_lang_Object;
Class *java_lang_Object_class;
//ClassDesc *class_Array;
//Class *class_Array_class;

code_t *array_vtable_prototype;


/*static char *codefile;
static char *codefilepos;
*/

#ifndef KERNEL

void install_handler(int sig, void (*handler) (int))
{
	struct sigaction act;

	memset(&act, 0, sizeof(act));
	act.sa_handler = handler;
	act.sa_flags = 0;	/*SA_SIGINFO; */
	if (sigaction(sig, &act, NULL) != 0) {
		perror("Error installing signal handler");
		exit(EXIT_FAILURE);
	}
}

#endif

#if defined(_BIG_ENDIAN)
NOT IMPL jint readInt()
{
	unsigned char *b;
	jint i;
	b = (unsigned char *) codefilepos;
	codefilepos += 4;
	i = (((((b[3] << 8) | b[2]) << 8) | b[1]) << 8) | b[0];
	/*debugf(("%d%d%d%d = %ld\n", b[3], b[2], b[1], b[0], i)); */
	return i;
}
#else
#define readInt(i) {i = *(jint*)codefilepos; codefilepos += 4;}
#endif

#define readShort(i) {i = *(jshort*)codefilepos; codefilepos += 2;}

#define readByte(i) {i = *(jbyte*)codefilepos; codefilepos += 1;}

#define readStringData(buf, nBytes) { memcpy(buf, codefilepos, nBytes); codefilepos += nBytes; buf[nBytes] = '\0';}

#define readString(buf,nbuf) { jint nBytes; readInt(nBytes); if (nBytes >= nbuf) sys_panic("buf too small\n"); readStringData(buf, nBytes);}

#define readStringID(buf) { jint id ; readInt(id); buf = string_table[id]; }

#define readAllocString(buf) {jint nBytes; readInt(nBytes);  if (nBytes >= 10000) sys_panic("nBytes too large\n"); buf = malloc_string(domain, nBytes+1); readStringData(buf, nBytes);}

#define readCode(buf, nbytes) {  memcpy(buf, codefilepos, nbytes); codefilepos += nbytes;}


ArrayDesc *vmSpecialAllocMultiArray(ClassDesc * elemClass, jint dim, jint sizes);

/**
 * SYMBOLS
 */


typedef SymbolDesc SymbolDescDomainZero;

typedef SymbolDesc SymbolDescExceptionHandler;

typedef struct {
	SYMBOLDESC_BASE;
	char *className;
	char *methodName;
	char *methodSignature;
} SymbolDescDEPFunction;

typedef SymbolDesc SymbolDescAllocObject;

typedef struct {
	SYMBOLDESC_BASE;
	char *className;
	jint kind;
	jint fieldOffset;
} SymbolDescStaticField;

typedef struct {
	SYMBOLDESC_BASE;
	jint kind;
} SymbolDescProfile;

typedef struct {
	SYMBOLDESC_BASE;
	char *className;
} SymbolDescClass;

typedef struct {
	SYMBOLDESC_BASE;
	char *className;
	char *methodName;
	char *methodSignature;
} SymbolDescDirectMethodCall;

typedef struct {
	SYMBOLDESC_BASE;
	char *value;
} SymbolDescString;

typedef SymbolDesc SymbolDescAllocArray;

typedef SymbolDesc SymbolDescAllocMultiArray;

typedef struct {
	SYMBOLDESC_BASE;
	jint operation;
} SymbolDescLongArithmetic;

typedef struct {
	SYMBOLDESC_BASE;
	jint operation;
} SymbolDescVMSupport;

typedef struct {
	SYMBOLDESC_BASE;
	int primitiveType;
} SymbolDescPrimitiveClass;

typedef struct {
	SYMBOLDESC_BASE;
	int targetNCIndex;
} SymbolDescUnresolvedJump;

typedef struct {
	SYMBOLDESC_BASE;
	int targetNCIndex;	/* extends UnresolvedJump ! */
	int rangeStart;
	int rangeEnd;
	char *className;
} SymbolDescExceptionTable;

typedef struct {
	SYMBOLDESC_BASE;
} SymbolDescThreadPointer;

typedef struct {
	SYMBOLDESC_BASE;
} SymbolDescStackChunkSize;

typedef struct {
	SYMBOLDESC_BASE;
} SymbolDescMethodeDesc;

typedef struct {
	SYMBOLDESC_BASE;
	jint kind;
} SymbolDescTCBOffset;

/*****/

/*
 * Prototypes
 */
MethodDesc *findMethod(DomainDesc * domain, char *classname, char *methodname, char *signature);
MethodDesc *findMethodInLib(LibDesc * lib, char *classname, char *methodname, char *signature);
Class *findClass(DomainDesc * domain, char *name);
ClassDesc *findClassDesc(char *name);
ClassDesc *findClassDescInSharedLib(SharedLibDesc * lib, char *name);
void findClassAndMethod(DomainDesc * domain, char *classname, char *methodname, char *signature, Class ** classFound,
			MethodDesc ** methodFound);
void findClassDescAndMethod(char *classname, char *methodname, char *signature, ClassDesc ** classFound,
			    MethodDesc ** methodFound);
void findClassAndMethodInLib(LibDesc * lib, char *classname, char *methodname, char *signature, Class ** classFound,
			     MethodDesc ** methodFound);
MethodDesc *findMethodInSharedLibs(char *classname, char *methodname, char *signature);
int findSubClasses(DomainDesc * domain, Class * c, Class ** subclassesFound, int bufsize);
int findSubClassesInLib(LibDesc * lib, Class * c, Class ** subclassesFound, int bufsize);
ObjectDesc *newString(DomainDesc * domain, char *value);
void sys_panic(char *msg, ...);
Class *findClassOrPrimitive(DomainDesc * domain, char *name);

jint *specialAllocStaticFields(DomainDesc * domain, int numberFields);

ArrayDesc *allocArrayInDomain(DomainDesc * domain, ClassDesc * type, jint size);


ArrayDesc *specialAllocArray(ClassDesc * elemClass, jint size);
ObjectDesc *specialAllocObject(ClassDesc * c);


/*
int numDEPInstances = 0;
DEPDesc **allDEPInstances = NULL;
*/


/*
 * Class management
 */

Class *class_I;
Class *class_J;
Class *class_F;
Class *class_D;
Class *class_B;
Class *class_C;
Class *class_S;
Class *class_Z;

ArrayClassDesc *sharedArrayClasses = NULL;
ClassDesc *vmclassClass;
ClassDesc *vmmethodClass;

Class *specialAllocClass(DomainDesc * domain, int number)
{
	int i;
	Class *ret;
	Class *c = malloc_classes(domain, number);
	memset(c, 0, sizeof(Class) * number);
	ret = c;
	for (i = 0; i < number; i++) {
#ifdef USE_QMAGIC
		c->magic = MAGIC_CLASS;
		c->objectDesc_magic = MAGIC_OBJECT;
#endif
		c->objectDesc_flags = OBJFLAGS_EXTERNAL_CLASS;

		if (vmclassClass != NULL)
			c->objectDesc_vtable = vmclassClass->vtable;
		c++;
	}
	return ret;
}

Class *createPrimitiveClass(char *name)
{
	PrimitiveClassDesc *cd = malloc_primitiveclassdesc(domainZero, strlen(name) + 1);
	Class *c = specialAllocClass(domainZero, 1);
#ifdef USE_QMAGIC
	cd->magic = MAGIC_CLASSDESC;
#endif
	c->classDesc = (ClassDesc *) cd;
	cd->classType = CLASSTYPE_PRIMITIVE;
	strcpy(cd->name, name);
	return c;
}

void initPrimitiveClasses()
{
	class_I = createPrimitiveClass("I");
	class_J = createPrimitiveClass("J");
	class_F = createPrimitiveClass("F");
	class_D = createPrimitiveClass("D");
	class_B = createPrimitiveClass("B");
	class_C = createPrimitiveClass("C");
	class_S = createPrimitiveClass("S");
	class_Z = createPrimitiveClass("Z");
}

ArrayClassDesc *createSharedArrayClassDesc(char *name);
ArrayClassDesc *createSharedArrayClassDescUsingElemClass(ClassDesc * elemClass);

ClassDesc *findSharedArrayClassDesc(char *name)
{
	ClassDesc *c;
	DISABLE_IRQ;
	//printf("FINDARRACLASS: %s\n", name);
	for (c = (ClassDesc *) sharedArrayClasses; c != NULL; c = c->next) {
		if (strcmp(c->name, name) == 0) {
			goto finished;
		}
	}
	/* not found, create one */
	c = (ClassDesc *) createSharedArrayClassDesc(name);
	c->next = (ClassDesc *) sharedArrayClasses;
	sharedArrayClasses = (ArrayClassDesc *) c;
      finished:
	RESTORE_IRQ;
	return c;
}

ArrayClassDesc *findSharedArrayClassDescByElemClass(ClassDesc * elemClass)
{
	ArrayClassDesc *c;
	DISABLE_IRQ;
	//   printf("FINDARRACLASS: %s ", elemClass->name);
	if (elemClass->arrayClass != NULL) {
		c = elemClass->arrayClass;
		goto finished;
	}
	/* not found, create one */
	c = createSharedArrayClassDescUsingElemClass(elemClass);
	c->next = (ClassDesc *) sharedArrayClasses;
	elemClass->arrayClass = c;
	sharedArrayClasses = c;
	//printf(" create %p\n", c);
      finished:
	RESTORE_IRQ;
	return c;
}

/* creates a new array class 
 * arrayclasses are not shared???
*/
Class *createArrayClass(DomainDesc * domain, char *name)
{
	Class *cl;
	char *n = name + 1;
	Class *c = findClassOrPrimitive(domain, n);
	ArrayClassDesc *arrayClass = (ArrayClassDesc *) jxmalloc(sizeof(ArrayClassDesc) MEMTYPE_OTHER);
	memset(arrayClass, 0, sizeof(ArrayClassDesc));
	//printf("CREATEARRAYCLASS %s\n", name);

#ifdef USE_QMAGIC
	arrayClass->magic = MAGIC_CLASSDESC;
#endif
	arrayClass->classType = CLASSTYPE_ARRAYCLASS;
	arrayClass->name = (char *) jxmalloc(strlen(name) + 1 MEMTYPE_OTHER);
	strcpy(arrayClass->name, name);
	arrayClass->elementClass = c->classDesc;

	/* create class */
	cl = specialAllocClass(domain, 1);
	cl->classDesc = (ClassDesc *) arrayClass;

	/* add to domain */
	arrayClass->nextInDomain = domain->arrayClasses;
	domain->arrayClasses = cl;

	return cl;
}

Class *findPrimitiveClass(char name);

ArrayClassDesc *createSharedArrayClassDesc(char *name)
{
	ClassDesc *c;
	Class *cl;
	ArrayClassDesc *arrayClass;
	char value[80];
	u4_t namelen;
	char *n = name + 1;

	//printf("CREATESHAREDARRAY %s\n", name);
	if (*n == 'L') {
		strncpy(value, name + 2, strlen(name) - 3);
		value[strlen(name) - 3] = '\0';
		c = findClassDesc(value);
	} else if (*n == '[') {
		c = findSharedArrayClassDesc(n);
	} else {
		cl = findPrimitiveClass(*n);
		if (cl == NULL)
			sys_panic("creating class");
		c = cl->classDesc;
	}
	if (c == NULL)
		sys_panic("not a shared element class");
	namelen = strlen(name) + 1;
	arrayClass = malloc_arrayclassdesc(domainZero, namelen);
#ifdef USE_QMAGIC
	arrayClass->magic = MAGIC_CLASSDESC;
#endif
	arrayClass->classType = CLASSTYPE_ARRAYCLASS;
	strcpy(arrayClass->name, name);
	arrayClass->elementClass = c;

	//printf("createSharedArrayClassDesc1: %s \n", arrayClass->name);
	memcpy(arrayClass->vtable, array_vtable, 11 * 4);
	*((u4_t *) (arrayClass->vtable) - 1) = arrayClass;

	//printf("createSharedArrayClassDesc2: %s\n", ( *(ClassDesc**)(( arrayClass->vtable-1) ))->name);

	//  printf("   <- created arrayClass %s\n", arrayClass->name);


	/* add to list of shared array classes */
	/*
	   arrayClass->nextShared = sharedArrayClasses;
	   sharedArrayClasses = arrayClass;
	 */

	/* add to domain */
	//arrayClass->nextInDomain = curdom()->arrayClasses;
	//curdom()->arrayClasses = cl;


	return arrayClass;
}


ArrayClassDesc *createSharedArrayClassDescUsingElemClass(ClassDesc * elemClass)
{
	ArrayClassDesc *arrayClass;
	ClassDesc *c = elemClass;
	jboolean primitiveElems;
	u4_t namelen;

	//printf("CREATESHAREDARRAYUsingElem %s\n", elemClass->name);
	if (c == NULL)
		sys_panic("not a shared element class");

	primitiveElems = *elemClass->name == '[' || elemClass->classType == CLASSTYPE_PRIMITIVE;
	if (primitiveElems) {
		namelen = strlen(elemClass->name) + 1 + 1;	/* [ ...  */
	} else {
		namelen = strlen(elemClass->name) + 1 + 3;	/* [L  ... ; */
	}

	arrayClass = malloc_arrayclassdesc(domainZero, namelen + 1);
#ifdef USE_QMAGIC
	arrayClass->magic = MAGIC_CLASSDESC;
#endif
	arrayClass->classType = CLASSTYPE_ARRAYCLASS;
	if (primitiveElems) {
		strcpy(arrayClass->name, "[");
		strcat(arrayClass->name, elemClass->name);
	} else {
		strcpy(arrayClass->name, "[L");
		strcat(arrayClass->name, elemClass->name);
		strcat(arrayClass->name, ";");
	}

	arrayClass->elementClass = elemClass;

	memcpy(arrayClass->vtable, array_vtable, 11 * 4);
	*((u4_t *) (arrayClass->vtable) - 1) = arrayClass;
	//printf("createSharedArrayClassDescUsingElemClass1: %s \n", arrayClass->name);
	//printf("createSharedArrayClassDescUsingElemClass2: %s\n", ( *(ClassDesc**)(( arrayClass->vtable-1) ))->name);


	//printf("   <- created arrayClass %s\n", arrayClass->name);

	/* add to list of shared array classes */
	/*
	   arrayClass->nextShared = sharedArrayClasses;
	   sharedArrayClasses = arrayClass;
	 */

	/* add to domain */
	//arrayClass->nextInDomain = curdom()->arrayClasses;
	//curdom()->arrayClasses = cl;


	return arrayClass;
}

Class *findPrimitiveClass(char name)
{
	ASSERT(name != 'L');
	switch (name) {
	case 'I':
		return class_I;
	case 'J':
		return class_J;
	case 'F':
		return class_F;
	case 'D':
		return class_D;
	case 'B':
		return class_B;
	case 'C':
		return class_C;
	case 'S':
		return class_S;
	case 'Z':
		return class_Z;
	}
	sys_panic("unknown primitive type %c", name);
	return NULL;
}

ClassDesc *findClassDescInSharedLib(SharedLibDesc * lib, char *name)
{
	int i;
	ASSERTSLIB(lib);
	if (strcmp(name, "java/lang/Object") == 0)
		return java_lang_Object;
	for (i = 0; i < lib->numberOfClasses; i++) {
		if (strcmp(lib->allClasses[i].name, name) == 0)
			return &lib->allClasses[i];
	}
	return NULL;
}

#ifdef PROFILE_SAMPLE
extern int do_sampling;
#endif

Class *findClassInLib(LibDesc * lib, char *name)
{
	int i;

#ifdef SAMPLE_FASTPATH
#ifdef PROFILE_SAMPLE
	if (do_sampling)
		printStackTrace("SLOWOPERATION-FINDCLASS ", curthr(), &lib - 2);
#endif
#endif

	ASSERTLIB(lib);

	if (strcmp(name, "java/lang/Object") == 0)
		return java_lang_Object_class;
	for (i = 0; i < lib->numberOfClasses; i++) {
		if (strcmp(lib->allClasses[i].classDesc->name, name) == 0)
			return &lib->allClasses[i];
	}
	return NULL;
}

/* classes are given as, for example,
 * Ljava/lang/Object; for Object class
 * or
 * I for primitive int type
 */
Class *findClassOrPrimitive(DomainDesc * domain, char *name)
{
	Class *cl;
	if (*name == '[') {
		//printf("FIND: %s\n", name);
		return findClass(domain, name);
	} else if (*name != 'L') {
		if ((cl = findPrimitiveClass(*name)) != NULL) {
			return cl;
		}
	} else {
		// FIXME: name is not a real class name but a signature
		char tmp[80];
		strncpy(tmp, name + 1, strlen(name) - 2);
		tmp[strlen(name) - 2] = '\0';
		return findClass(domain, tmp);
	}
	sys_panic("findClOrPrim error name=%s", name);
	return NULL;
}

void addHashKey(char *name, char *key, int len)
{
	int i;
	for (i = 0; i < len; i++) {
		if (name[i] == 0)
			return;
		key[i] = key[i] | name[i];
	}
}

jint testHashKey(char *name, char *key, int len)
{
	int i;
	for (i = 0; i < len; i++) {
		if (name[i] == 0)
			return JNI_TRUE;
		if ((name[i] & key[i]) != name[i])
			return JNI_FALSE;
	}
	return JNI_TRUE;
}

u4_t findFieldOffset(ClassDesc * c, char *fieldname)
{
	u4_t i;
	for (i = 0; i < c->numberFields; i++) {
		if (strcmp(c->fields[i].fieldName, fieldname) == 0)
			return c->fields[i].fieldOffset;
	}
	return -1;
}

/* find class looking through all shared libs */
/* TODO: the same classname could be used in different
   shared libs. These libs cannot be used together but
   can both be available as global shared lib! */
ClassDesc *findClassDesc(char *name)
{
	SharedLibDesc *sharedLib;
	ClassDesc *cl;

	if (strcmp(name, "java/lang/Object") == 0)
		return java_lang_Object;

	sharedLib = sharedLibs;
	while (sharedLib != NULL) {
		if (testHashKey(name, sharedLib->key, LIB_HASHKEY_LEN)) {
			cl = findClassDescInSharedLib(sharedLib, name);
			if (cl != NULL)
				return cl;
		}
		sharedLib = (SharedLibDesc *) sharedLib->next;
	}
	return NULL;
}

Class *findClass(DomainDesc * domain, char *name)
{
	jint i;
	Class *cl;

	if (strcmp(name, "java/lang/Object") == 0)
		return java_lang_Object_class;

	for (i = 0; i < domain->numberOfLibs; i++) {
		if (testHashKey(name, domain->libs[i]->key, LIB_HASHKEY_LEN)) {
			cl = findClassInLib(domain->libs[i], name);
			if (cl != NULL)
				return cl;
		}
	}

	if (name[0] == '[') {
		Class *acl;
		DISABLE_IRQ;
		acl = domain->arrayClasses;
		for (; acl != NULL; acl = ((ArrayClassDesc *) (acl->classDesc))->nextInDomain) {
			if (strcmp(acl->classDesc->name, name) == 0) {
				cl = (Class *) acl;
				goto finished;
			}
		}
		cl = createArrayClass(domain, name);
	      finished:
		RESTORE_IRQ;
		return cl;
	}
	return NULL;
}

LibDesc *sharedLib2Lib(DomainDesc * domain, SharedLibDesc * slib)
{
#ifdef USE_LIB_INDEX
	//  ASSERT(domain->numberOfLibs > slib->ndx);
	if (slib == 0)
		return (LibDesc *) 0;
#ifdef DEBUG
	if (domain->ndx_libs[slib->ndx] != NULL)
		ASSERTLIB(domain->ndx_libs[slib->ndx]);
#endif
	return domain->ndx_libs[slib->ndx];
#else
	int i;
	jint nlibs;
	LibDesc **libs;

	nlibs = domain->numberOfLibs;
	libs = domain->libs;

	for (i = 0; i < nlibs; i++) {
		if (slib != libs[i]->sharedLib)
			continue;
		return libs[i];
	}

	return (LibDesc *) 0;
#endif
}

ClassDesc *nonatomic_handle2ClassDesc(ObjectDesc ** handle)
{
	ASSERTOBJECT(*handle);
	return obj2ClassDesc(*handle);
}

Class *classDesc2Class(DomainDesc * domain, ClassDesc * classDesc)
{
	int ndx;
	char *name;
	LibDesc *lib;
	SharedLibDesc *slib;

	name = classDesc->name;
	if (strcmp(name, "java/lang/Object") == 0)
		return java_lang_Object_class;

	if (name[0] == '<') {
		return NULL;	/* domainzero class */
	}

	if (name[0] != '[') {

		slib = classDesc->definingLib;
		if ((lib = sharedLib2Lib(domain, slib))) {
			ndx = (int) (classDesc - (slib->allClasses));
			return &(lib->allClasses[ndx]);
		}

		sys_panic("Could not find class %s in domain %s!\n", name, domain->domainName);
	} else {

		Class *acl = domain->arrayClasses;
		for (; acl != NULL; acl = ((ArrayClassDesc *) (acl->classDesc))->nextInDomain) {
			if (strcmp(acl->classDesc->name, name) == 0) {
				return (Class *) acl;
			}
		}
		return createArrayClass(domain, name);

	}

	return NULL;
}

/* returns number of found classes */
int findSubClassesInLib(LibDesc * lib, Class * c, Class ** subclassesFound, int bufsize)
{
	jint i, j;
	if (bufsize == 0)
		return 0;
	j = 0;
	for (i = 0; i < lib->numberOfClasses; i++) {
		if (lib->allClasses[i].superclass == c) {
			subclassesFound[j++] = &(lib->allClasses[i]);
			//printf("**FOUND SUB: %s\n", lib->allClasses[i].classDesc->name);
			if (bufsize == j)
				return j;
		}
	}
	return j;
}

int findSubClasses(DomainDesc * domain, Class * c, Class ** subclassesFound, int bufsize)
{
	jint i;
	jint found;
	jint total = 0;
	for (i = 0; i < domain->numberOfLibs; i++) {
		found = findSubClassesInLib(domain->libs[i], c, subclassesFound, bufsize);
		total += found;
		subclassesFound += found;
		bufsize -= found;
	}
	return total;
}

/*
 * support system
 */

char *methodName2str(ClassDesc * class, MethodDesc * method, char *buffer, int size)
{
	int i, p;
#ifdef COMPACT_EIP_INFO
	int o;
#endif
	char *src;

	if (class == NULL) {
		buffer[0] = 0;
		return buffer;
	}

	src = class->name;
	p = 0;
	for (i = 0; p < (size - 2); i++) {
		if (src[i] == 0)
			break;
		if ((src[i] == '/') || (src[i] == '\\')) {
			buffer[p++] = '.';
		} else {
			buffer[p++] = src[i];
		}
	}
	buffer[p++] = '.';

	if (method == NULL) {
		buffer[p] = 0;
		return buffer;
	}

	src = method->name;
	for (i = 0; p < (size - 1); i++) {
		if (src[i] == 0)
			break;
		buffer[p++] = src[i];
	}
	src = method->signature;
#ifdef COMPACT_EIP_INFO
	o = 0;
	for (i = 0; p < (size - 1); i++) {
		if (src[i] == 0)
			break;
		if (o == 0) {
			buffer[p++] = src[i];
			if (src[i] == 'L')
				o = p;
		} else {
			if (src[i] == '/') {
				p = o;
				continue;
			}
			buffer[p++] = src[i];
			if (src[i] == ';')
				o = 0;
		}
	}
#else
	for (i = 0; p < (size - 1); i++) {
		if (src[i] == 0)
			break;
		buffer[p++] = src[i];
	}
#endif
	buffer[p++] = 0;

	return buffer;
}

code_t findAddrOfMethodBytecode(char *className, char *method, char *signature, jint bytecodePos)
{
	MethodDesc *m = findMethodInSharedLibs(className, method, signature);
	if (m == NULL)
		return NULL;
	return m->code;
}


ObjectDesc *allocObject(ClassDesc * c)
{
	ASSERTCLASSDESC(c);
	return allocObjectInDomain(curdom(), c);
}

jint getArraySize(ArrayDesc * array)
{
	if (array == NULL)
		sys_panic("array NULLPOINTER");
	return array->size;
}

ObjectDesc *getReferenceArrayElement(ArrayDesc * array, jint pos)
{
	ASSERT(pos < array->size);
	return (ObjectDesc *) array->data[pos];
}

void copyIntoCharArray(ArrayDesc * array, char *str, jint size)
{
	jint i;
#ifdef ALL_ARRAYS_32BIT
	u4_t *field = (u4_t *) (array->data);
#else
#ifdef CHARS_8BIT
	u1_t *field = (u1_t *) (array->data);
#else
	u2_t *field = (u2_t *) (array->data);
#endif
#endif

	ASSERT_ARRAYTYPE(array, 'C');

	for (i = 0; i < size; i++) {
		field[i] = str[i];
	}
}

void copyFromCharArray(char *buf, jint buflen, ArrayDesc * array)
{
	jint i, size;
#ifdef ALL_ARRAYS_32BIT
	u4_t *field = (u4_t *) (array->data);
#else
#ifdef CHARS_8BIT
	u1_t *field = (u1_t *) (array->data);
#else
	u2_t *field = (u2_t *) (array->data);
#endif
#endif

	if (buflen == 0)
		return;

	buflen--;
	size = (array->size < buflen) ? array->size : buflen;

	for (i = 0; i < size; i++) {
		buf[i] = field[i];
	}

	buf[i] = 0;
}

void copyIntoByteArray(ArrayDesc * array, char *str, jint size)
{
	jint i;
#ifdef ALL_ARRAYS_32BIT
	u4_t *field = (u4_t *) (array->data);
#else
	u1_t *field = (u1_t *) (array->data);
#endif

	for (i = 0; i < size; i++) {
		field[i] = str[i];
	}
}

jint string_Length(ObjectDesc * str)
{
	return ((ArrayDesc *) str->data[0])->size;
}

int string_CompareChar(ObjectDesc * str, const char *c)
{
	ArrayDesc *charArray;
	charArray = (ArrayDesc *) str->data[0];
	return strncmp(c, charArray->data, charArray->size);
}

ObjectDesc *string_replace_char(ObjectDesc * str, jint c1, jint c2)
{
	jint i;
	ArrayDesc *array;
	array = (ArrayDesc *) str->data[0];
	for (i = 0; i < array->size; i++) {
		if (array->data[i] == c1)
			array->data[i] = c2;
	}
	return str;
}

void stringToChar(ObjectDesc * str, char *c, jint buflen)
{
	ArrayDesc *arrObj;
	arrObj = (ArrayDesc *) str->data[0];
	copyFromCharArray(c, buflen, arrObj);
}

ObjectDesc *newString(DomainDesc * domain, char *value)
{
	ObjectDesc *s;
	ArrayDesc *arrObj;
	jint size;

	s = (ObjectDesc *) allocObjectInDomain(domain, findClassDesc("java/lang/String"));
	size = strlen(value);
	arrObj = allocArrayInDomain(domain, class_C->classDesc, size);
	s->data[0] = (jint) arrObj;
	copyIntoCharArray(arrObj, value, size);
	return s;
}

ObjectDesc *newStringArray(DomainDesc * domain, int size, char *arr[])
{
	ObjectDesc *sObj;
	ArrayDesc *arrObj;
	ClassDesc *acl;
	char *s;
	int i;

	//printf("SIZE: %d\n", size);

	acl = findClass(domain, "[Ljava/lang/String;")->classDesc;
	arrObj = allocArrayInDomain(domain, acl, size);

	for (i = 0; i < size; i++) {
		s = arr[i];
		sObj = newString(domain, s);
		arrObj->data[i] = sObj;
	}
	return arrObj;
}

ObjectDesc *newStringFromClassname(DomainDesc * domain, char *value)
{
	ObjectDesc *s;
	ArrayDesc *arrObj;
	jint size, i;

	s = (ObjectDesc *) allocObjectInDomain(domain, findClassDesc("java/lang/String"));
	size = strlen(value);
	arrObj = allocArrayInDomain(domain, class_C->classDesc, size);
	s->data[0] = (jint) arrObj;

	for (i = 0; i < size; i++) {
		if (value[i] == '/') {
			arrObj->data[i] = '.';
		} else {
			arrObj->data[i] = value[i];
		}
	}

	return s;
}

/* TODO: alloc shared strings in non-movable area (code area); this would allow to GC the heap of DomainZero */
ObjectDesc *newDomainZeroString(char *value)
{
	ObjectDesc *o = newString(domainZero, value);
	setObjFlags(o, OBJFLAGS_EXTERNAL_STRING);
	return o;
}


/*
 * load/link/execute
 */
#ifdef KERNEL
#define READFROMZIP 1
#endif

#ifdef READFROMZIP
char *read_codefile(char *filename, jint * size)
{
	zipentry entry;
	char *codefile;

	if ((codefile = libcache_lookup_jll(filename, size)) != NULL) {
		return codefile;
	}

	zip_reset();
	for (;;) {
		if (zip_next_entry(&entry) == -1)
			return NULL;
		//printf("%s\n",entry.filename);
		if (strcmp(entry.filename, filename) == 0) {
			codefile = entry.data;
			*size = entry.uncompressed_size;
			return codefile;
		}
	}
	return NULL;
}
#else
char *read_codefile(char *filename, jint * size)
{
	int fd;
	struct stat statbuf;
	char path[128];
	char *codefile;

	if ((codefile = libcache_lookup_jll(filename, size)) != NULL) {
		return codefile;
	}

	fd = open(filename, O_RDONLY);
	if (fd == -1) {
		strcpy(path, "../libs/");
		strcat(path, filename);
		fd = open(path, O_RDONLY);
		if (fd == -1) {
			strcpy(path, "../domains/");
			strcat(path, filename);
			fd = open(path, O_RDONLY);
			if (fd == -1) {
				return NULL;
			}
		}
	}
	if (fstat(fd, &statbuf) == -1)
		return NULL;
	codefile = (char *) jxmalloc(statbuf.st_size MEMTYPE_EMULATION);
	if (read(fd, codefile, statbuf.st_size) != statbuf.st_size)
		return NULL;
	close(fd);
	*size = statbuf.st_size;
	return codefile;
}
#endif

SharedLibDesc *loadSharedLibrary(DomainDesc * domain, char *filename, TempMemory * tmp_mem);
static LibDesc *loadLib(DomainDesc * domain, SharedLibDesc * sharedLib);

SharedLibDesc *findSharedLib(char *libname)
{
	SharedLibDesc *sharedLib;

	sharedLib = sharedLibs;
	while (sharedLib != NULL) {
		if (strcmp(sharedLib->name, libname) == 0) {
			/* found lib */
			break;
		}
		sharedLib = (SharedLibDesc *) sharedLib->next;
	}

	return sharedLib;
}

LibDesc *load(DomainDesc * domain, char *filename)
{
	SharedLibDesc *sharedLib;

	//printf("load %s\n",filename);

	/* try to find an already loaded library */
	sharedLib = findSharedLib(filename);

	if (sharedLib == NULL) {
		/* could not find a loaded lib, now try to load it */
		TempMemory *tmp_mem = jxmalloc_tmp(6000);

		/*FIXME:  shared libraries should not always be loaded into domainzero */
		sharedLib = loadSharedLibrary(domainZero, filename, tmp_mem);

		if (sharedLib == NULL)
			sys_panic("could not load shared library \"%s\"", filename);
		ASSERTSLIB(sharedLib);

		linksharedlib(domainZero, sharedLib, (jint) specialAllocObject, (jint) vmSpecialAllocArray, tmp_mem);
		jxfree_tmp(tmp_mem);

	}

	return loadLib(domain, sharedLib);
}

void loadIt(DomainDesc * domain, char *libname)
{
	TempMemory *tmp_mem = jxmalloc_tmp(5000);
	SharedLibDesc *sharedLib = loadSharedLibrary(domain, libname, tmp_mem);
	loadLib(domain, sharedLib);
	linksharedlib(domain, sharedLib, (jint) specialAllocObject, (jint) vmSpecialAllocArray, tmp_mem);
	jxfree_tmp(tmp_mem);
}


/* is called by no more than one thread per domain at a time */
LibDesc *loadLib(DomainDesc * domain, SharedLibDesc * sharedLib)
{
	int i;
	LibDesc *lib;

	/*
	   load neede Libs
	 */

	for (i = 0; i < sharedLib->numberOfNeededLibs; i++) {
		if (sharedLib2Lib(domain, sharedLib->neededLibs[i]) == NULL) {
			//printf("%s miss %d %p ",sharedLib->name,i,sharedLib->neededLibs[i]);
			//printf("->%s\n",sharedLib->neededLibs[i]->name);
			loadLib(domain, sharedLib->neededLibs[i]);
		}
	}

	if (domain->numberOfLibs == domain->maxNumberOfLibs) {
		sys_panic("max number of libs in domain %s reached!", domain->domainName);
		return NULL;
	}

	lib = malloc_libdesc(domain);
	memset(lib, 0, sizeof(LibDesc));
#ifdef USE_QMAGIC
	lib->magic = MAGIC_LIB;
#endif
	lib->sharedLib = sharedLib;

	/* insert the lib in the domain */
	domain->libs[domain->numberOfLibs++] = lib;
#ifdef USE_LIB_INDEX
	if (sharedLib->ndx < domain->maxNumberOfLibs) {
		domain->ndx_libs[sharedLib->ndx] = lib;
	} else {
		sys_panic("max number of libs reached! %d\n", domain->maxNumberOfLibs);
	}
#endif

	/* create the non-shared part of all classes */
	lib->numberOfClasses = sharedLib->numberOfClasses;
	lib->allClasses = specialAllocClass(domain, lib->numberOfClasses);

#ifdef USE_LIB_INDEX
	{
		jint static_offset;
		jint *sfield = NULL;

		static_offset = 0;
		if (sharedLib->memSizeStaticFields != 0) {
			sfield = specialAllocStaticFields(domain, sharedLib->memSizeStaticFields);
			memset(sfield, 0, sharedLib->memSizeStaticFields * sizeof(jint));
			domain->sfields[sharedLib->ndx] = sfield;
		}

		for (i = 0; i < sharedLib->numberOfClasses; i++) {
			int ssize;
			ClassDesc *cd;
			Class *cl;
#ifdef USE_QMAGIC
			lib->allClasses[i].magic = MAGIC_CLASS;
#endif

			lib->allClasses[i].classDesc = &sharedLib->allClasses[i];
			cd = &sharedLib->allClasses[i];
			cl = &lib->allClasses[i];
			ssize = cd->staticFieldsSize;

			cl->objectDesc_flags = OBJFLAGS_EXTERNAL_CLASS;
#ifdef USE_QMAGIC
			cl->magic = MAGIC_CLASS;
			cl->objectDesc_magic = MAGIC_OBJECT;
#endif

			addHashKey(cd->name, lib->key, LIB_HASHKEY_LEN);
			/* superclass */
			if (cd->superclass == NULL) {
				cl->superclass = NULL;
			} else {
				cl->superclass = classDesc2Class(domain, sharedLib->allClasses[i].superclass);
			}

			if (ssize != 0) {
				cl->staticFields = &sfield[static_offset];
				static_offset += ssize;
			} else {
				cl->staticFields = 0;
				cd->sfield_offset = 0;
			}
		}
	}
#else
	for (i = 0; i < sharedLib->numberOfClasses; i++) {
		lib->allClasses[i].classDesc = &sharedLib->allClasses[i];
#ifdef USE_QMAGIC
		lib->allClasses[i].magic = MAGIC_CLASS;
		lib->allClasses[i].objectDesc_magic = MAGIC_OBJECT;
#endif
		addHashKey(sharedLib->allClasses[i].name, lib->key, LIB_HASHKEY_LEN);
		/* superclass */
		if (sharedLib->allClasses[i].superclass == NULL) {
			lib->allClasses[i].superclass = NULL;
		} else {
			lib->allClasses[i].superclass = classDesc2Class(domain, sharedLib->allClasses[i].superclass);
		}
		/* static fields */
		if (sharedLib->allClasses[i].staticFieldsSize != 0) {
			lib->allClasses[i].staticFields =
			    specialAllocStaticFields(domain, sharedLib->allClasses[i].staticFieldsSize);
			memset(lib->allClasses[i].staticFields, 0, sharedLib->allClasses[i].staticFieldsSize * 4);
		} else {
			lib->allClasses[i].staticFields = 0;
		}

	}
#endif

	return lib;
}

char *testCheckSumAndVersion(char *filename, char *codefile, int size)
{
	jint i, checksum, version;
	char *codefilepos;
	char processor[20];

	codefilepos = codefile;

	checksum = 0;
	for (i = 0; i < size - 4; i++) {
		checksum = (checksum ^ (*(jbyte *) (codefile + i))) & 0xff;
	}

	if (checksum != *(jint *) (codefile + size - 4)) {
		//printf("%s: COMPUTED CHECKSUM: %ld\n", filename, checksum);
		//printf("    STORED CHECKSUM: %ld \n", *(jint*)(codefile + size - 4));
		sys_panic("WRONG CHECKSUM");
	}

	readInt(version);
	debugf(("Version: %ld\n", version));
	if (version != CURRENT_COMPILER_VERSION) {
		//printf("Library name=%s version=%ld. ", filename, version);
		//printf("Cannot load code with version != %d\n", CURRENT_COMPILER_VERSION);
		sys_panic("Mismatch between library version and version supported by jxcore");
	}
	readString(processor, sizeof(processor));
	//debugf(("Processor: %s\n", processor));

	return codefilepos;
}

SharedLibDesc *loadSharedLibrary(DomainDesc * domain, char *filename, TempMemory * tmp_mem)
{
	jint i, j, k, m;
	jint completeCodeBytes;
	jint completeVtableSize = 0;
	jint completeBytecodeSize = 0;
	char *supername;
	char libname[32];
	SharedLibDesc *lib;
	jint totalNumberOfClasses;
	SharedLibDesc *neededLib;
	char *codefilepos;
	char **string_table;
	jint dummy;
	jint size;
	jint isinterface;
#ifdef USE_LIB_INDEX
	jint sfields_offset;
#endif

	DISABLE_IRQ;

	/* FIXME: Don't check DomainZero */
	if (domain->id != 0) {
		/* check if sufficient stack space exist to run this function */
		CHECK_STACK_SIZE(domain, 512);
	}

	if ((codefilepos = read_codefile(filename, &size)) == NULL) {
		//printf("%s not found!\n",filename);
		return NULL;
	}

	codefilepos = testCheckSumAndVersion(filename, codefilepos, size);

	lib = malloc_sharedlibdesc(domain, strlen(filename) + 1);
#ifdef USE_QMAGIC
	lib->magic = MAGIC_SLIB;
#endif
	strcpy(lib->name, filename);
#ifdef USE_LIB_INDEX
	lib->ndx = -1;
#endif

	/*
	   reserved for option fields
	 */

	readInt(i);

	/*
	   load needed libs
	 */

	readInt(lib->numberOfNeededLibs);

	if (lib->numberOfNeededLibs == 0) {
		lib->neededLibs == NULL;
	} else {
		lib->neededLibs = malloc_sharedlibdesctable(domain, lib->numberOfNeededLibs);
	}

	for (i = 0; i < lib->numberOfNeededLibs; i++) {
		readString(libname, sizeof(libname));
		neededLib = findSharedLib(libname);

		if (neededLib == NULL) {
			/*  could not find a loaded lib, now try to load it  */
			TempMemory *tmp_mem = jxmalloc_tmp(5000);

			/* FIXME:  shared libraries should not always be loaded into domainzero  */
			//printf("slib %s load %s\n",lib->name,libname);
			neededLib = loadSharedLibrary(domain, libname, tmp_mem);
			if (neededLib == NULL) {
				//printf("Could not load shared library %s needed by %s!\n",libname,filename);
			} else {
				//printf("link %s ",libname);
				linksharedlib(domain, neededLib, (jint) specialAllocObject, (jint) vmSpecialAllocArray, tmp_mem);
				//printf("done.\n");
			}

			jxfree_tmp(tmp_mem);
		}

		ASSERTSLIB(neededLib);
		lib->neededLibs[i] = neededLib;
	}

	//printf("load %s\n", filename);
	/*
	   load meta
	 */
	readInt(lib->numberOfMeta);
	lib->meta = malloc_metatable(domain, lib->numberOfMeta);
	for (i = 0; i < lib->numberOfMeta; i++) {
		readAllocString(lib->meta[i].var);
		readAllocString(lib->meta[i].val);
		//printf("%s = %s\n", lib->meta[i].var, lib->meta[i].val);
	}

	/*
	   read string table
	 */

	readInt(i);
	if (i == 0) {
		string_table = NULL;
	} else {
		string_table = (char **) malloc_code(domain, i * sizeof(char *));
		for (j = 0; j < i; j++)
			readAllocString(string_table[j]);
	}

	/*
	   vmsymbol-table
	 */

	readInt(i);
	if (i > 0) {
		char symbol[30];
		for (j = 0; j < i; j++) {
			readString(symbol, sizeof(symbol));
			/* printf("%2d %s ",j,symbol); */
			if (j == numberVMOperations)
				sys_panic("to many symbols");
			vmsupport[j].index = 0;
			for (k = 0; k < numberVMOperations; k++) {
				if (strcmp(symbol, vmsupport[k].name) == 0) {
					vmsupport[j].index = k;
					break;
				}
			}
			/* printf("-> %2d 0x%lx %s\n",vmsupport[j].index,VMSUPPORT(j).fkt,VMSUPPORT(j).name); */
		}
	}

	/*
	   load classes
	 */

	readInt(totalNumberOfClasses);
	lib->numberOfClasses = 0;
	lib->allClasses = malloc_classdescs(domain, totalNumberOfClasses);
	memset(lib->allClasses, 0, sizeof(ClassDesc) * totalNumberOfClasses);
	completeCodeBytes = 0;
#ifdef USE_LIB_INDEX
	sfields_offset = 0;
	lib->memSizeStaticFields = 0;
#endif
	for (i = 0; i < totalNumberOfClasses; i++) {
		lib->allClasses[i].classType = CLASSTYPE_CLASS;
#ifdef USE_QMAGIC
		lib->allClasses[i].magic = MAGIC_CLASSDESC;
#endif
		lib->allClasses[i].definingLib = lib;

		readStringID(lib->allClasses[i].name);
		addHashKey(lib->allClasses[i].name, lib->key, LIB_HASHKEY_LEN);

		//printf("Class: %s\n", lib->allClasses[i].name);

		readStringID(supername);
		if (supername[0] == '\0') {
			lib->allClasses[i].superclass = NULL;
		} else {
			ClassDesc *scl = NULL;
			if (strcmp(supername, "java/lang/Object") == 0) {
				scl = java_lang_Object;
			}
			if (scl == NULL)
				scl = findClassDescInSharedLib(lib, supername);
			if (scl == NULL)
				scl = findClassDesc(supername);
			if (scl == NULL)
				sys_panic("find superclass");
			lib->allClasses[i].superclass = scl;
		}

		readInt(isinterface);
		if (isinterface) {
			lib->allClasses[i].classType = CLASSTYPE_INTERFACE;
		}
		readInt(lib->allClasses[i].numberOfInterfaces);
		if (lib->allClasses[i].numberOfInterfaces > 0) {
			lib->allClasses[i].interfaces = malloc_classdesctable(domain, lib->allClasses[i].numberOfInterfaces);
			lib->allClasses[i].ifname =
			    malloc_tmp_stringtable(domain, tmp_mem, lib->allClasses[i].numberOfInterfaces);
		} else {
			lib->allClasses[i].interfaces = (ClassDesc **) NULL;
			lib->allClasses[i].ifname = NULL;
		}
		for (j = 0; j < lib->allClasses[i].numberOfInterfaces; j++) {
			readStringID(lib->allClasses[i].ifname[j]);
		}
		readInt(lib->allClasses[i].numberOfMethods);
		if (lib->allClasses[i].numberOfMethods > 0) {
			lib->allClasses[i].methods = malloc_methoddescs(domain, lib->allClasses[i].numberOfMethods);
		} else {
			lib->allClasses[i].methods = NULL;
		}
		readInt(lib->allClasses[i].instanceSize);

		/* fieldmap */
		readInt(lib->allClasses[i].mapBytes);
		lib->allClasses[i].map = NULL;
		if (lib->allClasses[i].mapBytes > 0) {
			lib->allClasses[i].map = malloc_objectmap(domain, lib->allClasses[i].mapBytes);
			for (j = 0; j < lib->allClasses[i].mapBytes; j++) {
				readByte(lib->allClasses[i].map[j]);
			}
		}

		/* fieldlist */
		readInt(lib->allClasses[i].numberFields);
		lib->allClasses[i].fields = NULL;
		if (lib->allClasses[i].numberFields > 0) {
			lib->allClasses[i].fields = malloc_fielddescs(domain, lib->allClasses[i].numberFields);
			for (j = 0; j < lib->allClasses[i].numberFields; j++) {
				readStringID(lib->allClasses[i].fields[j].fieldName);
				readStringID(lib->allClasses[i].fields[j].fieldType);
				readInt(lib->allClasses[i].fields[j].fieldOffset);
			}
		}


		/* static fields */
		readInt(lib->allClasses[i].staticFieldsSize);
#ifdef USE_LIB_INDEX
		lib->allClasses[i].sfield_offset = sfields_offset;
		sfields_offset += lib->allClasses[i].staticFieldsSize;
		lib->memSizeStaticFields += lib->allClasses[i].staticFieldsSize;
#endif

		/* static maps */
		readInt(lib->allClasses[i].staticsMapBytes);
		lib->allClasses[i].staticsMap = NULL;
		if (lib->allClasses[i].staticsMapBytes > 0) {
			lib->allClasses[i].staticsMap = malloc_staticsmap(domain, lib->allClasses[i].staticsMapBytes);
			for (j = 0; j < lib->allClasses[i].staticsMapBytes; j++) {
				readByte(lib->allClasses[i].staticsMap[j]);
			}
		}
		readInt(dummy);


		completeBytecodeSize += dummy;
		/*printf("class=%s bytecodesize=%d\n", lib->allClasses[i].name, dummy); */

		readInt(dummy);

		/* read vtable */
		readInt(lib->allClasses[i].vtableSize);
		completeVtableSize += lib->allClasses[i].vtableSize;
		if (lib->allClasses[i].vtableSize != 0) {
			lib->allClasses[i].vtableSym = malloc_vtableSym(domain, lib->allClasses[i].vtableSize);
			for (j = 0; j < lib->allClasses[i].vtableSize * 3; j += 3) {
				readStringID(lib->allClasses[i].vtableSym[j]);	/* class */
				readStringID(lib->allClasses[i].vtableSym[j + 1]);	/* name */
				readStringID(lib->allClasses[i].vtableSym[j + 2]);	/* type */
				readInt(dummy);
			}
		} else {
			debugf(("VTableSize == 0\n"));
		}

		for (j = 0; j < lib->allClasses[i].numberOfMethods; j++) {
			int symsize;
			lib->allClasses[i].methods[j].objectDesc_flags = OBJFLAGS_EXTERNAL_METHOD;
#ifdef USE_QMAGIC
			lib->allClasses[i].methods[j].magic = MAGIC_METHODDESC;
			lib->allClasses[i].methods[j].objectDesc_magic = MAGIC_OBJECT;
#endif
			if (vmmethodClass)
				lib->allClasses[i].methods[j].objectDesc_vtable = vmmethodClass->vtable;

			readStringID(lib->allClasses[i].methods[j].name);
			readStringID(lib->allClasses[i].methods[j].signature);

			//printf("  Method: %s%s\n", lib->allClasses[i].methods[j].name, lib->allClasses[i].methods[j].signature);

			readInt(lib->allClasses[i].methods[j].sizeLocalVars);
			lib->allClasses[i].methods[j].classDesc = &(lib->allClasses[i]);
#ifdef PROFILE
			lib->allClasses[i].methods[j].isprofiled = JNI_FALSE;
#endif
			debugf(("  Method: %s.%s%s\n", lib->allClasses[i].name, lib->allClasses[i].methods[j].name,
				lib->allClasses[i].methods[j].signature));
			readInt(lib->allClasses[i].methods[j].numberOfCodeBytes);
			debugf(("     NumberOfCodeBytes: %ld\n", lib->allClasses[i].methods[j].numberOfCodeBytes));
			ASSERT(lib->allClasses[i].methods[j].numberOfCodeBytes >= 0);
			lib->allClasses[i].methods[j].sizeOfExceptionTable = 0;

			readInt(lib->allClasses[i].methods[j].numberOfArgTypeMapBytes);
			readInt(lib->allClasses[i].methods[j].numberOfArgs);
			lib->allClasses[i].methods[j].argTypeMap = NULL;
			if (lib->allClasses[i].methods[j].numberOfArgTypeMapBytes > 0) {
				lib->allClasses[i].methods[j].argTypeMap =
				    malloc_argsmap(domain, lib->allClasses[i].methods[j].numberOfArgTypeMapBytes);
				for (m = 0; m < lib->allClasses[i].methods[j].numberOfArgTypeMapBytes; m++) {
					readByte(lib->allClasses[i].methods[j].argTypeMap[m]);
				}
			}
			readInt(lib->allClasses[i].methods[j].returnType);
			/* printf("  Method: %s %s %d\n", lib->allClasses[i].methods[j].name, lib->allClasses[i].methods[j].signature,
			   lib->allClasses[i].methods[j].returnType
			   ); */
			readInt(lib->allClasses[i].methods[j].flags);

			lib->allClasses[i].methods[j].codeOffset = completeCodeBytes;
			completeCodeBytes += lib->allClasses[i].methods[j].numberOfCodeBytes;
			readInt(lib->allClasses[i].methods[j].numberOfSymbols);
			if (lib->allClasses[i].methods[j].numberOfSymbols > 0) {
				lib->allClasses[i].methods[j].symbols = malloc_symboltable(domain, lib->allClasses[i].methods[j].numberOfSymbols);	/* FIXME: alloc them in temp memory ? */
			} else {
				lib->allClasses[i].methods[j].symbols = NULL;
			}
			debugf(("     NumberOfSymbols: %ld\n", lib->allClasses[i].methods[j].numberOfSymbols));
			for (k = 0; k < lib->allClasses[i].methods[j].numberOfSymbols; k++) {
				jint type;
				jint immediateNCIndex;
				jint numBytes;
				jint nextInstrNCIndex;

				readInt(type);
				readInt(immediateNCIndex);
				readInt(numBytes);
				readInt(nextInstrNCIndex);
/*
				readShort(type);
				readShort(immediateNCIndex);
				readShort(numBytes);
				readShort(nextInstrNCIndex);
*/
				if ((immediateNCIndex + numBytes) > lib->allClasses[i].methods[j].numberOfCodeBytes) {
					sys_panic
					    ("wrong patch index: %d in method %s.%s\nMethod has %d codebytes.\nNumber of bytes to patch is %d.\n",
					     immediateNCIndex, lib->allClasses[i].name, lib->allClasses[i].methods[j].name,
					     lib->allClasses[i].methods[j].numberOfCodeBytes, numBytes);
				}
				switch (type) {
				case 0:
					sys_panic("Error: Symboltype 0");
					break;
				case 1:{	/* DomainZeroSTEntry */
						debugs(("     Symbol: DomainZero\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescDomainZero));
						break;
					}
				case 2:{	/* ExceptionHandlerSTEntry */
						debugs(("     Symbol: ExceptionHandler\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescExceptionHandler));
						break;
					}
				case 3:{	/* DEPFunctionSTEntry */
						SymbolDescDEPFunction *s;
						debugs(("     Symbol: DEPFunction\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescDEPFunction));
						s = (SymbolDescDEPFunction *) lib->allClasses[i].methods[j].symbols[k];
						readAllocString(s->className);
						readAllocString(s->methodName);
						readAllocString(s->methodSignature);
						break;
					}
				case 4:{	/* StaticFieldSTEntry */
						SymbolDescStaticField *s;
						debugs(("     Symbol: StaticField\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescStaticField));
						s = (SymbolDescStaticField *) lib->allClasses[i].methods[j].symbols[k];
						readStringID(s->className);
						readInt(s->kind);
						readInt(s->fieldOffset);
						break;
					}
				case 5:{	/* AllocObjectSTEntry */
						debugs(("     Symbol: AllocObject\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescAllocObject));
						break;
					}
				case 6:{	/* ClassSTEntry */
						SymbolDescClass *s;
						debugs(("     Symbol: Class\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescClass));
						s = (SymbolDescClass *)
						    lib->allClasses[i].methods[j].symbols[k];
						readStringID(s->className);
						break;
					}
				case 7:{	/* DirectMethodCallSTEntry */
						SymbolDescDirectMethodCall *s;
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescDirectMethodCall));
						s = (SymbolDescDirectMethodCall *) lib->allClasses[i].methods[j].symbols[k];
						//readAllocString(s->className);
						//readAllocString(s->methodName);
						//readAllocString(s->methodSignature);
						readStringID(s->className);
						readStringID(s->methodName);
						readStringID(s->methodSignature);
						debugs(("     Symbol: DirectMethodCall %s.%s%s\n", s->className, s->methodName,
							s->methodSignature));
						break;
					}
				case 8:{	/* StringSTEntry */
						SymbolDescString *s;
						debugs(("     Symbol: String\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescString));
						s = (SymbolDescString *)
						    lib->allClasses[i].methods[j].symbols[k];
						readStringID(s->value);
						//readAllocString(s->value);
						break;
					}
				case 9:{	/* AllocArraySTEntry */
						debugs(("     Symbol: AllocArray\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescAllocArray));
						break;
					}
				case 10:{	/* AllocMultiArraySTEntry */
						debugs(("     Symbol: MultiAllocArray\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescAllocMultiArray));
						break;
					}
				case 11:{	/* LongArithmeticSTEntry */
						SymbolDescLongArithmetic *s;
						debugs(("     Symbol: LongArithmetic\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescLongArithmetic));
						s = (SymbolDescLongArithmetic *) lib->allClasses[i].methods[j].symbols[k];
						readInt(s->operation);
						break;
					}
				case 12:{	/* VMSupportSTEntry */
						SymbolDescVMSupport *s;
						debugs(("     Symbol: VMSupport\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescVMSupport));
						s = (SymbolDescVMSupport *)
						    lib->allClasses[i].methods[j].symbols[k];
						readInt(s->operation);
						break;
					}
				case 13:{	/* PrimitiveClassSTEntry */
						SymbolDescPrimitiveClass *s;
						debugs(("     Symbol: PrimitiveClass\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescPrimitiveClass));
						s = (SymbolDescPrimitiveClass *) lib->allClasses[i].methods[j].symbols[k];
						readInt(s->primitiveType);
						break;
					}
				case 14:{	/* UnresolvedJump */
						SymbolDescUnresolvedJump *s;
						debugs(("     Symbol: UnresolvedJump\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescUnresolvedJump));
						s = (SymbolDescUnresolvedJump *) lib->allClasses[i].methods[j].symbols[k];
						readInt(s->targetNCIndex);
						break;
					}
				case 15:{	/* VMAbsoluteSTEntry */
						SymbolDescVMSupport *s;
						debugs(("     Symbol: VMAbsolute\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescVMSupport));
						s = (SymbolDescVMSupport *)
						    lib->allClasses[i].methods[j].symbols[k];
						readInt(s->operation);
						break;
					}
				case 16:	// old version
					{	/* StackMap */
						SymbolDescStackMap *s;
						int mapPos;
						debugs(("     Symbol: StackMap\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescStackMap));
						s = (SymbolDescStackMap *)
						    lib->allClasses[i].methods[j].symbols[k];
						readInt(s->n_bytes);
						readInt(s->n_bits);
						if (s->n_bytes > 0)
							s->map = malloc_stackmap(domain, s->n_bytes);
						else
							s->map = NULL;
						for (mapPos = 0; mapPos < s->n_bytes; mapPos++) {
							readByte(s->map[mapPos]);
						}
						break;
					}
				case 17:	// new version 
					{	/* StackMap */
						SymbolDescStackMap *s;
						int mapPos;
						debugs(("     Symbol: StackMap\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescStackMap));
						s = (SymbolDescStackMap *)
						    lib->allClasses[i].methods[j].symbols[k];
						readInt(s->immediateNCIndexPre);
						readInt(s->n_bytes);
						readInt(s->n_bits);
						if (s->n_bytes > 0)
							s->map = malloc_stackmap(domain, s->n_bytes);
						else
							s->map = NULL;
						for (mapPos = 0; mapPos < s->n_bytes; mapPos++) {
							readByte(s->map[mapPos]);
						}
						break;
					}
				case 18:{	/* jx.compiler.symbols.ExceptionTableSTEntry %d %d %s %p */
						SymbolDescExceptionTable *s;
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescExceptionTable));
						lib->allClasses[i].methods[j].sizeOfExceptionTable++;
						s = (SymbolDescExceptionTable *) lib->allClasses[i].methods[j].symbols[k];
						readInt(s->targetNCIndex);	/* UnresolvedJump */
						readInt(s->rangeStart);
						readInt(s->rangeEnd);
						//readAllocString(s->className);
						readStringID(s->className);
						debugs(("     Symbol: ExceptionTableSTEntry %d %d %s %p\n", s->rangeStart,
							s->rangeEnd, s->className, s->targetNCIndex));
						break;
					}
				case 19:{	/* CurrentThreadPointerSTEntry */
						debugs(("     Symbol: CurrentThreadPointerSTEntry\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescThreadPointer));
						break;
					}
				case 20:{	/* StackChunkSizeSTEntry */
						debugs(("     Symbol: StackChunkSizeSTEntry\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescStackChunkSize));
						break;
					}
				case 21:{	/* ProfileSTEntry */
						SymbolDescProfile *s;
						debugs(("     Symbol: ProfileSTEntry\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescProfile));
						s = (SymbolDescProfile *)
						    lib->allClasses[i].methods[j].symbols[k];
						readInt(s->kind);
						break;
					}
				case 22:{	/* MethodeDescSTEntry */
						debugs(("     Symbol: MethodeDescSTEntry\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescMethodeDesc));
						break;
					}
				case 23:{	/* TCBOffsetSTEntry */
						SymbolDescTCBOffset *s;
						debugs(("     Symbol: TCBOffsetSTEntry\n"));
						lib->allClasses[i].methods[j].symbols[k] =
						    malloc_symbol(domain, sizeof(SymbolDescTCBOffset));
						s = (SymbolDescTCBOffset *)
						    lib->allClasses[i].methods[j].symbols[k];
						readInt(s->kind);
						break;
					}
				default:
					sys_panic("Unknown symbol %d", type);
				}
				if (lib->allClasses[i].methods[j].symbols[k]) {
					lib->allClasses[i].methods[j].symbols[k]->type = type;
					lib->allClasses[i].methods[j].symbols[k]->immediateNCIndex = immediateNCIndex;
					lib->allClasses[i].methods[j].symbols[k]->numBytes = numBytes;
					lib->allClasses[i].methods[j].symbols[k]->nextInstrNCIndex = nextInstrNCIndex;
				}
			}

			if (lib->allClasses[i].methods[j].sizeOfExceptionTable > 0) {
				lib->allClasses[i].methods[j].exceptionTable =
				    malloc_exceptiondescs(domain, lib->allClasses[i].methods[j].sizeOfExceptionTable);
			} else {
				lib->allClasses[i].methods[j].exceptionTable = NULL;
			}

			readInt(lib->allClasses[i].methods[j].numberOfByteCodes);
			if (lib->allClasses[i].methods[j].numberOfByteCodes > 0) {
				lib->allClasses[i].methods[j].bytecodeTable =
				    malloc_bytecodetable(domain, lib->allClasses[i].methods[j].numberOfByteCodes);
				for (k = 0; k < lib->allClasses[i].methods[j].numberOfByteCodes; k++) {
					readInt(lib->allClasses[i].methods[j].bytecodeTable[k].bytecodePos);
					readInt(lib->allClasses[i].methods[j].bytecodeTable[k].start);
					readInt(lib->allClasses[i].methods[j].bytecodeTable[k].end);
					debugbt(("POS: %ld\n", lib->allClasses[i].methods[j].bytecodeTable[k].bytecodePos));
				}
			} else {
				lib->allClasses[i].methods[j].bytecodeTable = NULL;
			}


			/* read BC -> SOURCELINE mapping */

			readInt(lib->allClasses[i].methods[j].numberOfSourceLines);
			debugf(("   BC->SC: %ld\n", lib->allClasses[i].methods[j].numberOfSourceLines));
			if (lib->allClasses[i].methods[j].numberOfSourceLines > 0) {
				lib->allClasses[i].methods[j].sourceLineTable =
				    malloc_sourcelinetable(domain, lib->allClasses[i].methods[j].numberOfSourceLines);
				for (k = 0; k < lib->allClasses[i].methods[j].numberOfSourceLines; k++) {
					readInt(lib->allClasses[i].methods[j].sourceLineTable[k].startBytecode);
					readInt(lib->allClasses[i].methods[j].sourceLineTable[k].lineNumber);
				}
			} else {
				lib->allClasses[i].methods[j].sourceLineTable = NULL;
			}


		}
		lib->numberOfClasses++;
	}

	/* read code */
	lib->code = malloc_nativecode(domainZero, completeCodeBytes);
#ifdef ASSERT_ALIGNED_CODE
#define ALIGN_CODE_TO_IBUFFERSIZE 1
#ifdef ALIGN_CODE_TO_IBUFFERSIZE
	/* align code to instruction buffer size (PIII = 16 byte) */
	if ((((int) lib->code) % 16) != 0)
		printf("lib code not aligned to 16 byte addr: %p\n", lib->code);
#else
	/* align code to 32 Bit == 4 Byte */
	if ((((int) lib->code) % 4) != 0)
		printf("lib code not aligned to 4 byte addr: %p\n", lib->code);
#endif
#endif
	lib->codeBytes = completeCodeBytes;
	lib->vtablesize = completeVtableSize;
	lib->bytecodes = completeBytecodeSize;
	/*printf("Code: 0x%x (numBytes=%d)\n", (jint)code, completeCodeBytes); */
	readCode(lib->code, completeCodeBytes);

#if 0				/* TODO: check whether we can free codefile */
#ifndef READFROMZIP
	/* when reading from zip file the memory is managed in the zip module */
	free(codefile);
#endif
#endif

#ifdef USE_LIB_INDEX
	lib->ndx = sharedLibsIndexNumber;
	sharedLibsIndexNumber++;
#endif

	lib->next = sharedLibs;
	sharedLibs = lib;

	RESTORE_IRQ;

	return lib;
}

void patchByte(code_t code, SymbolDesc * symbol, jint value)
{
	char *addr;
	addr = (char *) ((char *) code + symbol->immediateNCIndex);
	addr[0] = value & 0xff;
}

void patchConstant(code_t code, SymbolDesc * symbol, jint value)
{
	char *addr;
	addr = (char *) ((char *) code + symbol->immediateNCIndex);
	addr[0] = value & 0xff;
	addr[1] = (value >> 8) & 0xff;
	addr[2] = (value >> 16) & 0xff;
	addr[3] = (value >> 24) & 0xff;
}

void patchRelativeAddress(code_t code, SymbolDesc * symbol, jint function)
{
	jint relAddr = -((jint) code + (jint) symbol->nextInstrNCIndex - (jint) function);
	patchConstant(code, symbol, relAddr);
}

void patchStaticFieldAddress(code_t code, SymbolDesc * symbol)
{
#ifdef USE_LIB_INDEX
	ClassDesc *c;
	SymbolDescStaticField *s;
	int libindex;

	s = (SymbolDescStaticField *) symbol;
	c = findClassDesc(s->className);
	if (c == NULL)
		sys_panic("could not find class");
	libindex = c->definingLib->ndx;

	switch (s->kind) {
	case 0:
		patchConstant(code, symbol, c->sfield_offset + s->fieldOffset);
		break;
	case 1:
		if (libindex < 0 || libindex > sharedLibsIndexNumber)
			sys_panic("invalid lib index\n");
		patchConstant(code, symbol, c->definingLib->ndx * sizeof(jint *));
		break;
	case 2:
		patchConstant(code, symbol, (c->sfield_offset + s->fieldOffset) * sizeof(jint *));
		break;
	case 3:
#ifndef STACK_ON_HEAP
		patchConstant(code, symbol, (0xffffffff << STACK_CHUNK));
#else
		sys_panic("stackchunk");
#endif
		break;
	default:
		sys_panic("unknown static field symbol %d\n", s->kind);
	}
#else
	ClassDesc *c;
	SymbolDescStaticField *s = (SymbolDescStaticField *) symbol;

	c = findClassDesc(s->className);
	if (c == NULL)
		sys_panic("could not find class");

	if (s->kind != 0)
		sys_panic("unknown static field symbol (%d)! compile with -DUSE_LIB_INDEX\n", s->kind);

	patchConstant(code, symbol, s->fieldOffset);
#endif
}

void patchClassPointer(code_t code, SymbolDesc * symbol)
{
	ClassDesc *c;
	SymbolDescClass *s = (SymbolDescClass *) symbol;


	if (*s->className == '[') {
		c = findSharedArrayClassDesc(s->className);
	} else {
		c = findClassDesc(s->className);
	}

	if (c == NULL)
		sys_panic("Link error: Required class not found: %s", s->className);

	patchConstant(code, symbol, (jint) c);
}

void should_not_be_called()
{
	sys_panic("Unknown method called.");
}

void patchDirectMethodAddress(code_t code, SymbolDesc * symbol)
{
	ClassDesc *c;
	MethodDesc *m;
	SymbolDescDirectMethodCall *s = (SymbolDescDirectMethodCall *) symbol;

	findClassDescAndMethod(s->className, s->methodName, s->methodSignature, &c, &m);

	if (m == NULL) {
		//#ifdef DEBUG
		printf("!!! no direct method found: %s.%s%s\n", s->className, s->methodName, s->methodSignature);
		//#endif
		patchRelativeAddress(code, symbol, (jint) should_not_be_called);
		return;
	}

	ASSERTMETHODDESC(m);
	ASSERTCLASSDESC(c);

	if (m->code == NULL) {
		//#ifdef DEBUG
		printf("!!! bad direct method address found: %s.%s%s\n", s->className, s->methodName, s->methodSignature);
		//#endif
		patchRelativeAddress(code, symbol, (jint) should_not_be_called);
		return;
	}

	debugp(("patch direct method %s.%s%s -> %p\n", s->className, s->methodName, s->methodSignature, m->code));

	patchRelativeAddress(code, symbol, (jint) m->code);
}

void patchStringAddress(code_t code, SymbolDesc * symbol)
{
	ObjectDesc *obj;
	SymbolDescString *s = (SymbolDescString *) symbol;

	obj = newDomainZeroString(s->value);	/* shared libs are part of domainzero and so are these constant strings */

	if (obj == NULL)
		sys_panic("could not create string object");
	patchConstant(code, symbol, (jint) obj);
}

jlong longDiv(jlong a, jlong b)
{
	printf("ldiv %x.%x %x.%x %x.%x\n", a, b, a / b);
	return a / b;
}

jlong longRem(jlong a, jlong b)
{
	printf("lrem %x.%x %x.%x %x.%x\n", a, b, a % b);
	return a % b;
}

jlong longShr(jlong a, jint b)
{
	return a >> (b & 63);
}

jlong longShl(jlong a, jint b)
{
	return a << (b & 63);
}

jlong longUShr(jlong a, jint b)
{
	b &= 63;
	return a >= 0 ? a >> b : (a >> b) + (2 << ~b);
}

jint longCmp(jlong a, jlong b)
{
	//printf("lcmp %x.%x %x.%x %d %d\n",a,b,b);
	if (a > b)
		return 1;
	if (a < b)
		return -1;
	return 0;
}

jlong longMul(jlong a, jlong b)
{
	return a * b;
}

code_t longops[] = {
	0,
	(code_t) longDiv,
	(code_t) longRem,
	(code_t) longShr,
	(code_t) longShl,
	(code_t) longUShr,
	(code_t) longCmp,
	(code_t) longMul,
};

void patchRelativeLongAddress(code_t code, SymbolDesc * symbol)
{
	SymbolDescLongArithmetic *s = (SymbolDescLongArithmetic *) symbol;
	patchRelativeAddress(code, symbol, (jint) (longops[s->operation]));
}

void patchPrimitiveClassPointer(code_t code, SymbolDesc * symbol)
{
	SymbolDescPrimitiveClass *s = (SymbolDescPrimitiveClass *) symbol;
	jint t = -1;
	switch ((jint) (s->primitiveType)) {
	case 0:
		t = (jint) class_I->classDesc;
		break;
	case 1:
		t = (jint) class_J->classDesc;
		break;
	case 2:
		t = (jint) class_F->classDesc;
		break;
	case 3:
		t = (jint) class_D->classDesc;
		break;
	case 5:
		t = (jint) class_B->classDesc;
		break;
	case 6:
		t = (jint) class_C->classDesc;
		break;
	case 7:
		t = (jint) class_S->classDesc;
		break;
	case 8:
		t = (jint) class_Z->classDesc;
		break;
	default:
		sys_panic("not primitive");
	}
	patchConstant(code, symbol, t);
}

void patchUnresolvedJump(code_t code, SymbolDesc * symbol)
{
	u4_t targetAddr;
	SymbolDescUnresolvedJump *s = (SymbolDescUnresolvedJump *) symbol;

	targetAddr = (u4_t) s->targetNCIndex;
	targetAddr += (u4_t) code;
	patchConstant(code, symbol, (jint) targetAddr);
}



void setCodeStart(SharedLibDesc * lib)
{
	jint i, j;
	ASSERT(lib != NULL);
	for (i = 0; i < lib->numberOfClasses; i++) {
		for (j = 0; j < lib->allClasses[i].numberOfMethods; j++) {
			if (lib->allClasses[i].methods[j].numberOfCodeBytes == 0) {
				/* abstract method */
				lib->allClasses[i].methods[j].code = 0;
			} else {
				/* resolve absolute code address */
				lib->allClasses[i].methods[j].code =
				    (code_t) (lib->code + lib->allClasses[i].methods[j].codeOffset);
			}
			debugc((" Codestart %s %s%s = 0x%lx\n", lib->allClasses[i].name, lib->allClasses[i].methods[j].name,
				lib->allClasses[i].methods[j].signature, (jint) lib->allClasses[i].methods[j].code));
		}
	}
}


void createVTable(DomainDesc * domain, ClassDesc * c)
{
	char **vtable;
	ASSERTCLASSDESC(c);
	vtable = malloc_vtable(domain, c->vtableSize + 1);
	ASSERT(vtable != NULL);
	memset(vtable, 0, 4 * c->vtableSize + 4);

	c->vtable = vtable + 1;	/* classpointer is at negative offset */
	*vtable = (char *) c;

	if (c != java_lang_Object) {
		c->methodVtable = malloc_methodVtable(domain, c->vtableSize);
	}
}

void stackMap(MethodDesc * method, SymbolDesc * symbol)
{
}

void patchMethodSymbols(MethodDesc * method, code_t code, jint allocObjectFunction, jint allocArrayFunction)
{
	int exCount, k;
	exCount = 0;
	for (k = 0; k < method->numberOfSymbols; k++) {
		if (method->symbols[k] == NULL)
			continue;
		switch (method->symbols[k]->type) {
		case 0:
			sys_panic("Error: Symboltype 0");
			break;
		case 1:{	/* DomainZeroSTEntry */
				sys_panic("DomainZeroSTEntry no longer used.");
				break;
			}
		case 2:{	/* ExceptionHandlerSTEntry */
				patchRelativeAddress(code, method->symbols[k], (jint)
						     exceptionHandler);
				break;
			}
		case 3:{	/* DEPFunctionSTEntry */
				sys_panic("DEPFUNCTION no longer supported");
				break;
			}
		case 4:{	/* StaticFieldSTEntry */
				patchStaticFieldAddress(code, method->symbols[k]);
				break;
			}
		case 5:{	/* AllocObjectSTEntry */
				patchRelativeAddress(code, method->symbols[k], (jint)
						     allocObjectFunction);
				break;
			}
		case 6:{	/* ClassSTEntry */
				patchClassPointer(code, method->symbols[k]);
				break;
			}
		case 7:{	/* DirectMethodCallSTEntry */
				patchDirectMethodAddress(code, method->symbols[k]);
				break;
			}
		case 8:{	/* StringSTEntry */
				patchStringAddress(code, method->symbols[k]);
				break;
			}
		case 9:{	/* AllocArraySTEntry */
				patchRelativeAddress(code, method->symbols[k], (jint)
						     allocArrayFunction);
				break;
			}
		case 10:{	/* AllocMultiArraySTEntry */
				patchRelativeAddress(code, method->symbols[k], (jint)
						     vmSpecialAllocMultiArray);
				break;
			}
		case 11:{	/* LongArithmeticSTEntry */
				patchRelativeLongAddress(code, method->symbols[k]);
				break;
			}
		case 12:{	/* VMSupportSTEntry */
				SymbolDescVMSupport *s = (SymbolDescVMSupport *) method->symbols[k];
				if (s->operation <= 0 || s->operation > numberVMOperations) {
					sys_panic("wrong vmsupport index");
				}
				patchRelativeAddress(code, method->symbols[k], (jint) VMSUPPORT(s->operation).fkt);
				break;
			}
		case 13:{	/* PrimitveClassSTEntry */
				patchPrimitiveClassPointer(code, method->symbols[k]);
				break;
			}
		case 14:{	/* UnresolvedJump */
				patchUnresolvedJump(code, method->symbols[k]);
				break;
			}
		case 15:{	/* VMAbsoluteSTEntry */
				SymbolDescVMSupport *s = (SymbolDescVMSupport *) method->symbols[k];
				if (s->operation <= 0 || s->operation > numberVMOperations) {
					sys_panic("wrong vmabssupport index");
				}
				patchConstant(code, method->symbols[k], (jint) VMSUPPORT(s->operation).fkt);
				break;
			}
		case 16:
		case 17:{	/* StackMap */
				stackMap(method, method->symbols[k]);
				break;
			}
		case 18:{	/* jx.compiler.symbols.ExceptionTableSTEntry  */
				SymbolDescExceptionTable *s;
				ExceptionDesc *e;

				s = (SymbolDescExceptionTable *) method->symbols[k];
				e = method->exceptionTable;

				/* e[exCount].addr  = (u4_t)s->targetNCIndex + (u4_t)code; */
				e[exCount].addr = s->targetNCIndex;
				e[exCount].start = s->rangeStart;
				e[exCount].end = s->rangeEnd;

				if (strcmp(s->className, "any") == 0) {
					e[exCount].type = NULL;
				} else {
					e[exCount].type = findClassDesc(s->className);
				}
				exCount++;

				break;
			}
		case 19:{	/* CurrentThreadPointerSTEntry */
				patchConstant(code, method->symbols[k], (jint) curthrP());
				break;
			}
		case 20:{	/* StackChunkSizeSTEntry */
#ifndef STACK_ON_HEAP
				patchByte(code, method->symbols[k], (jint) STACK_CHUNK);
#else
				sys_panic("");
#endif
				break;
			}
		case 21:{	/* ProfileSTEntry */
				SymbolDescProfile *s;
				s = (SymbolDescProfile *) method->symbols[k];
				switch (s->kind) {
#ifdef EVENT_LOG
				case 0:{
						patchConstant(code, s, &events);
						break;
					}
				case 1:{
						patchConstant(code, s, &n_events);
						break;
					}
				case 2:{
						patchConstant(code, s, MAX_EVENTS);
						break;
					}
				case 3:{
						jint event_id = cpuManager_createNewEventID(method->name);
						if (event_id < 0) {
							sys_panic("warn: out of event types MAX_EVENT_TYPES=%d", MAX_EVENT_TYPES);
							event_id = 0;
						}
						patchConstant(code, s, event_id);
						break;
					}
#endif
#ifdef PROFILE
				case 4:{	/* ProfileCallSTEntry */
						method->isprofiled = JNI_TRUE;
						patchRelativeAddress(code, method->symbols[k], (jint)
								     profile_call);
						break;
					}
				case 5:{	/* ProfilePtrOffsetSTEntry */
						patchConstant(code, method->symbols[k], (char *)
							      &(curthr()->profile) - (char *)
							      curthr());
						break;
					}
				case 6:{	/* TraceCallSTEntry */
						patchRelativeAddress(code, method->symbols[k], (jint)
								     profile_trace);
						break;
					}
#endif
				default:
					sys_panic("Linker unknown profile symbol %d", s->kind);
				}
				break;
			}
		case 22:{	/* MethodeDescSTEntry */
				patchConstant(code, method->symbols[k], (jint) method);
				break;
			}
		case 23:{	/* TCBOffsetSTEntry */
				SymbolDescTCBOffset *s;
				s = (SymbolDescTCBOffset *) method->symbols[k];
				switch (s->kind) {
				case 1:{
						patchConstant(code, s, (char *) &(curthr()->stack) - (char *)
							      curthr());
						break;
					}
				default:
					sys_panic("Linker unknown TCBOffset symbol %d", s->kind);
				}
				break;
			}
		default:
			sys_panic("Linker unknown symbol %d", method->symbols[k]->type);
		}
	}
}

void repatchMethodSymbols(MethodDesc * method, code_t code, jint allocObjectFunction, jint allocArrayFunction)
{
	int exCount, k;
	exCount = 0;
	for (k = 0; k < method->numberOfSymbols; k++) {
		if (method->symbols[k] == NULL)
			continue;
		switch (method->symbols[k]->type) {
		case 0:
			sys_panic("Error: Symboltype 0");
			break;
		case 1:{	/* DomainZeroSTEntry */
				/* patchConstant(code, method->symbols[k],  (jint)getDomainZeroProxy()); */
				break;
			}
		case 2:{	/* ExceptionHandlerSTEntry */
				patchRelativeAddress(code, method->symbols[k], (jint)
						     exceptionHandler);
				break;
			}
		case 3:{	/* DEPFunctionSTEntry */
				sys_panic("DEPFUNCTION no longer supported");
				break;
			}
		case 4:{	/* StaticFieldSTEntry */
				patchStaticFieldAddress(code, method->symbols[k]);
				break;
			}
		case 5:{	/* AllocObjectSTEntry */
				patchRelativeAddress(code, method->symbols[k], (jint) allocObjectFunction);	/* FIXME */
				break;
			}
		case 6:{	/* ClassSTEntry */
				patchClassPointer(code, method->symbols[k]);
				break;
			}
		case 7:{	/* DirectMethodCallSTEntry */
				patchDirectMethodAddress(code, method->symbols[k]);
				break;
			}
		case 8:{	/* StringSTEntry */
				patchStringAddress(code, method->symbols[k]);
				break;
			}
		case 9:{	/* AllocArraySTEntry */
				patchRelativeAddress(code, method->symbols[k], (jint)
						     allocArrayFunction);
				break;
			}
		case 10:{	/* AllocMultiArraySTEntry */
				patchRelativeAddress(code, method->symbols[k], (jint)
						     vmSpecialAllocMultiArray);
				break;
			}
		case 11:{	/* LongArithmeticSTEntry */
				patchRelativeLongAddress(code, method->symbols[k]);
				break;
			}
		case 12:{	/* VMSupportSTEntry */
				SymbolDescVMSupport *s = (SymbolDescVMSupport *) method->symbols[k];
				if (s->operation <= 0 || s->operation > numberVMOperations) {
					sys_panic("wrong vmsupport index");
				}
				patchRelativeAddress(code, method->symbols[k], (jint) VMSUPPORT(s->operation).fkt);
				break;
			}
		case 13:{	/* PrimitveClassSTEntry */
				patchPrimitiveClassPointer(code, method->symbols[k]);
				break;
			}
		case 14:{	/* UnresolvedJump */
				patchUnresolvedJump(code, method->symbols[k]);
				break;
			}
		case 15:{	/* VMAbsoluteSTEntry */
				break;
			}
		case 16:
		case 17:{	/* StackMap */
				stackMap(method, method->symbols[k]);
				break;
			}
		case 18:{	/* jx.compiler.symbols.ExceptionTableSTEntry  */
				/*
				 */
				break;
			}
		case 19:{	/* CurrentThreadPointerSTEntry */
				/*
				   patchConstant(code,
				   method->symbols[k],
				   (jint)curthrP());
				 */
				break;
			}
		case 20:{	/* StackChunkSizeSTEntry */
				/*
				   patchByte(code,
				   method->symbols[k],
				   (jint)STACK_CHUNK);
				 */
				break;
			}
		case 21:{	/* ProfileSTEntry */
				SymbolDescProfile *s;
				s = (SymbolDescProfile *) method->symbols[k];
				switch (s->kind) {
#ifdef EVENT_LOG
				case 0:{
						/*patchConstant(code,s,&events); */
						break;
					}
				case 1:{
						/*patchConstant(code,s,&n_events); */
						break;
					}
				case 2:{
						/*patchConstant(code,s,MAX_EVENTS); */
						break;
					}
				case 3:{
						/*
						   jint event_id = cpuManager_createNewEventID(method->name);
						   if (event_id<0) {
						   printf("warn: out of event types MAX_EVENT_TYPES=%d",MAX_EVENT_TYPES);
						   event_id=0;
						   }
						   patchConstant(code,s,event_id);
						 */
						break;
					}
#endif
#ifdef PROFILE
				case 4:{	/* ProfileCallSTEntry */
						method->isprofiled = JNI_TRUE;
						patchRelativeAddress(code, method->symbols[k], (jint)
								     profile_call);
						break;
					}
				case 5:{	/* ProfilePtrOffsetSTEntry */
						patchConstant(code, method->symbols[k], (char *)
							      &(curthr()->profile) - (char *)
							      curthr());
						break;
					}
				case 6:{	/* TraceCallSTEntry */
						patchRelativeAddress(code, method->symbols[k], (jint)
								     profile_trace);
						break;
					}
#endif
				default:
					sys_panic("Linker unknown profile symbol %d", s->kind);
				}
				break;
			}
		case 22:{	/* MethodeDescSTEntry */
				patchConstant(code, method->symbols[k], (jint) method);
				break;
			}
		default:
			sys_panic("Linker unknown symbol %d", method->symbols[k]->type);
		}
	}
}

MethodDesc *cloneMethodInDomain(DomainDesc * domain, MethodDesc * method)
{
	MethodDesc *new_method;

	new_method = malloc_methoddescs(domain, 1);
	if (new_method == NULL)
		return NULL;
	memcpy(new_method, method, sizeof(MethodDesc));

	new_method->code = malloc_nativecode(domain, method->numberOfCodeBytes);
	memcpy(new_method->code, method->code, method->numberOfCodeBytes);

	repatchMethodSymbols(new_method, new_method->code, (jint) specialAllocObject, (jint) vmSpecialAllocArray);

	return new_method;
}

void linksharedlib(DomainDesc * domain, SharedLibDesc * lib, jint allocObjectFunction, jint allocArrayFunction,
		   TempMemory * tmp_mem)
{
	jint i, j;
	MethodDesc *method;

	setCodeStart(lib);	/* must resolve code start addresses first because of direct method calls */
	for (i = 0; i < lib->numberOfClasses; i++) {
		ClassDesc *superclass;
		/* create vtable */
		superclass = lib->allClasses[i].superclass;
		createVTable(domain, &(lib->allClasses[i]));

	}

	for (i = 0; i < lib->numberOfClasses; i++) {

		/* find and link interfaces to this class */
		for (j = 0; j < lib->allClasses[i].numberOfInterfaces; j++) {
			ClassDesc *scl = NULL;
			char *ifname;
			ifname = lib->allClasses[i].ifname[j];
			scl = findClassDescInSharedLib(lib, ifname);
			if (scl == NULL)
				scl = findClassDesc(ifname);
			if (scl == NULL) {
				sys_panic("Cannot find interface %s while linking class %s of library %s\n", ifname,
					  lib->allClasses[i].name, lib->name);
			}
			lib->allClasses[i].interfaces[j] = scl;
		}

		/* patch addresses in method code */
		for (j = 0; j < lib->allClasses[i].numberOfMethods; j++) {
			patchMethodSymbols(&(lib->allClasses[i].methods[j]), lib->allClasses[i].methods[j].code,
					   allocObjectFunction, allocArrayFunction);
		}

		/* build vtable */
		if (lib->allClasses[i].vtableSize < 11)
			sys_panic("vtableSize<11");

		for (j = 0; j < lib->allClasses[i].vtableSize; j++) {
			if (lib->allClasses[i].vtableSym[j * 3][0] == '\0')
				continue;	/* hole */
			method =
			    findMethodInSharedLibs(lib->allClasses[i].vtableSym[j * 3], lib->allClasses[i].vtableSym[j * 3 + 1],
						   lib->allClasses[i].vtableSym[j * 3 + 2]);

			ASSERT(method != NULL);
#ifdef ASSERT_ALIGNED_CODE
#define ALIGN_CODE_TO_IBUFFERSIZE 1
#ifdef ALIGN_CODE_TO_IBUFFERSIZE
			/* align code to instruction buffer size (PIII = 16 byte) */
			if ((((int) method->code) % 16) != 0) {
				printf("%s not aligned to 16 byte addr: %p\n", method->name, method->code);
				sys_panic("");
			}
#else
			/* align code to 32 Bit == 4 Byte */
			if ((((int) method->code) % 4) != 0)
				printf("%s not aligned to 4 byte addr: %p\n", method->name, method->code);
#endif
#endif
			lib->allClasses[i].vtable[j] = method->code;
			lib->allClasses[i].methodVtable[j] = method;
		}
	}
}

void findClassAndMethodInLib(LibDesc * lib, char *classname, char *methodname, char *signature, Class ** classFound,
			     MethodDesc ** methodFound)
{
	jint i, j;
	ClassDesc *cl;

	for (i = 0; i < lib->numberOfClasses; i++) {
		cl = lib->allClasses[i].classDesc;
		//printf("findClassAndMethodInLib: %s %s =?= %s \n", lib->sharedLib->name, classname, cl->name);
		if (strcmp(classname, cl->name) == 0) {
			for (j = 0; j < cl->numberOfMethods; j++) {
				if (strcmp(methodname, cl->methods[j].name)
				    == 0) {
					if (strcmp(signature, cl->methods[j].signature) == 0) {
						*classFound = &(lib->allClasses[i]);
						*methodFound = &(cl->methods[j]);
						return;
					}
				}
			}
		}
	}
	*classFound = NULL;
	*methodFound = NULL;
}

void findClassAndMethod(DomainDesc * domain, char *classname, char *methodname, char *signature, Class ** classFound,
			MethodDesc ** methodFound)
{
	jint i;
	*classFound = NULL;
	*methodFound = NULL;

	for (i = 0; i < domain->numberOfLibs; i++) {
		if (testHashKey(classname, domain->libs[i]->key, LIB_HASHKEY_LEN)) {
			findClassAndMethodInLib(domain->libs[i], classname, methodname, signature, classFound, methodFound);
			if (*classFound != NULL)
				return;
		}
	}

	for (i = 0; i < domain->numberOfLibs; i++) {
		if (!testHashKey(classname, domain->libs[i]->key, LIB_HASHKEY_LEN)) {
			findClassAndMethodInLib(domain->libs[i], classname, methodname, signature, classFound, methodFound);
			if (*classFound != NULL)
				return;
		}
	}
}


void findClassDescAndMethodInLib(SharedLibDesc * lib, char *classname, char *methodname, char *signature, ClassDesc ** classFound,
				 MethodDesc ** methodFound)
{
	jint i, j;

	if (strcmp(classname, "java/lang/Object") == 0) {	/* Object is part of every lib */
		findClassDescAndMethodInObject(lib, classname, methodname, signature, classFound, methodFound);
		return;
	}

	for (i = 0; i < lib->numberOfClasses; i++) {
		if (strcmp(classname, lib->allClasses[i].name) == 0) {
			for (j = 0; j < lib->allClasses[i].numberOfMethods; j++) {
				if (strcmp(methodname, lib->allClasses[i].methods[j].name) == 0) {
					if (strcmp(signature, lib->allClasses[i].methods[j].signature) == 0) {
						*classFound = &(lib->allClasses[i]);
						*methodFound = &(lib->allClasses[i].methods[j]);
						return;
					}
				}
			}
		}
	}
	*classFound = NULL;
	*methodFound = NULL;
}

void findClassDescAndMethod(char *classname, char *methodname, char *signature, ClassDesc ** classFound,
			    MethodDesc ** methodFound)
{
	SharedLibDesc *sharedLib;
	*classFound = NULL;
	*methodFound = NULL;
	sharedLib = sharedLibs;

	while (sharedLib != NULL) {
		if (testHashKey(classname, sharedLib->key, LIB_HASHKEY_LEN)) {
			findClassDescAndMethodInLib(sharedLib, classname, methodname, signature, classFound, methodFound);
			if (*classFound != NULL)
				return;
		}
		sharedLib = sharedLib->next;
	}

//printf(" %s::%s%s not found with hashkey\n",classname,methodname,signature);

	sharedLib = sharedLibs;
	while (sharedLib != NULL) {
		if (!testHashKey(classname, sharedLib->key, LIB_HASHKEY_LEN)) {
			findClassDescAndMethodInLib(sharedLib, classname, methodname, signature, classFound, methodFound);
			if (*classFound != NULL)
				return;
		}
		sharedLib = sharedLib->next;
	}

}

MethodDesc *findMethodInSharedLibs(char *classname, char *methodname, char *signature)
{
	ClassDesc *classFound;
	MethodDesc *methodFound;

	findClassDescAndMethod(classname, methodname, signature, &classFound, &methodFound);
	return methodFound;
}


MethodDesc *findMethodInLib(LibDesc * lib, char *classname, char *methodname, char *signature)
{
	Class *classFound;
	MethodDesc *methodFound;
	//printf("findMethodInLib: %s %s %s %s\n", lib->sharedLib->name, classname, methodname, signature);
	findClassAndMethodInLib(lib, classname, methodname, signature, &classFound, &methodFound);
	return methodFound;
}

MethodDesc *findMethod(DomainDesc * domain, char *classname, char *methodname, char *signature)
{
	jint i;
	MethodDesc *m;

	//printf("findMethod: %s %s %s\n", classname, methodname, signature);
	for (i = 0; i < domain->numberOfLibs; i++) {
		if (testHashKey(classname, domain->libs[i]->key, LIB_HASHKEY_LEN)) {
			m = findMethodInLib(domain->libs[i], classname, methodname, signature);
			if (m != NULL) {
				ASSERT(m->code != NULL);
				return m;
			}
		}
	}

#if DEBUG
	for (i = 0; i < domain->numberOfLibs; i++) {
		if (!testHashKey(classname, domain->libs[i]->key, LIB_HASHKEY_LEN)) {
			m = findMethodInLib(domain->libs[i], classname, methodname, signature);
			if (m != NULL) {
				sys_panic("warn: Bug in findMethod of testHashKey!\n");
				return m;
			}
		}
	}
#else
	/* java/lang/Object */
	if (domain->numberOfLibs > 0)
		m = findMethodInLib(domain->libs[0], classname, methodname, signature);
#endif

	return NULL;
}

code_t findVirtualMethodCode(DomainDesc * domain, char *classname, char *methodname, char *signature, ObjectDesc * obj)
{
	jint j;
	Class *c = findClass(domain, classname);
	ClassDesc *cl = c->classDesc;
	if (cl == NULL) {
		sys_panic("Cannot find class %s\n", classname);
	}
	for (j = 0; j < cl->vtableSize; j++) {
		if (cl->vtableSym[j * 3][0] == '\0')
			continue;	/* hole */
//printf("%s\n",cl->vtableSym[j*3+1]);
		if ((strcmp(cl->vtableSym[j * 3 + 1], methodname) == 0)
		    && (strcmp(cl->vtableSym[j * 3 + 2], signature) == 0)) {
			return obj->vtable[j];
		}
	}
	sys_panic("Cannot find method %s::%s%s\n", classname, methodname, signature);
	return NULL;
}


jint findDEPMethodIndex(DomainDesc * domain, char *className, char *methodName, char *signature)
{
	jint j;
	ClassDesc *cl;
	Class *c = findClass(domain, className);
	ASSERTCLASS(c);
	if (c == NULL) {
		sys_panic("Cannot find DEP %s\n", className);
	}
	cl = c->classDesc;
	ASSERTCLASSDESC(cl);
	for (j = 0; j < cl->vtableSize; j++) {
		if (cl->vtableSym[j * 3][0] == '\0')
			continue;	/* hole */
		if ((strcmp(cl->vtableSym[j * 3 + 1], methodName) == 0)
		    && (strcmp(cl->vtableSym[j * 3 + 2], signature) == 0)) {
			return j;
		}
	}
	sys_panic("Cannot find DEP method %s:: %s%s\n", className, methodName, signature);
	return 0;
}

/* this conforms not exactly to the JVM spec */
void callClassConstructors(DomainDesc * domain, LibDesc * lib)
{
	jint i;

	ASSERTLIB(lib);

	if (domain != curdom())
		sys_panic("WRONG DOMAIN called in class constructor");

	if (lib->initialized == 1)
		return;		/* already done */

	for (i = 0; i < lib->numberOfClasses; i++)
		callClassConstructor(&lib->allClasses[i]);

	lib->initialized = 1;
}

void callClassConstructor(Class * cl)
{
	jint i;
	code_t c;
	ASSERTCLASS(cl);
	if (cl->state == CLASS_READY)
		return;
	cl->state = CLASS_READY;
	for (i = 0; i < cl->classDesc->numberOfMethods; i++) {
		if (strcmp("<clinit>", cl->classDesc->methods[i].name) == 0) {
			c = (code_t) cl->classDesc->methods[i].code;
			c();
			break;
		}
	}
}

u4_t executeStatic(DomainDesc * domain, char *className, char *methodname, char *signature, jint * params, jint params_size)
{
	MethodDesc *method;
	code_t c;

	method = findMethod(domain, className, methodname, signature);
	if (method == 0)
		sys_panic("StaticMethod not found: %s.%s%s", className, methodname, signature);
	//printf("callnative static %s.%s%s %p\n",className, methodname, signature, method->code);

	c = (code_t) method->code;

	ASSERT(c != NULL);
	return callnative_static(params, c, params_size);
}

void executeSpecial(DomainDesc * domain, char *className, char *methodname, char *signature, ObjectDesc * obj, jint * params,
		    int params_size)
{
	MethodDesc *method;
	code_t c;
	jint ret;

	method = findMethod(domain, className, methodname, signature);
	if (method == 0)
		sys_panic("SpecialMethod not found: %s %s %s", className, methodname, signature);

	c = (code_t) method->code;
	ASSERT(c != 0);
	ret = callnative_special(params, obj, c, params_size);
}

void executeInterface(DomainDesc * domain, char *className, char *methodname, char *signature, ObjectDesc * obj, jint * params,
		      int params_size)
{
	code_t c;
	jint ret;
	ASSERT(obj != 0);
	ASSERTOBJECT(obj);
	c = findVirtualMethodCode(domain, className, methodname, signature, obj);
	ASSERT(c != 0);
	ret = callnative_special(params, obj, c, params_size);
}

u4_t executeVirtual(DomainDesc * domain, char *className, char *methodname, char *signature, ObjectDesc * obj, jint * params,
		    int params_size)
{
	code_t c;
	ASSERT(obj != 0);
	ASSERTOBJECT(obj);
	c = findVirtualMethodCode(domain, className, methodname, signature, obj);
	ASSERT(c != 0);
	return callnative_special(params, obj, c, params_size);
}

void set_message(ObjectDesc * obj, jint value)
{
	//printf("SETMESSAGE: %lx %ld \n", (jint)obj, (jint)value);
}

jint get_message(ObjectDesc * obj)
{
	//printf("GETMESSAGE: %lx \n", (jint)obj);
	return 42;
}

char backup;

void set_breakpoint(DomainDesc * domain, char *classname, char *methodname, char *signature, int offset)
{
	MethodDesc *m = findMethod(domain, classname, methodname, signature);
	backup = ((u1_t *) (m->code))[offset];
	((u1_t *) (m->code))[offset] = 0xcc;
}


void test_irq_no()
{
	//printf("IGNORE IRQ\n");
}

void test_irq_yes()
{
	//printf("HANDLE IRQ\n");
}

void test_irq_missed(DEPDesc * dep)
{
	ASSERTDEP(dep);
	//printf("MISSED IRQ at dep %p, no receiver %p\n", dep, dep->firstWaitingReceiver);

}
