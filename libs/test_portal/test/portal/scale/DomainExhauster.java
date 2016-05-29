package test.portal.scale;

import jx.zero.*;
import jx.zero.debug.*;
import test.portal.*;
import java.io.*;

public class DomainExhauster {

    public static int N_DOMAINS;
    public static boolean verbose = false;


    public static void main(String[] args) {
	Naming naming = InitialNaming.getInitialNaming();
	String mainLib = "mini_domain.jll";
	String startClass = "test/portal/scale/MiniDomain";
	String schedulerClass = null;

	int initialHeap = 2048;
	int chunkSize = 2048;
	int gcAt = 1024*40;
	int codeSize = 1024*9;

	int domains = 1000;
	
	DomainManager domainManager = (DomainManager)naming.lookup("DomainManager");
	MemoryManager memMgr = (MemoryManager)naming.lookup("MemoryManager");
	for(int i=0; i<100; i++)  Thread.yield();
	if (args.length>2) {
		domains     = Integer.parseInt(args[0]);
		chunkSize   = Integer.parseInt(args[1]);
		initialHeap = chunkSize; 
	        gcAt        = Integer.parseInt(args[2]) * chunkSize;	
	}
	int start = memMgr.getTotalFreeMemory();
	int ndomains=0;
	try {
	    for(ndomains=0;ndomains<domains;ndomains++) {
		DomainStarter.createDomain("D"+ndomains, mainLib, startClass, initialHeap, chunkSize, gcAt, codeSize, 3);
	    }
	} catch(MemoryExhaustedException e) {
	}
	//for(int i=0;i<ndomains*1;i++)       Thread.yield();
	 Thread.yield();
	Debug.out.println ("Domains created: "+ndomains);	    
	int end = memMgr.getTotalFreeMemory();
	Debug.out.println ("end "+end);
	Debug.out.println ("diff: "+(start-end));
	Debug.out.println ("diff: "+((start-end)/1024)+" kbyte");
	Debug.out.println ("chunk: "+chunkSize);
	Debug.out.println ("gc: "+gcAt);
    }
}
