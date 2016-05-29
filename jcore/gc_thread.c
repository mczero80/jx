#include "all.h"

#include "gc_thread.h"

int nopreempt_check(u4_t eip);

ThreadDesc *activeGC = NULL;

void _return_to_continue_thread()
{
	ASSERTTHREAD(activeGC);
	destroy_switch_to(curthrP(), activeGC);
}

void return_to_continue_thread();
asm(".text\n" ".align 4\n" ".type  return_to_continue_thread,@function\n" "return_to_continue_thread:\n"
    "     call save_context2current\n" "     jmp _return_to_continue_thread\n" ".Lreturn_to_continue_thread_end:\n"
    "	         .size return_to_continue_thread, .Lreturn_to_continue_thread_end-return_to_continue_thread\n");




#define EFLAGS_ENABLED_IRQS  0x00000212
#define EFLAGS_DISABLED_IRQS 0x00000012

/* resumes a thread until a given IP is reached */
static void continue_Thread_to_pos(ThreadDesc * thread, MethodDesc * method, u4_t eip)
{

	u4_t offset, patch_offset;
	char *newcode, *addr;
	//int i;
	MethodDesc *newMethod;
#ifdef KERNEL
	volatile u4_t flags_backup;
#else
	sigset_t sigmask_backup, sigmask_temp;
#endif

	if (eip == thread->context[PCB_EIP])
		return;

	activeGC = curthr();

	ASSERT(eip > (u4_t) method->code)
	    ASSERT(eip < (u4_t) (method->code + method->numberOfCodeBytes))
	    ASSERT(eip > (u4_t) thread->context[PCB_EIP])
	    ASSERT(thread->context[PCB_EIP] > (u4_t) method->code)
	    ASSERT(thread->context[PCB_EIP] < (u4_t) (method->code + method->numberOfCodeBytes))

	    offset = thread->context[PCB_EIP] - (u4_t) method->code;
	patch_offset = eip - (u4_t) method->code;

	/* copy method */
	newMethod = cloneMethodInDomain(thread->domain, method);
	newcode = (char *) (newMethod->code);

	/* patch new code */
	newcode[patch_offset++] = 0xe8;	/* call */
	addr = (char *) ((char *) return_to_continue_thread - (newcode + patch_offset + 4));
	*(char **) (newcode + patch_offset) = addr;

	/* prepare thread and run it */
	thread->context[PCB_EIP] = (u4_t) (newcode + offset);
#ifdef KERNEL			/* switch_to will restore the eflags -> so we have to modify them */
	flags_backup = thread->context[PCB_EFLAGS];
	thread->context[PCB_EFLAGS] = EFLAGS_DISABLED_IRQS;
#else
	sigmask_backup = thread->sigmask;
	sigemptyset(&sigmask_temp);
	sigaddset(&sigmask_temp, SIGALRM);
	thread->sigmask = sigmask_temp;
#endif

	/*printf("activating Thread at %p ...\n",newcode + offset); */
	switch_to(curthrP(), thread);
	/*printf("....back in continue_Thread_to_pos\n"); */
	/* repair TCB */
	thread->context[PCB_EIP] = eip;
#ifdef KERNEL
	thread->context[PCB_EFLAGS] = flags_backup;
#else
	thread->sigmask = sigmask_backup;
#endif

/* free Memory ?? */
}

void irq_exit(int cpuID);
void return_from_java0(ThreadDesc * next, ContextDesc * restore);
void return_from_java1(long param, ContextDesc * restore, ThreadDesc * next);
void return_from_java2(long param1, long param2, ThreadDesc * next, ContextDesc * restore);
void never_return(void);
extern unsigned char callnative_special_end[], callnative_special_portal_end[], callnative_static_end[], thread_exit_end[];
#ifdef KERNEL
extern unsigned char irq_exit_end[];
#endif
//#ifdef JAVASCHEDULER
extern unsigned char return_from_javaX_end[], never_return_end[];
//#endif



/* checks wether there is a stackmap at the eip of the thread 
   if there is none the thread is activated until a stackmap-position is reached */
void check_thread_position(DomainDesc * domain, ThreadDesc * thread)
{
	u4_t eip;
	ClassDesc *classInfo;
	MethodDesc *method;
	jint bytecodePos, lineNumber;

	eip = thread->context[PCB_EIP];
	/*
	   printf("Thread:%p (%s)\n",thread, thread->name);
	   printf("  X eip:%p ",eip);
	   print_eip_info(eip);
	   printf("\n");
	 */


#ifdef NOPREEMPT
#ifdef ROLLFORWARD_ON_GC
	if (nopreempt_check(eip)) {
		sys_panic("ROLLFORWARD_ON_GC not yet implemented");
	}
#endif				/* ROLLFORWARD_ON_GC */
#endif

	if ((eip >= (u4_t) callnative_special && eip <= (u4_t) callnative_special_end)
	    || (eip >= (u4_t) callnative_static && eip <= (u4_t) callnative_static_end)
	    || (eip >= (u4_t) callnative_special_portal && eip <= (u4_t) callnative_special_portal_end)
//#ifdef JAVASCHEDULER
	    || (eip >= (u4_t) return_from_java0 && eip <= (u4_t) return_from_javaX_end)
	    || (eip >= (u4_t) return_from_java1 && eip <= (u4_t) return_from_javaX_end)
	    || (eip >= (u4_t) return_from_java2 && eip <= (u4_t) return_from_javaX_end)
	    || (eip >= (u4_t) never_return && eip <= (u4_t) never_return_end)
//#endif
	    ) {			/* C -> Java */
		sys_panic("what should I do?");
	} else if (findMethodAtAddrInDomain(domain, (char *) eip, &method, &classInfo, &bytecodePos, &lineNumber) == 0) {
		/* find stack map */
		SymbolDescStackMap *sym;
		char *symip;
		int j;
		/*printf("eip: %p  %s\n",eip, method->name); */
		for (j = 0; j < method->numberOfSymbols; j++) {
			if ((method->symbols[j]->type != 16) && (method->symbols[j]->type != 17))
				continue;
			sym = (SymbolDescStackMap *) method->symbols[j];
			symip = (char *) (method->code) + (jint) (sym->immediateNCIndex);

			/*printf("   stackmap at: %p  (IPpre: %p)\n",symip,(char*)(method->code + (jint)(sym->immediateNCIndexPre))); */

			if ((u4_t) symip == eip) {
				/*printf("Map found at NCIndex %ldat %p\n", sym->immediateNCIndex); */
				break;
			}
			if ((u4_t) symip > eip) {
				printf("No stackmap for this frame! at %p (Thread: %s); thread=%p\n", (void *) eip, thread->name,
				       thread);
				thread->domain->advancingThreads = 1;
				continue_Thread_to_pos(thread, method, (u4_t) ((u1_t *)
									       method->code + (jint)
									       (sym->immediateNCIndexPre)));
				thread->domain->advancingThreads = 0;
				break;
			}
		}
	}

}

extern unsigned char cpuManager_receive_end[];
jint cpuManager_receive(ObjectDesc * self, Proxy * portal);
