package jx.classstore;


import jx.classfile.constantpool.*; 
import jx.classfile.*; 

import jx.zero.Debug;
import jx.zero.Memory;

import jx.zip.ZipFile;
import jx.zip.ZipEntry;


import jx.collections.QuickSort;
import jx.collections.SortedList;
import jx.collections.Iterator;
import jx.collections.Collection;
import jx.collections.Comparator;
import jx.collections.List;



import java.util.Vector;
import java.util.Enumeration;

import java.io.PrintStream;
import java.io.IOException;


/**
 * Use classmanager as follows:
 *  - add Object class
 *  - add predefined classes
 *  - then add Zip-Files 
 */
public class ClassManager implements ClassFinder {
    ClassStore libClassStore;
    ClassStore domClassStore;


    // add all classes that are in the zipfile
    public void addFromZip(Memory domainZip, Memory[] libZip) throws Exception {	
	Vector libdata = new Vector();
	Vector domdata = new Vector();
	libClassStore = new ClassStore();
	domClassStore = new ClassStore();

	// split zipfiles into classfiles
	{
	int i=-1;
	if (domainZip==null) i=0;
	for(; i<libZip.length; i++) {
	    Memory in;
	    if (i==-1) in = domainZip;
	    else  in = libZip[i];
	    ZipFile zip = new ZipFile(in);
	    ZipEntry entry = null;
	    while ((entry = zip.getNextEntry()) != null) {
		if (entry.isDirectory()) {
		    continue;
		}
		if (entry.getName().indexOf(".class")>0) {
		    if (i==-1) domdata.addElement(entry.getData());
		    else libdata.addElement(entry.getData());
		}
	    }
	}
	}
	// parse classfiles into classes
	try {
	    for(int i=0; i<libdata.size(); i++) {
		ClassData classData = new MemoryClassSource((Memory)libdata.elementAt(i)); 
		String className = classData.getClassName();
		ClassData oldClass = findClass(className);
		if (oldClass != null) {
		    Debug.throwError("Duplicate class "+className);
		}
		libClassStore.addClass(className, classData);
	    }
	    if (domainZip != null) {
		for(int i=0; i<domdata.size(); i++) {
		    ClassData classData = new MemoryClassSource((Memory)domdata.elementAt(i)); 
		    String className = classData.getClassName();
		    ClassData oldClass = findClass(className);
		    if (oldClass != null) {
			Debug.throwError("Duplicate class "+className);
		}
		    domClassStore.addClass(className, classData);
		}
	    }
	} catch(IOException e) {
	    Debug.throwError("Exception while reading classes.");
	}
    }

    private void computeClassGraph() {
	Enumeration classIterator = domClassStore.elements();
	while(classIterator.hasMoreElements()) {
	    ClassData aClass = (ClassData)classIterator.nextElement();
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

    public Enumeration getDomClasses() {
	return domClassStore.elements();
    }

    public Iterator getAllClasses() {
	return new Iterator() {
		Iterator d = domClassStore.iterator();
		Iterator l = libClassStore.iterator();
		public boolean hasNext() {
		    return d.hasNext() || l.hasNext();
		}
		public Object next() {
		    if (d.hasNext()) return d.next();
		    return l.next();
		}
	    };
    }

    /*
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
	
	
	
	Iterator iter = sorted.iterator();
	while(iter.hasNext()) {
	    BCClass c = (BCClass) iter.next();
	    //Debug.out.println("SORTED:"+c.getClassName());
	}
	
	return new ClassStore(sorted);
     }
    */


    /**
     * Implementation of the ClassFinder interface
     */
    public ClassData findClass(String classN) {
	String className = classN.replace('.', '/');
	ClassData cc =  domClassStore.findClass(className);
	if (cc ==null) cc = libClassStore.findClass(className);
	//if (cc!=null)Debug.out.println("FOUNDCLASS: "+className +" -> "+cc.hashCode());
	return cc;
    }


    public void dump() {
	Debug.out.println("Domainclasses:");
	domClassStore.dump();
	Debug.out.println("Libclasses:");
	libClassStore.dump();
    }

    /*
    public boolean isAssignableTo(String className, String superName) {
	BCClass c1 = findClass(className);
	if (c1 == null ) Debug.throwError("class "+className+" not found");
	BCClass c2 = findClass(superName);
	if (c2==null) Debug.throwError("class "+superName+" not found");
	return domClassStore.isAssignableTo(c1, c2);
    }

    public boolean isAssignableTo(ClassData c1, BCClass c2) {
	return domClassStore.isAssignableTo(c1, c2);
    }
    */

}

