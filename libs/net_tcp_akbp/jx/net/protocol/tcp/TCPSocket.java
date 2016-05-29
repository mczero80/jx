package jx.net.protocol.tcp;

import jx.net.*;
import jx.zero.debug.*;
import jx.zero.*;
import jx.net.protocol.ip.*;
import java.util.*;
import jx.buffer.multithread.MultiThreadList;
import jx.timer.*;
import timerpc.*;
import java.io.IOException;
//import jx.net.protocol.ether.EtherFormat;

/* 
 * EINE TCP-Verbindung
 * Autor: Torsten Ehlers
 * redesigned by: Meik Felser
 */


public class TCPSocket implements jx.net.TCPSocket, Service {

    private static final boolean debug = false;
    private static final boolean NOSPLITTING = true;
    private static final boolean MEMORYCYCLE = false;
    private static final boolean COPY_AND_RETRANSMIT = true;
    private static final boolean LISTENQ = true;
    private static final int EtherFormatrequiresSpace=14 /*EtherFormat.requiresSpace()*/;

    IPAddress remoteIP;
    IPAddress localIP;
    int remotePort;
    int localPort;
  
    // Aktuelle Sequence- und Ack-Number
    int seq;
    int ack;
  
    int remoteWindowSize;
    int localWindowSize;
    private int     currentReceivedAck;

    // Referenz aufs TCP-Objekt, um sich da zu registrieren
    TCP tcp;
    private IPSender lowerLayer;

    // Pufferkreislauf  
    private MultiThreadList usableBufs;
    private MultiThreadList listenQ;
    // Empfangs- und Retransmit (= Sende)-Liste  
    private MultiThreadList inputBufs;
    private Vector retransmitQueue;

    // Status des Sockets
    AtomicVariable state;
    CPUState waitingThread = null;

    private int     timer = 0;  // used to wait 2MSL in TIME_WAIT and for imeouts
//    private CAS     retransmit_CAS; 
//    private boolean do_retransmit;
    Memory[] bufs4newSocket = null;
    TCPSocket newClientSocket = null;
    
    public static final  TCPSocketState CLOSED      = new TCPSocketState();
    public static final  TCPSocketState LISTEN      = new TCPSocketState();
    public static final  TCPSocketState SYN_RCVD    = new TCPSocketState();
    public static final  TCPSocketState SYN_SENT    = new TCPSocketState();
    public static final  TCPSocketState ESTABLISHED = new TCPSocketState();
    //  public static final byte CLOSE_WAIT = 6;
    // Status CLOSE_WAIT nicht implementiert, da Half-Close der Gegenseite nicht unterstuetzt
    public static final  TCPSocketState LAST_ACK    = new TCPSocketState();
    public static final  TCPSocketState FIN_WAIT_1  = new TCPSocketState();
    public static final  TCPSocketState FIN_WAIT_2  = new TCPSocketState();
    public static final  TCPSocketState CLOSING     = new TCPSocketState();
    public static final  TCPSocketState TIME_WAIT   = new TCPSocketState();
    
    private static final int RETRANSMIT_INTERVAL = 2000;
    public static final int  MAX_RETRANSMITS = 5;
    private static final int CLOSE_TIME = 100;
    private static final int TIMEOUT = 100;
    private static final int MAX_LISTENQ_LENGTH = 10;
    private static final int MAX_RETRANSMITQ_LENGTH = 10;
    CPUManager cpuManager;   
    TimerManager timerManager;

    public TCPSocket(TCP tcp, int localPort, IPAddress ip, Memory[] bufs) {
	this(tcp, localPort, ip);
	if (MEMORYCYCLE) {
	    usableBufs = new MultiThreadList();
	    for ( int i = 0; i<bufs.length; i++) {
		IPData d = new IPData();
		d.mem = bufs[i];
		usableBufs.appendElement(d);
	    }
	}
    }

    private TCPSocket(TCP tcp, IPSender lowerLayer, int localPort, IPAddress ip, Memory[] bufs) {
	this(tcp, localPort, ip, bufs);
	this.lowerLayer = lowerLayer;
    }

    public TCPSocket(TCP tcp, int localPort, IPAddress ip) {
	this.tcp = tcp;
	this.localPort = localPort;
	if (debug) Debug.out.println("Creating socket on local port " + this.localPort);
	this.localIP = ip;
	inputBufs = new MultiThreadList();
	listenQ = new MultiThreadList();
	retransmitQueue = new Vector();

	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	state = cpuManager.getAtomicVariable();
	state.set(CLOSED);
//	remoteWindowSize = cpuManager.getAtomicVariable();
//	remoteWindowSize.set(new Integer(0));

//	do_retransmit = false;
//	retransmit_CAS = cpuManager.getCAS("jx/net/protocol/tcp/TCPSocket", "do_retransmit");
//	retransmit_CAS.casBoolean(this, true, false);

	timerManager = (TimerManager) InitialNaming.getInitialNaming().lookup("TimerManager");
	localWindowSize = 1024; //hack
    }

    // the sequence number is initialized using the system runtime
    private int getInitialSequenceNumber() {
	if (timerManager == null) Debug.out.println("Timerman is null");
	return timerManager.getTimeInMillis();
    }
    
