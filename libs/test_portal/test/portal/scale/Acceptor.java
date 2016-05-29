package test.portal.scale;

import jx.zero.*;
import jx.zero.debug.*;
import test.portal.*;
import java.io.*;

class MyConnection implements Connection {
    char [] c = { 'H', 'A', 'L', 'L', 'O' };
    int i;
    public int read() throws IOException {
	if (i < c.length) return c[i++];
	return -1;
    }
    public void write(int c) throws IOException {
    }
}

class MyNaming implements Naming {
    Naming baseNaming;
    MyNaming(Naming baseNaming) {
	this.baseNaming = baseNaming;
    }
    public void registerPortal(Portal portal, String name) {
	//Debug.out.println("Register: "+name);
	baseNaming.registerPortal(portal, name);
    }
    public Portal lookup(String name) {
	//Debug.out.println("Lookup: "+name);
	return baseNaming.lookup(name);
    }
    public Portal lookupOrWait(String depName) {throw new Error();}
}

public class Acceptor {

    public static int N_DOMAINS;
    public static boolean verbose = false;

    public static void init(Naming naming, String[] args) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	Acceptor.main(args);
    }

    public static void main(String[] args) {
	Naming naming = InitialNaming.getInitialNaming();
	N_DOMAINS = Integer.parseInt(args[0]);
	//naming.registerPortal(c, "Connection");
	String domainName = "Servlet";
	String mainLib = "test_portal.jll";
	String startClass = "test/portal/scale/ServletDomain";
	String schedulerClass = null;
	int heapSize = 80000;
	int codeSize = 30000;
	String[] argv = new String[0];
	DomainManager domainManager = (DomainManager)naming.lookup("DomainManager");
	MemoryManager memMgr = (MemoryManager)naming.lookup("MemoryManager");
	for(int i=0; i<100; i++)  Thread.yield();
	int start = memMgr.getTotalFreeMemory();
	Domain domain[] = new Domain[N_DOMAINS];
	Naming clientNaming = new MyNaming(naming);
	int ndomains=0;
	Clock clock = (Clock) naming.lookup("Clock");
	int startTime = clock.getTimeInMillis();
	try {
	    for(; ndomains<N_DOMAINS; ndomains++) {
		if (verbose) Debug.out.println (ndomains+" "+memMgr.getTotalFreeMemory());
		Connection c = new MyConnection();
		Portal[] portals = new Portal [] { c };
		domain[ndomains] = DomainStarter.createDomain(domainName+ndomains, mainLib, startClass, schedulerClass, heapSize, codeSize, argv, clientNaming, portals);
	    }
	} catch(MemoryExhaustedException e) {
	    Debug.out.println ("Domains created: "+ndomains);	    
	}
	for(int i=0; i<ndomains; i++) {
	    while (! domain[i].isTerminated()) Thread.yield();
	}
	//for(int i=0; i<1000; i++) Thread.yield();
	int endTime = clock.getTimeInMillis();
	Debug.out.println("Time to create "+ndomains+" domains (millisec): "+((endTime-startTime)));
	Domain curdomain = domainManager.getCurrentDomain();
	domainManager.gc(curdomain);
	//for(int i=0; i<1000; i++) Thread.yield();
	int end = memMgr.getTotalFreeMemory();
	Debug.out.println ("end "+end);
	Debug.out.println ("diff: "+(start-end));
    }
}
