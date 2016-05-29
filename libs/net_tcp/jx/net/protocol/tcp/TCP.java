package jx.net.protocol.tcp;
import java.util.*;
import java.lang.*;
import java.net.*;

import jx.zero.*;
import jx.timer.*;



/**
  * TCP conforming to RFC-793
  */

public class TCP {
  /* Timer definitions */
  public static final int RETRANSMITTIME = 30000;     /* interval at which retransmitter is called */
  private static final int LONGTIMEOUT = 310000;       /* timeout for opens */
  private static final int TIMEOUT = 150000;           /* timeout during a connection */
  // how many retransmissions of a segment are allowed to occure before abort
  private static final int MAXNUMRETRANSMIT = 30;
    //  private IP iplayer;  
  Vector sockets = new Vector();
  private int timeout;
  private int start;
  private Clock clock;
  private TimerManager timerManager;

  // used in random port
  private static long randI = 0;  
  // used by getISS()
  private static int globalSequenceNumber = 0;

  class TCPRetransmitter implements ThreadEntry {
    int timeout;
    public TCPRetransmitter() {
      Debug.out.println("TCP-Thread wird konstruiert!");
      timeout = 0;
    }
    // infinite loop to call the retransmitter() for each socket still containing data not acknowledged
    // BESSER MACHEN ALS MIT EINER BUSY-WAIT SCHLEIFE!!!
    public synchronized void run() {
      Debug.out.println("TCP-Thread gestartet...");
      while (true) {
	if (!sockets.isEmpty()) {
	  start = timerManager.getTimeInMillis();
	  if (timerManager.getTimeInMillis() > timeout) {
	    retransmitter();
	    timeout = timerManager.getTimeInMillis() + RETRANSMITTIME;
	  }
	  timeout -= (timerManager.getTimeInMillis() - start);
	}
	else
	  continue;
      }
    }
  }
  
  // constructor of TCP - get reference to clock and register tcp-layer at the ip-layer
  // initialize and start the retransmitting system

  public TCP(IP ip, Clock clock, TimerManager timerManager) {
      this.timerManager = timerManager;
      
    iplayer = ip;
    if (!iplayer.tcpRegister(this))
      System.out.println("TCP: ERROR -> couldn´t register TCP at IPLayer!!");
    //    TCPRetransmitter retransmitter = new TCPRetransmitter(clock);
    //    retransmitter.start();
    // create a Retransmit object and queue it in the timer list
    timerManager.addTimer(RETRANSMITTIME,  new Retransmit(timerManager), new RetransmitArg(this));
  }

  /** 
    * returns the space that is needed for a complete packet (consisting of ethernet-, ip- and tcp-header)
    *
    * @return number of bytes which have to be allocated for the various header
    *
    */
  public static int requiresSpace() {
    return (IP.requiresSpace() + TCPFormat.requiresSpace());
  }
  
  /**
    * informs about the maximum transfer unit of the underlying network (this information is propagated through the layers)
    *
    * @return maximum transfer unit measured in bytes
    *
    */
  public int maxDataSize() {
    return iplayer.maxDataSize();
  }

  /**
    * get the initial sequence number for a new connection
    * well, this method uses a global value incremented by 16000 each new connection and it takes this as a startvalue
    * the actual time is further used and divided by some factor to obtain a random value 
    * ok: this mehtod isn´t a fast way to get a ISS and furthermore the steps taken to get a number have no specific reason
    * but it´s better than just always taken 0 - and I think it´s pretty hard to predict a ISS even if you know this routine
    *
    * @return a suitable initial sequence number to easy to predict 
    *
    */
  private int getISS() {
    long oldseqnum = globalSequenceNumber;
    globalSequenceNumber += 16000;
    return (int)((oldseqnum + clock.getTicks()*oldseqnum/100) & 0xffffffff);
  }
  
  /**
    * Open a TCP connection - active open
    *
    * this method is just an alias for the open method below 
    * @param localPort: port at local host, 0 means the system chooses one
    * @param socket: reference to socket who does the open call
    * @param address: destination address
    * @param destPort: destination port to connect to
    *  @exception PortUsedException indicates, that the port cannot be used, as there is already a socket with this port
    */
  public void open(Socket socket, int localPort, InetAddress address, int destPort) throws PortUsedException {
    open(socket, localPort, address.getAddress(), destPort);
  }