    final static  String SocketState2String(TCPSocketState state) {
	if (state == CLOSED) return "CLOSED";
	else if (state == LISTEN)      return "LISTEN";
	else if (state == SYN_RCVD)    return "SYN_RCVD";
	else if (state == SYN_SENT)    return "SYN_SENT";
	else if (state == ESTABLISHED) return "ESTABLISHED";
	else if (state == LAST_ACK)    return "LAST_ACK";
	else if (state == FIN_WAIT_1)  return "FIN_WAIT_1";
	else if (state == FIN_WAIT_2)  return "FIN_WAIT_2";
	else if (state == CLOSING)     return "CLOSING";
	else if (state == TIME_WAIT)   return "TIME_WAIT";
	else                           return "UNKNOWN";
    }
    
    void setSocketState(TCPSocketState state) {
	if (debug)  Debug.out.println("State changed from "+ SocketState2String(getSocketState()) +" to " + SocketState2String(state));
	//this.state.set(state);
	this.state.atomicUpdateUnblock(state, waitingThread);
    }
    
    public TCPSocketState getSocketState() {
	return (TCPSocketState)state.get();
    }
    
    void setSequenceNumber(int seq) {
	this.seq = seq;
    }

    void setRemoteWindowSize(int  sz) {
	remoteWindowSize = sz;
    }

    void setAckNumber(int ack) {
	this.ack = ack;
    }
  
    public void registerRemoteIP(IPAddress remoteIP) {
	this.remoteIP = remoteIP;
    }

    public void registerRemotePort(int port) {
	remotePort = port;
    }

    private void sendHeaderOnlyPacket(int flags) {
	if (NOSPLITTING)
	    sendHeaderOnlyPacket1(flags);
	else
	    if (!MEMORYCYCLE)
		throw new Error("SPLITTING requires MEMORYCYCLE");
	    else
		sendHeaderOnlyPacket0(flags);
    }

    public void sendHeaderOnlyPacket0(int flags) {
	IPData d = (IPData)usableBufs.undockFirstElement();
	if (d == null) throw new Error ("no free Buffers");
	Memory pack = d.mem;
	Memory arr[] = new Memory[2];
	pack.split2(20, arr); // Hack. Nur Header = 20 Bytes ohne Options
	pack = arr[0];
	TCPFormat packet = new TCPFormat(pack, localIP, remoteIP);
	
	packet.insertFlags(flags);
	packet.insertSourcePort(localPort);
	packet.insertDestinationPort(remotePort);
	packet.insertAcknowledgmentNumber(ack);
	packet.insertSequenceNumber(seq);
	packet.insertWindowSize(localWindowSize);
	packet.computeHeaderLength();
	packet.insertChecksum();

	if (!packet.areFlagsSet(TCPFormat.ACK))  seq++;

	// Paket versenden und von IP erhaltenes Paket dafuer wieder in Frei-Liste haengen
	d.mem =  lowerLayer.send(pack);
	usableBufs.appendElement(d);
    }	

    public void sendHeaderOnlyPacket1(int flags) {
	IPData data;
	if (MEMORYCYCLE) {
	    data = (IPData)usableBufs.undockFirstElement();
	    if (data == null) throw new Error ("no free Buffers");
	} else {
	    data = new IPData();
	    data.mem = tcp.getTCPBuffer1();
	}

	data.offset = EtherFormatrequiresSpace + IPFormat.requiresSpace();   
	data.size = 20;
	TCPFormat packet = new TCPFormat(data, localIP, remoteIP);
	
	packet.insertFlags(flags);
	packet.insertSourcePort(localPort);
	packet.insertDestinationPort(remotePort);
	packet.insertAcknowledgmentNumber(ack);
	packet.insertSequenceNumber(seq);
	packet.insertWindowSize(localWindowSize);
	packet.computeHeaderLength();
	packet.insertChecksum();

	if (!packet.areFlagsSet(TCPFormat.ACK))  seq++;

	Memory back = lowerLayer.send1(data.mem, data.offset, data.size);
	if (MEMORYCYCLE) {
	    // von IP erhaltenes Paket wieder in Frei-Liste haengen
	    data.mem = back;
	    usableBufs.appendElement(data);
	}
	
    }	

    private Memory insertConnectionReq(IPData d){
	if (listenQ.size() >= MAX_LISTENQ_LENGTH) {
	    if (debug) Debug.out.println("listenQ full droping connection request");
	    return d.mem;
	}
	if (MEMORYCYCLE) {
	    IPData newD = (IPData)usableBufs.nonblockingUndockFirstElement();
	    if (newD == null) {
		if (debug) Debug.out.println("TCPSocket on port " + localPort + ": No buffer available, no queing of incomming connection");
	    } else {
		listenQ.appendElement(d);
		return newD.mem;
	    }
	} else {
	    listenQ.appendElement(d);
	    return tcp.getTCPBuffer1();
	}
	return d.mem;   // drop packet
    }


