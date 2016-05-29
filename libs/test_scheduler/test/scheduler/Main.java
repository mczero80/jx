package test.scheduler;

import jx.zero.*;
import jx.zero.debug.*;
import jx.zero.scheduler.*;

import jx.scheduler.*;

class TargetDomain_Impl {
    final boolean createObjects = false;
    int count;
    int count1;
    int count2;
    public static void init(Naming naming, String [] args) {
	new TargetDomain_Impl(naming, args);
    }
    public TargetDomain_Impl(Naming naming, String [] args) {
	CPUManager cpuManager = (CPUManager)naming.lookup("CPUManager");
	int event = cpuManager.createNewEvent(args[0]);
	for(;;) {
	    if (createObjects) new Object();
	    count++;
	    if (count == 0x00000fff) {
		cpuManager.recordEvent(event);
		count =0; count1++;
		if (count1 == 0x0fffffff) {
		    count1=0;
		    count2++;
		}
	    }
	}
    }
}

public class Main {
    public static void main(String args[]) {
	    DomainStarter.createDomain("DomainA","test_scheduler","test/scheduler/TargetDomain_Impl",1*1024*1024, 
				       new String[] {"domain1"}, new int[] {10} );
	    DomainStarter.createDomain("DomainB","test_scheduler","test/scheduler/TargetDomain_Impl",1*1024*1024,
				       new String[] {"domain2"}, new int[] {5});
    }
}
