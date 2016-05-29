package test.freeze;

import jx.zero.*;
import jx.zero.debug.*;

public class Main {
    static MemoryManager memMgr;
    static Naming naming;
    public static void init (final Naming naming /*String []args*/) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	Main.naming = naming;
	memMgr = (MemoryManager)naming.lookup("MemoryManager");
	//for(int i=0;i<1000;i++) Thread.yield();

	Debug.out.println ("Starting test domain ...\n");
	Domain child = naming.createDomain("Test1",
					       null, // CPU Objects
					       null, // Highlevel-Scheduler Objects
					       "test_freeze.jll", 
					       null,
					       "test/freeze/ChildDomain",
					       1*1024*1024);
	
	if (child == null) throw new Error("Unable to create child domain\n");
	for(int i=0;i<1000;i++) Thread.yield();
    
	naming.freeze(child);
	naming.melt(child);

	//for(;;) Thread.yield();
	//	createDomains();
	//createDomains2();
	
    }

    static void createDomains() {
	int mem_before = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory before domain started: "+mem_before);

	int n = 20;
	Debug.out.println ("Starting "+n+" domains and terminating them...\n");
	Domain child[] = new Domain[n];
	for(int i=0; i<n; i++) {
	    child[i] = naming.createDomain("TestChild"+i,
					       null, // CPU Objects
					       null, // Highlevel-Scheduler Objects
					       "test_termination.jll", 
					       null,
					       "test/termination/ChildDomain",
					       80*1024);
	    
	    if (child[i] == null) throw new Error("Unable to create child domain\n");
	}
	for(int i=0; i<n; i++) {
	    child[i].destroy();
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
		child[i] = naming.createDomain("TestChild"+i,
						   null, // CPU Objects
						   null, // Highlevel-Scheduler Objects
						   "test_termination.jll", 
						   null,
						   "test/termination/ChildDomain",
						   80*1024);
		
		if (child[i] == null) throw new Error("Unable to create child domain\n");
	    }
	    for(int i=0; i<n; i++) {
		child[i].destroy();
	    }
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
