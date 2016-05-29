package jx.compiler; 

import jx.zero.Memory;
import jx.zero.MemoryManager;
import jx.compiler.MemoryClassSource;
import jx.classfile.ClassSource;
import jx.classfile.NoMagicNumberException;
import jx.zip.ZipFile;
import jx.zip.ZipEntry;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;
import java.io.EOFException;

/*
 * Represents all classes that are contained in a ZIP file
 */
public class ZipClasses {
    Hashtable classes = new Hashtable();
    public ZipClasses(Memory m, boolean allowNative) throws Exception {

	// tokenize zipfile into classfiles
	Vector libdata = new Vector();
	ZipFile zip = new ZipFile(m);
	ZipEntry entry = null;
	while ((entry = zip.getNextEntry()) != null) {
	    if (entry.isDirectory()) {
		continue;
	    }
	    libdata.addElement(entry.getData());
	}

	// parse classfiles into classes
	for(int i=0; i<libdata.size(); i++) {
	    try {
		ClassSource classData = new MemoryClassSource((Memory)libdata.elementAt(i), allowNative); 
		String className = classData.getClassName();
		classes.put(className, classData);
	    } catch(EOFException ex) {
	    } catch(NoMagicNumberException ex) {
	    }
	}
    }
    public ClassSource findclass(String className) {
	return (ClassSource)classes.get(className);
    }
    public Enumeration elements() {
	return classes.elements();
    }
}
