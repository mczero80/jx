package jx.net.protocol.udp;

import jx.zero.*;
import jx.zero.debug.Dump;

import jx.buffer.separator.MemoryConsumer;
import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;
import jx.net.PacketsConsumer1;
import jx.net.IPAddress;

import jx.net.IPProducer;
import jx.net.IPProducer1;
import jx.net.IPConsumer;
import jx.net.IPData;
import jx.net.UDPConsumer;
import jx.net.UDPConsumer1;
import jx.net.IPConsumer1;
import jx.net.UDPData;

public class UDP implements IPConsumer, IPConsumer1 {
  
    private PacketsConsumer lowerConsumer;
    private IPProducer lowerProducer;
    private IPProducer1 lowerProducer1;

    static final boolean dumpAll = false; // switch on to see all udp frames
    static final boolean printIfNoReceiver = false; // print to debug out if no receiver for port
    static final boolean printRegistration = false;
    private final static boolean debugPacketNotice = false;

    private UDPConsumer[] udpConsumerList = new UDPConsumer[65536]; // hack
    private UDPConsumer1[] udpConsumerList1 = new UDPConsumer1[65536]; // hack

    CPUManager cpuManager;
    int event_rcv;
    int event_snd;
    
    public UDP(IPProducer lowerProducer) {
	init();
	this.lowerProducer = lowerProducer;
	lowerProducer.registerConsumer(this, "UDP");
    }
    public UDP(IPProducer1 lowerProducer) {
	init();
	this.lowerProducer1 = lowerProducer;
	lowerProducer.registerConsumer1(this, "UDP");
    }
    private void init() {
	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	event_rcv = cpuManager.createNewEvent("UDPRcv");
	event_snd = cpuManager.createNewEvent("UDPSnd");
    }

    public Memory processIP1(IPData buf) {
	if (debugPacketNotice) Debug.out.println("UDP.receive: "+buf.size);
	cpuManager.recordEvent(event_rcv);
	Memory mem = buf.mem;
	UDPFormat udp = new UDPFormat(buf.mem, buf.offset);

	if (dumpAll) { 
	    Debug.out.println("UDP packet received");
	    udp.dump();
	}

	int port = udp.getDestPort();
	int srcPort = udp.getSourcePort();
	if (dumpAll) Debug.out.println("UDP destination port: "+port);
	MemoryConsumer consumer;
	UDPConsumer1 udpConsumer;
	checkPort(port);
	if ((udpConsumer=udpConsumerList1[port]) != null) {
	    int space = udp.length();
	    UDPData u = new UDPData();
	    u.mem = buf.mem;
	    u.offset = buf.offset+UDPFormat.requiresSpace();
	    u.size = buf.size-UDPFormat.requiresSpace();
	    u.sourcePort = srcPort;
	    u.sourceAddress = buf.sourceAddress; 
	    return udpConsumer.processUDP1(u);
	}

	if (printIfNoReceiver)
	    Debug.out.println("No UDP receiver for port: "+port);
	return buf.mem;
    }

    public Memory processIP(IPData buf) {
	if (debugPacketNotice) Debug.out.println("UDP.receive: "+buf.mem.size());
	cpuManager.recordEvent(event_rcv);
	Memory mem = buf.mem;
	if (dumpAll) { 
	    Debug.out.println("UDP packet received");
	    Dump.xdump1(buf.mem, 0, 128);
	}

	UDPFormat udp = new UDPFormat(buf.mem);

	int port = udp.getDestPort();
	int srcPort = udp.getSourcePort();
	if (dumpAll) Debug.out.println("UDP destination port: "+port);
	MemoryConsumer consumer;
	UDPConsumer udpConsumer;
	checkPort(port);
	if ((udpConsumer=udpConsumerList[port]) != null) {
	    int space = udp.length();
	    //Debug.out.println("UDPDATALEN: "+(buf.size()-space));
	    Memory data = buf.mem.getSubRange(space, buf.mem.size()-space);
	    UDPData u = new UDPData();
	    u.mem = data;
	    u.sourcePort = srcPort;
	    u.sourceAddress = buf.sourceAddress; //lowerProducer.getSource(data.joinPrevious());
	    //Debug.out.println("Source address: "+u.sourceAddress);
	    return udpConsumer.processUDP(u);
	}

	if (printIfNoReceiver)
	    Debug.out.println("No UDP receiver for port: "+port);
	return buf.mem;
    }

