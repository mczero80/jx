package java.net;

import jx.zero.*;
//import jx.zero.debug.*;
import java.io.OutputStream;
import java.io.InputStream;
import jx.net.TCPSocket;
import jx.net.NetInit;


public class ServerSocket {
    private static final int INITIAL_BUFFER_SIZE = 15;

    private NetInit net;
    private TCPSocket tcpSocket;
    private int port;

    public ServerSocket(int port) {
	this.port = port;

	net = (NetInit) LookupHelper.waitUntilPortalAvailable(InitialNaming.getInitialNaming(), "NET");
	
	Memory[] bufs = new Memory[INITIAL_BUFFER_SIZE];
	for (int i = 0; i < bufs.length; i++) bufs[i] = net.getTCPBuffer1();
	tcpSocket = net.getTCPSocket(port, net.getLocalAddress(), bufs);
    }
    
    public Socket accept() throws java.io.IOException{
	Memory[] bufs = new Memory[INITIAL_BUFFER_SIZE];
	for (int i = 0; i < bufs.length; i++) bufs[i] = net.getTCPBuffer1();
	try {
	    return new Socket(tcpSocket.accept(bufs));
	} catch (java.io.IOException e) {
	    Debug.out.println("ServerSocket: TCPSocket reported IOException "+e.getMessage());
	    throw e;
	}

    }

    public void close()  throws java.io.IOException {
	tcpSocket.close();
    }
}
