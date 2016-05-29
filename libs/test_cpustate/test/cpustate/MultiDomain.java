/* Test moving CPUState objects between domains */
/* DomainB remembers CPUState reference */
/* TEST1: DomainA performs GC -> reference in DomainB still valid? */
/* TEST2 (not implemented): DomainA is terminated -> reference in DomainB still valid? */

package test.cpustate;

import jx.zero.*;

interface MyService extends Portal {
    void init(CPUState c);
    boolean unblock();
}

class DomainA {
    public static void main(String [] args) {
	final Naming naming = InitialNaming.getInitialNaming();
	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");

	final CPUState first = cpuManager.getCPUState();
	final MyService svc = (MyService) LookupHelper.waitUntilPortalAvailable(naming, "MyService");


	new Thread() {
		public void run() {
		    CPUState second = cpuManager.getCPUState();
		    svc.init(first);
		    for(;;) {
			while(! svc.unblock());
			Debug.out.print("1");
			Thread.yield();
		    }
		}
	    }.start();

	for(;;) {
	    cpuManager.block();
	    Debug.out.print("2");
	}

    }
}




class DomainB implements MyService {
    CPUManager cpuManager;
    CPUState thread;

    public static void main(String [] args) {
	new DomainB(args);
    }

    DomainB(String [] args) {
	final Naming naming = InitialNaming.getInitialNaming();	
	cpuManager = (CPUManager) naming.lookup("CPUManager");
	naming.registerPortal(this, "MyService");
    }

    public void init(CPUState c) {
	this.thread = c;
    }

    public boolean unblock() {
	return cpuManager.unblock(thread); 
    }
}
