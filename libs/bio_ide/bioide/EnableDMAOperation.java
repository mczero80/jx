package bioide;

import jx.zero.Debug;

/**
 * Configure drive for fastest (Ultra)DMA mode
 * @author Michael Golm
 * @author Andreas Weissel
 */
class EnableDMAOperation extends SetFeaturesOperation {
    private boolean ultraDmaSupported;

    public EnableDMAOperation(Controller controller, Drive drive, boolean ultraDmaSupported) {
        super(controller, drive);
	this.ultraDmaSupported = ultraDmaSupported;
    }

    public void startOperation() throws IDEException{
	int maxmode = 0, currmode = 0, dmatype = -1;  // 0 == mword, 1 == ultra
	DriveIdData id_data = drive.id_data;
	int timeout;
	int pcicmd;

	if (((id_data.field_valid() & 2) > 0) && ((id_data.dma_mword() & 0x07) > 0)) { // irgendein dma_mword-Modus unterstuetzt?
	    dmatype = 0;
	    currmode = -1;
	    if (((id_data.dma_mword() >> 8) & 0x01) > 0) currmode = 0;
	    if (((id_data.dma_mword() >> 8) & 0x02) > 0) currmode = 1;
	    if (((id_data.dma_mword() >> 8) & 0x04) > 0) currmode = 2;
	    if ((id_data.dma_mword() & 0x01) > 0) maxmode = 0;
	    if ((id_data.dma_mword() & 0x02) > 0) maxmode = 1;
	    if ((id_data.dma_mword() & 0x04) > 0) maxmode = 2;
	}

	if (((id_data.field_valid() & 4) > 0) && ((id_data.dma_ultra() & 0x07) > 0)) { // irgendein dma_ultra-Modus unterstuetzt?
	    if (ultraDmaSupported) {
		dmatype = 1;
		currmode = -1;
		if (((id_data.dma_ultra() >> 8) & 0x01) > 0) currmode = 0;
		if (((id_data.dma_ultra() >> 8) & 0x02) > 0) currmode = 1;
		if (((id_data.dma_ultra() >> 8) & 0x04) > 0) currmode = 2;
		if ((id_data.dma_ultra() & 0x01) > 0) maxmode = 0;
		if ((id_data.dma_ultra() & 0x02) > 0) maxmode = 1;
		if ((id_data.dma_ultra() & 0x03) > 0) maxmode = 2;
	    }
	}

	if (dmatype == -1)
	    throw new DMAUnavailableException(); // PIO verwenden

	if (currmode != maxmode) {
	    controller.setCTLReg(drive.ctl);
	    controller.setFeatureReg((byte)0x03); // Transfermodus einstellen
	    controller.setCountReg((byte)((0x20 << dmatype) | (maxmode & 0x03)));
	    // DMA, mode maxmode
	    controller.setCommandReg(CMD_SETFEATURES);
	    timeout = 5 * 20; // 5 * 20 50msec-Loops = 5 sec
	    
	    Debug.out.println("---");
	    do {
		if (timeout <= 0) { // Timeout
		    Debug.out.println("     Fehler beim Setzen des Transfermodus (timeout), keine DMA-Unterstuetzung");
		    throw new DMAUnavailableException();
		}
		Env.sleepManager.mdelay(50);
		Debug.out.println("*");
		timeout--;
	    } while (controller.statusBusy()); // Intr. abfangen
	    Env.sleepManager.mdelay(50);

	    if ((controller.getStatus() & BAD_R_STAT) != 0) {
		Debug.out.println("     Fehler beim Setzen des Transfermodus, keine DMA-Unterstuetzung");
		throw new DMAUnavailableException();
	    }
	}
	
	if (dmatype == 1)
	    Debug.out.println("     Ultra-DMA (Modus " + maxmode + ")");
	else
	    Debug.out.println("     Multiword-DMA (Modus " + maxmode + ")");
	drive.using_dma = true;
    }
}
