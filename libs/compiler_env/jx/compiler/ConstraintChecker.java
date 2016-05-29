package jx.compiler;

import jx.classfile.ClassSource;
import jx.classfile.MethodSource;
import jx.collections.Iterator;
import jx.compiler.persistent.ExtendedDataOutputStream;
import jx.compiler.persistent.ExtendedDataInputStream;

import java.util.Hashtable;
import java.util.Vector;
import java.io.IOException;

import jx.zero.*;

class ForbiddenInterfaceImplementedException extends Exception {
    public ForbiddenInterfaceImplementedException() {super();}
    public ForbiddenInterfaceImplementedException(String s) {super(s);}
}

public class ConstraintChecker {
    
    final static String[] forbiddenInterfaces = {
	"jx/zero/AtomicVariable",
	"jx/zero/IRQ",
	"jx/zero/Mutex",
	"jx/zero/BootFS",
	"jx/zero/ChildDomain",
	"jx/zero/DeviceMemory",
	"jx/zero/Ports",
	"jx/zero/Domain",
	"jx/zero/Profiler",
	"jx/zero/CPU",
	"jx/zero/ReadOnlyMemory",
	"jx/zero/CPUInfo",
	"jx/zero/DomainZero",
	"jx/zero/CPUState",
	"jx/zero/InitialNaming",
	"jx/zero/VMClass",
	"jx/zero/VMObject",
	"jx/zero/Credential",
	"jx/zero/Memory",
	"jx/zero/MemoryManager",
	"jx/zero/InterceptOutboundInfo",
	"jx/zero/InterceptInboundInfo",
	"jx/zero/CAS",
    };

    public ConstraintChecker(Iterator classFactory) throws Exception {
	Hashtable classFinder = new Hashtable();
	Vector all = new Vector();	
	while(classFactory.hasNext()) {
	    ClassSource source = (ClassSource)classFactory.next();
	    //Debug.out.println(source.getClassName());
	    if (source.getClassName().startsWith("jx/zero/")) {
		// TODO: use a different way to find out whether this is lib zero
		return;
	    }
	    String[] ifs = source.getInterfaceNames();
	    for(int i=0; i<ifs.length; i++) {
		for(int j=0; j<forbiddenInterfaces.length; j++) {
		    if (ifs[i].equals(forbiddenInterfaces[j]))
			throw new ForbiddenInterfaceImplementedException("Class "+source.getClassName()+" is not allowed to implement "+ifs[i]);
		}
	    }
	}
    }
}
