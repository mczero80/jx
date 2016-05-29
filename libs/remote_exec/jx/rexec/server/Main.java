package jx.rexec.server;

import jx.rexec.*;

import jx.zero.*;
import jx.net.IPAddress;
import jx.net.NetInit;
import jx.net.UDPSender;
import jx.net.UDPReceiver;

import jx.net.UDPData;

import test.compiler.OnlineCompiler;

import jx.zero.debug.*;

import java.net.*;
import java.io.*;

public class Main {

    
    public static void main(String[] args) throws Exception {
	new Main(args);
    }

    static final boolean debugData = false;

    Main(String[] args) throws Exception {
	tcp(6666);
    }

    void udp(String[] args) throws Exception {
	String netName = args[0];

	final Naming naming = InitialNaming.getInitialNaming();

	NetInit net= (NetInit) LookupHelper.waitUntilPortalAvailable(InitialNaming.getInitialNaming(),netName);
	
	// prepare sender
	Memory sbuf = net.getUDPBuffer(50);
	
	// prepare receiver
	Memory[] bufs = new Memory[10]; 
	for(int i=0; i<10; i++) {
	    bufs[i] = net.getUDPBuffer(0);
	}
	UDPReceiver socket = net.getUDPReceiver(Encoder.REXEC_PORT, bufs);
	Memory rbuf = net.getUDPBuffer(0);
	Memory sbuf1;
	Domain createdDomain=null;
	for(;;) {
	    // receive request
	    //UDPData udpdata = socket.receive(rbuf);
	    UDPData udpdata = socket.receive1(rbuf);
	    rbuf = udpdata.mem;
	    System.out.println("RECEIVED PACKET:");
	    Dump.xdump(System.out, rbuf, udpdata.offset, udpdata.size-udpdata.offset);
	    
	    Decoder p = new Decoder(rbuf, udpdata.offset, udpdata.size-udpdata.offset);
	    
	    int magic = p.readInt();
	    Debug.out.println("MAGIC:"+ magic);
	    if (magic != Encoder.MAGIC) {
		Debug.out.println("WRONG MAGIC");
		continue;
	    }
	    int xid = p.readInt();
	    int cmd = p.readInt();	    
	    Debug.out.println("Command:"+ cmd);
	    if (cmd == Encoder.CMD_CREATEDOMAIN) {
		String domainName = p.readString();
		Debug.out.println("DomainName:"+ domainName);
		String startClassName = p.readString();
		Debug.out.println("StartClassName:"+ startClassName);
		
		// send reply
		//IPAddress srcAddr = socket.getSource(rbuf);
		//int srcPort = socket.getSourcePort(rbuf);
		IPAddress srcAddr = udpdata.sourceAddress;
		int srcPort = udpdata.sourcePort;
		UDPSender replySender = net.getUDPSender(Encoder.REXEC_PORT, srcAddr, srcPort);
		//sbuf1 = net.getUDPBuffer(50);
		Encoder e = new Encoder(sbuf);
		e.writeInt(Encoder.MAGIC);
		e.writeInt(xid);
		e.writeInt(Encoder.TRANSMISSION_OK);
		sbuf = replySender.send1(sbuf, 14+20+8,e.getLength()-(14+20+8));
		//sbuf = sbuf1;
				
	    } else if (cmd == Encoder.CMD_DATA) { 
		Memory data = p.readData();
		Debug.out.println("SIZE:"+ data.size());
		//Dump.xdump1(data, 0, data.size());
		
		//componentManager.registerLib(domainName+".zip",data);
		

	    } else if (cmd == Encoder.CMD_DESTROYDOMAIN) { 
		String domainName = p.readString();
		Debug.out.println("DomainName:"+ domainName);
		//naming.destroyDomain(domainName);		    
		//createdDomain.destroy();
	    } else if (cmd == Encoder.CMD_DUMPDOMAIN) { 
		String domainName = p.readString();
		Debug.out.println("DomainName:"+ domainName);
		//naming.destroyDomain(domainName);		    
		//createdDomain.dump();
	    }   
	}


    }
    /*
    void install() {
	ComponentManager  componentManager =  (ComponentManager)naming.lookup("ComponentManager");
	DomainManager  domainManager =  (DomainManager)naming.lookup("DomainManager");
	
	OnlineCompiler onc = new OnlineCompiler(InitialNaming.getInitialNaming());
	
	onc.compile(domainName+".jll",domainName+".jln",domainName+".zip","int");
	
	
	DomainStarter.createDomain(domainName,domainName+".jll", startClassName, 1 * 1024 * 1024, 20000);
    }
    */

    void tcp(int port) throws Exception {
	final Naming naming = InitialNaming.getInitialNaming();
	ComponentManager  componentManager =  (ComponentManager)naming.lookup("ComponentManager");
	MemoryManager  memoryManager =  (MemoryManager)naming.lookup("MemoryManager");
	ServerSocket ssock = new ServerSocket(port);
	// accept conections
	while (true) {
	    final Socket sock = ssock.accept();
	    InputStream istream = sock.getInputStream();
	    DataInputStream in = new DataInputStream(istream);
	    boolean finish = false;
	    while(! finish) {
		int cmd = in.readInt();
		Debug.out.println("Command:"+ cmd);
		switch(cmd) {
		case Encoder.CMD_INSTALLZIP:
		    String domainName = in.readUTF();
		    Debug.out.println("DomainName:"+ domainName);
		    String startClassName = in.readUTF();
		    Debug.out.println("StartClassName:"+ startClassName);
		    int size = in.readInt();
		    byte[] data = new byte[size];
		    Debug.out.println("Sizee:"+ size);
		    in.readFully(data);
		    Memory m = memoryManager.alloc(size);
		    m.copyFromByteArray(data, 0, 0, size);
		    componentManager.registerLib(domainName+".zip",m);
		    Debug.out.println("Installed:"+ domainName);
		    OnlineCompiler onc = new OnlineCompiler(InitialNaming.getInitialNaming());
		    onc.compile(domainName+".jll",domainName+".jln",domainName+".zip","int");
		    break;
		default:
		    Debug.out.println("ERROR");
		    finish=true;
		    break;
		}
	    }
	    sock.close();
	}
    }
}
