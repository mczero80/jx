package bioide;

import jx.zero.*;
import jx.zero.debug.*;
import java.util.Vector;

/**
 * Partition table of a drive.
 * @author Michael Golm
 * @author Andreas Weissel
 */
class PartitionTable {
    private PartitionEntry[] partitions;
    private Drive drive;

    private static final byte  UNUSED_PARTITION   = 0x0;
    private static final byte  DOS_EXTENDED_PARTITION   = 0x05;
    private static final byte  LINUX_SWAP_PARTITION = (byte)0x82;
    private static final byte  LINUX_PARTITION = (byte)0x83;
    private static final byte  LINUX_EXTENDED_PARTITION = (byte)0x85;
    private static final byte  WIN95_FAT32_PARTITION = 0x0b;
    private static final byte  WIN95_EXTENDED_PARTITION = 0x0f;
    private static final short PCBIOS_BOOTMAGIC         = (short)0xaa55;

    PartitionTable(Drive drive) {
	this.drive = drive;
	readPartitionTable();
    }

    public PartitionEntry[] getPartitions() {
	return partitions;
    }

    /**
     * liefert die Kapazit&auml;t (die Anzahl der Sektoren) der angegebenen
     * Partition zurück
     *
     * @param  part die Nummer der Partition (0 f&uuml;r die gesamte Festplatte)
     * @return die Anzahl der Sektoren der Partition (-1 falls die angegebene Partition nicht existiert)
     */
    public int getPartitionSize(int part) {
	//if (isPresent(part) == false)	    return -1;
	return partitions[part].size;
    }