    public Memory processTCPServer(IPData d){
	Memory ret = null;
	TCPSocketState st = getSocketState();

	if (LISTENQ) {
	    TCPFormat packet = new TCPFormat(d, d.destinationAddress, d.sourceAddress);
	    if (packet.areFlagsSet(TCPFormat.SYN) && st != LISTEN) {
		if (debug) Debug.out.println("TCP: storing connection request");
		return  insertConnectionReq(d);
	    } 
	}
	if (st == LISTEN)           ret = listen(d);
	else if (st == SYN_RCVD)    ret = syn_rcvd(d);
        else if (st == SYN_SENT)    ret = syn_sent(d);
	else if (st == FIN_WAIT_1)  ret = fin_wait_1(d);
	else if (st == FIN_WAIT_2)  ret = fin_wait_2(d);
	else if (st == LAST_ACK)    ret = last_ack(d);
	else if (st == ESTABLISHED) { ret = d.mem; Debug.out.println("server socket drops a packet");}
//	else if (st == CLOSED)
//	else if (st == CLOSING)
//	else if (st == TIME_WAIT)
	else throw new Error("unexpected state:"+SocketState2String(st));
	return ret;
    }
    
    public Memory processTCP(IPData d){
	Memory ret = null;

	TCPSocketState st = getSocketState();
	if (st == SYN_SENT)          ret = syn_sent(d);
	else if (st == ESTABLISHED)  ret = established(d);
	else if (st == FIN_WAIT_1)   ret = fin_wait_1(d);
	else if (st == FIN_WAIT_2)   ret = fin_wait_2(d);
	else if (st == LAST_ACK)     ret = last_ack(d);
//	else if (st == LISTEN)
//	else if (st == SYN_RCVD)
	else if (st == CLOSING)      ret = closing(d);
	else if (st == TIME_WAIT)    ret = d.mem;
	else if (st == CLOSED)       ret = d.mem;
	else throw new Error("unexpected state:"+SocketState2String(st));
	return ret;
    }

    public void processServerSocket(){
	if (debug){
	    Debug.out.print ("ServerSocket on: "+localIP+":"+localPort+" = "+SocketState2String(getSocketState()));	
	    if (LISTENQ)
		if (listenQ.size() > 1)
		    Debug.out.print (" "+listenQ.size()+" waiting connections");
	    Debug.out.println("");
	}
	TCPSocketState st = getSocketState();

	if (st == SYN_RCVD) {  
	    if (timeout(TIMEOUT)==true) {
		sendHeaderOnlyPacket(TCPFormat.RST);
		setSocketState(CLOSED);
	    } 
	}
//	     else if (st == ESTABLISHED)  
//	     else if (st == TIME_WAIT) 
//	     else if (st == SYN_SENT) 
//           else if (st == CLOSED)
//           else if (st == CLOSING)
//	     else if (st == LISTEN)
//           else if (st == LAST_ACK)
//	     else if (st == FIN_WAIT_1)
//	     else if (st == FIN_WAIT_2)
    }
 
    public void processClientSocket(){
	if (debug) Debug.out.println (localIP+":"+localPort+" -> "
				      +remoteIP+":"+remotePort+" = "
				      +SocketState2String(getSocketState()));

	TCPSocketState st = getSocketState();

	if (st == ESTABLISHED) { 
	    checkRetransmit();
	} else if (st == TIME_WAIT) {
	    time_wait();
	} else if (st == SYN_SENT) {
	    if (timeout(TIMEOUT)==true) setSocketState(CLOSED);
	} else if (st == SYN_RCVD) {  
	    if (timeout(TIMEOUT)==true) {
		sendHeaderOnlyPacket(TCPFormat.RST);
		setSocketState(CLOSED);
	    } 
	} else if (st == FIN_WAIT_2) {
	    if (timeout(TIMEOUT)==true){
		sendHeaderOnlyPacket(TCPFormat.RST);
		setSocketState(CLOSED);
	    }
	} else if (st == CLOSED) {
	    tcp.unregisterSocket(this);
	}
//           else if (st == CLOSING)
//	     else if (st == LISTEN)
//           else if (st == LAST_ACK)
//	     else if (st == FIN_WAIT_1)
    }
    
    
    public void open(IPAddress remoteIP, int remotePort) throws UnknownAddressException, java.io.IOException {
	if (getSocketState() != CLOSED) throw new Error("open can only be called on closed sockets!");
	this.remoteIP = remoteIP;
	this.remotePort = remotePort;

	try {
	    if (remoteIP == null) throw new Error("TCPSocket.open: no remote IP given");
	    lowerLayer = tcp.getIPSender(remoteIP);
	} catch (UnknownAddressException e) {
	    Debug.out.println("TCPSocket.open: IPSender reported invalid IPAddress");
	    throw e;
	}

	tcp.registerSocket(this);

	seq = getInitialSequenceNumber();
			
	sendHeaderOnlyPacket(TCPFormat.SYN);
		
	if (debug) Debug.out.println("TCPSocket.open: SYN-Packet sent");
	
	setSocketState(SYN_SENT);
	waitingThread = cpuManager.getCPUState();
	state.blockIfEqual(SYN_SENT);
	waitingThread = null; // fix it: beware of race condition!!
	if (getSocketState() != ESTABLISHED) {
	    sendHeaderOnlyPacket(TCPFormat.RST);
	    throw new java.io.IOException("Verbindung konnte nicht aufgebaut werden. (keine Reaktion auf SYN??)\n[TCPSocket.open: state(="+SocketState2String(getSocketState())+") != ESTABLISHED]");
	}
    }

