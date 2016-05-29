package test.minitcp;

import java.net.*;
import java.io.*;
import jx.zero.*;
import jx.zero.debug.*;

class Main {
    public static void main (String[] args) throws Exception {
	new Main(args);
    }

    public Main(String[] args) throws Exception {
	Naming naming = InitialNaming.getInitialNaming();
	String mainLib = "mini_tcp.jll";
	String startClass = "test/minitcp/MiniServerDomain";

	int initialHeap = 1024 * 6;
	int chunkSize = 1024 * 6;
	int gcAt = 1024*40;
	int codeSize = 1024*9;

	Object[] portals = new Object [1];
	String[] argv = new String[0];

	int port  = 80;

	ServerSocket ssock = new ServerSocket(port);
	

	DomainManager domainManager = (DomainManager)naming.lookup("DomainManager");
	MemoryManager memMgr = (MemoryManager)naming.lookup("MemoryManager");
	for(int i=0; i<100; i++)  Thread.yield();
	int start = memMgr.getTotalFreeMemory();
	int ndomains=0;
	try {
	    for(ndomains=0;ndomains<10;ndomains++) {
		final Socket sock = ssock.accept();
		portals[0] = sock;
		DomainStarter.createDomain("D"+ndomains, mainLib, startClass, initialHeap, chunkSize, gcAt, codeSize, 3, portals);
	    }
	} catch(MemoryExhaustedException e) {
	}
	//for(int i=0;i<ndomains*1;i++)       Thread.yield();
	 Thread.yield();
	Debug.out.println ("Domains created: "+ndomains);	    
	int end = memMgr.getTotalFreeMemory();
	Debug.out.println ("end "+end);
	Debug.out.println ("diff: "+(start-end));
	for(;;) Thread.yield();
    }
}
