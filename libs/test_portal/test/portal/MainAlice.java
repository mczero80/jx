package test.portal;

import jx.zero.*;
import jx.zero.debug.*;

public class MainAlice {
    static final int ncalls = 100000;

    public static void init(Naming naming) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));

	// lookup portal
	Daddy daddy = (Daddy) naming.lookup("Daddy");

	// make call
	for(int i=0; i<100; i++) {
	    daddy.getCount();
	}
	Debug.out.println("Alice completed first stage");

	// make call
	for(int i=0; i<ncalls; i++) {
	    daddy.inc();
	}
	int count = daddy.getCount();
	Debug.out.println("Count as seen by alice = "+count);

	// lookup portal
	HansService hanssvc;
	while((hanssvc = (HansService) naming.lookup("HansService")) == null) Thread.yield();

	// make call
	if (hanssvc != null) hanssvc.test("Alice count is "+count);
	
    }
}
