package jx.disass;

import jx.zero.Debug;
import java.util.Vector;
import java.io.*;
import jx.jit.persistent.*;

public class ObjDump {
    public static void main(String [] args) throws Exception {
	/*
    	if (args.length != 1) {
	    System.out.println("Usage: ");
	    System.out.println("    ObjDump <codefilename> ");
	    return;
	}
	ExtendedDataInputStream stream = new ExtendedDataInputStream(new FileInputStream(args[0]));
	CodeFile file = new CodeFile();
	Vector allClasses = file.read(stream);
	for(int i=0; i<allClasses.size(); i++) {
	    CompiledClass compiledClass = (CompiledClass) allClasses.elementAt(i);
	    System.out.println("Class: "+compiledClass.getName());
	    CompiledMethod[] allMethods = compiledClass.getMethods();
	    for(int j=0; j<allMethods.length; j++) {
		CompiledMethod compiledMethod = allMethods[j];
		if (compiledMethod.getCode() == null) {
		    System.out.print("  Size:  - ");
		} else {
		    System.out.print("  Size: "+compiledMethod.getCode().length);
		}
		System.out.println(" Method: "+compiledMethod.getName());
		
		}
	    }
	*/
	}
}

