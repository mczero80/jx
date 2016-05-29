/**
 * parser and container class for compiler options
 *
 * Autor: Chr. Wawersich
 */

package jx.compiler;

import jx.zero.Debug;

import java.util.Vector;
import java.util.StringTokenizer;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.RandomAccessFile;

import jx.compiler.execenv.BCClass;
import jx.compiler.execenv.BCMethod;

public class CompilerOptionsNative extends CompilerOptions {

    protected CompilerOptionsNative() {}

    /**
     * constructor to parse command line options
     */

    public CompilerOptionsNative(String[] args) {
        //this();
	parseArgs(args);
    }

    public void parseOptionLine(String line) {
	StringTokenizer tk = new StringTokenizer(line, " ");
	Vector args = new Vector();
	while (tk.hasMoreTokens()) {
	    String opt = tk.nextToken();
	    if (opt=="") continue;
	    args.addElement(opt);
	}
	args.trimToSize();
	String[] argv = new String[args.size()];
	int i;
	for (i=0;i<argv.length;i++) argv[i]=(String)args.elementAt(i);
	for (i=0;i<argv.length;) i+=parseOption(argv,i);
    }

    private int parseOption(String[] args, int i) {	
	int o = i;

	if (args[i].charAt(0)!='-') return 1;

	if (args[i].equals("-O")) {
	    doFastStatics   = true;
	    doOptimize      = true;
	    doOptSizeChecks = true;
	    doAlignCode     = true;
	} else if (args[i].equals("-O2")) {
	    doFastStatics   = true;
	    doOptimize      = true;
	    doOptSizeChecks = true;
	    doAlignCode     = true;
	    doInlining      = true;
	    doCF            = true;
	} else if (args[i].equals("-path")) {
	    doOptExecPath=true;
	} else if (args[i].equals("-alignCode")) {
	    doAlignCode = true;
	} else if (args[i].equals("-noAlignCode")) {
	    doAlignCode = false;
	} else if (args[i].equals("-old")) {
	    doNewCode = false;
	} else if (args[i].equals("-new")) {
	    doNewCode = true;
	} else if (args[i].startsWith("-new:")) {
	    newCompiler = new Vector();
	    doNewCode=true;
	    StringTokenizer tk = new StringTokenizer(args[i], ":");
	    tk.nextToken();
	    while (tk.hasMoreTokens()) newCompiler.addElement(tk.nextToken());
	} else if (args[i].equals("-imcode")) {
	    doPrintIMCode = true;
	} else if (args[i].equals("-noirq")) {
	    doProfileNoIRQ = true;
	} else if (args[i].equals("-profile")) {
	    doProfiling=true;
	} else if (args[i].equals("-dbgStack")) {
	    doStackTrace=true;
	} else if (args[i].equals("-trace")) {
	    doTrace=true;
	} else if (args[i].equals("-exceptions")) {
	    doExceptions=true;
	} else if (args[i].equals("-noExceptions")) {
	    doExceptions=false;
	} else if (args[i].equals("-paranoid")) {
	    doParanoid=true;
	} else if (args[i].equals("-magic")) {
	    doMagicTests=true;		      
	} else if (args[i].equals("-noprofile")) {
	    doProfiling=false;
	} else if (args[i].equals("-events")) {
	    doEventLoging=true;
	} else if (args[i].equals("-log")) {
	    logMethods = new Vector();
	    logMethods.addElement("*");
	} else if (args[i].startsWith("-log:")) {
	    logMethods = new Vector();
	    StringTokenizer tk = new StringTokenizer(args[i],":");
	    tk.nextToken();
	    while (tk.hasMoreTokens()) logMethods.addElement(tk.nextToken());
	} else if (args[i].equals("-fma")) {
	    doFastMemoryAccess=true;
	} else if (args[i].equals("-nofma")) {
	    doFastMemoryAccess=false;
	} else if (args[i].equals("-ftp")) {
	    doFastThisPtr=true;
	} else if (args[i].equals("-cf")) {
	    doCF=true;
	} else if (args[i].equals("-directSend")) {
	    doDirectSend=true;
	} else if (args[i].equals("-revocationCLI")) {
	    revocationCheckUsingCLI=true;
	} else if (args[i].equals("-revocationSpinLock")) {
	    revocationCheckUsingSpinLock=true;
	} else if (args[i].equals("-noDivChk")) {
	    doZeroDivChecks=false;
	} else if (args[i].equals("-noNullChk")){ 
	    doNullChecks=false;
	} else if (args[i].equals("-noBoundChk")) {
	    doBoundsChecks=false;
	} else if (args[i].equals("-noMemRangeChk")) {
	    doMemoryRangeChecks=false;
	} else if (args[i].equals("-noStackSizeChk")) {
	    doStackSizeCheck=false;
	} else if (args[i].equals("-noStackClear")) {
	    doClearStack=false;
	} else if (args[i].equals("-clearStack")) {
	    doClearStack=true;
	} else if (args[i].equals("-noInline")) {		    
	    doInlining=false;
	} else if (args[i].equals("-inline")) {
	    doInlining=true;
	} else if (args[i].startsWith("-inline:")) {
	    inlineMethods = new Vector();
	    doInlining=true;
	    StringTokenizer tk = new StringTokenizer(args[i], ":");
	    tk.nextToken();
	    while (tk.hasMoreTokens()) inlineMethods.addElement(tk.nextToken());
	} else if (args[i].startsWith("-forceInline:")) {
	    forceInline = new Vector();
	    StringTokenizer tk = new StringTokenizer(args[i], ":");
	    tk.nextToken();
	    while (tk.hasMoreTokens()) forceInline.addElement(tk.nextToken());
	} else if (args[i].startsWith("-profile:")) {
	    profileMethods = new Vector();
	    doProfiling = true;
	    StringTokenizer tk = new StringTokenizer(args[i], ":");
	    tk.nextToken();
	    while (tk.hasMoreTokens()) {				    
		profileMethods.addElement(tk.nextToken());
	    }
	} else if (args[i].equals("-debug")) {
	    debug = true;
	} else if (args[i].equals("-noDbg")) {
	    doRemoveDebug = true;
	} else if (args[i].equals("-nodebug")) {
	    debug = false;
	} else if (args[i].equals("-v")) {
	    doVerbose = true;
	} else if (args[i].startsWith("-v:")) {
	    verboseList=new Vector();
	    StringTokenizer tk = new StringTokenizer(args[i], ":");
	    tk.nextToken();
	    while (tk.hasMoreTokens()) verboseList.addElement(tk.nextToken());
	} else if (args[i].startsWith("-x:")) {
	    if (optionList==null) optionList=new Vector();
	    StringTokenizer tk = new StringTokenizer(args[i], ":");
	    tk.nextToken();
	    while (tk.hasMoreTokens()) optionList.addElement(tk.nextToken());
	} else if (args[i].startsWith("-debug:")) {
	    debug = true;
	    debugFlags=new Vector();
	    StringTokenizer tk = new StringTokenizer(args[i], ":");
	    tk.nextToken();
	    while (tk.hasMoreTokens()) {				    
		debugFlags.addElement(tk.nextToken());
	    }
	} else if (args[i].equals("-javadoc")) {
	    doJavaDoc = true;
	} else if (args[i].startsWith("-replace:")) {
	    replaceInterfaceWithClass = new Vector();
	    StringTokenizer tk = new StringTokenizer(args[i], ":");
	    tk.nextToken();
	    while (tk.hasMoreTokens()) replaceInterfaceWithClass.addElement(tk.nextToken());
	} else if (args[i].startsWith("-monitorClass:")) {
	    StringTokenizer tk = new StringTokenizer(args[i], ":");
	    tk.nextToken();
	    while (tk.hasMoreTokens()) 	    monitorClass = tk.nextToken();
	} else if (args[i].equals("-lib")) {
	    makeLib = true;
	} else if ((i+1)<args.length && 
		   args[i+1].charAt(0)!='-') {
	    if (args[i].equals("-L")) {
		libPath = args[++i];
	    } else if (args[i].equals("-env")) {
		enviroment = args[++i];
	    } else if (args[i].equals("-o")) {
		targetName = args[++i];
	    } else if (args[i].equals("-s")) {
		zipClassFile=args[++i];
		
		if (zipClassFile.lastIndexOf(".zip")<0) {
		    zipClassFile = zipClassFile+".zip";
		}
	    } else if (args[i].equals("-l")) {
		/* libraries */
		if (args[i+1].charAt(0)!='-') {
		    i++;
		    zipLibFiles= new Vector();
		    jlnLibFiles= new Vector();
		    StringTokenizer tk = new StringTokenizer(args[i], ",;:");
		    
		    while (tk.hasMoreTokens()) {
			String classFile = tk.nextToken();
			    int idx;
			    if ((idx=classFile.indexOf(".zip"))>0) {
				classFile = classFile.substring(0,idx);
			    }
			    zipLibFiles.addElement(classFile+".zip");
			    jlnLibFiles.addElement(classFile+".jln");
		    }
		    
		}
	    }
	} else {
	    Debug.out.print("WARNING: Unknown option "+args[i]+" ignored.");
	    return 1;
	}

	return (i-o)+1;
    }

