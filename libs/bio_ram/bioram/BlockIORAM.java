package bioram;

import jx.zero.*;
import jx.zero.debug.*;
import jx.timer.*;
import timerpc.*;

/**
 * Access to a block device.
 * Slow down sector access to simulate real disk device.
 * @author Michael Golm
 */
public class BlockIORAM implements jx.bio.BlockIO, Service {
    private final static int DEFAULT_CAPACITY = 5000;
    int capacity;
    Memory buffer;
    SleepManager sleepManager = new SleepManagerImpl();



    public BlockIORAM() { 
	this(DEFAULT_CAPACITY);
    }
    public BlockIORAM(int numberKiloBytes) { 
	capacity = numberKiloBytes;
	MemoryManager memMgr = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");
	buffer = memMgr.alloc(1024*capacity);
    }
    public BlockIORAM(Memory mem) {
	capacity = mem.size() >> 10;
	buffer = mem;
    }

    public static void init(Naming naming, String [] args) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	Debug.out = new jx.zero.debug.DebugPrintStream(new jx.zero.debug.DebugOutputStream(d));
	main(args);
    }

    public static void main(String [] args) {
	Naming naming = InitialNaming.getInitialNaming();
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	String bioName = args[0];
	final BlockIORAM bio = new BlockIORAM(20 *  1024);
	naming.registerPortal(bio, bioName);
	Debug.out.println("Block I/O device registered as "+bioName);
    }

    public int getCapacity() {
	return capacity;
    }

    public int getSectorSize() { return 512; }

    public void readSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
	if ((startSector + numberOfSectors)  > capacity) {
	    Debug.out.println("read attempted out of storage range: startSector="+startSector
			      +", numberOfSectors="+numberOfSectors
			      +", buf.size()="+buf.size()
			      +", capacity="+capacity);
	    throw new Error();
	}
	buf.copyFromMemory(buffer, startSector*512, 0, numberOfSectors*512);
	sleepManager.mdelay(3);
    }

    public void writeSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
	if ((startSector + numberOfSectors)  > capacity) {
	    Debug.out.println("write attempted out of storage range");
	    throw new Error();
	}
	buf.copyToMemory(buffer, 0, startSector*512, numberOfSectors*512);
	sleepManager.mdelay(3);
    }
}
