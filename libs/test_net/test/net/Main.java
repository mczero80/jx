package test.net;

import jx.zero.*;
import jx.shell.*;

import jx.timer.*;
import timerpc.*;

import jx.devices.pci.*;

import metaxa.os.devices.net.D3C905;
import metaxa.os.devices.net.ComInit;

import jx.devices.*;

import jx.zero.debug.Dump;

import jx.buffer.separator.MemoryConsumer;

import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;
import jx.net.AddressResolution;
import jx.net.IPAddress;
import jx.net.UnknownAddressException;

import jx.net.NetInit;
import jx.net.UDPSender;
import jx.net.UDPReceiver;
import jx.net.UDPData;

import jx.devices.net.*;

import jx.devices.net.emulation.EmulNetFinder;

import java.io.PrintStream;

public class Main {
    public static boolean test(final Naming naming) {
	new Main(naming);
	return true;
    }

    NetInit net;

    MemoryManager memMgr;
    Profiler profiler;
    Naming naming;

    Main(final Naming naming) {
	this.naming = naming;
	memMgr = (MemoryManager) naming.lookup("MemoryManager");
	profiler = (Profiler) naming.lookup("Profiler");
    }

    static D3C905 init3com(final Naming naming) {
	TimerManager timerManager = (TimerManager)LookupHelper.waitUntilPortalAvailable(naming,"TimerManager");

	//	SleepManager sleepManager = new SleepManagerImpl();

	Debug.out.println("lookup PCI Access Point...");
	PCIAccess bus = (PCIAccess)LookupHelper.waitUntilPortalAvailable(naming,"PCIAccess");

	Debug.out.println("scanning PCIBus for 3c905 devices...");
	ComInit com = new ComInit(timerManager, null/*sleepManager*/, null);
	final D3C905[] nics = com.findDevice(bus);
	final D3C905 nic = nics[0];

	if (nic == null) return null;

	if (! nic.NicOpen()) {
	    throw new Error("Cannot initialize network card.");
	}

	nic.NICSetReceiveMode(null);
	nic.unmaskInterrupts();

	System.out.print("Ethernet address: ");
	Dump.xdump(System.out, nic.getMACAddress(), 6);

	return nic;
    }


    static void simpletest(final NetInit net) throws Exception {
	// send
	UDPSender u = net.getUDPSender(6665, new IPAddress("192.168.34.2"), Integer.parseInt("9876"));
	Memory buf = net.getUDPBuffer(50);
	for(int i=0; i<50; i++) {
	    buf.set8(i, (byte)i);
	}
	for(int j=0; j<10; j++) {
	    u.send(buf);
	}
    }

    
    void dotest(final NetworkDevice nic) {
	final TimerManager timerManager = (TimerManager)LookupHelper.waitUntilPortalAvailable(naming,"TimerManager");

	System.out.print("Perform init as first command!");

	Shell shell = new Shell(naming);

	shell.register("init", new Command() {
		public void command(PrintStream out, String[] args) {
		    try {
			Memory[] bufs = new Memory[10];
			for(int i=0; i<bufs.length; i++) {
			    bufs[i] = memMgr.alloc(1514);
			}
			net = new jx.netmanager.NetInit(nic, timerManager, bufs);
		    } catch(Exception e) {
			throw new Error("Could not setup");
		    }
		}
	    public String getInfo() { return "setup network"; }	    
	});
		   

	shell.register("snd", new Command() {
	    public void command(PrintStream out, String[] args) {
	      if (args.length != 2) {
		System.out.println("need 2 args");
		return;
	      }

	      try {
		  UDPSender u = net.getUDPSender(6665, new IPAddress(args[0]), Integer.parseInt(args[1]));
		  Memory buf = net.getUDPBuffer(50);
		  for(int i=0; i<50; i++) {
		      buf.set8(i, (byte)i);
		  }
		  for(int j=0; j<10; j++) {
		      u.send(buf);
		  }
	      } catch(UnknownAddressException ex) {
		  throw new Error();
	      }
	    }
	    public String getInfo() { return "send packets to <addr> <port>"; }	    
	});

	shell.register("rcv", new Command() {
	    public void command(PrintStream out, String[] args) {
		Memory[] bufs = new Memory[1]; bufs[0] = net.getUDPBuffer(0);
		UDPReceiver socket = net.getUDPReceiver(6666, bufs);
		Memory buf = net.getUDPBuffer(0);
		UDPData udp = socket.receive(buf);
		buf = udp.mem;
		System.out.println("RECEIVED PACKET:");
		Dump.xdump(System.out, buf, 0, 256);
		socket.close();
	    }
	    public String getInfo() { return "receive udp packets at port "+6666; }	    
	});

	shell.register("pingsvc", new Command() {
	    public void command(PrintStream out, String[] args) {
	      if (args.length != 1) {
		  //System.out.println("need 1 arg");
		  args = new String[1];
		  args[0] = "192.168.34.2";
	      }
		try {
		// prepare sender
		UDPSender u = net.getUDPSender(6665, new IPAddress(args[0]), 6666);
		Memory sbuf = net.getUDPBuffer(50);
		
		// prepare receiver
		Memory[] bufs = new Memory[10]; 
		for(int i=0; i<10; i++) {
		    bufs[i] = net.getUDPBuffer(0);
		}
		UDPReceiver socket = net.getUDPReceiver(6666, bufs);
		Memory rbuf = net.getUDPBuffer(0);
		Memory sbuf1;
		//Debug.out = null;
		profiler.startSampling();
		for(;;) {
		    // receive request
		    UDPData udp = socket.receive(rbuf);
		    rbuf = udp.mem;
		    //System.out.println("RECEIVED PACKET:");
		    //Dump.xdump(System.out, rbuf, 0, 256);
		    
		    // send reply
		    sbuf1 = net.getUDPBuffer(50); // HACK: TODO
		    sbuf = u.send(sbuf);
		    sbuf = sbuf1;
		}

		//socket.close();
		} catch(Exception ex) { System.out.println("EXCEPTION"); }

	    }
	    public String getInfo() { return "receive udp request and send reply to <host> at port "+6666; }	    
	});



	try {
	    shell.mainloop();
	} catch(Exception ex) {}
    }
}




class StartNetworkProtocols {
    
    public static void main(String[] args) {
	Naming naming = InitialNaming.getInitialNaming();
	NetworkDevice nic = (NetworkDevice)LookupHelper.waitUntilPortalAvailable(naming, args[0]);

	MemoryManager memMgr = (MemoryManager) naming.lookup("MemoryManager");

	Memory[] bufs = new Memory[10];
	for(int i=0; i<bufs.length; i++) {
	    bufs[i] = memMgr.alloc(1514);
	}
	TimerManager timerManager = (TimerManager)LookupHelper.waitUntilPortalAvailable(naming,"TimerManager");
	
	try {
	    NetInit net = new jx.netmanager.NetInit(nic, timerManager, bufs);
	    naming.registerPortal(net, args[1]);
	} catch(Exception e) {throw new Error();}

    }
}


