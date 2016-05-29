package metaxa.os.devices.net;

import metaxa.os.*;
import jx.zero.*;

class UpdListEntry extends ListEntry {
 
    // linking of the Upds
    private UpdListEntry Next;
    private UpdListEntry Previous;
 
    // actual memory objects
    private Memory UpdMemory;
    private Memory StoreMem;
    
    // the physical address
    private int UPDPhysicalAddress; 

    // constructor
    public UpdListEntry(MemoryManager memMgr) {
	UpdMemory = memMgr.allocAligned(16, 8);
	if (UpdMemory == null) {
	    System.out.println("ERROR: couldn´t allocate memory for UpdListEntry!");
	    return;
	}	      
	UPDPhysicalAddress = UpdMemory.getStartAddress();

	StoreMem = memMgr.alloc(AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE);
	if (StoreMem == null) {
	    System.out.println("ERROR: couldn´t allocate memory for Storage!");
	    return;
	}
	// we initialize the Up Next pointer with 0, indicating that this is the last packet 
	// the actual linking is done in the constructor of D3C905, so here we just set it to 0 for completeness reasons
	UpdMemory.set32(0, 0);
	// the Up Packet Status is set to 0 - after an upload we can read this value as the NIC writes the UpPktStatus register to this entry
	UpdMemory.set32(1, 0);
	// enter the start address of the first memory fragment (also the last)
	UpdMemory.set32(2, StoreMem.getStartAddress());
	// enter the size of the first memory fragment and indicate that this is the last fragment (set bit [31])
	UpdMemory.set32(3, (StoreMem.size() | 0x80000000)); 
    }
  // for inserting a new receive memory object
  public void StoreMem(Memory mem) {
    StoreMem = mem;
    UpdMemory.set32(2, mem.getStartAddress());
    UpdMemory.set32(3, (mem.size() | 0x80000000)); 
  }
    public void UpPacketStatus(int uppacketstatus) {
        UpdMemory.set32(1, uppacketstatus);
    }
    public void UpNextPointer(int upnextpointer) {
	UpdMemory.set32(0, upnextpointer);
    }
    public void Next(UpdListEntry next) {
	Next = next;
    }
    public void Previous(UpdListEntry previous) {
	Previous = previous;
    }
    
    public int UpPacketStatus() {
	return UpdMemory.get32(1);
    }
    public Memory UpdMemory() {
	return UpdMemory;
    }
    public Memory StoreMem() {
	return StoreMem;
    }
    public UpdListEntry Next() {
	return Next;
    }
    public UpdListEntry Previous() {
	return Previous;
    }
    public int UPDPhysicalAddress() {
	return UPDPhysicalAddress;
    }

   public String toString() {

	System.out.println("-------------UPD_Eintrag---------------");
	System.out.println("UpPacketStatus: " + Integer.toHexString(UpdMemory.get32(1)));
	System.out.println("UPDPhysicalAddress: " +  Integer.toHexString(UPDPhysicalAddress));
	System.out.println("------- Ende der internen Variablen ---------");
	if (UpdMemory != null) {
	    System.out.println("UPDMem(0): " + UpdMemory.get32(0));
	    System.out.println("UPDMem(4): " + UpdMemory.get32(1));
	    System.out.println("UPDMem(8): " + UpdMemory.get32(2));
	    System.out.println("UPDMem(12): " + UpdMemory.get32(3));
	    System.out.println("StartAddresse UPDMem: " + UpdMemory.getStartAddress() + "  Groesse: " + UpdMemory.size());
	}
	else {
	    System.out.println("UPDMem ist null!!!");
	}
	if (StoreMem != null) {
	    System.out.println("StoreMem -> Groesse: " + StoreMem.size() + "  StartAdresse: " + StoreMem.getStartAddress());
	}
	else {
	  System.out.println("StoreMem ist null!!!");
	}
	System.out.println("------------- ENDE -> UPD_Eintrag <- ENDE ---------------");

    	// fake return
	return "";
   }

    
} 