    public void close() throws IOException{
	TCPSocketState st = getSocketState();
	if (debug) Debug.out.println("TCPSocket.close called in state: "+SocketState2String(st));

	if (st == SYN_RCVD || st == ESTABLISHED) {
	    sendHeaderOnlyPacket(TCPFormat.FIN);
	    if (debug) Debug.out.println("TCPSocket.close: FIN-Packet sent");
	    setSocketState(FIN_WAIT_1);
	    waitingThread = cpuManager.getCPUState();
	    state.blockIfEqual(FIN_WAIT_1);
	    state.blockIfEqual(FIN_WAIT_2);
	    waitingThread = null; // fix it: beware of race condition!!
	    if (getSocketState() != CLOSED) {
		sendHeaderOnlyPacket(TCPFormat.RST);
		throw new java.io.IOException("Verbindung konnte nicht geschlossen werden.\n[TCPSocket.open: state(="+SocketState2String(getSocketState())+") != CLOSED]");
	    }

	} else if (st == LISTEN || st == SYN_SENT) {
	    setSocketState(CLOSED);
	} else if (st == LAST_ACK || st == FIN_WAIT_1 || st == FIN_WAIT_2 || st == CLOSING || st == TIME_WAIT) {
	    throw new Error(SocketState2String(st)+": setSocketState(CLOSED); return;");
	} else if (st == CLOSED || st == CLOSING) { 
	    return;
	} else 
	    throw new Error (" a terrible Error occured");
    }
		
    // wartet auf eine Verbindung und blockiert sich, bis Verbindung hergestellt. Liefert dann neuen Socket
    public jx.net.TCPSocket accept(Memory[] newbufs) throws java.io.IOException {
	
	if (getSocketState() != CLOSED) throw new Error("Accept can only be called on closed sockets!");
	
	if (debug) Debug.out.println("TCPSocket.accept: Called to accept connections on port " + localPort);
	
	setSocketState(LISTEN);
	
	if (LISTENQ) {
	    // search for pending connections
	    IPData d = (IPData)listenQ.nonblockingUndockFirstElement();
	    if (d != null) {
		if (debug) Debug.out.println("TCPSocket on port " + localPort + ": using waiting connection from "+d.sourceAddress);
		d.mem = listen(d);
		if (MEMORYCYCLE)
		    usableBufs.appendElement(d);
	    }
	}
	bufs4newSocket = newbufs;

	// bei TCP als ServerSocket anmelden
	tcp.registerServerSocket(this);
	
	waitingThread = cpuManager.getCPUState();  // beware of race conditions!!
	state.blockIfEqual(LISTEN);
	state.blockIfEqual(SYN_RCVD);
	waitingThread = null;

	if (getSocketState() != ESTABLISHED)
	    throw new java.io.IOException("TCPSocket.listen: state (="+SocketState2String(getSocketState())+") != ESTABLISHED");
	
	if (newClientSocket == null)
	    throw new Error("no client Socket created!");
	TCPSocket ret = newClientSocket;
	newClientSocket = null;    // beware of race condition
	setSocketState(CLOSED); // diesen Serversocket zurueck auf CLOSED 
	return ret;
    }
    private TCPSocket createClientSocket(Memory[] newbufs){
  	TCPSocket ret = new TCPSocket(tcp, lowerLayer, localPort, localIP, newbufs);
	bufs4newSocket = null;
	ret.setSocketState(ESTABLISHED);
	ret.setSequenceNumber(seq);
	ret.setAckNumber(ack);
	ret.setRemoteWindowSize(remoteWindowSize);
	ret.registerRemoteIP(remoteIP);
	ret.registerRemotePort(remotePort);
	tcp.registerSocket(ret);
 	return ret;
    }
    
    /** deletes old packets in the RetransmitQ 
     *  and decreases the remoteWinSz for each nonACKed Packed 
     */
    private void cleanUpRetransmitQueue(int receivedAck) {

	if (debug) Debug.out.println("cleanUpRetransmitQueue ack:"+receivedAck+" Qlen"+retransmitQueue.size());

	if (receivedAck > currentReceivedAck) currentReceivedAck = receivedAck;
	// else if (receivedAck == currentReceivedAck) return;  /* handled by caller */
	else return;

	for (int i=0; i<retransmitQueue.size(); i++) {
	    TCPData d = (TCPData) retransmitQueue.elementAt(i);
	    TCPFormat packet = new TCPFormat(d);
	    if ((d.getRetransmitCounter() > MAX_RETRANSMITS) || (packet.getSequenceNumber() < receivedAck)) {
		if (debug) {
		    if (d.getRetransmitCounter() > MAX_RETRANSMITS)
			Debug.out.println("ccleanUpRetransmitQueue: MAX_RETRANSMITS reached ["+packet.getSequenceNumber()+"] --> removing packet from queue");
		    else
			Debug.out.println("cleanUpRetransmitQueue: ["+packet.getSequenceNumber()+"] removing packet from queue");
			
		}
		retransmitQueue.removeElementAt(i);
		i--;
		// release buffer
		if (MEMORYCYCLE)
		    usableBufs.appendElement(d);
	    } else if (packet.getSequenceNumber() > receivedAck) { /* decrease RWinSz for all unACKed packages */
		int size = (packet.length() - packet.getHeaderLength());
		remoteWindowSize -= size;
	    }
	}
	if (debug) dumpRetransmitQ();
    }
 
