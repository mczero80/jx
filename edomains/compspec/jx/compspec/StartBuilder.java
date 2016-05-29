/*
 * JX Builder
 * Author: Michael Golm
 */
package jx.compspec;

import java.io.*;
import java.util.Vector;

import jx.compiler.CompilerOptions;
import jx.compiler.CompilerOptionsNative;
import jx.compiler.CompileNative;

class BuilderOptions extends CompilerOptionsNative {
	
    public BuilderOptions(Vector libs, Vector jlns, String src, String jlnFile, String target, String env) {
	
	/* set defaults */
	doZeroDivChecks      = true; 
	doNullChecks         = true;
        doBoundsChecks       = true;  
	doStackSizeCheck     = true;
	doExceptions         = true;
	doMemoryRangeChecks  = true;

	/* -O ============== */
	//doOptimize           = true;
	//doFastStatics        = true;
	//doOptSizeChecks      = true;
	//doAlignCode          = true;
	/* -O ============= */

	//doInlining           = true;
	//doCF                 = true;

	doProfiling          = false;
        doEventLoging        = false;
	doFastMemoryAccess   = false;
	doFastStatics        = false;
	doPrintIMCode        = false;
        doStackTrace         = false;	    
	doUsePackedArrays    = false; // old Compiler allways use 32 Bit

	replaceInterfaceWithClass = null; // substitute a classname for an interface name when loading

	debug        = false;
	makeLib      = true;
	libPath      = null;

	enviroment   = env;
	zipClassFile = src;
	zipLibFiles  = libs;
	jlnLibFiles  = jlns;
	jxdFileName  = target;
	jlnFileName  = jlnFile;
    }

    //public boolean doDebug() {return true;}
}

public class StartBuilder {
    private static final boolean doDebug = true;
    private static final boolean silent = true;

    private static Vector metas = new Vector(); // MetaInfo

    static String compdir;
    static String buildDir;

    private static String components;
    private static String javacpath;

    private static boolean opt_f = false; // force recompilation 
    private static boolean opt_c = false; // make clean
    private static boolean opt_n = false; // do nothing
    private static boolean opt_fast = false; // compile only the jll that has changed
    private static boolean opt_new = false; // compute transitive if-depends, impl-depends, and extends relations
    private static boolean opt_debug = false; // verbose debug output

    private static boolean new_jcflags = false;

    private static File zipFileListFile;
    private static CompilerOptions compilerOpts;