    public boolean registerUDPConsumer1(UDPConsumer1 consumer, int port) {
	checkPort(port);
	if (printRegistration) {
	  Debug.out.println("Register1 (nosplit) UDP receiver for port: "+port);
	}
	if (udpConsumerList1[port] != null) throw new Error("port already used");
	udpConsumerList1[port] = consumer;
	return true;
    }


    public boolean registerUDPConsumer(UDPConsumer consumer, int port) {
	checkPort(port);
	if (printRegistration) {
	  Debug.out.println("Register (splitting) UDP receiver for port: "+port);
	}
	if (udpConsumerList[port] != null) throw new Error("port already used");
	udpConsumerList[port] = consumer;
	return true;
    }

    public boolean unregisterUDPConsumer(UDPConsumer consumer, int port) {
	checkPort(port);
	if (udpConsumerList[port] == null) throw new Error("no consumer registered");
	if (udpConsumerList[port] != consumer) throw new Error("this consumer is not registered");
	udpConsumerList[port] = null;
	return true;
    }

    public PacketsConsumer1 getTransmitter1(final PacketsConsumer1 lowerLayer, final int srcPort, final int dstPort) {
	checkPort(srcPort);
	checkPort(dstPort);
	return new PacketsConsumer1() {
		public Memory processMemory(Memory userbuf, int offset, int size) {
		    if (debugPacketNotice) Debug.out.println("UDP.transmit: "+size);
		    cpuManager.recordEvent(event_snd);
		    Memory buf = userbuf;
		    offset -= UDPFormat.requiresSpace();
		    UDPFormat udp = new UDPFormat(buf, offset);
		    udp.insertSourcePort(srcPort);
		    udp.insertDestPort(dstPort);
		    udp.insertLength(size+8);
		    Memory ret = lowerLayer.processMemory(buf, offset, UDPFormat.requiresSpace()+size);
		    return ret;
		}
		public int requiresSpace() {return UDPFormat.SIZE;}
		public int getMTU() {return 1000; /*TODO*/}
	    };
    }

    public PacketsConsumer getTransmitter(final PacketsConsumer lowerLayer, final int srcPort, final int dstPort) {
	checkPort(srcPort);
	checkPort(dstPort);
	return new PacketsConsumer() {
		public Memory processMemory(Memory userbuf) {
		    if (debugPacketNotice) Debug.out.println("UDP.transmit: "+userbuf.size());
		    cpuManager.recordEvent(event_snd);
		    Memory buf = userbuf.joinPrevious();
		    UDPFormat udp = new UDPFormat(buf);
		    udp.insertSourcePort(srcPort);
		    udp.insertDestPort(dstPort);
		    udp.insertLength(buf.size());
		    if (dumpAll) {
		      Debug.out.println("UDP send to "+dstPort+" lLayer "+lowerLayer.getClass().getName());
		      Dump.xdump1(buf, 0, 128);
		    }
		    Memory ret = lowerLayer.processMemory(buf);
		    int space = udp.length();
		    //cpuManager.dump("..",ret);
		    //return ret.getSubRange(space, ret.size()-space);
		    Memory [] arr = new Memory[2];
		    ret.split2(space, arr);
		    return arr[1];
		}
		public int requiresSpace() {return UDPFormat.SIZE;}
		public int getMTU() {return 1000; /*TODO*/}
	    };
    }

    private void checkPort(int port) {
	if (port <0 || port >65535) {
	    Debug.out.println("PORT: "+port);
	    throw new Error("Invalid port number.");
	}
    }
}







