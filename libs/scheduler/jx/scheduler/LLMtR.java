package jx.scheduler;

import jx.zero.*;
import jx.zero.debug.*;
import java.util.*;

import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

/** Move-to-Rear List Scheduler */
public class LLMtR implements  LowLevelScheduler{
    Naming naming;
    IRQ irq;
    CPUManager cpuManager;
    SMPCPUManager SMPcpuManager;
    LLSchedulerSupport LLschedulerSupport;
    DebugPrintStream out;
  
    private LinkedList tokens;
    private CPU MyCPU = null;
    private int TimeSlice_init = 0;

  public LLMtR() {
    naming= (Naming) InitialNaming.getInitialNaming();
    irq = (IRQ) naming.lookup("IRQ");
    cpuManager = (CPUManager) naming.lookup("CPUManager");
    SMPcpuManager = (SMPCPUManager) naming.lookup("SMPCPUManager");
    LLschedulerSupport = (LLSchedulerSupport) naming.lookup("LLSchedulerSupport");
    /* init Debug.out */
    DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
    out = new DebugPrintStream(new DebugOutputStream(d));
    Debug.out = out;
    tokens = new LinkedList();
    TimeSlice_init = LLschedulerSupport.getDomainTimeslice();
  }
  
    public void registered(int irq_nr){
	Debug.out.println("LLMtR init called");
	MyCPU = SMPcpuManager.getMyCPU();

	irq.installFirstLevelHandler(irq_nr, this);
	irq.enableIRQ(irq_nr);
	LLschedulerSupport.tuneTimer(TimeSlice_init);
    }

     public void interrupt() {
	 //Debug.out.print("\nLLMtR IRQ");
	 LLschedulerSupport.preemptCurrentThread();
	 LLschedulerSupport.tuneTimer(0);
	 Decision_Epoche();

	 throw new Error("should never return!");
     }
    
    public void registerDomain(Domain domain) {
	  Debug.out.print("LLMtR registerDomain:");
	  LLschedulerSupport.printDomainName(domain);
	  Debug.out.println(" ");
	  cpuManager.dump("MtR:", domain);
	  DomainContainer newDom;
	  
	  if (domain == null)
	      return;
	  
	  if (tokens.contains(domain) == true) {
	       Debug.out.print("Domain\n   ");
	       cpuManager.dump("",domain);
	       Debug.out.println("      already registered.");
	      return;
	  }
	  
	  newDom = getNewContainer(domain);
	  if (tokens.isEmpty())
	    activeDomain=newDom;
	  tokens.add(newDom);

//	  Decision_Epoche();

	  return; 
    }
     
    public void unregisterDomain(Domain domain) {
	throw new Error("not implemented");
    }
    
    public void setTimeSlice(Domain domain, int time) {
	Debug.out.println("LLMtR::setTimeSlice not supported");
    }
    
    public void activate_currDomain()    {
	Decision_Epoche();
	throw new Error("not implemented");
    }

    public void activate_nextDomain()   {
      if (tokens.size()==1){
	//Debug.out.print("!!!!!Naming Loop!!!!!!!!!!!!!!!!!\n");
	LLschedulerSupport.tuneTimer(activeDomain.DomainTimeSlice);
	LLschedulerSupport.activateIdleThread();
	throw new Error("should never return!");
      }
        Decision_Epoche();
	throw new Error("should never return!");
    }

    public void dump() {
	  Debug.out.print("DomainQueue for CPU: "+SMPcpuManager.getMyCPU()+":");
   	  Debug.out.println("                     (ContainerPoolSize: "+ContainerPoolSize+")");

	  dumpTokens();
 	  Debug.out.println("");	  

  //	  Vector tmp = new Vector();
	  Domain d;
	  for (DomainContainer c = (DomainContainer)tokens.getFirst(); c != null ;  c = (DomainContainer)tokens.getNext()) {
	      d = c.domain;
//	      LLschedulerSupport.printDomainName(d);
//	      Debug.out.print(" ");
//	      if (tmp.contains(d))
//		continue;
//	      tmp.addElement(d);
	      Debug.out.print("Threads in Domain ");
	      LLschedulerSupport.printDomainName(d);
	      Debug.out.println("");
	      LLschedulerSupport.dumpHLSched(d);
	  }
   }
    
