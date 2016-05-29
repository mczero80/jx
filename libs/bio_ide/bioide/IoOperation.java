package bioide;

import jx.zero.*;
import jx.zero.Debug;

/**
 * Base class for I/O operations
 * @author Michael Golm
 * @author Andreas Weissel
 */
abstract class IoOperation extends Operation {
    // possible states of an IoOperation
    public static final Object STATE_UNKNOWN = new Object();
    public static final Object STATE_RUNNING = new Object();
    public static final Object STATE_COMPLETED = new Object();

    protected Memory     buffer;      // data buffer
    protected int        offset;      // offset in buffer
    protected int        sector;      // start sector 
    protected int        count;       // number of sectors
    protected CPUState   cpuState;    // thread that waits for completion of this operation
    protected AtomicVariable state;   // current state of this operation

    private   boolean    synchronous; // synchronous/asynchronous operation

    public IoOperation(Memory buffer, int count, Controller controller, Drive drive, int sector, boolean synchronous) {
	super(controller, drive);
	this.synchronous = synchronous;
	this.offset      = 0;
	this.sector      = sector;
	this.buffer      = buffer;
	this.count       = count;
	if (count > buffer.size() / 512) throw new Error();
	state = Env.cpuManager.getAtomicVariable();

    }

    public void endOperation(boolean uptodate) {
	errors = 0;
	if (!uptodate)
	    errors = 1;
	controller.nextOperation();
        if( Env.verboseIO )
	    Debug.out.println("endOperation " + uptodate);
        if( Env.verboseIO )
	    Debug.out.println("endOperation fertig");
	state.atomicUpdateUnblock(STATE_COMPLETED, cpuState);
    }

    public void waitForCompletion() {
        if( Env.verboseIO )
	    Env.cpuManager.dump("waitForCompletion in IoOperation:", this);
	cpuState = Env.cpuManager.getCPUState();
        if( Env.verboseIO )
	    Env.cpuManager.dump("ctx in waitForCompletion:", cpuState);

	state.blockIfEqual(STATE_RUNNING);
    }



    /**
     * Fill controller registers with operation parameters (number of sectors, first sector...).
     */
    protected void ioInit() throws IDEException {
	int block = sector;

	controller.setLDHReg(drive.select);
	if (! controller.waitFor(Controller.STATUS_RDY, Controller.STATUS_BSY|Controller.STATUS_DRQ, 30)) { 
	    Debug.out.println("" + drive.name + ": Geraet nicht bereit");
	    endOperation(false);
	    throw new Error(); // return -1;
	}

	// Laufwerk selektieren
	controller.setCTLReg(drive.ctl);
	// Anzahl zu lesender/schreibender Sektoren
	if (count < 256)
	    controller.setCountReg((byte)(count & 0xff));
	else
	    controller.setCountReg((byte)0);

	if (drive.lba()) {  // LBA-Modus
	    controller.setSectorReg((byte)(block & 0xff));
	    controller.setLoCylReg((byte)((block >>= 8) & 0xff));
	    controller.setHiCylReg((byte)((block >>= 8) & 0xff));
	    controller.setLDHReg((byte)(((block >> 8) & 0x0f) | drive.select));
	    /* die LBA-Nummer wird folgendermassen codiert:
	       28 Bit: 0000HHHH CCCCCCCC cccccccc SSSSSSSS (Bit 0)
	       |--| |---------------| |------|
	       Kopf      Zylinder      Sektor
	    */
	} else {
	    int sect,head,cyl,spur; // unsigned int
      	    spur  = block / drive.sect;
	    sect  = block % drive.sect + 1;
	    // auf EINE Platte bezogen: pro Spur drive.sect Sektoren, Sektoren werden ab 1 gezaehlt

	    controller.setSectorReg((byte)(sect & 0xff));

	    head  = spur % drive.head;
	    cyl   = spur / drive.head;
	    // auf ALLE Platten bezogen: pro Zylinder drive.head Spuren
      
	    controller.setLoCylReg((byte)(cyl & 0xff));
	    controller.setHiCylReg((byte)(cyl>>8));
	    controller.setLDHReg((byte)(head | drive.select));
	    /* select.all == 1 L 1 D x x x x, mit L == 1 fuer LBA-Modus, 0 sonst,
	       D == 1 fuer Slave, 0 fuer Master, head == xxxx (0 bis 15) */
	}
    }
}
