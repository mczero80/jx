package test.termination;

import jx.zero.*;
import jx.zero.debug.*;

public class Main {
    static MemoryManager memMgr;
    static Naming naming;
    static DomainManager dm;
    public static void main(String []args) {
	Main.naming = InitialNaming.getInitialNaming();
	memMgr = (MemoryManager)naming.lookup("MemoryManager");
	dm = (DomainManager)naming.lookup("DomainManager");
	//for(int i=0;i<1000;i++) Thread.yield();

	int mem_before = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory before domain started: "+mem_before);

	Debug.out.println ("Starting test domain ...\n");
	Domain child = DomainStarter.createDomain("Test1",
								 "test_termination.jll", 
								 "test/termination/ChildDomain",
								 2*1024*1024);
	
	if (child == null) throw new Error("Unable to create child domain\n");
    
	//for(int i=0;i<1000;i++) Thread.yield();
	Debug.out.println ("Free memory when domain running: "+memMgr.getTotalFreeMemory());

	//for(;;) Thread.yield();

	dm.terminate(child);

	int mem_term = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory when domain terminated: "+mem_term);
	Debug.out.println ("    Difference: "+(mem_before - mem_term));

	//for(;;) Thread.yield();
	createDomains();
	createDomains2();
	
    }

    static void createDomains() {
	int mem_before = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory before domain started: "+mem_before);

	int n = 20;
	Debug.out.println ("Starting "+n+" domains and terminating them...\n");
	Domain child[] = new Domain[n];
	for(int i=0; i<n; i++) {
	    child[i] = DomainStarter.createDomain("TestChild"+i,
					       "test_termination.jll", 
					       "test/termination/ChildDomain",
					       100*1024);
	    
	    if (child[i] == null) throw new Error("Unable to create child domain\n");
	}
	for(int i=0; i<n; i++) {
	    dm.terminate(child[i]);
	}
	int mem_term = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory when domain terminated: "+mem_term);
	Debug.out.println ("    Difference: "+(mem_before - mem_term));

    }

    static void createDomains2() {
	int mem_before = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory before domain started: "+mem_before);

	int n = 100;
	int rounds = 100;
	Debug.out.println (rounds+" times starting "+n+" domains and terminating them\n");
	Domain child[] = new Domain[n];
	for(int j=0; j<rounds;j++) {
	    for(int i=0; i<n; i++) {
		child[i] = DomainStarter.createDomain("TestChild"+i,
						   "test_termination.jll", 
						   "test/termination/ChildDomain",
						   100*1024);
		
		if (child[i] == null) throw new Error("Unable to create child domain\n");
	    }
	    for(int i=0; i<1000; i++) Thread.yield();
	    for(int i=0; i<n; i++) {
		dm.terminate(child[i]);
	    }
	    Debug.out.print(".");
	}
	int mem_term = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory when domain terminated: "+mem_term);
	Debug.out.println ("    Difference: "+(mem_before - mem_term));

    }


}

class ChildDomain {
    public static void init (final Naming naming /*String []args*/) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	for(;;) Thread.yield();
    }
}
