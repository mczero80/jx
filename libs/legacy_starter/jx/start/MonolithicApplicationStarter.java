package jx.start;

import jx.zero.*;
import jx.zero.debug.*;
import java.io.*;
import java.util.Vector;

public class MonolithicApplicationStarter {

    static class StartInfo {
	VMMethod method;
	String name;
	Object [] args;
    }

    static class MyOutputStream extends java.io.OutputStream {	
	public void write(int b) throws IOException {
	    Debug.out.print(""+(char)b);
	}	
    }
    
    public static void init (final Naming naming, String []args) {
	
	Debug.out = new jx.zero.debug.DebugPrintStream(new jx.zero.debug.DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	/*	System.out = new PrintStream(new MyOutputStream());
	System.err = System.out;
	*/
	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");

	Vector start = new Vector();

	for(int i=0; i<args.length; i++) {
	    StartInfo info = new StartInfo();
	    final String progName = args[i];
	    final String libName = args[i+1];
	    //info.name = "domaininit "+progName+" "+libName;
	    info.name = progName;

	    Debug.out.println("Starting "+progName+" in lib "+libName);
	    int l=0;
	    int j;
	    for(j=i+2; j<args.length; j++) {
		if(args[j] == null) {
		    break;
		}
	    }
	    if (j==args.length) throw new Error("no null found");

	    final String[] pureargs = new String[j-i-2];

	    System.arraycopy(args, i+2, pureargs, 0, pureargs.length);

	    info.args = new Object[] { pureargs };

	    i=j;

	    ComponentManager componentManager = (ComponentManager) naming.lookup("ComponentManager");
	    int componentID = componentManager.load(libName);
	    cpuManager.executeClassConstructors(componentID);
	    
	    VMClass cl = cpuManager.getClass(progName);
	    if (cl == null) {
		throw new Error("Class "+progName+" not found.");
	    }
	    final VMMethod[] methods = cl.getMethods();


	    String name = "main";
	    String signature = "([Ljava/lang/String;)V";
	    for(int k=0; k<methods.length; k++) {
		//Debug.out.println("M: "+methods[i].getName() + ", S: "+ methods[k].getSignature());
		if (name.equals(methods[k].getName()) && signature.equals(methods[k].getSignature())) {
		    info.method = methods[k];
		    break;
		}
	    }
	    if (info.method==null) throw new Error("Method "+name + signature + " not found in class "+progName);
	    
	    start.addElement(info);
	}


	for(int i=0; i<start.size(); i++) {
	    final StartInfo info = (StartInfo)start.elementAt(i);
	    cpuManager.start(cpuManager.createCPUState(new ThreadEntry() {
		    public void run() {
			cpuManager.setThreadName(info.name);
			info.method.invoke(null, info.args);
		    }
		}));
	}
    }
}
