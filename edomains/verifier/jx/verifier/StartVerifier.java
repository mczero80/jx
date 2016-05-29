package jx.verifier;

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
import java.util.Enumeration;

import jx.compspec.MetaInfo;

public class StartVerifier {

    private static final boolean doDebug = true;

    static Vector metas;
    private static String components;
    private static String componentsDir;
    private static String componentsInc;
    private static String componentsAll;
    private static String componentToVerify;
    /*
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
    */

    public static void main(String[] args) throws Exception {

	jx.emulation.Init.init();	

       System.out.println("JX Bytecode Verifier version 0.5");
       VerifierOptions options = new VerifierOptions();
       args = options.parseArgs(args);
       if (args.length == 0) {
	   System.out.println("Need component name: verifier [options] components componentsDir	componentsInc componentsAll componentToVerify ");
	   System.out.println(VerifierOptions.helpString);
	   return;
       }

	components    = args[0];
	componentsDir = args[1];
	componentsInc = args[2];
	componentsAll = args[3];

	componentToVerify = args[4];

	//	metas = jx.compspec.StartBuilder.readlibs(componentsInc, componentsDir);
	metas = jx.compspec.StartBuilder.findlibs(componentsInc, componentsDir, components, componentsAll);

       /*
       String classfiles = args[0];
       Vector libs = parsePath(args[1]);
       */

       if (doDebug) System.out.println("Verifying "+componentToVerify);       

       /*       MemoryManager memMgr = new MemoryManagerImpl();
       Memory domClasses = getZIP(memMgr, classfiles);
       */   
       Memory[] zi=null;
       Memory ci=null;
       for(int i=0; i<metas.size(); i++) {
	    MetaInfo s = (MetaInfo)metas.elementAt(i); // process this component
	    System.out.print("* "+s.getComponentName()+"... ");

	    if (! s.getComponentName().equals(componentToVerify)) continue;

	    String libdir = componentsDir+"/";
	    String zipname = libdir+s.getComponentName()+".zip";
	    String jllname = libdir+s.getComponentName()+".jll";

	    File zipfile = new File(zipname);
	    Vector libs = new Vector();
	    String[] neededLibs = s.getNeededLibs();
	    for(int j=0; j<neededLibs.length; j++) {
		String nl = libdir+neededLibs[j]+".zip";
		System.out.println("use component "+nl);
		libs.addElement(nl);
	    }

	    zi = jx.compiler.CompileNative.getZIPs(libs);
	    ci = jx.compiler.CompileNative.getZIP(zipname);
	    System.out.println("verify component "+zipname);
	    break;
       }
	    
       ClassManager classManager = new ClassManager();
       classManager.addFromZip(ci, zi);

       ClassFinder classFinder = classManager; // avoid accessing classManager directly
       Enumeration verifyEnum = classManager.getDomClasses();
       Verifier verifier = new Verifier(classFinder, verifyEnum, options);


    }

}

