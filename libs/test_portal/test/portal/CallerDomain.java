package test.portal;

import jx.zero.*;
import jx.zero.scheduler.*;
import jx.scheduler.*;
import jx.zero.debug.*;

public class CallerDomain {

    public static void init(Naming naming) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	new CallerDomain(naming);
    }

    public CallerDomain(Naming naming) {
//	SMPcpuManager = (SMPCPUManager) naming.lookup("SMPCPUManager");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Debug.out.println("CallerDomain started...");

	cpuManager.setThreadName("Caller");

	ServiceDomain service = (ServiceDomain) naming.lookup("Service1");
	int result;
	Debug.out.println("calling service1");
	result = service.do_service1(42);
	Debug.out.println("result: "+result);
	result = service.do_service2(42);
	Debug.out.println("result: "+result);
    }

}

