/**
 * parser and container class for compiler options
 *
 * Autor: Chr. Wawersich
 */

package jx.compiler;

import jx.zero.Debug;

import java.util.Vector;
import java.util.StringTokenizer;

import jx.compiler.execenv.CompilerOptionsInterface;
import jx.compiler.execenv.BCClass;
import jx.compiler.execenv.BCMethod;

public class CompilerOptions implements CompilerOptionsInterface {

    protected String  codeType;
 
    protected boolean debug;
    protected Vector  debugFlags;
    protected boolean makeLib;
    protected String  enviroment;

    private boolean useLibsDependence = false;

    protected boolean doNewCode            = false;
    protected Vector  newCompiler;

    protected boolean doInlining           = false;
    protected Vector  inlineMethods;
    protected Vector  forceInline = null;

    protected boolean doOptimize           = false;
    protected boolean doAlignCode          = false;
    protected boolean doOptExecPath        = false;
    protected boolean doOptSizeChecks      = false;
    protected boolean doFastStatics        = false;
    protected boolean doFastCheckCast      = false;
    protected boolean doUsePackedArrays    = false;
    protected boolean doExperimentalCode   = false;
    protected boolean doFastThisPtr        = false;
    protected boolean doCF                 = false;

    protected boolean doZeroDivChecks      = true; 
    protected boolean doNullChecks         = true;
    protected boolean doStackSizeCheck     = true;
    protected boolean doBoundsChecks       = true;
    protected boolean doMemoryRangeChecks  = true;
    protected boolean doExceptions         = false;
    protected boolean doClearStack         = true;

    protected boolean doProfiling          = true;
    protected boolean doEventLoging        = false;
    protected Vector logMethods            = null;
    protected boolean doProfileNoIRQ       = false;
    protected Vector  profileMethods;
    protected boolean doFastMemoryAccess   = false;
    protected boolean doPrintIMCode;
    protected boolean doVerbose            = false;
    protected Vector  verboseList          = null;
    protected Vector  optionList           = null;
    protected boolean doRemoveDebug        = false;
    protected boolean doStackTrace;
    protected boolean doParanoid           = false;
    protected boolean doMagicTests         = false;
    protected boolean doTrace              = false;
    /* must be true (see portal.c:sendportal) */
    protected boolean doDirectSend         = true;

    protected boolean doJavaDoc            = false;

    protected String targetName   = null;
    protected String srcName      = null;

    protected String jxdFileName  = null;
    protected String jlnFileName  = null;
    protected String zipClassFile = null;
    protected String libPath      = null;
    protected Vector zipLibFiles  = null;
    protected Vector jlnLibFiles  = null;

    protected Vector replaceInterfaceWithClass = null;

    protected String monitorClass = null;

    protected boolean revocationCheckUsingCLI         = false;
    protected boolean revocationCheckUsingSpinLock    = false;

    /**
     * default constructor
     */
    public CompilerOptions() {
	codeType   = "x86";
	enviroment = "int";
    }

    public CompilerOptions(String env) {
	this();
	enviroment = env;
    }

    public CompilerOptions(String[] libs,String env) {
	this();
	setNeededLibs(libs);
	enviroment = env;
    }

    public CompilerOptions(Vector libs,String env) {
	this();
	setNeededLibs(libs);
	enviroment  = env;
    }

    public String getTargetName() {
	return targetName;
    }

    /**
     * The filename of the compiled code.
     */
    public String getOutputFile() {
	return jxdFileName;
    }

    /**
     * The filename of the linker information.
     */
    public String getLinkerOutputFile() {
	return jlnFileName;
    }

    /**
     * The zipfilename of the domain classes.
     */
    public String getClassFile() {
	return zipClassFile;
    }

    /**
     *  The zipfilenames of the lib classes or null.
     */
   public Vector getLibs() {
	return zipLibFiles;
    }

    public void setUseLibsDependence(boolean flag) {
	useLibsDependence = flag;
    }

    public void setNeededLibs(String[] libs) {
	if (libs!=null) {
	    zipLibFiles = new Vector();
	    for (int i=0;i<libs.length;i++) {
		if (libs[i].lastIndexOf('.')>0) {
		    zipLibFiles.addElement(libs[i]);
		} else {
		    zipLibFiles.addElement(libs[i]+".zip");
		}
	    }
	}
    }

