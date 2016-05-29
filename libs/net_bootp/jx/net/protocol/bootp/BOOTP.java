package jx.net.protocol.bootp;

import jx.zero.*;
import jx.zero.debug.*;
import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;
import jx.buffer.separator.MemoryConsumer;

//import jx.net.protocol.ether.*;
import jx.net.IPAddress;
import jx.net.NetInit;
import jx.net.UDPSender;
import jx.net.UDPReceiver;
import jx.net.UDPData;

public class BOOTP  {
    MemoryManager memMgr;
    CPUManager cpuManager;
    int xid = 1;

    final static int CLIENT_PORT = 68;
    final static int SERVER_PORT = 67;
    byte myHWaddr[];

    UDPSender sender;
    UDPReceiver receiver;
    Memory buf;
    int event_snd,event_rec;
    NetInit net;
 
    public BOOTP(NetInit net, byte[] hwaddr) {
	this.myHWaddr = hwaddr;
	memMgr = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");
	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	event_snd = cpuManager.createNewEvent("BOOTPsnd");
	event_rec = cpuManager.createNewEvent("BOOTPrec");
 	this.net = net;
    }      
  
    public IPAddress sendRequest() {
      Memory rbuf2=null;
      UDPData udp=null;
      Memory arr[] = new Memory[2];
	try { 
	    sender = net.getUDPSender(CLIENT_PORT, new IPAddress(255,255,255,255), SERVER_PORT);
	    receiver = net.getUDPReceiver(CLIENT_PORT, new Memory[] { 
			    net.getUDPBuffer(0), net.getUDPBuffer(0), net.getUDPBuffer(0), net.getUDPBuffer(0)});
	    buf = net.getUDPBuffer(300);
	} catch(jx.net.UnknownAddressException ex) { throw new Error("broadcast ip address unknown"); }
      do {
	  buf.split2(BOOTPFormat.requiresSpace(), arr);
	  buf = arr[0];
	  BOOTPFormat bootp = new BOOTPFormat(buf);
	  bootp.insertOp(BOOTPFormat.REQUEST);
	  bootp.insertHtype((byte)1);
	  bootp.insertHlen((byte)6); // ether address size
	  bootp.insertXid(xid++);
	  bootp.insertHwaddr(myHWaddr);	
	  Debug.out.println("SENDING BOOTP REQUEST...");
	  cpuManager.recordEvent(event_snd);
	  
	  buf = sender.send(buf);	
	  Memory bufx = buf.revoke();
	  //cpuManager.dump("UDP.send returned:", buf);
	  if (! bufx.isValid()) throw new Error("????notvalid???");
	  udp = receiver.receive(bufx, 200);
	  //udp = receiver.receive(bufx);
	  if (udp == null) {
	      if (! bufx.isValid()) throw new Error("????notvalid???");
	      bufx = bufx.revoke();
	      buf = bufx;
	      if (! bufx.isValid()) throw new Error("????notvalid???");
	  }
	  cpuManager.recordEvent(event_rec);
	  if (udp == null) { Debug.out.println("Timout waiting for bootp reply"); }
      } while (udp == null);
      BOOTPFormat b = new BOOTPFormat(udp.mem);
      return b.getYiaddr();
    }

    public IPAddress sendRequest1() {
      Memory rbuf2=null;
      UDPData udp=null;
	try { 
	    sender = net.getUDPSender(CLIENT_PORT, new IPAddress(255,255,255,255), SERVER_PORT);
	    receiver = net.getUDPReceiver(CLIENT_PORT, new Memory[] { 
			    net.getUDPBuffer1(), net.getUDPBuffer1(), net.getUDPBuffer1(), net.getUDPBuffer1()});
	    buf = net.getUDPBuffer1();
	} catch(jx.net.UnknownAddressException ex) { throw new Error("broadcast ip address unknown"); }
      do {
	  BOOTPFormat bootp = new BOOTPFormat(buf, 14+20+8);
	  bootp.insertOp(BOOTPFormat.REQUEST);
	  bootp.insertHtype((byte)1);
	  bootp.insertHlen((byte)6); // ether address size
	  bootp.insertXid(xid++);
	  bootp.insertHwaddr(myHWaddr);	
	  Debug.out.println("SENDING1 BOOTP REQUEST...");
	  cpuManager.recordEvent(event_snd);
	  
	  buf = sender.send1(buf, 14+20+8, BOOTPFormat.requiresSpace());	

	  udp = receiver.receive1(buf, 200);
	  cpuManager.recordEvent(event_rec);

	  if (udp == null) {
	      Debug.out.println("Timout waiting for bootp reply"); 
	  }
      } while (udp == null);
      BOOTPFormat b = new BOOTPFormat(udp.mem, udp.offset);
      return b.getYiaddr();
    }
  

   
}
