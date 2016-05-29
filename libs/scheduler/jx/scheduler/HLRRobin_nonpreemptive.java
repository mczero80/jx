package jx.scheduler;

import jx.zero.*;
import jx.zero.debug.*;

import jx.zero.scheduler.*;

import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

public class HLRRobin_nonpreemptive implements HLScheduler_runnables, HLS_switchTo, HLS_serviceCalled, HLS_blockedInService, HLS_GCThread {
//public class HLRRobin implements jx.zero.scheduler.HLScheduler_all {
     
    Naming domainZero;
    CPUManager cpuManager;
    SMPCPUManager SMPcpuManager;
    HLSchedulerSupport HLschedulerSupport;
    DebugPrintStream out;
  
    protected ThreadList runnables=null;
    protected ThreadList portalThreads=null;
    private CPU MyCPU = null;
    public CPUState active = null;
    public CPUState INTbyGC = null;

  public String DomainName="";   //test
  private boolean debug=false;   //test

    public HLRRobin_nonpreemptive() {
	domainZero= InitialNaming.getInitialNaming();
	  cpuManager = (CPUManager) domainZero.lookup("CPUManager");
	  SMPcpuManager = (SMPCPUManager) domainZero.lookup("SMPCPUManager");
	  HLschedulerSupport = (HLSchedulerSupport) domainZero.lookup("HLSchedulerSupport");
	  /* init Debug.out */
	  DebugChannel d = (DebugChannel) domainZero.lookup("DebugChannel0");
	  out = new DebugPrintStream(new DebugOutputStream(d));
	  Debug.out = out;
	  runnables = new ThreadList();
	  portalThreads = new ThreadList();
   }

    public void registered(){
	Debug.out.println("HLRRobin_nonpreemptive registered called on CPU"+SMPcpuManager.getMyCPU());
	MyCPU = SMPcpuManager.getMyCPU();
	
	active = null;  // not necessary!!?
	INTbyGC = null;
   }

    public boolean Scheduler_interrupted(){
	return false;
    }

    public boolean Scheduler_preempted(){
	return false;
    }

    public void interrupted(CPUState Thread)  {
	throw new Error("should never be called!");
    }
 
   public void preempted(CPUState Thread)  {
                //Debug.out.println("\n"+DomainName+toString()+"::preempted called");
	if (active == null)
	    active = Thread;
	else 
	    throw new Error("should never happen!");
	
    }
 
    public void yielded(CPUState Thread)  {
	Debug.out.println(DomainName+toString()+"::yielded called");
	runnables.add(Thread);
    }
    public void unblocked(CPUState Thread)  {
	Debug.out.println(DomainName+toString()+"::unblocked called");
	runnables.add(Thread);
    }
    
    public void created(CPUState Thread)  {
	Debug.out.println(DomainName+toString()+"::created called");
	runnables.add(Thread);
    }

    public void switchedTo(CPUState Thread){
	Debug.out.println(DomainName+toString()+"::switchedTo called");
	if (Thread != active) {
	    // do not switch!!
	    if (Thread.isPortalThread()) {
	       runnables.remove(Thread); 
	       portalThreads.add(Thread);
	    }
	    else
	      runnables.add(Thread);
	}
	activated();
	throw new Error("should never return!");
    }

    public void blockedInService (CPUState Thread, CPUStateLink PortalThread){
	Debug.out.println(DomainName+": HLRRobin_nonpreemptive::blockedInService called");
    }
    public boolean serviceCalled(CPUState Thread, CPUStateLink PortalThread) {
	Debug.out.println(DomainName+": HLRRobin_nonpreemptive::serviceCalled called");
	return true;  // hand-off
	//return false;
    }

    public void startGCThread (CPUState interruptedThread, CPUState GCThread) {
	Debug.out.println(DomainName+": HLRRobin_nonpreemptive::startGCThread called");
	if (active != null)
		throw new Error("schould not happen");
	INTbyGC=interruptedThread; // maybe null

	HLschedulerSupport.activateThread(GCThread);
    }
   public void unblockedGCThread (CPUState GCThread){
      Debug.out.println(DomainName+": HLRRobin_nonpreemptive::unblockedGCThread called");
      INTbyGC=active;
      active = GCThread;
    }
    public void destroyedGCThread (CPUState GCThread){
	//Debug.out.println(DomainName+": HLRRobin_nonpreemptive::destroyedGCThread called");
	active = INTbyGC;
    }
    
     public void activated()	  {
	 Debug.out.println(DomainName+toString()+"::activated called");
	 CPUState tmp;
	 if (active != null) {
	 Debug.out.println("1");
	     tmp = active;
	     active = null;
	 } else {
	 Debug.out.println("2");
	     tmp = (CPUState)portalThreads.removeFirst();
	     if (tmp == null) { 
		 tmp = (CPUState)runnables.removeFirst();
		 if (tmp == null) {  /* runQ empty*/
		     /*Debug.out.println("runQ empty ->yield!?");*/
		     HLschedulerSupport.clearMyRunnableFlag();
		     HLschedulerSupport.yield();
		     throw new Error("should never return!");
		 }
	     }
	 }
	 HLschedulerSupport.activateThread(tmp);
	 throw new Error("should never return!");
     }
     
      public String toString() {
	  return " HLRRobin_nonpreemptive";
     }

    public void dump() {
	  runnables.dump();
	  if (active != null) {
	      Debug.out.println("activ:");
	      HLschedulerSupport.dumpThread(active);
	  }
	  Debug.out.println("  Portalthreads:");
	 portalThreads.dump();
    }

}

