package java.net;

import jx.zero.*;
//import jx.zero.debug.*;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import jx.net.IPAddress;
import jx.net.TCPSocket;
import jx.net.NetInit;
import jx.net.UnknownAddressException;
import jx.net.TCPInputStream;
import jx.net.TCPOutputStream;

public class Socket {
    private static final int INITIAL_BUFFER_SIZE = 5;

    private IPAddress remoteAddress;
    private int remotePort;

    private NetInit net;
    private TCPSocket tcpSocket;

    private TCPInputStream inputStream;
    private TCPOutputStream outputStream;
    
    public Socket(TCPSocket tcpSocket) {
	this.tcpSocket = tcpSocket;
	inputStream = new TCPInputStream(tcpSocket);
	outputStream = new TCPOutputStream(tcpSocket);
    }

    public Socket(String host, int remotePort) throws java.io.IOException {
	this(new IPAddress(host), remotePort);
    }

    public Socket(IPAddress remoteAddress, int remotePort) throws java.io.IOException {
	this.remoteAddress = remoteAddress;
	this.remotePort = remotePort;

	net = (NetInit) LookupHelper.waitUntilPortalAvailable(InitialNaming.getInitialNaming(), "NET");
	
	Memory[] bufs = new Memory[INITIAL_BUFFER_SIZE];
	for (int i = 0; i < bufs.length; i++)
	    bufs[i] = net.getTCPBuffer1();

	tcpSocket = net.getTCPSocket(remotePort, net.getLocalAddress(), bufs);
	try {
	    tcpSocket.open(remoteAddress, remotePort);
	} catch (UnknownAddressException e) {
	    Debug.out.println("Socket: TCPSocket reported invalid IPAddress");
	    throw new Error("");
	} catch (java.io.IOException e) {
	    Debug.out.println("Socket: TCPSocket reported IOException "+e.getMessage());
	    throw e;
	}
	
	inputStream = new TCPInputStream(tcpSocket);
	outputStream = new TCPOutputStream(tcpSocket);
    }
    
    public OutputStream getOutputStream() {
	return outputStream;
    }

    public InputStream getInputStream() {
	return inputStream;
    }

    public IPAddress getRemoteAddress() {
	return remoteAddress;
    }
    
    public int getRemotePort() {
	return remotePort;
    }

    public void close() throws IOException {
	tcpSocket.close();
    }

    public void setSoTimeout(int timeout)   throws SocketException {
	throw new Error("no setsotimout");
    }
}
