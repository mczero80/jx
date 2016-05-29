package jx.compiler.vtable;

import jx.classfile.ClassSource;
import jx.classfile.MethodSource;
import jx.collections.Iterator;
import jx.compiler.persistent.ExtendedDataOutputStream;
import jx.compiler.persistent.ExtendedDataInputStream;

import java.util.Hashtable;
import java.util.Vector;
import java.io.IOException;


public class MethodTableFactory {
    Main main;

    public MethodTableFactory(Iterator classFactory, ClassInfo[] predefinedClasses, ExtendedDataInputStream[] oldTables) throws Exception {
	Hashtable classFinder = new Hashtable();
	Vector all = new Vector();	
	while(classFactory.hasNext()) {
	    ClassSource source = (ClassSource)classFactory.next();
	    ClassInfo info = new ClassInfo();
	    info.data = source;
	    info.isInterface = source.isInterface();
	    info.className = source.getClassName();
	    //System.out.println(info.className); // null pointer exception in jxcore
	    MethodSource [] m =  source.getMethods();
	    info.methods = new Method[m.length];
	    for(int i=0; i<m.length; i++) {
		info.methods[i] = new Method(info, m[i]);
	    }
	    info.indexInAll = all.size();
	    classFinder.put(info.className, info);
	    all.addElement(info);
	}
	main = new Main(classFinder, all, predefinedClasses, oldTables, predefinedClasses[0]);
    }

    /**
     * read methodtables from stream 
     */
    //public MethodTableFactory(ExtendedDataInputStream in) throws Exception {
    //	main = new Main(in);
    //}

    public void process() throws Exception {
	main.process();
    }

    public MethodTable getMethodTable(String className) {
	return main.getMethodTable(className);
    }

    public void serialize(ExtendedDataOutputStream out) throws IOException {
	main.serialize(out);
    }
}
