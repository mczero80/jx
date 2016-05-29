package jx.bioemul;

import jx.zero.*;
import jx.zero.debug.*;
import jx.bio.BlockIO;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

class Main {
    public static void main(String [] args) {
	Naming naming = InitialNaming.getInitialNaming();
	String bioName = args[0];
	DiskEmulation emu = (DiskEmulation)naming.lookup("DiskEmulation");
	BlockIO bio = new BioEmu(emu);
	// register as DEP
	naming.registerPortal(bio, bioName);
	Debug.out.println("Block I/O device registered as "+bioName);
    }
    public static void init(Naming naming, String [] args) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	Debug.out.println("IDEDomain speaking.");
	
	String bioName = args[0];

	DiskEmulation emu = (DiskEmulation)naming.lookup("DiskEmulation");
	BlockIO bio = new BioEmu(emu);
	
	// register as DEP
	naming.registerPortal(bio, bioName);
	Debug.out.println("Block I/O device registered as "+bioName);
	
    }
    static class BioEmu implements BlockIO, Service {
	DiskEmulation disk;
	BioEmu(DiskEmulation disk) { 
	    if (disk == null) throw new Error("NO DISKEMULATION AVAILABLE");
	    this.disk=disk;
	}
	public int getCapacity() { return disk.getCapacity();}
	public int getSectorSize(){ return disk.getSectorSize();}
	public void readSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
	    disk.readSectors(startSector, numberOfSectors, buf, synchronous);
	}
	public void writeSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
	    disk.writeSectors(startSector, numberOfSectors, buf, synchronous);
	}
    }
}


