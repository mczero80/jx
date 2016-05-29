package timerpc;

import jx.timer.*;

class TimerEntry implements Timer {
    
    // time in ticks when the method via objref should be called
    private int expires;
    private int interval;
    private int diffTime;
    private TimerEntry next;
    private TimerEntry previous;
    // argument to the timer-method - method has to cast to the approbiate object
    private Object arg;
    private TimerHandler objref;
    private TimerManagerImpl timerManager;


    /**
     * timer expires in now + diffTime 
     */
    public TimerEntry(int expires, int interval, TimerManagerImpl timerManager, TimerHandler ref, Object c) {
	this.expires = expires;
	this.interval = interval;
	arg = c;
	objref = ref;
	next = null;
	previous = null;
	this.timerManager = timerManager;
    }

     final public int getExpiresFromNow() {
	return expires - timerManager.getCurrentTime();
    }

     final public int getInterval() {
	return interval;
    }

    public TimerManager getTimerManager() {
	return timerManager;
    }

    final public void set_next(TimerEntry n) {
	next = n;
    }
     final public void set_previous(TimerEntry p) {
	previous = p;
    }
     final public int get_expires() {
	return expires;
    }
     final public void set_expires(int e) {
	expires = e;
    }
     final public int get_diffTime() {
	return diffTime;
    }
     final public TimerEntry get_next() {
	return next;
	//throw new Error("TIMER!");
    }
     final public TimerEntry get_previous() {
	//throw new Error("TIMER!");
	return previous;
    }
     final public Object get_arg() {
	return arg;
    }
     final public void docall() {
	objref.timer(arg);
    }
     final public boolean compare(TimerHandler compareto) {
	return objref.equals(compareto);
    }

     public String toString() {
	String result = "************ TimerEntry ************\n";
	result +=       "expires: " + expires + "\narg: " + arg + "\nobjref: " + objref + "\nTimerTyp: " + objref.getClass().getName();
	result +=     "\n************* finished *************\n";
	return result;
    }
	    

}
