package test.fs;

import jx.bio.*;
import jx.zero.*;

public class ReadDisk {
    public static void main(String [] args) {
	Naming naming = (Naming)InitialNaming.getInitialNaming();
	BlockIO bio = (BlockIO)LookupHelper.waitUntilPortalAvailable(naming, args[0]);
	int capacity = bio.getCapacity();
	int sectorSize = bio.getSectorSize();

	int MAX_SECTORS = 1000;
	int ITERATIONS = 100;
	int NUMBER_COMPLETE_SECTORS = 1000*512;


	int numSec = NUMBER_COMPLETE_SECTORS;
	if (numSec > capacity) {
	    numSec = capacity;
	    Debug.out.println("Disk not large enough. Benchmark will only use "+numSec+" sectors.");
	}

	Debug.out.println("Capacity: "+capacity);
	Debug.out.println("SectorSize: "+sectorSize);
	MemoryManager memoryManager = (MemoryManager)naming.lookup("MemoryManager");
	Memory buf = memoryManager.alloc(sectorSize*MAX_SECTORS);

	// check whether read works
	Debug.out.println("read test ....");
	bio.readSectors(0, 1, buf, true);
	Debug.out.println("done.");

	Clock clock = (Clock)naming.lookup("Clock");
	CycleTime starttimec = new CycleTime();
	CycleTime endtimec = new CycleTime();
	CycleTime diff = new CycleTime();

	Debug.out.println("Read "+numSec+" sectors.");

	boolean readSecondTime = false;
	for(int nsec=1; nsec<MAX_SECTORS; nsec *= 2) {
	    if (! readSecondTime && nsec == 2) {
		nsec = 1;
		readSecondTime = true;
	    }
	    Debug.out.println("Request size "+nsec+" sectors.");
	    clock.getCycles(starttimec);
	    int iterations = numSec/nsec; 
	    for (int i=0; i<iterations; i += nsec) {
		bio.readSectors(i, nsec, buf, true);
	    }
	    clock.getCycles(endtimec);
	    clock.subtract(diff, endtimec, starttimec);
	    int time =  clock.toMilliSec(diff);
	    MyBigNumber n0 = new MyBigNumber(iterations);
	    n0 = n0.mul(nsec);
	    n0 = n0.mul(sectorSize);
	    n0 = n0.mul(1000); // millisec -> sec
	    n0 = n0.div(1024); // bytes -> kilobytes
	    n0 = n0.div(time);
	    int readrate = n0.toInt();
	    Debug.out.println("Time: "+time +" milliseconds, throughput: "+readrate+" KB/s");
	}

	/*
	// 100 sector reads 
	numSec /= 100;
	clock.getCycles(starttimec);
	for (int i=0; i<numSec; i++) {
	    bio.readSectors(i, 100, buf, true);
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	time =  clock.toMilliSec(diff);
	n0 = new MyBigNumber(numSec);
	n0 = n0.mul(100);
	n0 = n0.mul(sectorSize);
	n0 = n0.mul(1024);
	n0 = n0.div(time);
	n0 = n0.div(1000);
	readrate = n0.toInt();
	Debug.out.println("Time to read "+(numSec*100)+" sectors ("+(numSec*100*sectorSize)+" bytes) (100-sector-reads): "+time +" milliseconds, throughput: "+readrate+" KB/s");
	*/
    }
}
