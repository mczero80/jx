package test.portal.scale;

import jx.zero.*;
import jx.zero.debug.*;
import java.io.*;

class MiniDomainSilent {
    public static void init(Naming naming, String[] args) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	for(;;)       cpuManager.yield();
    }
}
