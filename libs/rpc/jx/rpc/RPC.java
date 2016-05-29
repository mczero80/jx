/*
 * RFC 1057
 */
package jx.rpc;

import jx.zero.*;
import jx.zero.debug.*;
import java.util.Hashtable;
import java.util.Vector;
import jx.net.IPAddress;
import jx.net.NetInit;
import jx.net.UDPReceiver;
import jx.net.UDPSender;
import jx.net.UDPData;

public class RPC implements Runnable,ThreadEntry {
  public static final int IPPROTO_TCP = 6;      /* protocol number for TCP/IP */
  public static final int IPPROTO_UDP = 17;     

   private UDPReceiver receiver;
    private short receivePort=-1;
    //private DatagramSocket socket;
    private MemoryManager memMgr;
    NetInit net;

    private Hashtable waiters = new Hashtable();
    private Hashtable buffers = new Hashtable();

    public boolean beforeReceive = false; // TODO: remove this one

    boolean debug = false;
    private final static boolean debugPacketNotice = false;
    private final static boolean debugPacketDump = false;

    /* accept status */
    private static final int SUCCESS       = 0; /* RPC executed successfully             */
    private static final int PROG_UNAVAIL  = 1; /* remote hasn't exported program        */
    private static final int PROG_MISMATCH = 2; /* remote can't support version #        */
    private static final int PROC_UNAVAIL  = 3; /* program can't support procedure       */
    private static final int GARBAGE_ARGS  = 4; /* procedure can't decode params         */
    private static final int SYSTEM_ERR    = 5;  /* errors like memory allocation failure */
  
    private static int xid = 10;

    final CPUManager cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");

    final static int NUMBER_CACHED_UDP_ENDPOINTS = 20;
    UDPSender[] senders = new UDPSender[NUMBER_CACHED_UDP_ENDPOINTS];

    private int event_reply;
    private int event_rcv;

    MemoryManager memoryManager = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");

    public RPC(NetInit net, int port) throws RPCInitException {
	this.net = net;
	event_reply = cpuManager.createNewEvent("RPCReply");
	event_rcv = cpuManager.createNewEvent("RPCRcv");
	init(false,port);
    }

    private void init(boolean privileged, int port)  {
	memMgr = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");
	
	receivePort = (short)port;
	cpuManager.start(cpuManager.createCPUState(this));
    }

    /* uses split/join     
    public RPCBuffer getNewBuffer() {
	Memory m = net.getUDPBuffer();
	Memory m0 = m.joinNext();
	if (m0!=null) m = m0;
	return new RPCBuffer(m);
    }
    */

    public RPCBuffer getNewBuffer() {
	Memory m = memoryManager.alloc(1514);
	return new RPCBuffer(m,14+20+8);
    }
    


  /** 
   * Create a buffer that represents the RPC DATA contained in buf.
   * Skips headers!
   */
    public RPCBuffer initFrom(Memory buf, RPCContinuation co) {
      Debug.out.println("rpc initFrom");
      RPCBuffer rpcbuf = new RPCBuffer(buf, co);
      RPCMessage replyMessage = RPCFormatterRPCMessage.read(rpcbuf);
      if (! ( replyMessage instanceof RPCMsgSuccess)) {
	throw new Error();
      }
      return rpcbuf;
    }

    public RPCContinuation call(IPAddress rpcHost, int dstPort, int prog, int version, int proc, RPCBuffer buf, Auth a, Auth c) throws RPCException {
	try {
	    while(! beforeReceive) {  // wait until receiver is ready; TODO: change this
		cpuManager.yield();
	    }
	    int xid = callOnly(rpcHost, dstPort, prog,version, proc, buf, a, c);
	    return waitForReply(xid);
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new RPCException(e.toString());
	}
    }
  
