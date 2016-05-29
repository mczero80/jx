#ifndef LOAD_H
#define LOAD_H

#define CURRENT_COMPILER_VERSION 9

#ifndef ASSEMBLER

#include "domain.h"
#include "object.h"
#include "code.h"

#ifdef PROFILE_SAMPLE_HEAPUSAGE
struct heapsample_s {
	char *eip[10];
	ClassDesc *cl;
	jint size;
};
#endif

void initPrimitiveClasses();
void stringToChar(ObjectDesc * str, char *buf, jint buflen);
LibDesc *load(DomainDesc * domain, char *filename);
void linkdomain(DomainDesc * domain, LibDesc * lib,
		jint allocObjectFunction, jint allocArrayFunction);
void callClassConstructorsInLib(LibDesc * lib);
u4_t executeStatic(DomainDesc * domain, char *className, char *methodname,
		   char *signature, jint * params, jint params_size);

jint getArraySize(ArrayDesc * array);
ObjectDesc *getReferenceArrayElement(ArrayDesc * array, jint pos);


char *methodName2str(ClassDesc * class, MethodDesc * method, char *buffer,
		     int size);
Class *findClass(DomainDesc * domain, char *name);
Class *findClassInLib(LibDesc * lib, char *name);
Class *findClassOrPrimitive(DomainDesc * domain, char *name);
int findClassForMethod(MethodDesc * method, Class ** class);
code_t findAddrOfMethodBytecode(char *className, char *method,
				char *signature, jint bytecodePos);
int findMethodAtAddr(u1_t * addr, MethodDesc ** method,
		     ClassDesc ** classInfo, jint * bytecodePos,
		     jint * lineNumber);

MethodDesc *cloneMethodInDomain(DomainDesc * domain, MethodDesc * method);

ArrayClassDesc *findSharedArrayClassDescByElemClass(ClassDesc * elemClass);
ObjectDesc *newString(DomainDesc * domain, char *value);
ObjectDesc *newStringArray(DomainDesc * domain, int size, char *arr[]);
ClassDesc *findClassDescInSharedLib(SharedLibDesc * lib, char *name);
ClassDesc *obj2ClassDesc(ObjectDesc * obj);
Class *classDesc2Class(DomainDesc * domain, ClassDesc * classDesc);
ObjectDesc *allocObject(ClassDesc * c);
void callClassConstructors(DomainDesc * domain, LibDesc * lib);
void linksharedlib(DomainDesc * domain, SharedLibDesc * lib,
		   jint allocObjectFunction, jint allocArrayFunction,
		   TempMemory * tmp_mem);
void executeInterface(DomainDesc * domain, char *className,
		      char *methodname, char *signature, ObjectDesc * obj,
		      jint * params, int params_size);
void executeSpecial(DomainDesc * domain, char *className, char *methodname,
		    char *signature, ObjectDesc * obj, jint * params,
		    int params_size);
void createVTable(DomainDesc * domain, ClassDesc * c);

Class *specialAllocClass(DomainDesc * domain, int number);

ClassDesc *findClassDesc(char *name);

#ifndef KERNEL
void install_handler(int sig, void (*handler) (int));
#endif				/* KERNEL */

struct DEP;

extern SharedLibDesc *sharedLibs;

extern DEPDesc **allDEPInstances;
extern int numDEPInstances;

extern DomainDesc *domainZero;

extern ClassDesc *java_lang_Object;
extern Class *java_lang_Object_class;
//extern ClassDesc *class_Array;
//extern Class *class_Array_class;
extern code_t *array_vtable;

#if defined(DEBUG) && defined(USE_FMAGIC) && defined(NORMAL_MAGIC)
#define obj2ClassDesc(obj) (((getObjMagic(obj)==MAGIC_OBJECT) && (((ObjectDesc*)(obj))->vtable != NULL))? *(ClassDesc**)(((ObjectDesc*)(obj))->vtable-1) : (printf("\"%s\", line %d: Assertion failed.\n", __FILE__, __LINE__), sys_panic(""), (ClassDesc*)NULL))
#else
#define obj2ClassDesc(obj) ( *(ClassDesc**)(((ObjectDesc*)(obj))->vtable-1) )
#endif

#define obj2ClassDescFAST(obj) ( *(ClassDesc**)(((ObjectDesc*)(obj))->vtable-1) )

#endif				/* ASSEMBLER */

#endif				/* LOAD_H */
