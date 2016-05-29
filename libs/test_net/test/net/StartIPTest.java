package test.net;

import jx.zero.*;

import jx.net.NetInit;
import jx.net.IPData;
import jx.net.IPSender;
import jx.net.IPReceiver;
import jx.net.IPAddress;
import jx.zero.debug.Dump;

class StartIPTest {
    
    public static void main(String[] args) {
	Naming naming = InitialNaming.getInitialNaming();
	NetInit net = (NetInit)LookupHelper.waitUntilPortalAvailable(naming, args[0]);
	MemoryManager memoryManager = (MemoryManager)LookupHelper.waitUntilPortalAvailable(naming, "MemoryManager");

	try{
	    // send
	    IPSender u = net.getIPSender(new IPAddress("192.168.34.2"), 42);
	    //Memory buf = net.getIPBuffer(50);
	    Memory buf = memoryManager.alloc(1000);
	    for(int i=0; i<50; i++) {
		buf.set8(i, (byte)i);
	    }
	    u.send1(buf, 14+20, 50);


	    Memory[] bufs = new Memory[10];
	    for(int i=0; i<bufs.length; i++) {
		bufs[i] = net.getIPBuffer(0);
	    }
	    IPReceiver iprec = net.getIPReceiver(bufs, "TCP");
	    
	    for(;1==1;) {
		Memory buf1 = net.getIPBuffer(0);
		IPData ipdata = iprec.receive(buf1);
		Memory recbuf = ipdata.mem;
		Debug.out.println("Received IP packet of size "+ipdata.size);
		Dump.xdump1(recbuf, ipdata.offset, ipdata.size);
		
	    }
	} catch(Exception e) {throw new Error();}
    }
}