    public int callOnly(IPAddress rpcHost, int dstPort, int prog, int version, int proc, RPCBuffer data, Auth a, Auth c)  {
	RPCBuffer buf = new RPCBuffer(net.getUDPBuffer());
	RPCFormatterRPCMessage.write(buf, new RPCCall(xid,prog,version,proc,a,c));
	if (debug) {
	    Debug.out.println("rpc header:");
	    buf.xdump();
	    Debug.out.println("rpc data:");
	    data.xdump();
	}
	buf.append(data);
	if (debug) {
	    Debug.out.println("size: "+buf.size());
	}
	try {
	    Memory b = net.getUDPBuffer(buf.size());
	    buf.copyInto(b);
	    UDPSender sender = net.getUDPSender(receivePort, rpcHost, dstPort);
	    if (debugPacketNotice) Debug.out.println("RPC.callsend: "+b.size());
	    sender.send(b);
	    sender.close();
	} catch(Exception e) {
	    Debug.out.println(e);
	}
	return xid++;
    }  

    public RPCContinuation waitForReply(int xid) {
	Waiter waiter;
	RPCContinuation c=new RPCContinuation();;
	if (debug) Debug.out.println("wait for xid="+xid);
	Integer id = new Integer(xid);
	
	Vector b = (Vector)buffers.get(id);
	if (b != null && b.size() > 0) {
	    Memory buf = (Memory)b.elementAt(0);
	    b.removeElementAt(0);
	    c.buf = buf;
	    return c;
	}
	waiter = new Waiter(this, xid, null);
	waiters.put(new Integer(xid), waiter);
	if (waiter.buf == null) { // if buf != null then receiver just received packet for us
	    if (debug) Debug.out.println("blocking in waitForReply");
	    
	    
	    waiter.doWait();
	}
	if (debug) {
	    Debug.out.println("inwait:");
	    Dump.xdump(waiter.buf,waiter.buf.size());
	}
	c.buf = waiter.buf;
	waiter.buf = null;
	c.waiter = waiter;
	c.rpc = this;
	return c;
    }

    public void waitForNextReply(RPCContinuation c) {
	/*
	synchronized (c.waiter) {
	    if (c.waiter.buf == null) 
		try { c.waiter.wait(); } catch(InterruptedException e) {}
	    c.buf = c.waiter.buf;
	    c.waiter.buf = null;
	}
	*/ throw new Error();
    }


    
    public void run() {
      cpuManager.setThreadName("RPC-Receiver");
	try {
	    if (debug) Debug.out.println("RPC receiver started.");
	    Memory buf = net.getUDPBuffer();
		Memory[] bufs = new Memory[10]; 
		for(int i=0; i<10; i++) {
		    bufs[i] = net.getUDPBuffer(0);
		}
	    receiver = net.getUDPReceiver(receivePort, bufs);
	waiting:
	    for(;;) {
		beforeReceive = true; // TODO: remove this hack and configure scheduler
		UDPData udp = receiver.receive1(buf);

		RPCBuffer rpcbuf = new RPCBuffer(udp, udp.offset, udp.size);
		if (debugPacketNotice) Debug.out.println("RPC.loopreceive: "+udp.size);
		RPCMessage replyMessage = RPCFormatterRPCMessage.read(rpcbuf);
		if (! ( replyMessage instanceof RPCMsgSuccess)) {
		    Debug.out.println("RPC: ignore:" + replyMessage);
		    continue waiting;
		}
		RPCMsgSuccess reply = (RPCMsgSuccess)replyMessage;
		Integer xid = new Integer(reply.xid);
		if (debug) Debug.out.println("RPC: received xid="+xid);
		Waiter w = null;
		w = (Waiter) waiters.get(xid);
		if (w == null) {
		    if (debug) Debug.out.println("RPC: No one is interested in this packet. xid="+xid);
		    if (debug) Debug.out.println("     Keeping it in buffer.");
		    Vector b = (Vector)buffers.get(xid);
		    if (b == null) {
			b = new Vector();
			buffers.put(xid,b);
		    }
		    //byte[] bb = new byte[repl.getLength()-rpcbuf.offset];
		    //System.arraycopy(rpcbuf.buf, rpcbuf.offset, bb, 0, bb.length);
		    //b.addElement(bb);
		    // TODO: add received packet to waiters
		    
		    b.addElement(rpcbuf);
		}
		if (w==null)
		    continue waiting;
		if (debug) {
		    //Debug.out.println("RPC: received reply for xid="+xid);
		    //rpcbuf.xdump();
		}
		
		if (w.buf==null) {
		    if (debug) Debug.out.println("new buffer");
		    w.buf = net.getUDPBuffer(rpcbuf.buf.size());
		}
		w.buf.copyFromMemory(rpcbuf.buf, 0, 0, rpcbuf.buf.size());
		//waiters.remove(xid);
		if (debug) Debug.out.println("notify waiter");
		
		
		w.unblock();
	    }
	} catch(Exception e) {
	    e.printStackTrace();
	    Debug.out.println(e);
	}
    }