  /**
    * Open a TCP connection - active open
    *
    * @param localPort: port at local host, 0 means the system chooses one
    * @param socket: reference to socket who does the open call
    * @param ina: byte array containing the destination address
    * @param destPort: destination port to connect to
    * @exception PortUsedException indicates, that the port cannot be used, as there is already a socket with this port
    */
  public void open(Socket socket, int localPort, byte[] ina, int destPort) throws PortUsedException {
    // first check the local Port: if the user requested to use a specific port, we have to check wether this port is already in use
    // checkPort returns true if the port is not in use and can be used by this new open call
    // if the user requests the system to choose the port (by calling with a local port of 0) we set this port some lines below
    if (localPort != 0)
      if (!checkPort(localPort)) 
	throw new PortUsedException();
    
    socket.setState(State.SYNSENT);
    socket.setTimeout(LONGTIMEOUT);
    // the system chooses the port: randomPort() also checks wether the port is already in use and returns a port-value that is ok to use
    // watch out: the more sockets (and ports of course) are in use, the longer this method will run in order to return a valid port number
    // it therefore can even happen that it becomes an infinite loop (if all possible port numbers (65535)) are alreay in use
    if ( localPort == 0 ) localPort = randomPort();
    socket.setLocalPort(localPort);
    // insert the destination address and the destination port - the hardware address of the destination will be searched for at a lower level
    socket.setDestAddress(ina);
    socket.setDestPort(destPort);
    // set the initial sequence number - method getiss() uses the actual time and a global starting value
    socket.setSeqNumber(getISS());
    socket.setDataSize(0);
    // set flag to SYN as we want to have a connection to be established
    socket.setFlags(Flags.SYN);
    // set the retransmission flag (not a TCP flag, internal flag)
    socket.setUnhappy(true);
    // insert the socket to the vector of sockets if it isn´t already contained (just being a bit more secure as it is needed)
    if (!sockets.contains(socket))
      sockets.addElement(socket);
    
    // now send a SYN packet
    send(socket);
    return;
  }

  /**
    * Open a TCP connection - passive open
    *
    * @param socket: reference to socket who does the open call
    * @param port: port we listen at
    * @param timeout: how long should the listen wait for incoming connections (0 for infinity)
    *
    * @exception PortUsedException indicates, that the port cannot be used, as there is already a socket with this port
    *            currently not used any longer, as the checking is done in the ServerSocket and we need to queue more than one socket with the same
    *            localPort
    */
    public void listen(Socket socket, int port, int timeout) /*throws PortUsedException*/ {
    socket.setState(State.LISTEN);
    // if timeout is zero we set it to "infinity"
    if (timeout == 0)
      socket.setTimeout(0x7fffff);
    // if (!checkPort(port)) 
    //  throw new PortUsedException();
    socket.setLocalPort(port);
    // destination port is unknown as long as there is no connection
    socket.setDestPort(0);
    // set the initial sequence number - method getiss() uses the actual time and a global starting value
    socket.setSeqNumber(getISS());
    socket.setDataSize(0);
    socket.setFlags(0);
    socket.setUnhappy(false);
    // insert the socket to the vector of sockets if it isn´t already contained (just being a bit more secure as it is needed)
    if (!sockets.contains(socket))
      sockets.addElement(socket);
    return;
  }
  
  /** 
    * Close a TCP Connection 
    *
    * @param socket: which connection should be closed
    *
    */
  public void close(Socket socket) {
    if (socket.getState() == State.ESTAB || socket.getState() == State.SYNREC) {
      // send a FIN segment
      socket.setFlags(Flags.ACK | Flags.FIN);
      socket.setState(State.FINWT1);
      // force to send at next call of retransmitter
      socket.setUnhappy(true);
    }
  }
  
