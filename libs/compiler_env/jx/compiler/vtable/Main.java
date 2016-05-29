package jx.compiler.vtable;

import jx.zero.MemoryManager;
import jx.zero.Memory;
import jx.zero.Debug;

import jx.classfile.ClassData;
import jx.classfile.ClassSource;
import jx.classfile.MethodSource;
import jx.classfile.constantpool.ConstantPool;
import jx.classfile.constantpool.ConstantPoolEntry;
import jx.classfile.constantpool.ClassCPEntry;

import jx.compiler.persistent.ExtendedDataOutputStream;
import jx.compiler.persistent.ExtendedDataInputStream;
import jx.compiler.ZipClasses;

import java.io.IOException;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;

/**
 * Experimental VTable generator
 */

class Main {
    static final boolean dumpAll = false;
    public static final int MAGIC_NUMBER = 0xaddedade;

    Hashtable classPool = new Hashtable();
    Hashtable classFinder = new Hashtable();
    Vector all = new Vector();
    InterfaceMethodsTable itable;
    Hashtable mtables = new Hashtable();
    Vector classList = new Vector();
    int originalClasses; // number of classes *not* imported from lib

    Main(Memory zipfilecontents, String[] args, ClassInfo objectClass) throws Exception {
	itable = new InterfaceMethodsTable(objectClass);    
	ZipClasses zip = new ZipClasses(zipfilecontents, true);

	Enumeration elements = zip.elements();
	while(elements.hasMoreElements()) {
	    ClassSource source = (ClassSource)elements.nextElement();
	    ClassInfo info = new ClassInfo();
	    info.data = source;
	    info.isInterface = source.isInterface();
	    info.className = source.getClassName();
	    System.out.println(info.className);
	    MethodSource [] m =  source.getMethods();
	    info.methods = new Method[m.length];
	    for(int i=0; i<m.length; i++) {
		info.methods[i] = new Method(info, m[i]);
	    }
	    info.indexInAll = all.size();
	    classFinder.put(info.className, info);
	    all.addElement(info);
	}
    }

    Main(Hashtable classFinder, Vector all, ClassInfo[] predefinedClasses, ExtendedDataInputStream[] oldTables, ClassInfo objectClass) throws Exception {
	this.classFinder = classFinder;
	this.all = all;
	itable = new InterfaceMethodsTable(objectClass);    
	for(int i=0; i<predefinedClasses.length; i++) {
	    addOldClass(predefinedClasses[i]);
	}
	for(int i=0; i<predefinedClasses.length; i++) {
	    predefinedClasses[i].adjustSuperClass(classFinder);
	}
	for(int i=0; i<oldTables.length; i++) {
	    //Debug.out.println("Reading table "+i);
	    deserialize(oldTables[i]);
	}
	
    }

    /**
     * deserialize method tables 
     */

    void process() throws Exception {
	ClassInfo info;
	//info = (ClassInfo)classFinder.get("java/lang/Object");
	///putIn(info, true, true);

	for(int i=0; i<all.size(); i++) {
	    info = (ClassInfo)all.elementAt(i);
	    if (info == null) continue;
	    putIn(info, true, true, false);
	}

	for(int i=0; i<all.size(); i++) {
	    info = (ClassInfo)all.elementAt(i);
	    if (info == null) continue;
	    putIn(info, true, false, false);
	}

	for(int i=0; i<all.size(); i++) {
	    info = (ClassInfo)all.elementAt(i);
	    if (info == null) continue;
	    putIn(info, false, false, false);
	}
	
    }


    boolean putIn(ClassInfo info, boolean rejectInterfaces, boolean rejectImplements, boolean enforcePutin) throws Exception {
	if (!enforcePutin && all.elementAt(info.indexInAll) == null) return true; // already there
	if (rejectInterfaces && info.isInterface()) return false;
	String[] ifs = info.data.getInterfaceNames();
	if (rejectImplements && ifs.length > 0) return false;
	if (! info.className.equals("java/lang/Object")) {
	    String s = info.data.getSuperClassName();
	    //Debug.out.println("FINDING SUPERCLASS: "+s);
	    info.superClass = (ClassInfo)classFinder.get(s);
	    if (info.superClass == null) {
		throw new Exception("no superclass "+s+" found for class "+info.className);
	    }
	    if (classPool.get(info.superClass.className) == null) {
		if (! putIn(info.superClass, rejectInterfaces, rejectImplements,enforcePutin)) return false;
	    }
	}
	// now put in interfaces
	for(int i=0; i<ifs.length; i++) {
	    // override rejectInterfaces
	    if (classPool.get(ifs[i]) == null) {
		ClassInfo inf = (ClassInfo)classFinder.get(ifs[i]);
		if (inf==null) {
		    throw new Error("Interface needed but not found "+ifs[i]);
		}
		if (! putIn(inf, false, rejectImplements,enforcePutin)) return false;
	    }
	}
	/*Debug.out.println("putin: "+info.className);*/
	createMTable(info);
	addMTable(info);
	all.setElementAt(null, info.indexInAll);
	return true;
    }

    void addMTable(ClassInfo info) {
	mtables.put(info.className, info.mtable);

	classList.addElement(info);
	originalClasses++;
	classPool.put(info.className, info);
    }

