package test.termination;

import jx.zero.*;
import jx.zero.debug.*;

public class TermIO {
    static MemoryManager memMgr;
    static Naming naming;
    static DomainManager dm;
    public static void main(String []args) {
	naming = InitialNaming.getInitialNaming();
	memMgr = (MemoryManager)naming.lookup("MemoryManager");
	dm = (DomainManager)naming.lookup("DomainManager");

	int mem_before = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory before domain started: "+mem_before);

	Debug.out.println ("Starting test domain ...\n");

	Domain bio = DomainStarter.createDomain("BIO",
					     "test_fs.jll", 
					     "test/fs/BioRAMDomain",
					     2*1024*1024,
						new String[] {"BIOFS"});
	/*
	Domain fs = DomainStarter.createDomain("FS",
					    "test_fs.jll", 
					    "test/fs/FSDomain",
					    3*1024*1024,
					       new String[] {"BIOFS","FS"});

	Domain iozone = DomainStarter.createDomain("IOZONE",
						"test_fs.jll", 
						"test/fs/IOZoneBench",
						8*1024*1024,
						   new String[] {"FS", "4", "512", "4096", "16777216"});
	*/
	Debug.out.println ("Free memory when domain running: "+memMgr.getTotalFreeMemory());

	for(int i=0;i<1000;i++) Thread.yield();

	//dm.terminate(iozone);
	//dm.terminate(fs);
	dm.terminate(bio);

	//	while (! iozone.isTerminated()) Thread.yield();
	//while (! fs.isTerminated()) Thread.yield();
	while (! bio.isTerminated()) Thread.yield();

	int mem_term = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory when domain terminated: "+mem_term);
	Debug.out.println ("    Difference: "+(mem_before - mem_term));

    }
}