  /**
    *
    * Abort a connection - send a RST segment 
    *
    * @param socket: which connection should be aborted
    *
    */
  public void abort(Socket socket) {
    if (socket.getState() != State.LISTEN && socket.getState() != State.CLOSED) {
      socket.setFlags(Flags.RST | Flags.ACK);
      send(socket);
    }
    socket.setUnhappy(false);
    socket.setDataSize(0);
    socket.setState(State.CLOSED);
    // notify the socket
    socket.abortConnection("connection aborted and RST segment send - connection closed!");
    if (!sockets.removeElement(socket))
      System.out.println("ERROR: tcp_abort() couldn´t remove socket form socket vector!!");
  }
    
    /**
     * this method does all the retransmission stuff 
     * it is called by a timer each 3 seconds - it searches through all sockets and for each socket it searches the retransmission
     * queue for segments that should be retransmitted (that means, which are queued for >= 3 seconds and have a retransmission 
     * count < specific maximum
     */  
   protected void retransmitter() {
    Socket socket;
    boolean x = false;
    // indicates an abort after too many retransmissions
    boolean abort = true;

    for (Enumeration e = sockets.elements() ; e.hasMoreElements() ;) {
      x = false;
      socket = (Socket)e.nextElement();
      // if there are some segments queued for retransmission or the unhappy flag is set
      if  (socket.getUnhappy()) {
	      send(socket);
	      x = true;
      }
      // if there are any segments queued for retransmission
      else if (socket.retransmitSize() > 0) {
	  for (Enumeration segments = socket.getRetransmitEnumeration(); segments.hasMoreElements(); ) {
	      TcpRetransmitSegment segment = (TcpRetransmitSegment)segments.nextElement();
	      // if the segment has not been retransmitted yet and hasn´t been queued for at least one RETRANSMITTIME (we divide by 10 as ticks come  I CHANGED THIS, TODO: change comment
	      // each 10 ms and RETRANSMITTIME is ms) we don´t retransmit - this means that this segment has entered the vector between the last call
	      // of this method and this call of the method and we think it´s to early to retransmit - we will wait at lest RETRANSMITTIME 
	      if ((segment.numretransmit() == 0) && ((timerManager.getTimeInMillis() - segment.queuedtime()) < RETRANSMITTIME ))
		  continue;
	      else if (segment.numretransmit() >= MAXNUMRETRANSMIT)
		  abort = true;
	      else {
		  // just send the already complete segment again
		  try {
		    iplayer.transmit(socket.getDestAddressByte(), IP.PROTOCOL_TCP, segment.segment());
		    segment.incnumretransmit();
		    x = true;
		  }
		  catch (UnknownIPAddressException u) {
		    System.out.println("TCP.retransmitter(): cannot happen");
		  }
	      }
	  }
	      
      }
      // now we decrement the timeout of the socket - we do this if we have retransmitted at least one segment or if we are not in the ESTABLISHED
      // state - this means we are trying to establish a connection or finish one and we wan´t to prevent the system of being infite in one of this
      // states - if there doesn´t come any answer from the other tcp we abort the connection or if we always have to retransmit the segments and 
      // don´t get any acknowledgments there seems to be some failure on the connection and we have to abort too
      if (x || socket.getState() != State.ESTAB) {
	  // this timeout indicates forever in the listen case - so do not decrement
	  if (socket.getTimeout() != 0x7fffff)
	      socket.setTimeout(socket.getTimeout() - RETRANSMITTIME);
      }
      // if there occured a timeout or one segment had been retransmitted too often (indicating a broken connection)
      if (socket.getTimeout() <= 0 || abort) {
	if (socket.getState() == State.TIMEWT) {
	  System.out.println("TCP_Retransmitter: closed!");
	  socket.setState(State.CLOSED);
	  // notify the socket
	  if (socket.getTimeout() <= 0)
	      socket.abortConnection("a timeout occured - connection closed!");
	  else
	      socket.abortConnection("too many retransmissions (network problems?) - connection close!");
	  if (!sockets.removeElement(socket))
	    System.out.println("ERROR: TCP_Retransmitter: couldn´t remove socket from socket vector!!!");
	}
	else {
	  System.out.println("TCP_Retransmitter: Timeout occured, aborting!");
	  abort(socket);
	}
      }
    }
  }
  
