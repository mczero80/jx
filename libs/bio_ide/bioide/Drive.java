package bioide;

import jx.zero.*;
import jx.bio.BlockIO;

/**
 * Represents an IDE drive.
 * @author Michael Golm
 * @author Andreas Weissel
 */
public class Drive implements BlockIO, Service {
    public  boolean     present;    // drive is present
    public  boolean     using_dma;  // drive uses DMA
    public  byte	select;	    // "normal" value of select register (IDE_OFF_LDH)
    public  byte	ctl;	    // "normal" value for IDE_OFF_CTL_REG
    public  short       head;       // "real" number of heads
    public  short       sect;	    // "real" number of sectors per track
    public  short       cyl;	    // "real" number of cylinders
    public  short       bios_head, bios_sect, bios_cyl;
    public  int         capacity;   // capacity in sectors
    public  DriveIdData id_data;    // drive info
    public  String      name;	    // drive name (linux), e.g. "hda"
    public  Controller  controller; // controller for this drive

    private byte        select_unit = 4; // Unit-Bit in select register (0: Master, 1: Slave)
    private byte        select_lba  = 6; // LBA-Bit in select register (0: no LBA, 1: LBA)
    private PartitionTable entries;
    private IDEDeviceImpl idedevice;

    public Drive(IDEDeviceImpl ide, Controller controller, int unit) {
	this.controller = controller;
	this.idedevice = ide;
	id_data = new DriveIdData();
	select = (byte)(Controller.LDH_DEFAULT | (unit << select_unit));
	ctl = Controller.CTL_EIGHTHEADS; // > 8 heads
	present = false;
	name = "hd";
	if (controller.index == 0) {
	    if (unit == 0) name += "a";
	    else name += "b";
	} else {
	    if (unit == 0) name += "c";
	    else name += "d";
	}
    }

    public boolean lba() {
	return ((select & (1 << select_lba)) > 0);
    }

    public void setLba(boolean value) {
	if (value)
	    select |= (1 << select_lba);
	else
	    select &= ~(1 << select_lba);
    }

    public int unit() {
	if ((select & (1 << select_unit)) > 0)
	    return 1;
	return 0;
    }


    /**
     * Get drive capacity, address schema (LBA or CHS), access mode (PIO or DMA) and
     * configure drive for fastest access mode
     */
    public void setup() throws IDEException {
	Operation operation;

	// Kapazitaet (in Sektoren) gemaess den aktuellen Geometrieeinstellungen berechnen
	capacity = cyl * head * sect;
	setLba(false); // lba auf 0 setzen

	// falls das Laufwerk LBA unterstuetzt, verwenden wir das:
	if ((id_data != null) && ((id_data.capability() & 2) > 0) && lbaCapacityIsOk()) {
	    if (id_data.lba_capacity() >= capacity) {
		cyl = (short)(id_data.lba_capacity() / (head * sect));
		capacity = id_data.lba_capacity();
		setLba(true); // lba auf 1 setzen
	    }
	}

	if (((head == 0) || (head > 16)) && !lba()) {
	    present = false;
	    return;
	}

	Debug.out.print("" + name + ": " + id_data.model().trim() + ", " + (capacity/2048) + "MB, ");
	if (lba())
	    Debug.out.print("LBA, ");
	if (bios_cyl < 0) Debug.out.print("CHS=" + (bios_cyl+65536)); else Debug.out.print("CHS=" + bios_cyl);
	if (bios_head < 0) Debug.out.print("/" + (bios_head+256)); else Debug.out.print("/" + bios_head);
	if (bios_sect < 0) Debug.out.println("/" + (bios_sect+256)); else Debug.out.println("/" + bios_sect);

	operation = new RecalibrateOperation(controller, this);
	operation.startOperation();
  
	if ((id_data != null) && ((id_data.capability() & 1) > 0) && idedevice.dmaSupported) {
	    // bit 0 von id_data.capability() = DMA-Unterstuetzung
	    // DMA  fuer alle Laufwerke aktivieren, die Multiword- oder Ultra-DMA unterstuetzen
	    try {
		operation = new EnableDMAOperation(controller, this, idedevice.ultraDmaSupported);
		operation.startOperation();
	    } catch(DMAException e) {
		operation = new EnablePIOOperation(controller, this);
		operation.startOperation();
	    }
	} else {
	    operation = new EnablePIOOperation(controller, this);
	    operation.startOperation();
	}

    }

    public PartitionEntry[] getPartitions() {
	if (entries==null) {
	    entries = new PartitionTable(this);
	    entries.dump();
	}
	return entries.getPartitions();
    }

