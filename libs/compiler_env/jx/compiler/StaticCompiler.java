/*
 * Bytecode to nativecode driver.
 *
 * Copyright 2000 Michael Golm
 */
package jx.compiler;

import jx.classfile.constantpool.*; 
import jx.classfile.*; 

import jx.zero.Debug;
import jx.zero.Memory;
import jx.zero.ReadOnlyMemory;

import jx.zip.ZipFile;
import jx.zip.ZipEntry;

import jx.collections.QuickSort;
import jx.collections.SortedList;
import jx.collections.Iterator;
import jx.collections.Collection;
import jx.collections.Comparator;
import jx.collections.List;

import jx.compiler.persistent.*;

import jx.compiler.vtable.MethodTableFactory;
import jx.compiler.vtable.ClassFactory;
import jx.compiler.vtable.ClassInfo;
import jx.compiler.vtable.Method;
import jx.compiler.vtable.MethodTable;

import jx.compiler.imcode.ExecEnvironmentInterface;
import jx.compiler.execenv.BCClass;
import jx.compiler.execenv.BCMethod;
import jx.compiler.execenv.BCMethodWithCode;
import jx.compiler.execenv.IOSystem;
import jx.compiler.symbols.*;

import java.util.Vector;
import java.util.Enumeration;

import java.io.PrintStream;
//import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.DataOutputStream;

import jx.compspec.MetaInfo;

import jx.compiler.execenv.NativeCodeContainer;
import jx.compiler.execenv.BinaryCode;

public class StaticCompiler implements ClassFinder {
    static final boolean dumpAll = false;
    static final boolean verboseFieldLayout = false;
    static final boolean opt_v = false;

    static final boolean addPredefinedObject = true;

    ExecEnvironmentInterface execEnvNew;

    PrintStream gasFile;
    CodeFile codeFile;
    ExtendedDataOutputStream out;
    ClassStore libClassStore;
    ClassStore domClassStore;
    ClassInfo[] predefinedClasses;
    ExtendedDataInputStream tableIn[];
    ExtendedDataOutputStream tableOut;
    CompilerOptions options;
    IOSystem ioSystem;
    jx.compiler.StatisticInfo stat;
    MetaInfo meta;

