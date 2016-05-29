package jx.scheduler;

import jx.zero.*;
import jx.zero.debug.*;

import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

public class LLRRobin implements  LowLevelScheduler{

    IRQ irq;
    CPUManager cpuManager;
    SMPCPUManager SMPcpuManager;
    LLSchedulerSupport LLschedulerSupport;
    DebugPrintStream out;
  
    private DomainContainer first;
    private DomainContainer activeDom;
    private CPU MyCPU = null;
    private int GlobalTimeSlice_init = 0;
    private int GlobalTimeSlice = 0; //amount of time left for current domain

    private final boolean debug = false;

  public LLRRobin() {
    Naming naming= (Naming) InitialNaming.getInitialNaming();
    /* init Debug.out */
    /*
    DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
    if (d == null) throw new Error("DebugChannel0 Portal not found");
    out = new DebugPrintStream(new DebugOutputStream(d));
    Debug.out = out;
    */

    irq = (IRQ) LookupHelper.waitUntilPortalAvailable(naming,"IRQ");
    cpuManager = (CPUManager) LookupHelper.waitUntilPortalAvailable(naming,"CPUManager");
    SMPcpuManager = (SMPCPUManager) LookupHelper.waitUntilPortalAvailable(naming,"SMPCPUManager");
    LLschedulerSupport = (LLSchedulerSupport) LookupHelper.waitUntilPortalAvailable(naming,"LLSchedulerSupport");
    first = null;
    activeDom = null;
    GlobalTimeSlice = GlobalTimeSlice_init = LLschedulerSupport.getDomainTimeslice();
  }
  
    public void registered(int irq_nr){
	if (debug)
	    Debug.out.println("LLRRobin init called");
	MyCPU = SMPcpuManager.getMyCPU();

	irq.installFirstLevelHandler(irq_nr, this);
	irq.enableIRQ(irq_nr);
	LLschedulerSupport.tuneTimer(GlobalTimeSlice);
    }

     public void interrupt() {
	 if (debug)
	     Debug.out.print("\nLLRRobin IRQ CPU:"+MyCPU);
	 DomainContainer next;

	 if (activeDom == null) {
	     GlobalTimeSlice = GlobalTimeSlice_init;
	     LLschedulerSupport.tuneTimer(GlobalTimeSlice);
	     return;
	 }

	 /* set new TimeSlice */
	 GlobalTimeSlice -= activeDom.DomainTimeSlice;
	 if (GlobalTimeSlice >= 0) {	    /* interrupt Domain */
	   LLschedulerSupport.interruptCurrentThread();
	   LLschedulerSupport.tuneTimer(activeDom.DomainTimeSlice);
	 } else {                           /* preempt Domain */
	     LLschedulerSupport.preemptCurrentThread();
	     next = nextDomain();
	     if (next == null) 
		 throw new Error("LLRRobin::interrupt: System error: no Domain found!!");
	     
	     activeDom = next;

	     GlobalTimeSlice = GlobalTimeSlice_init - activeDom.DomainTimeSlice;
	     LLschedulerSupport.tuneTimer(activeDom.DomainTimeSlice);
	 }

	 LLschedulerSupport.activateDomain(activeDom.domain);
	 throw new Error("should never return!");
  }

    public void registerDomain(Domain domain) {
	  if (debug)
	      Debug.out.println("LLRRobin registerDomain called");
	  //cpuManager.dump("ROBIN", domain);
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
	 if (debug)
	     Debug.out.println("LLRRobin unregisterDomain called");
	 DomainContainer prev, curr;
	 
	 if (domain == null)
	       return;
	  
	  prev = null;
	  curr = first;
	  while (curr != null) {
	      if (curr.domain == domain) {   // domain found
		  if (activeDom == curr) {    // Domain is active now (should never happen)    
		    activeDom = nextDomain();
		    if (activeDom == curr)    // there is no other Domain
		      activeDom = null;
		    GlobalTimeSlice = GlobalTimeSlice_init;
		    LLschedulerSupport.tuneTimer(GlobalTimeSlice);
		  }

		  if (curr == first) 
		    first = first.next;
		  else 
		    prev.next = curr.next;
		  releaseContainer(curr);
		  break;
	      }
	      prev = curr;
	      curr = curr.next;
	  }  // end while
     }

