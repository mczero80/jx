package jx.compiler.vtable;

import java.util.Vector;
import java.util.Hashtable;
import java.io.DataOutput;
import java.io.IOException;
import jx.compiler.persistent.ExtendedDataOutputStream;
import jx.compiler.persistent.ExtendedDataInputStream;
import jx.compiler.symbols.StringTable;
import jx.zero.Debug;

public class MethodTable {
    public static final int TYPE_CLASS = 1;
    public static final int TYPE_INTERFACE = 2;

    Method [] mtable;
    Hashtable mfinder = new Hashtable();
    
    String classname;

    /*    MethodTable() {
	mtable = new Method[0];
    }
    */
    protected MethodTable(String classname) {
	this.classname = classname;
    }

    public MethodTable(Vector mt, String classname) {
	this.classname = classname;
	addAll(mt);
    }

    void addAll(Vector mt) {
	mtable = new Method[mt.size()];
	// copy into methodtable
	for(int i=0; i<mt.size(); i++) {
	    mtable[i] = (Method)mt.elementAt(i);
	    if (mtable[i] == null) continue;
	    if (mfinder.get(mtable[i].nameAndType) != null) continue; // insert the first index only
	    mfinder.put(mtable[i].nameAndType, mtable[i]);
	}
    }

    public int getIndex(String nameAndType) {
	Method m = (Method)mfinder.get(nameAndType);
	if (m==null) {
	    Debug.throwError("Method "+nameAndType+" not found in methodtable of class "+classname);
	}
	Vector indices = m.indices;
	/*
	if (indices.size() > 1) {
	    for(int i=0; i<indices.size(); i++) {
		System.out.println("I: "+(Integer)indices.elementAt(i));
	    }
	}
	*/
	Integer ind = (Integer)indices.elementAt(0);
	int index = ind.intValue();
	if (index == 0) {
	    /*System.out.println("  WARNING: Method index=0: "+m.nameAndType);*/
	}
	return index;
    }

    
    public int length() {
	return mtable.length;
    }

    public void print() {
	for(int i=0; i<mtable.length; i++) {
	    if (mtable[i]==null) {
		System.out.println("    "+i+":  -");
	    } else {
		System.out.println("    "+i+":"+mtable[i].nameAndType + "   :: "+mtable[i].implementedIn.className);
	    }
	}
    }

    public void registerStrings(StringTable strTable) {
	for(int i=0; i<mtable.length; i++) {
	    if (mtable[i]==null) {
		strTable.register("");
	    } else {
		strTable.register(mtable[i].implementedIn.className);
		strTable.register(mtable[i].name);
		strTable.register(mtable[i].type);
	    }
	}	
    }

    public void serialize(ExtendedDataOutputStream out, StringTable strTable) throws IOException {
	out.writeInt(TYPE_CLASS);
	out.writeInt(mtable.length);
	for(int i=0; i<mtable.length; i++) {
	    if (mtable[i]==null) {		
		//out.writeString("");
		//out.writeString("");
		//out.writeString("");
		strTable.writeStringID(out,"");
		strTable.writeStringID(out,"");
		strTable.writeStringID(out,"");
		out.writeInt(0);
	    } else {
		//out.writeString(mtable[i].implementedIn.className);
		//out.writeString(mtable[i].name);
		//out.writeString(mtable[i].type);
		strTable.writeStringID(out,mtable[i].implementedIn.className);
		strTable.writeStringID(out,mtable[i].name);
		strTable.writeStringID(out,mtable[i].type);
		out.writeInt(mtable[i].ifMethodIndex);
	    }
	}
    }

    public void serialize(ExtendedDataOutputStream out) throws IOException {
	out.writeInt(TYPE_CLASS);
	out.writeInt(mtable.length);
	for(int i=0; i<mtable.length; i++) {
	    if (mtable[i]==null) {		
		out.writeString("");
		out.writeString("");
		out.writeString("");
		out.writeInt(0);
	    } else {
		out.writeString(mtable[i].implementedIn.className);
		out.writeString(mtable[i].name);
		out.writeString(mtable[i].type);
		out.writeInt(mtable[i].ifMethodIndex);
	    }
	}
    }

    public static MethodTable deserialize(ExtendedDataInputStream in, ClassInfo info, Hashtable classPool) throws IOException {
	MethodTable methodTable;
	int type = in.readInt();
	if (type == TYPE_INTERFACE) {
	    methodTable = new InterfaceMethodTable(info.className);
	} else {
	    methodTable = new MethodTable(info.className);
	}
	int len =  in.readInt();
	Method[] mtable = new Method[len];
	methodTable.mtable = mtable;
	for(int i=0; i<len; i++) {
	    String className = in.readString();
	    String methodName = in.readString();
	    String typeName = in.readString();
	    int ifMethodIndex = in.readInt();
	// ask the class object if this method exists, or create it
	    if (className.equals("")) {
		// hole
		mtable[i] = null;
	    } else if (className.equals(info.className)) {
		// a new Method or existing method at new index
		Method existing = (Method) methodTable.mfinder.get(methodName+typeName);
		if (existing == null) {
		    mtable[i] = new Method(info, methodName, typeName);
		} else {
		    mtable[i] = existing;
		}
		mtable[i].ifMethodIndex = ifMethodIndex;
		if (type == TYPE_INTERFACE) {
		} else {
		    mtable[i].indices.addElement(new Integer(i));
		}
	    } else {
		ClassInfo cinf = (ClassInfo)classPool.get(className);
		if (cinf==null) throw new Error("Cannot find class "+className);
		try {
		    mtable[i] = cinf.mtable.getAt(cinf.mtable.getIndex(methodName+typeName));
		} catch(ArrayIndexOutOfBoundsException e) {
		    System.out.println(e.toString());
		    System.out.println("While creating table for class "+info.className);
		    System.out.println("    MTABLE method entry: "+i+": "+className+"::"+methodName+typeName+", ifmethodindex="+ifMethodIndex);
		    System.out.println("    CLASS "+cinf.className+", mtable:"+cinf.mtable);
		    System.out.println("    CLASS mtable:");
		    cinf.mtable.print();
		    System.out.println("    THIS mtable:");
		    methodTable.print();
		    ClassInfo cininf = (ClassInfo)classPool.get("jx/wintv/CommonInterface");
		    System.out.println("    CINFCLASS mtable:");
		    cininf.mtable.print();

		    e.printStackTrace();
		    throw e;
		}
	    }
	    if (mtable[i] != null) {
		if (methodTable.mfinder.get(mtable[i].nameAndType) != null) continue; // insert the first index only
		methodTable.mfinder.put(methodName+typeName, mtable[i]);
	    }
	}
	return methodTable;
    }

    // only package relevant
    Method getAt(int index) {
	return mtable[index];
    }

    void setAt(Method m, int i) {
	mtable[i] = m;
    }


    public String toString() {
	return "MethodTable(size="+mtable.length+")";

    }

}