    // compile all classes that are in the zipfile
    public StaticCompiler(PrintStream gasFile, 
			  ExtendedDataOutputStream out,
			  ExtendedDataOutputStream tableOut,			  
			  ReadOnlyMemory domainZip, 
			  ReadOnlyMemory[] libZip,
			  ExtendedDataInputStream tableIn[],
			  CompilerOptions opts,
			  IOSystem ioSystem
			  ) throws Exception {

	if (opts==null) {
	    this.options = new CompilerOptions();
	} else {
	    this.options = opts;
	}

	this.stat = new jx.compiler.StatisticInfo(opts.getTargetName()+" ");

	this.execEnvNew = new ExecEnvironmentIA32(this,options);

	this.gasFile = gasFile;
	this.out = out;
	this.tableOut = tableOut;
	this.tableIn = tableIn;
	this.ioSystem = ioSystem;

	Vector libdata = new Vector();
	Vector domdata = new Vector();
	libClassStore = new ClassStore();
	domClassStore = new ClassStore();

	if (addPredefinedObject) {
	// play a trick: java.lang.Object is automatically part of libclasstore
	// this removes domainzero dependencies from a certain lib
	ClassSource objectClassSource = new NativeClassSource("java/lang/Object", null);
	BCClass objectClass = new BCClass(objectClassSource,"java/lang/Object");
	libClassStore.addClass("java/lang/Object", objectClass);
	predefinedClasses = new ClassInfo[1];
	Vector objectMtable = new Vector();
	predefinedClasses[0] = new ClassInfo("java/lang/Object", "", false);

	String[][] omethods = {{"getClass", "()Ljava/lang/Class;"},
			       {"hashCode", "()I"},
			       {"clone", "()Ljava/lang/Object;"},
			       {"wait", "()V"},
			       {"wait", "(J)V"},
			       {"wait", "(JI)V"},
			       {"notify", "()V"},
			       {"notifyAll", "()V"},
			       {"toString", "()Ljava/lang/String;"},
			       {"equals", "(Ljava/lang/Object;)Z"},
			       {"finalize", "()V"}};

	for(int i=0; i<omethods.length; i++) {
	    addMethod(predefinedClasses[0], objectMtable, omethods[i][0], omethods[i][1]);
	}
	predefinedClasses[0].addAll(objectMtable);

	createClassInfoForClass(objectClass);
	BCClassInfo oinfo = (BCClassInfo) objectClass.getInfo();
	oinfo.objectLayout = new FieldLayout();
	oinfo.classLayout = new FieldLayout();
	oinfo.methodTable = new MethodTable(objectMtable, "java/lang/Object");
	//Debug.out.println("ObjectCLASS -> "+objectClass.hashCode());
	} // end addPredefinedObject


	// split zipfiles into classfiles
	for(int i=-1; i<libZip.length; i++) {
	    ReadOnlyMemory in;
	    if (i==-1) in = domainZip;
	    else  in = libZip[i];
	    ZipFile zip = new ZipFile(in);
	    ZipEntry entry = null;
	    while ((entry = zip.getNextEntry()) != null) {
		if (entry.isDirectory()) {
		    continue;
		}
		//if (entry.getName().indexOf(".class")>0) {		    

		if (i==-1 && entry.getName().equals("META")) {
		    ReadOnlyMemory mem = entry.getData();
		    byte[] barr = new byte[mem.size()];
		    mem.copyToByteArray(barr, 0, 0, mem.size());
		    meta = new MetaInfo("", barr);
		    continue;
		}

		if (!entry.getName().equals("libs.dep")&&
		    !entry.getName().equals("META")) {
		    //Debug.out.println("classfile "+entry.getName());
		    if (i==-1) domdata.addElement(entry.getData());
		    else libdata.addElement(entry.getData());
		}
	    }
	}

	this.codeFile = new CodeFile(options,meta);

	// parse classfiles into classes
	try {
	    if (opt_v) Debug.out.println("Adding classes from existing lib");
	    for(int i=0; i<libdata.size(); i++) {
		ClassSource classData = new MemoryClassSource((Memory)libdata.elementAt(i)); 
		String className = classData.getClassName();

		if (addPredefinedObject && className.equals("java/lang/Object")) continue;

		if (opt_v) Debug.out.println("Add class "+className);

		BCClass oldClass = findClass(className);
		if (oldClass != null) {
		    Debug.throwError("Duplicate class "+className);
		}
		BCClass clazz = new BCClass(classData, className);
		libClassStore.addClass(className, clazz);
	    }
	    if (opt_v) Debug.out.println("Adding classes from lib");
	    for(int i=0; i<domdata.size(); i++) {
		ClassSource classData = new MemoryClassSource((Memory)domdata.elementAt(i)); 
		String className = classData.getClassName();

		if (addPredefinedObject && className.equals("java/lang/Object")) continue;

		if (opt_v) Debug.out.println("Add class "+className);

		BCClass oldClass = findClass(className);
		if (oldClass != null) {
		    Debug.out.println("Duplicate class "+className);
		    Debug.throwError("Duplicate class "+className);
		}
		BCClass clazz = new BCClass(classData, className);
		domClassStore.addClass(className, clazz);
	    }
	} catch(IOException e) {
	    Debug.throwError("Exception while reading classes.");
	}

	compileStatic();
    }

    private void addMethod(ClassInfo aClass, Vector mtable, String methodName, String signature) {
	Method method;
	method = new Method(aClass, methodName, signature);
	mtable.addElement(method);
	method.indices.addElement(new Integer(mtable.size()-1));
    }

    private void computeClassGraph() {
	Enumeration classIterator = domClassStore.elements();
	while(classIterator.hasMoreElements()) {
	    BCClass aClass = (BCClass)classIterator.nextElement();
	    ConstantPool cp = aClass.getConstantPool();
	    int numEntries = cp.getNumberOfEntries();
	    for(int i=0; i<numEntries; i++) {
		ConstantPoolEntry e = cp.entryAt(i);
		if (e instanceof ClassCPEntry) {
		    ClassCPEntry ec = (ClassCPEntry)e;
		    String className = ec.getClassName();
		    if (className.charAt(0) == '[') {
			//Debug.out.println("Skipping array class "+className);
			continue;
		    }
		    if (findClass(className) == null) {
			// not seen until now
			Debug.throwError("Class "+ec.getClassName()+ " not found. Needed by "+aClass.getClassName());
		    }
		}
	    }
	}
    }



