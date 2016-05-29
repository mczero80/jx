/********************************************************************************
 * Exception handling
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Christian Wawersich
 *******************************************************************************/

#include "all.h"

#ifdef DEBUG
#define VERBOSE_UNCAUGHT_EXCEPTION 1
#endif

#define VERBOSE_UNCAUGHT_EXCEPTION 1
//#define VERBOSE_EXCEPTION 1   /* verbose scanning of exception table */
#define VERBOSE_DOMAINCROSSING_EXCEPTION 1	/* verbose throwing of exceptions across domain borders */
//#define VERBOSE_STACKTRACE 1  /* always print stack trace */

static void uncaught_exception(ObjectDesc * self);

ObjectDesc *createExceptionInDomain(DomainDesc * domain, const char *exception, const char *details)
{
	ObjectDesc *self;
	ObjectDesc *desc;
	jint params[1];
	jint ret;
	ClassDesc *exclass;
	code_t c;
	int i;

	/* fixme: find class in domain */
	exclass = findClassDesc(exception);
	if (exclass == NULL)
		return NULL;

	/* fixme: we alloc memory with irqs off */
	self = (ObjectDesc *) allocObjectInDomain(domain, exclass);
	if (self == NULL)
		return NULL;

	if (details == NULL) {
		/* call constructor <init>()V */
		for (i = 0; i < exclass->numberOfMethods; i++) {
			if ((strcmp("<init>", exclass->methods[i].name) == 0)
			    && (strcmp("()V", exclass->methods[i].signature)
				== 0)) {

				c = (code_t) exclass->methods[i].code;
				ASSERT(c != 0);
				ret = callnative_special(params, self, c, 0);

				break;
			}
		}
	} else {
		/* call constructor <init>(Ljava/lang/String;)V */
		for (i = 0; i < exclass->numberOfMethods; i++) {
			if ((strcmp("<init>", exclass->methods[i].name) == 0)
			    && (strcmp("(Ljava/lang/String;)V", exclass->methods[i].signature) == 0)) {

				params[0] = (jint) newString(domain, details);
				c = (code_t) exclass->methods[i].code;
				ASSERT(c != 0);
				ret = callnative_special(params, self, c, 1);

				break;
			}
		}
	}

	return self;
}

void throw_RuntimeException(jint dummy)
{
}

void throw_NullPointerException(jint dummy)
{
	u4_t *base;
	ObjectDesc *self;
	base = (u4_t *) & dummy - 2;	/* stackpointer */
	self = createExceptionInDomain(curdom(), "java/lang/NullPointerException", NULL);
	throw_exception(self, base);
}

void throw_OutOfMemoryError(jint dummy)
{
}

void throw_IndexOutOfBounds(jint dummy)
{
	u4_t *base;
	ObjectDesc *self;
	base = (u4_t *) & dummy - 2;	/* stackpointer */
	self = createExceptionInDomain(curdom(), "java/lang/IndexOutOfBoundsException", "memory access out of range");
	throw_exception(self, base);
}

void throw_ArrayIndexOutOfBounds(jint dummy)
{
	u4_t *base;
	ObjectDesc *self;
	base = (u4_t *) & dummy - 2;	/* stackpointer */
	self = createExceptionInDomain(curdom(), "java/lang/IndexOutOfBoundsException", "array index out of bounds");
	throw_exception(self, base);
}

void throw_StackOverflowError()
{
	/*
	 * FIXME: GC is collecting the Exception before it is thrown!!
	 */
	DISABLE_IRQ;
#ifdef STACK_GROW
	thread_inc_current_stack(STACK_CHUNK_SIZE);
#else
	sys_panic("out of stack (%s:%d)", __FILE__, __LINE__);
#endif
	RESTORE_IRQ;
	return;
}

void throw_ArithmeticException(jint dummy)
{
	u4_t *base;
	ObjectDesc *self;
	base = (u4_t *) & dummy - 2;	/* stackpointer */
	self = createExceptionInDomain(curdom(), "java/lang/ArithmeticException", NULL);
	throw_exception(self, base);
}

void exceptionHandlerInternal(char *msg)
{
	ObjectDesc *ex = createExceptionInDomain(curdom(), "java/lang/RuntimeException", msg);
	throw_exception(ex, ((u4_t *) & msg - 2));
}

void exceptionHandler(jint * p)
{
	exceptionHandlerMsg(p, "unknown");
}

