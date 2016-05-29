package jx.scheduler;

import jx.zero.*;
import jx.zero.debug.*;

import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

public class LLRRobinTest implements  LowLevelScheduler{
    Naming naming;
    IRQ irq;
    CPUManager cpuManager;
    SMPCPUManager SMPcpuManager;
    LLSchedulerSupport LLschedulerSupport;
    DebugPrintStream out;
  
    private DomainContainer first;
    private DomainContainer activeDom;
    private CPU MyCPU = null;

  public LLRRobinTest() {
    naming= (Naming) InitialNaming.getInitialNaming();
    irq = (IRQ) naming.lookup("IRQ");
    cpuManager = (CPUManager) naming.lookup("CPUManager");
    SMPcpuManager = (SMPCPUManager) naming.lookup("SMPCPUManager");
    LLschedulerSupport = (LLSchedulerSupport) naming.lookup("LLSchedulerSupport");
    /* init Debug.out */
    DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
    out = new DebugPrintStream(new DebugOutputStream(d));
    Debug.out = out;
    first = null;
    activeDom = null;
  }
  
    public void registered(int irq_nr){
	Debug.out.println("LLRRobin init called (IRQ"+irq_nr+")");
	MyCPU = SMPcpuManager.getMyCPU();

	irq.installFirstLevelHandler(irq_nr, this);
	irq.enableIRQ(irq_nr);
    }

     public void interrupt() {
//	 Debug.out.print("\nLLRRobin IRQ CPU:"+MyCPU.getID());
	 DomainContainer next;

	 if (activeDom == null) {
	     return;
	 }

	 LLschedulerSupport.preemptCurrentThread();
	 LLschedulerSupport.activateDomain(activeDom.domain);
	 throw new Error("should never return!");
  }

    public void registerDomain(Domain domain) {
	  Debug.out.println("LLRRobin registerDomain called");
	  DomainContainer newDom;
	  
	  if (domain == null)
	      return;

	  for (DomainContainer c = first; c != null; c=c.next) 
	       if (c.domain == domain) {
		    Debug.out.println("Domain "+"XXX"+" already registered.");
		    return;
	       }
	  
	  newDom = getNewContainer(domain);
	  newDom.next = first;
	  first = newDom;
	  
	  if (activeDom == null)
	      activeDom = first;
	  
	 return; 
    }
     
     public void unregisterDomain(Domain domain) {
	 Debug.out.println("LLRRobin unregisterDomain called");
	 throw new Error("LLRRobin_mini::unregisterDomain not implemented");
     }

    public void setTimeSlice(Domain domain, int time) {
	 Debug.out.println("private TimeSlices are not allowed by this LLScheduler");
    }
    
    public void activate_currDomain()    {
	      __activate_currDomain();
    }

     private void __activate_currDomain()    {
	  LLschedulerSupport.activateDomain(activeDom.domain);
	  throw new Error("should never return!");
     }
    
    public void activate_nextDomain()   {
	DomainContainer next = nextDomain();
	
	if (next == null) 
	      throw new Error("LLRRobin::activate_nextDomain: System error: no Domain found!!");

	if (activeDom == next){  /* Naming HLSched is looping because there is no Thread and no other HLS */
	    //Debug.out.print("!!!!!Naming Loop!!!!!!!!!!!!!!!!!\n");
	    LLschedulerSupport.activateIdleThread();
	    throw new Error("should never return!");
	}
	activeDom = next;
	
	__activate_currDomain();
    }

     public String toString() {
	  return "this is a LowLevel RoundRobin Scheduler (LLRRobin) Object";
     }

     public void dump() {
	  Debug.out.print("DomainQueue for CPU: "+SMPcpuManager.getMyCPU()+":");
   	  Debug.out.println("                     (ContainerPoolSize: "+ContainerPoolSize+")");
	  if (activeDom != null) {
	      Debug.out.print("Active Domain: ");
	      LLschedulerSupport.printDomainName(activeDom.domain);
	      Debug.out.println("");
	  } else
	      Debug.out.println("no Domain active");
	  for (DomainContainer c = first; c != null; c=c.next) {
	       LLschedulerSupport.dumpDomain(c.domain);
	  }
	  for (DomainContainer c = first; c != null; c=c.next) {
	      Debug.out.print("Threads in Domain ");
	      LLschedulerSupport.printDomainName(c.domain);
	      Debug.out.println("");
	      LLschedulerSupport.dumpHLSched(c.domain);
	  }
    }

     /************** private methods ************************************/     

     private DomainContainer nextDomain() {
	  if (activeDom.next != null)
	      return activeDom.next;
	  else
	       return first;
     }

     /**************************************************/     
     private DomainContainer ContainerPool = null;
     private int ContainerPoolSize=0;

     private DomainContainer getNewContainer(Domain dom)   {
	  
	  DomainContainer c = ContainerPool;
	  if (c == null) {
	       ContainerPoolSize++;
	       /*Debug.out.println("LLRRobin: ContainerPool empty, new size: "+ContainerPoolSize);*/
	      return new DomainContainer(dom);
	  }
	  ContainerPool=ContainerPool.next;
	  
	  c.domain = dom;
	  c.next = null;
	  return c;
     }
     
     private void releaseContainer(DomainContainer c)	  {
	       c.domain = null;
	       c.DomainTimeSlice = 0;
	       c.next = ContainerPool;
	       ContainerPool = c;
     }
     
/*  Helper Class */
     class DomainContainer {
	  Domain domain;
	  int DomainTimeSlice;
	  DomainContainer next;
	  
	  public DomainContainer(Domain content)  {
	       this.domain=content;
	       next = null;
	  }
     }
}