    public void setTimeSlice(Domain domain, int time) {
	if (debug)
	    Debug.out.println("LLRRobin: setTimeSlice called");
	if (time == 0)
	    time = GlobalTimeSlice_init;
	if (time < 0) {
	    Debug.out.println("requested TimeSlice ("+time+") is less than 0");
	    time = GlobalTimeSlice_init;
	}
	if (time > GlobalTimeSlice_init) {
	    Debug.out.println("requested TimeSlice ("+time+") is bigger than global TimeSlice ("+GlobalTimeSlice+")");
	    time = GlobalTimeSlice_init;
	}
	for (DomainContainer c = first; c != null; c=c.next) 
	    if (c.domain == domain) {
		c.DomainTimeSlice = time;
		return;
	    }
	Debug.out.print("Domain ");
	LLschedulerSupport.printDomainName(domain);
	Debug.out.println(" not registered.");
	throw new Error("System Error");
    }
    
    public void activate_currDomain()    {
	/*Debug.out.println("LLRRobin::activate_currDomain called (GTS:"+GlobalTimeSlice+" DTSleft:"+ LLschedulerSupport.readTimer()+")");*/
	 /* is there enougth time left for the current Domain? */
 //Debug.out.println("\nrest:"+ LLschedulerSupport.readTimer()+" GTS:"+GlobalTimeSlice);
	 GlobalTimeSlice += LLschedulerSupport.readTimer()-activeDom.DomainTimeSlice;


	 if (GlobalTimeSlice >= 0) 	    /* activate current Domainn */
	 {
	      __activate_currDomain();
	 }
	 else                               /* activate next Domain */
	      activate_nextDomain();
	 
    }

     private void __activate_currDomain()    {
	  LLschedulerSupport.tuneTimer(activeDom.DomainTimeSlice);
	  
	  LLschedulerSupport.activateDomain(activeDom.domain);
	  throw new Error("should never return!");
     }
    
    public void activate_nextDomain()   {
	DomainContainer next = nextDomain();
	
	if (next == null) 
	      throw new Error("LLRRobin::activate_nextDomain: System error: no Domain found!!");

/*	if (activeDom == next){  // DomaZero HLSched is looping because there is no Thread and no other HLS 
	    if (debug)
		Debug.out.print("!!!!!DomainZero Loop!!!!!!!!!!!!!!!!!\n");
	    LLschedulerSupport.tuneTimer(activeDom.DomainTimeSlice);
	    LLschedulerSupport.activateIdleThread();
	    throw new Error("should never return!");

	}
*/	activeDom = next;
	
	/* reset TimeSlice for new Domain */
	GlobalTimeSlice = GlobalTimeSlice_init-activeDom.DomainTimeSlice;
	__activate_currDomain();
    }

     public String toString() {
	  return "this is a LowLevel RoundRobin Scheduler (LLRRobin) Object";
     }

     public void dump() {
	  Debug.out.print("DomainQueue for CPU"+SMPcpuManager.getMyCPU()+":");
//   	  Debug.out.print("                     (ContainerPoolSize: "+ContainerPoolSize+")");
   	  Debug.out.println("");
	  if (activeDom != null) {
	      Debug.out.print("Running domain: "/*+activeDom.domain.getID()*/+"(");
	      LLschedulerSupport.printDomainName(activeDom.domain);
	      Debug.out.println(")");
	      LLschedulerSupport.dumpHLSched(activeDom.domain);
	  } else
	      Debug.out.println("no Domain active");
/*	  for (DomainContainer c = first; c != null; c=c.next) {
	       LLschedulerSupport.dumpDomain(c.domain);
	  }
*/	  for (DomainContainer c = first; c != null; c=c.next) {
              if (c == activeDom)
		  continue;
	      Debug.out.print("Threads in Domain "/*+activeDom.domain.getID()*/+"(");
	      LLschedulerSupport.printDomainName(c.domain);
	      Debug.out.println(")");
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
	       if (debug)
		   Debug.out.println("LLRRobin: ContainerPool empty, new size: "+ContainerPoolSize);
	      return new DomainContainer(dom);
	  }
	  ContainerPool=ContainerPool.next;
	  
	  c.domain = dom;
	  c.next = null;
	  return c;
     }
     
     private void releaseContainer(DomainContainer c)	  {
	       c.domain = null;
	       c.DomainTimeSlice = GlobalTimeSlice_init;
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
	       DomainTimeSlice = GlobalTimeSlice_init;
	       next = null;
	  }
     }
}