    /**
     * liest die Partitionstabelle des Laufwerks und ermittelt die vorhandenen Partitionen.
     */
    private void readPartitionTable() {
	if (Env.verbosePT) Debug.out.println("READPARTIONTABLE");
	Vector entries=new Vector();
	MBRData mbr_data;
	PartitionEntry part_entry;
	int start_ext_sector, ext_sector, old_ext_sector, found = 0, ext_found = 0;

	part_entry = new PartitionEntry(drive, 0, drive.getCapacity(), false, -1);
	entries.addElement(part_entry);
	
	Memory buffer = Env.memoryManager.allocAligned(512, 8);
	drive.readSectors(0, 1, buffer, true);

	mbr_data = new MBRData(buffer);
	if (mbr_data.magic() != PCBIOS_BOOTMAGIC) {
	    Debug.out.println("readPartitionTable: Bootsektor fehlerhaft");
	    Debug.out.println("  magic: "+mbr_data.magic()+", expected: "+PCBIOS_BOOTMAGIC);
	    Dump.xdump(buffer);
	    throw new Error();
	}

	for (int i = 0; i < 4; i++) {
	    PartitionData part_data = new PartitionData(buffer, 446+i*16);
	    if ((isExtended(part_data.os_indicator()) == false) && (part_data.os_indicator() != 0) && (part_data.length_in_sectors() > 0)) {
		part_entry = new PartitionEntry(drive, part_data.start_sector(), part_data.length_in_sectors(), true, part_data.os_indicator());
		entries.addElement(part_entry);
		if (Env.verbosePT) Debug.out.println("found partition");
		found++;
	    }
	}
	for (int i = 0; i < 4; i++) {
	    PartitionData part_data = new PartitionData(buffer, 446+i*16);
	    if (Env.verbosePT) Debug.out.println("entry " + i + ": os = " + osName(part_data.os_indicator()) + ", length = " + part_data.length_in_sectors());
	    if (isExtended(part_data.os_indicator()) && (part_data.length_in_sectors() > 0)) { // erweiterte Partition
		start_ext_sector = part_data.start_sector();
		ext_sector = part_data.start_sector();
		if (ext_sector > 0) {
		    part_entry = new PartitionEntry(drive, ext_sector, part_data.length_in_sectors(), false, part_data.os_indicator());
		    entries.addElement(part_entry);
		    found++;
		    for (int j = found; j < 4; j++)
			entries.addElement(null);
		}
		if (Env.verbosePT) Debug.out.println("extended partition found");
		while ((ext_sector > 0) && (ext_found < 5)) {
		    old_ext_sector = ext_sector;
		    if (ext_sector >= drive.getCapacity())
			break;
		    if (Env.verbosePT) Debug.out.println("loading block " + ext_sector);
		    drive.readSectors(ext_sector, 1, buffer, true);
		    mbr_data = new MBRData(buffer);
		    if (mbr_data.magic() != PCBIOS_BOOTMAGIC)
			break;
		    if (Env.verbosePT) Debug.out.println("extended partition found");
		    for (int j = 0; j < 2; j++) {
			part_data = new PartitionData(buffer, 446 + j*16);
			if ((!isExtended(part_data.os_indicator())) && (part_data.os_indicator() != 0) &&
			    (part_data.length_in_sectors() > 0)) {
			    part_entry = new PartitionEntry(drive, ext_sector + part_data.start_sector(), part_data.length_in_sectors(), true, part_data.os_indicator());
			    //part_entry = new PartitionEntry(start_ext_sector + part_data.start_sector(), part_data.length_in_sectors());
			    entries.addElement(part_entry);
			    if (Env.verbosePT) Debug.out.println("found partition");
			    ext_found++;
			}
		    }

		    for (int j = 0; j < 4; j++) {
			part_data = new PartitionData(buffer, 446 + j*16);
			if (isExtended(part_data.os_indicator()) && (part_data.length_in_sectors() > 0)) {
			    //ext_sector += part_data.start_sector();
			    ext_sector = start_ext_sector + part_data.start_sector();
			    break;
			}
		    }
		    if (ext_sector == old_ext_sector)
			break;
		}
	    }
	}

	/* nach Startsektor sortieren */
	/*
	  for (int i = 1; i < found+1; i++) {
	  for (int j = i+1; j < found+1; j++)
	  ((PartitionEntry)entries.elementAt(i)).swapWithPartition((PartitionEntry)entries.elementAt(j));
	  }
	  for (int i = 0; i < ext_found; i++) {
	  for (int j = i+1; j < ext_found; j++)
	  ((PartitionEntry)entries.elementAt(i+5)).swapWithPartition((PartitionEntry)entries.elementAt(j+5));
	  }
	*/
	partitions = new PartitionEntry[entries.size()];
	entries.copyInto(partitions);
    }
    public void dump() {
	Debug.out.println("Partitiontable:");
	Debug.out.println(alignString("Number", 11) 
			  + "   "
			  + alignString("Start", 11)
			  + "   "
			  + alignString("End", 11)
			  + "   " 
			  + alignString("Size", 11)
			  + "   "
			+ alignString("OS", 11));
	for (int part = 1; part < partitions.length; part++) {
	    PartitionEntry entry = (PartitionEntry)partitions[part];
	    if (part == 5)
		Debug.out.print("< ");
	    if (entry != null)
		Debug.out.println(alignString(Integer.toString(part), 11) 
				  + "   "
				  + alignString(Integer.toString(entry.start), 11)
				  + "   "
				  + alignString(Integer.toString(entry.start+entry.size-1), 11)
				  + "   " 
				  + alignString(Integer.toString(entry.size), 11)
				  + "   "
				  + alignString(osName((byte)entry.os), 11));
	}
	if (partitions.length > 4)
	    Debug.out.print(">");
	Debug.out.println("");
    }
    
    
    /** Ermittelt, ob es sich um eine erweiterte Partition handelt. */
    private static boolean isExtended(byte os_indicator) {
	if ((os_indicator == DOS_EXTENDED_PARTITION) ||
	    (os_indicator == LINUX_EXTENDED_PARTITION) ||
	    (os_indicator == WIN95_EXTENDED_PARTITION))
	    return true;
	return false;
    }
    /** Ermittelt, ob es sich um eine erweiterte Partition handelt. */
    public static String osName(byte os_indicator) {
	switch(os_indicator) {
	case UNUSED_PARTITION: return "unused";
	case DOS_EXTENDED_PARTITION: return "DOS extended";
	case LINUX_EXTENDED_PARTITION: return "Linux extended";
	case LINUX_PARTITION: return "Linux";
	case LINUX_SWAP_PARTITION: return "Linux Swap";
	case WIN95_EXTENDED_PARTITION: return "Win95 extended";
	case WIN95_FAT32_PARTITION: return "Win95 FAT32";
	}
	return "unknown ("+os_indicator+")";
    }

    private String alignString(String value, int length) {
	String tmp1 = new String();
	int leer = length - value.length();
	for (int i = 0; i < leer; i++)
	    tmp1 += " ";
	return (tmp1 + value);
    }

}

/**
 * Access to Master Boot Record (MBR)
 */
class MBRData {
    Memory mem;
    public MBRData(Memory mem) {
	this.mem = mem;
    }
    public short magic() { return mem.get16(255); }
}

/**
 * Access one entry in the partion table 
 */
class PartitionData {
    byte os_indicator;
    int  start_sector;
    int length_in_sectors;
    public PartitionData(Memory mem, int offset) {
	os_indicator = mem.get8(offset+4);
	start_sector = mem.getLittleEndian32(offset+8);
	length_in_sectors = mem.getLittleEndian32(offset+12);
    }
    public byte os_indicator()      { return os_indicator; }
    public int  start_sector()      { return start_sector; }
    public int  length_in_sectors() { return length_in_sectors; }
}

