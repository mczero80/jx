package test.ide;

import jx.zero.*;
import jx.zero.debug.*;

import bioide.IDEDeviceImpl;
import bioide.Drive;
import bioide.Partition;
import jx.bio.BlockIO;
import timerpc.SleepManagerImpl;
import timerpc.TimerManagerImpl;


public class Main {

    public static boolean commonTest(Naming naming, BlockIO bio, boolean readOnly) {
	MemoryManager memoryManager = (MemoryManager)naming.lookup("MemoryManager");
	Memory buffer = memoryManager.allocAligned(1024, 4);

	Debug.out.println("Capacity: " + bio.getCapacity());
	for(int i=0; i<50; i++) {
	    Debug.out.println("nr:" + i);
	    bio.readSectors(i, 2, buffer, true);
	}
	Dump.xdump(buffer, 0, 256);
	for(int i=0; i<buffer.size(); i++) {
	    buffer.set8(i, (byte)(i &0xff));
	}
	if (! readOnly) bio.writeSectors(0, 2, buffer, true);
	Debug.out.println("write done.");
	// read in the sectors
	for(int i=0; i<buffer.size(); i++) {
	    buffer.set8(i, (byte)0);
	}
	bio.readSectors(0, 2, buffer, true);
	Dump.xdump(buffer, 0, 256);

	int cap = bio.getCapacity();
	
	for(int i=1; i<20; i++) {
	    throughput(bio, memoryManager, i);
	}
	Debug.out.println("throughput test finished.");

	for(int i=0; i<cap; i++) {
	    Debug.out.println("nr:" + i);
	    bio.readSectors(i, 1, buffer, true);
	}

	Debug.out.println("read all test finished.");

	return true;
	
    }
    /** 
     * @param nsec number of sectors per read request
     */
    static void throughput(BlockIO bio, MemoryManager memoryManager, int nsec) {
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	Memory buffer = memoryManager.allocAligned(512*nsec, 4);

	int starttime = clock.getTimeInMillis();
	for(int i=0; i<100; i++) {
	    //Debug.out.println("nr:" + i);
	    bio.readSectors(i, nsec, buffer, true);	    
	}
	int stoptime = clock.getTimeInMillis();
	Debug.out.println("   Time for 100 "+(512*nsec)+" byte blocks (ms): "+(stoptime-starttime));
	Debug.out.println("   Throughput (KByte/s): "+(100*(512*nsec)/(stoptime-starttime)));
    }
    
    public static void main(String[] args) {
	Naming naming = InitialNaming.getInitialNaming();

	//naming.registerPortal(new SleepManagerImpl(), "SleepManager");
	//naming.registerPortal(new TimerManagerImpl(), "TimerManager");

	//IDEDeviceImpl ide = new IDEDeviceImpl(null);
	IDEDeviceImpl ide = new IDEDeviceImpl();
	Drive[] drives = ide.getDrives();
	Partition[] partitions = drives[0].getPartitions();
	
	//BlockIO bio = partitions[1];
	BlockIO bio = drives[0];
	
	boolean ro=true;
	commonTest(naming, bio, ro);
    }
}
