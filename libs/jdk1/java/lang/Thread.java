package java.lang;

import jx.zero.Naming;
import jx.zero.CPUManager;
import jx.zero.InitialNaming;
import jx.zero.ThreadEntry;
import jx.zero.Naming;
import jx.zero.debug.*;


public class Thread implements Runnable {
    public final static int MIN_PRIORITY = 1;
    public final static int NORM_PRIORITY = 5;
    public final static int MAX_PRIORITY = 10;

    private static Naming naming;
    private static CPUManager cpuManager;

    static {
	naming = InitialNaming.getInitialNaming();
    }

    private Runnable runnable;
    private int priority;
    private String name;
    private ThreadGroup group;
    private boolean daemon = false;
    boolean alive = false;
    public static Thread currentThread() {
	return null;
    }

    public static void yield() {
      //((CPUManager)DomainZeroLookup.getDomainZero().lookup("CPUManager")).yield();
      cpuManager.yield();
    }

    public static void sleep(long msec) throws InterruptedException {
	cpuManager.sleep((int)msec, 0);
    }

    public static void sleep(long msec, int nsec) throws InterruptedException {
	cpuManager.sleep((int)msec, nsec);
    }

    public Thread() {
    }
    public Thread(Runnable runnable) {
	this(null, runnable, "thread");
    }
    public Thread(ThreadGroup group, Runnable runnable) {
	this(group, runnable, "thread");
    }
    public Thread(String name) {
	this(null, null, name);
    }    
    public Thread(ThreadGroup group, String name) {
	this(group, null, name);
    }
    public Thread(Runnable runnable, String name) {
	this(null, runnable, name);
    }
    public Thread(ThreadGroup group, Runnable runnable, String name) {
	this.group = group;
	this.runnable = runnable;
	this.name = name;
    }
    public synchronized void start() {
	Naming naming = InitialNaming.getInitialNaming();
	//DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	//DebugPrintStream out = new DebugPrintStream(new DebugOutputStream(d));
	//out.println("java.lang.Thread: Starting new thread.");
	alive = true;
//	naming.startThread(new ThreadStarter(this));
	cpuManager.start(cpuManager.createCPUState(new ThreadStarter(this)));
    }
    public void run() {
	if (runnable != null) {
	    runnable.run();
	}
    }
    public final void stop() {
    }
    public final synchronized void stop(Throwable o) {
    }
    public void interrupt() {
    }
    public static boolean interrupted() {
	return currentThread().isInterrupted(true);
    }
    public boolean isInterrupted() {
	return isInterrupted(false);
    }
    private boolean isInterrupted(boolean clearInterrupted) {
	return false;
    }
    public void destroy() {
    }
    public final boolean isAlive() {
	return alive;
    }
    public final void suspend() {
    }
    public final void resume() {
    }
    public final void setPriority(int newPriority) {
	if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
	    throw new IllegalArgumentException();
	}
	if (newPriority > group.getMaxPriority()) {
	    newPriority = group.getMaxPriority();
	}
	priority = newPriority;
    }
    public final int getPriority() {
	return priority;
    }
    public final void setName(String name) {
	this.name = name;
    }
    public final String getName() {
	return name;
    }
    public final ThreadGroup getThreadGroup() {
	return group;
    }
    public static int activeCount() {
	return currentThread().getThreadGroup().activeCount();
    }
    public static int enumerate(Thread tarray[]) {
	return currentThread().getThreadGroup().enumerate(tarray);
    }
    public int countStackFrames() {
	return 0;
    }
    public final synchronized void join(long millis) throws InterruptedException {
	long base = System.currentTimeMillis();
	long now = 0;

	if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
	}

	if (millis == 0) {
	    while (isAlive()) {
		Thread.yield();
	    }
	} else {
	    while (isAlive()) {
		long delay = millis - now;
		if (delay <= 0) {
		    break;
		}
		wait(delay);
		now = System.currentTimeMillis() - base;
	    }
	}
    }
    public final synchronized void join(long millis, int nanos) throws InterruptedException {

	if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
	}

	if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
				"nanosecond timeout value out of range");
	}

	if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
	    millis++;
	}

	join(millis);
    }
    public final void join() throws InterruptedException {
	while (isAlive()) {
	    Thread.yield();
	}
    }
    public static void dumpStack() {
	new Exception("Stack trace").printStackTrace();
    }
    public final void setDaemon(boolean on) {
	if (isAlive()) {
	    throw new IllegalThreadStateException();
	}
	daemon = on;
    }
    public final boolean isDaemon() {
	return daemon;
    }
    public String toString() {
	if (getThreadGroup() != null) {
	    return "Thread[" + getName() + "," + getPriority() + "," + 
		            getThreadGroup().getName() + "]";
	} else {
	    return "Thread[" + getName() + "," + getPriority() + "," + 
		            "" + "]";
	}
    }

    public ClassLoader getContextClassLoader() {
	throw new Error("NOT IMPLEMENTED");
    }

    static {
	naming = InitialNaming.getInitialNaming();
	cpuManager = (CPUManager) naming.lookup("CPUManager");
    }


  class ThreadStarter implements ThreadEntry {
    Thread runnable;
    ThreadStarter(Thread runnable) { this.runnable = runnable; }
    public void run() {
      if (runnable.name != null) {
	CPUManager cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	cpuManager.setThreadName(runnable.name);
      }
      
      runnable.run();
      runnable.alive = false;	
    }
  }
  
  
}


