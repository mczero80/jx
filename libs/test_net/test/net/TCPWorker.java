package test.net;

import jx.zero.*;
import jx.zero.debug.*;

import jx.net.NetInit;
import jx.net.TCPSocket;
import jx.net.IPAddress;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;

class TCPWorker {
    public static void init(Naming naming, String[] argv, Object[] portals) throws Exception {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	Socket sock = (Socket) portals[0];
	InputStream ips = sock.getInputStream();
	for (char c=0; true; c=(char)ips.read()) {
	    Debug.out.println("received: "+c);
	    if (c == 'X') {
		DomainManager domainManager = (DomainManager) naming.lookup("DomainManager");
		domainManager.terminateCaller();
	    }
	}
    }
}
