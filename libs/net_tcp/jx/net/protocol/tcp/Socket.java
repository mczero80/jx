package jx.net.protocol.tcp;
import java.io.*;
import java.util.*;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketImpl;
import java.net.SocketImplFactory; 

import jx.timer.*;


/**
 * this a the implementation of a client socket
 * it has the same methods as a JDK socket, but some are not supported (for example some constructors)
 * there is one difference: because of the architecture of the networking system, our sockets need a reference to a tcp stack
 * it is possible that each socket uses a different stack
 * if compatibility to JDK sockets is wanted, some kind of "socket wrapper" could be used
 * a socket class having the same interface as a JDK socket, using a global known tcp stack and passing this to an instance of this class
 * and returning an object of this class
 */
public class Socket {
    /** this is the maximum number of queued segments in the retransmitqueue */
    private final static int MAXRETRANSMITQUEUE = 10;
    /** the actual state of the connection (for example ESTABLISHED or SYN RECEIVED) */
    private int state;
    /** our own port */
    private int localPort;
    /** the target port of the connection */
    private int destPort;
    /** a timeout for aborting for example connection request which can´t establish a connection in this timeout period */
    private int timeout;
    /** the IP-address of the target as a byte array */
    private byte[] destAddress;
    /** the sequence number */
    private int seq;
    /** the acknowlegment number */
    private int ack;
    /** dataSize is deprecated but still saved - it counts the amount of buffered data */
    private int dataSize;
    /** flags to include into the next segment */
    private int flags;
    /** a marker flag for retransmission */
    private boolean unhappy;
    /** for conformance with the JDK socket API - saved but not yet supported */
    private int linger;
    /** for conformance with the JDK socket API - saved but not yet supported */
    private boolean delay;    
    // this is a flag that has to be checked before sending and maybe at other intervals too
    // if it is set, in indicates that a RST segment got received or some other kind of error occured
    private boolean connectionError = false;
    private String reason = "";
    // each socket has a vector of send tcp segments that are not yet acknowledged - therefore we retransmit them after a specific timeout
    private Vector retransmit = new Vector(10);
    // we use this variable instead of always calling a method of vector to obtain the number of elements in the vector
    // the size of segments hold for retransmission should be bounded to something like 10
    private int retransmitSize;
  // a lock for the retransmission queue if there is not enough space
  private Object bufferlock = new Object();
    
    /** these are the streams for reading and writing data to the connection
     * they have the interface of a input respectively output stream and some additional methods for 
     * the support of the tcp stack 
     */
    private MyOutputStream output;
    private MyInputStream input;

    /** Creates an unconnected socket - actually there is no method to connect this socket afterwards
     *
     * @param tcp the tcp layer this socket uses
     */ 
    public Socket() {
	output = new MyOutputStream(Socket_ENV.tcp, this, Socket_ENV.timerManager);
	input = new MyInputStream(Socket_ENV.tcp, this);
    }
    
    /** Creates a stream socket and connects it to the specified port number at the specified IP address
     *
     * @param tcp the tcp layer this socket uses
     * @param address an InetAddress object determining the target IP-address
     * @param port the target 
     *
     * @exception IOException if an internal error occures while setting up the socket 
     */    
    public Socket(InetAddress address, int port) throws IOException { 
	this();
	// watch out! - network byte order
	destAddress = address.getAddress();
	destPort = port;
	TCP tcp = Socket_ENV.tcp;
	try {
	    tcp.open(this, 0, destAddress, port);
	}
	catch (PortUsedException e) {
	    // this should only happen if all possible port numbers are in use
	    System.out.println("Could not select a local port!");
	    throw new IOException();
	}

	// now we check wether we can establish a connection
	// there is a timeout set when tcp.open is called
	// so if the timeout is expired the state must be established otherwise we´ve been not able to establish the connection
	// if during the timeout period the state changes to established everything is fine
	// if it is bigger than ESTAB it means that some error like receiving a RST segment occured and we also have to give u
	while (timeout > 0) {
	    if (state == State.ESTAB)
		break;
	    if (state > State.ESTAB) {
		System.out.println("Connection could not be established!");
		throw new IOException();
	    }
	}
	if (state != State.ESTAB) {
	    System.out.println("Connection could not be established!");
	    throw new IOException();
	}
	else 
	    return;
    }

