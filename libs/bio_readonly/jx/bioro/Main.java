package jx.bioro;

import jx.zero.*;
import jx.zero.debug.*;
import jx.bio.BlockIO;

class Main {
    public static void main(String [] args) {
	Naming naming = InitialNaming.getInitialNaming();
	String bioNameRW = args[0];
	String bioNameRO = args[1];
	BlockIO bioRW = (BlockIO)LookupHelper.waitUntilPortalAvailable(naming, bioNameRW);
	BlockIO bioRO = new BioRO(bioRW);
	naming.registerPortal(bioRO, bioNameRO);
    }
    static class BioRO implements BlockIO, Service {
	BlockIO bio;
	BioRO(BlockIO bio) { 
	    this.bio=bio;
	}
	public int getCapacity() { return bio.getCapacity();}
	public int getSectorSize(){ return bio.getSectorSize();}
	public void readSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
	    bio.readSectors(startSector, numberOfSectors, buf, synchronous);
	}
	public void writeSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
	    throw new Error("Read-only block I/O device");
	}
    }
}