    private ClassStore sortClasses(ClassStore classStore) {
	Vector all = new Vector();
	Enumeration classIterator = classStore.elements();
	while(classIterator.hasMoreElements()) {
	    all.addElement(classIterator.nextElement());
	}
	List sorted = new List();
	for(int i=0; i<all.size(); i++) {
	    BCClass c = (BCClass) all.elementAt(i);
	    BCClassInfo info = (BCClassInfo)c.getInfo();
	    BCClass s = info.superClass;
	    if (s == null) { // Object class
		sorted.add(c);
		all.setElementAt(null, i);
		break;
	    }
	}
    outerloop:
	for(int i=0; i<all.size(); i++) {
	    BCClass c = (BCClass) all.elementAt(i);
	    if (c == null) continue;
	    BCClassInfo info = (BCClassInfo)c.getInfo();
	    BCClass s = info.superClass;
	    for(int j=0; j<sorted.size(); j++) {
		if ((BCClass) all.elementAt(i) == s) {
		    sorted.add(c);
		    all.setElementAt(null, i);
		    continue outerloop; // OK
		}
	    }
	    // superclass not in sorted list
	    // add all superclasses (starting with Object) to
	    // the sorted list, provided they are not already
	    // on the list
	    Vector classesToAdd = new Vector();
	    classesToAdd.addElement(c);
	    all.setElementAt(null, i);
	    c = ((BCClassInfo)c.getInfo()).superClass;
	searchloop:
	    while(true) {
		for(int j=i; j<all.size(); j++) {
		    if (c == (BCClass) all.elementAt(j)) {
			classesToAdd.addElement(c);
			all.setElementAt(null, j);
			c = ((BCClassInfo)c.getInfo()).superClass;
			continue searchloop;
		    }
		}
		break;
	    }
	    // add to sorted in reverse order
	    for(int j=classesToAdd.size()-1; j>=0; j--) {
		sorted.add(classesToAdd.elementAt(j));
	    }
	}
			
	return new ClassStore(sorted);
    }
    
    private void createInterfaceMethodTables(ClassStore classStore) {
    }

    private void compileToDomain(ClassStore classStore) {
	Iterator iter = classStore.iterator();
	while(iter.hasNext()) {
	    BCClass c = (BCClass) iter.next();
	    compileToDomain(c);
	}
    }

    private void compileToDomain(BCClass aClass) {
	try {
	    options.printVerbose("***** Compiling class: " + aClass.getClassName());	    

	    execEnvNew.setCurrentlyCompiling(aClass);

	    ConstantPool cPool = aClass.getConstantPool(); 
	    BCClassInfo info = (BCClassInfo)aClass.getInfo();		

	    PrintStream imOut = null;
	    if (options.doPrintIMCode()) {
		try {
		    imOut = new PrintStream(ioSystem.getOutputStream(aClass.getClassName()+".imcode"));	
		} catch (java.io.IOException ex) {
		    ex.printStackTrace(); 
		    Debug.throwError(); 
		}
		imOut.println("class "+aClass.getClassName()+" {");
	    }
	    
	    for(int i=0; i<info.methods.length; i++) {
		BCMethod m = info.methods[i];

		if (m.isNative()) {
		    // compile native method to throw exception
		    /*
		    jx.compiler.imcode.CodeContainer imCode = new jx.compiler.imcode.CodeContainer(execEnvNew,method);
		    imCode.init();
		    imCode.translate();
		    if (options.doPrintIMCode()) imCode.writeCode(imOut);
		    info.nativeCode[i] = imCode;
		    */
		    info.nativeCode[i] = new NativeCodeContainer() {
			    public BinaryCode getMachineCode() {throw new Error("NATIVE");}
			    public Vector     getInstructionTable() {throw new Error("NATIVE");}
			    public int        getLocalVarSize() {throw new Error("NATIVE");}
			};
		    continue;
		}

		if (! (m instanceof BCMethodWithCode)) continue;
		BCMethodWithCode method = (BCMethodWithCode)m;
		options.printVerbose("**  Compiling method "+method.getName());
		
		
		jx.compiler.imcode.CodeContainer imCode = new jx.compiler.imcode.CodeContainer(execEnvNew,method,stat);
		
		imCode.init();
		if (options.doInlining(aClass,method)) {
		    if (options.doPrintIMCode()) {
			imOut.println("// try to inline methods ");
		    }
		    imCode.inlineMethods(4);
		}
		
		try {			    
		    imCode.translate();
		    if (options.doPrintIMCode()) imCode.writeCode(imOut);
		    info.nativeCode[i] = imCode;
		} catch (Exception ex) {
		    if (options.doInlining(aClass,method)) {
			System.err.println("!! FAIL to inline !!!!!!!!!!!!!!!");
			if (options.doVerbose("inline")) {
			    ex.printStackTrace();
			} else {
			    System.err.println("!! "+ex.getClass().getName());
			}
			System.err.println("!! try without inlining !!!!!!!!!");
			imCode = new jx.compiler.imcode.CodeContainer(execEnvNew,method);
			imCode.init();
			imCode.translate();
			if (options.doPrintIMCode()) imCode.writeCode(imOut);
			info.nativeCode[i] = imCode;
		    } else { 
			ex.printStackTrace();		    
			System.exit(1);
		    }
		}
		
		if (options.doPrintIMCode()) imCode.writeStatistics(imOut);
	    }
	    
	    if (options.doPrintIMCode()) {
		imOut.println("}");
		imOut.close();
	    }		
	} catch (CompileException ex) {
	    Debug.out.println("Compilation Failed :" + ex.getMessage()); 
	    ex.printStackTrace(); 
	    Debug.throwError(); 
	}
    }
    
