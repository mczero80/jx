package test.portal;

import jx.zero.*;
import jx.zero.debug.*;

import jx.zero.scheduler.*;
import jx.scheduler.*;

public class CallerHLS implements HLScheduler_runnables, HLS_switchTo, HLS_portalCalled,  HLS_serviceCalled,  HLS_blockedInService, HLS_GCThread {
//public class HLRRobin implements jx.zero.scheduler.HLScheduler_all {
     
    Naming naming;
    CPUManager cpuManager;
    SMPCPUManager SMPcpuManager;
    HLSchedulerSupport HLschedulerSupport;
    DebugPrintStream out;
  
    protected ThreadList runnables=null;
    private CPU MyCPU = null;

    private int anzSchedInterrupted = 0;
    private int anzSchedPreempted = 0;
    private boolean yielded=false;

    public String DomainName="";   //test
    private static int instNr = 0; //test
    private boolean debug=true;   //test
    
     private CPUState GCThread=null;

     CPUState     caller;
     CPUStateLink portal;

    public CallerHLS() {
	naming = InitialNaming.getInitialNaming() ;
	  cpuManager = (CPUManager) naming.lookup("CPUManager");
	  SMPcpuManager = (SMPCPUManager) naming.lookup("SMPCPUManager");
	  HLschedulerSupport = (HLSchedulerSupport) naming.lookup("HLSchedulerSupport");
	  /* init Debug.out */
	  DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	  out = new DebugPrintStream(new DebugOutputStream(d));
	  Debug.out = out;
	  DomainName="Inst:"+(instNr++);
	  runnables = new ThreadList();
   }

    public void registered(){
	Debug.out.println("CallerHLS registered called on CPU"+SMPcpuManager.getMyCPU());
	MyCPU = SMPcpuManager.getMyCPU();

//	int timebase = HLschedulerSupport.getTimeBaseInMicros();
//	int slice = HLschedulerSupport.getDomainTimeslice ();
//	HLschedulerSupport.setMyTimeslice (slice/2);
//	HLschedulerSupport.setMyTimeslice (42);
    }

    public boolean Scheduler_interrupted(){
//	anzSchedInterrupted++;
	return false;
    }

    public boolean Scheduler_preempted(){
//	anzSchedPreempted++;
	return false;
    }

    public void interrupted(CPUState Thread)  {
        Debug.out.println(DomainName+": CallerHLS::interrupted called");
	if (Thread == GCThread)
	    return;
	caller = null;
	portal = null;
	runnables.add(Thread);
    }

    public void preempted(CPUState Thread)  {
	Debug.out.println(DomainName+": CallerHLS::preempted called");
	if (Thread == GCThread)
	    return;
	caller = null;
	portal = null;
	runnables.add(Thread);
    }
 
    public void yielded(CPUState newThread)  {
	Debug.out.println(DomainName+": CallerHLS::yielded called");
	runnables.add(newThread);
    }
    public void unblocked(CPUState Thread)  {
	Debug.out.println(DomainName+": CallerHLS::unblocked called");
	if (Thread == caller)
	    caller = null;
	runnables.add(Thread);
    }
    
    public void created(CPUState newThread)  {
	Debug.out.println(DomainName+": CallerHLS::created called");
	runnables.add(newThread);
    }

    public void switchedTo(CPUState Thread){
      if (debug)
	Debug.out.println(DomainName+": CallerHLS::switchedTo called");
      HLschedulerSupport.activateThread(Thread);
      throw new Error("should never return!");
    }

    public void blockedInPortalCall(CPUState Thread){
	Debug.out.println(DomainName+": CallerHLS::blockedInPortalCall");
	//	DomainZeroLookup.breakpoint();
    }
    
    public boolean portalCalled(CPUState Thread){
	Debug.out.println(DomainName+": CallerHLS::portalCalled called");
	//	DomainZeroLookup.breakpoint();
	return true;  // hand-off
	//return false;
    } 

    public void blockedInService(CPUState Thread, CPUStateLink  PortalThread){
	Debug.out.println(DomainName+": CallerHLS::blockedInService");
     }
 
    public boolean serviceCalled(CPUState Thread, CPUStateLink PortalThread){
	Debug.out.println(DomainName+": CallerHLS::serviceCalled");
	caller = Thread;
	portal = PortalThread;
	return true;  // hand-off
	//return false;
    }
/*    
     public void blocked(CPUState Thread)  {
// 	if (debug)
// 	    Debug.out.println(DomainName+": HLRRobin::blocked called");
     }
*/    
  
     public void startGCThread (CPUState interruptedThread, CPUState GCthread) {
	  Debug.out.println(DomainName+": CallerHLS::startGCThread called");
	  if (this.GCThread != null)
	       throw new Error("should not happen!");  
	  if (interruptedThread != null)
	       runnables.add(interruptedThread);
	  this.GCThread = GCthread;
	  HLschedulerSupport.activateThread(GCthread);
     }
     public void unblockedGCThread (CPUState GCthread){
	  Debug.out.println(DomainName+": CallerHLS::unblockedGCThread called");
	  if (this.GCThread != null)
	       throw new Error("should not happen!");  
	  this.GCThread = GCThread;
     }
     public void destroyedGCThread (CPUState GCthread){
	  //Debug.out.println(DomainName+": CallerHLS::destroyedGCThread called");
	  if (this.GCThread != GCthread)
	       throw new Error("should not happen!");  
	  this.GCThread = null;
     }
     
     public void activated()	  {
	 Debug.out.println(toString()+"::activated called");

	 if (GCThread != null)
	     HLschedulerSupport.activateThread(GCThread);

	 if(caller != null) {
	     Debug.out.println("   activating Portal (caller != null)"); 
	     //	     DomainZeroLookup.breakpoint();
	     HLschedulerSupport.activateService(portal);
	     Debug.out.println("   activateService returned"); 
//	     caller = null;
	 }
	  CPUState t = (CPUState)runnables.removeFirst();
	  if (t == null) {  /* runQ empty*/
	      Debug.out.println(DomainName+": CallerHLS: yield!");
	      HLschedulerSupport.clearMyRunnableFlag();  
	      HLschedulerSupport.yield();
	      throw new Error("should never return! (A)");
	  }
	  
	  HLschedulerSupport.activateThread(t);
	  throw new Error("should never return! (C)");
   }
     
     public String toString() {
	  return "CallerHLS";
     }

     public void dump() {
	  runnables.dump();
    }

}