  /** 
    * flush any data that isn´t already sent - the PUSH flag is set (in this implementation is no buffering at this level, so calling this method
    * need not be used to force sending of buffered data, but can be used to set the PUSH flag in order to force the receiving tcp to deliver the data
    * to the application 
    *
    * @param socket: which connection should be flushed 
    *
    */
  public void flush(Socket socket) {
    if (socket.getDataSize() > 0) {
      socket.setFlags(socket.getFlags() | Flags.PUSH);
      send(socket);
    }
  }
    

  /** 
    * a helper method to compare IP-addresses - in JDK1.2 we could use Arrays.equals()
    *
    * @param a1: first array to compare
    * @param a2: second array to compare
    *
    * @return true if the arrays contain the same values, false otherwise
    */
  public boolean arrayCompare(byte[] a1, byte[] a2) {
    if (a1.length != a2.length)
      return false;
    for (int i = 0; i<a1.length; i++) {
      if (a1[i] != a2[i])
	return false;
    }
    return true;
  }
    
  /**
    * this is the main dispatching method for incoming packets and is called by IP.receive()
    *
    * @param buf a handle to the received packet - we need at least a complete IP packet, but it can be also more 
    *        as the offset parameter can point to the beginning of the IP packet
    * @param offset points to the beginning of the IP packet 
    *
    */
  public void receive(Memory buf, int offset) {
    Debug.out.println("TCPHANDLER AUFGERUFEN!!!");
  }
  public void receive(byte[] buf, int offset) {
    
    Debug.out.println("TCPHANDLER AUFGERUFEN!!!");
    
    // we don´t receive TCP packet, but a IP packet as we need the IP-header to compute the checksum with the pseudoheader
    IPFormat ip = new IPFormat(buf, offset);

    PseudoHeader ph = new PseudoHeader();
    int len;
    int x, diff;
    Socket socket = null;
    byte flags;
    int counter = 0;
    
    // get the length of the IP-header
    len = ip.getHeaderLength();

    // now we know that the TCP header starts at offset + len
    TCPFormat tp = new TCPFormat(buf, offset + len);
    
    // getTotalLength() returns the length of the complete IP packet - header and data
    // so since this point len is the length of the complete TCP packet with the header - this information is needed by the pseudoheader
    len = ip.getTotalLength() - len;
    
    // this part checks wether there is socket the received packet is destined for
    // first we check the sockets vector for an active socket
    // if we can´t find a socket with an active open we search again for a passive one

    for (Enumeration e = sockets.elements() ; e.hasMoreElements() ;) {
      socket = (Socket)e.nextElement();
      counter++;
      // find an "active socket"
      if (socket.getDestPort() != 0 && 
	  tp.getDestPort() == socket.getLocalPort() && 
	  tp.getSourcePort() == socket.getDestPort() &&
	  arrayCompare(ip.getSourceIPAddress(), socket.getDestAddressByte()))
	break;
      
    }
	
    if (counter == sockets.size()) {
      counter = 0;
      for (Enumeration e = sockets.elements() ; e.hasMoreElements() ;) {
	counter++;
	socket = (Socket)e.nextElement();
	// find a "passive socket"
	if (socket.getDestPort() == 0 && 
	    tp.getDestPort() == socket.getLocalPort())
	  break;
      }
    }
    
    // we didn´t find neither an "active" nor a "passive" socket matching the received packet - drop it
    if (counter == sockets.size()) {
      System.out.println("TCP: Discarding received packet!");
      return;
    }
    
    // now fill the pseudoheader and check the checksum - we use the checksum method from the IP class
    ph.insertSourceAddress(ip.getSourceIPAddress());
    ph.insertDestAddress(ip.getDestIPAddress());
    ph.insertMBZ(0);
    ph.insertProtocol(6);
    ph.insertLength(len);
    // here we use kind of a simple trick - we have extented the pseudoheader and insert the checksum of the TCP segment
    // the we compute the checksum of the pseudoheader and because it already contains the checksum of the TCP segment we therefore
    // get the complete checksum of TCP segment combined with the pseudoheader
    ph.insertTCPChecksum(IPFormat.checksum(tp));
    if (IPFormat.checksum(ph) != 0xffff) {
      System.out.println("TCP: bad tcp checksum, dropping packet!");
      return;
    }

    flags = (byte)tp.getFlags();
    // if we received a RST (reset) segment, we close the connection and notify the receiver
    if ((flags & Flags.RST) > 0) {
      System.out.println("TCP: Connection reset!");
      socket.setState(State.CLOSED);
      // notify the socket
      socket.abortConnection("RST segment received - connection closed!");
      if (!sockets.removeElement(socket))
	System.out.println("TCP: couldn´t remove socket from socket vector");
      return;
    }
	
    // starting from here we process the received segment depending on the state we are currently in

    switch (socket.getState()) {
      
    case State.LISTEN:
      if ((flags & Flags.SYN) > 0) {
	socket.setAckNumber(tp.getSequenceNumber() + 1);
	socket.setDestPort(tp.getSourcePort());
	socket.setDestAddress(ip.getSourceIPAddress());
	socket.setFlags(Flags.SYN  | Flags.ACK);
	send(socket);
	socket.setState(State.SYNREC);
	// we enable the retransmission and set a timeout
	socket.setUnhappy(true);
	socket.setTimeout(TIMEOUT);
	System.out.println("TCP: Syn from " + socket.getDestAddressByte() + ":" + socket.getDestPort() + " (seq: " + tp.getSequenceNumber() + ")");
      }
      break;
      
    case State.SYNSENT:
      if ((flags & Flags.SYN) > 0) {
	socket.setAckNumber(socket.getAckNumber() + 1);
	socket.setFlags(Flags.ACK);
	socket.setTimeout(TIMEOUT);
	// if it is an acknowledgment we wait for we open the connection
	if ( ((flags & Flags.ACK) > 0) && tp.getAcknowledgeNumber() == (socket.getSeqNumber() + 1) ) {
	  System.out.println("TCP: Open");
	  socket.setState(State.ESTAB);
	  socket.setSeqNumber(socket.getSeqNumber() + 1);
	  socket.setAckNumber(tp.getSequenceNumber() + 1);
	  socket.setUnhappy(false);
	}
	else {
	  socket.setState(State.SYNREC);
	}
      }
      break;

    case State.SYNREC:
      if ((flags & Flags.SYN) > 0) {
	// to answer to an open request we send an acknowledgment and a syn 
	socket.setFlags(Flags.SYN | Flags.ACK);
	send(socket);
	socket.setTimeout(TIMEOUT);
	System.out.println("TCP: retransmit of original syn");
      }
      if ( ((flags & Flags.ACK) > 0) && tp.getAcknowledgeNumber() == (socket.getSeqNumber() + 1) ) { 
	socket.setFlags(Flags.ACK);
	send(socket);
	socket.setSeqNumber(socket.getSeqNumber() + 1);
	socket.setUnhappy(false);
	socket.setState(State.ESTAB);
	socket.setTimeout(TIMEOUT);
	System.out.println("Synack received - connection established");
      }
      break;
      
    case State.ESTAB:
      // if there is no acknowledgment this segment is not valid as every segment has to carry an ack flag after the connection has been established
      if ((flags & Flags.ACK) == 0) 
	return;
      // now we process the ack value in the packet - calculate the number of octets this segment acknowledges
      int ackvalue = tp.getAcknowledgeNumber();
      diff = ackvalue - socket.getSeqNumber();
      if (diff > 0) {
	// check the retransmission queue and delete acknowledged segments
	socket.ackReceived(ackvalue);
	// NICHT UNBEDINGT NÖTIG!!!!!!!
	socket.setDataSize(socket.getDataSize() - diff);
	socket.setSeqNumber(socket.getSeqNumber() + diff);
      }
      socket.setFlags(Flags.ACK);
      // processData handles the received data
      processData(socket, tp, len);
      break;

      // this state can be reached by a close-call of the application
    case State.FINWT1:
      // as above - if there is no ack flag the segment is not valid
      if ( (flags & Flags.ACK) == 0) 
	return;
      diff = tp.getAcknowledgeNumber() - socket.getSeqNumber() - 1;
      socket.setFlags(Flags.ACK | Flags.FIN);
      if (diff == 0) {
	socket.setState(State.FINWT2);
	socket.setFlags(Flags.ACK);
	System.out.println("TCP: finack received");
      }
      processData(socket, tp, len);
      break;

    case State.FINWT2:
      socket.setFlags(Flags.ACK);
      processData(socket, tp, len);
      break;

    case State.CLOSING:
      if (tp.getAcknowledgeNumber() == (socket.getSeqNumber() + 1)) {
	socket.setState(State.TIMEWT);
	socket.setTimeout(TIMEOUT);
      }
      break;

    case State.LASTACK:
      if (tp.getAcknowledgeNumber() == (socket.getSeqNumber() + 1)) {
	socket.setState(State.CLOSED);
	socket.setUnhappy(false);
	socket.setDataSize(0);
	if (!sockets.removeElement(socket)) 
	    System.out.println("TCP: couldn´t remove socket from socket vector");
	System.out.println("TCP: closed");
      }
      else {
	socket.setFlags(Flags.ACK | Flags.FIN);
	send(socket);
	socket.setTimeout(TIMEOUT);
	System.out.println("TCP: retransmitting fin");
      }
      break;

    case State.TIMEWT:
      socket.setFlags(Flags.ACK);
      send(socket);
    }

  }

