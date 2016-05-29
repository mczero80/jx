package jx.fsshell;

import jx.streams.*;
import java.io.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.shell.*;

import jx.fs.*;

public class Main {
    class OutputStreamProxy extends OutputStream {
	OutputStreamPortal o;
	OutputStreamProxy(OutputStreamPortal o) {
	    this.o = o;
	}
	public void write(int c) throws IOException {
	    //Debug.out.println("PROXY: write"+c);
	    o.write(c);
	}
	public void flush() throws IOException {
	    o.flush();
	}	
    }
    
    class InputStreamProxy extends InputStream {
	InputStreamPortal o;
	InputStreamProxy(InputStreamPortal o) {
	    this.o = o;
	}
	public int read() throws IOException {

	    int c = o.read();
	    while (c==255) { /* ignore telnet control sequence */
		Debug.out.println("PROXY: control sequence");
		o.read();
		o.read();
		c = o.read();
	    }
	    Debug.out.println("PROXY: read: "+c);
	    return c;
	}
	
    }

    FS fs;
    String cwd="/";
    MemoryManager memoryManager;
    Memory buffer;

    public static void init(Naming naming, String[] argv, Object[] objs)  throws Exception {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));

	Debug.out.println("jx.fsshell.Main");
	StreamProvider streamProvider=null;
	try {
	    streamProvider =  (StreamProvider) objs[0];
	    FS fs = (FS)objs[1];
	    
	    new Main(streamProvider, fs);
	    
	} catch(Throwable e) {
	    Debug.out.println("FSShell: Exception "+e);
	}
	streamProvider.close();
	DomainManager domainManager = (DomainManager) naming.lookup("DomainManager");
	domainManager.terminateCaller();	
    }

    public static void main(String[] args) throws Exception {
	Naming naming = InitialNaming.getInitialNaming();
	if (args.length != 2) throw new Error("Need name of stream provider as first argument and FS as second arg"); 
	StreamProvider streamProvider = (StreamProvider)LookupHelper.waitUntilPortalAvailable(naming, args[0]);	
	new Main(streamProvider,null);
    }

    public Main(StreamProvider streamProvider, FS fs0) throws Exception {
	this.fs = fs0;
	Naming naming = InitialNaming.getInitialNaming();
	Shell shell = new Shell(new OutputStreamProxy(streamProvider.getOutputStream()), new InputStreamProxy(streamProvider.getInputStream()));
	memoryManager = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	buffer = memoryManager.alloc(4096);

	shell.register("ls", new Command() {
		public void command(PrintStream out, String[] args) throws FSException  {
		    String[] names = null;
		    Inode inode;
		    if (args.length == 0) {
			inode = fs.getCwdInode();
		    } else {
			inode = (Inode)fs.lookup(args[0]);
		    }
		    names = inode.readdirNames();
		    for (int i = 0; i < names.length; i++)
			out.println((String)names[i]);
		}
		public String getInfo() { return "list directory"; }	    
	    });


	shell.register("mkdir", new Command() {
		public void command(PrintStream out, String[] args) throws FSException  {
		    fs.mkdir(args[0], InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
		}
		public String getInfo() { return "create directory"; }	    
	    });
		   
	shell.register("cd", new Command() {
		public void command(PrintStream out, String[] args) throws FSException {
			fs.cd(args[0]);
		}
		public String getInfo() { return "change working directory"; }	    
	    });

	shell.register("cat", new Command() {
		public void command(PrintStream out, String[] args) throws FSException {
		    Inode inode = (Inode)fs.lookup(args[0]);
		    int l = inode.getLength();
		    int o=0;
		    while(l>0) {
			int m = l<4096?l:4096;
			inode.read(buffer, o,  m);
			for(int i=0; i<m; i++) out.write(buffer.get8(i));
			l-=m;
			o+=m;
		    }
		    out.flush();
		}
		public String getInfo() { return "print file on standard output"; }	    
	    });
		   



	shell.mainloop();
    }
}

