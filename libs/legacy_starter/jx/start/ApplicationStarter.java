package jx.start;

import jx.zero.*;
import jx.zero.debug.*;
import java.io.*;

public class ApplicationStarter {

    static class MyOutputStream extends java.io.OutputStream {	
	public void write(int b) throws IOException {
	    Debug.out.print(""+(char)b);
	}	
    }
    
    public static void init (final Naming naming, String []args) {

	Debug.out = new jx.zero.debug.DebugPrintStream(new jx.zero.debug.DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	/*System.out = new PrintStream(new MyOutputStream());
	System.err = System.out;
	*/
	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");

	String progName = args[0];
	String libName = args[1];
	String[] pureargs = new String[args.length-2];
	cpuManager.setThreadName(progName);

	System.arraycopy(args, 2, pureargs, 0, pureargs.length);

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
