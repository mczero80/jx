package test.portal;

import jx.zero.*;
import jx.zero.debug.*;
import jx.zero.scheduler.*;

import jx.scheduler.*;

public class Main {
    /*
    
    static private void createServiceDom1()  {
	
	ServiceHLS sched= new ServiceHLS();
	sched .DomainName = "ServiceDomain1";
	
	Naming naming = InitialNaming.getInitialNaming();
	DomainManager dm = (DomainManager) naming.lookup("DomainManager");
	    DomainStarter.createDomain("ServiceDomain1",
				null,  // CPU Objects
//       			new CPU[] {Cpus[0]},  // CPU Objects
//				new String[] {"test/portal/ServiceHLS",},
			        new HighLevelScheduler [] {sched}, // Highlevel-Scheduler Objects
				"test_portal.jll",
				null,
				"test/portal/ServiceDomainImpl1",500*1024,naming,null);
	ServiceDomain dom;
        CPUManager c = (CPUManager)naming.lookup("CPUManager");	
	int i=0;
	do {
	    c.yield();
	    //Debug.out.println("lookup");
	    dom = (ServiceDomain) naming.lookup("Service1");
	    if (i++>1000) {
		i=0;
		Debug.out.println("NO Service1 Portal!");
	    }
	} while(dom == null);
    }

     static private void createServiceDom2()  {
	
//	ServiceHLS sched= new ServiceHLS();
	HLRRobin_nonpreemptive sched= new HLRRobin_nonpreemptive();
	sched.DomainName = "ServiceDomain2";
	
	Naming naming = InitialNaming.getInitialNaming();
	DomainManager dm = (DomainManager) naming.lookup("DomainManager");
	DomainStarter.createDomain("ServiceDomain2",
				null,  // CPU Objects
//       			new CPU[] {Cpus[0]},  // CPU Objects
//				new String[] {"jx/scheduler/HLRRobin_nonpreemptive",},
				new HighLevelScheduler [] {sched}, // Highlevel-Scheduler Objects
				"test_portal.jll",
				null,
				"test/portal/ServiceDomainImpl2",500*1024,naming,null);
	ServiceDomain dom;
	
	int i=0;
	do {
	    Thread.yield();
	    //Debug.out.println("lookup");
	    dom = (ServiceDomain) naming.lookup("Service2");
	    i++;
	    if (i>1000) {
		i=0;
		Debug.out.println("NO Service2 Portal!");
	    }
	} while(dom == null);
    }

     static private void createCallerDom()  {
	
	CallerHLS sched= new CallerHLS();
	sched .DomainName = "CallerDomain";

	Naming naming = InitialNaming.getInitialNaming();
	DomainManager dm = (DomainManager) naming.lookup("DomainManager");
	DomainStarter.createDomain("CallerDomain",
				null,  // CPU Objects
//       			new CPU[] {Cpus[0]},  // CPU Objects
// 				new String[] {"test/portal/CallerHLS",},
				new HighLevelScheduler [] {sched}, // Highlevel-Scheduler Objects
				"test_portal.jll",
				null,
				"test/portal/CallerDomain",500*1024,naming,null);
    }

    public static boolean test(Naming naming) {
	
	createServiceDom2();
	Debug.out.println("Service2 Portal ok!");
	
       	createServiceDom1();
	Debug.out.println("Service1 Portal ok!");

	createCallerDom();
	return true;			
   }
    */
}
