package bioide;

import jx.zero.Debug;

/**
 * Configure drive for fastest PIO mode
 * @author Michael Golm
 * @author Andreas Weissel
 */
class EnablePIOOperation extends SetFeaturesOperation {
    public EnablePIOOperation(Controller controller, Drive drive) {
        super(controller, drive);
    }

    public void startOperation() throws IDEException {
	int maxmode = 0;
	DriveIdData id_data = drive.id_data;
	int timeout;  

	drive.using_dma = false;

	if ((id_data.eide_pio_modes() & 0x01) > 0) maxmode = 0;
	if ((id_data.eide_pio_modes() & 0x02) > 0) maxmode = 1;
	if ((id_data.eide_pio_modes() & 0x04) > 0) maxmode = 2;

	controller.setCTLReg(drive.ctl);
	controller.setFeatureReg((byte)0x03);  // Transfermodus einstellen
	controller.setCountReg((byte)(0x08 | (maxmode & 0x03))); // PIO, mode maxmode
	controller.setCommandReg(CMD_SETFEATURES);
	timeout = 5 * 20; // 5 * 20 50msec-Loops = 5 sec
  
	do {
	    if (timeout <= 0) { // Timeout
		Debug.out.println("     Fehler beim Setzen des Transfermodus (timeout), PIO-Modus unveraendert");
		throw new IDEException("Timeout setting transfer mode");
	    }
	    Env.sleepManager.mdelay(50);
	    timeout--;
	} while (controller.statusBusy());  // Intr. abfangen
	Env.sleepManager.mdelay(50);

	if ((controller.getStatus() & BAD_R_STAT) != 0) {
	    Debug.out.println("     Fehler beim Setzen des Transfermodus, PIO-Modus unveraendert");
	    throw new IDEException("Error setting transfer mode");
	}

	Debug.out.println("     PIO (Modus " + (maxmode+3) + ")");
    }
}
