package jx.net.protocol.tcp;

import jx.net.*;
import jx.zero.debug.*;
import jx.zero.*;
import jx.net.protocol.ip.*;
import java.util.*;
import jx.buffer.multithread.MultiThreadList;
import jx.timer.TimerManager;
import jx.timer.TimerHandler;

/*
 * Verwaltet Verbindungen und verteilt Pakete an TCPSockets
 * Autor: Torsten Ehlers
 */


public class TCP implements IPConsumer, IPConsumer1, Runnable {

    static final boolean debug = false;
    static final boolean verbose = false;

    // Liste aller bestehenden Verbindungen
    private Vector tcpSockets;
    // Liste aller lauschenden Sockets
    private TCPSocket[] tcpServerSockets;

    // Pufferkreislauf  
    private MultiThreadList usableBufs;
    private MultiThreadList filledBufs;
    private static final int INITIAL_BUFFER_SIZE = 50;

    NetInit net;

    public TCP(IPProducer lowerProducer, jx.net.NetInit net, final TimerManager timerManager) {
	// bei der IP-Schicht anmelden
	lowerProducer.registerConsumer(this, "TCP");

	this.net = net;
	tcpSockets = new Vector();
	tcpServerSockets = new TCPSocket[65536];
    }
    public TCP(IPProducer1 lowerProducer, jx.net.NetInit net, final TimerManager timerManager ) {
	// bei der IP-Schicht anmelden
	usableBufs = new MultiThreadList();
	for ( int i = 0; i<INITIAL_BUFFER_SIZE; i++) {
	    IPData d = new IPData();
	    d.mem = net.getTCPBuffer1();
	    usableBufs.appendElement(d);
	}
	filledBufs = new MultiThreadList();

	this.net = net;
	tcpSockets = new Vector();
	tcpServerSockets = new TCPSocket[65536];

	lowerProducer.registerConsumer1(this, "TCP");

	final CPUManager cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	CPUState cyclops = cpuManager.createCPUState( new ThreadEntry(){
		public void run(){ // check retransmitt
		    cpuManager.setThreadName("CyclicRetransmitCheck");
		    while (true) {
			cpuManager.block();
			for (int i = 0; i < 65536; i++)
			    if (tcpServerSockets[i] != null)
				tcpServerSockets[i].processServerSocket();
			
			if (debug) Debug.out.println("check all ClientSockets");
			Enumeration e = tcpSockets.elements();
			TCPSocket comp;
			while (e.hasMoreElements()) {
			    comp = (TCPSocket) e.nextElement();
			    comp.processClientSocket();
			}
		    }
		}
	    });
	cpuManager.start(cyclops);
	timerManager.unblockInMillisInterval(cyclops, 3000,3000);

	new Thread(this, "TCP-Dispatcher").start();
    }

    // durchsucht die Liste der bestehenden Verbindungen nach einer passenden
    private TCPSocket findSocket(IPAddress si, IPAddress di, int sp, int dp) {
	Enumeration e = tcpSockets.elements();
	TCPSocket comp;
	
	while (e.hasMoreElements()) {
	    comp = (TCPSocket) e.nextElement();

	    if ((comp.remoteIP.equals(si)) && (comp.localIP.equals(di)) && (comp.remotePort == sp) && (comp.localPort == dp)) {
		return comp;
	    }
	}
	return null;
    }

    public Memory getTCPBuffer1() {
	return net.getTCPBuffer1();  
    }

    public jx.net.IPSender getIPSender(IPAddress dest) throws UnknownAddressException {
	return net.getIPSender(dest, IP.PROTO_TCP);  
    }


    // wird von den Sockets aufgerufen, um bestehende Verbindung zu registrieren
    public void registerSocket(TCPSocket s) {
	if (tcpSockets.contains(s)) throw new Error("TCP: Connection already registered");
	tcpSockets.addElement(s);
    }

    public void registerServerSocket(TCPSocket s) {
	if (tcpServerSockets[s.localPort] != null) {
	    if (tcpServerSockets[s.localPort] == s){
		if (debug) Debug.out.println("TCP: This ServerSocket is already registered on port " + s.localPort);
	    } else
		throw new Error("TCP: Port " + s.localPort + " already in use");
	} else {
	    tcpServerSockets[s.localPort] = s;
	    if (debug) Debug.out.println("TCP: ServerSocket registered on port " + s.localPort);
	}
    }


