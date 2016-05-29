/********************************************************************************
 * Code-management related data structures
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifndef CODE_H
#define CODE_H

#ifdef ASSEMBLER


#else				/* ASSEMBLER */

#define DEPFLAG_NONE   0
#define DEPFLAG_REDO   1

#include "types.h"
#include "config.h"
#include "lock.h"

#define SYMBOLDESC_BASE jint type;jint immediateNCIndex;jint numBytes;jint nextInstrNCIndex


typedef struct {
	SYMBOLDESC_BASE;
} SymbolDesc;

typedef struct {
	SYMBOLDESC_BASE;
	jint immediateNCIndexPre;
	int n_bytes;
	int n_bits;
	jbyte *map;
} SymbolDescStackMap;

/*
 *
 */

typedef struct FieldDesc_s {
	char *fieldName;
	char *fieldType;
	char fieldOffset;
} FieldDesc;

typedef struct {
	jint bytecodePos;
	jint start;
	jint end;
} ByteCodeDesc;

typedef struct {
	jint startBytecode;
	jint lineNumber;
} SourceLineDesc;

typedef struct {
	jint start;
	jint end;
	struct ClassDesc_s *type;
	u4_t addr;
} ExceptionDesc;

typedef struct MethodDesc_s {
#ifdef GC_SWAP_MAGIC_WITH_FLAGS
# ifdef USE_QMAGIC
	u4_t objectDesc_magic;
# endif
	u4_t objectDesc_flags;
#else
	u4_t objectDesc_flags;
# ifdef USE_QMAGIC
	u4_t objectDesc_magic;
# endif
#endif
	code_t *objectDesc_vtable;
#ifdef USE_QMAGIC
	u4_t magic;
#endif
	char *name;
	char *signature;
	jint numberOfCodeBytes;
	jint numberOfSymbols;
	SymbolDesc **symbols;
	jint numberOfByteCodes;
	ByteCodeDesc *bytecodeTable;
	jint codeOffset;
	code_t code;
	jint numberOfArgs;
	jint numberOfArgTypeMapBytes;
	jbyte *argTypeMap;
	jint returnType;	/* 0 or 1 */

	jint sizeLocalVars;

#ifdef PROFILE
	jint isprofiled;
#endif

	struct ClassDesc_s *classDesc;

	jint sizeOfExceptionTable;
	ExceptionDesc *exceptionTable;

	jint numberOfSourceLines;
	SourceLineDesc *sourceLineTable;

	u4_t flags;
} MethodDesc;

#ifdef USE_QMAGIC
#define MAGIC_METHODDESC 0x42414039
#endif
#if defined(USE_QMAGIC) && defined(NORMAL_MAGIC)
#define ASSERTMETHODDESC(x) ASSERT(x->magic==MAGIC_METHODDESC)
#else
#define ASSERTMETHODDESC(x)
#endif

#define METHODFLAGS_STATIC 0x00000001

#define IS_STATIC(m) ((m)->flags & METHODFLAGS_STATIC != 0)

#define CLASSTYPE_CLASS          0x01
#define CLASSTYPE_INTERFACE      0x03
#define CLASSTYPE_ARRAYCLASS     0x04
#define CLASSTYPE_PRIMITIVE      0x08


struct SharedLibDesc_s;
struct ArrayClassDesc_s;

#ifdef DEBUG
#define CLASSDEBUGINFO u4_t numberOfImplementors; struct ClassDesc_s* implementedBy;
#else
#define CLASSDEBUGINFO
#endif

#ifdef COPY_STATISTICS
#define CLASSSTATISTICS u4_t copied; u4_t copied_arrayelements;
#else
#define CLASSSTATISTICS
#endif

#define CLASSDESC0\
    jint classType;\
    char *name;\
    struct ClassDesc_s *superclass;\
    jint numberOfInterfaces;\
    char **ifname;\
    struct ClassDesc_s **interfaces;\
    jint numberOfMethods;\
    jint vtableSize;\
    MethodDesc *methods;\
    code_t *vtable;\
    char **vtableSym;\
    jint instanceSize;\
    jint staticFieldsSize;\
    struct SharedLibDesc_s *definingLib;\
    struct ClassDesc_s *next;\
    jint mapBytes;\
    jbyte *map;\
    jint staticsMapBytes;\
    jbyte *staticsMap;\
    code_t *proxyVtable;\
    MethodDesc **methodVtable;\
    struct ArrayClassDesc_s *arrayClass;\
    jint numberFields;\
    struct FieldDesc_s *fields; \
    u4_t inheritServiceThread; \
    CLASSSTATISTICS;\
    CLASSDEBUGINFO



