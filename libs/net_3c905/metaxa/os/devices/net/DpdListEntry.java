package metaxa.os.devices.net;
import metaxa.os.*;
import jx.zero.*;
import jx.buffer.multithread.Buffer2;

class DpdListEntry extends ListEntry {
    
    // linking of the Dpds
    private DpdListEntry Next;
    private DpdListEntry Previous;
    
    // the actual memory-objects
    private Memory DpdMemory;
    private Memory storeMem;
  
    private int DPDPhysicalAddress;
    private int PacketLength;  
    private int addr;

    Buffer2 buffer; /* the buffer that is related to this memory */

    // constructor 
    public DpdListEntry(MemoryManager memMgr) {
	// allocate 16 byte of memory, aligned on a 8 byte boundary
	DpdMemory = memMgr.allocAligned(16, 8);
	if (DpdMemory == null) {
	    Debug.out.println("ERROR: couldn´t allocate memory for DpdListEntry!");
	    return;
	}
	DPDPhysicalAddress = DpdMemory.getStartAddress();
	storeMem = null;
	PacketLength = -1;
	addr = -1;
    }

    public void FrameStartHeader(int framestartheader) {
	DpdMemory.set32(1, framestartheader);
    }
   
    public void StoreMem(Memory mem, int offset, int len) {
	storeMem = mem;
	addr = mem.getStartAddress() + offset;
	DpdMemory.set32(2, addr);
	DpdMemory.set32(3, (len | 0x80000000));
	PacketLength = len;
    }

    public void StoreMem(Memory mem) {
	storeMem = mem;
	DpdMemory.set32(2, mem.getStartAddress());
	addr = mem.getStartAddress();
	DpdMemory.set32(3, (mem.size() | 0x80000000));
	PacketLength = mem.size();
    }


    public void Next(DpdListEntry next) {
	Next = next;
    }
    public void Previous(DpdListEntry previous) {
	Previous = previous;
    }
    public void DownNextPointer(int dnp) {
	DpdMemory.set32(0, dnp);
    }
    public boolean clear_StoreMem() {
	storeMem = null;
	DpdMemory.set32(2, 0);
	DpdMemory.set32(3, 0);
	return true;
    }
   
    public int FrameStartHeader() {
	return DpdMemory.get32(1);
    }  
    public Memory DpdMemory() {
	return DpdMemory;
    }   
    public Memory StoreMem() {
	return storeMem;
    }
    public DpdListEntry Next() {
	return Next;
    }
    public DpdListEntry Previous() {
	return Previous;
    }
    public int DPDPhysicalAddress() {
	return DPDPhysicalAddress;
    }
    public int PacketLength() {
	return PacketLength;
    }

    public String toString() {

	Debug.out.println("-------------DPD_Eintrag---------------");
	Debug.out.println("FrameStartHeader: " + Integer.toHexString(DpdMemory.get32(1)));
	Debug.out.println("DPDPhysicalAddress: " +  Integer.toHexString(DPDPhysicalAddress));
	Debug.out.println("PacketLength: " + Integer.toHexString(PacketLength));
	Debug.out.println("DownNextPointer: " + Integer.toHexString(DpdMemory.get32(0x0)));
	Debug.out.println("------- Ende der internen Variablen ---------");
	if (DpdMemory != null) {
	    Debug.out.println("DPDMem(0): " + DpdMemory.get32(0));
	    Debug.out.println("DPDMem(4): " + DpdMemory.get32(1));
	    try {
		BitPosition bit = new BitPosition();
		if (bit.isSet(DpdMemory.get32(1), 30))
		    Debug.out.println("FEHLER: TYPE 1 FORMAT");
		else
		    Debug.out.println("ALLES OK: TYPE 0 FORMAT");
	    }
	    catch (Exception e){}
	    Debug.out.println("DPDMem(8): " + DpdMemory.get32(2));
	    Debug.out.println("DPDMem(12): " + DpdMemory.get32(3));
	    Debug.out.println("StartAddresse DPDMem: " + DpdMemory.getStartAddress() + "  Groesse: " + DpdMemory.size());
	}
	else {
	    Debug.out.println("DPDMem ist null!!!");
	}
	if (storeMem != null) {
	    Debug.out.println("storeMem -> Groesse: " + storeMem.size() + "  StartAdresse: " + storeMem.getStartAddress());
	}
	else {
	  Debug.out.println("storeMem ist null!!!");
	}
	Debug.out.println("------------- ENDE -> DPD_Eintrag <- ENDE ---------------");

	// fake return
	return "";
    }

} 
