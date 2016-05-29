package test;

import jx.zero.*;
import jx.zero.debug.*;

public class HelloWorld {
    public static void init(Naming naming) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	DebugPrintStream out = new DebugPrintStream(new DebugOutputStream(d));
	Debug.out = out;

	Debug.out.println("Hello World!");
    }
}
