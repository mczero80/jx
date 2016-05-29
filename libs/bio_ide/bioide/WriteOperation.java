package bioide;

import jx.zero.*;

/**
 * Write using PIO.
 * @author Michael Golm
 * @author Andreas Weissel
 */
class WriteOperation extends IoOperation {
    private static final byte CMD_WRITE = (byte)0x30;   // Daten schreiben

    public WriteOperation(Memory buffer, int count, Controller controller, Drive drive, int sector, boolean synchronous) {
	super(buffer, count, controller, drive, sector, synchronous);
    }

    public void startOperation() throws IDEException {
	ioInit();

	/* Sektoren schreiben:
	   IRQ14 wird immer dann vom Controller aktiviert, wenn er Daten von der CPU erwartet,
	   ausser beim ersten Sektor, der sofort uebergeben wird. Wurden alle Sektoren geschrieben,
	   also beim Erreichen der Ergebnisphase, aktiviert der Controller ebenfalls IRQ14. Die
	   Zahl der Hardware-Interrupts stimmt also mit der Zahl der geschriebenen Sektoren ueberein.
	*/

	if (sector < 100) {
	    Debug.out.println("Write with sector < 100, aborting.");
	    endOperation(false);
	    throw new IDEException("Write with sector < 100, aborting.");
	}

	controller.setCommandReg(CMD_WRITE);
	if (! controller.waitFor(DATA_READY, BAD_W_STAT, 50)) { 
	    Debug.out.println("Drive not ready.");
	    endOperation(false);
	    throw new IDEException("Drive not ready.");
	}

	// Kontroller schickt Interrupt, sobald er die Daten uebernommen hat
	controller.setTimeout(10, 0); // 10 sec
	controller.outputData(buffer, 0, SECTOR_LONGS);

	// neu:
	if (true) {
	while (count > 0) {
	    int timeout = 100; // 5sec
	    do {
		if (timeout <= 0) {// Timeout
		    Debug.out.println("Timeout");
		    throw new IDEException("Timeout");
		}
		Env.sleepManager.mdelay(50);
		timeout--;
	    } while (controller.statusBusy());
	    /* Wenn die CPU das Statusregister liest, wird automatisch eine eventuell
	       anhaengige Interrupt-Anforderung - im PC ueber IRQ14 - storniert.
	    */
	    
	    Env.sleepManager.mdelay(50); 
	    
	    if ((controller.getStatus() & (DRIVE_READY | BAD_W_STAT)) != DRIVE_READY) {
		controller.getStatus();
		Debug.out.println("Interrupt, but drive not ready");
		throw new IDEException("Interrupt, but drive not ready");
	    }
	    
	    handler();
	}
	}

    }

    public void handler() {
	byte stat = controller.getStatus();
	if ((stat & (DRIVE_READY | BAD_W_STAT)) == DRIVE_READY) {
	    if ((count == 1) ^ ((stat & Controller.STATUS_DRQ) != 0)) { // Laufwerk nicht bereit
		sector++;
		offset += 128; //512;
		errors = 0;
		count--;
		if (count <= 0)
		    endOperation(true);
		else {
		    controller.setTimeout(10, 0); // 10 sec
		    controller.outputData(buffer, offset, SECTOR_LONGS);
		    state.set(STATE_COMPLETED);
		}
		return;
	    } else
		Debug.out.println("Laufwerk nicht bereit"); // debug
	}
	controller.ideError(drive, "write interrupt", stat);
    }
}