  /**
    * this method is called from all states where data can be received - established, fin-wait-1, fin-wait-2
    * it handles the received data and transfers it into the receive buffer
    *
    * @param socket the connection which has received the segment
    * @param tp the received TCP segment
    * @param len length of the TCP segment with the header
    *
    */
  public void processData(Socket socket, TCPFormat tp, int len) {
    
    int diff, x;
    byte flags;
    int datapointer;

    flags = (byte)tp.getFlags();
    // diff is the difference between the sequence number we are waiting for and the sequence number we received in the last segment
    // therefore following rules hold: if diff = 0 the segment continues exactly at the sequence number we were waiting for
    //                                 if diff < 0 we received a segment out of order and so we have a hole in our stream of data
    //                                 if diff > 0 we have received some new, but also some old data which we have already received
    //                                 in this case diff is the number of "old" data we received again
    diff = socket.getAckNumber() - tp.getSequenceNumber();
    // the SYN flag uses one sequence number - so decrement diff
    if ((flags & Flags.SYN) > 0) 
      diff--;
    // get the beginning of data (multiply by 4 because DataOffset tells us the number of dwords the header has)
    x = tp.getDataOffset() << 2;
    // wir brauchen eigentlich keinen datapointer mehr, sondern machen das übern nen offset in die format-klasse
    //datapointer = tp + x;
    // determine the "real" length - the length of the data we received (len was the length of complete TCP segment - we subtract the length of
    // the TCP header and so get the length of the data in the segment)
    len = len - x;
    // if we have received new data or new data combined with some "old" data - see explanation above
    if (diff >= 0) {
      // dies zeigt dann exakt auf die neuen daten
      //datapointer = datapointer + diff;
      // we determine the length of the new data (subtracting the number of old octets from the whole number of received octets)
      len = len - diff;
      socket.setAckNumber(socket.getAckNumber() + len);
      // here we hand the received data to the socket which itself puts them into the input stream
      // if the buffer of the input stream is full (because the application consuming the data is too slow) we discard the data
      // and decrement the acknowledgment so the other tcp will retransmit this data
      if (!socket.dataReceived(tp, x)) {
	System.out.println("TCP.processData(): input stream can´t take received data - discarding data and waiting for retransmission!!");
	socket.setAckNumber(socket.getAckNumber() - len);
      }
      // now we check for a piggy-packed FIN flag telling us that the other side of the connection want´s to close it
      if ((flags & Flags.FIN) > 0) {
	socket.setAckNumber(socket.getAckNumber() + 1);
	
	switch (socket.getState()) {
	case State.ESTAB:
	  socket.setState(State.LASTACK);
	  socket.setFlags(socket.getFlags() | Flags.FIN);
	  socket.setUnhappy(true);
	  break;
	  
	case State.FINWT1:
	  socket.setState(State.CLOSING);
	  break;
	case State.FINWT2:
	  socket.setState(State.TIMEWT);
	  break;
	}
      }
    }
    socket.setTimeout(TIMEOUT);
    send(socket);
  }

