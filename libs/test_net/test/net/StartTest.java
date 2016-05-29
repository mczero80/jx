package test.net;

import jx.zero.*;

import jx.net.NetInit;
import jx.net.UDPSender;
import jx.net.UDPReceiver;
import jx.net.IPAddress;

class StartTest {
    
    public static void main(String[] args) {
	Naming naming = InitialNaming.getInitialNaming();
	NetInit net = (NetInit)LookupHelper.waitUntilPortalAvailable(naming, args[0]);
	MemoryManager memoryManager = (MemoryManager)LookupHelper.waitUntilPortalAvailable(naming, "MemoryManager");

	try{
	    // send
	    UDPSender u = net.getUDPSender(6665, new IPAddress("192.168.34.2"), Integer.parseInt("9876"));
	    //Memory buf = net.getUDPBuffer(50);
	    Memory buf = memoryManager.alloc(1000);
	    for(;;) {
		int offset=14+20+8;
		int size = 50;
		for(int j=0; j<10; j++) {
		    for(int i=0; i<size; i++) {
			buf.set8(i+offset, (byte)(i));
		    }
		    //buf=u.send(buf);
		    buf=u.send1(buf, offset, size);
		}
		for(int k=0; k<10; k++) Thread.yield();
	    }
	} catch(Exception e) {throw new Error();}
    }
}
