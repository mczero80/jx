package bioide;

import jx.zero.Debug;

/**
 * Set head to factory setting.
 * @author Michael Golm
 * @author Andreas Weissel
 */
class RecalibrateOperation extends Operation {
    private static final byte CMD_RECALIBRATE = (byte)0x10;   // Laufwerk zuruecksetzen

    public RecalibrateOperation(Controller controller, Drive drive) {
        super(controller, drive);
    }

    public void startOperation() throws IDEException {
	int timeout;
	boolean bad = false;
	int retries = 0;
	do {
	    retries++;
	    bad = false;
	    controller.setCTLReg(drive.ctl);
	    // controller.setCountReg(drive.sect);
	    controller.setCommandReg(CMD_RECALIBRATE);
	    timeout = 10 * 20; /* 10 * 20 50msec-Loops = 10 sec */
	    
	    do {
		if (timeout <= 0) { // Timeout
		    Debug.out.println("     Fehler beim Zuruecksetzen des Laufwerks (timeout)");
		    throw new IDEException("Timeout in recalibrate");
		}
		Env.sleepManager.mdelay(50);
		timeout--;
	    } while (controller.statusBusy());  // Intr. abfangen
	    Env.sleepManager.mdelay(400);
	    
	    if ((controller.getStatus() & BAD_R_STAT) != 0) {
		Debug.out.println("     Fehler beim Zuruecksetzen des Laufwerks");
		bad = true;
		if (retries > 5) throw new IDEException("Bad status in recalibrate");
		Env.sleepManager.mdelay(2000);
	    }
	} while (bad);
    }
}
