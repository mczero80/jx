package jx.compiler.vtable;

import jx.classfile.ClassSource;
import jx.classfile.MethodSource;
import jx.compiler.persistent.ExtendedDataOutputStream;
import jx.compiler.persistent.ExtendedDataInputStream;

import java.util.Vector;
import java.util.Hashtable;
import java.io.IOException;

public class ClassInfo {
    String className;
    String superClassName;
    ClassInfo superClass;
    MethodTable mtable;

    transient Method [] methods;
    transient ClassSource data;
    transient int indexInAll;
    boolean isInterface;
    boolean isOld;

    ClassInfo() {}

    ClassInfo(ExtendedDataInputStream in, Hashtable classPool) throws IOException {
	deserialize(in, classPool);
    }

    public ClassInfo(String className, String superClassName, boolean isInterface) {
	this.className = className;
	this.superClassName = superClassName;
	this.isInterface = isInterface;
	mtable = new MethodTable(className);
    }

    public void addAll(Vector mt) {
	mtable.addAll(mt);
    }

    void adjustSuperClass(Hashtable classPool) {
	if (superClassName.equals("")) superClass = null;
	else superClass = (ClassInfo)classPool.get(superClassName);
    }

    public boolean isInterface() {
	return isInterface;
    }

    public void serialize(ExtendedDataOutputStream out) throws IOException {
	out.writeString(className);
	if (superClass == null) out.writeString("");
	else out.writeString(superClass.className);
	if (isInterface) out.writeInt(1); else out.writeInt(0);
	mtable.serialize(out);
    }

    public void deserialize(ExtendedDataInputStream in, Hashtable classPool) throws IOException {
	className = in.readString();
	/*System.out.println("Deserialize class "+className);*/
	String superClassName = in.readString();
	if (superClassName.equals("")) superClass = null;
	else superClass = (ClassInfo)classPool.get(superClassName);
	isInterface = (in.readInt()==1);
	mtable = MethodTable.deserialize(in, this, classPool);
    }

}
