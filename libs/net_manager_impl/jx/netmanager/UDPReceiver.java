package jx.netmanager;

import jx.buffer.separator.MemoryConsumer;
import jx.zero.*;
import jx.zero.debug.*;

import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;
import jx.net.AddressResolution;
import jx.net.IPAddress;
import jx.net.UnknownAddressException;
import jx.net.UDPData;
import jx.net.UDPConsumer;
import jx.net.UDPConsumer1;

import jx.buffer.multithread.MultiThreadBufferList;
import jx.buffer.multithread.MultiThreadBufferList2;
import jx.buffer.multithread.Buffer;
import jx.buffer.multithread.Buffer2;



class UDPReceiver implements jx.net.UDPReceiver, Service {
    PacketsConsumer ipLayer;
    PacketsConsumer udpSender;

    //Memory buf;
    CPUManager cpuManager = (CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager");
    UDPConsumer consumer;
    UDPConsumer1 consumer1;
    int port;
    NetInit net;
    private MultiThreadBufferList usableBufs, filledBufs;
  
    public UDPReceiver(NetInit net, int localPort, Memory[] bufs, boolean avoidSplitting) {
	this.net = net;
	this.usableBufs = new MultiThreadBufferList(bufs);
	this.usableBufs.setListName("UDP-available-queue");
	this.usableBufs.enableRecording("UDP-available-queue");
	this.filledBufs = new MultiThreadBufferList();
	this.filledBufs.setListName("UDP-receive-queue");
	this.filledBufs.enableRecording("UDP-receive-queue");
	
	//filledBufs.setVerbose(true);
	filledBufs.requireMoredata(true);

	if (avoidSplitting) {
	    consumer1 = new UDPConsumer1() {
		    public Memory processUDP1(UDPData buf) {
			Buffer h = usableBufs.nonblockingUndockFirstElement();
			if (h == null) {
			    Debug.out.println("jx.netmanager.UDPReceiver: no buffer available, must drop packet!");
			    return buf.mem;
			}
			Memory in = h.getData();
			//in = in.revoke();
			h.setData(buf.mem);
			h.setMoreData(buf);
			filledBufs.appendElement(h);
			return in;
		    }
		};
	    net.udp.registerUDPConsumer1(consumer1, localPort);
	} else {
	    consumer = new UDPConsumer() {
		    public Memory processUDP(UDPData buf) {
			Buffer h = usableBufs.nonblockingUndockFirstElement();
			if (h == null) {
			    Debug.out.println("jx.netmanager.UDPReceiver: no buffer available, must drop packet!");
			    return buf.mem;
			}
			Memory in = h.getData();
			in = in.revoke();
			h.setData(buf.mem);
			h.setMoreData(buf);
			filledBufs.appendElement(h);
			return in;
		    }
		};
	    net.udp.registerUDPConsumer(consumer, localPort);
	}
	
	port = localPort;
		
    }
    /*
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
    */

    public UDPData receive1(Memory buf, int timeoutMillis) {
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	Buffer h =null;
	CycleTime now = new CycleTime();
	CycleTime start = new CycleTime();
	CycleTime diff = new CycleTime();
	clock.getCycles(start);
	while (h == null) {
	    clock.getCycles(now);
	    clock.subtract(diff, now, start);
	    if (clock.toMilliSec(diff) >= timeoutMillis) {
		for (int i=0;i<(10*timeoutMillis);i++) Thread.yield();
		return null;
	    }
	    h = filledBufs.nonblockingUndockFirstElement();
	    Thread.yield();
	}
	UDPData result = (UDPData) h.getMoreData();
	if (result == null) throw new Error("received packet but UDPData result==null");
	//result.offset = h.getOffset();
	//result.size = h.getSize();
	h.setData(buf);// extendfull?
	usableBufs.appendElement(h);
	return result;
     }

    public UDPData receive1(Memory buf) {
	Buffer h = filledBufs.undockFirstElement();
	UDPData result = (UDPData) h.getMoreData();
	//buf = buf.revoke();
	h.setData(buf);// extendfull?
	usableBufs.appendElement(h);
	return result;
    }


    public UDPData receive(Memory buf, int timeoutMillis) {
	if (1==1) throw new Error("temporarily disabled. use receive1");
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	Buffer h =null;
	CycleTime now = new CycleTime();
	CycleTime start = new CycleTime();
	CycleTime diff = new CycleTime();
	clock.getCycles(start);
	while (h == null) {
	    clock.getCycles(now);
	    clock.subtract(diff, now, start);
	    if (clock.toMilliSec(diff) >= timeoutMillis) {
		for (int i=0;i<(10*timeoutMillis);i++) Thread.yield();
		return null;
	    }
	    h = filledBufs.nonblockingUndockFirstElement();
	    Thread.yield();
	}
	buf = buf.revoke();
	UDPData result = (UDPData) h.getMoreData();
	if (result == null) throw new Error("received packet but UDPData result==null");
	h.setData(buf);// extendfull?
	usableBufs.appendElement(h);
	return result;
     }



    public UDPData receive(Memory buf) {
	if (1==1) throw new Error("temporarily disabled. use receive1");
	Buffer h = filledBufs.undockFirstElement();
	UDPData result = (UDPData) h.getMoreData();
	buf = buf.revoke();
	h.setData(buf);// extendfull?
	usableBufs.appendElement(h);
	//UDPData u = new UDPData();
	//u.mem = result;
	//u.sourceAddress = net.udp.getSource(result);
	//u.sourcePort = net.udp.getSourcePort(result);
	return result;
    }

    /*
    public UDPData receive(Memory buf) {
	Buffer h = filledBufs.undockFirstElement();
	Memory result = h.getData();
	h.setData(buf.extendAndRevoke());
	usableBufs.appendElement(h);
	UDPData u = new UDPData();
	u.mem = result;
	u.sourceAddress = net.udp.getSource(result);
	u.sourcePort = net.udp.getSourcePort(result);
	return u;
    }
    */

    public void close() {
	net.udp.unregisterUDPConsumer(consumer, port);
    }

}
