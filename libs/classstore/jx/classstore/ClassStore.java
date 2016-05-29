package jx.classstore;

import jx.classfile.*;

import jx.zero.*;
import java.util.*;

import jx.collections.Iterator;
import jx.collections.List;


class ClassStore implements ClassFinder {

    Hashtable classes = new Hashtable();
    List classList = new List();
    //    Hashtable registered = new Hashtable();

    public ClassStore() {
    }

    public ClassStore(List classList) {
	Iterator iter = classList.iterator();
	while(iter.hasNext()) {
	    ClassData c = (ClassData) iter.next();
	    addClass(c.getClassName(), c);
	}
    }

    public int size() {
	return classes.size();
    }

    void initPrimitiveClasses() {
	addPrimitiveClass("C");
    }

    void addPrimitiveClass(String className) {
	addClass(className, null);
    }

    void addArrayClass(String elementClassName, Memory elemPtr) {
	addClass('['+elementClassName, null);
    }

    //  void registerClass(String className, BCClass aClass) {
    //	registered.put(className, aClass);
    //}

    void addClass(String className, ClassData aClass) {
	//Debug.out.println("addClass: "+className);
	classes.put(className, aClass);
	classList.add(aClass);
    }
    
    public Enumeration elements() {
	return classList.elements();
    }

    public Iterator iterator() {
	return classList.iterator();
    }
    public Iterator getAllClasses() {
	return classList.iterator();
    }

    public Iterator classSourceIterator() {
	return new Iterator() {
	    Iterator iter = classList.iterator();
	    public boolean hasNext() {
		return iter.hasNext();
	    }
	    public Object next() {
		return (ClassData)iter.next();
	    }
	};
    }

    public ClassData findClass(String className) {
	//Debug.out.println("findClass");
	ClassData ret = (ClassData)classes.get(className);
	if (ret == null) {
	    //Debug.out.println("Cannot find class "+className);
	}
	return ret;
    }

    public void dump() {
	Enumeration e = classList.elements();
	for(;e.hasMoreElements();) {
	    ((ClassData)e.nextElement()).dump();
	}
    }

}
