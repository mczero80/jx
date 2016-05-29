package jx.init;

import jx.zero.*;
import jx.zero.debug.*;


import jx.bootrc.*;
import java.util.Hashtable;
import java.util.Enumeration;

public class InitNaming implements Naming {
    Naming baseNaming;
    Hashtable names = new Hashtable();
    InitNaming(Naming baseNaming) {
	this.baseNaming = baseNaming;

	// enable debugging
	DebugSupport debugSupport = (DebugSupport)baseNaming.lookup("DebugSupport");
	debugSupport.registerMonitorCommand("initns", new MonitorCommand() {
		public void execCommand(String[] args) {
		    Enumeration n = names.keys();
		    for(; n.hasMoreElements();) {
			String name = (String)n.nextElement();
			Debug.out.println(name);
		    }
		}
		public String getHelp() {
		    return "Dump contents of DomainInit name server";
		}
	    });

	// copy well known portals of domainzero to our hashtable
	add("CPUManager");
	add("DebugSupport");
	add("Clock");
	add("DebugChannel0");
	add("DebugSupport");
	add("HLSchedulerSupport");
	add("LLSchedulerSupport");
	add("JAVASchedulerSupport");
	add("SMPCPUManager");
	add("MemoryManager");
	add("DomainManager");
	add("ComponentManager");
	add("BootFS");
	add("Ports");
	add("Profiler");
	add("IRQ");
	add("NetEmulation");
	add("FBEmulation");
	add("DiskEmulation");
	add("TimerEmulation");
    }

    public void registerPortal(Portal portal, String name) {
	//Debug.out.println("Register: "+name);
	names.put(name, portal);
    }
    public Portal lookup(String name) {
	//Debug.out.println("Lookup: "+name);
	return (Portal) names.get(name);
    }
    public Portal lookupOrWait(String depName) {throw new Error();}

    private void add(String name) {
	Portal p = baseNaming.lookup(name);
	if (p==null) return;
	names.put(name, p);
    }

    private void serviceFinalizer() {
	Debug.out.println("*****  InitNaming: THIS SERVICE TERMINATES NOW ***");
    }

}