    public void unregisterSocket(TCPSocket s) {
	if (!tcpSockets.contains(s)) { Debug.out.println("TCP: connection not registered"); return;}
	tcpSockets.removeElement(s);
	if (debug) Debug.out.println("TCP: Socket unregistered on port " + s.localPort);
    }

    public void unregisterServerSocket(TCPSocket s) {
	if (tcpServerSockets[s.localPort] == null) throw new Error("TCP: Port " + s.localPort + " not in use");
	tcpServerSockets[s.localPort] = null;
	if (debug) Debug.out.println("TCP: ServerSocket unregistered on port " + s.localPort);
    }
		       
    // wird von IP aufgerufen, wenn TCP-Paket ankommt
    public Memory processIP(IPData data) {
	data.offset=0;
	data.size=data.mem.size();
	return processIP1(data);
    }

    public Memory processIP1(IPData data) {
	/* get free buffer */
	if (debug) Debug.out.println("IP packet received");
	IPData d = (IPData)usableBufs.nonblockingUndockFirstElement();
	if (d == null) {
	    Debug.out.println("jx.net.tcp: no buffer available, must drop a packet!");
	    d = (IPData) filledBufs.nonblockingUndockFirstElement();
	    if (d == null) throw new Error("where are my buffers gone?");
	}
	/* return mem of buffer and store received mem */
 	Memory ret = d.mem;
	//ret = ret.revoke();
	filledBufs.appendElement(data);
	if (debug)
	    if (ret == null)
		throw new Error ("ret == null");
    	return ret;
    }

    public void run() {
	while (true) {
	    // wait for packets
	    if (debug) Debug.out.println("Blocked on input queue (sz:#"+filledBufs.size()+")");
	    IPData data = (IPData)filledBufs.undockFirstElement();
	    if (data == null) throw new Error ("no filled Buffers");
	    if (debug) Debug.out.println("\nBlock on input queue released");

	    data = dispatch(data);

	    usableBufs.appendElement(data);
	}
    }
    final private IPData dispatch(IPData data){

	    IPAddress sourceIP = data.sourceAddress;
	    IPAddress destinationIP = data.destinationAddress;
	    TCPFormat tcpPacket = new TCPFormat(data, sourceIP, destinationIP);
	    int tcpSourcePort = tcpPacket.getSourcePort();
	    int tcpDestinationPort = tcpPacket.getDestinationPort();
	    TCPSocket sock;
	    

	    if (debug){
		Debug.out.println("TCP-Packet received from " +  sourceIP + "." + tcpSourcePort +  " to " + destinationIP + "." + tcpDestinationPort+" Flags:"+tcpPacket.Flags2String());
		byte[] dt = tcpPacket.getData();
		Debug.out.print("Data: ");
		if (dt.length == 0) Debug.out.print("none");
		else for (int i = 0; i< dt.length; i++) Debug.out.print((char)dt[i]);
		Debug.out.println("");
	    }
	    if (verbose)
		tcpPacket.dump();
	    if (!tcpPacket.isChecksumValid()) {
		tcpPacket.dump();
		Debug.out.println("TCP-Packet dropped due to invalid checksum");
		return data;
	    }
	    // gehoert das Paket zu einer bestehenden Verbindung? 
	    if ((sock = findSocket(sourceIP, destinationIP, tcpSourcePort, tcpDestinationPort)) != null) {
		if (debug) Debug.out.println("TCP: dispatched to socket");
		IPData d = new IPData();
		d.mem = sock.processTCP(data);
		return d;
	    }
	    
	    // gehoert nicht zu Verbindung. SYN an einen Port, an dem gelauscht wird? oder ACK (3. Teil des Handshakes!)
	    if (((sock = tcpServerSockets[tcpDestinationPort]) != null)  && (destinationIP.equals(sock.localIP))) {
		if (debug) Debug.out.println("TCP: dispatched to serversocket");
		IPData d = new IPData();
		d.mem = sock.processTCPServer(data);
		return d;
	    }

	    // Paket ungueltig, RST zurueckschicken
	    /* Entsprechende Methode fehlt noch */
	    Debug.out.println("SYN: " + tcpPacket.isSYNFlagSet());
	    Debug.out.println("Port: "+ tcpDestinationPort +" Socket: " + tcpServerSockets[tcpDestinationPort]);
	    if  (tcpServerSockets[tcpDestinationPort] != null) 
		Debug.out.println("IP " + destinationIP + "localIP of Socket: " + tcpServerSockets[tcpDestinationPort].localIP);
	    
	    Debug.out.println("TCP: unknown packet");
	    return data;
    }
}


