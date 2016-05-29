package jx.disass;

import jx.zero.Debug;
import java.util.Vector;
import jx.compiler.*;
import jx.jit.persistent.*;
import java.io.*;

public class Main {
    public static void main(String [] args) throws Exception {
	if (args.length != 2 && args.length != 3) {
	    System.out.println("Usage: ");
	    System.out.println("    Disassembler <codefilename> <classname> [<methodname>]");
	    return;
	}
	ExtendedDataInputStream stream = new ExtendedDataInputStream(new FileInputStream(args[0]));
	CodeFile file = new CodeFile();
	Vector allClasses = file.read(stream);
	for(int i=0; i<allClasses.size(); i++) {
	    CompiledClass compiledClass = (CompiledClass) allClasses.elementAt(i);
	    //System.out.println("Class: "+compiledClass.getName());
	    if (! compiledClass.getName().equals(args[1])) continue;
	    CompiledMethod[] allMethods = compiledClass.getMethods();
	    if (allMethods == null) {
		System.out.println("  -> no compiled methods");
		continue;
	    }
	    for(int j=0; j<allMethods.length; j++) {
		CompiledMethod compiledMethod = allMethods[j];
		//System.out.println("  Method: "+compiledMethod.getName());
		if (args.length == 2 // print all methods
		    || compiledMethod.getName().equals(args[2])) {
		    byte[] code = compiledMethod.getCode();
		    System.out.println("Assembler code for \""+
				       compiledClass.getName()+"::"+
				       compiledMethod.getName()+
				       compiledMethod.getType()+
				       "\"");
		    Disassembler disass = new Disassembler(code, 0, code.length);
		    disass.disasm(compiledMethod.getLineTable());
		    if (args.length != 2) return; // print only this method
		}
	    }
	    return;
	}
	System.out.println("Method not found");



    }
}
