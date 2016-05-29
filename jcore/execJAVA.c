#include "config.h"
//#ifdef JAVASCHEDULER

#include "all.h"
#include "execJAVA.h"

/* see schedSWITCH.S */
void return_from_java0(ThreadDesc * next, ContextDesc * restore);
void return_from_java1(long param, ContextDesc * restore, ThreadDesc * next);
void return_from_java2(long param1, long param2, ThreadDesc * next, ContextDesc * restore);
#ifdef KERNEL
jint call_java(ContextDesc * backup, ThreadDesc * to);
#else
jint call_java(ThreadDesc * backup, ThreadDesc * to);
#endif
void destroy_call_java(ThreadDesc * to);

/*executes a JAVA method in the worker thread */
/* the JAVA Method must not need parameter but can return a value*/
/* ! IRQ must be disabled before calling this function*/
int call_JAVA_method0(ObjectDesc * Object, ThreadDesc * worker, java_method0_t function)
{
	/*int (*function)(ObjectDesc*)) { */
	u4_t *sp;
	int result;
#ifdef KERNEL
	ContextDesc backup_ctx;
#else
	ThreadDesc backup_ctx;
	backup_ctx.contextPtr = &(backup_ctx.context);
#endif

	if (function == NULL)
		sys_panic("null function not supported yet");

	if (worker == NULL)	/* DomainZero? */
		return function(Object);
	ASSERTTHREAD(worker);

	/* switch worker->state from AVAILABLE to RUNNING */
	if (!cas((u4_t *) (&worker->state), (u4_t) STATE_AVAILABLE, (u4_t) STATE_RUNNABLE))
		sys_panic("the worker thread %d.%d (%s) is not AVAILABLE\n", TID(worker), worker->name);

	/* prepare worker thread */
	sp = worker->stackTop;
	stack_push(&sp, (int) &backup_ctx);
	stack_push(&sp, (int) curthr());
	stack_push(&sp, (int) Object);
	stack_push(&sp, (int) return_from_java0);
	worker->context[PCB_ESP] = (long) sp;
	worker->context[PCB_EIP] = (long) function;

	/* activate worker thread */
	result = call_java(&backup_ctx, worker);
	worker->state = STATE_AVAILABLE;
	return result;
}


/*executes a JAVA method in the worker thread */
/* the JAVA Method can have one parameter and can return a value*/
/* ! IRQ must be disabled before calling this function*/
int call_JAVA_method1(ObjectDesc * Object, ThreadDesc * worker, java_method1_t function,
		      /*int (*function)(ObjectDesc*,long), */
		      long param)
{
	jint *sp;
	int result;
#ifdef KERNEL
	ContextDesc backup_ctx;
#else
	ThreadDesc backup_ctx;
	backup_ctx.contextPtr = &(backup_ctx.context);
#endif

	if (function == NULL)	/* method not implemented */
		//       sys_panic("null function not supportet yet");
		return JNI_FALSE;


	if (worker == NULL)	/* DomainZero? */
		return function(Object, param);
	ASSERTTHREAD(worker);

	/* switch worker->state from AVAILABLE to RUNNING */
	if (!cas((u4_t *) (&worker->state), (u4_t) STATE_AVAILABLE, (u4_t) STATE_RUNNABLE))
		sys_panic("the worker thread %d.%d (%s) is not AVAILABLE\n", TID(worker), worker->name);

	/* prepare worker thread */
	sp = worker->stackTop;
//     stack_push(&sp,  curthr());
//     stack_push(&sp,  &backup_ctx);
	stack_push(&sp, (int) &backup_ctx);
	stack_push(&sp, (int) curthr());
	stack_push(&sp, param);
	stack_push(&sp, (int) Object);
	stack_push(&sp, (int) return_from_java1);
	worker->context[PCB_ESP] = (long) sp;
	worker->context[PCB_EIP] = (long) function;

	/* activate worker thread */
	result = call_java(&backup_ctx, worker);
	worker->state = STATE_AVAILABLE;
	return result;
}

/*executes a JAVA method in the worker thread */
/* the JAVA Method can have two parameters and may return a value*/
/* ! IRQ must be disabled before calling this function*/
int call_JAVA_method2(ObjectDesc * Object, ThreadDesc * worker, java_method2_t function,
		      /*int (*function)(ObjectDesc*,long,long), */
		      long param1, long param2)
{
	jint *sp;
	int result;
#ifdef KERNEL
	ContextDesc backup_ctx;
#else
	ThreadDesc backup_ctx;
	backup_ctx.contextPtr = &(backup_ctx.context);
#endif

	if (function == NULL)
		sys_panic("null function not supported yet");

	if (worker == NULL)	/* DomainZero? */
		return function(Object, param1, param2);
	ASSERTTHREAD(worker);

	/* switch worker->state from AVAILABLE to RUNNING */
	if (!cas((u4_t *) (&worker->state), (u4_t) STATE_AVAILABLE, (u4_t) STATE_RUNNABLE))
		sys_panic("the worker thread %d.%d (%s) is not AVAILABLE\n", TID(worker), worker->name);

	/* prepare worker thread */
	sp = worker->stackTop;
	stack_push(&sp, (int) &backup_ctx);
	stack_push(&sp, (int) curthr());
	stack_push(&sp, param2);
	stack_push(&sp, param1);
	stack_push(&sp, (int) Object);
	stack_push(&sp, (int) return_from_java2);
	worker->context[PCB_ESP] = (long) sp;
	worker->context[PCB_EIP] = (long) function;

	/* activate worker thread */
	result = call_java(&backup_ctx, worker);
	worker->state = STATE_AVAILABLE;
	return result;
}

