package jx.scheduler;

import jx.zero.*;
import jx.zero.debug.*;

import jx.zero.scheduler.*;

import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

public class HLRRobin_mini implements HLScheduler_runnables, HLS_GCThread {
//public class HLRRobin implements jx.zero.scheduler.HLScheduler_all {
     
    Naming domainZero;
    CPUManager cpuManager;
    SMPCPUManager SMPcpuManager;
    HLSchedulerSupport HLschedulerSupport;
    DebugPrintStream out;
  
    protected ThreadList runnables=null;
    private CPU MyCPU = null;

    private boolean uninterruptable = false;
    private CPUState unINTthread=null;

  public String DomainName="";   //test
  private boolean debug=false;   //test

    public HLRRobin_mini() {
	domainZero= InitialNaming.getInitialNaming();
	  cpuManager = (CPUManager) domainZero.lookup("CPUManager");
	  SMPcpuManager = (SMPCPUManager) domainZero.lookup("SMPCPUManager");
	  HLschedulerSupport = (HLSchedulerSupport) domainZero.lookup("HLSchedulerSupport");
	  /* init Debug.out */
	  DebugChannel d = (DebugChannel) domainZero.lookup("DebugChannel0");
	  out = new DebugPrintStream(new DebugOutputStream(d));
	  uninterruptable = false;
	  Debug.out = out;
	  runnables = new ThreadList();
   }

    public void registered(){
	Debug.out.println("HLRRobin registered called on CPU"+SMPcpuManager.getMyCPU());
	MyCPU = SMPcpuManager.getMyCPU();
    }

    public boolean Scheduler_interrupted(){
	return false;
    }

    public boolean Scheduler_preempted(){
	return false;
    }

    public void interrupted(CPUState newThread)  {
	preempted(newThread);
    }

    public void preempted(CPUState Thread)  {
	//Debug.out.println("HLRRobin::preempted called");
	if (uninterruptable == true)
	    unINTthread = Thread;
	else
	    runnables.add(Thread);
    }
 
    public void yielded(CPUState newThread)  {
	//Debug.out.println(DomainName+" "+MyCPU.getID()+":HLRRobin::yielded called");
	runnables.add(newThread);
    }
    public void unblocked(CPUState newThread)  {
	//Debug.out.println(DomainName+": HLRRobin::unblocked called");
	runnables.add(newThread);
    }
    
    public void created(CPUState newThread)  {
	//Debug.out.println("HLRRobin::created called");
	runnables.add(newThread);
    }
 
  public void startGCThread (CPUState interruptedThread, CPUState GCThread) {
     //Debug.out.println(DomainName+": HLRRobin_mini::startGCThread called");
     if (interruptedThread != null)
       runnables.add(interruptedThread);
     uninterruptable = true;
     HLschedulerSupport.activateThread(GCThread);
    }
    public void unblockedGCThread (CPUState GCThread){
      //Debug.out.println(DomainName+": HLRRobin_mini::unblockedGCThread called");
      if (uninterruptable == true)
	throw new Error("should not happen!");  
      unINTthread= GCThread;
      uninterruptable = true;
    }
    public void destroyedGCThread (CPUState GCThread){
     //Debug.out.println(DomainName+": HLRRobin_mini::destroyedGCThread called");
     uninterruptable = false;
    }
   
     public void activated()	  {
	 //Debug.out.println(DomainName+": HLRRobin_mini::activated called");
	 CPUState t = (CPUState)runnables.removeFirst();

	  if (uninterruptable == true)
	    HLschedulerSupport.activateThread(unINTthread);

	  if (t == null) {  /* runQ empty*/
	      /*Debug.out.println("runQ empty ->yield!?");*/
	      HLschedulerSupport.clearMyRunnableFlag();
	      HLschedulerSupport.yield();
	      throw new Error("should never return! (A)");
	  }
	  
	  HLschedulerSupport.activateThread(t);
	  throw new Error("should never return! (C)");
   }
     
     public void dump() {
	  if(uninterruptable == true)
	      { Debug.out.print("U:"); HLschedulerSupport.dumpThread(unINTthread);}
 	  runnables.dump();
    }

}

