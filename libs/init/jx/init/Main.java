package jx.init;

import jx.zero.*;
import jx.zero.scheduler.HighLevelScheduler;
import jx.zero.debug.*;

import jx.scheduler.*;

import jx.bootrc.*;

public class Main {
     private final static boolean debug = false;      
     private final static boolean security = false;      
     private final static boolean javascheduler = true;      
     
     public static void init(Naming naming) {
	 DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	 if (d == null) throw new Error ("Portal \"DebugChannel0\" not found");
	 Debug.out = new jx.zero.debug.DebugPrintStream(new jx.zero.debug.DebugOutputStream(d));
	 
	 Debug.out.println("Init running...");
	 
	 if (security) {
	     //CentralSecurityManager secmgr = (CentralSecurityManager)new jx.secmgr.SecMgr(naming);
	 }
	 
	 if (! execStartupScript(naming)) {
	     throw new Error("no startup script boot.rc");
	 }
	 Debug.out.println("Init finished.");
    }

    static boolean execStartupScript(Naming naming) {
	BootFS bootFS = (BootFS) naming.lookup("BootFS");
	if (bootFS == null) {
	    Debug.out.println("****************************");
	    Debug.out.println("*  NO BootFS portal found. *");
	    Debug.out.println("****************************");
	    return false;
	}
	ReadOnlyMemory startupScript = bootFS.getFile("boot.rc");
	if (startupScript == null) return false;
	BootRC p = new BootRC(startupScript);
	// start domains
	Section s = p.getSelectedSection();
	s.reset();
	Record r;
	/* check for JAVA-Shedulers */
	if (javascheduler) {
	    while((r = s.nextRecord()) != null) 
		if (r.schedulerClass != null) {
		    install_LLS(naming);
		    break;
		}
	}
	s.reset();
	while((r = s.nextRecord()) != null) {
	    DomainStarter.createDomain(r.domainName,r.mainLib,r.startClass,r.schedulerClass,r.heapSize,r.argv);
	}
	Debug.out.println("boot.rc done");
	return true;		
    }

    static private void install_LLS(Naming naming){
	Debug.out.println("installing LowLevel-Scheduler:");
	SMPCPUManager SMPcpuManager = (SMPCPUManager)LookupHelper.waitUntilPortalAvailable(naming,"SMPCPUManager");
	// install a LowLevelScheduler for this CPU
	LowLevelScheduler LLS = new LLRRobin();
	SMPcpuManager.register_LLScheduler(SMPcpuManager.getMyCPU(), LLS);
	Debug.out.println("CPU"+SMPcpuManager.getMyCPU().getID()+": LowLevel-Scheduler created.");
	
	/*
	//install a HighLevelScheduler for this CPU
	HighLevelScheduler HLS = new HLRRobin();
	//      register_HLScheduler(domainInit->cpu[cpu_ID], domainInit, domainInit, Scheduler);
	Debug.out.println("CPU"+SMPcpuManager.getMyCPU().getID()+": HighLevel-Scheduler created for Domain Init.");
	*/
    }
}
