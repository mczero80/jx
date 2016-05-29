package test.freeze;

import jx.zero.*;
import jx.zero.debug.*;

public class TermIO {
    static MemoryManager memMgr;
    static DomainZero domainZero;
    public static void init (final DomainZero domainZero /*String []args*/) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) domainZero.lookup("DebugChannel0")));
	Main.domainZero = domainZero;
	memMgr = (MemoryManager)domainZero.lookup("MemoryManager");

	int mem_before = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory before domain started: "+mem_before);

	Debug.out.println ("Starting test domain ...\n");

	Domain bio = domainZero.createDomain("BIO",
					     null, // CPU Objects
					       null, // Highlevel-Scheduler Objects
					     "test_fs.jll", 
					     null,
					     "test/fs/BioRAMDomain",
					     2*1024*1024);

	Domain fs = domainZero.createDomain("FS",
					    null, // CPU Objects
					    null, // Highlevel-Scheduler Objects
					    "test_fs.jll", 
					    null,
					    "test/fs/FSDomain",
					    3*1024*1024);

	Domain iozone = domainZero.createDomain("IOZONE",
						null, // CPU Objects
						null, // Highlevel-Scheduler Objects
						"test_fs.jll", 
						null,
						"test/fs/IOZoneTest",
						8*1024*1024);
    
	Debug.out.println ("Free memory when domain running: "+memMgr.getTotalFreeMemory());

	for(int i=0;i<1000;i++) Thread.yield();

	iozone.destroy();
	fs.destroy();
	bio.destroy();

	int mem_term = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory when domain terminated: "+mem_term);
	Debug.out.println ("    Difference: "+(mem_before - mem_term));

    }
}
