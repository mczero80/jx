package bioide;

import jx.zero.*;
import jx.zero.Debug;

/**
 * Read/Write BM-DMA
 * Transfer direction (read/write) specified during creation of operation object.
 * @author Michael Golm
 * @author Andreas Weissel
 */
class DMAOperation extends IoOperation {
    private static final byte CMD_READ_DMA  = (byte)0xc8;   // or 0xc9 (no retransmission)
    private static final byte CMD_WRITE_DMA = (byte)0xca;   // or 0xcb (no retransmission)
    private boolean read;

    public DMAOperation(Memory buffer, int count, Controller controller, Drive drive, int sector, boolean synchronous, boolean read) {
	super(buffer, count, controller, drive, sector, synchronous);
	this.read = read;
    }

    public void startOperation() throws IDEException {
	state.set(STATE_RUNNING);

	ioInit();

	if (controller.dmatable == null)
	    throw new IDEException("non-existent DMA table");

	Env.ports.outl(controller.dma_base+4, controller.dmatable.getStartAddress());  // DMA table

	if (read)
	    Env.ports.outb(controller.dma_base, (byte)(1 << 3)); // read operation
	else
	    Env.ports.outb(controller.dma_base, (byte)0); // write operation

	// reading/writing vertauscht (Reihenfolge Peripherie -> Speicher), eigentlich: read == 0
	Env.ports.outb(controller.dma_base+2, (byte)(Env.ports.inb(controller.dma_base+2)|(byte)0x06));

	// durch das Setzen der Bits Interrupt Status und DMA Error auf 1 werden sie geloescht (auf 0 gesetzt)
	controller.setTimeout(10, 0); // 10 sec

	if (read)
	    controller.setCommandReg(CMD_READ_DMA);  
	else
	    controller.setCommandReg(CMD_WRITE_DMA);

	Env.ports.outb(controller.dma_base, (byte)(Env.ports.inb(controller.dma_base)|(byte)1));    // start DMA operation
    }

    public void stopOperation() {
	Env.ports.outb(controller.dma_base, (byte)(Env.ports.inb(controller.dma_base) & (byte)~1)); // abort DMA operation
	endOperation(false); // richtig?
    }

    public void handler() {
	byte stat, dma_stat;
	int dma_base = controller.dma_base;

	if (Env.verboseIO) Debug.out.println("DMAOperation.handler()");

	dma_stat = Env.ports.inb(dma_base+2);  // DMA-Status auslesen
	Env.ports.outb(dma_base, (byte)(Env.ports.inb(dma_base) & (byte)~1));  // DMA-Operation beenden
	stat = controller.getStatus();     // Status des Laufwerks abfragen
	if ((stat & (DRIVE_READY | BAD_W_STAT | Controller.STATUS_DRQ)) == DRIVE_READY) {
	    if ((dma_stat & 7) == 4) {	   // DMA-Status ueberpruefen
		// alle Lese/Schreib-Operationen beendet
		endOperation(true);
		return;
	    }
	    Debug.out.println("" + drive.name + ": fehlerhafter DMA-Status: " + dma_stat);
	}
	controller.ideError(drive, "dma interrupt", stat);
	throw new Error();
    }
}