    /** Creates a socket and connects it to the specified remote address on the specified remote port
     *
     * @param tcp the tcp layer this socket uses
     * @param address an InetAddress object containing the target IP-address
     * @param port the target port
     * @param localAddr an InetAdress object containing the local address this socket binds to - this implementation checks the
     * the address setup in the IP layer and throws a connection if they don´t match
     * @param localPort the local port the connection should use
     *
     * @exception IOException is thrown if a internal error occurs or if the localAddr parameter doesn´t match our IP address 
     */
    public Socket(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
	this();
	// watch out! - network byte order
	destAddress = address.getAddress();
	destPort = port;
	// we cannot bind the socket to a different IP address than our own so we throw an exception in this case 
	
	TCP tcp = Socket_ENV.tcp;
	if (!tcp.arrayCompare(tcp.getmyselfIP(), localAddr.getAddress()))
	    throw new IOException();
	localPort = localPort;

	try {
	    tcp.open(this, localPort, destAddress, port);
	}
	catch (PortUsedException e) {
	    // this should only happen if all possible port numbers are in use
	    System.out.println("Port is already used!");
	    throw new IOException();
	}

	// now we check wether we can establish a connection
	// there is a timeout set when tcp.open is called
	// so if the timeout is expired the state must be established otherwise we´ve been not able to establish the connection
	// if during the timeout period the state changes to established everything is fine
	// if it is bigger than ESTAB it means that some error like receiving a RST segment occured and we also have to give up
	while (timeout > 0) {
	    if (state == State.ESTAB)
		break;
	    if (state > State.ESTAB) {
		System.out.println("Connection could not be established!");
		throw new IOException();
	    }
	}
	if (state != State.ESTAB) {
	    System.out.println("Connection could not be established!");
	    throw new IOException();
	}
	else 
	    return;
    }

    /** Creates an unconnected Socket with a user-specified SocketImpl
     * not supported because of lacking support for SocketImpl 
     */ 
    protected Socket(SocketImpl impl) throws SocketException {
	System.out.println("ERROR: this constructor is not supported!");
	throw new SocketException();
    }
    /** Creates a stream socket and connects it to the specified port number on the named host
     * not supported because the network system doesn´t support name resolution yet
     */
    public Socket(String host, int port) throws UnknownHostException, IOException {
	System.out.println("ERROR: this constructor is not supported - can´t query the IP");
	throw new IOException();
    }

