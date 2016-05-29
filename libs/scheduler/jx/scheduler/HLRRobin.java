package jx.scheduler;

import jx.zero.*;
import jx.zero.debug.*;

import jx.zero.scheduler.*;

import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

public class HLRRobin implements HLScheduler_runnables, HLS_switchTo, HLS_portalCalled, HLS_GCThread {
//public class HLRRobin implements jx.zero.scheduler.HLScheduler_all {
     
    CPUManager cpuManager;
    SMPCPUManager SMPcpuManager;
    HLSchedulerSupport HLschedulerSupport;
    DebugPrintStream out;
  
    protected ThreadListCAS runnables=null;
    private CPU MyCPU = null;

    private int anzSchedInterrupted = 0;
    private int anzSchedPreempted = 0;
    private boolean yielded=false;
    private boolean uninterruptable = false;
    private CPUState unINTthread=null;

  public String DomainName="";   //test
  private static int instNr = 0; //test
  private boolean debug=false;   //test

    public HLRRobin() {
    Naming naming= (Naming) InitialNaming.getInitialNaming();
    /* init Debug.out */
    DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
    if (d == null) throw new Error("DebugChannel0 Portal not found");
    out = new DebugPrintStream(new DebugOutputStream(d));
    Debug.out = out;

    cpuManager = (CPUManager) LookupHelper.waitUntilPortalAvailable(naming,"CPUManager");
    SMPcpuManager = (SMPCPUManager) LookupHelper.waitUntilPortalAvailable(naming,"SMPCPUManager");
    HLschedulerSupport = (HLSchedulerSupport) LookupHelper.waitUntilPortalAvailable(naming,"HLSchedulerSupport");
    
    DomainName="Inst:"+(instNr++);
    runnables = new ThreadListCAS();

   }

    public void registered(){
	Debug.out.println("HLRRobin registered called on CPU"+SMPcpuManager.getMyCPU());
	MyCPU = SMPcpuManager.getMyCPU();

	int timebase = HLschedulerSupport.getTimeBaseInMicros();
	int slice = HLschedulerSupport.getDomainTimeslice ();
	//	HLschedulerSupport.setMyTimeslice (slice/2);
//	HLschedulerSupport.setMyTimeslice (42);

//      DomainZeroLookup.breakpoint();
    }

    public boolean Scheduler_interrupted(){
//	anzSchedInterrupted++;
// 	Debug.out.println("The Scheduler Thread was interrupted ("+anzSchedInterrupted+")");
//	Debug.out.println("ignorring Interruption");
//	Debug.out.print("=");
	return false;
    }

    public boolean Scheduler_preempted(){
//	anzSchedPreempted++;
// 	Debug.out.println("CPU"+SMPcpuManager.getMyCPU()+":The Scheduler Thread was preempted ("+anzSchedPreempted+")");
//	Debug.out.println("this can lead to inconsistent Scheduling data!!");
//	Debug.out.println("resume Thread next time this Domain gets CPU Time");
//	Debug.out.print("|");
//      throw new Error("Scheduler_preempted");
	return false;
    }

    public void interrupted(CPUState newThread)  {
	if (debug)
	    Debug.out.println("HLRRobin::interrupted called");
	preempted(newThread);
    }

    public void preempted(CPUState newThread)  {
	if (debug)
	    Debug.out.println("HLRRobin::preempted called (uninterruptable:"+uninterruptable+")");
	if (uninterruptable)
	    unINTthread = newThread;
	else
	    runnables.add(newThread);
    }
 
    public void yielded(CPUState newThread)  {
	if (debug)
	    Debug.out.println(DomainName+" "+MyCPU.getID()+":HLRRobin::yielded called");
        yielded = true;
	runnables.add(newThread);
    }
    public void unblocked(CPUState newThread)  {
	//Debug.out.println(DomainName+": HLRRobin::unblocked called");
	runnables.add(newThread);
    }
    
    public void created(CPUState Thread)  {
	if (debug)
	    Debug.out.println("HLRRobin::created called");
	/* fixme: check if Thread is the idle Thread!! */
	runnables.add(Thread);
    }

    public void switchedTo(CPUState Thread){
      if (debug)
	  Debug.out.println(DomainName+": HLRRobin::switchedTo called");
      HLschedulerSupport.activateThread(Thread);
      throw new Error("should never return!");
    }

    public void blockedInPortalCall(CPUState Thread){
	if (debug)
 	    Debug.out.println(DomainName+": HLRRobin::blockedInPortalCall");
     }
    
    public boolean portalCalled(CPUState Thread){
	if (debug)
	    Debug.out.println(DomainName+": HLRRobin::portalCalled called");
	return true;  // hand-off
	//return false;
    }
/*    
     public void blocked(CPUState Thread)  {
 	if (debug)
 	    Debug.out.println(DomainName+": HLRRobin::blocked called");
     }
*/    
/*    
     public void destroyed(CPUState Thread)  {
	 //Debug.out.println("HLRRobin::destroyed called");
     }
*/    
   public void startGCThread (CPUState interruptedThread, CPUState GCThread) {
       if (debug)
	   Debug.out.println("HLRRobin::startGCThread called");
       if (interruptedThread != null)
	   runnables.add(interruptedThread);
       uninterruptable = true;
       HLschedulerSupport.activateThread(GCThread);
    }

    public void unblockedGCThread (CPUState GCThread){
 	if (debug)
	    Debug.out.println("HLRRobin::unblockedGCThread called");
      if (uninterruptable)
	throw new Error("should not happen!");  
      unINTthread= GCThread;
      uninterruptable = true;
    }

    public void destroyedGCThread (CPUState GCThread){
	if (debug)
	    Debug.out.println("HLRRobin::destroyedGCThread called");
	uninterruptable = false;
    }
   
     public void activated()	  {
	 if (debug)
	     Debug.out.println("HLRRobin::activated called");

	 if (uninterruptable)
	     HLschedulerSupport.activateThread(unINTthread);
	 
	  /* only one Thread activatable
	     and this Thread called yield
	     so activate the next Domain*/

//	  if (yielded == true && anzThreads == 1) {
	  if (yielded == true ) {
	    /*Debug.out.println("sole Thread wants to yield ->yield!?");*/
	    yielded = false;
	    HLschedulerSupport.yield();
	    throw new Error("should never return! (B)");
	  }
//	  yielded = false;
	  CPUState t = (CPUState)runnables.removeFirst();

	  if (t == null) {  /* runQ empty*/
	      /*Debug.out.println("runQ empty ->yield!?");*/
	      HLschedulerSupport.clearMyRunnableFlag();
	      HLschedulerSupport.yield();
 	      throw new Error("should never return!");
	  }
	  
	  HLschedulerSupport.activateThread(t);
	  throw new Error("should never return!");
   }
     
     public String toString() {
	  return "this is a HLRRobin Object";
     }

     public void dump() {
	  runnables.dump();
    }

}