  /** 
    * check wether a port is already in use or not
    *
    * @param p the port which we check wether it can be used or wether it is already in use
    *
    * @return true if the port can be used (port is not used), false otherwise
    *
    */
  protected boolean checkPort(int p) {
    boolean used = false;
    if (p < 0 || p > 0xffff)
	return false;
    Socket socket;
    for (Enumeration e = sockets.elements() ; e.hasMoreElements() ;) {
      socket = (Socket)e.nextElement();
      if (socket.getLocalPort() == p) {
	used = true;
      }
    }
    return !(used);
  }
  
    /**
     * the socket needs to know our own IP-adress in one of the constructors
     * so this method get´s it from the IP-layer and transmits it
     *
     * @return a byte array containing the local IP-address 
     */
    byte[] getmyselfIP() {
	return iplayer.getmyselfIP();
    }

  /**
    * offer a random port if a open call doesn´t indicate that it wants to use a specific port
    * the method does some simple calculation using the actual time (in ticks) and a starting variable incremented each call of this method
    * java.lang.Math.random() might be a better choice, but java.util.Random is not implemented so this method wil do the job
    * there is also a check done wether the port is already in use
    * therefore the more ports are already in use, the longer this method will take
    * if all ports are in use, this will loop infinite
    *
    * @return a portnumber that isn´t already in use 
    *
    */
  protected int randomPort() { 
    int retValue = 0;
    int cvalue = 0;
    while (true) {
      cvalue = timerManager.getTimeInMillis();
      randI++;
      retValue = (int)((cvalue * randI) % 65535);
      if (checkPort(retValue))
	break;
    }
    return retValue;
  }
  