void exceptionHandlerMsg(jint * p, char *msg)
{
	u4_t *base;
	ObjectDesc *self = NULL;

#ifdef VERBOSE_STACKTRACE
	printf("\n\nMESSAGE: %s\n\n", msg);
#endif

	base = (u4_t *) & p - 2;	/* stackpointer */
	self = NULL;

	/* first check whether exception was thrown during GC -> terminate this domain */
	if (curdom()->gc.gcThread != NULL && curdom()->gc.gcThread->state != STATE_AVAILABLE) {
		domain_panic(curdom(), "exception during GC");
	}

	if (((jint) p) == THROW_RuntimeException) {
		self = createExceptionInDomain(curdom(), "java/lang/RuntimeException", msg);
	} else if (((jint) p) == THROW_NullPointerException) {
		self = createExceptionInDomain(curdom(), "java/lang/NullPointerException", NULL);
	} else if (((jint) p) == THROW_OutOfMemoryError) {
		self = createExceptionInDomain(curdom(), "java/lang/OutOfMemoryError", NULL);
		/*printf("OUT OF MEMORY EXCEPTION\n"); */
	} else if (((jint) p) == THROW_MemoryIndexOutOfBounds) {
		self = createExceptionInDomain(curdom(), "java/lang/IndexOutOfBoundsException", "memory access out of range");
	} else if (((jint) p) == THROW_StackOverflowError) {
		self = createExceptionInDomain(curdom(), "java/lang/StackOverflowError", NULL);
		printf("STACK OUT OF RANGE EXCEPTION");
	} else if (((jint) p) == THROW_ArithmeticException) {
		self = createExceptionInDomain(curdom(), "java/lang/ArithmeticException", "divied by zero");
	} else if (((jint) p) == THROW_MagicNumber) {
		u4_t *arg;
		arg = (u4_t *) & p;
		printf("WRONG MAGICNUMBER EXCEPTION\n");
		printf("   Arg0: %p\n", *(arg + 0));
		printf("   Arg1: %p\n", *(arg + 1));
		printf("   Arg2: %p\n", *(arg + 2));
		//printf("    Scanning stack %p ...\n",curthr());
		//scan_stack(curdom(), curthr(), JNI_TRUE);
		domain_panic(curdom(), "wrong magicnumber");
	} else if (((jint) p) == THROW_ParanoidCheck) {
		printf("PARANOID CHECK EXCEPTION\n");
	} else if (((jint) p) == THROW_StackJam) {
		printf("STACK MIXUP EXCEPTION\n");
	} else if (((jint) p) == THROW_ArrayIndexOutOfBounds) {
		self = createExceptionInDomain(curdom(), "java/lang/IndexOutOfBoundsException", "array index out of bounds");
	} else if (((jint) p) == THROW_UnsupportedByteCode) {
		self =
		    createExceptionInDomain(curdom(), "java/lang/RuntimeException", "unsupported bytecode (long/double/float)");
	} else if (((jint) p) == THROW_InvalidMemory) {
		self = createExceptionInDomain(curdom(), "java/lang/RuntimeException", "memory invalid (revoked)");
	} else if (((jint) p) == THROW_MemoryExhaustedException) {
		self = createExceptionInDomain(curdom(), "jx/zero/MemoryExhaustedException", NULL);
	} else if (((jint) p) == THROW_MemoryExhaustedException) {
		self = createExceptionInDomain(curdom(), "jx/zero/DomainTerminatedException", NULL);
	} else if (p != NULL) {
		self = (ObjectDesc *) p;
	}
#ifdef SAMPLE_FASTPATH
	if (do_sampling) {
		printStackTraceNew("SLOWOPT-EXCEPTION ");
	}
#endif

	if (self != NULL)
		throw_exception(self, base);

	uncaught_exception(self);
}

static void uncaught_exception(ObjectDesc * self)
{
#ifdef VERBOSE_UNCAUGHT_EXCEPTION
	ClassDesc *exclass = 0;

	if (self != NULL) {
		exclass = obj2ClassDesc(self);
	} else {
		exclass = NULL;
	}

	if (((u4_t) exclass >= 0xfffffff0 && (u4_t) exclass <= 0xffffffff)
	    || exclass == NULL) {
		printf("uncaught exception %d\n", exclass);
	} else {
		printf("uncaught exception \"%s\" 0x%lx!\n", exclass->name, self);
		if (self->data[1] != NULL) {	/* String message; */
			char value[128];
			stringToChar(self->data[1], value, sizeof(value));
			printf(" MESSAGE: %s\n", value);
		}
	}
	printf("TERMINATE THREAD %d.%d\n", TID(curthr()));
	printStackTraceNew("TERMINATING thread");
#endif
	thread_exit();
}