    /**
     * Check whether LBA is necessary (drive > 8 GB) and if capacity is correct
     */
    private boolean lbaCapacityIsOk() {
	int lba_sects   = id_data.lba_capacity();
	int chs_sects   = id_data.cyls() * id_data.heads() * id_data.sectors();
	int _10_percent = chs_sects / 10;

	/* Sehr grosse Laufwerke (8 GB oder mehr) geben fuer CHS (max. 8 GByte) einen Schluesselwert an,
	   damit der Treiber sie als solche erkennt und LBA verwendet; die richtige Kapazitaet steht in id_data.lba_capacity() */
	if ((id_data.lba_capacity() >= 16514064) && /* 16383 * 16 * 63 == 8 GByte */
	    (id_data.cyls() == 16383) && (id_data.heads() == 16) && (id_data.sectors() == 63)) {
	    id_data.cyls((short)(lba_sects / (16 * 63))); // Zylinderzahl korrigieren
	    return true;       // lba_capacity ist die einzige Moeglichkeit
	}

	// falls sich lba_capacity und CHS-Sektorenzahl um weniger als 10% unterscheiden, ist der Wert in Ordnung
	if ((lba_sects - chs_sects) < _10_percent)
	    return true;	// lba_capacity ist in Ordnung
	
	// bei manchen Laufwerken sind die Woerter vertauscht
	lba_sects = (lba_sects << 16) | (lba_sects >> 16);
	if ((lba_sects - chs_sects) < _10_percent) {
	    id_data.lba_capacity(lba_sects);	// richtigstellen
	    return true;	// lba_capacity ist (jetzt) in Ordnung
	}

	return false;	// der lba_capacity-Wert ist nicht in Ordnung
    }

    /**
     * Checks drive parameters (no of sectors, heads, cylinders) and corrects them if necessary.
     */
    public void identify() throws IDEException {
	Debug.out.println("DRIVE IDENTIFY: "+name);
	IdentifyOperation operation = new IdentifyOperation(id_data, controller, this);
	operation.startOperation();
  
	if (!present) {
	    present = true;
	    cyl  = bios_cyl  = id_data.cyls();
	    head = bios_head = id_data.heads();
	    sect = bios_sect = id_data.sectors();
	}
	Debug.out.println("DRIVE IDENTIFIED: "+name+"; cyl="+cyl+", head="+head+", sect="+sect);

	// Umsetzung der logischen Geometrie durch das Laufwerk ueberpruefen
	if (((id_data.field_valid() & 1) > 0) && (id_data.cur_cyls() > 0) && (id_data.cur_heads() > 0)
	    && (id_data.cur_heads() <= 16) && (id_data.cur_sectors() > 0)) {
	    cyl  = id_data.cur_cyls();
	    head = id_data.cur_heads();
	    sect = id_data.cur_sectors();
	}

	// Die physikalische Geometrie uebernehmen, falls die bisherige keinen Sinn macht
	if ((head == 0 || head > 16) && id_data.heads() > 0 && id_data.heads() <= 16) {
	    cyl  = id_data.cyls();
	    head = id_data.heads();
	    sect = id_data.sectors();
	}

	// Die Zylinderzahl korrigieren, falls der Bios-Wert zu klein ist
	if (sect == bios_sect && head == bios_head) {
	    if (cyl > bios_cyl)
		bios_cyl = cyl;
	}

	Debug.out.println("DRIVE OK: "+name+"; cyl="+cyl+", head="+head+", sect="+sect);
    }

    /**
     * Read data from disk.
     */
    public void readSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) { 
	Operation operation;
        if( Env.verboseDR ) Debug.out.println("readSectors - start");
	
	if (using_dma && controller.buildDMATable(buf, 512 * numberOfSectors)) {
	    operation = new DMAOperation(buf, numberOfSectors, controller, this, startSector, true, true/*read*/);
	} else {
	    operation = new ReadOperation(buf, numberOfSectors, controller, this, startSector, true);
	}

	controller.queueOperation(operation, false);
	if (synchronous) operation.waitForCompletion();
    }

    /** 
     * Write data to disk.
     */
    public void writeSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous ) { 
	Operation operation;
        if( Env.verboseDR )  Debug.out.println("writeSectors - start");
	
	if (using_dma && controller.buildDMATable(buf, 512 * numberOfSectors)) {
	    operation = new DMAOperation(buf, numberOfSectors, controller, this, startSector, true, false /*write*/);
	} else {
	    operation = new WriteOperation(buf, numberOfSectors, controller, this, startSector, true);
	}

	controller.queueOperation(operation, false);
	if (synchronous) operation.waitForCompletion();
    }

    public int getCapacity() {
	return capacity;
    }
    public int getSectorSize() { return 512; }
}
