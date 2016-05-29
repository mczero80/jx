package test.portal;

import jx.zero.*;
import jx.zero.debug.*;


public class MainHans {
    static final int ncalls = 100000;

    public static void init(Naming naming, String[] args) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));

	// create a thread that allocates memory forcing a GC
	if (args.length > 0 && args[0].equals("gc")) {
	    new Thread() {
		    public void run() {
			for(;;) {
			    int[] a = new int[100000];
			    Thread.yield();
			}
		    }
		}.start();
	}

	// lookup portal
	Daddy daddy = (Daddy) naming.lookup("Daddy");

	// make call
	for(int i=0; i<100; i++) {
	    daddy.getCount();
	}
	Debug.out.println("Hans completed first stage");

	// make portal thread busy for a certain time to test request queuing
	//daddy.longRunning();

	// make call
	for(int i=0; i<ncalls; i++) {
	    daddy.inc();
	}
	
	Debug.out.println("Count as seen by hans = "+daddy.getCount());

	// create portal
	HansServiceImpl svc = new HansServiceImpl(daddy);
	naming.registerPortal(svc, "HansService");
    }
}