    /* receive using join/split
    public void run() {
      cpuManager.setThreadName("RPC-Receiver");
	try {
	    if (debug) Debug.out.println("RPC receiver started.");
	    Memory buf = net.getUDPBuffer();
		Memory[] bufs = new Memory[10]; 
		for(int i=0; i<10; i++) {
		    bufs[i] = net.getUDPBuffer(0);
		}
	    receiver = net.getUDPReceiver(receivePort, bufs);
	waiting:
	    for(;;) {
		beforeReceive = true; // TODO: remove this hack and configure scheduler
		UDPData udp = receiver.receive(buf);

		RPCBuffer rpcbuf = new RPCBuffer(udp);
		if (debugPacketNotice) Debug.out.println("RPC.loopreceive: "+udp.mem.size());
		if (debug) Debug.out.println("RPC packet received. Size="+buf.size());
		RPCMessage replyMessage = RPCFormatterRPCMessage.read(rpcbuf);
		if (! ( replyMessage instanceof RPCMsgSuccess)) {
		    Debug.out.println("RPC: ignore:" + replyMessage);
		    continue waiting;
		}
		RPCMsgSuccess reply = (RPCMsgSuccess)replyMessage;
		Integer xid = new Integer(reply.xid);
		if (debug) Debug.out.println("RPC: received xid="+xid);
		Waiter w = null;
		w = (Waiter) waiters.get(xid);
		if (w == null) {
		    if (debug) Debug.out.println("RPC: No one is interested in this packet. xid="+xid);
		    if (debug) Debug.out.println("     Keeping it in buffer.");
		    Vector b = (Vector)buffers.get(xid);
		    if (b == null) {
			b = new Vector();
			buffers.put(xid,b);
		    }
		    //byte[] bb = new byte[repl.getLength()-rpcbuf.offset];
		    //System.arraycopy(rpcbuf.buf, rpcbuf.offset, bb, 0, bb.length);
		    //b.addElement(bb);
		    // TODO: add received packet to waiters
		    
		    b.addElement(rpcbuf);
		}
		if (w==null)
		    continue waiting;
		if (debug) {
		    //Debug.out.println("RPC: received reply for xid="+xid);
		    //rpcbuf.xdump();
		}
		
		if (w.buf==null) {
		    if (debug) Debug.out.println("new buffer");
		    w.buf = net.getUDPBuffer(rpcbuf.buf.size());
		}
		w.buf.copyFromMemory(rpcbuf.buf, 0, 0, rpcbuf.buf.size());
		//waiters.remove(xid);
		if (debug) Debug.out.println("notify waiter");
		
		
		w.unblock();
	    }
	} catch(Exception e) {
	    e.printStackTrace();
	    Debug.out.println(e);
	}
    }
*/
    public IPAddress getLocalAddress() {
	return net.getLocalAddress();
    }

    
    // server part
    public UDPReceiver createReceiver(int port) {
	Memory[] bufs = new Memory[10]; 
	for(int i=0; i<10; i++) {
	    bufs[i] = net.getUDPBuffer(0);
	}
	return net.getUDPReceiver(port, bufs);
    }

