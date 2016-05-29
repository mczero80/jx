package jx.scheduler;

import jx.zero.*;
import jx.zero.debug.*;

public class SuperHLRRobin extends HLRRobin {

    public SuperHLRRobin() {
	super();
    }
    
    /** extract all ThreadInfo from the previous Scheduler */
    public void getThreads(SuperHLRRobin oldSched) {
	this.runnables = oldSched.runnables;
    }
   
    public String toString() {
	return "this is a SuperHLRRobin Object";
    }
}