    private void createClassInfoForClass(BCClass aClass) throws CompileException {

	if (aClass.getInfo() != null) return; // predefined classinfo
	/* if (options.doDebug()) Debug.out.println("Creating classinfo for "+aClass.getClassName()); */

	BCClassInfo info = new BCClassInfo(); 
	aClass.setInfo(info);
	info.methods = aClass.getAllMethodsWithCode(options.replaceInterfaceWithClass); 
	info.nativeCode = new jx.compiler.execenv.NativeCodeContainer[info.methods.length];

	ClassSource cs = aClass.getClassSource();
	String superName = cs.getSuperClassName();
	if (superName == null) {
	    info.superClass = null;
	} else {
	    BCClass cc =  findClass(superName);
	    if (cc ==null) Debug.throwError("Class \""+superName+"\" not found.  Needed for class "+aClass.getClassName());
	    info.superClass = cc;
	    
	}
	String[] ifs = cs.getInterfaceNames();
	info.interfaces = new BCClass[ifs.length];
	for(int j=0; j<ifs.length; j++) {
	    BCClass cc =  findClass(ifs[j]);
	    if (cc ==null) {
		Debug.throwError("Class "+ifs[j]+" not found. Needed for class "+aClass.getClassName());
	    }
	    info.interfaces[j] = cc;
	}
    }

    private void createClassInfo(ClassStore classStore) throws CompileException {
	Enumeration classIterator = classStore.elements();
	while(classIterator.hasMoreElements()) {
	    BCClass c = (BCClass)classIterator.nextElement();
	    createClassInfoForClass(c);
	}
    }
    
    private void compileStatic() throws Exception {
	Iterator iter=null;
	computeClassGraph();
	//Debug.out.println("**Finished class checking!");

	createClassInfo(libClassStore);
	createClassInfo(domClassStore);

	domClassStore = sortClasses(domClassStore); // we dont need to sort the classes anymore
	libClassStore = sortClasses(libClassStore);

	new ConstraintChecker(domClassStore.classSourceIterator());
	MethodTableFactory mtableFactory = new MethodTableFactory(domClassStore.classSourceIterator(), predefinedClasses, tableIn);
	mtableFactory.process();

	iter = libClassStore.iterator();
	while(iter.hasNext()) {
	    BCClass c = (BCClass) iter.next();
	    BCClassInfo info = (BCClassInfo)c.getInfo();
	    info.methodTable = mtableFactory.getMethodTable(c.getClassName());
	    //Debug.out.println(" setting mtable for "+c.getClassName());
	    if (info.methodTable==null) Debug.throwError("No Methodtable for "+c.getClassName());
	}

	iter = domClassStore.iterator();
	while(iter.hasNext()) {
	    BCClass c = (BCClass) iter.next();
	    BCClassInfo info = (BCClassInfo)c.getInfo();
	    info.methodTable = mtableFactory.getMethodTable(c.getClassName());
	    if (dumpAll) { 
		Debug.out.println(" setting mtable for "+c.getClassName());
		info.methodTable.print();
	    }
	    if (info.methodTable==null) Debug.throwError("No Methodtable for "+c.getClassName());
	}


	// compute object layout and class layout
	computeObjectAndClassLayout(libClassStore);
	computeObjectAndClassLayout(domClassStore);

	options.printVerbose("**********Finished resolving!");
	
	compileToDomain(domClassStore);

	options.printVerbose("**********Finished compiling!");

	if (options.doVerbose("stat")) {
	    System.out.println(stat.toString());
	}	

	codeFile.write(out, domClassStore);
	options.printVerbose("written");

	out.close();
	options.printVerbose("closed");

	mtableFactory.serialize(tableOut);
	options.printVerbose("table");

	tableOut.close();
	options.printVerbose("table closed");

    }