    private void dumpRetransmitQ(){
	Debug.out.print("[");
 	for (int i=0; i<retransmitQueue.size(); i++) {
	    TCPData d = (TCPData) retransmitQueue.elementAt(i);
	    TCPFormat packet = new TCPFormat(d);
	    Debug.out.print(packet.getSequenceNumber()+", ");
	}
	Debug.out.print("]\n");
   }

    private void forceRetransmit(int seqNr) {
	int qlen = retransmitQueue.size();
	for (int i =0; (i < qlen && i < retransmitQueue.size()); i++) {
	    TCPData data = (TCPData) retransmitQueue.elementAt(i);
	    TCPFormat packet = new TCPFormat(data);
	    if (packet.getSequenceNumber() == seqNr) {
		TCPData del = (TCPData)retransmitQueue.remove(i);
		if (del != data)
		    throw new Error("wrong element deleted from retransmit Q");
		/* if (debug) */ Debug.out.println("forceRetransmit ["+seqNr+"]");
		retransmit(data);
		return;
	    }
	}
    }

    private void checkRetransmit() {
	int       currentTime = timerManager.getTimeInMillis();
	if (retransmitQueue.size() == 0)
	    return;
	int qlen = retransmitQueue.size();
	for (int i =0; (i < qlen && i < retransmitQueue.size()); i++) {
	    TCPData data = (TCPData) retransmitQueue.elementAt(i);
	    TCPFormat packet = new TCPFormat(data);
	    //Debug.out.println(" pckTS:"+data.getRetransmitTimestamp()+" cutTime"+currentTime+" dif:"+compareUnsigned(data.getRetransmitTimestamp(), currentTime));
	    if ((compareUnsigned(data.getRetransmitTimestamp(), currentTime) <= 0) 
		&& (data.getRetransmitCounter() < MAX_RETRANSMITS)) {
		TCPData del = (TCPData)retransmitQueue.remove(i);
		if (del != data)
		    throw new Error("wrong element deleted from retransmit Q");
		i--;
		qlen--;
		retransmit(data);
	    }
	}
    }

    /* retransmitts packet data and appends it to the retransmittQ */
    private void retransmit(TCPData data){
        if (debug)  Debug.out.println("retransmit: queue length is "+retransmitQueue.size());
	if (retransmitQueue.size() == 0)
	    return;

	/* retransmit the oldest packet, this is mostly sufficient */
	TCPFormat packet = new TCPFormat(data);
	int size = (packet.length() - packet.getHeaderLength());
	if (remoteWindowSize < size) {
	    Debug.out.println("retransmit: retransmitting aborted due to remote window size");
	    return;
	}
	if (debug) Debug.out.println("retransmit: retransmitting packet ["+packet.getSequenceNumber()+"]");
	
	data.setRetransmitCounter(data.getRetransmitCounter() + 1);
	data.setRetransmitTimestamp(data.getRetransmitTimestamp() + RETRANSMIT_INTERVAL);
	TCPData d = null;
	// Paket kopieren
	if (MEMORYCYCLE) {
	    IPData ipd = (IPData)usableBufs.nonblockingUndockFirstElement();
	    if (ipd == null) 
		Debug.out.println("TCPSocket on port " + localPort + ": No buffer available, no re-retransmition");
	    else { 
		d = new TCPData(ipd);
		d.mem.copyFromMemory(data.mem,0,0,data.mem.size());
	    }
	} else {
	    d = new TCPData(new IPData());
	    d.mem = tcp.getTCPBuffer1();
	    d.mem.copyFromMemory(data.mem,0,0,data.mem.size());
	}
	
	Memory back = lowerLayer.send1(data.mem,data.offset,data.size);
	remoteWindowSize -= size;
	if (MEMORYCYCLE) {
	    data.mem = back;
		usableBufs.appendElement(data);
	}
	
	// Paket in Retransmitqueue einhaengen
	if (d!=null) {
	    if (MEMORYCYCLE)
		usableBufs.appendElement(d);
		else {
		    data.mem = d.mem;  // because d has no valid offset/size info
		    retransmitQueue.addElement(data);
		}
	}
    }

    private int compareUnsigned(int a, int b) {
	int   counter = 31;
	byte  bita, bitb;
	
	while (counter >= 0) {
	    bita = (byte)((a >> counter) & 0x1);
	    bitb = (byte)((b >> counter) & 0x1);
	    if (bita > bitb) return 1;
	    else if (bita < bitb) return -1;
	    else counter--;
	}
	return 0;
    }
    
    // fuer jeden Zustand ne Methode, vgl. Stevens, S. 241
    private Memory listen(IPData d) {
	if (debug) Debug.out.println("Accept received packet from input queue");
	registerRemoteIP(d.sourceAddress);
	
	TCPFormat packet = new TCPFormat(d, localIP, remoteIP);
	
	// SYN-Paket
	if (packet.areFlagsSet(TCPFormat.SYN)) {
	    ack = packet.getSequenceNumber() + 1;
	    seq = getInitialSequenceNumber();
	    remotePort = packet.getSourcePort();
	    remoteWindowSize = packet.getWindowSize();
	    if (debug) Debug.out.println("TCPSocket.listen: SYN-Packet received from port " + remotePort);
	    
	    try {
		if (remoteIP == null) throw new Error("TCPSocket: no remote IP registered");
		lowerLayer = tcp.getIPSender(remoteIP);
	    } catch (UnknownAddressException e) {
		Debug.out.println("TCPSocket.listen: Unknown IP Address reported by getIPSender()!");
		throw new Error();
	    }
	    
	    sendHeaderOnlyPacket(TCPFormat.SYN | TCPFormat.ACK);
	    if (debug) Debug.out.println("TCPSocket.listen: SYN/ACK-Packet sent");
	    setSocketState(SYN_RCVD);
	} else
	    if (debug) Debug.out.println("TCPSocket.listen: unexpected Packet received");
	return d.mem;	    
    }
    