    void createMTable(ClassInfo info) {
	if (dumpAll) Debug.out.println("  Creating mtable for "+info.className);
	if (info.isInterface()) { 
	    itable.add(info);
	    info.mtable = new InterfaceMethodTable(info, itable, classPool);
	    //Debug.out.println("IF: "+info.className);
	    //info.mtable.print();
	    return;
	}

	Vector mt = new Vector();
	Hashtable inherited = new Hashtable();
	// copy superclass mtable
	if (info.superClass != null) {
	    for(int i=0; i<info.superClass.mtable.length(); i++) {
		mt.addElement(info.superClass.mtable.getAt(i));
		if (info.superClass.mtable.getAt(i) != null) // mtable may have holes!
		    inherited.put(info.superClass.mtable.getAt(i).nameAndType, info.superClass.mtable.getAt(i));
	    }
	}
	/*
	for(int i=0; i<info.methods.length; i++) {
	    if (itable.contains(info.methods[i])) {
		Debug.out.println("Method "+info.methods[i].nameAndType+" clashes.");
	    }
	}
	*/
	// override method or append method
	for(int i=0; i<info.methods.length; i++) {
	    if (info.methods[i].data.isStatic() || info.methods[i].name.equals("<init>"))
		continue;
	    Method im = (Method) inherited.get(info.methods[i].nameAndType);
	    inherited.put(info.methods[i].nameAndType, info.methods[i]); // override/append in method finder
	    if (im != null) {
		//Debug.out.println("Override "+info.methods[i].nameAndType);
		info.methods[i].indices = (Vector)im.indices.clone();
		for(int j=0; j<im.indices.size(); j++) { // method may appear more than once in mtable
		    int index = ((Integer)im.indices.elementAt(j)).intValue();
		    if (mt.size() <= index) mt.setSize(index+1);
		    mt.setElementAt(info.methods[i], index);
		}
	    } else {
		int index = mt.size();
		for(index = mt.size(); ! itable.isFree(index); index++);
		info.methods[i].indices.addElement(new Integer(index));
		mt.setSize(index+1);
		mt.setElementAt(info.methods[i], index);
		itable.markOccupied(index);
	    }
	}
	// add new interface methods
	String [] ifs =info.data.getInterfaceNames();
	for(int i=0; i<ifs.length; i++) {
	    ClassInfo ii = (ClassInfo)classPool.get(ifs[i]);
	    if (dumpAll) Debug.out.println("   implements INTERFACE: "+ii.className);
	    //if (ii.methods == null) {
		for(int j=0; j<ii.mtable.length(); j++) {
		    if(ii.mtable.getAt(j)==null) continue;
			if (dumpAll) Debug.out.println("METHOD "+j+":"+ii.mtable.getAt(j).ifMethodIndex

+ii.mtable.getAt(j).nameAndType);
		    Method realM = (Method)inherited.get(ii.mtable.getAt(j).nameAndType);
		    if(realM == null) {
			System.out.println("METHOD NOT IMPLEMENTED: "+ii.mtable.getAt(j).nameAndType);
			System.out.println("   IF: "+ii.className);
			System.out.println("   CL: "+info.className);
		    }
		    /*
		    if(ii.mtable.getAt(j).ifMethodIndex == 0) {
			System.out.println("ERROR: interface method index = 0");
			System.out.println("       at "+j);
			ii.mtable.print();
			throw new Error();
		    }
		    */
		    if (mt.size() <= ii.mtable.getAt(j).ifMethodIndex)
			mt.setSize(ii.mtable.getAt(j).ifMethodIndex+1);
		    if (dumpAll) Debug.out.println("  INTERFACEMETHOD "+ii.mtable.getAt(j).nameAndType+" at "+ii.mtable.getAt(j).ifMethodIndex);
		    mt.setElementAt(realM, ii.mtable.getAt(j).ifMethodIndex);
		    realM.indices.addElement(new Integer(ii.mtable.getAt(j).ifMethodIndex));
		}
		/* } else {
		for(int j=0; j<ii.methods.length; j++) {
		    if (ii.methods[j].name.equals("<clinit>")) continue;
		    Method realM = (Method)inherited.get(ii.methods[j].nameAndType);
		    if(realM == null) {
			System.out.println("METHOD NOT IMPLEMENTED: "+ii.methods[j].nameAndType);
			System.out.println("   IF: "+ii.className);
			System.out.println("   CL: "+info.className);
		    }
		    if (mt.size() <= ii.methods[j].ifMethodIndex)
			mt.setSize(ii.methods[j].ifMethodIndex+1);
		    mt.setElementAt(realM, ii.methods[j].ifMethodIndex);
		    realM.indices.addElement(new Integer(ii.methods[j].ifMethodIndex));
		}
		}*/
	}

	info.mtable = new MethodTable(mt, info.className);
    }

    void printMTable(ClassInfo info) {
	info.mtable.print();
    }

    public MethodTable getMethodTable(String className) {
	return (MethodTable) mtables.get(className);
    }

    public void serialize(ExtendedDataOutputStream out) throws IOException {
	out.writeInt(MAGIC_NUMBER);
	out.writeInt(originalClasses);
	for(int i=0; i<classList.size(); i++) {
	    ClassInfo info = (ClassInfo)classList.elementAt(i);
	    if (info.isOld) continue;
	    info.serialize(out);
	}
    }

    public void addOldClass(ClassInfo info) throws Exception {
	info.isOld = true;
	classFinder.put(info.className, info);
	//Debug.out.println("ADDCF: "+info.className);
	addMTable(info);	    
	//printMTable(info);
	originalClasses--; // this is an imported class, correct counter
	itable.addOld(info);
    }
    
    public void deserialize(ExtendedDataInputStream in) throws Exception {
	int magic = in.readInt();
	if (magic != MAGIC_NUMBER) {
	    throw new IOException("This is not a method table  "+magic);
	}
	int ntables = in.readInt();
	for(int i=0; i<ntables; i++) {
	    ClassInfo info = new ClassInfo(in, classPool);
	    addOldClass(info);
	}
    }

}
