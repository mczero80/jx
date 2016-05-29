package test.portal;

import jx.zero.*;
import jx.zero.debug.*;

import jx.zero.scheduler.*;
import jx.scheduler.*;

public class ServiceHLS implements HLScheduler_runnables, HLS_switchTo, HLS_portalCalled,  HLS_serviceCalled,  HLS_blockedInService, HLS_GCThread {
//public class HLRRobin implements jx.zero.scheduler.HLScheduler_all {
     
    Naming naming;
    CPUManager cpuManager;
    SMPCPUManager SMPcpuManager;
    HLSchedulerSupport HLschedulerSupport;
    DebugPrintStream out;
  
    protected ThreadList runnables=null;

    public String DomainName="";   //test
    private static int instNr = 0; //test
    private boolean debug=true;   //test

    CPUState dummy=null;

    private boolean uninterruptable = false;
    private CPUState unINTthread=null;
    

    public ServiceHLS() {
	naming= InitialNaming.getInitialNaming();
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
	Debug.out.println("ServiceHLS registered called on CPU"+SMPcpuManager.getMyCPU());
//	MyCPU = SMPcpuManager.getMyCPU();

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

    public void interrupted(CPUState newThread)  {
//	Debug.out.println(DomainName+": ServiceHLS::interrupted called");
	runnables.add(newThread);
    }

    public void preempted(CPUState newThread)  {
//	Debug.out.println(DomainName+": ServiceHLS::preempted called");
	 if (uninterruptable) {
	     if (unINTthread != null)
		  throw new Error ("schould never happen");
	    unINTthread = newThread;
	 }
	else
	    runnables.add(newThread);
    }
 
    public void yielded(CPUState newThread)  {
//	Debug.out.println(DomainName+": ServiceHLS::yielded called");
	runnables.add(newThread);
    }
    public void unblocked(CPUState newThread)  {
//	Debug.out.println(DomainName+": ServiceHLS::unblocked called");
	runnables.add(newThread);
    }
    
    public void created(CPUState newThread)  {
	Debug.out.println(DomainName+": ServiceHLS::created called");
//	dummy = newThread;
	runnables.add(newThread);
    }

    public void switchedTo(CPUState Thread){
	Debug.out.println(DomainName+": ServiceHLS::switchedTo called");
	HLschedulerSupport.activateThread(Thread);
	throw new Error("should never return!");
    }

    public void blockedInPortalCall(CPUState Thread){
	Debug.out.println(DomainName+": ServiceHLS::blockedInPortalCall");
    }
    
    public boolean portalCalled(CPUState Thread){
	Debug.out.println(DomainName+": ServiceHLS::portalCalled called");
	return true;  // hand-off
	//return false;
    } 

    public void blockedInService(CPUState Thread, CPUStateLink  PortalThread){
	Debug.out.println(DomainName+": ServiceHLS::blockedInService");
     }
 
    public boolean serviceCalled(CPUState Thread, CPUStateLink PortalThread){
	Debug.out.println(DomainName+": ServiceHLS::serviceCalled");
	return true;  // hand-off
	//return false;
    }

    public void startGCThread (CPUState interruptedThread, CPUState GCThread) {
	//Debug.out.println(DomainName+": ServiceHLS::startGCThread called");
	if (interruptedThread != null)
	     runnables.add(interruptedThread);
	uninterruptable = true;
	unINTthread = null;
	HLschedulerSupport.activateThread(GCThread);
    }
   public void unblockedGCThread (CPUState GCThread){
       //Debug.out.println(DomainName+": ServiceHLS::unblockedGCThread called");
      if (uninterruptable)
	   throw new Error("should not happen!");  
      unINTthread= GCThread;
      uninterruptable = true;
   }
     public void destroyedGCThread (CPUState GCThread){
	//Debug.out.println(DomainName+": ServiceHLS::destroyedGCThread called");
	uninterruptable = false;
	unINTthread = null;
     }
     
     public void activated()	  {
	  //Debug.out.println(toString()+"::activated called");

	 if (uninterruptable)
	     HLschedulerSupport.activateThread(unINTthread);

	  CPUState t = (CPUState)runnables.removeFirst();
	  if (t == null) {  // runQ empty
	      Debug.out.println(DomainName+": ServiceHLS: yield!");
	      HLschedulerSupport.clearMyRunnableFlag();  
	      HLschedulerSupport.yield();
	      throw new Error("should never return! (A)");
	  }
	  
	  HLschedulerSupport.activateThread(t);

	  throw new Error("should never return! (C)");
   }
     
     public String toString() {
	  return "ServiceHLS";
     }

     public void dump() {
	 Debug.out.println(DomainName+": ServiceHLS::dump called");
    }

}