    private Memory syn_rcvd(IPData d) {
	TCPFormat packet = new TCPFormat(d, localIP, remoteIP);
      
	// Reset
	if (packet.isRSTFlagSet() && packet.getAcknowledgmentNumber() == seq) {
	    setSocketState(LISTEN);
	}
	// korrekter 3. Teil des Handshake
	else if (packet.isACKFlagSet() && packet.getAcknowledgmentNumber() == seq) {
	    if (debug) Debug.out.println("TCPSocket.syn_rcvd: ACK-Packet received");
	    newClientSocket = createClientSocket(bufs4newSocket);
	    setSocketState(ESTABLISHED);
	} else { 	  // irgendein anderes Paket
	    if (debug) {
		Debug.out.println("Syn_rcvd: unexpected Packet received");
		packet.dump();
	    }
	}
	return d.mem;
    }
    
    private Memory syn_sent(IPData d) {
	if (debug) Debug.out.println("In syn_sent()");

	TCPFormat packet = new TCPFormat(d, localIP, remoteIP);
	    
	// RST/ACK bekommen
	if ((packet.isRSTFlagSet()) && (packet.getAcknowledgmentNumber() == seq)) {
	    Debug.out.println("RST received: nobody is listening!");
	    setSocketState(CLOSED);
	}
	// korrektes SYN/ACK bekommen
	else if ((packet.areFlagsSet(TCPFormat.ACK | TCPFormat.SYN)) && (packet.getAcknowledgmentNumber() == seq)) {
	    if (debug) Debug.out.println("TCPSocket.syn_sent: SYN/ACK-Packet received");
	    remoteWindowSize = packet.getWindowSize();
	    ack = packet.getSequenceNumber() + 1;
	    sendHeaderOnlyPacket(TCPFormat.ACK);
	    setSocketState(ESTABLISHED);
	}
	else
	    if (debug) Debug.out.println("syn_sent: unexpected Packet received");
 	return d.mem;
    }


    private Memory established(IPData d) {

	TCPFormat packet = new TCPFormat(d);

	// FIN received	    
	// normal transition for server
	if (packet.isFINFlagSet()) {
	    if (debug) Debug.out.println("****Established, FIN-Paket");
	    ack++;
	    sendHeaderOnlyPacket(TCPFormat.ACK | TCPFormat.FIN);
	    setSocketState(LAST_ACK);
	    // may a FIN segment contain data???
	    return d.mem;
	}
	if (packet.isRSTFlagSet()) {
	    if (debug) Debug.out.println("****Established, RST-Paket");
	    ack++;
	    setSocketState(TIME_WAIT);
	    return d.mem;
	}
	else {	// anderes Paket
	    // richtige Reihenfolge der ankommenden Pakete?
	    if (packet.getSequenceNumber() == ack) {
		if (debug) Debug.out.print("****Established, normales Paket, richtige Reihenfolge");
		
		if (packet.isACKFlagSet()){
		    if (debug) Debug.out.print(", ACK");
		    if (packet.getAcknowledgmentNumber() >= currentReceivedAck || packet.getAcknowledgmentNumber() == seq) 
			remoteWindowSize = packet.getWindowSize();
		    if (packet.getHeaderLength() == packet.length()) {  // reines ACK-Paket
			if (debug) Debug.out.println(" sonst nix");
			if ( packet.getAcknowledgmentNumber() ==  currentReceivedAck) 
			    forceRetransmit(currentReceivedAck);
			else
			    cleanUpRetransmitQueue(packet.getAcknowledgmentNumber()); // bestaetigte Pakete aus entfernen
			return d.mem;
		    } else
			cleanUpRetransmitQueue(packet.getAcknowledgmentNumber()); // bestaetigte Pakete aus entfernen
		}
		// Datenpaket
		if (debug) Debug.out.println(", Datenpaket");
		ack += (packet.length() - packet.getHeaderLength());
		
		// Paket hochreichen
		Memory ret = null;
		if (MEMORYCYCLE) {
		    IPData newD = (IPData)usableBufs.nonblockingUndockFirstElement();
		    if (newD == null) {
			Debug.out.println("TCPSocket on port " + localPort + ": No buffer available, data dropped, not ACKed");
			return d.mem;
		    }
		    ret = newD.mem;
		} else 
		    ret = tcp.getTCPBuffer1();
		inputBufs.appendElement(d);
		
		// Paket bestaetigen
		sendHeaderOnlyPacket(TCPFormat.ACK);
		return ret;
		
	    } else if (packet.getSequenceNumber() < ack) {
		Debug.out.println("****Established, resent data -> verwerfen");
		if (debug) Debug.out.println("                  ist:"+packet.getSequenceNumber());
		if (debug) Debug.out.println("                 soll:"+ack);
		sendHeaderOnlyPacket(TCPFormat.ACK);
		return d.mem;
	    } else /*if (packet.getSequenceNumber() > ack) */ {
		Debug.out.println("****Established, out-of-order -> verwerfen");
		if (debug) Debug.out.println("                  ist:"+packet.getSequenceNumber());
		if (debug) Debug.out.println("                 soll:"+ack);
		return d.mem;
	    }
	}
    }