    private void dumpTokens() {
	  for (DomainContainer c = (DomainContainer)tokens.getFirst(); c != null ;  c = (DomainContainer)tokens.getNext()) {
	      Debug.out.print("  (");
	      LLschedulerSupport.printDomainName(c.domain);
	      if (LLschedulerSupport.isRunnable(c.domain))
		  Debug.out.print("*");
	      Debug.out.print(","+c.DomainTimeSlice+")");
	  }

 
	  Debug.out.print("\n*******************************************************\n ");
	  
    }


    /************** MTR-LS methods ************************************/     
    private int left=0;
    private DomainContainer activeDomain= null;

    private void Decision_Epoche()  {
	left = LLschedulerSupport.readTimer();
	Update_Domain();
	dump();
	Run_a_Domain();
    }
    private void Update_Domain() {
	 int elapsed = activeDomain.DomainTimeSlice - left;

	if (elapsed == 0) // if no time was used
 	     return;           // we do not need to update the domains
	if (left <= 0)  {
	    MoveToRear(activeDomain);
	}  else {
	    activeDomain.DomainTimeSlice = left;
	    tokens.add(getNewContainer(activeDomain.domain, elapsed));
	}
	Combine_Elements(tokens.size()-1);
    }

    private void Run_a_Domain()  {
	left = 0;
	activeDomain = getFirstRunnable();
	LLschedulerSupport.tuneTimer(activeDomain.DomainTimeSlice);
	LLschedulerSupport.activateDomain(activeDomain.domain);
	throw new Error("should never return!");
    }
    private void  MoveToRear(DomainContainer c) {
	// move to rear and combine elements around the gap
	int pos = tokens.indexOf(c);
	if (pos == -1)
	    throw new Error("system error!");
	tokens.remove(pos);
	Combine_Elements(pos);
	tokens.add(c);
   }
    private void Combine_Elements(int pos) {
	if (pos < 1)
	    return;
	if (pos >=  tokens.size())
	    return;
	/* combine the element at pos with pos -1 */
	DomainContainer a = (DomainContainer)tokens.get(pos-1);
	DomainContainer b = (DomainContainer)tokens.get(pos);
	if (a.domain == b.domain) {
	    a.DomainTimeSlice += b.DomainTimeSlice;
	    // make sure activeDomain is always valid
	    // else there is a problem if no domain is runnable
	    // (method "Run_a_Domain" does not update  "activeDomain"
	    //   --> MoveToRear will result in error next time, because "c" will not be in "tokens")
	    if (activeDomain == b)    
		 activeDomain = a;  
	    tokens.remove(pos);
	    releaseContainer(b);
	}
    }

    private DomainContainer getFirstRunnable() {
	for (DomainContainer c = (DomainContainer)tokens.getFirst(); c != null ;  c = (DomainContainer)tokens.getNext())
	    if (LLschedulerSupport.isRunnable(c.domain))
		return c;
	//Debug.out.println("no domain is runnable");
	LLschedulerSupport.tuneTimer(activeDomain.DomainTimeSlice);
	LLschedulerSupport.activateIdleThread();
	throw new Error("no domain is runnable");
    }

     /**************************************************/     
     private DomainContainer ContainerPool = null;
     private int ContainerPoolSize=0;


     private DomainContainer getNewContainer(Domain dom)  {
	 return getNewContainer(dom, TimeSlice_init);	  
     }
    
    private DomainContainer getNewContainer(Domain dom, int TimeSlice)   {
	DomainContainer c = ContainerPool;
	if (c == null) {
	    ContainerPoolSize++;
	    /*Debug.out.println("LLRRobin: ContainerPool empty, new size: "+ContainerPoolSize);*/
	    return new DomainContainer(dom, TimeSlice);
	}
	ContainerPool=ContainerPool.next;
	
	c.domain = dom;
	c.DomainTimeSlice = TimeSlice;
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
	  
	  public DomainContainer(Domain content, int TimeSlice)  {
	       this.domain=content;
	       DomainTimeSlice = TimeSlice;
	       next = null;
	  }
     }
}

