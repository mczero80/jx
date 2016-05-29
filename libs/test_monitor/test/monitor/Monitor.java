package test.monitor;

import jx.zero.*;
import java.util.*;

public class Monitor {
    static Hashtable locks = new Hashtable();
    static CPUManager cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
    static final public void enter(Object obj) {
	cpuManager.inhibitScheduling();
	try {
	    Debug.out.println("MONITORENTER");
	    Lock lock = (Lock)locks.get(obj);
	    if (lock == null) {
		lock = new Lock();
		locks.put(obj, lock);
	    } 
	    if (lock.owner==null) {
		lock.owner = cpuManager.getCPUState();
	    }
	    if (lock.owner != cpuManager.getCPUState()) {
		throw new Error("waiting not implemented");
	    }
	} finally {
	    cpuManager.allowScheduling();
	}
    }
    static final public void exit(Object obj) {
	cpuManager.inhibitScheduling();
	try {
	    Debug.out.println("MONITOREXIT");
	    Lock lock = (Lock)locks.get(obj);
	    if (lock.owner != cpuManager.getCPUState()) {
		throw new Error();
	    }
	    lock.owner = null;
	    if (lock.waiting != null)
		throw new Error("waiting not implemented");
	} finally {
	    cpuManager.allowScheduling();
	}
    }
}

class Waiter {
    CPUState thread;
    Waiter next;
}

class Lock {
    Object obj;
    CPUState owner;
    Waiter waiting;
}
