package test.ide;

import jx.zero.*;
import jx.zero.debug.*;
import bioide.IDEDeviceImpl;
import bioram.BlockIORAM;
import jx.bio.BlockIO;
import timerpc.SleepManagerImpl;
import timerpc.TimerManagerImpl;
import bioide.IDEDeviceImpl;
import bioide.Drive;
import bioide.Partition;
import bioide.PartitionEntry;
import jx.bio.BlockIO;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

public class IDEDomain {
    Naming naming;

    public static void init(Naming naming) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	Debug.out.println("Domain IDEDomain speaking.");
	cpuManager.setThreadName("IDEDomain-Main");
	new IDEDomain(naming);
    }
    IDEDomain(final Naming naming) {
	this.naming = naming;	    

	//naming.registerPortal(new SleepManagerImpl(), "SleepManager");
	//naming.registerPortal(new TimerManagerImpl(), "TimerManager");

	//IDEDeviceImpl ide = new IDEDeviceImpl(null);
	IDEDeviceImpl ide = new IDEDeviceImpl();
	Drive[] drives = ide.getDrives();
	PartitionEntry[] partitions = drives[0].getPartitions();
	
	//	BlockIO bio = partitions[1];

	// final BlockIO portal = (BlockIO)naming.promoteDEP(partitions[2], "jx/bio/BlockIO");
      final BlockIO portal = partitions[2];
      
      // register as DEP
      naming.registerPortal(portal, "BlockIO");
      
    }
}
