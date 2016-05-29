package jx.fsshell;

import jx.streams.*;
import java.io.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.timer.TimerManager;

public class FSShell {
    class OutputStreamProxy extends OutputStream {
	OutputStreamPortal o;
	OutputStreamProxy(OutputStreamPortal o) {
	    this.o = o;
	}
	public void write(int c) throws IOException {
	    o.write(c);
	}
	
    }

    public static void main(String[] args) {
	new FSShell(args);
    }
    public FSShell(String[] args) {
	Naming naming = InitialNaming.getInitialNaming();
	StreamProvider streamProvider = (StreamProvider)LookupHelper.waitUntilPortalAvailable(naming, args[0]);	
	OutputStreamPortal o = streamProvider.getOutputStream();
	PrintStream out = new PrintStream(new OutputStreamProxy(o));
	Debug.out = new DebugPrintStream(new OutputStreamProxy(o));
	out.println("Hello World!");
	Debug.out.println("Debug-Hello World!");
	for(int j=0; j<8; j++) {
	    for(int i=0; i<10; i++) {
		Debug.out.print(i);
	    }
	}
	Debug.out.println();
	TimerManager timerManager = (TimerManager)LookupHelper.waitUntilPortalAvailable(naming, args[1]);
	CPUManager cpuManager = (CPUManager)LookupHelper.waitUntilPortalAvailable(naming, "CPUManager");
	for(int i=0;;i++) {
	    Debug.out.println("Hello "+i);
	    timerManager.unblockInMillis(cpuManager.getCPUState(), 1000);
	    cpuManager.block();
	}
    }
}