    private Memory last_ack(IPData d) {
	TCPFormat  packet = new TCPFormat(d);
	
	// ACK received
	// normal transition for server
	if (packet.isACKFlagSet() && (packet.getAcknowledgmentNumber() == seq)) {
	    setSocketState(CLOSED);
	}
	else
	    if (debug) Debug.out.println("last_ack: unexpected Packet received");
 	return d.mem;
    }

    private Memory fin_wait_1(IPData d) {
	TCPFormat packet = new TCPFormat(d, localIP, remoteIP);
	
	if (packet.getAcknowledgmentNumber() != seq) {
	    if (debug) Debug.out.println("fin_wait_1: rcvd packet with bad ack");
	}
	else if (packet.areFlagsSet(TCPFormat.ACK | TCPFormat.FIN)) {
	    ack++;
	    sendHeaderOnlyPacket(TCPFormat.ACK);
	    setSocketState(TIME_WAIT);
	}
	else if (packet.areFlagsSet(TCPFormat.FIN)) {
	    ack++;
	    sendHeaderOnlyPacket(TCPFormat.ACK);
	    setSocketState(CLOSING);
	}
	else if (packet.areFlagsSet(TCPFormat.ACK)) {
	    setSocketState(FIN_WAIT_2);
	}
	else 
	    if (debug) Debug.out.println("fin_wait_1: unexpected Packet received");
	
	return d.mem;
    }

    private Memory fin_wait_2(IPData d) {
	TCPFormat packet = new TCPFormat(d, localIP, remoteIP);
	
	if (packet.areFlagsSet(TCPFormat.FIN)) {
	    ack++;
	    sendHeaderOnlyPacket(TCPFormat.ACK);
	    setSocketState(TIME_WAIT);
	} else
	    if (debug) Debug.out.println("fin_wait_2: unexpected Packet received");
	return d.mem;
    }

    private Memory closing(IPData d) {
	if (debug) Debug.out.println("In closing()");
	TCPFormat packet = new TCPFormat(d, localIP, remoteIP);
	
	if (packet.getAcknowledgmentNumber() != seq) {
	    if (debug) Debug.out.println("closing: rcvd packet with bad ack");
	}
	else if (packet.areFlagsSet(TCPFormat.ACK)) {
	    setSocketState(TIME_WAIT);
	}
	return d.mem;
    }

    private void time_wait() {
	if (debug) Debug.out.println("In Time_wait()");
	if (timeout(CLOSE_TIME)==true)
  	    setSocketState(CLOSED);
    }
    
    /* set a new timeoutvalue if timer == 0 (false is returned)
     * else the timer is decreases (returns true if the timelimit is exceeded else false
     */
    private boolean timeout(int initvalue) {
	int currentTime = timerManager.getTimeInMillis();
	if (debug) Debug.out.println("timeout: timer:"+timer+" curtime:"+currentTime+" diff:"+compareUnsigned(timer , currentTime));
	if ( timer == 0) {
	    timer = currentTime + initvalue;
	    return false;
	} else if ((compareUnsigned(timer , currentTime) <= 0)) {
	    timer = 0;
	    return true;
	} else
	    return false;
    }
    
    /*  
	public Memory processTCP(IPData data) {
	
	// einen freien Buffer zum zurueckgeben finden
	Buffer buf = usableBufs.nonblockingUndockFirstElement();
	if (buf == null) {
	Debug.out.println("TCPSocket on port " + localPort + ": No buffer available, packet dropped");
	return data.mem;
	}
	
	Memory ret = buf.getData();
	//ret = ret.revoke();
	buf.setData(data.mem);
	buf.setMoreData(data);
	filledBufs.appendElement(buf);
    
	return ret;
	}
    */


    public void send(byte data) throws IOException {
	byte[] byteArr = new byte[1];
	byteArr[0] = data;
	send(byteArr);
    }
  
    public void send(byte[] byteArr) throws IOException {
	if (NOSPLITTING)
	    send1(byteArr);
	else
	    if (!MEMORYCYCLE)
		throw new Error("SPLITTING requires MEMORYCYCLE");
	    else
		send0(byteArr);
    }

