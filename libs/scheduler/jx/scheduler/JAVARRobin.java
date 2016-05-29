package jx.scheduler;

import jx.zero.*;
import jx.zero.debug.*;

import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

public class JAVARRobin implements JAVAScheduler, FirstLevelIrqHandler{
     
    Naming domainZero;
    IRQ irq;
    CPUManager cpuManager;
    SMPCPUManager SMPcpuManager;
    JAVASchedulerSupport JAVAschedulerSupport;
    DebugPrintStream out;
  
    private CPUStateContainer first;
    private CPUStateContainer last;
    private CPUState idleThread;
    private CPU MyCPU = null;

 
    public JAVARRobin() {
	  domainZero=  InitialNaming.getInitialNaming();
 	  irq = (IRQ) domainZero.lookup("IRQ");
	  cpuManager = (CPUManager) domainZero.lookup("CPUManager");
	  SMPcpuManager = (SMPCPUManager) domainZero.lookup("SMPCPUManager");
	  JAVAschedulerSupport = (JAVASchedulerSupport) domainZero.lookup("JAVASchedulerSupport");
	  /* init Debug.out */
	  DebugChannel d = (DebugChannel) domainZero.lookup("DebugChannel0");
	  out = new DebugPrintStream(new DebugOutputStream(d));
	  Debug.out = out;
	  
	  first = null;
	  last  = null;
	  
   }

    public void init(int irq_nr){
	Debug.out.println("RRobin register called");
	idleThread = JAVAschedulerSupport.getIdleThread(); 
	MyCPU = SMPcpuManager.getMyCPU();
//	JAVAschedulerSupport.setTimeslice(1);
//	JAVAschedulerSupport.setTimeslice(100);
	irq.installFirstLevelHandler(irq_nr, this);
	irq.enableIRQ(irq_nr);
    }


     public void interrupt() {
	 /*Debug.out.println("RRobin IRQ CPU:"+cpu_id);*/
	  CPUState c=removeNext();
	  if (c == null) {
	       /*Debug.out.println("IRQ:runQ is empty");*/
	       return;
	  }
	  /*Debug.out.println("IRQ:switch to next Thread");*/
	  add(JAVAschedulerSupport.getCurThr());  //	=add_current();
	  JAVAschedulerSupport.switchTo(c);
     }
     
     public void add(CPUState newThread)  {
	  /*Debug.out.println("RRobin add called");*/
	 //Debug.out.print("+"); //ttt
	  CPUStateContainer c;
	  
	  if (newThread == null)
	      return;
	  if (newThread == idleThread)
	      return;  // ignore idle Thread

	  for (c = first; c != null; c=c.next) 
	       if (c.content == newThread) {
		    Debug.out.println("Thread "+"<THREAD>"+" already in runqueue.");
		    return;
	       }

	  if (last == null)
	       last = first = getNewContainer(newThread);
	  else
	  {
	       last.next = getNewContainer(newThread);
	       last = last.next;
	  } 
	  return; 
     }
 
     /** returns the next Thread or null if there is none */
     public CPUState removeNext()	  {
	  /*Debug.out.println("RRobin::removeNext called");*/
	 //Debug.out.print("-"); //ttt
	  CPUStateContainer c = null;
	  CPUState t = null;
	  /* lock Q */  // todo
	  
	  if (first == null) {  /* runQ empty*/
	      /*Debug.out.println("runQ empty");*/
	      return null;
	  }
	      
	      c = first;
	      first = first.next;
	      if (first == null)   /* runQ empty now*/
		   last = null;
	      
	      t = c.content;
	      releaseContainer(c);

	  return t;
     }
     
     public String toString() {
	  return "this is a RRobin Object";
     }

     public void dump() {
	  Debug.out.println("RunQueue for CPU "+SMPcpuManager.getMyCPU()+":");
   	  Debug.out.println("ContainerPoolSize: "+ContainerPoolSize);
	  for (CPUStateContainer c = first; c != null; c=c.next) 
	       JAVAschedulerSupport.dumpThread(c.content);
    }
     
     private CPUStateContainer ContainerPool = null;
     private int ContainerPoolSize=0;

     private CPUStateContainer getNewContainer(CPUState t)   {
	  
	  CPUStateContainer c = ContainerPool;
	  if (c == null) {
	       ContainerPoolSize++;
	       Debug.out.println("RRobin: ContainerPool empty, new size: "+ContainerPoolSize);
	      return new CPUStateContainer(t);
	  }
	  ContainerPool=ContainerPool.next;
	  
	  c.content = t;
	  c.next = null;
	  return c;
     }
     
     private void releaseContainer(CPUStateContainer c)	  {
	       c.next = ContainerPool;
	       ContainerPool = c;
     }
     
/*  Helper Class */
     class CPUStateContainer {
	  CPUState content;
	  CPUStateContainer next;
	  
	  public CPUStateContainer(CPUState content)  {
	       this.content=content;
	       next = null;
	  }
     }
}