    /** Creates a socket and connects it to the specified remote host on the specified remote port
     * not supported because the network system doesn´t support name resolution yet
     */
    public Socket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
	System.out.println("ERROR: this constructor is not supported - can´t query the IP");
	throw new IOException();
    }
   

    /*
     * these are some methods to set the internal variables
     * a javadoc for every method is not necessary as these methods should be quite clear
     */
    void setState(int state) { this.state = state; } 
    void setTimeout(int timeout) { this.timeout = timeout; } 
    void setLocalPort(int port) { this.localPort = port; } 
    void setDestPort(int port) { this.destPort = port; } 
    void setDestAddress(byte[] addr) { destAddress = addr; }
    void setAckNumber(int ack) {this.ack = ack; }
    void setSeqNumber(int seq) { this.seq = seq; } 
    void setDataSize(int size) { this.dataSize = size; } 
    void setFlags(int flags) { this.flags = flags; } 
    void setUnhappy(boolean u) { this.unhappy = u; } 

    /*
     * these are some methods to get the internal variables
     * a javadoc for every method is not necessary as these methods should be quite clear
     */
    int getState() { return state; }
    int getDataSize() { return dataSize; }
    int getDestPort() { return destPort; } 
    byte[] getDestAddressByte() { return destAddress; }
    byte[] getLocalAddressByte() { return Socket_ENV.tcp.getmyselfIP(); }
    int getSeqNumber() { return seq; } 
    int getAckNumber() { return ack; } 
    boolean getUnhappy() { return unhappy; }  
    int getFlags() { return flags; }
    int getTimeout() { return timeout; }
    
    /** check the size of the retransmitqueue
     *
     * @return the number of queued segments 
     */
    int retransmitSize() {
	return retransmitSize;
    }
    
    /** 
     * add a segment to the retransmit queue
     *
     * @param segment a TcpRetransmitSegment containing the segment to be queued 
     */
    void addRetransmit(TcpRetransmitSegment segment) {
	retransmit.addElement(segment);
	retransmitSize++;
    }
    
    /**
     * remove a segment from the retransmission queue
     *
     * @param segment the TcpRetransmitSegment which should be removed
     *
     * @return true if the segment was found and deleted, false otherwise
     */
    boolean deleteRetransmit(TcpRetransmitSegment segment) {
	boolean returnval;
	returnval = retransmit.removeElement(segment);
	if (returnval) {
	    retransmitSize--;
	    bufferlock.notify();
	}
	return returnval;
    }
    
    /**
     * get a enumeration of the retransmission queue
     *
     * @return enumeration object of the retransmission segments
     */
    Enumeration getRetransmitEnumeration() {
	return retransmit.elements();
    }
    
    /**
     * handles the deletion of acknowledged data - it checks the retransmission queue for queued segments which have a last sequence number
     * less or equal the received acknowledgment and deletes segments that fit this criteria
     *
     * @param the received acknowledgment number to check for
     *
     */
    void ackReceived(int acknum) {
	if (retransmitSize == 0) return;
	for (Enumeration segments = retransmit.elements(); segments.hasMoreElements(); ) {
	    TcpRetransmitSegment segment = (TcpRetransmitSegment)segments.nextElement();
	    if (acknum >= segment.lastseqnum()) {
		// if this segment is completly acknowledged, we can delete it
		if (!deleteRetransmit(segment))
		    System.out.println("TCP.ackReceived() : can´t delete acknowledged retransmit segment - retransmissions may further occur!!");
	    }
	}
	return; 
    }
  
  /**
    * checks wether there is still space to send a segment and buffer it in the retransmission queue
    * 
    * @return true if there is still enough space to buffer one more segment, false otherwise
    */
  public boolean canBufferRetransmit() {
    if (retransmitSize >= MAXRETRANSMITQUEUE)
      return false;
    return true;
  }

  /**
    * calls wait at a lock for the retransmission queue - should be used together with the method canBufferRetransmit()
    * if there is not enough space, call this method and get notfied by deleteRetransmit if there is space available again
    *
    */
  public void waitForBufferSpace() {
    try {
      bufferlock.wait();
    }
    catch (InterruptedException e) {
      System.out.println("Socket.waitForBufferSpace(): wait() interrupted!!");
    }
  }


    /**
     * this method takes the received data and hands it over to the input stream
     * if the stream is not capable of receiving so much data, false is returned - it´s up to the caller to handle this
     *
     * @param the array of data that should be transferred to the input stream
     *
     * @return true if success - false if datasize is to big
     *
     */
    boolean dataReceived(TCPFormat tcp, int offset) {
	return input.insertReceived(tcp, offset);
    }
    
    
    // if we receive a RST segment or there occurs a timeout this method notifys the socket
    public void abortConnection(String r) {
	connectionError = true;
	reason = r;
    }
    /**
     * returns wether the connection is still valid or not
     *
     * @return true if connection was aborted is invalid, false otherwise
     */
    public boolean connectionAborted() {
	return connectionError;
    }

    /**
     * returns a reason, if connection was aborted
     *
     * @return string containing the abort reason 
     */
    public String connectionAbortReason() {
	return reason;
    }

    /** Return an output stream for this socket
     *
     * @return a outputstream to read data from this connection 
     *
     * @exception IOException if an internal error occures
     */
    protected OutputStream getOutputStream() throws IOException {
	return output;
    }
    /** Returns an input stream for this socket
     *
     * @return the inputstream to write data to the connection
     *
     * @exception IOException if an internal error occures 
     */
    protected InputStream getInputStream() throws IOException {
	return input;
    }

    /** Closes this socket
     *
     * @exception IOException if an internal error occures 
     */
    public synchronized void close() throws IOException {
      Socket_ENV.tcp.close(this);
    }
    
    /** Return the address to which the socket is connected
     * 
     * @return an InetAddress object containing the address of the target we are connected to  
     *
     * @exception UnknownHostException is thrown if the address of the destination is unknown
     */
    public InetAddress getInetAddress() throws UnknownHostException {
    return java.net.InetAddress.getByName(new String(destAddress));
    }
 
    /** Return our own address
     *
     * @return an InetAddress object containing our own address (and of course this is the address the socket is bound to)
     *
     * @exception UnknownHostException is thrown, if the local address can not be determined
     */ 
    public InetAddress getLocalAddress() throws UnknownHostException {
    return java.net.InetAddress.getByName(new String(Socket_ENV.tcp.getmyselfIP()));
    }
    
    /** Return the local port to which this socket is bound
     *
     * @return the local port
     */ 
    public int getLocalPort() {
    return localPort;
    }
           
    /** Returns the remote port to which this socket is connected
     *
     * @return the port of target system
     */ 
    public int getPort() {
    return destPort;
    }

    /**
     * just for interface conformity - not implemented 
     */
    public int getSoLinger() throws SocketException {return -1;}

    /**
     * just for interface conformity - not implemented 
     */
    public synchronized int getSoTimeout() throws SocketException {return 0;}
    
    /**
     * just for interface conformity - not implemented 
     */
    public boolean getTcpNoDelay() throws SocketException {return false;}
 
    /**
     * just for interface conformity - not implemented 
     */
    public void setSocketImplFactory(SocketImplFactory factory) {}
    
    /**
     * just for interface conformity - not implemented 
     */
    public void setSoLinger(boolean on, int val) throws SocketException {}

    /**
     * just for interface conformity - not implemented 
     */
    public synchronized void setSoTimeout(int timeout) throws SocketException {}

    /**
     * just for interface conformity - not implemented 
     */
    public void setTcpNoDelay(boolean on) throws SocketException {}

    /**
     * Converts this socket to a String
     *
     * @return a string containing the most important variables of the socket 
     */ 
    public String toString() {
	String result = "";
	result += "LocalPort: " + localPort;
	result += "\nDestPort: " + destPort;
	try {
	    result += "\nLocalAddress: " + getLocalAddress();
	}
	catch (UnknownHostException e) {
	    result += "\nLocalAddress: unbekannt";
	}
	result += "\nDestAddress: " + destAddress;
	result += "\nSequenceNumber: " + seq;
	result += "\nAcknowledgment: " + ack;
	result += "\nTimeOut: " + timeout;
	return result;
    }
    
}
