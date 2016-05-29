package jx.rlogin;


import jx.zero.*;
import jx.zero.debug.*;
import jx.net.NetInit;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.util.StringTokenizer;
import java.net.Socket;
import java.net.ServerSocket;

import java.io.IOException;

import jx.streams.StreamProvider;
import jx.streams.InputStreamPortal;
import jx.streams.OutputStreamPortal;

import jx.fs.FS;

public class RloginServer {

    public static void main (String[] args) throws Exception {
	new RloginServer(args);
    }

    RloginServer(String[] args) throws Exception {
	int port = Integer.parseInt(args[1]);

	Naming naming = InitialNaming.getInitialNaming();
	FS fs = (FS)LookupHelper.waitUntilPortalAvailable(naming, args[2]);

	ServerSocket ssock = new ServerSocket(port);
	
	// accept conections
	while (true) {
	    final Socket sock = ssock.accept();
	    Debug.out.println("Network: got new connection");
	    String domainName = "RloginSession";
	    String mainLib = "fsshell.jll";
	    String startClass = "jx/fsshell/Main";
	    String schedulerClass = null;
	    int heapSize = 80000;
	    String[] argv = new String[0];
	    StreamProvider stream = new SocketStreamProvider(sock);
	    Object[] portals = new Object [] { stream, fs };
	    Domain domain = DomainStarter.createDomain(domainName, mainLib, startClass, schedulerClass, heapSize, argv, portals);
	}
    }

    class SocketOutputStream implements OutputStreamPortal {
	OutputStream out;
	SocketOutputStream(Socket sock) { this.out = sock.getOutputStream(); }
	public void write(int b) throws IOException {
	    //Debug.out.println("SOCKOUT: write"+b);
	    out.write(b);
	}
	public void flush() throws IOException {
	    out.flush();
	}
    }

    class SocketInputStream implements InputStreamPortal {
	InputStream in;
	SocketInputStream(Socket sock) { this.in = sock.getInputStream(); }
	public int read() throws IOException {
	    return in.read();
	}
    }

    class SocketStreamProvider implements  StreamProvider, Service {
	SocketInputStream in;
	SocketOutputStream out;
	Socket sock;
	SocketStreamProvider(Socket sock) {
	    this.sock = sock;
	    in = new SocketInputStream(sock);
	    out = new  SocketOutputStream(sock);
	}

	public InputStreamPortal getInputStream() {
	    return in;
	}
	
	public OutputStreamPortal getOutputStream() {
	    return out;
	}
	
	public OutputStreamPortal getErrorStream() {
	    return out;
	}
	public void close() {
	    sock.close();
	}
    }
}
