package timerpc;

import jx.timer.SleepManager;
import jx.zero.*;

public class SleepManagerImpl implements SleepManager, Service {
    Clock clock;
    //    TimerManager timerManager;
    CPUManager cpuManager;
    public SleepManagerImpl(String[] args) {
	this();
    }
    public SleepManagerImpl() {
	clock = (Clock) InitialNaming.getInitialNaming().lookup("Clock");
	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	//timerManager = (TimerManager) LookupHelper.waitUntilPortalAvailable(InitialNaming.getInitialNaming(), "TimerManager");
    }
    public void mdelay(int milliseconds) {
	int end = clock.getTimeInMillis()+milliseconds;
	while(end > clock.getTimeInMillis()) ; //Thread.yield();


	/*	
	CycleTime now = new CycleTime();
	CycleTime start = new CycleTime();
	CycleTime diff = new CycleTime();
	clock.getCycles(start);
	for(;;) {
	    for (int i = 0; i < 1000; i++) for (int j = 0; j < 10; j++);
	    clock.getCycles(now);
	    clock.subtract(diff, now, start);
	    int n = clock.toMilliSec(diff);
	    //Debug.out.println(n);
	    if (clock.toMilliSec(diff) > milliseconds) break;
	    cpuManager.yield();
	}
	*/


	/*
	timerManager.unblockInMillis(cpuManager.getCPUState(), milliseconds);
	cpuManager.block();  // DANGER: lost-update problem (FIXME)
	*/

    }
    public void udelay(int microseconds) {
      //int low = clock.getTicks_low();
      //int high = clock.getTicks_high();

      for (int j = 0; j < microseconds; j++)
	for (int i = 0; i < 150; i++)
	  ;
      
	/*
	// tsc is incremented every 2 nanoseconds on a 500 MHz CPU
	// we have to multiply our microsecs with 500 
	long startTime = CPU.rdtsc();
	long endTime = startTime + (microseconds-10) * 500; // remove 10 microseconds to account for the overhead
	while(CPU.rdtsc() < endTime);
	*/
	
    }
    public void sleep(int sec, int usec) { throw new Error(); }
}
