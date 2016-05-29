package jx.scheduler;

import jx.zero.*;
import jx.zero.debug.*;

public class ThreadListCAS extends LinkedListCAS {
    final private HLSchedulerSupport HLschedulerSupport=(HLSchedulerSupport)InitialNaming.getInitialNaming().lookup("HLSchedulerSupport");

    public void dump() {
	if (getFirst() == null)
	    Debug.out.println("       none");
	else
	    for ( CPUState t = (CPUState)getFirst(); t != null; t=(CPUState)getNext()) {
		HLschedulerSupport.dumpThread(t);
		}
    }
}
