package test.cpustate;

import jx.zero.*;

/** First thread endlessly blocks and prints number 1;
 * second thread endlessly unblocks first thread prints number two and yields
 */
public class Main {
    public static void main(String [] args) {
	final Naming naming = InitialNaming.getInitialNaming();
	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");

	final CPUState first = cpuManager.getCPUState();

	new Thread() {
		public void run() {
		    CPUState second = cpuManager.getCPUState();
		    for(;;) {
			while(! cpuManager.unblock(first));
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
