package jx.compiler;

import jx.zero.*;
import jx.zero.debug.*;
import jx.zero.memory.*;

import jx.compiler.CompilerOptions;
import jx.compiler.StaticCompiler;
import jx.compiler.persistent.*;
import jx.compiler.execenv.BCClass;
import jx.compiler.execenv.IOSystem;

import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;

import jx.zip.*;

public class OnlineCompilerDomain implements ByteCodeTranslater, Service {

    private BootFS bootfs;
    private MemoryManager memMgr;
    private CompilerOptions opts;
    private Naming naming;
    private ComponentManager cm;

    public OnlineCompilerDomain() {
	naming = InitialNaming.getInitialNaming();	

	bootfs = (BootFS)naming.lookup("BootFS");
	memMgr = (MemoryManager)naming.lookup("MemoryManager");
	cm = (ComponentManager)naming.lookup("ComponentManager");

	opts = new CompilerOptions();
	opts.setUseNewCompiler(true);
	//opts.setDebug(true);
    }

    public static void init(Naming naming) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	Debug.out = new jx.zero.debug.DebugPrintStream(new jx.zero.debug.DebugOutputStream(d));

	Debug.out.println("Compiler Domain started.");
	
	CPUManager c = (CPUManager)naming.lookup("CPUManager");

	OnlineCompilerDomain oncDomain = new OnlineCompilerDomain();
	ByteCodeTranslater translater = oncDomain;
	naming.registerPortal(translater,"ByteCodeTranslater");

       /* old  
	c.setThreadName("ByteCodeTranslater - Domain");
	Debug.out.println("Compiler gets ready");
	c.receive(translater);	
       */
    }

    public void translate(String zip, String lib) throws Exception {
	String info=null;
	int i;
	
	if ((i=lib.indexOf(".jll"))>0) {
	    info = lib.substring(0,i)+".jln";
	} else if ((i=lib.indexOf(".jdx"))>0) {
	    info = lib.substring(0,i)+".jln";
	} else if ((i=lib.indexOf("."))>0) {
	    info = lib.substring(0,i)+".jln";
	}

	translate(zip,lib,info);
    }

    public void translate(String zip, String lib, String info) throws Exception {
	Vector needed;

	Debug.out.println("==== compile "+zip+" ====");

	if (!bootfs.lookup(zip)) {
	    throw new Error("file not found "+zip);
	}
      
	/* lookup and/or compile dependences */
      
	needed = findDependences(zip);
	
	if (needed!=null) {
 	    for (int i=0;i<needed.size();i++) {
	        String libName = (String)needed.elementAt(i);
		if (!bootfs.lookup(libName+".jll")) {
		    if (bootfs.lookup(libName+".zip")) {
			translate(libName+".zip",libName+".jll",libName+"jln");
		    } else {
			Debug.out.println("zip not found "+libName+".zip !");
		    }
		}
	    }
	}
	
	/* compile library */

	compile(lib,info,zip,needed);
    }
 
    public void compile(String targetName, String linkOut, String domainClasses,Vector needed) throws Exception {

	PrintStream gasFile=null;
	ExtendedDataOutputStream codeFile=null;
	ExtendedDataOutputStream tableOut=null;

	Debug.out.println("Reading domain classes from "+domainClasses);
	Debug.out.println("Compiling domain to "+targetName);
	Debug.out.println("Writing linker output to "+linkOut);

	ReadOnlyMemory domClasses =  bootfs.getFile(domainClasses);

	Vector libs = new Vector();
	Vector links = new Vector();

	if (needed!=null) {
	    for (int i=0;i<needed.size();i++) {
		String libName = (String)needed.elementAt(i);	      
		libs.addElement(libName+".zip");
		links.addElement(libName+".jln");
	    }
	}

	ReadOnlyMemory[] libClasses = null;
	if (libs.size()>0) {
	    Debug.verbose("libs:");
	     libClasses = new ReadOnlyMemory[libs.size()];
	     for(int i=0; i<libs.size(); i++) {
		 libClasses[i] = bootfs.getFile((String)libs.elementAt(i));
		 if (libClasses[i] == null) {
		     Debug.message("Cannot find file "+(String)libs.elementAt(i));
		     return;
		 }
		 Debug.verbose("+ "+(String)libs.elementAt(i));
	     }
	} else {
	     libClasses = new Memory[0];
	}

	ExtendedDataInputStream[] tableIn;
	if (links.size()>0) {
	    Debug.verbose("link: ");
	    tableIn=new ExtendedDataInputStream[links.size()];
	    for(int i=0; i<links.size(); i++) {
		ReadOnlyMemory memIn = bootfs.getFile((String)links.elementAt(i));
		if (memIn==null) {
		     Debug.message("Cannot find file "+(String)links.elementAt(i));
		     throw new Error("Cannot find file");
		}
		 Debug.verbose("+ "+(String)links.elementAt(i));
		tableIn[i] = new ExtendedDataInputStream(new MemoryInputStream(memIn));
	    }
	} else {
	    tableIn=new ExtendedDataInputStream[0];
	}

	Memory codeFileMemory = memMgr.alloc(1024*1024);
	Memory tableOutMemory = memMgr.alloc(1024*1024);

	codeFile = new ExtendedDataOutputStream(new MemoryOutputStream(codeFileMemory));
	tableOut = new ExtendedDataOutputStream(new MemoryOutputStream(tableOutMemory));

	IOSystem io = new IOSystem() {
		public OutputStream getOutputStream(String filename) throws IOException {
		    return new OutputStream() {
			    public void write(int b) throws IOException {}
			};
		}
		public void set(String path) {}
	    };

	opts.setUseNewCompiler(true);
	opts.setDebug(true);
	opts.setNeededLibs(libs);

	//try {

	  StaticCompiler compiler = new StaticCompiler(gasFile, codeFile,
						       tableOut,
						       domClasses,
						       libClasses, tableIn,
						       opts,io);
	  compiler=null;
	  /*} catch(Exception ex) {
	  Debug.out.println("Static Compiler exception "+ex.getClass().getName());
	  ex.printStackTrace();
	  throw ex;
	  } */
	
	cm.registerLib(targetName,codeFileMemory);
	cm.registerLib(linkOut,tableOutMemory);	

	Debug.out.println("==== compilation done =====");
    }

    /*
     * Helpers
     *
     */

    private Vector findDependences(String zipFileName) {
	Vector needed = new Vector();
	
	ReadOnlyMemory zipFile = bootfs.getFile(zipFileName);
	ZipFile zip = new ZipFile(zipFile);
	ZipEntry entry = null;

	Debug.verbose("read "+zipFileName);
	
	while ((entry=zip.getNextEntry())!=null) {
	    if (entry.isDirectory()) continue;
	    if (entry.getName().equals("libs.dep")) {
		Debug.verbose("find libs.dep");
		DataInputStream dis = new DataInputStream(new MemoryInputStream(entry.getData()));
		String line = null;	
		try {
		    while ((line=dis.readLine())!=null) {
			if (line.equals("#EOF")) break;
			if (!line.startsWith("#")) {
			    StringTokenizer tk = new StringTokenizer(line," ,;:");
			    while (tk.hasMoreTokens()) {
				String dep=tk.nextToken();
				if (zipFileName.equals(dep+".zip")) {
				    Debug.message("WARN: conflicting dependence name "+dep+" skipped!");
				} else {
				    Debug.verbose("need: "+dep);
				    needed.addElement(dep);
				}
			    }
			}
		    }
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
	    }
	}
	
	return needed;
  }
}