  /** 
    * Format and send an outgoing segment - the calling environment has to assure that the data to be transmitted doesn´t exceed
    * the maximum size of a segment - this method is called by the outputbuffer of the socket which has to care for this
    * this version accepts an array of data to send, the second version below just sends segments without data
    *
    * @param socket the socket containing the information for this connection
    * @param mydata the data to send
    *
    */
  public void send(Socket socket, byte[] mydata) {

   // first of all - if we haven´t a established connection, we cannot send - so just return
   if (socket.getState() != State.ESTAB)
      return;

   // if we can´t add no more TcpRetransmitSegment into the list we have to wait for some acknowlegment to free the buffer
   if (!socket.canBufferRetransmit())
     socket.waitForBufferSpace();

    int len = requiresSpace();
    if ( (socket.getFlags() & Flags.SYN) != 0 ) {
      len +=  4;
    } 
    // auskommentiert, weil die Datasize nicht stimmt und wir die aus dem übergebenen Array nehmen muessen
    // len += socket.getDataSize();
    len += mydata.length;
    //socket.setDataSize(mydata.length);
    // now we know the space that is needed - the size of the TCP header and the IP header and the Ethernet header (done by requiresSpace())
    // + 4 if we are going to establish a connection as we send a max segment size option + the length of the data to transmit 
    // we allocate memory for the TCP-Format
    byte buf[] = new byte[len];

    // get a TCPFormat with buffer buf and offset IP.requiresSpace()
    TCPFormat tcp = new TCPFormat(buf, IP.requiresSpace());
    tcp.insertSourcePort(socket.getLocalPort());
    tcp.insertDestPort(socket.getDestPort());
    tcp.insertSeqNumber(socket.getSeqNumber());
    tcp.insertAckNumber(socket.getAckNumber());
    // we use a fixed window size of 1024 Octets
    tcp.insertWindow(1024);
    // to compute the checksum we have to clear the checksum field
    tcp.insertChecksum(0);
    // no urgent pointer
    tcp.insertUrgentPointer(0);

    // if we establish a connection we send a maximum segment size option
    if ( (socket.getFlags() & Flags.SYN) != 0 ) {
      // we have to adjust the data offset by one as this option requires 4 Bytes of additional space
      tcp.insertFlags((socket.getFlags() | 0x5000) + 0x1000); /* data offset (options) */
      // now enter the option combined in one write: kind = 2, length = 4, maximum segment size = 1400 Bytes
      tcp.insertMaxSegOpt(0x02040578); // 1400 bytes
    } 
    else {
      // otherwise we write the flags combined with writing the data offset into the TCP header (5 -> header length is 20 Bytes)
      tcp.insertFlags(socket.getFlags() | 0x5000);
    }
    // finally copy the data into the buffer
    tcp.insertData(mydata);

    /* 
     * compute tcp checksum 
     */
    PseudoHeader ph = new PseudoHeader();
    ph.insertSourceAddress(iplayer.getmyselfIP());
    ph.insertDestAddress(socket.getDestAddressByte());
    ph.insertMBZ(0);
    ph.insertProtocol(6);
    // we insert the length of the TCP header + the length of the data contained in the segment
    ph.insertLength(len - IP.requiresSpace());
    // finally compute the checksum of the pseudoheader and the TCP segment
    tcp.insertChecksum(tcp.checksummer(ph, tcp));

    // hand the segment over to the IP-layer
    try {
      iplayer.transmit(socket.getDestAddressByte(), IP.PROTOCOL_TCP, buf);
    }
    catch(UnknownIPAddressException e) {
      System.out.println("TCP.send(): cannot happen(1)");
    }
    
    // if unhappy is not set, we queue this segment for retransmission
    // if unhappy is set, this means that we are in a open or close sequence or in LISTEN state or received a FIN segment
    // in these cases we don´t queue, but check especially in the retransmit mehthod and do a direct call to send(socket)
    if (!socket.getUnhappy()) 
	socket.addRetransmit(new TcpRetransmitSegment( socket.getSeqNumber(), (socket.getSeqNumber() + mydata.length - 1), buf, timerManager));
  }
  

