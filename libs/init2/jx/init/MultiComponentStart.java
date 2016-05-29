package jx.init;

import jx.zero.*;
import jx.zero.debug.*;
import java.io.*;
import java.util.Vector;
import jx.bootrc.*;

public class MultiComponentStart {

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
    
    public static void init (final Naming naming, String []args, Object[] objectArgs) throws Exception {
	jx.zero.debug.DebugOutputStream out = new jx.zero.debug.DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0"));
	Debug.out = new jx.zero.debug.DebugPrintStream(out);
	System.out = new java.io.PrintStream(out);
	System.err = System.out;

	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	final ComponentManager componentManager = (ComponentManager) naming.lookup("ComponentManager");

	ComponentSpec[] componentSpec = (ComponentSpec[]) objectArgs[0];
	Vector start = new Vector();

	for(int i=0; i<componentSpec.length; i++) {
	    String initLib = componentSpec[i].getString("InitLib");
	    String startClass = componentSpec[i].getString("StartClass");
	    // optional parameters
	    String[] argv = new String[]{};
	    String schedulerClass = null;
	    try { argv = componentSpec[i].getStringArray("Args"); } catch(NameNotFoundException e) {}


	    StartInfo info = new StartInfo();
	    info.name = startClass;
	    info.args=new Object[]{argv};

	    int componentID = componentManager.load(initLib);
	    
	    try {
		String[] cname = componentSpec[i].getStringArray("InheritThread"); 
		for(int j=0; j<cname.length; j++) {
		    componentManager.setInheritThread(cname[j]);
		}
	    } catch(NameNotFoundException e) {}
	    

	    cpuManager.executeClassConstructors(componentID);
	    
	    VMClass cl = cpuManager.getClass(startClass);
	    if (cl == null) {
		throw new Error("Class "+startClass+" not found.");
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
	    if (info.method==null) throw new Error("Method "+name + signature + " not found in class "+startClass);
	    
	    start.addElement(info);
	}


	for(int i=0; i<start.size(); i++) {
	    final StartInfo info = (StartInfo)start.elementAt(i);
	    cpuManager.start(cpuManager.createCPUState(new ThreadEntry() {
		    public void run() {
			Debug.out.println("START : "+info.name);
			cpuManager.setThreadName(info.name);
			info.method.invoke(null, info.args);
		    }
		}));
	}
    }
}