    /* receive using split 
    public RPCBuffer receive(UDPReceiver receiveSocket, RPCBuffer inbuf) {
	UDPData buf = receiveSocket.receive(inbuf.getMemory());
	cpuManager.recordEvent(event_rcv);	
	if (debugPacketNotice) Debug.out.println("RPC.receive: "+buf.mem.size());
	if (debugPacketDump) {
	    Debug.out.println("RECEIVED REQUEST PACKET:");
	    Dump.xdump1(buf.mem, 0, 128);
	}

	inbuf.init(buf);
	RPCBuffer rpcbuf = inbuf;//new RPCBuffer(buf);
	if (debugPacketDump){
	    Debug.out.println("RECEIVED RPC PACKET:");
	    rpcbuf.xdump();
	}
	//Debug.out.println("   SOURCE address: "+rpcbuf.sourceAddress+", port="+rpcbuf.sourcePort);
	return rpcbuf;
    }
    */
    public RPCBuffer receive(UDPReceiver receiveSocket, RPCBuffer inbuf) {
	UDPData buf = receiveSocket.receive1(inbuf.getMemory());
	cpuManager.recordEvent(event_rcv);	
	inbuf.init(buf, buf.offset, buf.size);

	if (debugPacketNotice) Debug.out.println("RPC.receive: "+buf.size);
	if (debugPacketDump) {
	    Debug.out.println("RECEIVED REQUEST PACKET: offset="+buf.offset+", size="+buf.size);
	    Dump.xdump1(buf.mem, buf.offset, buf.size);
	    //Debug.out.println(" xid="+jx.xdr.Format.readInt(inbuf)); // readInt advances the offset pointer!
	    //Debug.out.println(" type="+jx.xdr.Format.readInt(inbuf));
	}
	return inbuf;
    }

    /* replyOnly using split
    public RPCBuffer replyOnly(int localPort, IPAddress rpcHost, int dstPort, RPCBuffer buf)  {
	cpuManager.recordEvent(event_reply);	
	//Memory b = net.getUDPBuffer(buf.size());
	//buf.copyInto(b);

	if (debugPacketDump){
	    Debug.out.println("SEND RPC REPLY PACKET:");
	    Dump.xdump1(buf.buf, 0, buf.offset);
	}
	Memory m=null;
	try {
	    UDPSender sender = findUDPSender(localPort, rpcHost, dstPort);
	    if (debugPacketNotice) Debug.out.println("RPC.replyOnlysend: "+buf.buf.size() +", "+ buf.offset);
	    //m = sender.send(buf.buf);
	    
	    Memory arr[] = new Memory[2];
	    buf.buf.split2(buf.offset, arr);
	    
	    m = sender.send(arr[0]);
	    
	    Memory m0 = m.joinNext();
	    if (m0!=null) m = m0;

	} catch(Exception e) {throw new Error(); }
	buf.init(m);
	return buf;
    }  
    */
    public RPCBuffer replyOnly(int localPort, IPAddress rpcHost, int dstPort, RPCBuffer buf)  {
	cpuManager.recordEvent(event_reply);	
	//Memory b = net.getUDPBuffer(buf.size());
	//buf.copyInto(b);

	if (debugPacketDump){
	    Debug.out.println("SEND RPC REPLY PACKET:");
	    Dump.xdump1(buf.buf, 14+20+8,buf.offset-(14+20+8));
	}
	Memory m=null;
	try {
	    UDPSender sender = findUDPSender(localPort, rpcHost, dstPort);
	    if (debugPacketNotice) Debug.out.println("RPC.replyOnlysend: "+buf.buf.size() +", "+ buf.offset);
	    m = sender.send1(buf.buf, 14+20+8,buf.offset-(14+20+8)); // FIXME
	} catch(Exception e) {throw new Error(); }
	buf.init(m);
	return buf;
    }  

    public Memory getUDPBuffer(int size) {
	return net.getUDPBuffer(0);
    }

    private UDPSender findUDPSender(int localPort, IPAddress rpcHost, int dstPort) throws jx.net.UnknownAddressException {
	for(int i=0; i<senders.length; i++) {
	    UDPSender s = senders[i];
	    if (s==null) continue;
	    if (s.getLocalPort() == localPort && s.getRemotePort() == dstPort && s.getDestination() == rpcHost) {
		return s;
	    }
	}
	UDPSender sender = net.getUDPSender(localPort, rpcHost, dstPort);
	for(int i=0; i<senders.length; i++) {
	    if (senders[i] == null) {
		senders[i] = sender;
	    }
	}	
	return sender;
    }


}
