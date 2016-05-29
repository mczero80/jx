package jx.compiler.vtable;

import java.util.Hashtable;
import java.util.Vector;
import jx.zero.Debug;

public class InterfaceMethodTable extends MethodTable {
    private static final boolean dumpAll = false;
    InterfaceMethodsTable itable;
    Vector mt = new Vector();
    InterfaceMethodTable(String className){
	super(className);
	Debug.out.println("Creating empty InterfaceMethodTable");
    }
    InterfaceMethodTable(ClassInfo info, InterfaceMethodsTable itable, Hashtable classPool) {	
	super(info.className);
	if (dumpAll) System.out.println("Procesing "+info.className);
	this.itable = itable;
	// first include object methods
	ClassInfo objInfo = (ClassInfo)classPool.get("java/lang/Object");
	for(int j=0; j<objInfo.mtable.length();j++) {
	    Method m = objInfo.mtable.getAt(j);
	    if (m==null) continue;
	    if (dumpAll) Debug.out.println("object-method impl:"+m.nameAndType);
	    addMethodAt(m,j);
	}
	

	// implements interface methods
	String ifs[] = info.data.getInterfaceNames();
	for(int i=0; i<ifs.length; i++) {
	    ClassInfo in = (ClassInfo)classPool.get(ifs[i]);
	    for(int j=0; j<in.mtable.length();j++) {
		Method m = in.mtable.getAt(j);
		if (m==null) continue;
		if (dumpAll) Debug.out.println("impl:"+m.nameAndType);
		addMethodAt(m,j);
	    }
	}
	// own methods
	for(int i=0; i<info.methods.length; i++) {
	    if (info.methods[i].data.isStatic() || info.methods[i].name.equals("<clinit>"))
		continue;
	    if (dumpAll) Debug.out.println("own:"+info.methods[i].nameAndType);
	    addMethod(info.methods[i]);
	}
	addAll(mt);
    }

    public int getIndex(String nameAndType) {
	Method m = (Method)mfinder.get(nameAndType);
	if (m.ifMethodIndex == 0) {
	    /*System.out.println("  WARNING: Method index=0: "+m.nameAndType);*/
	}
	return m.ifMethodIndex;
    }

    private void addMethod(Method m) {
	int index = itable.contains(m);
	itable.dump();
	if (index == -1) {
	    System.out.println("NF: "+m.nameAndType);
	}
	if (mt.size() <= index) mt.setSize(index+1);
	mt.setElementAt(m, index);
	m.ifMethodIndex = index;
    }

    private void addMethodAt(Method m, int index) {
	itable.dump();
	if (index == -1) {
	    System.out.println("NF: "+m.nameAndType);
	}
	if (mt.size() <= index) mt.setSize(index+1);
	mt.setElementAt(m, index);
	m.ifMethodIndex = index;
    }


}
