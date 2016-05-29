package jx.net.protocol.arp;

import java.util.Vector;
import jx.zero.*;
import jx.timer.*;

import jx.net.protocol.ether.Ether;
import jx.net.protocol.ether.EtherFormat;
import jx.net.protocol.ip.IP;

import jx.net.EtherConsumer1;
import jx.net.EtherData;

import jx.net.IPAddress;
import jx.net.AddressResolution;
import jx.net.UnknownAddressException;
import jx.buffer.separator.MemoryConsumer;

public class ARP implements AddressResolution, MemoryConsumer, EtherConsumer1 {
  
    private IPAddress ownProtocolAddress;
    private byte[] ownHardwareAddress;
  
    private ARPCache arpCache;
    private Ether ethernet;

    private IP ipLayer = null;

    private boolean sendARPs;
    //private Memory answer;

    static final boolean dumpAll = false; // switch on to see all arp frames
    private final static boolean debugPacketNotice = false;
    CPUManager cpuManager;
    MemoryManager memoryManager;
    private int event_rcv;

    public ARP(Ether ethernet, IP ip, TimerManager timerManager, boolean sendARPs) {
	this.ethernet = ethernet;
	this.ipLayer = ip;
	this.sendARPs = sendARPs;
	memoryManager = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");
	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	event_rcv = cpuManager.createNewEvent("ARPRcv");
	//answer = memoryManager.alloc(1514).getSubRange(14, ARPFormat.requiresSpace());
	ownHardwareAddress = ethernet.getMacAddress();
	if (ipLayer!=null) ownProtocolAddress = ipLayer.getOwnAddress();
 
	arpCache = new ARPCache(this, timerManager);
	if (sendARPs) timerManager.addMillisTimer(900000,   new ARPTimer(timerManager), new ARPTimerArg(this));
    }
    public boolean register(Object ip) {
	if (ipLayer != null)
	    return false;
	ipLayer = (IP)ip;
	ownProtocolAddress = ipLayer.getOwnAddress();
	return true;
    }

    public void notifyAddressChange(Object o) {
	if (ipLayer!=null) ownProtocolAddress = ipLayer.getOwnAddress();
    }

    public int requiresSpace() {
	return (ARPFormat.requiresSpace() + EtherFormat.requiresSpace());
    }

    public void add(byte[] etherAddress, byte[] ipAddress) {
	arpCache.add(etherAddress, ipAddress);
    }

    public byte[] lookup(byte[] ipAddress) throws UnknownAddressException {
	return arpCache.lookup(ipAddress);
    }

    public byte[] lookup(IPAddress ipAddress) throws UnknownAddressException {
	return arpCache.lookup(ipAddress);
    }

    public void clearCache() {
	arpCache.clearAll();
    }
  
    private boolean byteCompare(byte[] a1, byte[] a2) {
	if (a1.length != a2.length) {
	    Debug.out.println("ARP.byteCompare: the arrays have different size");
	    return false;
	}
	for (int i=0; i < a1.length; i++) {
	    if (a1[i] != a2[i]) {
		//Debug.out.println("ARP.byteCompare: position "+i+"differs:"+a1[i]+"!="+a2[i]);
		return false;
	    }
	}
	return true;
    }

    public void sendQuery(byte[] ipAddress) {
	/*if (verbose)*/ Debug.out.println("   send arp query...");
	
	Memory userbuf = memoryManager.alloc(1514);
	//Memory buf = userbuf.getSubRange(14, ARPFormat.requiresSpace());
	Memory [] arr = new Memory[3];
	//userbuf.split3(14, ARPFormat.requiresSpace(),arr);
	//Memory buf = arr[1];
	//if (debugPacketNotice) Debug.out.println("ARP.sendQuery: "+buf.size());
	ARPFormat format = new ARPFormat(userbuf, 14);
	// Ethernet
	format.insertHardwareAddressSpace((short)1);
	// IP-Address
	format.insertProtocolAddressSpace((short)0x800);
	format.insertHardwareAddressLen((byte)6);
	format.insertProtocolAddressLen((byte)4);
	// ARP query
	format. insertCommand((byte)1);
	format.insertSenderHardwareAddress(ownHardwareAddress);
	if (ownProtocolAddress != null) format.insertSenderProtocolAddress(ownProtocolAddress);
	format.insertTargetProtocolAddress(ipAddress);
	ethernet.transmitARPBroadcast1(userbuf);
    } 
    
