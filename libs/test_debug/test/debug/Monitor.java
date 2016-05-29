package test.debug;

import jx.zero.*;
import jx.zero.debug.*;

public class Monitor {
    public static void init(Naming naming, String[] args) {
	Debug.out = new jx.zero.debug.DebugPrintStream(new jx.zero.debug.DebugOutputStream((DebugChannel) InitialNaming.getInitialNaming().lookup("DebugChannel0")));
	final DebugSupport debugSupport = (DebugSupport) InitialNaming.getInitialNaming().lookup("DebugSupport");
	debugSupport.registerMonitorCommand("test", new MonitorCommand() {
		public void execCommand(String[] args) {
		    Debug.out.println("***********************************************");
		    Debug.out.println("*             SUCCESS                         *");
		    Debug.out.println("***********************************************");
		}
		public String getHelp() {
		    return "";
		}
	    });
    }
}
