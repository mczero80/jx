#include "config.h"
#ifdef TIMER_EMULATION
#include "all.h"

#ifndef NO_TIMER_IRQ
#error ERROR  TIMER_EMULATION without NO_TIMER_IRQ
#endif

//static ThreadDesc *handler=NULL;
static CPUStateProxy *handler = NULL;
AtomicVariableProxy *atomic = NULL;

static void signal_handler_timeremulation(int signum, struct sigcontext sc)
{
	if (handler) {
		//threadunblock(handler);
		atomicvariable_atomicUpdateUnblock(atomic, NULL, handler);
	}
}


jint timerEmulation_getTime(ObjectDesc * self)
{
	sys_panic("not implemented");
}

void timerEmulation_installIntervalTimer(ObjectDesc * self, AtomicVariableProxy * atomicProxy, CPUStateProxy * cpuStateProxy,
					 jint sec, jint usec)
{
	struct itimerval value;
	if (handler != NULL)
		sys_panic("handler already installed");
	// handler = cpuState2thread(cpuStateProxy);
	handler = cpuStateProxy;
	atomic = atomicProxy;

	install_alrmhandler(SIGALRM, signal_handler_timeremulation);

	value.it_interval.tv_sec = sec;
	value.it_interval.tv_usec = usec;
	value.it_value.tv_sec = sec;
	value.it_value.tv_usec = usec;
	setitimer(ITIMER_REAL, &value, NULL);
}


MethodInfoDesc timerEmulationMethods[] = {
	{"getTime", "", timerEmulation_getTime}
	,
	{"installIntervallTimer", "", timerEmulation_installIntervalTimer}
	,
};

void init_timer_emulation_portal()
{
	init_zero_dep("jx/zero/TimerEmulation", "TimerEmulation", timerEmulationMethods, sizeof(timerEmulationMethods),
		      "<jx/zero/TimerEmulation>");
}

#endif				/* TIMER_EMULATION */
