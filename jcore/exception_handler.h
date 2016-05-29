#ifndef _EXCEPTION_HANDLER_H_
#define _EXCEPTION_HANDLER_H_

/* #define THROW_VirtualMachineError */
#define THROW_OutOfMemoryError       ((jint*)-3)
#define THROW_StackOverflowError     ((jint*)-5)

#define THROW_RuntimeException       ((jint*)-1)
#define THROW_NullPointerException   ((jint*)-2)
#define THROW_MemoryIndexOutOfBounds ((jint*)-4)
#define THROW_ArithmeticException    ((jint*)-6)

#define THROW_MagicNumber            ((jint*)-7)
#define THROW_ParanoidCheck          ((jint*)-8)
#define THROW_StackJam               ((jint*)-9)

#define THROW_ArrayIndexOutOfBounds  ((jint*)-10)
#define THROW_UnsupportedByteCode    ((jint*)-11)
#define THROW_InvalidMemory          ((jint*)-12)

#define THROW_MemoryExhaustedException ((jint*)-13)
#define THROW_DomainTerminatedException ((jint*)-14)

#ifndef ASSEMBLER
#include "types.h"
#include "domain.h"

#ifdef KERNEL
#define CHECK_NULL_PTR(_ptr_) {if (_ptr_==NULL) exceptionHandler(THROW_NullPointerException);}
#define CHECK_NULL_POINTER(_ptr_)
#else
#define CHECK_NULL_PTR(_ptr_) {if (_ptr_==NULL) exceptionHandler(THROW_NullPointerException);}
#define CHECK_NULL_POINTER(_exp_) {if (_exp_) exceptionHandler(THROW_NullPointerException);}
#endif

ObjectDesc *createExceptionInDomain(DomainDesc * domain,
				    const char *exception,
				    const char *details);

void throw_exception(ObjectDesc * exception, u4_t * sp);
void throw_ArithmeticException(jint dummy);
void throw_StackOverflowError();
void throw_ArrayIndexOutOfBounds(jint dummy);
void throw_NullPointerException(jint dummy);

void exceptionHandlerMsg(jint * p, char *msg);
void exceptionHandler(jint * p);
#endif

#endif
