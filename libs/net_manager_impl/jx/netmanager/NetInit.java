package jx.netmanager;

import jx.net.protocol.ether.*;
import jx.net.protocol.ip.*;
import jx.net.protocol.arp.*;
import jx.net.protocol.icmp.*;
import jx.net.protocol.udp.*;
import jx.net.protocol.udp.UDP;
import jx.net.protocol.bootp.*;
import jx.net.protocol.tcp.*;

import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;
import jx.net.AddressResolution;
import jx.net.IPAddress;
import jx.net.UnknownAddressException;
import jx.net.IPProducer1;
import jx.net.EtherProducer1;

import jx.devices.net.NetworkDevice;
import jx.devices.DeviceFinder;
import jx.devices.Device;

import jx.timer.*;
import jx.zero.Memory;
import jx.zero.MemoryManager;
import jx.zero.InitialNaming;
import jx.zero.Service;

import jx.zero.*;
import jx.zero.debug.Dump;
import jx.devices.pci.PCIAccess;

import jx.timer.TimerManager;
import jx.timer.SleepManager;

// --- DEVICE DRIVERS ---

// 3com
import metaxa.os.devices.net.ComInit;
import metaxa.os.devices.net.D3C905;

// lance
import jx.net.devices.lance.*;

// emulated device
import jx.devices.net.emulation.EmulNetFinder;

public class NetInit implements jx.net.NetInit, Service {
    TCP tcp;
    IP ip;
    UDP udp;
    ARP arp;
    Ether ether;
    
    IPAddress localAddress;
    MemoryManager memMgr = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");
    CPUManager cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");

    public NetInit(NetworkDevice nic, TimerManager timerManager, Memory[] bufs) throws Exception {
	this(nic, timerManager, bufs, null); 
    }
    public NetInit(NetworkDevice nic, TimerManager timerManager, Memory[] bufs, IPAddress myAddress) throws Exception {
	// nic
	ether = new Ether(nic, nic.getMACAddress());
	nic.registerNonBlockingConsumer(ether.getNonBlockingReceiver(bufs)); // buffered


	arp = new ARP(ether, null, timerManager, false);
	Debug.out.println("NetInit: init IP");
	ip = new IP((EtherProducer1)ether); // avoid splitting
	udp = new UDP((IPProducer1)ip); //TODO: no need for transmitter
        tcp = new TCP((jx.net.IPProducer1)ip, this, timerManager);

	// connect ARP with Ether
	if (! ether.registerConsumer1(arp, "ARP")) {
	    Debug.out.println("ARP: couldn´t register at etherLayer!!");
	    throw new Error();
	}

	// now network system is configured and we can start using it (e.g. bootp)
	Debug.out.println("NetInit: Waiting 3 seconds...");
	//timerManager.unblockInMillis(cpuManager.getCPUState(), 3000);
	/* cpuManager.block(); */ // DANGER: lost-update problem (FIXME)
	for (int i=0;i<3000;i++) cpuManager.yield();
	Debug.out.println("back.");

	// boot
	localAddress = myAddress;
	if (localAddress == null) {
	    BOOTP bootp = new BOOTP(this, ether.getMacAddress());
	    localAddress = bootp.sendRequest1();
	}
	Debug.out.println("IP address: "+localAddress.toString());
	ip.changeSourceAddress(localAddress);
	
	arp.register(ip);		    
	ip.setAddressResolution(arp);		    
	ether.registerConsumer(ip, "IP");
	ether.registerConsumer(arp, "ARP");
	
	
    }

    public jx.net.protocol.tcp.TCP getTCP() {
        return tcp;
    }

    public jx.net.TCPSocket getTCPSocket(int port, IPAddress ip, Memory[] bufs) {
        return new TCPSocket(tcp, port, ip, bufs);
    }

    public jx.net.UDPReceiver getUDPReceiver(int port, Memory[] bufs) { 
	return new UDPReceiver(this, port, bufs, true); // avoidSplitting 
    }
    public jx.net.UDPSender getUDPSender(int localPort, IPAddress dst, int remotePort) throws UnknownAddressException {
	return new UDPSender(this, localPort, dst, remotePort);
    }