    public void setNeededLibs(Vector libs) {
	zipLibFiles = libs;
    }

    public boolean isUseLibDependence() {
	return useLibsDependence;
    }

    /**
     * 
     */

    public String[] getNeededLibs() {
      
	if (zipLibFiles==null) return null;

	String[] neededLibs = new String[zipLibFiles.size()];
	for (int i=0;i<zipLibFiles.size();i++) {
	    String lib = (String)zipLibFiles.elementAt(i);
	    int b = lib.lastIndexOf('/');
	    int e = lib.lastIndexOf('.');
	    neededLibs[i] = lib.substring(b+1,e)+".jll";
	}

	return neededLibs;
      }

    /**
     * The linker information files for the libraries or null.
     */
    public Vector getLibsLinkerInfo() {
	return jlnLibFiles;
    }

    public boolean doExtraFalting() {
	return doCF;
    }

    public boolean doFastMemoryAccess() {
	return doFastMemoryAccess;
    }

    public boolean doMemoryRangeChecks() {
	return doMemoryRangeChecks;
    }

    public boolean doExceptions() {
	return doExceptions;
    }

    public boolean doClearStack() {
	return doClearStack;
    }


    public boolean doZeroDivChecks() {
	return doZeroDivChecks;
    }

    public boolean doNullChecks() {
	return doNullChecks;
    }

    public boolean doBoundsChecks() {
	return doBoundsChecks;
    }

    public boolean doStackSizeCheck(boolean isLeafMethod) {
	if (isLeafMethod && doOptSizeChecks) {
	    return false;
	} else {
	    return doStackSizeCheck;
	}
    }

    public boolean doVerbose() {
	return doVerbose;
    }

    public boolean doVerbose(String kind) {
	if (doVerbose) return true;
	if (verboseList!=null) {
	    for (int i=0;i<verboseList.size();i++) {
		if (verboseList.elementAt(i).equals(kind)) return true;
	    }
	}
	return false;
    }

    public boolean isOption(String kind) {
	if (optionList!=null) {
	    for (int i=0;i<optionList.size();i++) {
		if (optionList.elementAt(i).equals(kind)) return true;
	    }
	}
	return false;
    }

    public boolean doPrintStackTrace() {
	return doStackTrace;
    }

    public boolean doParanoidChecks() {
	return doParanoid;
    }

    public boolean doMagicChecks() {
	return doMagicTests;
    }

    public boolean doTrace() {
	return doTrace;
    }

    public void printVerbose(String txt) {
	if (doVerbose) Debug.out.println(txt);
    }

    public boolean doAlignCode() {
	return doAlignCode;
    }

    public boolean doOptimize() {
	return doOptimize;
    }

    public boolean doFastThisPtr() {
	return doFastThisPtr;
    }

    public boolean doOptExecPath() {
	return doOptExecPath;
    }

    public boolean doFastStatics() {
	return doFastStatics;
    }

    public boolean doFastCheckCast() {
	return doFastCheckCast;
    }

    public boolean doDirectSend() {
	return doDirectSend;
    }

    public boolean doJavaDoc() {
	return doJavaDoc;
    }


    public boolean doUsePackedArrays() {
	return doUsePackedArrays;
    }

    public boolean forceInline(BCMethod bcMethod)
    {
	if (forceInline!=null) {
	    String currentMethod = null;
	    for (int i=0;i<forceInline.size();i++) {
		String entry = (String)forceInline.elementAt(i);

		if (entry.startsWith("*.")) currentMethod = "*."+bcMethod.getName();
		else {currentMethod = bcMethod.getClassName()+"."+bcMethod.getName();}

		if (entry.substring(entry.length()-1).equals("*")) {
		    entry = entry.substring(0,entry.length()-1);
		    if (currentMethod.startsWith(entry)) return true;
		}

		if (currentMethod.equals(entry)) return true;
	    }
	}
	return false;
    }

    public boolean doInlining(BCClass bcClass,BCMethod bcMethod) {
	if (!doInlining) return false;
        String currentMethod = bcClass.getClassName()+"."+bcMethod.getName();
	if (inlineMethods!=null) {
	    for (int i=0;i<inlineMethods.size();i++) {
		String entry = (String)inlineMethods.elementAt(i);
		//if (currentMethod.equals(entry)) return true;
		if (entry.substring(entry.length()-1).equals("*")) {
		    entry = entry.substring(0,entry.length()-1);
		    if (currentMethod.startsWith(entry)) return true;
		} 
		if (currentMethod.equals(entry)) return true;
	    }
	} else {
	    return true;
	}
	return false;
    }