  /**
    * Format and send an outgoing segment - the calling environment has to assure that the data to be transmitted doesn´t exceed
    * the maximum size of a segment - this method is called by the outputbuffer of the socket which has to care for this
    * this version sends segments without data
    *
    * @param socket the socket containing the information for this connection
    *
    */
  private void send(Socket socket) {
    int len = requiresSpace();
    if ( (socket.getFlags() & Flags.SYN) != 0 ) {
      len +=  4;
    }
    byte buf[] = new byte[len];
    Debug.out.println("TATATATATA - der buffer ist " + buf.length);
    
    TCPFormat tcp = new TCPFormat(buf, IP.requiresSpace());
    tcp.insertSourcePort(socket.getLocalPort());
    tcp.insertDestPort(socket.getDestPort());
    tcp.insertSeqNumber(socket.getSeqNumber());
    tcp.insertAckNumber(socket.getAckNumber());
    tcp.insertWindow(1024);
    tcp.insertChecksum(0);
    tcp.insertUrgentPointer(0);

    if ( (socket.getFlags() & Flags.SYN) != 0 ) {
      tcp.insertFlags((socket.getFlags() | 0x5000) + 0x1000); 
      tcp.insertMaxSegOpt(0x02040578); 
    } 
    else {
      tcp.insertFlags(socket.getFlags() | 0x5000);
    }

    /* 
     * compute tcp checksum 
     */
    PseudoHeader ph = new PseudoHeader();
    ph.insertSourceAddress(iplayer.getmyselfIP());
    ph.insertDestAddress(socket.getDestAddressByte());
    ph.insertMBZ(0);
    ph.insertProtocol(6);
    ph.insertLength(len - IP.requiresSpace());
    tcp.insertChecksum(tcp.checksummer(ph, tcp));
    Debug.out.println("Nach Aufruf von insertCheckSum");
    try {
      Debug.out.println("TCP.send: Jetzt wird der ip-transmit aufgerufen!");
      iplayer.transmit(socket.getDestAddressByte(), IP.PROTOCOL_TCP, buf);
    } 
    catch(UnknownIPAddressException e) {
      System.out.println("TCP.send(): cannot happen(2)");
    }
  }
}
