package jx.emulation;

import jx.zero.*;
import java.io.*;
import jx.bio.BlockIO;

class BlockIOFile implements BlockIO {
    RandomAccessFile file;
    final static int SECTOR_SIZE = 512;
    byte[] b = new byte[SECTOR_SIZE];
    public BlockIOFile(String filename, int sectors) {
	try {
	    //File f = new File(filename);
	    //f.
	    file = new RandomAccessFile(filename, "rw");
	    file.seek(SECTOR_SIZE * (sectors-1));
	    file.write(b);
	} catch(IOException e) {
	    e.printStackTrace();
	    throw new Error();
	}
    }

    public int getCapacity() { 
	try {
	    return (int)(file.length()/SECTOR_SIZE);
	} catch(IOException e) {
	    e.printStackTrace();
	    throw new Error();
	}
    }

    public int getSectorSize() { return 512; }

    public void readSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
	try {
	file.seek(SECTOR_SIZE * startSector);
	for(int i=0; i<numberOfSectors; i++) {
	    file.read(b);
	    buf.copyFromByteArray(b, 0, i*SECTOR_SIZE, SECTOR_SIZE);
	}
	} catch(Exception e) {
	    throw new Error();
	}
    }

    public void writeSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
	try {
	file.seek(SECTOR_SIZE * startSector);
	for(int i=0; i<numberOfSectors; i++) {
	    buf.copyToByteArray(b, 0,  i*SECTOR_SIZE, SECTOR_SIZE);
	    file.write(b);
	}
	} catch(Exception e) {
	    throw new Error();
	}
    }


}
