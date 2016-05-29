package jx.wintv;

import jx.zero.*;

public class ZeroDomain {
   public final static Naming 	naming;
   public final static CPUManager 	cpuMgr;
   public final static MemoryManager 	memMgr;
   public final static IRQ		irqMgr;
   public final static Clock		clock;
   public final static DomainManager 	dm;
   
   static {
      naming= InitialNaming.getInitialNaming();
      cpuMgr	= (CPUManager)naming.lookup("CPUManager");
      dm	= (DomainManager)naming.lookup("DomainManager");
      memMgr	= (MemoryManager)naming.lookup("MemoryManager");
      irqMgr	= (IRQ)naming.lookup("IRQ");
      clock	= (Clock)naming.lookup("Clock");
   }
   
   
   public static Portal lookup(String depName){
      return naming.lookup(depName);
   }
   
   public static void registerPortal(Portal dep, String name){
      naming.registerPortal(dep, name);
   }
   
    /*
   public static Domain createDomain(String name, 
			    String domainCode, String[] libs,
			    String startClass, int heapSize){
      return DomainStarter.createDomain(name, domainCode, libs, startClass, heapSize);
   }
    */

   public static void startThread(ThreadEntry entry){
     cpuMgr.start(cpuMgr.createCPUState(entry));
   }
}
