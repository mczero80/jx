package jx.start;

import jx.zero.*;
import jx.zero.debug.*;
import java.io.*;
import jx.streams.*;

import jx.wm.*;
import jx.wm.message.*;
import jx.devices.fb.*;

public class ApplicationStarter {
    public static void init (final Naming naming, String []args) {
	jx.zero.debug.DebugOutputStream out = new jx.zero.debug.DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0"));
	Debug.out = new jx.zero.debug.DebugPrintStream(out);
	System.out = new java.io.PrintStream(out);
	System.err = System.out;

	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");

	String progName = args[1];
	String libName = args[2];
	String[] pureargs = new String[args.length-2-1];
	cpuManager.setThreadName(progName);

	System.arraycopy(args, 2+1, pureargs, 0, pureargs.length);

	ComponentManager componentManager = (ComponentManager) naming.lookup("ComponentManager");
	int componentID = componentManager.load(libName);
	cpuManager.executeClassConstructors(componentID);

	VMClass cl = cpuManager.getClass(progName);
	if (cl == null) {
	    throw new Error("Class "+progName+" not found.");
	}
	VMMethod[] methods = cl.getMethods();

	String name = "main";
	String signature = "([Ljava/lang/String;)V";

	for(int i=0; i<methods.length; i++) {
	    //Debug.out.println("M: "+methods[i].getName() + ", S: "+ methods[i].getSignature());
	    if (name.equals(methods[i].getName()) && signature.equals(methods[i].getSignature())) {
		methods[i].invoke(null, new Object[] { pureargs });		
		return;
	    }
	}
	throw new Error("Method "+name + signature + " not found in class "+progName);
    }
}
