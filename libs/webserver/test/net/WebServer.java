package test.net;

import jx.zero.*;
import jx.zero.debug.*;
import jx.net.NetInit;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.util.StringTokenizer;
import java.net.Socket;
import java.net.ServerSocket;

import jx.fs.*;

public class WebServer {

    private static final boolean debug = false;

    static NetInit net;
    static Memory fileBuf;
    static byte[] fbytes;
    Memory buffer;

    Naming naming = InitialNaming.getInitialNaming();
    FS fs=null;

    public static void main (String[] args) throws Exception {
	new WebServer(args);
    }

    static void usage() {
	Debug.out.println("[-verbose] [-port <port>] [-fs <FSName>] [-threads] [-dummyfiles]");
    }

     public WebServer(String[] args) throws Exception {
	 int port = 80;
	 boolean opt_threads = false;
	 String fsname=null;
	 boolean opt_use_fs = false;
	 boolean opt_verbose = false;
	 boolean opt_dummyfiles = false;

	 int argc;
	 for(argc = 0; argc < args.length; argc++) {
	     if (args[argc].equals("-fs")) {
		 argc++;
		 opt_use_fs = true;
		 fsname = args[argc];
	     } else if (args[argc].equals("-verbose")) {
		 opt_verbose = true;
	     } else if (args[argc].equals("-threads")) {
		 opt_threads = true;
	     } else if (args[argc].equals("-port")) {
		 argc++;
		 port = Integer.parseInt(args[argc]);
	     } else if (args[argc].equals("-dummyfiles")) {
		 argc++;
		 opt_dummyfiles = true;
	    } else {
		Debug.out.println("Unknown option ignored: "+args[argc]);
		break;
	    }
	}

	if (opt_use_fs)	{
	    Debug.out.println("Webserver: use filesystem with name "+fsname);
	    fs = (FS)LookupHelper.waitUntilPortalAvailable(naming, fsname);

	    // create buffer
	    MemoryManager memoryManager = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	    buffer = memoryManager.alloc(1024);

	    if (opt_dummyfiles) {
		// create files
		createFile(fs, "index.html", 
			   "<html><head><title>JX index.html Testseite (FILEXX)</title><body bgcolor=ffffff>\n\n" +
			   "<center><h2>Herzlich willkommen auf der JX-FILE-Testseite</h2></center><br><br>\n" +
			   "Link zur <a href=page2.html>zweiten</a> Seite</body></html>\n");
		
		createFile(fs, "page2.html", 
			   "<html><head><title>JX Page2 (FILE)</title><body bgcolor=ffffff>\n\n" +
			   "<center><h2>Herzlich willkommen auf der JX-FILEXX-Testseite</h2></center><br><br>\n" +
			   "Link zur <a href=index.html>ersten</a> Seite</body></html>\n");
	    }
	}

	ServerSocket ssock = new ServerSocket(port);

	if (opt_threads) Debug.out.println("Webserver: use one thread per connection");
	else Debug.out.println("Webserver: use one domain per connection");

	// accept conections
	while (true) {
	    if (debug) Debug.out.println("Network: accept() called");
	    final Socket sock = ssock.accept();
	    if (debug) Debug.out.println("Network: got new connection");
	    if (opt_threads)
		startWorkerThread(sock);
	    else
		startWorkerDomain(sock);
	}
    }

    private final void createFile(FS fs, String name, String contents) throws Exception {
	fs.create(name, InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
	Inode inode = (Inode)fs.lookup(name);
	byte[] b = contents.getBytes();
	buffer.copyFromByteArray(b, 0, 0, b.length);
	inode.write(buffer, 0, b.length);
	inode.decUseCount();
    }
         
    private final void startWorkerThread(final Socket sock)  {
	    new Thread ("WebWorker"){
		public void run() {
		    try {
			TCPWebWorker.processRequest(sock, fs);
		    } catch(Exception e) {
			throw new Error();
		    }
		}
	    }.start();
    }

    private final void startWorkerDomain(final Socket sock){
	String domainName = "Servlet";
	String mainLib = "webserver.jll";
	String startClass = "test/net/TCPWebWorker";
	String[] argv = new String[0];
	Object[] portals = new Object [] { sock, fs };
	boolean useChunkedGC = false;
	int codesize = 10000;
	if (useChunkedGC) {
	    int HeapInitialSize = 40000;
	    int HeapChunkSize = 10000;
	    int StartGCSize = 80000;
	    Domain domain = DomainStarter.createDomain(domainName, mainLib, startClass, (String)null, HeapInitialSize, HeapChunkSize, StartGCSize, argv, portals);
	} else {
	    int HeapSize = 400000;
	    Domain domain = DomainStarter.createDomain(domainName, mainLib, startClass, HeapSize, codesize, argv, portals);
	}
    }

}