#ifdef PROFILE_HEAPUSAGE
#define CLASSDESC1 CLASSDESC0; jint n_instances; jint n_arrayelements;
#else
#define CLASSDESC1 CLASSDESC0
#endif

#ifdef USE_QMAGIC
#define CLASSDESC u4_t magic; CLASSDESC1 ; jint sfield_offset;
#else
#define CLASSDESC CLASSDESC1 ; jint sfield_offset;
#endif

typedef struct ClassDesc_s {
	CLASSDESC;
} ClassDesc;

#ifdef USE_QMAGIC
#define MAGIC_CLASSDESC 0x47114711
#endif
#if defined(USE_QMAGIC) && defined(NORMAL_MAGIC)
#define ASSERTCLASSDESC(x) ASSERT(x->magic==MAGIC_CLASSDESC)
#else
#define ASSERTCLASSDESC(x)
#endif

#define CLASS_NOT_INIT 0
#define CLASS_READY 1

/* anchor for shared / private part of classes */
typedef struct Class_s {
#ifdef GC_SWAP_MAGIC_WITH_FLAGS
# ifdef USE_QMAGIC
	u4_t objectDesc_magic;
# endif
	u4_t objectDesc_flags;
#else
	u4_t objectDesc_flags;
# ifdef USE_QMAGIC
	u4_t objectDesc_magic;
# endif
#endif
	code_t *objectDesc_vtable;
#ifdef USE_QMAGIC
	u4_t magic;
#endif
	ClassDesc *classDesc;	/* shared */
	struct Class_s *superclass;	/* private */
	jint *staticFields;	/* private */
	jint state;
#ifdef HEAP_STATISTICS
	jint numberOfInstances;
#endif				/* HEAP_STATISTICS */
} Class;

#ifdef USE_QMAGIC
#define MAGIC_CLASS 0x007babab
#endif
#if defined(NORMAL_MAGIC) && defined(USE_QMAGIC)
#define ASSERTCLASS(x) ASSERT(x->magic==MAGIC_CLASS)
#else
#define ASSERTCLASS(x)
#endif

typedef struct ArrayClassDesc_s {
	CLASSDESC;
	ClassDesc *elementClass;
	ClassDesc *nextInDomain;
} ArrayClassDesc;

typedef struct PrimitiveClassDesc_s {
	CLASSDESC;
} PrimitiveClassDesc;

#define LIB_HASHKEY_LEN 10

/*
 * Libs
 */
struct meta_s {
	char *var;
	char *val;
};

typedef struct SharedLibDesc_s {
#ifdef USE_QMAGIC
	u4_t magic;
#endif
	char *name;
#ifdef USE_LIB_INDEX
	jint ndx;
	jint memSizeStaticFields;
#endif
	u4_t id;
	jint numberOfClasses;
	ClassDesc *allClasses;
	jint numberOfNeededLibs;
	struct SharedLibDesc_s **neededLibs;
	char *code;
	u4_t codeBytes;
	char key[LIB_HASHKEY_LEN];
	u4_t vtablesize, bytecodes;	/* for statistical purposes */
	u4_t numberOfMeta;
	struct meta_s *meta;
	struct SharedLibDesc_s *next;
} SharedLibDesc;

#ifdef USE_QMAGIC
#define MAGIC_SLIB 0x42240911
#endif
#if  defined(NORMAL_MAGIC) &&defined(USE_QMAGIC)
#define ASSERTSLIB(x) ASSERT(x->magic==MAGIC_SLIB)
#else
#define ASSERTSLIB(x)
#endif

typedef struct {
#ifdef USE_QMAGIC
	u4_t magic;
#endif
	jint numberOfClasses;
#ifdef FASTER_METHOD_LOOKUP
	char hasNoImplementations;
#endif
	Class *allClasses;
	char key[LIB_HASHKEY_LEN];
	SharedLibDesc *sharedLib;
	int initialized;
} LibDesc;

#ifdef USE_QMAGIC
#define MAGIC_LIB 0x12345678
#endif
#if defined(NORMAL_MAGIC) && defined(USE_QMAGIC)
#define ASSERTLIB(x) ASSERT(x->magic==MAGIC_LIB)
#else
#define ASSERTLIB(x)
#endif

#endif				/* ASSEMBLER */

#endif				/* CODE_H */