    public boolean doInlining(String methodName) {
	if (doInlining) {
	    if (inlineMethods!=null) {
		for (int i=0;i<inlineMethods.size();i++) {
		    if (methodName.equals((String)(inlineMethods.elementAt(i)))) return true;
		}
		return false;
	    }
	    return true;
	}	
	return false;
    }

    public boolean doProfile(BCClass bcClass, BCMethod bcMethod) {
	if (!doProfiling) return false;
        
	if (bcClass.getClassName().equals("jx/zero/timing/Control")) {
	    if (bcMethod.getName().startsWith("timing")) return true;
	    else return false;
	}

	String currentMethod = bcClass.getClassName()+"."+bcMethod.getName();

	if (profileMethods!=null) {
	    for (int i=0;i<profileMethods.size();i++) {
		String entry = (String)profileMethods.elementAt(i);
		if (entry.substring(entry.length()-1).equals("*")) {
		    entry = entry.substring(0,entry.length()-1);
		    if (currentMethod.startsWith(entry)) return true;
		} 
		if (currentMethod.equals(entry)) return true;
	    }
	} else {
	    return true;
	}

	return false;
    }

    public boolean doEventLoging() {
	return doEventLoging;
    }

    public void setUseNewCompiler(boolean flag) {
	doNewCode = flag;
    }

    public boolean doNewCompiler() {
	return doNewCode;
    }

    public boolean doLogMethod(BCClass bcClass,BCMethod bcMethod) {	
      if (logMethods!=null) {
	String currentMethod = bcClass.getClassName()+"."+bcMethod.getName();
	for (int i=0;i<logMethods.size();i++) {
	  String entry = (String)logMethods.elementAt(i);
	  if (entry.substring(entry.length()-1).equals("*")) {
	    entry = entry.substring(0,entry.length()-1);
	  if (currentMethod.startsWith(entry)) return true;
	  } 
	  if (currentMethod.equals(entry)) return true;
	}
      }
      return false;		
    }
  
    public boolean doNewCompiler(BCClass bcClass,BCMethod bcMethod) {	
	if (!doNewCode) return false;
        String currentMethod = bcClass.getClassName()+"."+bcMethod.getName();
	if (newCompiler!=null) {
	    for (int i=0;i<newCompiler.size();i++) {
		String entry = (String)newCompiler.elementAt(i);
		if (entry.substring(entry.length()-1).equals("*")) {
		    entry = entry.substring(0,entry.length()-1);
		    if (currentMethod.startsWith(entry)) return true;
		} 
		if (currentMethod.equals(entry)) return true;
	    }
	} else {
	    return true;
	}
	return false;		
    }

    public boolean doPrintIMCode() {
	return doPrintIMCode;
    }

    public boolean doProfileNoIRQ() {
	return doProfileNoIRQ;
    }

    public void debug(String txt) {
	if (debug) Debug.out.println(txt);
    }

    public boolean doDebug() {
	return debug;
    }

    public boolean doRemoveDebug() {
	return doRemoveDebug;
    }

    public void setDebug(boolean flag) {
	debug = flag;
	doVerbose = flag;
    }

    public boolean doDebug(String flag) {
	if (debug) {
	    if (debugFlags!=null) {
		for (int i=0;i<debugFlags.size();i++) {
		    if (flag.equals((String)debugFlags.elementAt(i))) return true;
		}
		return false;
	    }
	    return true;
	}
	return false;
    }

    public boolean rejectFloat() {
	if (enviroment.equals("float")) {
	    return false;
	} else {
	    return true;
	}
    }

    /**
     * returns the processor type
     */
    public String codeType() {
	if (doProfiling) {
	    return "x86profile";
	} else {
	    return codeType;
	}
    }

    public boolean revocationCheckUsingCLI() {
	return revocationCheckUsingCLI;
    }

    public boolean revocationCheckUsingSpinLock() {
	return revocationCheckUsingSpinLock;
    }


    public String monitorClass() {
	return monitorClass;
    }

}
