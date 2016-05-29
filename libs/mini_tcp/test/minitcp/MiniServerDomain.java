package test.minitcp;

import java.net.*;
import java.io.*;
import jx.zero.*;
import jx.zero.debug.*;

class MiniServerDomain {
 
   public static void init(Naming naming, String[] argv, Object[] objs)  throws Exception {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	Socket sock = (Socket) objs[0];
	OutputStream ostream = sock.getOutputStream();
	while(true) {
	    Thread.yield();
	    //ostream.write(42);
	}
    }
}
