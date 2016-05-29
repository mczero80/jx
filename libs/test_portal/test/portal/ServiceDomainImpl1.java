package test.portal;

import jx.zero.*;
import jx.zero.debug.*;

public class ServiceDomainImpl1 implements ServiceDomain, Service {
    SMPCPUManager SMPcpuManager;
    
    public static void init(Naming naming) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	new ServiceDomainImpl1(naming);
    }

    public ServiceDomainImpl1 (Naming naming) {
	SMPcpuManager = (SMPCPUManager) naming.lookup("SMPCPUManager");
	CPUManager c = (CPUManager)naming.lookup("CPUManager");
	Debug.out.println("Hello I am ServiceDomain 1");

	/* export service */
	ServiceDomain service = this;
	naming.registerPortal(service, "Service1");
    }

    public Portal lookup(String name) {
	return null;
    }

    public int do_service1(int i) {
	ServiceDomain service = (ServiceDomain) InitialNaming.getInitialNaming().lookup("Service2");
	Debug.out.println("  Service 1 is calling Service 2 with: "+i);
	int result = service.do_service1(i);
	Debug.out.println("   result from Service 2: "+result);
	return do_service2(result+1);
    }
    public int do_service2(int i) {
	Debug.out.println("  Service 1 (do_service2): "+i);
	int j=0;
	for (int a=0; a < 500*100000; a ++) {
    //	    Thread.yield();
	    if (j++>100000) {
	 	j=0;
//		new Integer(42);  // waste memory
		Debug.out.print(".");
	    } 
      	}
	Debug.out.println("end");
	return i;
    }

    public void dump() {throw new Error();}
    public void destroy() {throw new Error();}

}

