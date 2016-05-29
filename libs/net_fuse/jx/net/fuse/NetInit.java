package jx.net.fuse;

import jx.net.protocol.ether.*;
import jx.net.protocol.ip.*;
import jx.net.protocol.arp.*;
import jx.net.protocol.icmp.*;
import jx.net.protocol.udp.*;
import jx.net.protocol.udp.UDP;
import jx.net.protocol.bootp.*;

import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;
import jx.net.AddressResolution;
import jx.net.IPAddress;
import jx.net.UnknownAddressException;

import jx.devices.NetworkDevice;

import jx.timer.*;
import jx.zero.Memory;
import jx.zero.MemoryManager;
import jx.zero.InitialNaming;
import jx.zero.Service;

import jx.zero.*;
import jx.zero.debug.Dump;
import jx.devices.pci.PCIAccess;

import metaxa.os.devices.net.ComInit;
import metaxa.os.devices.net.D3C905;

import jx.timer.TimerManager;
import jx.timer.SleepManager;
import timerpc.SleepManagerImpl;

public class NetStack implements MemoryConsumer2 {
}

public class NetInit implements jx.net.NetInit, Service {
    IP ip;
    UDP udp;
    ARP arp;
    Ether ether;
    
    IPAddress localAddress;
    MemoryManager memMgr = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");

    public NetInit(NetworkDevice nic, TimerManager timerManager, Memory[] bufs) throws Exception {
	NetStack stack = new NetStack(nic, nic.getMACAddress());
	nic.registerConsumer2(ether.getReceiver2(bufs)); // buffered

	/*
	arp = new ARP(ether, null, timerManager, false);
	ip = new IP(null, (PacketsProducer)ether, (AddressResolution) null);
	udp = new UDP(null, (PacketsProducer)ip); //TODO: no need for transmitter
	*/

	// now network system is configured and we can start using it (e.g. bootp)

	initIP();
    }
    void initIP() { 
	Debug.out.println("Waiting 3 seconds...");
	SleepManager sleepMgr = new timerpc.SleepManagerImpl();
	sleepMgr.mdelay(3000);
	Debug.out.println("back.");

	// bootp      
	BOOTP bootp = new BOOTP(this, ether);
	localAddress = bootp.sendRequest();
	Debug.out.println("IP address: "+localAddress.toString());
	ip.changeSourceAddress(localAddress);
	
	arp.register(ip);		    
	ip.setAddressResolution(arp);		    
	ether.registerConsumer(ip, "IP");
	ether.registerConsumer(arp, "ARP");
	
	
    }

    public jx.net.UDPReceiver getUDPReceiver(int port) { 
	throw new Error();
    }
    public jx.net.UDPReceiver getUDPReceiver(int port, Memory[] bufs) { 
	return new UDPReceiver(this, port, bufs); 
    }
    public jx.net.UDPSender getUDPSender(int localPort, IPAddress dst, int remotePort) throws UnknownAddressException {
	return new UDPSender(this, localPort, dst, remotePort);
    }

    // TODO: make this independent from Ethernet
    public Memory getUDPBuffer() {
	return getUDPBuffer(1514-(14+20+8));
    }
    public Memory getUDPBuffer(int size) {
	Memory buf = memMgr.alloc(1514);// ETHER FRAME SIZE   // 14+20+8 + size);
	return buf.getSubRange(14+20+8, size);
    }



    public IPAddress getLocalAddress() {
	return localAddress;
    }


    public static void init(final Naming naming) {
	final SleepManager sleepManager = new SleepManagerImpl();
	final MemoryManager memMgr = (MemoryManager) naming.lookup("MemoryManager");
	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Debug.out.println("lookup PCI Access Point...");
	PCIAccess bus;
	int counter=0;
	for(;;) {
	    bus = (PCIAccess)naming.lookup("PCIAccess");
	    if (bus == null) {
		if (counter % 20 == 0) { counter = 0; Debug.out.println("NetInit still waiting for PCI");}
		counter++;
		Thread.yield();
	    } else {
		break;
	    }
	}
	
	Debug.out.println("scanning PCIBus for 3c905 devices...");
	ComInit com = new ComInit(timerManager, sleepManager, null);
	final D3C905 nic = com.findDevice(bus);

	if (! nic.NicOpen()) {
	    throw new Error("Cannot initialize network card.");
	}

	nic.NICSetReceiveMode(null);
	nic.unmaskInterrupts();

	Debug.out.print("Ethernet address: ");
	Dump.xdump(nic.getMACAddress(), 6);

	jx.netmanager.NetInit netinstance;
	try {
	    Memory[] bufs = new Memory[30];
	    for(int i=0; i<bufs.length; i++) {
		bufs[i] = memMgr.alloc(1514);
	    }
	    netinstance = new jx.netmanager.NetInit(nic, timerManager,bufs);

	    final jx.net.NetInit net = netinstance;
	    //final jx.net.NetInit net = (jx.net.NetInit)naming.promoteDEP(netinstance, "jx/net/NetInit");
	    naming.registerDEP(net, "NetManager");
	    cpuManager.start(cpuManager.createCPUState(new ThreadEntry () {
		    public void run() {
		      cpuManager.setThreadName("Portal:NFS-NetManager");
		      cpuManager.receive(net);
		    }
		}));
	} catch(Exception e) {
	    throw new Error("Could not setup");
	}	

	}

}