    public static void main(String[] args) throws Exception {
	String componentsDir=null;
	int argc = 0;
	//jx.emulation.Init.init();	

	System.setSecurityManager(new NoExit());

	if (args.length < 2) {
	    throw new Error("StartBuilder: wrong number of parameters");
	}

	while(argc < args.length) {
	    if (args[argc].equals("-fast")) {
		opt_fast = true;
	    } else if (args[argc].equals("-new")) {
		opt_new = true;
	    } else if (args[argc].equals("-debug")) {
		opt_debug = true;
	    } else if (args[argc].equals("-javacpath")) {
		argc++;
		javacpath = args[argc];
	    } else if (args[argc].equals("-components")) {
		argc++;
		components = args[argc];
	    } else if (args[argc].equals("-compdir")) {
		argc++;
		componentsDir = args[argc];
	    } else {
		throw new Error("Unknown option "+args[argc]);
	    }
 	    argc++;
	}
	if (components == null || componentsDir == null) throw new Error("need COMPONENTS and components dir");

	String[] compdirs = MetaInfo.split(componentsDir, ':');

	if (opt_new) {
	    metas = findlibsNew(compdirs, components);
	} else {
	    metas = findlibs(compdirs, components);
	}
	buildDir = compdirs[0];
	String jcFileName = compdirs[0]+"/../JC_CONFIG";
	String addFileName = compdirs[0]+"/../ADD_TO_ZIP";
	String codeFileName = compdirs[0]+"/../code.zip";
	File jcFile = new File(jcFileName);
	zipFileListFile = new File(addFileName);
	File codeFile = new File(codeFileName);
	if (jcFile.exists() && codeFile.exists()) {
	    if (jcFile.lastModified() > codeFile.lastModified()) new_jcflags=true;
	}
	compilerOpts = getCompilerOptions(null,null,null,null,null,jcFileName);

	String classPath = "";
	for(int i=0; i<metas.size(); i++) {
	    MetaInfo s = (MetaInfo)metas.elementAt(i); // process this component
	    if (!silent) System.out.println("COMPONENTS += " + s.getComponentName());	
	    String filename = componentsDir+"/"+s.getComponentName();
	    classPath += filename + ":";
	}
	
	System.out.println("*********** BUILDING COMPONENTS: JAVA -> CLASS");
	String jxclasspath = "";
	for(int i=0; i<metas.size(); i++) {
	    MetaInfo s = (MetaInfo)metas.elementAt(i); // process this component

	    if (!silent) System.out.print("* "+s.getComponentName());
	    boolean firstout = true;

	    String filename = s.getFilename()/*+"/"+s.getComponentName()+"/"*/;
	    //System.out.println("FILENAME=" + filename);

	    jxclasspath += filename + ":";

	    // System.out.println("CLASSPATH=" + jxclasspath);

	    // first activate generators, such as rpcgen
	    String rpcif = s.getVar("RPC_INTERFACES");
	    if (rpcif != null) {
		String[] comps = MetaInfo.split(s.getVar("RPC_COMPONENTS"));
		String[] rargs = new String[comps.length+2];
		rargs[0] = filename + s.getVar("RPC_OUTPUTDIR");

		String[] ifs = MetaInfo.split(rpcif);

		// find youngest component
		long youngest=0;
		for(int r=0; r<comps.length; r++) {
		    File cf = new File(componentsDir+"/"+comps[r]+".zip");
		    if (cf.exists() && cf.lastModified() > youngest)
			youngest = cf.lastModified();
		}
		/* seems not to work as expected
		boolean worktodo = false;
		for(int r=0; r<ifs.length; r++) {
		    String stubname = rargs[0] + "/" + packageToClassName(ifs[r])+"_Stub.java";
		    File stubfile = new File(stubname);
		    if (! stubfile.exists() || youngest > stubfile.lastModified()) {		    
			worktodo = true;
			break;
		    }
		}
		*/
		boolean worktodo = true;

		if (worktodo) {
		    // build zip files for needed components
		    for(int r=0; r<comps.length; r++) {
			MetaInfo compmeta=null;
			for(int l=0; l<metas.size(); l++) {
			    MetaInfo sc = (MetaInfo)metas.elementAt(l); // process this component
			    if (comps[r].equals(sc.getComponentName())) {
				compmeta = sc;
				break;
			    }
			}
			build_zip(compmeta); // rpcgen reads zip
			rargs[r+1] = componentsDir+"/"+comps[r]+".zip";
		    }
		    rargs[rargs.length-1] = rpcif;
		    jx.rpcgen.StartRPCGen.main(rargs);
		}
	    }

	    // now activate javac
	    String sd = s.getVar("SUBDIRS");
	    String[] sda = MetaInfo.split(sd);

	    Vector todo = new Vector();
	    // todo.addElement("-verbose");
	    todo.addElement("-O");
	    todo.addElement("-d");
	    todo.addElement(filename);
	    //System.out.println("Generating into dir "+filename);
	    todo.addElement("-classpath");
	    todo.addElement(jxclasspath);
	    /* component zero needs minimal jdk but jdk0 needs zero
	     * so we use the sun jdk
	     * jdk0 and jdk1 also depend on unknown types of the jdk */
	    /*	    if (javacpath != null 
		&& s.getComponentName().equals("zero") 
		) {*/
		
	    if (javacpath != null 
		&& !s.getComponentName().equals("zero") 
		&& !s.getComponentName().equals("jdk0") 
		&& !s.getComponentName().equals("jdk1")
		) {
		
		todo.addElement("-bootclasspath");
		todo.addElement(javacpath);
	    }

	    todo.addElement("-sourcepath");
	    todo.addElement(filename);

	    boolean worktodo = false;
	    for(int j=0;j<sda.length;j++){
		String dirname=null;
		dirname = sda[j];
		String subdirname = filename+"/"+dirname+"/";
		File subdir = new File(subdirname);
		if (! subdir.exists()) throw new Error("Subdir "+subdirname+" does not exist.");

		Vector output = new Vector();
		String dirlist[] = subdir.list();
		for(int k=0; k<dirlist.length; k++) {
		    if (hasExtension(new String[] {".java"}, dirlist[k])) {
			String javafilename = filename+"/"+dirname+"/"+dirlist[k];
			File javafile = new File(javafilename);
			String classfilename = filename+"/"+dirname+"/"+j2c(dirlist[k]);
			File classfile = new File(classfilename);
			if (opt_f || javafile.lastModified() > classfile.lastModified()) {
			    todo.addElement(subdirname+"/"+dirlist[k]);
			    output.addElement(dirlist[k]);
			    worktodo = true;
			}
		    }
		}
		
		if (output.size() > 0) {
		    if (firstout) {System.out.println(""); firstout=false;}
		    System.out.print(dirname+": ");
		    for(int o=0; o<output.size(); o++) {
			System.out.print(((String)output.elementAt(o))+" ");
		    }
		    System.out.println("");
		}
	    }

	    if (worktodo) { 
		String jargs[] = new String[todo.size()];
		todo.copyInto(jargs);
		String mainClass = "sun.tools.javac.Main";
		int ret = exec("", classPath, mainClass, jargs);
		if (ret != 0) throw new Error("Compiler returned error");
	    } else {
		if (!silent) System.out.println(" ... nothing to do.");
	    }

	    if (compilerOpts.doJavaDoc()) { 
		Vector output = new Vector();
		for(int j=0;j<sda.length;j++){
		    String dirname=null;
		    dirname = sda[j];
		    String subdirname = filename+"/"+dirname+"/";
		    File subdir = new File(subdirname);
		    if (! subdir.exists()) throw new Error("Subdir "+subdirname+" does not exist.");
		    
		    String dirlist[] = subdir.list();
		    for(int k=0; k<dirlist.length; k++) {
			if (hasExtension(new String[] {".java"}, dirlist[k])) {
			    String javafilename = filename+"/"+dirname+"/"+dirlist[k];
			    output.addElement(javafilename);
			}
		    }
		} 
		String files[] = new String[output.size()];
		output.copyInto(files);
		int ret = execJavaDoc(s.getComponentName(), componentsDir, files, classPath);
		if (ret != 0) throw new Error("Javadoc returned error");
	    }
	    

	}

	System.out.println("*********** BUILDING ZIPS: CLASS -> ZIP");
	for(int i=0; i<metas.size(); i++) {
	    MetaInfo s = (MetaInfo)metas.elementAt(i); // process this component
	    if (!silent) System.out.print("* "+s.getComponentName()+"...  ");
	    if (build_zip(s)) {
		if (!silent) System.out.println("Creating.");
		else System.out.println("* "+s.getComponentName()+" Creating.");
	    } else {
		if (!silent) System.out.println(" nothing to do.");
	    }
	}

	System.out.println("*********** BUILDING COMPONENTS: ZIP -> JLL");
	build_jlls();

	System.out.println("*********** BUILDING BOOT ZIP FILE");
	build_bootzip();

	System.out.println("*********** BUILD COMPLETED");
    }

	
    static boolean build_zip(MetaInfo s) throws Exception {
	String componentName = s.getComponentName();
	String sd = s.getVar("SUBDIRS");
	String[] sda = MetaInfo.split(sd);
	
	String filename = s.getFilename();
	//System.out.println("FILE: "+filename);
	String zipname = buildDir+"/"+componentName+".zip";
	File zipfile = new File(zipname);
	long ziptime=-1;
	if (zipfile.exists()) ziptime = zipfile.lastModified();
	Vector todo = new Vector();
	
	boolean worktodo = false;
	
	String metafilename = "META";
	File metafile = new File( filename +"/"+metafilename);
	if (metafile.exists()) {
	    todo.addElement(metafilename);
	    if (ziptime == -1 || metafile.lastModified()>ziptime) worktodo = true;
	}
	
	for(int j=0;j<sda.length;j++){
	    String dirname=null;
	    dirname = sda[j];
	    String subdirname = filename+"/"+dirname+"/";
	    File subdir = new File(subdirname);
	    if (! subdir.exists()) throw new Error("Subdir "+subdirname+" does not exist.");
	    
	    String dirlist[] = subdir.list();
	    for(int k=0; k<dirlist.length; k++) {
		if (hasExtension(new String[] {".class"}, dirlist[k])) {
		    String classfilename = filename+"/"+dirname+"/"+dirlist[k];
		    File subfile = new File( classfilename);
		    if (subfile.exists()) {
			todo.addElement(dirname+"/"+dirlist[k]);
			if (ziptime == -1 || subfile.lastModified()>ziptime) worktodo = true;
		    }
		}
	    }		
	}
	
	if (worktodo) {
	    try {
		FileOutputStream out = new FileOutputStream(zipname);
		java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out);
		zip.setMethod(java.util.zip.ZipOutputStream.STORED);
		for(int m=0; m<todo.size(); m++) {
		    String subfile = (String)todo.elementAt(m);
		    addZipEntry(zip, filename, subfile);
		}
		zip.close();
		return true;
	    } catch(IOException ex) {
		new File(zipname).delete();
		throw ex;
	    }
	} else {
	    return false;
	}
    }

    /**
     * @param absdir absolute directory necessary to open the file
     * @param subfile part of the path that should be included in the zip file
     */
    static void addZipEntry(java.util.zip.ZipOutputStream zip, String absdir, String subfile) throws Exception {
	String absfile = absdir+"/"+subfile;

	RandomAccessFile f = new RandomAccessFile(absfile, "r");
	byte [] data = new byte[(int)f.length()];
	f.readFully(data);
	f.close();
	java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(subfile);
	entry.setSize(data.length);
	java.util.zip.CRC32 crc = new java.util.zip.CRC32();
	crc.update(data);
	entry.setCrc(crc.getValue());
	zip.putNextEntry(entry);
	zip.write(data, 0, data.length);
	zip.closeEntry();
    }
    
    static public CompilerOptions getCompilerOptions(Vector libs,
						     Vector jlns,
						     String zipname, String jlnname, String jllname,
						     String optionFile) {
	File config = new File(optionFile);
	BuilderOptions opts = new BuilderOptions(libs,jlns,zipname,jlnname,jllname,"int");

	if (config.exists()) {
	    try {

		BufferedReader file = new BufferedReader(new FileReader(config));
		String line;
		while ((line=file.readLine())!=null) {
		    if (line.startsWith("#")) continue;
		    int ndx = line.indexOf("JCFLAGS");
		    if (ndx>0) {
			ndx=line.indexOf("=",ndx);
			String jcflags = line.substring(ndx+1);
			opts.parseOptionLine(jcflags);
			return opts;
		    }
		}
	    } catch (Exception ex) {
		System.err.println(ex.getClass().getName());
		ex.printStackTrace();
		System.err.println("WARNING: Can`t read "+optionFile+" !!!");
	    }

	    System.err.println("WARNING: JCFLAGS not found in "+optionFile+" !!!");
	    return opts;
	}

	System.err.println("WARNING: "+optionFile+" not found !!!");
	return opts;
    }

    static public Vector findlibsNew(String[] componentsDir, String components) throws Exception {
	String[][] com = new NewParser(components).getComponents();
	return null;
    }

    /** compute all needed components */
    static public Vector findlibs(String[] compdir, String components) throws Exception {
	String[] com = new Parser(components).getComponents();
	//	String[] allcom = new Parser(componentsAll).getComponents();
	MetaReader metaReader;
	metaReader = new MetaReader(compdir);

	/*
	// check whether all components in COMPONENTS are also in ALLCOMPONENTS
	for(int j=0; j<com.length; j++) {
	    int i;
	    for(i=0; i<allcom.length; i++) {
		if (com[j].equals(allcom[i])) {
		    break;
		}
	    }
	    if (i==allcom.length) throw new Error("Component "+com[j]+" not in ALLCOMPONENTS");
	}
	*/

	for(int j=0; j<com.length; j++) {
	    metaReader.addMeta(metas, com[j]);
	}

	// compute transitive needlibs/ifdependson for all components
	transloop:
	for(int i=0; i<metas.size(); i++) {
	    MetaInfo s = (MetaInfo)metas.elementAt(i); // process this component
	    //Debug.out.println("COMPUTE: "+s.getComponentName());
	    String v;
	    if (opt_new) {
		v = s.getVar("IFDEPENDSON");
		if (v==null) {
		    v = s.getVar("NEEDLIBS");
		}
	    } else {
		v = s.getVar("NEEDLIBS");
	    }
	    if (v==null) continue; // example: zero lib
	    String[] c = MetaInfo.split(v);
	    if (c==null) continue; // example: zero lib

	    int doneIndex=0;
	    Vector allLibs = new Vector(); // will contain transitive closure
	    if (opt_debug) System.out.println("PROCESS: "+s.getComponentName());

	    // sort COMPONENTS in lexical order and add metas
	    Sort.quicksort(c);

	    for(int origi=0;origi<c.length;origi++) {
		if (opt_debug) System.out.println("ADDNEEDED: "+c[origi]);
		String lib0 = c[origi];

		// first try to find in components then in allcomponents
		boolean found = false;
		for(int k=0; k<metas.size(); k++) {
		    if (((MetaInfo)metas.elementAt(k)).getComponentName().equals(lib0)) {
			found = true;
			break;
		    }
		}

		if (! found) {
		    metaReader.addMeta(metas, lib0);
		}

		found = false;
		for(int k=0; k<metas.size(); k++) {
		    if (((MetaInfo)metas.elementAt(k)).getComponentName().equals(lib0)) {
			//Debug.out.println("FOUND: "+lib0);
			if (k > i) {
			    // backoff from processing this lib and do the other lib first
			    // swap the two libs
			    MetaInfo needed = (MetaInfo)metas.elementAt(k);
			    metas.setElementAt(needed, i);
			    metas.setElementAt(s, k);
			    //throw new Error(s.getComponentName()+"DEPEND ON UNPROCESSED "+lib0);
			    i--;
			    //Debug.out.println("BACKOFF: "+s.getComponentName());
			    continue transloop;
			}
			
			
			// add needed libs if not there
			String [] nl = ((MetaInfo)metas.elementAt(k)).getNeededLibs();
			if (nl == null) {
			    throw new Error("No NEEDEDLIBS computed for component "+lib0);
			}
			for(int j=0;j<nl.length;j++) {
			    //Debug.out.println("NEED: "+nl[j]);
			    add(allLibs, nl[j]);
			}
			found = true;
			break;
		    }			
		}
		if (! found) {
		    throw new Error("needed lib "+lib0+" not found");
		}
		add(allLibs, lib0);
		
	    }
	    
	    /*
	    Debug.out.print("DONE "+s.getComponentName()+": ");
	    for(int j=0;j <allLibs.size();j++) {
		Debug.out.print(allLibs.elementAt(j));
	    }
	    Debug.out.println("");
	    */
	    s.setNeededLibs(allLibs);
	}
	return metas;
    }
    
    static void add(Vector allLibs, String lib) {
	// is this lib already there?
	for(int k=0; k<allLibs.size(); k++) {
	    if (lib.equals(allLibs.elementAt(k))) return;
	}
	allLibs.addElement(lib);
    }

    static void dumpMetas(String filename) {
	try {
	    PrintStream out = new PrintStream(new FileOutputStream(filename));
	    out.println("# **** LIST OF COMPONENTS INCLUDED IN BOOT IMAGE *********");
	    out.println("# AUTOMATICALLY GENERATED FILE.");
	    out.println("# DO NOT MODIFY!");
	    for(int i=0; i<metas.size(); i++) {
		MetaInfo s = (MetaInfo)metas.elementAt(i); // process this component
		out.println(s.getComponentName());
	    }
	} catch(IOException e) {throw new Error();}
    }

    static public void build_jlls() {
	boolean compiledAJll = false;
	for(int i=0; i<metas.size(); i++) {
	    MetaInfo s = (MetaInfo)metas.elementAt(i); // process this component
	    if (!silent) System.out.print("* "+s.getComponentName()+"... ");

	    String libdir = buildDir;
	    if (!libdir.endsWith("/")) libdir=libdir+"/";

	    String zipname = libdir+s.getComponentName()+".zip";
	    String jllname = libdir+s.getComponentName()+".jll";

	    File zipfile = new File(zipname);
	    File jllfile = new File(jllname);

	    if ((!compiledAJll || opt_fast) && (jllfile.exists() && !new_jcflags) && (jllfile.lastModified() > zipfile.lastModified())) {
		if (!silent) System.out.println(" nothing to do.");
		continue;
	    }
	    if (silent) System.out.print("* "+s.getComponentName()+"... ");
	    compiledAJll = true;

	    Vector libs = new Vector();
	    Vector jlns = new Vector();

	    String[] neededLibs = s.getNeededLibs();
	    for(int j=0; j<neededLibs.length; j++) {
		libs.addElement(libdir+neededLibs[j]+".zip");
		jlns.addElement(libdir+neededLibs[j]+".jln");
	    }

	    String jlnname = libdir+s.getComponentName()+".jln";

	    /* public BuilderOptions(Vector libs, Vector jlns, String src, String target, String env) */
	    //BuilderOptions opts = new BuilderOptions(libs,jlns,zipname,jlnname,jllname,"int");
	    CompilerOptions opts = getCompilerOptions(libs,jlns,zipname,jlnname,jllname,libdir+"../JC_CONFIG");

	    try {
		CompileNative.compile(libdir+s.getComponentName(),opts);
	    } catch (Throwable ex) {
		ex.printStackTrace();
		new File(jllname).delete();
		throw new Error("Translator returned error");
	    }
	}
    }

    static public void build_bootzip() throws Exception {
	String bootzipname = buildDir+"/"+"../code.zip";
	File bootzipfile = new File(bootzipname);
	long bootzipmod = bootzipfile.lastModified();
	FileOutputStream out = new FileOutputStream(bootzipname);
	java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out);
	zip.setMethod(java.util.zip.ZipOutputStream.STORED);

	for(int i=0; i<metas.size(); i++) {
	    MetaInfo s = (MetaInfo)metas.elementAt(i); // process this component
	    if (!silent) System.out.print("* "+s.getComponentName()+"... ");
	    
	    String zipname = buildDir+"/"+s.getComponentName()+".jll";

	    File zipfile = new File(zipname);
	    if (zipfile.lastModified() > bootzipmod) {
		if (!silent) System.out.println(" update.");
		else System.out.println("* "+s.getComponentName()+" update.");
	    } else {
		if (!silent) System.out.println(" nothing to do.");
	    }
	    addZipEntry(zip, buildDir, s.getComponentName()+".jll");
	}

	// additional files
	if (zipFileListFile.exists())  {
	    DataInputStream listin = new DataInputStream(new FileInputStream(zipFileListFile));
	    for(;;) {
		String name = listin.readLine();
		if (name == null) break;
		name = name.trim();
		if (name.length()==0) continue;
		int s = name.lastIndexOf('/');		
		String filename = name;
		String pathname = "";
		if (s != -1) {
		    filename = name.substring(s+1);
		    pathname = name.substring(0, s);
		}
		//System.out.println("NAME: "+filename);
		//System.out.println("PATH: "+pathname);
		addZipEntry(zip, buildDir+"/../"+pathname, filename);
	    }
	    
	    //		addZipEntry(zip, componentsDir+"/../", "boot.rc");
	    //addZipEntry(zip, componentsDir+"/wm_impl/jx/wm/", "std.keymap");
	    //addZipEntry(zip, componentsDir+"/wm_impl/jx/wm/", "default.fon");
	    //File rmfile = new File(componentsDir+"../jcore/realmode");
	    //if (rmfile.exists()) {
	    //	addZipEntry(zip, componentsDir+"../jcore/", "realmode");
	    //} else {
	    //System.out.println("WARNING: no realmode file found");
	    //}
	    
	    // ONLINECOMPILER REQUIRES ALL JLN AND ZIP FILES!!
	    // ...
	}

	zip.close();
    }


    public static int exec(String cwd, String classPath, String mainClass, String args[]) {
        int ret = 0;
        String cp = System.getProperty("env.class.path");
	// System.out.println("CP: " + cp);
	try {
	    try {
		if (false) {		
		    System.out.println("************");
		    System.out.println("CWD: " + cwd);
		    System.out.println("CP: " + classPath);
		    System.out.println("Class: " + mainClass);
		    System.out.print("args: ");
		    for (int j = 0; j< args.length; j++) {
			System.out.print(args[j] + " ");
		    }
		    System.out.println("");
		}

		//setProperty("user.dir", cwd);
		
                //setProperty("env.class.path", classPath + ":" + cp);
		java.lang.Class c = java.lang.Class.forName(mainClass);
		Class parameterTypes[] = new Class[1];
		parameterTypes[0] = args.getClass();
		java.lang.reflect.Method method = c.getMethod("main", parameterTypes);
		Object pars[] = new Object[1];
		pars[0] = args;
                //System.out.println("CP2: " + classPath );
                setProperty("env.class.path", classPath + ":" + cp);
		try {
		    method.invoke(null, pars);
		} catch (java.lang.reflect.InvocationTargetException e) {
		    throw e.getTargetException();
		}
		
	    } catch (NoExitException e) {
		ret = e.status;
	    } 
	} catch (Throwable t) {
	    System.out.println("Exception: " + t.getMessage());
	    t.printStackTrace();
	    ret = 1;
	}
        if (cp == null) setProperty("env.class.path", "");
        else setProperty("env.class.path", cp);
	return ret;
    }

    public static int execJavaDoc(String libname, String componentsDir, String[] files, String classPath) {
        int ret = 0;
	try {
	    String cmd = "javadoc -author -version -d "+componentsDir+"../docs/"+libname+" -classpath "+classPath+" ";
	    for(int i=0; i<files.length; i++) {
		cmd += " "+files[i];
	    }
	    System.out.println(cmd);

	    String cmd0 = "rm -rf "+componentsDir+"../docs/"+libname;
	    String cmd1 = "mkdir "+componentsDir+"../docs/"+libname;
	    
	    exec0(cmd0);
	    exec0(cmd1);
	    exec0(cmd);
	} catch (Throwable t) {
	    System.out.println("Exception: " + t.getMessage());
	    t.printStackTrace();
	    ret = 1;
	}
	return ret;
    }

    static void exec0(String cmd) throws Exception {
	Process p = Runtime.getRuntime().exec(cmd);
	BufferedInputStream in = new BufferedInputStream(p.getInputStream());
	int c;
	while((c=in.read()) != -1) { System.out.write(c); }
    }


    static boolean hasExtension(String[] exts, String name) {
	for(int i=0; i<exts.length; i++) {
	    if (name.endsWith(exts[i])) return true;
	}
	return false;
    }

    static String j2c(String javafile) {
	return javafile.substring(0, javafile.length()-4)+"class";
    }

    static String packageToDir(String pack) {
	char[] c = new char[pack.length()];
	pack.getChars(0, c.length, c, 0);
	for(int i=0; i<c.length; i++) {
	    if (c[i] == '.') c[i] = '/';
	}
	return new String(c);
    }
    static String packageToClassName(String pack) {
	int i = pack.lastIndexOf('.');
	return pack.substring(i+1);
    }

    static void setProperty(String k, String v) {
	 java.util.Properties properties = System.getProperties();
	 properties.remove(k);
	 properties.put(k, v);
	 System.setProperties(properties);
    }

    static class NoExitException extends SecurityException {
	int status;
	public NoExitException(int status) {
	    super("NoExit");
	    this.status = status;
	}
    }
    
    static class NoExit extends java.lang.SecurityManager {
	public NoExit() {
	    
	}
	public void checkCreateClassLoader() { } 
	public void checkAccess(Thread g) { }
	public void checkAccess(ThreadGroup g) { }
	public void checkExec(String cmd) { }
	public void checkLink(String lib) { }
	public void checkRead(FileDescriptor fd) { }
	public void checkRead(String file) { }
	public void checkRead(String file, Object context) { }
	public void checkWrite(FileDescriptor fd) { }
	public void checkWrite(String file) { }
	public void checkDelete(String file) { }
	public void checkConnect(String host, int port) { }
	public void checkConnect(String host, int port, Object context) { }
	public void checkListen(int port) { }
	public void checkAccept(String host, int port) { }
	public void checkMulticast(java.net.InetAddress maddr) { }
	public void checkMulticast(java.net.InetAddress maddr, byte ttl) { }
	public void checkPropertiesAccess() { }
	public void checkPropertyAccess(String key) { }
	public void checkPropertyAccess(String key, String def) { }
	public boolean checkTopLevelWindow(Object window) { return true; }
	public void checkPrintJobAccess() { }
	public void checkSystemClipboardAccess() { }
	public void checkAwtEventQueueAccess() { }
	public void checkPackageAccess(String pkg) { }
	public void checkPackageDefinition(String pkg) { }
	public void checkSetFactory() { }
	public void checkMemberAccess(Class clazz, int which) { }
	public void checkSecurityAccess(String provider) { }
	
	public void checkExit(int status) {
	    throw new NoExitException(status);
	}
	public void checkPermission(java.security.Permission perm) {
	}
    }
}



class Sort {
    public static void swap(Object data[], int x, int y) {
	Object t = data[x];
	data[x] = data[y];
	data[y] = t;
    }

    static boolean less(String a, String b) {
	return a.compareTo(b)>0;
    }

    public static void quicksort(Vector a) {
	String[] v = new String[a.size()];
	a.copyInto(v);
	quicksort(v, 0, v.length-1);
	a.removeAllElements();
	for(int i=0; i<v.length; i++) {
	    //System.out.println("S: "+v[i]);
	    a.addElement(v[i]);
	}
    }

    public static void quicksort(String[] v) {
	quicksort(v, 0, v.length-1);
    }

    static void quicksort(String[] a, int left, int right) {
	int i=left;
	int j=right;
	if (left>=right) return;
	String pivot = a[right];       
	do {
	    while(less(a[i], pivot)) {
		i++;
	    }
	    
	    while(less(pivot, a[j])) {
		j--;
	    }
	    
	    if(i<=j) {
		swap(a, i, j);
		i++;
		j--;
	    }
	} while (i<j);
	
	quicksort(a, left, j);
	quicksort(a, i, right);
    }
} 

