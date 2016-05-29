package jx.scheduler;

import jx.zero.*;
import jx.zero.debug.*;

import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

public class HLRRobin_all implements jx.zero.scheduler.HLScheduler_all {
     
    Naming domainZero;
    CPUManager cpuManager;
    SMPCPUManager SMPcpuManager;
    HLSchedulerSupport HLschedulerSupport;
    DebugPrintStream out;
  
    protected ThreadList runnables = new ThreadList();
    protected ThreadList blocked = new ThreadList();
    private CPU MyCPU = null;

    private int anzSchedInterrupted = 0;
    private int anzSchedPreempted = 0;
    private boolean yielded=false;

    private boolean uninterruptable = false;
    private CPUState unINTthread=null;

  public String DomainName="";   //test
  private static int instNr = 0; //test
  private boolean debug=false;   //test

    public HLRRobin_all() {
	domainZero=  InitialNaming.getInitialNaming();
	  cpuManager = (CPUManager) domainZero.lookup("CPUManager");
	  SMPcpuManager = (SMPCPUManager) domainZero.lookup("SMPCPUManager");
	  HLschedulerSupport = (HLSchedulerSupport) domainZero.lookup("HLSchedulerSupport");
	  /* init Debug.out */
	  DebugChannel d = (DebugChannel) domainZero.lookup("DebugChannel0");
	  out = new DebugPrintStream(new DebugOutputStream(d));
	  Debug.out = out;
	  DomainName="Inst:"+(instNr++);
   }

    public void registered(){
	Debug.out.println("HLRRobin registered called on CPU"+SMPcpuManager.getMyCPU());
	MyCPU = SMPcpuManager.getMyCPU();

	int timebase = HLschedulerSupport.getTimeBaseInMicros();
	int slice = HLschedulerSupport.getDomainTimeslice ();
	HLschedulerSupport.setMyTimeslice (slice/2);
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
	//Debug.out.println("HLRRobin::interrupted called");
	preempted(newThread);
    }

    public void preempted(CPUState newThread)  {
	//Debug.out.println("HLRRobin::preempted called");
	if (uninterruptable)
	    unINTthread = newThread;
	else
	    runnables.add(newThread);
     }
 
    public void yielded(CPUState newThread)  {
	//Debug.out.println(DomainName+" "+MyCPU.getID()+":HLRRobin::yielded called");
        yielded = true;
	runnables.add(newThread);
    }
    public void unblocked(CPUState newThread)  {
	if (debug)
	    Debug.out.println(DomainName+": HLRRobin::unblocked called");
	if (blocked.remove(newThread) == false)
	    Debug.out.println(DomainName+": unblocked: Thread XXX not blocked ");
	runnables.add(newThread);
    }
    
    public void created(CPUState newThread)  {
	//Debug.out.println("HLRRobin::created called");
	runnables.add(newThread);
    }

    public void switchedTo(CPUState Thread){
      if (debug)
	Debug.out.println(DomainName+": HLRRobin::switchedTo called");

      if (blocked.remove(Thread) == false){
	    Debug.out.println(DomainName+": switchedTo: Thread XXX not blocked ");
      }

      HLschedulerSupport.activateThread(Thread);
      throw new Error("should never return!");
    }
    public void blockedInPortalCall(CPUState Thread){
      if (debug)
	Debug.out.println(DomainName+": HLRRobin::blockedInPortalCall");
      blocked.add(Thread);
    }
     public boolean portalCalled(CPUState Thread){
       if (debug)
	 Debug.out.println(DomainName+": HLRRobin::portalCalled called");
       blocked.add(Thread);
       return true;  // hand-off
       //return false;
    }
    
    public void blocked(CPUState Thread)  {
      if (debug)
	Debug.out.println(DomainName+": HLRRobin::blocked called");
      blocked.add(Thread);
    }
     public void destroyed(CPUState Thread)  {
	 //Debug.out.println("HLRRobin::destroyed called");
    }

   public void startGCThread (CPUState interruptedThread, CPUState GCThread) {
       //Debug.out.println("HLRRobin::startGCThread called");
       runnables.add(interruptedThread);
       uninterruptable = true;
       HLschedulerSupport.activateThread(GCThread);
    }
    public void unblockedGCThread (CPUState GCThread){
      //Debug.out.println("HLRRobin_all::unblockedGCThread called");
      if (uninterruptable)
	throw new Error("should not happen!");  
      unINTthread= GCThread;
      uninterruptable = true;
    }
    public void destroyedGCThread (CPUState GCThread){
	//Debug.out.println("HLRRobin::destroyedGCThread called");
	uninterruptable = false;
    }
   
      
     public void activated()	  {
	 //Debug.out.println("HLRRobin::activated called");
	  CPUState t = null;

	 if (uninterruptable)
	     HLschedulerSupport.activateThread(unINTthread);


	  /* only one Thread activatable
	     and this Thread called yield
	     so activate the next Domain*/
	  if (yielded == true && runnables.size() == 1) {
	    /*Debug.out.println("sole Thread wants to yield ->yield!?");*/
	    yielded = false;
	    HLschedulerSupport.yield();
	    throw new Error("should never return! (B)");
	  }
	  yielded = false;
	   
	  t=(CPUState)runnables.removeFirst();
	  if (t == null) {  /* runQ empty*/
	      /*Debug.out.println("runQ empty ->yield!?");*/
	      HLschedulerSupport.clearMyRunnableFlag();
	      HLschedulerSupport.yield();
	      throw new Error("should never return! (A)");
	  }

	  HLschedulerSupport.activateThread(t);
	  throw new Error("should never return! (C)");
   }
     
     public String toString() {
	  return "this is a HLRRobin_all Object";
     }

     public void dump() {
//	  Debug.out.println("RunQueue for Domain XXX"+0+":");
//   	  Debug.out.println("                    (ContainerPoolSize: "+ContainerPoolSize+")");
	 if (runnables.getFirst() == null && blocked.getFirst() == null) {
	     Debug.out.println("       no runnable or blocked Threads");
	     return;
	 }
	     
	 Debug.out.println("  runnable Threads:");
	 runnables.dump();

	 Debug.out.println("  blocked Threads:");
	 blocked.dump();

    }
}

