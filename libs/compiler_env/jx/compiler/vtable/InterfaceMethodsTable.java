package jx.compiler.vtable;

import java.util.Vector;
import java.util.Hashtable;

import jx.zero.Debug;

// implements a table with holes
public class InterfaceMethodsTable {
    private Vector methods = new Vector();
    private Hashtable methodFinder = new Hashtable();
    
    InterfaceMethodsTable(ClassInfo objectClass) {
	addObjectMethods(objectClass);
    }

    public int nextFreeIndex() {
	return methods.size();
    }

    public void add(ClassInfo info) {
	for(int i=0; i<info.methods.length; i++) {
	    if (info.methods[i].name.equals("<clinit>")) continue; // interfaces could contain class constructors
	    if (contains(info.methods[i]) == -1) {
		append(info.methods[i]);
	    }
	}	
    }
    /**
     * add class or interface from lib 
     */
    public void addOld(ClassInfo info) {
	for(int i=0; i<info.mtable.length(); i++) {
	    if (info.mtable.getAt(i) == null) continue;
	    if (info.isInterface()) { 
		if (contains(info.mtable.getAt(i)) == -1) {
		    setAt(i, info.mtable.getAt(i));
		}
	    } else {
		markOccupied(i);
	    }
	}	
    }

    /**
     * add methods from class Object 
     */
    public void addObjectMethods(ClassInfo info) {
	for(int i=0; i<info.mtable.length(); i++) {
	    if (info.mtable.getAt(i) == null) continue;
	    if (contains(info.mtable.getAt(i)) == -1) {
		setAt(i, info.mtable.getAt(i));
	    }
	}	
    }

    public void append(Method m) {
	m.ifMethodIndex = nextFreeIndex();
	methods.addElement(m);
	methodFinder.put(m.nameAndType, m);
    }

    public void setAt(int index, Method m) {
	m.ifMethodIndex = index;
	if (methods.size() <= index) {
	    methods.setSize(index+1);
	}
	methods.setElementAt(m, index);
	methodFinder.put(m.nameAndType, m);
    }

    public boolean isFree(int index) {
	if (index >= methods.size()) return true;
	return false; // DO NOT (because markOccupied just increases size!): return methods.elementAt(index)==null;
    }

    public void markOccupied(int index) {
	if (methods.size() <= index) {
	    methods.setSize(index+1);
	}
    }

    /**
     * @return -1 when method not in table, otherwise index of method
     */
    public int contains(Method m) {
	if (m==null) return -1;
	Method mf = (Method)  methodFinder.get(m.nameAndType);
	if (mf == null) return -1;
	return mf.ifMethodIndex;
    }

    public void dump() {
	for(int i=0; i<methods.size();i++) {
	    Method m = (Method) methods.elementAt(i);
	    // Debug.out.println("IMT "+i+": "+(m==null?"null":m.nameAndType));
	}
    }
}