    private void computeObjectAndClassLayout(ClassStore classStore) {
	Iterator iter = classStore.iterator();
	while(iter.hasNext()) {
	    BCClass c = (BCClass) iter.next();
	    computeObjectAndClassLayout(c);
	    computeMappedLayout(c);
	}
    }

    private void computeObjectAndClassLayout(BCClass aClass) {
	BCClassInfo info = (BCClassInfo)aClass.getInfo();
	if (info.objectLayout != null && info.classLayout != null) return;
	info.objectLayout = new FieldLayout();
	info.classLayout = new FieldLayout();
	BCClass s = info.superClass;
	if (s != null) {
	    FieldLayout superLayout = ((BCClassInfo)s.getInfo()).objectLayout;
	    if (superLayout == null) {
		Debug.throwError("FieldLayout of class "+s.getClassName()+" not initialized.");
	    }
	    info.objectLayout.addFields(superLayout);
	}
	ClassSource source = aClass.getClassSource();
	FieldData[] fields = source.getFields();
	//Debug.out.println("FieldLayout::getObjectLayout: "+aClass.getClassName()+","+fields.length);
	for(int i=0; i<fields.length; i++) {
	    if (fields[i].isStatic()) {
		//Debug.out.println("compute static FieldLayout "+aClass.getClassName()+","+fields[i].getName());
		info.classLayout.addField(fields[i].getName(), fields[i].getType());
	    } else {
		//Debug.out.println("FieldLayout::getObjectLayout: "+aClass.getClassName()+","+fields[i].getName());
		info.objectLayout.addField(fields[i].getName(), fields[i].getType());
	    }
	}
	if(verboseFieldLayout) {
	    Debug.out.println("FieldLayout of class "+aClass.getClassName());
	    info.objectLayout.dump();
	    Debug.out.println("StaticLayout of class "+aClass.getClassName());
	    info.classLayout.dump();
	}
    }

    private void computeMappedLayout(BCClass aClass) {
	BCClass mapClass = findClass("jx/zero/MappedLittleEndianObject");
	if (! (mapClass != null && isAssignableTo(aClass, mapClass))) return;
	BCClassInfo info = (BCClassInfo)aClass.getInfo();
	if (info.mappedLayout != null) return;
	info.mappedLayout = new CompactFieldLayout();
	BCClass s = info.superClass;
	if (s != null) {
	    info.mappedLayout.addFields(((BCClassInfo)aClass.getInfo()).mappedLayout);
	}
	ClassSource source = aClass.getClassSource();
	FieldData[] fields = source.getFields();
	for(int i=0; i<fields.length; i++) {
	    if (! fields[i].isStatic()) {
		info.mappedLayout.addField(fields, fields[i].getName(), fields[i].getType());
	    }
	}
    }

    /**
     * Implementation of the ClassFinder interface
     */
    public BCClass findClass(String className) {
	BCClass cc =  domClassStore.findClass(className);
	if (cc ==null) cc = libClassStore.findClass(className);
	return cc;
    }

    public boolean isAssignableTo(String className, String superName) {
	BCClass c1 = findClass(className);
	if (c1 == null ) Debug.throwError("class "+className+" not found");
	BCClass c2 = findClass(superName);
	if (c2==null) Debug.throwError("class "+superName+" not found");
	return domClassStore.isAssignableTo(c1, c2);
    }

    public boolean isAssignableTo(BCClass c1, BCClass c2) {
	return domClassStore.isAssignableTo(c1, c2);
    }

    public boolean implementsInterface(BCClass ifClass, BCClass clClass) {
	return domClassStore.implementsInterface(ifClass, clClass);
    }

    public static String version() {
	return "1";
    }
}

