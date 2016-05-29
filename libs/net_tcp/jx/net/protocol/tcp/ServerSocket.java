package jx.net.protocol.tcp;
import java.io.*;
import java.util.*;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketImpl;
import java.net.SocketImplFactory; 
import jx.timer.*;
import jx.zero.*;

/**
 * this a the implementation of a server socket
 * it has the same methods as a JDK socket, but some are not supported (for example some constructors)
 * there is one difference: because of the architecture of the networking system, our sockets need a reference to a tcp stack
 * it is possible that each socket uses a different stack
 * if compatibility to JDK sockets is wanted, some kind of "socket wrapper" could be used
 * a socket class having the same interface as a JDK socket, using a global known tcp stack and passing this to an instance of this class
 * and returning an object of this class
 */
public class ServerSocket {
    /** reference to the tcp layer we need for calling listen, ... */
    private TCP tcp;
    /** the port we listen at */
    private int localPort;
    /** length of the backlogqueue for buffering incoming connections that maybe accepted by calling accept */
    private int queueLength;
    /** timeout for accept method call */
    private int timeout;
    /** the backlogqueue for buffering incoming connections that maybe accepted by calling accept */
    private Vector backlogqueue;
    /** clock for timeout handling */
    private Clock clock;
    
    /**
     * class constructor - creates a server socket on a specified port
     * 
     * @param tcp a reference to the tcp layer
     * @param port the port we listen at
     *
     * @exception IOException is thrown if socket can´t be initalized, for example if the port is already in use 
     */
     public ServerSocket(TCP tcp, int port, Clock clock) throws IOException {
	this(port, 3);
    } 

    /**
     * class constructor - crates a server socket on a specified port and specifies the buffer size for incoming connections
     *
     * @param tcp a reference to the tcp layer
     * @param port the port we listen at
     * @param backlog incoming connection request get buffered in a queue; the accept method returns sockets from this queue; this parameter
     *                specifies the size of this queue meaning the number of connection requests that can be buffered
     *
     * @exception IOException is thrown if socket can´t be initialized, for example if the port is already in use 
     */
    public ServerSocket(int port, int backlog) throws IOException {
	Socket sock;
	TCP tcp = Socket_ENV.tcp;
	timeout = 0;
	if (port == 0)
	    localPort = tcp.randomPort();
	else if (tcp.checkPort(port))
	    localPort = port;
	else {
	    System.out.println("Port already in use!");
	    throw new IOException();
	}
	queueLength = backlog;
	if (queueLength > 50) {
	    System.out.println("Parameter backlog to big - resetting to 50!");
	    queueLength = 50;
	}
	backlogqueue = new Vector(queueLength);
	for (int i=0; i<queueLength; i++) {
	    sock = new Socket();
	    backlogqueue.addElement(sock);
	    tcp.listen(sock, localPort, 0);
	}
    } 

    /**
     * class constructor - crates a server socket on a specified port and specifies the buffer size for incoming connections
     *
     * @param tcp a reference to the tcp layer
     * @param port the port we listen at
     * @param backlog incoming connection request get buffered in a queue; the accept method returns sockets from this queue; this parameter
     *                specifies the size of this queue meaning the number of connection requests that can be buffered
     * @param bindAddr a InetAddress specifing the address the socket should bind to - currently only our own IP-address is supported
     *                 so if an other address is submitted an IOException is thrown
     *
     * @exception IOException is thrown if socket can´t be initialized, for example if the port is already in use 
     */
    public ServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {   
	this(port, backlog);
	// if the bindAddr is null, we should accept incoming connections on any address of our maybe multihomed system
	// we don´t support multihomed systems yet, so we just check but do nothing else
	if (bindAddr == null) {}
	else if (!(tcp.arrayCompare(bindAddr.getAddress(), tcp.getmyselfIP()))) {
	    System.out.println("bindAddr doesn´t match our own IP-address - other bindings not yet supported!");
	    throw new IOException();
	}
    } 

    /**
     * return a socket specifying a established connection
     * this method searches the backlog queue for the first socket with state == ESTABLISHED and returns this socket
     * a timeout how long the search should last can be submitted via the method setSoTimeout but must be called before calling this method
     * 
     * @return the first socket with a established connection
     *
     * @exception SocketException actually not really thrown - just for interface conformity
     * @exception InterruptedIOException if a timeout expired if set with setSoTimeout
     */
    public Socket accept() throws SocketException, java.io.InterruptedIOException {
	Socket socket;
	int starttime = clock.getTicks_low();
	// -1 indicates no timeout
	int endtime = -1;
	if (timeout > 0)
	    endtime = starttime + timeout/10;
	
	while (true) {
	    // search the backlog queue for established connections
	    for (Enumeration e = backlogqueue.elements() ; e.hasMoreElements() ;) {
		socket = (Socket)e.nextElement();
		if (socket.getState() == State.ESTAB) {
		    backlogqueue.removeElement(socket);
		    // because we removed one socket, we have to enter a new one ready for a new connection
		    Socket s = new Socket();
		    backlogqueue.addElement(s);
		    tcp.listen(s,localPort, 0);
		    return socket;
		}
	    }
	    // no established connection found - if timeout set, see wether to continue or raise an exception
	    if (endtime != -1) {
		if (clock.getTicks() >= endtime) {
		    throw new java.io.InterruptedIOException();
		}
	    }
	}
    }

    /** closes the connection and therefore removes all sockets of the backlogqueue by calling abort to each socket
     * 
     * @exception IOException is thrown if an internal error occures 
     */ 
    public void close() throws IOException {
	for (Enumeration e = backlogqueue.elements() ; e.hasMoreElements() ;) {
	    tcp.abort((Socket)e.nextElement());
	}
    }
    
    /** Returns the local address of this server socket */ 
    public InetAddress getInetAddress() throws UnknownHostException {
	return java.net.InetAddress.getByName(new String(tcp.getmyselfIP()));
    }
    /** Returns the port on which this socket is listening */ 
    public int getLocalPort() {
	return localPort;
    }
    
    /** Retrieve setting for SO_TIMEOUT */
    public synchronized int getSoTimeout() throws IOException {
	return timeout;
    }
    
    /** Subclasses of ServerSocket use this method to override accept() to return their own subclass of socket
     * not implemented - dummy method
     */
    protected final void implAccept(Socket socket) throws IOException {} 
    
    /** Sets the server socket implementation factory for the application 
     * not implemented - dummy method
     */
    public synchronized static void setSocketFactory(SocketImplFactory fac) throws IOException {} 
    
    /** Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds
     * this timeout applies to the accept call - if no timeout is specified accept will search as long as it finds the first established socket
     * otherwise it throws an exception if the timeout set with this method has expired before finding a socket
     *
     * @param timeout the expire time in milliseconds
     *
     * @exception SocketException just for interface conformity
     */
    public synchronized void setSoTimeout(int timeout) throws SocketException {
	if (timeout < 0)
	    return;
	else
	    this.timeout = timeout;
    }
    
    /** Returns the implementation address and implementation port of this socket as a String */ 
    public String toString() {
	String result = "";
	try {
	    result += "Address: " + getInetAddress();
	}
	catch (UnknownHostException e) {
	    result += "Address: unknown";
	}
	result += "\nPort: " + localPort;
	return result;
    }
}