    public void send0(byte[] byteArr) throws IOException  {
	if (getSocketState() != ESTABLISHED) throw new IOException("send can only be called on open sockets");
	if (debug) Debug.out.println("Entered send() method");

	if (debug) Debug.out.println("local port " + this.localPort);

	int size = byteArr.length;

	if (size > 1400) throw new Error("Send: Byte array size exceeds 1400 Bytes");

	IPData d = (IPData)usableBufs.undockFirstElement();
	if (d == null) throw new Error ("no free Buffers");
	Memory mem  = d.mem;
	//	if (mem.getSize() < 1514) 
	Memory arr[] = new Memory[2];
	mem.split2(20 + size, arr); // Hack. Header +  Datenbytes
	mem = arr[0];
	
	TCPFormat sendPacket = new TCPFormat(mem, localIP, remoteIP);
	sendPacket.insertFlags(TCPFormat.PSH | TCPFormat.ACK);
	sendPacket.insertSourcePort(localPort);
	sendPacket.insertDestinationPort(remotePort);
	sendPacket.insertAcknowledgmentNumber(ack);
	sendPacket.insertSequenceNumber(seq);
	sendPacket.insertWindowSize(localWindowSize);
	sendPacket.computeHeaderLength();
	sendPacket.insertData(byteArr);
	sendPacket.insertChecksum();

	//	sendPacket.dump();
	
	d.mem = lowerLayer.send(mem);
	usableBufs.appendElement(d);

	seq += size;
	if (debug) Debug.out.println("Sent: Seq after send is " + seq);

	// Paket kopieren und in Retransmitqueue anhaengen
	// sendPacket.setRetransmitTimestamp(timerManager.getTimeInMillis() + RETRANSMIT_INTERVAL);
	// retransmitQueue.addElement(sendPacket);	
    }

    public void send1(byte[] byteArr) throws IOException {
	if (getSocketState() != ESTABLISHED) throw new IOException("send can only be called on open sockets");
	if (debug) Debug.out.println("Entered send() method");

	if (debug) Debug.out.println("local port " + this.localPort);

	int size = byteArr.length;

	if (size > 1400) throw new Error("Send: Byte array size exceeds 1400 Bytes");

	IPData data;
	if (MEMORYCYCLE) {
	    data = (IPData)usableBufs.undockFirstElement();
	    if (data == null) throw new Error ("no free Buffers");
	} else {
	    data = new IPData();
	    data.mem = tcp.getTCPBuffer1();
	}
	data.offset = EtherFormatrequiresSpace+IPFormat.requiresSpace();   
	data.size = 20+size;
	TCPFormat sendPacket = new TCPFormat(data, localIP, remoteIP);

	sendPacket.insertFlags(TCPFormat.PSH | TCPFormat.ACK);
	sendPacket.insertSourcePort(localPort);
	sendPacket.insertDestinationPort(remotePort);
	sendPacket.insertAcknowledgmentNumber(ack);
	sendPacket.insertSequenceNumber(seq);
	sendPacket.insertWindowSize(localWindowSize);
	sendPacket.computeHeaderLength();
	sendPacket.insertData(byteArr);
	sendPacket.insertChecksum();

//	Integer rWsz = remoteWindowSize.get();
//	while (rWsz.intValue() < size) // Empfänger nicht bereit...
//	    remoteWindowSize.blockIfEqual(rWsz);
	while (remoteWindowSize < size)
	    cpuManager.yield();
	//Debug.out.println(remoteWindowSize+"->"+seq);

	if (COPY_AND_RETRANSMIT) 
	    while (retransmitQueue.size() > MAX_RETRANSMITQ_LENGTH)
		cpuManager.yield();
	
        IPData d = null;
	if (COPY_AND_RETRANSMIT) {
	    if (MEMORYCYCLE) {
		d = (IPData)usableBufs.nonblockingUndockFirstElement();
		if (d == null) {
		    if (debug) Debug.out.println("TCPSocket on port " + localPort + ": No buffer available, no retransmition");
		} else {
		    d.mem.copyFromMemory(data.mem,0,0,data.mem.size());
		} 
	    }else {
		d = new IPData();
		d.mem = tcp.getTCPBuffer1();
		d.mem.copyFromMemory(data.mem,0,0,data.mem.size());
	    }
	} 
	
	// und ab geht die Post
	Memory back = lowerLayer.send1(data.mem,data.offset,data.size);
	if (MEMORYCYCLE) {
	    data.mem = back;
	    usableBufs.appendElement(data);
	}
	remoteWindowSize -= size;
	seq += size;
	if (debug) Debug.out.println("Sent: Seq is " + seq +" --> "+(seq+size));

	// ab in die Retransmitqueue
        if (COPY_AND_RETRANSMIT) {
	    if ( d!=null) {
		TCPData tcpData;
		if (MEMORYCYCLE) 
		    tcpData = new TCPData (d);
		else {
		    data.mem = d.mem;  // because offser/size info of d is not valid
		    tcpData = new TCPData (data);
		}
		    tcpData.setRetransmitTimestamp(timerManager.getTimeInMillis() + RETRANSMIT_INTERVAL);
		    retransmitQueue.addElement (tcpData);
	    }
	}
    }

    public byte[] readFromInputBuffer() {
	if (debug) Debug.out.println("In readFromInputBuffer()");
	IPData d = (IPData)inputBufs.undockFirstElement();
	if (d == null) throw new Error("input buffer ist empty");

	TCPFormat packet = new TCPFormat(d);
	byte[] data = packet.getData();
	if (MEMORYCYCLE)
	    usableBufs.appendElement(d); 
	return data;
    }
    
    //      public void writeToOutputBuffer(byte[] data) {
    //  	Buffer buf = usableBufs.undockFirstElement();
    //  	Memory mem = buf.getData();
	
    //  	TCPFormat packet = new TCPFormat(buf.getData());
    //  	packet.insertData(data);
    //      }
}