void throw_exception(ObjectDesc * exception, u4_t * sp)
{
	u4_t *ebp, *eip;
	u4_t bytecodePos, i;
	MethodDesc *method;
	ClassDesc *classInfo, *exclass;
	ThreadDesc *thread;
	DomainDesc *domain;
	ExceptionDesc *e;
#ifdef PROFILE
	unsigned long long t;
#endif

#ifdef VERBOSE_STACKTRACE
	printStackTraceNew("EXCEPTION");
#endif

	domain = curdom();
	exclass = obj2ClassDesc(exception);


	if (exclass != NULL) {
		thread = curthr();

		while (sp > thread->stack && sp < thread->stackTop) {
			ebp = (u4_t *) * sp++;
			eip = (u4_t *) * sp++;
			/* catch all exceptions that reach a service entry point (callnative_special_portal) */
			if (in_portalcall(eip)) {
				ThreadDesc *source = curthr()->mostRecentlyCalledBy;
				/* check if domain still exists */
				if (curthr()->callerDomainID != curthr()->callerDomain->id
				    || curthr()->callerDomain->state != DOMAIN_STATE_ACTIVE) {
					/* caller disappeared, ignore exception */
					printf("CALLER DOMAIN WAS TERMINATED!!!!");
					printf("CANNOT DELIVER THE FOLLOWING EXCEPTION.");
					printStackTraceNew("EXCEPTION");
				} else {
					u4_t quota = RECEIVE_PORTAL_QUOTA;
					source->portalReturn = copy_reference(curdom(), source->domain, exception, &quota);
					source->portalReturnType = PORTAL_RETURN_TYPE_EXCEPTION;
#ifdef VERBOSE_DOMAINCROSSING_EXCEPTION
					printf("CATCH AND RETHROW IN CALLER DOMAIN\n");
					printStackTraceNew("EXCEPTION");
#endif				/* VERBOSE_EXCEPTION */
				}
				reinit_service_thread();
				//} else if (in_fastportalcall(eip)) {
				//printf("CATCH IN FASTPORTAL AND THROW IN CALLER DOMAIN\n");
			} else {
				//      printf("%p not in portalcall\n", eip);
			}

#ifdef USE_PUSHED_METHODDESC
			if (findMethodAtFramePointer(ebp, &method, &classInfo) == 0) {	/* } */
				bytecodePos = findByteCodePosition(method, eip);
#else
			if (findMethodAtAddrInDomain(curdom(), (char *) eip, &method, &classInfo, &bytecodePos, &i) == 0) {
#endif
#ifdef VERBOSE_EXCEPTION
				printf("EX: %s, exclass=%s\n", method->name, exclass->name);
#endif				/* VERBOSE_EXCEPTION */

				/* compute the top of the operand stack */
				sp = ebp - method->sizeLocalVars;
#ifdef PROFILE
				if (method->isprofiled == JNI_TRUE)
					sp -= 5;
#endif
				/* lookup exception handler */
				for (i = 0; i < method->sizeOfExceptionTable; i++) {
					e = &(method->exceptionTable[i]);

					if (bytecodePos >= e->start && bytecodePos < e->end) {
#ifdef VERBOSE_EXCEPTION
						if (e->type)
							printf("    CATCHER: %s\n", e->type->name);
#endif				/* VERBOSE_EXCEPTION */
						if (e->type == NULL || check_assign(e->type, exclass)
						    == JNI_TRUE) {

							/* push exception */
							sp--;
							sp[0] = (u4_t)
							    exception;

							/* jump into exception handler (point of no return) */
#ifdef VERBOSE_EXCEPTION
							printf("    HANDLER: %p\n", (char *) ((u4_t)
											      e->addr + (u4_t)
											      method->code));
#endif				/* VERBOSE_EXCEPTION */
							callnative_handler(ebp, sp, (char *) ((u4_t)
											      e->addr + (u4_t)
											      method->code));
#ifdef DEBUG
							domain_panic(curdom(), "should not reach this point");
#endif
						}
					}
				}	/* end of exception table ( no exception handler found ) */
#ifdef PROFILE
				/* compute profile data */
				asm volatile ("rdtsc":"=A" (t):);
				if (method->isprofiled == JNI_TRUE)
					sp = profile_call2(method, sp, t);
#endif
			} else {	/* Method not found */
#ifdef VERBOSE_EXCEPTION
				printf("EX: method not found\n");
#endif				/* VERBOSE_EXCEPTION */
				//break;
			}
			/* leaf method frame */
			sp = ebp;
		}		/* scan stack */
	}
	/* known exception */
	uncaught_exception(exception);
}
