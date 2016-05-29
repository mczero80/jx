package bioide;

import jx.zero.*;

/**
 * Retrieves drive information by using IDENTIFY command (capacity, geometry, LBA, DMA capabilities, etc.)
 * @author Michael Golm
 * @author Andreas Weissel
 */
class IdentifyOperation extends Operation {
    private static final byte CMD_IDENTIFY    = (byte)0xec;   // Laufwerk identifizieren
    private static final byte CMD_ATAPI_IDENT = (byte)0xa1;   // Abfrage von CDROMs und Bandlaufwerken
    private DriveIdData id_data;

    public IdentifyOperation(DriveIdData id_data, Controller controller, Drive drive) {
        super(controller, drive);
	this.id_data = id_data;
    }

    public void startOperation() throws IDEException {
	int retval = 0;

	// try to select the drive
	controller.setLDHReg(drive.select);
	Env.sleepManager.mdelay(50);
	if (controller.getLDHReg() != drive.select && !drive.present) {
	    // select drive 0 and return
	    controller.setLDHReg(Controller.LDH_DEFAULT);
	    Env.sleepManager.mdelay(50);
	    throw new IDEException("Drive not present");
	}

	if (((controller.getStatus() & (Controller.STATUS_RDY | Controller.STATUS_BSY)) == Controller.STATUS_RDY) 
	    || drive.present) {
	    if (identifyCommand(CMD_IDENTIFY) == -1)
		if (identifyCommand(CMD_ATAPI_IDENT) == -1) {
		    Debug.out.println("" + drive.name + ": keine Antwort (Status = " + controller.getStatus() + ")");

		}
	    controller.getStatus(); // sichergehen, dass Interrupt unterbunden wird
	} else {
	    Debug.out.println("Controller status: "+controller.getStatus());
	    retval = -1;
	}

	if (drive.unit() != 0) {
	    // Laufwerk 0 selektieren und zurueck
	    controller.setLDHReg(Controller.LDH_DEFAULT);
	    Env.sleepManager.mdelay(50);
	    controller.getStatus(); // sichergehen, dass Interrupt unterbunden wird
	}

	if (retval == -1) throw new IDEException("Error in identify");
    }

    /**
     * Identify drive and mark ATAPI devices as "not available"
     */
    private int identifyCommand(byte cmd) {
	int timeout, flags;

	Env.sleepManager.mdelay(50);
	controller.setCommandReg(cmd);
	timeout = 30 * 20; // 30 * 20 50msec-Loops = 30 sec
	
	do {
	    if (timeout <= 0) {// Timeout
		if (Env.verboseID) Debug.out.println("Controller busy. I'm tired of waiting.");  
		return -1;
	    }
	    Env.sleepManager.mdelay(50);
	    timeout--;
	} while (controller.statusBusy());
	/* Wenn die CPU das Statusregister liest, wird automatisch eine eventuell
	   anhaengige Interrupt-Anforderung - im PC ueber IRQ14 - storniert.
	*/
	Env.sleepManager.mdelay(200);

	
	for(int tries=0; tries<10 && ((controller.getStatus() & (Controller.STATUS_DRQ | BAD_R_STAT)) != Controller.STATUS_DRQ); tries++) {
	    Env.sleepManager.mdelay(200);
	}

	if ((controller.getStatus() & (Controller.STATUS_DRQ | BAD_R_STAT)) != Controller.STATUS_DRQ) {
	    int status = controller.getStatus(); // clears interrupt
	    if (Env.verboseID) Debug.out.println("Controller not ready for transfer. Status="+status);  
	    return -1;
	}
	controller.inputData(id_data.getData(), 0, SECTOR_LONGS);
	controller.getStatus();

	id_data.model(fixstring(id_data.model()));

	if (cmd == CMD_ATAPI_IDENT) {
	    byte type = (byte)((id_data.config() >> 8) & 0x1f);
	    Debug.out.print("" + drive.name + ": " + id_data.model().trim() + ", ATAPI "); //drive.trim
	    switch (type) {
	    case 0x0:
		Debug.out.println("FLOPPY oder CDROM - nicht unterstuetzt");
		break;
	    case 0x5:
		Debug.out.println("CDROM - nicht unterstuetzt");
		break;
	    case 0x1:
		Debug.out.println("TAPE - nicht unterstuetzt");
		break;
	    case 0x7:
		Debug.out.println("OPTICAL DISK - nicht unterstuetzt");
		break;
	    case 0x21:
		Debug.out.println("SCSI DISK - nicht unterstuetzt");
		break;
	    default:
		Debug.out.println("unbekanntes Geraet (Typ " + type + ")");
	    }
	    drive.present = false;
	    if (Env.verboseID) Debug.out.println("Unknown ATAPI type="+type);  
	    return -1;
	}
	return 0;
   }

    /**
     * Convert drive strings from big endian to little endian.
     */
    private String fixstring(String string) {
	byte[] p = string.getBytes();
	byte tmp;
	int index = p.length & ~1; // muss gerade sein

	// von big_endian nach little-endian konvertieren
	for (int i = index; i > 0; i -= 2) {
	    tmp = p[i-2];
	    p[i-2] = p[i-1];
	    p[i-1] = tmp;
	}

	return new String(p);
    }
}
