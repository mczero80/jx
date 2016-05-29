/* Test the timeslicing.
 */
package test.portal.timeslicing;

import jx.zero.*;
import jx.zero.debug.*;

public class Simple {
    public static int EMPTY_ITERATIONS = 5000000;
    public static int VCALL_ITERATIONS = 1000000;
    public static int EVENT_ITERATIONS = 100000;
    public static int PORTAL_ITERATIONS = 1500000;
    public static int PORTAL_ONEINT_ITERATIONS = 150000;
    public static int NEW_ITERATIONS = 30000;

    public static void init(Naming naming) {     
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	new Simple();
    }
    
    Simple() {
	new Thread("thr1") {
	    public void run(){
		int i=0;
		while(true) {
		    if (((i++) % 1000)== 0)
			Debug.out.print("t");
		    //Thread.yield();
		}
	    }
	}.start();
	
	new Thread("thr2") {
	    public void run(){
		int i=0;
		while(true) {
		    if (((i++) % 1000)== 0)
			Debug.out.print("m");
    		    //Thread.yield();
		}
	    }
	}.start();

	Debug.out.println("yielding");
	Thread.yield();

	Integer in;
	int i=0;
	
	if (1 == 1)
	    while (true) {
		in = new Integer(42);
		//		if (((i++) % 1000)== 0)
		Debug.out.print("4");
		//Thread.yield();
	    }
	
	Debug.out.println("Success");
	return;
    }
}
