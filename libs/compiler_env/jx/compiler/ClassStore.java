package jx.compiler;

import jx.zero.*;
import jx.compiler.execenv.BCClass;

import java.util.*;

import jx.collections.Iterator;
import jx.collections.List;

public class ClassStore implements ClassFinder {

    Hashtable classes = new Hashtable();
    List classList = new List();
    //    Hashtable registered = new Hashtable();

    public ClassStore() {
    }

    public ClassStore(List classList) {
	Iterator iter = classList.iterator();
	while(iter.hasNext()) {
	    BCClass c = (BCClass) iter.next();
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

    void addClass(String className, BCClass aClass) {
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

    public Iterator classSourceIterator() {
	return new Iterator() {
	    Iterator iter = classList.iterator();
	    public boolean hasNext() {
		return iter.hasNext();
	    }
	    public Object next() {
		return ((BCClass)iter.next()).getClassSource();
	    }
	};
    }

    public BCClass findClass(String className) {
	//Debug.out.println("findClass");
	BCClass ret = (BCClass)classes.get(className);
	if (ret == null) {
	    //Debug.out.println("Cannot find class "+className);
	}
	return ret;
    }

    public boolean isAssignableTo(String className, String superName) {
	//Debug.out.println("isInstanceOf: "+className+", "+superName);
	BCClass bc = findClass(className);
	BCClass sc = findClass(superName);
	return isAssignableTo(bc, sc);
    }

    public boolean isAssignableTo(BCClass bc, BCClass sc) {
	Debug.assert(sc != null);
	Debug.assert(bc != null);
	if (sc == bc) return true;
	if (isSubclassOf(sc, bc)) return true;
	return implementsInterface(sc, bc);
    }

    /**
     * @return true if clClass implements ifClass
     */
    public boolean implementsInterface(BCClass ifClass, BCClass clClass) {
	//Debug.out.println("IMPLI: "+ifClass+"  "+clClass);
	if (! ifClass.isInterface()) return false;
	if (ifClass == clClass) return true;	
	BCClassInfo info = (BCClassInfo)clClass.getInfo();
	for(int i=0; i<info.interfaces.length; i++) {
	    //Debug.out.println("   IMPLICHECK: "+ifClass+"  "+info.interfaces[i]);
	    if (isAssignableTo(info.interfaces[i], ifClass)) {
		//Debug.out.println(" TRUE");
		return true;
	    }
	}
	//Debug.out.println(" FALSE");
	return false;
    }

    /**
     * @param bc the base class
     * @param sc the subclass
     * @return true if sc is a subclass of bc
     */
    boolean isSubclassOf(BCClass bc, BCClass sc) {
	Debug.assert(bc != sc);
	Debug.assert(sc != null);
	Debug.assert(bc != null);
	//Debug.out.println("ISSUB: "+bc+"  "+sc);
	for(BCClass testClass = sc; testClass != null; ) {
	    //System.out.println("TESTSUB: "+bc.getClassName()+", "+testClass.getClassName());
	    if (bc == testClass) {
		//System.out.println("SUBCLASS: "+bc.getClassName()+", "+sc.getClassName());
		return true;	
	    }
	    BCClassInfo info = (BCClassInfo)testClass.getInfo();
	    testClass = info.superClass;
	}
	//System.out.println("NOT A SUBCLASS: "+bc.getClassName()+", "+sc.getClassName());
	return false;
    }

}
