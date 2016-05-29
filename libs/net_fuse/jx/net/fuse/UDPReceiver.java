package jx.net.fuse;

import jx.buffer.separator.MemoryConsumer;
import jx.zero.*;
import jx.zero.debug.*;

import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;
import jx.net.AddressResolution;
import jx.net.IPAddress;
import jx.net.UnknownAddressException;

import jx.buffer.multithread.MultiThreadBufferList;
import jx.buffer.multithread.MultiThreadBufferList2;
import jx.buffer.multithread.Buffer;
import jx.buffer.multithread.Buffer2;


class UDPReceiver implements jx.net.UDPReceiver, DEPImpl {
    PacketsConsumer ipLayer;
    PacketsConsumer udpSender;

    //Memory buf;
    CPUManager cpuManager = (CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager");
    MemoryConsumer consumer;
    int port;
    NetInit net;
    MultiThreadBufferList usableBufs, filledBufs;
  
    public UDPReceiver(NetInit net, int localPort, Memory[] bufs) {
	this.net = net;
	this.usableBufs = new MultiThreadBufferList(bufs);
	this.filledBufs = new MultiThreadBufferList();

	consumer = new MemoryConsumer() {
		public Memory processMemory(Memory buf) {
		    Buffer h = usableBufs.nonblockingUndockFirstElement();
		    if (h == null) {
			Debug.out.println("jx.netmanager.UDPReceiver: no buffer available, must drop packet!");
			return buf;
		    }
		    Memory in = h.getData();
		    h.setData(buf);
		    filledBufs.appendElement(h);
		    return in;
		}
	    };
	port = localPort;
		
	net.udp.registerConsumer(consumer, localPort);
    }

    public UDPReceiver(NetInit net, int localPort, Memory[] bufs, boolean xxxx) {
	this.net = net;
	this.usableBufs = new MultiThreadBufferList2(bufs);
	this.filledBufs = new MultiThreadBufferList2();

	consumer = new MemoryConsumer() {
		public Memory processMemory(Memory buf) {
		    Buffer h = usableBufs.nonblockingUndockFirstElement();
		    if (h == null) {
			Debug.out.println("jx.netmanager.UDPReceiver: no buffer available, must drop packet!");
			return buf;
		    }
		    Memory in = h.getData();
		    h.setData(buf);
		    Debug.out.println("APPEND TO FILLED");
		    filledBufs.appendElement(h);
		    return in;
		}
	    };
	port = localPort;
		
	net.udp.registerConsumer(consumer, localPort);
    }


    public Memory receive(Memory buf, int timeoutMillis) {
      return receive(buf); // TODO
    }

    public Memory receive(Memory buf) {
	Buffer h = filledBufs.undockFirstElement();
	Memory result = h.getData();
	h.setData(buf.extendAndRevoke());
	usableBufs.appendElement(h);
	return result;
    }

    public IPAddress getSource(Memory buf) {
	return net.udp.getSource(buf);
    }

    public int getSourcePort(Memory buf) {
	return net.udp.getSourcePort(buf);
    }

    public void close() {
	net.udp.unregisterConsumer(consumer, port);
    }

}