    public jx.net.IPReceiver getIPReceiver(Memory[] bufs) { 
	return new IPReceiver(this, bufs); 
    }
    public jx.net.IPReceiver getIPReceiver(Memory[] bufs, String proto) { 
	return new IPReceiver(this, bufs, proto); 
    }

    /* akbp
    public jx.net.IPSender getIPSender(IPAddress dst) throws UnknownAddressException {
	return new IPSender(this, dst);
    }
    */

    public jx.net.IPSender getIPSender(IPAddress dst, int id) throws UnknownAddressException {
	return new IPSender(this, dst, id);
    }

    public Memory getTCPBuffer1() {
        return memMgr.alloc(1514);
    }

    public Memory getUDPBuffer1() {
        return memMgr.alloc(1514);
    }

    public Memory getTCPBuffer() {
        return getTCPBuffer(1514-(14+20+20));
    }

    public Memory getTCPBuffer(int size) {
        Memory buf = getIPBuffer(size+20);
        return buf;
    }

    // TODO: make this independent from Ethernet
    public Memory getUDPBuffer() {
	return getUDPBuffer(1514-(14+20+8));
    }
    public Memory getUDPBuffer(int size) {
	Memory buf = memMgr.alloc(1514);// ETHER FRAME SIZE   // 14+20+8 + size);
	Memory[] arr = new Memory[3];
	buf.split3(14+20+8, size, arr); /* parts size: 14+20+8, size, rest */
	Memory[] arr1 = new Memory[3];
	arr[0].split3(14,20, arr1); /* parts size: 14, 20, 8 */
	//cpuManager.dump("UDPBUFFER: ", arr[1]);
	return arr[1];
    }

    // TODO: make this independent from Ethernet
    public Memory getIPBuffer() {
	return getIPBuffer(1514-(14+20));
    }
    public Memory getIPBuffer(int size) {
	Memory buf = memMgr.alloc(1514);// ETHER FRAME SIZE   // 14+20+8 + size);
	Memory[] arr = new Memory[3];
	buf.split3(14+20, size, arr); /* parts size: 14+20, size, rest */
	Memory[] arr1 = new Memory[2];
	arr[0].split2(14, arr1); /* parts size: 14, 20 */
	//cpuManager.dump("IPBUFFER: ", arr[1]);
	return arr[1];
    }



    public IPAddress getLocalAddress() {
	return localAddress;
    }


    public static void init(final Naming naming, String[] args) {
	final TimerManager timerManager = (TimerManager)LookupHelper.waitUntilPortalAvailable(naming, "TimerManager");
	//final SleepManager sleepManager = new SleepManagerImpl();
	final MemoryManager memMgr = (MemoryManager) naming.lookup("MemoryManager");
	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	PCIAccess bus = (PCIAccess)LookupHelper.waitUntilPortalAvailable(naming, "PCIAccess");
	bus.dumpDevices();

	Debug.out.println("scanning PCIBus for network devices...");

	DeviceFinder[] finder = { 
	    new EmulNetFinder(args[1], args[2]),
	    new ComInit(timerManager, /*sleepManager*/null, null),
	    new LanceFinder(),
	};
	NetworkDevice[] nics = null;
	for(int i=0; i<finder.length; i++) {
	    nics = (NetworkDevice[]) finder[i].find(new String[] {});
	    if (nics != null && nics.length != 0) break;
	}
	NetworkDevice nic = nics[0];

	nic.open(null);

	nic.setReceiveMode(NetworkDevice.RECEIVE_MODE_INDIVIDUAL);

	Debug.out.print("Ethernet address: ");
	Dump.xdump(nic.getMACAddress(), 6);

	jx.netmanager.NetInit netinstance;
	try {
	    Memory[] bufs = new Memory[30];
	    for(int i=0; i<bufs.length; i++) {
		bufs[i] = memMgr.alloc(1514);
	    }
	    netinstance = new jx.netmanager.NetInit(nic, timerManager,bufs);
	    naming.registerPortal(netinstance, args[0]);
	} catch(Exception e) {
	    throw new Error("Could not setup");
	}	

    }

}