    public void parseArgs(String[] args) {
	
	if (debug) {
	    Debug.out.print("jc ");
	    for (int i=0;i<args.length;i++) {
		Debug.out.print(args[i]+" ");
	    }
	    Debug.out.println("\n");
	}
	
	/* set defaults */
	doZeroDivChecks      = true; 
	doNullChecks         = true;
        doBoundsChecks       = true;  
	doStackSizeCheck     = true;
	doExceptions         = true;
	doMemoryRangeChecks  = true;
	doOptimize           = false;
	doInlining           = false;
	doProfiling          = false;
        doEventLoging        = false;
	doFastMemoryAccess   = false;
	doFastStatics        = false;
	doNewCode            = true;
	doPrintIMCode        = false;
        doStackTrace         = false;	    
	doUsePackedArrays    = false; // old Compiler allways use 32 Bit

	debug        = false;
	makeLib      = false;
	libPath      = null;
	enviroment   = "int";
	targetName   = null;
	zipLibFiles  = null;
	jlnLibFiles  = null;
	srcName      = null;
	replaceInterfaceWithClass = null; // substitute a classname for an interface name when loading
	 
	try {
	    for (int i=0;i<args.length;i++) {
		if ((i+=parseOption(args,i))>0) {
		    continue;
		} else {
		    srcName=args[i];
		    i++;
		}
	    }
	} catch (Exception ex) {
	    Debug.out.println("\n\ncompiler exception: "+ex.getClass().getName());
	    printUsage();
	    System.exit(-1);
	}

	/* set values for src */

	if (srcName==null) {
	    printUsage();
	    System.exit(-1);
	}

	/*
	if (zipClassFile==null) {
	    zipClassFile = srcName+".zip";
	}
	*/

	/* set values for target */

	if (targetName==null) {
	    targetName=srcName;
	}

	int idx;
	if ((idx=targetName.lastIndexOf(".jll"))>0||
	    (idx=targetName.lastIndexOf(".jxd"))>0) {
	    jxdFileName = targetName;
	    jlnFileName = targetName.substring(0,idx)+".jln";
	} else {	   
	    if (makeLib) {
		jxdFileName = targetName+".jll";
	    } else {
		jxdFileName = targetName+".jxd";
	    }
	    jlnFileName = targetName+".jln";
	}
	
	/* set values for libs */

	if (zipLibFiles!=null) {
	    if (libPath!=null) {
		Vector newLib = new Vector(zipLibFiles.size());
		Vector newInf = new Vector(zipLibFiles.size());
		for (int i=0;i<zipLibFiles.size();i++) {
		    String libFile = (String) zipLibFiles.elementAt(i);
		    String infFile = (String) jlnLibFiles.elementAt(i);
		    if ((libFile.charAt(0)!='/')||(libFile.charAt(0)!='\\')) {
			newLib.addElement(libPath+"/"+libFile);
			newInf.addElement(libPath+"/"+infFile);
		    } else {
			newLib.addElement(libFile);
			newInf.addElement(infFile);
		    }
		}
		zipLibFiles = newLib;
		jlnLibFiles = newInf;
	    } else {
		zipLibFiles.trimToSize();
		jlnLibFiles.trimToSize();
	    }
	}
    }



    protected void printUsage() {
	System.out.print("usage: jc ");
	System.out.print("[-lib] [-O] [-env <int|float>] [-profile[:<methodname>]]");
	System.out.print("[-o <targetName>] ");
	System.out.print("[-L <libPath>] ");
	System.out.print("[-l <libs>] ");
	System.out.println("<src>");
	System.out.println("  <targetName> : The filename of the compiled code.");
	System.out.println("  <domainin> : The zipfilename of the domain classes.");
	System.out.println("  <libs>       : The zipfilenames of the lib classes in the format lib1.zip:lib2.zip:lib3.zip.");
    }

    protected Vector parsePath(String path) {
	Vector paths = new Vector();
	StringTokenizer tk = new StringTokenizer(path, ",:");
	while (tk.hasMoreTokens()) {
	    paths.addElement(tk.nextToken());
	}
	return paths;
    }

}
