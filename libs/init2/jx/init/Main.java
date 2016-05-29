package jx.init;

import jx.zero.*;
import jx.zero.debug.*;

/*
import jx.zero.scheduler.HighLevelScheduler;
import jx.scheduler.*;
*/

import jx.bootrc.*;

public class Main {
     private final static boolean debug = false;      
     private final static boolean javascheduler = true;      
     private static Naming initNaming;

     public static void init(Naming naming) throws Exception {
	jx.zero.debug.DebugOutputStream out = new jx.zero.debug.DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0"));
	Debug.out = new jx.zero.debug.DebugPrintStream(out);
	System.out = new java.io.PrintStream(out);
	System.err = System.out;
	 
	 Debug.out.println("Init running...");
	
	main(new String[] {"boot.rc"});
     }
     public static void main(String args[]) throws Exception {
	 Naming naming = InitialNaming.getInitialNaming();
	 initNaming = naming;
	 String filename=args[0];
	BootFS bootFS = (BootFS) naming.lookup("BootFS");
	if (bootFS == null) {
	    Debug.out.println("****************************");
	    Debug.out.println("*  NO BootFS portal found. *");
	    Debug.out.println("****************************");
	    throw new Exception();
	}
	ReadOnlyMemory startupScript = bootFS.getFile(filename);
	if (startupScript == null) throw new Error("no startup script "+filename);
	 
	BootRC2 p = new BootRC2(startupScript);
	
	// global configuration
	installGlobal(p.getGlobalSpec());


	// start domains
	DomainSpec domainSpec;
	while((domainSpec = p.nextDomainSpec()) != null) {
	    // required parameters
	    int garbageCollector = 0;
	    String domainName = domainSpec.getString("Name");
	    int codeSize = domainSpec.getInt("CodeSize");
	    try { domainSpec.getString("SchedulerClass"); } catch(NameNotFoundException e) {}
	    try { 
		String gcName = domainSpec.getString("GarbageCollector"); 
		if (gcName.equals("copying")) {
		    garbageCollector = 0;
		} else if (gcName.equals("compacting")) {
		    garbageCollector = 1;
		} else if (gcName.equals("bitmap")) {
		    garbageCollector = 2;
		} else if (gcName.equals("chunked")) {
		    garbageCollector = 3;
		} else {
		    throw new Error("unknown GC implementation");
		}
	    } catch(NameNotFoundException e) {}

	    int gcinfo0=0;
	    int gcinfo1=0;
	    int gcinfo2=0;
	    String gcinfo3=null;
	    int gcinfo4=-1;

	    switch(garbageCollector) {
	    case 0:
	    case 1:
	    case 2:
		gcinfo0 = domainSpec.getInt("HeapSize");
		break;
	    case 3:
		gcinfo0 = domainSpec.getInt("GCHeapInitial");
		gcinfo1 = domainSpec.getInt("GCHeapChunk");
		gcinfo2 = domainSpec.getInt("GCStartGC");
		try { gcinfo3 = domainSpec.getString("GCAccountTo"); } catch(NameNotFoundException e) {}
		try { gcinfo4 = domainSpec.getInt("GCLimit"); } catch(NameNotFoundException e) {}
		break;
	    default:
		    throw new Error("unknown GC implementation");		
	    }

	    ComponentSpec[] componentSpec = domainSpec.getComponents();

	    //if (componentSpec.length == 1) {
	    /*
		String initLib = componentSpec[0].getString("InitLib");
		String startClass = componentSpec[0].getString("StartClass");
		Debug.out.println("Start "+domainName);
		// optional parameters
		String[] argv = null;
		String schedulerClass = null;
		try { argv = componentSpec[0].getStringArray("Args"); } catch(NameNotFoundException e) {}
		DomainStarter.createDomain(domainName, initLib, startClass, schedulerClass, gcinfo0, codeSize, argv);
	    */
		//} else {
		// multi component domain
		String initLib = "init2.jll";
		String startClass = "jx/init/MultiComponentStart";
		DomainStarter.createDomain(domainName, initLib, startClass, gcinfo0, gcinfo1, gcinfo2, gcinfo3, gcinfo4, codeSize, initNaming, garbageCollector, new Object[]{componentSpec});
		//}
	}
	Debug.out.println("Init finished.");
    }


    static private void installGlobal(GlobalSpec globalSpec) {
	if (globalSpec == null) {
	    Debug.out.println("!!ATTENTION!!                                      !!ATTENTION!!");
	    Debug.out.println("!!ATTENTION!!  No [Global] section found           !!ATTENTION!!");
	    Debug.out.println("!!ATTENTION!!                                      !!ATTENTION!!");
	    return;
	}
	try {	
	    Naming naming = InitialNaming.getInitialNaming();
	    String secLib = globalSpec.getString("SecurityManagerLib");
	    String secClass = globalSpec.getString("SecurityManagerClass");

	    final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	    final ComponentManager componentManager = (ComponentManager) naming.lookup("ComponentManager");
	    
	    int componentID = componentManager.load(secLib);
	    cpuManager.executeClassConstructors(componentID);
	    VMClass cl = cpuManager.getClass(secClass);
	    if (cl == null) {
		throw new Error("SecurityManager class "+secClass+" not found.");
	    }
	    cl.newInstance();
	} catch(NameNotFoundException e) {
	    Debug.out.println("!!ATTENTION!!                                      !!ATTENTION!!");
	    Debug.out.println("!!ATTENTION!!  No Security Manager is used         !!ATTENTION!!");
	    Debug.out.println("!!ATTENTION!!                                      !!ATTENTION!!");
	}

	try {
	    String namingClass = globalSpec.getString("InstallNaming");
	    initNaming = new InitNaming(initNaming);
	} catch(NameNotFoundException e) {
	    Debug.out.println("!!ATTENTION!!                                      !!ATTENTION!!");
	    Debug.out.println("!!ATTENTION!!  DomainZero's naming service is used !!ATTENTION!!");
	    Debug.out.println("!!ATTENTION!!                                      !!ATTENTION!!");
	}

    }

    /*
    static private void install_LLS(Naming naming){
	Debug.out.println("installing LowLevel-Scheduler:");
	SMPCPUManager SMPcpuManager = (SMPCPUManager)LookupHelper.waitUntilPortalAvailable(naming,"SMPCPUManager");
	// install a LowLevelScheduler for this CPU
	LowLevelScheduler LLS = new LLRRobin();
	SMPcpuManager.register_LLScheduler(SMPcpuManager.getMyCPU(), LLS);
	Debug.out.println("CPU"+SMPcpuManager.getMyCPU().getID()+": LowLevel-Scheduler created.");
	
	
	//install a HighLevelScheduler for this CPU
	//HighLevelScheduler HLS = new HLRRobin();
	//      register_HLScheduler(domainInit->cpu[cpu_ID], domainInit, domainInit, Scheduler);
	//Debug.out.println("CPU"+SMPcpuManager.getMyCPU().getID()+": HighLevel-Scheduler created for Domain Init.");
	
    }
    */
}
