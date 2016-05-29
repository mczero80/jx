package jx.compiler.vtable;

import jx.classfile.MethodSource;
import java.util.Vector;
import jx.zero.Debug;

public class Method {
    public String name;
    public String type;
    public String nameAndType;
    public Vector indices=new Vector(); // method can appear more than once in a methodtable
    public int ifMethodIndex; // index of method in itable, if this method is part of an interface
    public MethodSource data;
    public ClassInfo implementedIn;

    public Method(ClassInfo cl, MethodSource m) {
	implementedIn = cl;
	data = m;
	name = m.getMethodName();
	type = m.getMethodType();
	nameAndType = name+type;
    }


    /** 
     * new method
     */
    public Method(ClassInfo implementedIn, String name, String type) {
	if (implementedIn == null) Debug.throwError("Each method must be implemented in some class.");
	this.implementedIn = implementedIn;
	this.name = name;
	this.type = type;
	this.nameAndType = name + type;
    }
}
