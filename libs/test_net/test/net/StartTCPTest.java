package test.net;

import jx.zero.*;

import jx.net.NetInit;
import jx.net.TCPSocket;
import jx.net.IPAddress;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;

class StartTCPTest {
    public static final boolean verbose = false;

    public static void main(String[] args) throws Exception {
	Naming naming = InitialNaming.getInitialNaming();
	NetInit net = (NetInit)LookupHelper.waitUntilPortalAvailable(naming, args[0]);
	MemoryManager memoryManager = (MemoryManager)LookupHelper.waitUntilPortalAvailable(naming, "MemoryManager");

	    int test = 4;

	    if (args.length == 2) {
		test = Integer.parseInt(args[1]);
	    }

	    if (test == 1) { /* send */
		Memory[] bufs = new Memory[100];
		for (int i = 0; i < bufs.length; i++)
		    bufs[i] = net.getTCPBuffer(); //memoryManager.alloc(1000);
		// send
		TCPSocket sock = net.getTCPSocket(6666, new IPAddress("192.168.34.20"), bufs);
		sock.open(new IPAddress("192.168.34.2"), 6666);
		byte[] sendBuf = new byte[10];
		for (int i=0; i<sendBuf.length; i++) {
		    sendBuf[i] = (byte)i;
		}
		sock.send(sendBuf);
	    }else if (test == 2) {  /* send */
		Socket sock = new Socket( new IPAddress("192.168.34.2"),6666);
		Debug.out.println("new Socket created");
		OutputStream ops = sock.getOutputStream();
		for (int i=65; i < 80; i++)
		    ops.write(i);
		Debug.out.println("flushing");
		ops.flush();
		Debug.out.println("flushed->closing");
		sock.close();
		Debug.out.println("closed");
	    } else if (test == 3) { /* listen */
		Memory[] bufs = new Memory[100];
		for (int i = 0; i < bufs.length; i++)
		    bufs[i] = net.getTCPBuffer(); //memoryManager.alloc(1000);
		// send
		TCPSocket sock = net.getTCPSocket(6666, new IPAddress("192.168.34.20"), bufs);
		// accept conections
		Memory[] newbufs = new Memory[100];
		for (int i = 0; i < newbufs.length; i++)
		    newbufs[i] = net.getTCPBuffer(); //memoryManager.alloc(1000);
		sock.accept(newbufs);
		for (;;) 
		    Debug.out.println(sock.readFromInputBuffer());
	    } else if (test == 4) { /* listen */
		// accept conections
		ServerSocket servSock = new ServerSocket(6666);
		for (;;) {
		    final Socket sock = servSock.accept();
		    if (verbose) Debug.out.println("accepted,... starting Thread");
		    new Thread ("receiver"){
			public void run() {
			    try {
				InputStream ips = sock.getInputStream();
				if (verbose) {
				for (char c=0; true; c=(char)ips.read())
				    Debug.out.println("received: "+c);
				}
			    } catch (Exception e){
				throw new Error(e.getMessage());
			    }
			}
		    }.start();
		}
	    } else if (test == 5) { /* listen */
		// accept conections
		ServerSocket servSock = new ServerSocket(6666);
		Socket sock = servSock.accept();
		final InputStream ips = sock.getInputStream();
		final OutputStream ops = sock.getOutputStream();
/*		new Thread ("receiver"){
		    public void run() {
			try {
			for (char c=0; true; c=(char)ips.read())
			    Debug.out.println("received: "+c);
			} catch (Exception e){
			    throw new Error(e.getMessage());
			}
		    }
		}.start();
*/
		for (;;) {
		    for (int i=65; i < 90; i++){
			ops.write(i);
			ops.flush();
		    }
		}
	    } else if (test == 6) { /* listen and create new domain */
		// accept conections
		ServerSocket servSock = new ServerSocket(6666);
		MemoryManager memMgr = (MemoryManager)naming.lookup("MemoryManager");
		for(int i=0; i<10; i++)  Thread.yield();
		int start = memMgr.getTotalFreeMemory();
		for (;;) {
		    final Socket sock = servSock.accept();
		    int freemem = memMgr.getTotalFreeMemory();
		    Debug.out.println("free mem: "+freemem+"; diff to start: "+(start-freemem));
		    Debug.out.println("accepted,... starting domain");
		    String domainName = "Servlet";
		    String mainLib = "test_net.jll";
		    String startClass = "test/net/TCPWorker";
		    String schedulerClass = null;
		    int HeapInitialSize = 40000;
		    int HeapChunkSize = 10000;
		    int StartGCSize = 80000;
		    String[] argv = new String[0];
		    Object[] portals = new Object [] { sock };
		    Domain domain = DomainStarter.createDomain(domainName, mainLib, startClass, schedulerClass, HeapInitialSize, HeapChunkSize, StartGCSize, argv, portals);
		}
	    } else if (test == 7) { /* listen, create new domain, write data as fast as possible */
		// accept conections
		ServerSocket servSock = new ServerSocket(6666);
		MemoryManager memMgr = (MemoryManager)naming.lookup("MemoryManager");
		for(int i=0; i<10; i++)  Thread.yield();
		int start = memMgr.getTotalFreeMemory();
		for (;;) {
		    final Socket sock = servSock.accept();
		    int freemem = memMgr.getTotalFreeMemory();
		    Debug.out.println("free mem: "+freemem+"; diff to start: "+(start-freemem));
		    Debug.out.println("accepted,... starting domain");
		    String domainName = "Servlet";
		    String mainLib = "test_net.jll";
		    String startClass = "test/net/TCPWorkerWriter";
		    String schedulerClass = null;
		    int HeapInitialSize = 40000;
		    int HeapChunkSize = 10000;
		    int StartGCSize = 80000;
		    String[] argv = new String[0];
		    Object[] portals = new Object [] { sock };
		    Domain domain = DomainStarter.createDomain(domainName, mainLib, startClass, schedulerClass, HeapInitialSize, HeapChunkSize, StartGCSize, argv, portals);
		}
	    }


    }
}
