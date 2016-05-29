package test.compiler;

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

public class OnlineCompiler {

    MemoryManager memMgr;
    Naming naming;
    BootFS bootFS;

    public OnlineCompiler(Naming naming) {
	this.naming = naming;

	if (Debug.out==null) {
	  DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	  Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	}

	Debug.out.println("Compiler started.");
	bootFS = (BootFS)naming.lookup("BootFS");
	memMgr = (MemoryManager) naming.lookup("MemoryManager");
    }

    static Vector parsePath(String path) {
	Vector paths = new Vector();
	StringTokenizer tk = new StringTokenizer(path, ":");
	while (tk.hasMoreTokens()) {
	    paths.addElement(tk.nextToken());
	}
	return paths;
    }

    public void compile(String targetName, String linkOut, String domainClasses, String env) {

	throw new Error("not implemented");

	/*
	PrintStream gasFile=null;
	ExtendedDataOutputStream codeFile=null;
	ExtendedDataOutputStream tableOut=null;

	boolean hasLibInfo = false;
	
	Debug.out.println("Reading domain classes from "+domainClasses);
	Debug.out.println("Compiling domain to "+targetName);
	Debug.out.println("Writing linker output to "+linkOut);

	ReadOnlyMemory domClasses =  bootFS.getFile(domainClasses);

	Vector libs = new Vector();
	Vector links = new Vector();

	CompilerOptions opts = new CompilerOptions(libs,env);
	opts.setUseNewCompiler(true);
	//opts.setDebug(true);

	ZipFile zip = new ZipFile(domClasses);
	ZipEntry entry = null;
	while ((entry = zip.getNextEntry()) != null) {
	    if (entry.isDirectory()) {
		continue;
	    }
	    if (entry.getName().equals("libs.dep")) {
		MemoryInputStream depin = new MemoryInputStream(entry.getData());
		//DataInputStream dis = new DataInputStream(new BufferedInputStream(depin));
		DataInputStream dis = new DataInputStream(depin);
		String line=null;

		try {

		while ((line=dis.readLine())!=null) {
		  if (line.equals("#EOF")) break;
		  if (!line.startsWith("#")) {
		    StringTokenizer tk = new StringTokenizer(line, " ,;:");
		    while (tk.hasMoreTokens()) {
		      String libFile = tk.nextToken();
		      int idx;					       
		      if ((idx=libFile.indexOf(".zip"))>0) {
			libFile = libFile.substring(0,idx);
		      }			
		      libs.addElement(libFile+".zip");
		      links.addElement(libFile+".jln");
		    }
		  }
		}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		hasLibInfo=true;
	    }
	}
	entry=null;
	zip=null;

	if (!hasLibInfo) {
	    Debug.out.println("!!!! no libs.dep found !!!!");
	}

	ReadOnlyMemory[] libClasses = null;
	if (libs.size()>0) {
	     libClasses = new ReadOnlyMemory[libs.size()];
	     for(int i=0; i<libs.size(); i++) {
		 libClasses[i] = bootFS.getFile((String)libs.elementAt(i));
		 if (libClasses[i] == null) {
		     Debug.out.println("Cannot find file "+(String)libs.elementAt(i));
		     return;
		 }
	     }
	} else {
	     libClasses = new ReadOnlyMemory[0];
	}

	ExtendedDataInputStream[] tableIn;
	if (links.size()>0) {
	    tableIn=new ExtendedDataInputStream[links.size()];
	    for(int i=0; i<links.size(); i++) {
		ReadOnlyMemory memIn = bootFS.getFile((String)links.elementAt(i));
		if (memIn==null) {
		     Debug.out.println("Cannot find file "+(String)links.elementAt(i));
		     throw new Error("Cannot find file");
		}
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
	    };


	try {
	    StaticCompiler compiler = new StaticCompiler(gasFile, codeFile,
							 tableOut,
							 domClasses,
							 libClasses, tableIn,
							 opts,io);
	} catch(Exception ex) {
	    Debug.out.println("Static Compiler exception");
	}
	
	naming.registerLib(targetName,codeFileMemory);
	naming.registerLib(linkOut,tableOutMemory);	
	*/
    }

    /*
    public void compile(String targetName,
		  String linkOut,
		  String domainClasses,
		  String zipList,
		  String jlnList,
		  String env) {
	PrintStream gasFile=null;
	ExtendedDataOutputStream codeFile=null;
	ExtendedDataOutputStream tableOut=null;

	Debug.out.println("Reading domain classes from "+domainClasses);
	Debug.out.println("Compiling domain to "+targetName);
	Debug.out.println("Writing linker output to "+linkOut);

	Memory domClasses =  bootFS.getFile(domainClasses);

	Memory[] libClasses = null;
	Vector libs = parsePath(zipList);
	if (! zipList.equals("-")) {
	     libClasses = new Memory[libs.size()];
	     for(int i=0; i<libs.size(); i++) {
		 libClasses[i] = bootFS.getFile((String)libs.elementAt(i));
		 if (libClasses[i] == null) {
		     Debug.out.println("Cannot find file "+(String)libs.elementAt(i));
		     return;
		 }
	     }
	} else {
	     libClasses = new Memory[0];
	}

	ExtendedDataInputStream[] tableIn;
	if (! jlnList.equals("-")) {
	    Vector links = parsePath(jlnList);
	    tableIn=new ExtendedDataInputStream[links.size()];
	    for(int i=0; i<links.size(); i++) {
		Memory memIn = bootFS.getFile((String)links.elementAt(i));
		if (memIn==null) {
		     Debug.out.println("Cannot find file "+(String)links.elementAt(i));
		     throw new Error("Cannot find file");
		}
		tableIn[i] = new ExtendedDataInputStream(new MemoryInputStream(memIn));
	    }
	} else {
	    tableIn=new ExtendedDataInputStream[0];
	}

	CompilerOptions opts = new CompilerOptions(libs,env);
	opts.setUseNewCompiler(true);

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
	   };


	try {
	StaticCompiler compiler = new StaticCompiler(gasFile, codeFile,
						     tableOut,
						     domClasses,
						     libClasses, tableIn,
						     opts,io);
	} catch(Exception ex) {
	    Debug.out.println("Static Compiler exception");
	}

	naming.registerLib(targetName,codeFileMemory);
	naming.registerLib(linkOut,tableOutMemory);
    }
    */
}