    public Memory processEther1(EtherData buf) {
	ARPFormat a = new ARPFormat(buf.mem, buf.offset);
	cpuManager.recordEvent(event_rcv);
	if (debugPacketNotice) Debug.out.println("ARP.processEther: "+buf.size);
	if (dumpAll){
	    Debug.out.println("ARP packet received.");
	    a.dump();
	}
	if (ownProtocolAddress == null) {
	    Debug.out.println("ARP: I don't know who I am");
	    //cpuManager.dump("ARPRET", buf);
	    return buf.mem; // can not respond to ARP because I don't know who I am
	}
	if (a.getCommand() == 1 && a.getHardwareAddressSpace() == 1 && a.getProtocolAddressSpace() == 0x800) {
	    if (dumpAll) Debug.out.println("ARP: request received!");
	    IPAddress target = a.getTargetProtocolAddress();
	    if (dumpAll) {
		Debug.out.println(" Address to resolve: "+target.toString());
		Debug.out.println(" My Address: "+ownProtocolAddress.toString());
	    }
	    if (target.equals(ownProtocolAddress)) {
		byte[] senderHW = a.getSenderHardwareAddress();
		byte[] senderProto = a.getSenderProtocolAddress();
		a = null;
		Memory answer = buf.mem;		
		if (dumpAll) Debug.out.println("ARP: my ethernet-address is queried!");
		ARPFormat answ = new ARPFormat(answer, EtherFormat.requiresSpace());
		answ.insertHardwareAddressSpace((short)1);
		answ.insertProtocolAddressSpace((short)0x800);
		answ.insertHardwareAddressLen((byte)6);
		answ.insertProtocolAddressLen((byte)4);
		answ.insertCommand((short)2);
		answ.insertSenderHardwareAddress(ownHardwareAddress);
		answ.insertSenderProtocolAddress(ownProtocolAddress);
		answ.insertTargetHardwareAddress(senderHW);
		answ.insertTargetProtocolAddress(senderProto);

		//answer = ethernet.transmitARPBroadcast(answer);
		if (debugPacketNotice) Debug.out.println("ARP.sendAnswer. ");
		return ethernet.transmitSpecial(ownHardwareAddress, senderHW, 0x0806, answer);
		
	    }
	    else {
		if (dumpAll) Debug.out.println("ARP-Packet was not targeted to us!");
	    }
	}
	else if (a.getCommand() == 2 && a.getHardwareAddressSpace() == 1 && a.getProtocolAddressSpace() == 0x800) {
	    arpCache.add(a.getSenderHardwareAddress(), a.getSenderProtocolAddress());
	}
	else {
	    Debug.out.println("Unknown ARP command:" + a.getCommand());
	}
	return buf.mem;
    }

    public Memory processMemory(Memory buf) {
	buf = buf.revoke();
	ARPFormat a = new ARPFormat(buf,0);
	cpuManager.recordEvent(event_rcv);
	if (debugPacketNotice) Debug.out.println("ARP.receive: "+buf.size());
	if (dumpAll) Debug.out.println("ARP packet received.");
	if (ownProtocolAddress == null) {
	    Debug.out.println("ARP: I don't know who I am");
	    //cpuManager.dump("ARPRET", buf);
	    return buf; // can not respond to ARP because I don't know who I am
	}
	if (a.getCommand() == 1 && a.getHardwareAddressSpace() == 1 && a.getProtocolAddressSpace() == 0x800) {
	    if (dumpAll) Debug.out.println("ARP: request received!");
	    IPAddress target = a.getTargetProtocolAddress();
	    if (dumpAll) {
		Debug.out.println(" Address to resolve: "+target.toString());
		Debug.out.println(" My Address: "+ownProtocolAddress.toString());
	    }
	    if (target.equals(ownProtocolAddress)) {
		byte[] senderHW = a.getSenderHardwareAddress();
		byte[] senderProto = a.getSenderProtocolAddress();
		a = null;
		Memory answer = buf.joinPrevious();		
		if (dumpAll) Debug.out.println("ARP: my ethernet-address is queried!");
		ARPFormat answ = new ARPFormat(answer, EtherFormat.requiresSpace());
		answ.insertHardwareAddressSpace((short)1);
		answ.insertProtocolAddressSpace((short)0x800);
		answ.insertHardwareAddressLen((byte)6);
		answ.insertProtocolAddressLen((byte)4);
		answ.insertCommand((short)2);
		answ.insertSenderHardwareAddress(ownHardwareAddress);
		answ.insertSenderProtocolAddress(ownProtocolAddress);
		answ.insertTargetHardwareAddress(senderHW);
		answ.insertTargetProtocolAddress(senderProto);

		//answer = ethernet.transmitARPBroadcast(answer);
		if (debugPacketNotice) Debug.out.println("ARP.sendAnswer: "+answer.size());
		return ethernet.transmitSpecial(ownHardwareAddress, senderHW, 0x0806, answer);
		
	    }
	    else {
		if (dumpAll) Debug.out.println("ARP-Packet was not targeted to us!");
	    }
	}
	else if (a.getCommand() == 2 && a.getHardwareAddressSpace() == 1 && a.getProtocolAddressSpace() == 0x800) {
	    arpCache.add(a.getSenderHardwareAddress(), a.getSenderProtocolAddress());
	}
	else {
	    Debug.out.println("Unknown ARP command!");
	}
	return buf;
    }

}







