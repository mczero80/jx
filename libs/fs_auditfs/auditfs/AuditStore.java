package auditfs;

import jx.bio.*;
import jx.zero.*;

public class AuditStore {
    BlockIO bio;
    int capacity;
    int sectorSize;
    int currentSector=0;
    MemoryManager memoryManager;
    Memory buf;
    int posInBuf;
    int numberOfSectorsToBuffer;
    Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");

    /**
     * @param bio block I/O device
     * @param append append to existing log
     * @param writeImmediately write each log to disk immediately
     * @param numberOfSectorsToBuffer maximum number of sectors that are buffered before log is written to disk (0 means do not buffer at all)
     */
    public AuditStore(Naming naming, BlockIO bio, boolean append, int numberOfSectorsToBuffer) {
	this.bio = bio;
	this.numberOfSectorsToBuffer = numberOfSectorsToBuffer;
	capacity = bio.getCapacity();
	sectorSize = bio.getSectorSize();
	memoryManager = (MemoryManager)naming.lookup("MemoryManager");
	buf = memoryManager.alloc(sectorSize);
	posInBuf=0;
	if (append) {
	    // find last entry
	    for (int i=1; i<capacity; i++) {
		bio.readSectors(i, 1, buf, true);
	    }
	}
    }


    public void writeAuditLog(String systemName, String domainName, String message) {
	writeAuditLog(new AuditRecord(clock, systemName,domainName, message));
    }

    private void writeAuditLog(AuditRecord r) {
	int size = r.getSize();
	if (posInBuf + size >  sectorSize) {
	    // flush old buffer and "create" new one
	    if (numberOfSectorsToBuffer>0) {
		bio.writeSectors(currentSector, 1, buf, true);
	    }
	    currentSector = (currentSector + 1) % capacity;
	    //if (currentSector == 0) currentSector++; // sector 0 is used for meta information
	    posInBuf = 0;
	}
	//Debug.out.println("WriteLog: sec="+currentSector+", pos="+posInBuf+", size="+size);
	r.addToBuffer(buf, posInBuf);
	posInBuf += size;
	if (numberOfSectorsToBuffer==0) {
	    bio.writeSectors(currentSector, 1, buf, true);
	}
    }

    public void erase() {
	buf.fill32(0, 0, sectorSize / 4);
	for (int i=0; i<capacity; i++) {
	    bio.writeSectors(i, 1, buf, true);
	}
    }

    public void dump() {
	for (int i=0; i<capacity; i++) {
	    bio.readSectors(i, 1, buf, true);
	    AuditRecord rec = new AuditRecord();
	    int pos = 0;
	    for(;;) {
		if (pos + 8 > sectorSize) break;
		if (buf.getLittleEndian32(pos) == 0 && buf.getLittleEndian32(pos+4) == 0) break;

		pos = rec.parseFromBuffer(buf, pos);
		Debug.out.println(Integer.toHexString(rec.time.high)
				  + "/" + Integer.toHexString(rec.time.low)
				  +" "+ rec.systemName +"/"+ rec.domainName+": "+rec.message);
	    }
	}
    }



    
}
