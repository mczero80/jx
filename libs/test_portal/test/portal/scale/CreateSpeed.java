package test.portal.scale;

import jx.zero.*;
import jx.zero.debug.*;
import test.portal.*;
import java.io.*;

public class CreateSpeed {

    public static int N_DOMAINS;
    public static boolean verbose = false;


    public static void main(String[] args) {
	Naming naming = InitialNaming.getInitialNaming();

	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	CycleTime starttimec = new CycleTime();
	CycleTime endtimec = new CycleTime();
	CycleTime diff = new CycleTime();

	String mainLib = "mini_domain.jll";
	String startClass = "test/portal/scale/MiniDomainSilent";
	String schedulerClass = null;

	int initialHeap = 100000;
	//int initialHeap = 2048;
	int chunkSize = 2048;
	int gcAt = 1024*40;
	int codeSize = 1024*9;

	int domains = 100;
	
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
	int ndomains=100;
	try {
	    clock.getCycles(starttimec);
	    for(ndomains=0;ndomains<domains;ndomains++) {
		DomainStarter.createDomain("D"+ndomains, mainLib, startClass, initialHeap, codeSize);
		//DomainStarter.createDomain("D"+ndomains, mainLib, startClass, initialHeap, -1, -1, codeSize, 1);
	    }
	} catch(MemoryExhaustedException e) {
	}

	Thread.yield();

	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	Debug.out.println("time to create and activate "+ndomains+"="+(clock.toMicroSec(diff))+" microseconds");
    }
}
