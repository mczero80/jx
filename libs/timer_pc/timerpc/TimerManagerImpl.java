package timerpc;

import jx.timer.*;
import jx.zero.*;

import jx.rtc.*;

public class TimerManagerImpl implements TimerManager, Service {
    static final boolean debug = false;
    Naming naming;
    Clock clock;

    // a list of timer-objects; sorted via the expiration entry, next expiration first
    TimerEntry firsttimer;
    TimerEntry helptimer;
    TimerEntry previous;
    int ticks;

    TimerHandler unblockHandler;

    public TimerManagerImpl() {
	naming =  InitialNaming.getInitialNaming();
	final Object dummy = new Object();
	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	clock = (Clock) naming.lookup("Clock");

	unblockHandler = new TimerHandler() {
		public void timer(Object arg) {
		    cpuManager.unblock((CPUState)arg);
		}
	    };

	final AtomicVariable atomic = cpuManager.getAtomicVariable();
	atomic.set(dummy);
	CPUState cpuState = cpuManager.createCPUState(new ThreadEntry() {
		public void run() {
		  cpuManager.setThreadName("TimerManager");
		    for(;;) {
			atomic.blockIfNotEqual(null);
			atomic.set(dummy);
			ticks++;
			helptimer = firsttimer;
			if (debug) Debug.out.println("Timerticks: " + ticks);
	
			// check for timers and do the corresponding calls
			// afterwards, remove the called timer-object

			if (debug) {
			    if (firsttimer != null)
				Debug.out.println("CLOCK: Timer ist eingetragen!");
			}

			// we do a quick hack to get also the timers removed which have a expires already passed
			while (helptimer != null /*&& ticks <= helptimer.get_expires()*/) {
			    if (helptimer.get_expires() /*==*/ <= ticks) {
				// found an expired one
				// call the method
				if (debug) Debug.out.println("Calling Timer with expires=: " + helptimer.get_expires());
				helptimer.docall();
				helptimer = helptimer.get_next();

				// remove the expired timer 
				{
				    TimerEntry helper;
				    TimerEntry ret=null;
				    if (debug) Debug.out.println("Entering removetimer...");
				    if (firsttimer != null) {
					ret = firsttimer;
					helper = firsttimer.get_next();
					if (helper != null)
					    helper.set_previous(null);
					firsttimer = helper;
				    }
				    if (debug) Debug.out.println("... removetimer finished");
				    int ival;
				    if ((ival = ret.getInterval()) != -1) {
					ret.set_expires(ret.get_expires()+ival);
					addTimerEntry(ret);
				    } 
				}

			    }
			    else
				break;
			}
		    }
		}
	});
	
	cpuManager.start(cpuState);

	TimerEmulation timerEmulation = (TimerEmulation) naming.lookup("TimerEmulation");
	if (timerEmulation==null) {
	    RTC rtc = new RTCImpl(naming);
	    rtc.installIntervallTimer(atomic, cpuState, RTC.HZ_4);
	    Debug.out.println("TimerManager: Installed RTC timer");
	} else {
	    timerEmulation.installIntervallTimer(atomic, cpuState, 0, 250000);
	    Debug.out.println("TimerManager: Use emulated timer");
	}
    }
    
    public Timer addMillisTimer(int expiresFromNowInMillis, TimerHandler handler, Object argument) {
	return addTimer(expiresFromNowInMillis * 1000 / getTimeBaseInMicros(), -1, handler, argument);
    }
    public Timer addMillisIntervalTimer(int expiresFromNowInMillis, int intervalInMillis, TimerHandler handler, Object argument) {
	int b = getTimeBaseInMicros();
	return addTimer(expiresFromNowInMillis * 1000 / b, intervalInMillis * 1000 / b, handler, argument);
    }
    public Timer addTimer(int expiresFromNow, int interval, TimerHandler handler, Object argument) {
	TimerEntry timer = new TimerEntry(expiresFromNow+ticks, interval, this, handler, argument);
	if (debug) Debug.out.println("Entering addtimer: "+(expiresFromNow+ticks));
	return addTimerEntry(timer);
    }
    public Timer addTimerEntry(TimerEntry timer) {
	TimerEntry helper = firsttimer;


	if (firsttimer == null) {
		firsttimer = timer;
	}
	else {
	    while (helper != null && (helper.get_expires() <= timer.get_expires())) {
		    previous = helper;
		    helper = helper.get_next();
	    }
	    if (previous != null) previous.set_next(timer);
	    timer.set_next(helper);
	    if (helper != null) helper.set_previous(timer);
	    timer.set_previous(previous);
	}
	if (debug) Debug.out.println("... addtimer finished!");
	return timer;
    }

    
    /** this method is for cases when we shut down a specific programm and want to delete all
     timers that may be registered but which aren´t yet expired
    */
    public boolean deleteTimer(TimerHandler which) {

        TimerEntry helper = firsttimer;
        TimerEntry nextone, prevone;
        boolean foundone = false;

	while (helper != null) {
	    if (helper.compare(which)) {
		foundone = true;
		nextone = helper.get_next();
		prevone = helper.get_previous();
		if (prevone == null && helper == firsttimer) {
		    firsttimer = nextone;
		}
		if (prevone != null) {
		    prevone.set_next(nextone);
		}
		if (nextone != null) {
		    nextone.set_previous(prevone);
                    }
		helper = nextone;
	    }
	    else {
		helper = helper.get_next();
	    }
	}
	
	return foundone;
    }    

    /** print all the timer-entrys currently contained in the list */

    public void dumptimers() {
	int amount = 0;
	TimerEntry helper = firsttimer;
	Debug.out.println("DumpTimers at: " + ticks);
	Debug.out.println("\nBegin of timers...\n");
	while (helper != null) {
	    Debug.out.println(helper);
	    Debug.out.println("\n");
	    amount++;
	    helper = helper.get_next();
	}
	
	Debug.out.println("" + amount + " Timers in the TimerList!\n");
	Debug.out.println("...End of timers!");
    }
    





    public int getTimeBaseInMicros() {
	return 250000; // 4 HZ
    }

    public int getCurrentTime() {
	return ticks;
	//return clock.getTimeInMillis()/250;
    }

    public int getCurrentTimePlusMillis(int milliSeconds) {
	return ticks + milliSeconds / 4;
    }


    public int getTimeInMillis() {
	/*
	int low = clock.getTicks_low();
	int high = clock.getTicks_high();
	return (high << 10) | (low >>> 22);
	*/
	return clock.getTimeInMillis();
    }

    public void unblockInMillis(CPUState thread, int timeFromNowInMillis) {
	addMillisTimer(timeFromNowInMillis, unblockHandler, thread);
    }

    public void unblockInMillisInterval(CPUState thread, int expiresFromNowInMillis, int intervalInMillis) {
	addMillisIntervalTimer(expiresFromNowInMillis, intervalInMillis, unblockHandler, thread);
    }
}
