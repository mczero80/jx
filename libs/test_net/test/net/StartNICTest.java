package test.net;

import jx.zero.*;

import jx.devices.net.NetworkDevice;
import jx.zero.debug.Dump;
import jx.timer.TimerManager;

class StartNICTest {
    
    public static void main(String[] args) throws Exception {
	Naming naming = InitialNaming.getInitialNaming();
	MemoryManager memMgr = (MemoryManager) naming.lookup("MemoryManager");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	NetworkDevice nic = (NetworkDevice)LookupHelper.waitUntilPortalAvailable(naming, args[0]);
	TimerManager timerManager = (TimerManager)LookupHelper.waitUntilPortalAvailable(naming, "TimerManager");

	    // send
	Memory buf = memMgr.alloc(1514);

	Debug.out.println("NICTest: Waiting 3 seconds...");

	timerManager.unblockInMillis(cpuManager.getCPUState(), 3000);
	cpuManager.block();  // DANGER: lost-update problem (FIXME)

	for(;;) {
	    for(int i=0; i<12; i++) {
		buf.set8(i, (byte)0xff);
	    }
	    for(int i=12; i<1514; i++) {
		buf.set8(i, (byte)i);
	    }

	    Debug.out.println("TRANSMIT");

	    //buf = nic.transmit(buf);
	    buf = nic.transmit1(buf, 0, 200);
	}
    }
}
