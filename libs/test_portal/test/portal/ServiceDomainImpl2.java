package test.portal;

import jx.zero.*;
import jx.zero.debug.*;

public class ServiceDomainImpl2 implements ServiceDomain, Service {
    SMPCPUManager SMPcpuManager;

    public static void init(Naming naming) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	new ServiceDomainImpl2(naming);
    }

    public ServiceDomainImpl2 (Naming naming) {
       	Debug.out.println("Hello I am ServiceDomain 2");
	SMPcpuManager = (SMPCPUManager) naming.lookup("SMPCPUManager");
	CPUManager c = (CPUManager) naming.lookup("CPUManager");
	
	/* activate Portal */
	//final ServiceDomain service = (ServiceDomain) naming.promoteDEP(this, "test/portal/ServiceDomain");
	final ServiceDomain service = this;
	naming.registerPortal(service, "Service2");
	
	new Thread("SrvDom2Thr2(p)") {
		public void run() {
		    Integer dummy;
		    for (int j=0, i=0;;) {
//			for (int a=0; a < 500; a ++);
			if (j++>100000) {
			    j=0; i++;
//			    for (int a=0; a < 50; a ++)
//				dummy = new Integer(42);   // waste memory
			    Debug.out.print("p");
			} 
			if (i==100) {
			    i = 0;
			    Thread.yield();
			}
		    }		
		}
	    }.start();
	
	  c.setThreadName("SrvDom2MainThr(o)");
	  for (int j=0, i=0;;) {
	       //	    for (int a=0; a < 500; a ++);
	       if (j++>100000) {
	 	j=0;  i++;
		for (int a=0; a < 500; a ++);
		Debug.out.print("o");
//		for (int a=0; a < 50; a ++)
//		    new Integer(43);
	       } 
       
	       if (i==100) {
		   i = 0;
		   Thread.yield();
	       }
	  }
      	}	
    
     public Portal lookup(String name) {
	  return null;
     }
     
    public int do_service1(int i) {
	int j=0;
	for (int a=0; a < 500*100000; a ++) {
    //	    Thread.yield();
	    if (j++>100000) {
	 	j=0;
		Debug.out.print("+");
	    }
//	   new Integer(42);  // waste memory
       	}
	Debug.out.println("end");
	return i+1;
    }
    public int do_service2(int i) {
	return i+1;
    }

    public void dump() {throw new Error();}
    public void destroy() {throw new Error();}
}

