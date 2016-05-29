/********************************************************************************
 * One shot threads
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Meik Felser
 *******************************************************************************/

#include "all.h"

ThreadDesc *switchBackTo = NULL;	/* FIXME: use one pointer per CPU */

void returnfrom_oneshot()
{
	register unsigned int _eax__;
	asm volatile ("movl %%eax, %0":"=r" (_eax__));
	{
		ThreadDesc *next;
		CLI;
		//  printf("EAX: %p\n", _eax__);
		curthr()->state = STATE_AVAILABLE;

		ASSERT(switchBackTo->state == STATE_WAIT_ONESHOT);
		switchBackTo->state = STATE_RUNNABLE;
		switchBackTo->context[PCB_EAX] = _eax__;
/*		printf("cur=%p back=%p\n", *curthrP(), switchBackTo);*/
		destroy_switch_to(curthrP(), switchBackTo);
	}
}

/* returns 1 if already prepared else 0 */
static int prepare_thread_using_code1(ObjectDesc * obj, ThreadDesc * thread, code_t c, u4_t param)
{
	jint *sp;
	ASSERTTHREAD(thread);
	if (thread->state != STATE_AVAILABLE) {
		printf("thread state != AVAILABLE\n");
		printf("State of oneshot thread is %s\n", get_state(thread));
		sys_panic("thread already prepared (sys_panic?)\n");
		return 1;
	}

	sp = thread->stackTop;
	stack_push(&sp, param);
	stack_push(&sp, obj);
	stack_push(&sp, returnfrom_oneshot);
	thread->context[PCB_ESP] = sp;
	thread->context[PCB_EBP] = NULL;
	thread->context[PCB_EIP] = c;
#ifdef KERNEL
	thread->context[PCB_EFLAGS] &= ~0x00000200;
#else
	sigaddset(&(thread->sigmask), SIGALRM);
#endif
	thread->state = STATE_RUNNABLE;
	return 0;
}

u4_t start_thread_using_code1(ObjectDesc * obj, ThreadDesc * thread, code_t c, u4_t param)
{
	prepare_thread_using_code1(obj, thread, c, param);

	if (curthr()->isInterruptHandlerThread)
		sys_panic("IRQ thread (%d.%d) is not allowed to start other thread (%d.%d)", curthr()->domain->id, curthr()->id,
			  thread->domain->id, thread->id);

	switchBackTo = curthr();
	curthr()->state = STATE_WAIT_ONESHOT;

	ASSERTCLI;
#ifdef JAVASCHEDULER
#ifdef DEBUG
	printf("oneshot starts Thread %d.%d\n", TID(thread));
#endif
	return switch_to(curthrP(), thread);	/* HACK */
//  return Sched_switchToGCThread(thread);
#else
	return switch_to(curthrP(), thread);
#endif
}

void unblock_thread_using_code1(ObjectDesc * obj, ThreadDesc * thread, code_t c, u4_t param)
{
#ifdef JAVASCHEDULER
	if (curthr() == thread->schedulingDomain->Scheduler[get_processor_id()]->SchedThread) {
		/* helperThread is busy -> do nothing but warn */
		/*printf("unblock_thread_using_code1: SchedThread is busy\n"); */
		return;
	}
#endif

	if (prepare_thread_using_code1(obj, thread, c, param) == 1)	/* thread already prepared */
		return;

#ifdef JAVASCHEDULER
#ifdef ENABLE_GC
	Sched_GCunblocked(thread);
#endif
#else
	printf("not tested (neither implemented) continue?");
	//monitor(0);
	// do nothing!!
//???  runqueue_append0(thread);
#endif
}
