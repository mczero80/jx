#ifndef OBJECT_H
#define OBJECT_H

typedef struct {
	code_t *vtable;
	jint data[1];		/* at least one word of data */
} ObjectDesc;

typedef ObjectDesc **ObjectHandle;


#ifdef USE_QMAGIC
#define MAGIC_OBJECT 0xbebeceee
#define MAGIC_INVALID 0xabcdef98
#endif
#if defined (USE_QMAGIC) && defined (NORMAL_MAGIC)
//#define ASSERTOBJECT(x) ASSERT(((char*)(x) == NULL) || (getObjMagic(x)==MAGIC_OBJECT))
#define ASSERTOBJECT(x) if (! (((char*)(x) == NULL) || (getObjMagic(x)==MAGIC_OBJECT))) {printf("\"%s\", line %d: Assertion failed \n NOT AN OBJECT: %p\n ", __FILE__, __LINE__, x); dump_data(x); asm("int $3");}
#define ASSERTPROXY(x) {ASSERTOBJECT(x); { ASSERT((getObjFlags(x) & FLAGS_MASK) == OBJFLAGS_PORTAL); }}
#define ASSERTHANDLE(_h_) \
 if (((char*)(_h_)==NULL) || (getObjMagic(*(_h_))!=MAGIC_OBJECT)) {\
  printf(" _h_ is not a valid handle! %s(%d)\n",__FILE__,__LINE__);\
  asm("int $3");\
}
#else
#define ASSERTOBJECT(x)
#define ASSERTHANDLE(x)
#define ASSERTPROXY(x)
#endif

#ifdef DEBUG
#define ASSERT_ARRAYTYPE(_array_,_type_) { if (((ArrayClassDesc *)((ArrayDesc*)(_array_)->arrayClass))->name[1]!=_type_) sys_panic("Assertion failed"); }
#else
#define ASSERT_ARRAYTYPE(_array_,_type_)
#endif

#include "code.h"

typedef struct {
	code_t *vtable;
	ClassDesc *arrayClass;
	jint size;
	jint data[1];		/* at least one word of data */
} ArrayDesc;

/* NOT derived from ObjectDesc */
typedef struct {
	//    code_t **vtable;
#ifdef USE_QMAGIC
	u4_t magic;
#endif
	int cpu_id;
} CPUDesc;

#ifdef USE_QMAGIC
#define MAGIC_CPU 0xcb0cb0ff
#endif
#if defined (NORMAL_MAGIC) && defined(USE_QMAGIC)
#define ASSERTCPU(x) ASSERT(x->magic==MAGIC_CPU)
#else
#define ASSERTCPU(x)
#endif


#endif				/* OBJECT_H */
