package bioide;

import jx.zero.*;

/**
 * Read using PIO.
 * @author Michael Golm
 * @author Andreas Weissel
 */
class ReadOperation extends IoOperation {
    private static final byte CMD_READ = (byte)0x20;   // Daten lesen

    public ReadOperation(Memory buffer, int count, Controller controller, Drive drive, int sector, boolean synchronous) {
	super(buffer, count, controller, drive, sector, synchronous);
    }

    public void startOperation() throws IDEException {
	state.set(STATE_RUNNING);
	
	ioInit();

	/* Sektoren lesen:
	   IRQ14 wird immer dann aktiviert, wenn die CPU einen Sektor lesen kann. Im
	   Gegensatz zu CMD_WRITE wird hier kein Interrupt zu Beginn der Ergebnisphase
	   ausgeloest, so dass die Zahl der Interrupts mit der Zahl der gelesenen
	   Sektoren uebereinstimmt.
	*/
	controller.setTimeout(10, 0); // 10 sec
	controller.setCommandReg(CMD_READ);

	// neu:
	if (false) {
	while (count > 0) {
	    int timeout = 100; // 5sec
	    do {
		if (timeout <= 0) // Timeout
		    throw new IDEException("timeout");
		Env.sleepManager.mdelay(50);
		timeout--;
	    } while (controller.statusBusy());
	    /* Wenn die CPU das Statusregister liest, wird automatisch eine eventuell
	       anhaengige Interrupt-Anforderung - im PC ueber IRQ14 - storniert.
	    */
	    
	    Env.sleepManager.mdelay(50);
	
	    if ((controller.getStatus() & (Controller.STATUS_DRQ | BAD_R_STAT)) != Controller.STATUS_DRQ) {
		controller.getStatus();
		throw new IDEException("Controller not ready");
	    }
	    
	    handler();
	}
	}
    }

    public void handler() {
	//System.out.println("ReadOperation.handler()");
	byte stat = controller.getStatus();
	if (!((stat & (DATA_READY | BAD_R_STAT)) == DATA_READY)) {
	    controller.ideError(drive, "read interrupt", stat);
	    return;
	}

	controller.inputData(buffer, offset, SECTOR_LONGS);
	sector++;
	offset += 128; //512;
	errors = 0;
	count--;
	if (count <= 0)
	    endOperation(true);
	else
	    controller.setTimeout(10, 0); // 10 sec
	state.set(STATE_COMPLETED);
	//System.out.println("ReadOperation.handler() - Ende");
    }
}
