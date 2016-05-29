package jx.net.protocol.arp;

import java.util.Vector;
import jx.zero.*;
import jx.timer.*;

import jx.net.UnknownAddressException;
import jx.net.IPAddress;

/**
 * implementing the cache which records already known IP-MAC pairs 
 */
class ARPCache {
    static final boolean verbose = false;

    final  byte[] broadcastIP = {(byte)255, (byte)255, (byte)255, (byte)255 };
    final byte[] broadcastETH = { (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff };


    /*
    final  byte[] faui48lIP = {(byte)131, (byte)188, 34, (byte)240 };
    final byte[] faui45IP = {(byte)131, (byte)188, 34, (byte)45 };
    final byte[] faui40dIP = {(byte)131, (byte)188, 34, (byte)128 };
    final byte[] faui40pIP = {(byte)131, (byte)188, 34, (byte)77 };
    final byte[] faui49fIP = {(byte)192, (byte)168, 34, (byte)7 };
    final byte[] faui49aIP = {(byte)192, (byte)168, 34, (byte)2 };
    final byte[] ultra1IP = {(byte)192, (byte)168, 34, (byte)101 };
    final byte[] pc8IP = {(byte)192, (byte)168, 34, (byte)13 };
    final byte[] faui40dETH = { (byte)0x8, (byte)0x0, (byte)0x20, (byte)0x86, (byte)0xc9, (byte)0x31};
    final byte[] faui40pETH = { 0x8, 0x0, 0x20, (byte)0x86, (byte)0xaa, (byte)0xd0};
    final byte[] faui48lETH = { 0x8, 0x0, 0x6, (byte)0x28, (byte)0x63, (byte)0x30};
    final byte[] faui45ETH = { 0x8, 0, 0x20, (byte)0x78, (byte)0x9d, (byte) 0x5f };
    final byte[] faui49fETH = { 0x8, 0, 0x20, (byte)0x78, (byte)0x9d, (byte) 0x5f };
    final byte[] faui49aETH = {00, 4, (byte)0x76, 0x12, (byte)0x92, (byte)0x73};
    final byte[] ultra1ETH = {8, 0, (byte)0x20, (byte)0x7b, (byte)0xfa, (byte)0x25 };
    final byte[] pc8ETH = {00, (byte)0x50, (byte) 0xDA, (byte) 0xE2, (byte) 0xD0, (byte) 0x8E };
    final byte[] pc10ETH = {00, (byte)0x50,(byte)4,(byte)0xec,(byte)0xed,(byte)0xf4};
    final byte[] pc10IP = {(byte)192, (byte)168, 34, (byte)15 };

    final  byte[] notebookIP = {(byte)192, (byte)168, (byte)34, (byte)21 };
    final byte[] notebookETH = { (byte)0x00, (byte)0xE0, (byte)0x18, (byte)0x1B, (byte)0x6E, (byte)0x05};

    final byte[] pc5IP = {(byte)192, (byte)168, 34, (byte)10 };
    final byte[] pc5ETH = {00, (byte)0x50, (byte) 0x04, (byte) 0xEC, (byte) 0xED, (byte) 0xE4 };
    */


    // set the maximum lookuptime to 10 seconds
    final int LOOKUPTIME = 10000;  // millis
    
    private Vector ethers;
    private Vector ips;
    private ARP arp;
    private TimerManager timerManager;
    ARPCache(ARP a, TimerManager timerManager) {
	this.timerManager = timerManager;
	ethers = new Vector();
	ips = new Vector();
	arp = a;
	init();	
    }
    
    void add(byte[] etherAddress, byte[] ipAddress) {
	if (!ethers.contains(etherAddress) && !ips.contains(ipAddress)) {
	    ethers.addElement(etherAddress);
	    ips.addElement(ipAddress);
	}
    }
    
    void clearAll() {
	ethers.removeAllElements();
	ips.removeAllElements();
    }
    
    byte[] lookup(IPAddress ipAddress) throws UnknownAddressException {
	if (verbose) Debug.out.println("ARPCache.lookup():"+ipAddress);
	return lookup(ipAddress.getBytes());
    }

    byte[] lookup(byte[] ipAddress) throws UnknownAddressException {
	byte[] hardwareaddress;
	int time;
	if ((hardwareaddress = scan(ipAddress)) != null) {
	    if (verbose) Debug.out.println("   <- OK");
	    return hardwareaddress;
	}
	
	//if (true) throw new Error();

	arp.sendQuery(ipAddress);
	
	time = timerManager.getCurrentTime();
	int timeout = time + LOOKUPTIME * 1000 / timerManager.getTimeBaseInMicros();
	while (timerManager.getCurrentTime() < timeout) {
	    hardwareaddress = scan(ipAddress);
	    if (hardwareaddress != null)
		return hardwareaddress;
	    Thread.yield();
	}

	throw new UnknownAddressException();
    }
    
    private byte[] scan(byte[] ipAddress) {
	for(int i=0; i<ips.size(); i++) {
	    if (equalIP((byte[])ips.elementAt(i), ipAddress)) {
		return (byte[])ethers.elementAt(i);
	    }
	}
	return null;
    }
    
    private boolean equalIP(byte[] ip1, byte[] ip2) {
	for(int i=0; i<4; i++) {
	    if (ip1[i] != ip2[i]) return false; 
	}
	return true;
    }
    
    private void init() {
	add(broadcastETH, broadcastIP);
	/*
	add(faui49aETH, faui49aIP);
	add(ultra1ETH, ultra1IP);
	add(pc8ETH, pc8IP);
	add(pc10ETH, pc10IP);
	add(pc5ETH, pc5IP);
	add(notebookETH, notebookIP);
	*/
    }
}
