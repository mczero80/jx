#ifndef __EXECJAVA_H
#define __EXECJAVA_H

#include "thread.h"
#include "smp.h"

typedef jint(*java_method0_t) (ObjectDesc *);
typedef jint(*java_method1_t) (ObjectDesc *, long arg1);
typedef jint(*java_method2_t) (ObjectDesc *, long arg1, long arg2);

int call_JAVA_method0(ObjectDesc * Object, ThreadDesc * worker,
		      java_method0_t function);
int call_JAVA_method1(ObjectDesc * Object, ThreadDesc * worker,
		      java_method1_t function, long param);
int call_JAVA_method2(ObjectDesc * Object, ThreadDesc * worker,
		      java_method2_t function, long param1, long param2);

/* activates a Java mthod but does not save the current context
   eflags specifies the EFLAGS for the worker-Thread
   see CALL_WITH_... defines below */
void destroy_call_JAVA_function(ObjectDesc * Object, ThreadDesc * worker,
				java_method0_t function, long eflags);
void destroy_call_JAVA_method1(ObjectDesc * Object, ThreadDesc * worker,
			       java_method1_t function, long param,
			       long eflags);
void destroy_call_JAVA_method2(ObjectDesc * Object, ThreadDesc * worker,
			       java_method2_t function, long param1,
			       long param2, long eflags);
#define CALL_WITH_ENABLED_IRQS 0x00000212
#define CALL_WITH_DISABLED_IRQS 0x00000012

#endif /*__EXECJAVAP_H*/
