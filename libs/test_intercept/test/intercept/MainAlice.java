package test.intercept;

import jx.zero.*;
import jx.zero.debug.*;

public class MainAlice {
    public static void init(Naming naming) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	DebugPrintStream out = new DebugPrintStream(new DebugOutputStream(d));
	Debug.out = out;

	// lookup portal
	Daddy daddy = (Daddy) naming.lookup("Daddy");

	// make call
	daddy.hello();

	// lookup portal
	HansService hanssvc = (HansService) naming.lookup("HansService");

	PortalTestObj o = new PortalTestObj();
	o.daddy=daddy;
	o.hans = hanssvc;
	daddy.testPortalParam(o);

	for(int i=0; i<1000;i++) Thread.yield();

	// make call
	try {
	    hanssvc.test("I tell you a secret!");
	    hanssvc.test2("string", 43, new TestObject(22, 23));
	} catch (Exception e) {
	    Debug.out.println("Alice: portal call to Hans failed");
	}
    }
}

class TestObject {
    int i;
    int j;
    String s="hallo";
    public TestObject (int i, int j) {
	this.i = i ; 
	this.j = j ; 
    }
}

class PortalTestObj {
    Daddy daddy;
    HansService hans;
}
