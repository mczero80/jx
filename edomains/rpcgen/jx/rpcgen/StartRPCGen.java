package jx.rpcgen;

import jx.emulation.MemoryManagerImpl;
import jx.zero.MemoryManager;
import jx.zero.Memory;
import jx.zero.Debug;
import jx.classstore.ClassManager;
import jx.classstore.ClassFinder;

import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class StartRPCGen {

    private static final boolean doDebug = true;
    public static void readZipFile(String filename, MemoryManager memMgr) {
	try {
	RandomAccessFile file = new RandomAccessFile(filename, "r");
	byte [] data = new byte[(int)file.length()];
	file.readFully(data);
	Memory m = memMgr.alloc(data.length);
	m.copyFromByteArray(data, 0, 0, data.length);
	} catch(IOException e) {
	    e.printStackTrace();
	}
    }

    public static Memory getZIP(MemoryManager memMgr, String filename) {
	try {
	    if (doDebug) Debug.out.println("read classes.zip file: "+filename);
	    RandomAccessFile file = new RandomAccessFile(filename, "r");
	    byte [] data = new byte[(int)file.length()];
	    file.readFully(data);
	    Memory m = memMgr.alloc(data.length);
	    m.copyFromByteArray(data, 0, 0, data.length);
	    return m;
	} catch(IOException e) {
	    Debug.throwError("could not read classes.zip file: "+filename);
	    return null;
	}
    }   

    static Vector parsePath(String path) {
	if (path.equals("-")) return null;
	Vector paths = new Vector();
	StringTokenizer tk = new StringTokenizer(path, ":");
	while (tk.hasMoreTokens()) {
	    paths.addElement(tk.nextToken());
	}
	return paths;
    }

    public static void main(String[] args) throws Exception {

       jx.emulation.Init.init();	

       System.out.println("JX RPC Generator version 0.1");

       if (args.length < 3) {
	   System.out.println("Parameters:");
	   System.out.println("     dirname of output files");
	   System.out.println("     zipfiles");
	   System.out.println("     classname");
	   return;
       }

       String classfile = args[args.length-1];

       if (doDebug) Debug.out.println("Generating for class "+classfile);       

       final String dirname = args[0];

       MemoryManager memMgr = new MemoryManagerImpl();
       Memory[] libClasses = new Memory[args.length-2];
       for(int i=1; i<args.length-1; i++) {
	   libClasses[i-1] = getZIP(memMgr, args[i]);
       }

	    
       ClassManager classManager = new ClassManager();
       classManager.addFromZip(null, libClasses);

       ClassFinder classFinder = classManager; // avoid accessing classManager directly
       //classFinder.dump();

       FileSys filesys = new FileSys() {
	       public OutputStream openFile(String filename) {
		   try {
		       Debug.out.println(" Generate file "+dirname+"/"+filename);
		   return  new FileOutputStream(dirname+"/"+filename);
		   } catch(Exception e) { 
		       Debug.out.println("Cannot open "+filename+"; exception: "+e);
		       throw new Error();
		   }
	       }
	   };
       RPCGen rpcgen = new RPCGen(classFinder, filesys);
       rpcgen.generate(classfile);
    }

}