/****************************************************************/
void never_return(void)
{
	sys_panic("should not return");
	asm(".global never_return_end;" " never_return_end:");
	;
}

/* ! IRQ must be disabled before calling this function*/
void destroy_call_JAVA_function(ObjectDesc * Object, ThreadDesc * worker, java_method0_t function, long eflags)
{
	jint *sp;
	/*printf("destroy_call_JAVA_function called\n"); */
	ASSERTTHREAD(worker);
	ASSERT(function != NULL)

	    /* switch worker->state from AVAILABLE to RUNNING */
	    if (!cas((u4_t *) (&worker->state), (u4_t) STATE_AVAILABLE, (u4_t) STATE_RUNNABLE))
		sys_panic("the worker thread %d.%d (%s) is not AVAILABLE\n", TID(worker), worker->name);

	/* prepare worker thread */
	sp = worker->stackTop;
	stack_push(&sp, (int) Object);
	stack_push(&sp, (int) never_return);
	worker->context[PCB_ESP] = (long) sp;
	worker->context[PCB_EIP] = (long) function;
#ifdef KERNEL
	worker->context[PCB_EFLAGS] = eflags;
#else
	if (eflags == CALL_WITH_ENABLED_IRQS)
		sigdelset(&(worker->sigmask), SIGALRM);
	else
		sigaddset(&(worker->sigmask), SIGALRM);
#endif
	/* activate worker thread */
	destroy_call_java(worker);
}

void destroy_call_JAVA_method1(ObjectDesc * Object, ThreadDesc * worker, java_method1_t function, long param, long eflags)
{
	jint *sp;
	/*printf("destroy_call_JAVA_method1 called\n"); */
	ASSERTTHREAD(worker);
	ASSERT(function != NULL)

	    /* switch worker->state from AVAILABLE to RUNNING */
	    if (!cas((u4_t *) (&worker->state), (u4_t) STATE_AVAILABLE, (u4_t) STATE_RUNNABLE))
		sys_panic("the worker thread (%d.d) is not AVAILABLE\n", TID(worker));

	/* prepare worker thread */
	sp = worker->stackTop;
	stack_push(&sp, param);
	stack_push(&sp, (int) Object);
	stack_push(&sp, (int) never_return);
	worker->context[PCB_ESP] = (long) sp;
	worker->context[PCB_EIP] = (long) function;
#ifdef KERNEL
	worker->context[PCB_EFLAGS] = eflags;
#else
	if (eflags == CALL_WITH_ENABLED_IRQS)
		sigdelset(&(worker->sigmask), SIGALRM);
	else
		sigaddset(&(worker->sigmask), SIGALRM);
#endif

	/* activate worker thread */
	destroy_call_java(worker);
}

void destroy_call_JAVA_method2(ObjectDesc * Object, ThreadDesc * worker, java_method2_t function, long param1, long param2,
			       long eflags)
{
	jint *sp;
	/*printf("destroy_call_JAVA_method2 called\n"); */
	ASSERTTHREAD(worker);
	ASSERT(function != NULL)

	    /* switch worker->state from AVAILABLE to RUNNING */
	    if (!cas((u4_t *) (&worker->state), (u4_t) STATE_AVAILABLE, (u4_t) STATE_RUNNABLE))
		sys_panic("the worker thread %d.%d (%s) is not AVAILABLE\n", TID(worker), worker->name);

	/* prepare worker thread */
	sp = worker->stackTop;
	stack_push(&sp, param2);
	stack_push(&sp, param1);
	stack_push(&sp, (int) Object);
	stack_push(&sp, (int) never_return);
	worker->context[PCB_ESP] = (long) sp;
	worker->context[PCB_EIP] = (long) function;
#ifdef KERNEL
	worker->context[PCB_EFLAGS] = eflags;
#else
	if (eflags == CALL_WITH_ENABLED_IRQS)
		sigdelset(&(worker->sigmask), SIGALRM);
	else
		sigaddset(&(worker->sigmask), SIGALRM);
#endif

	/* activate worker thread */
	destroy_call_java(worker);
}

//#endif /* JAVASCHEDULER */
