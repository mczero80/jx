package bioide;

import jx.zero.Debug;

/**
 * Drive reset (necessary when drive does not answer).
 * @author Michael Golm
 * @author Andreas Weissel
 */
class ResetOperation extends Operation {
    private int poll_timeout;

    public ResetOperation(Controller controller, Drive drive, int poll_timeout) {
        super(controller, drive);
	this.poll_timeout = poll_timeout;
    }

    public void startOperation() throws IDEException {
	/* Neben SRST (CTL_RESET) wird auch nIEN (CTL_INTDISABLE) gesetzt,
	   um Interrupts waehrend des Resets auszumaskieren. Dadurch wird
	   sofort ein Interrupt ausgeloest, der uns aber als schnelle Abfrage
	   dient, fuer Laufwerke, die sich von einem Reset sehr schnell erholen
	   (das spart die ersten 50 msec).
	*/
	// SRST und nIEN setzen:
	controller.setCTLReg((byte)(drive.ctl | controller.CTL_RESET | controller.CTL_INTDISABLE));
	Env.sleepManager.mdelay(10);
	// SRST loeschen, nIEN gesetzt lassen
	controller.setCTLReg((byte)(drive.ctl | controller.CTL_INTDISABLE));
	Env.sleepManager.mdelay(10);
    }

    public void handler() {
	poll_timeout -= 50;
	
	if (controller.statusBusy()) {
	    if (poll_timeout > 0) {
		controller.setTimeout(0, 50); // 50 msec
		return;	// weiter pollen
	    }
	    Debug.out.println("Reset-Timeout");
	}
	poll_timeout = 0;	// polling beenden
	controller.nextOperation();
    }
}
