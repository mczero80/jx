package bioide;

/**
 * Base class for operations.
 * - startOperation (start op),
 * - endOperation (end op),
 * - stopOperation (terminate op)
 * @author Michael Golm
 * @author Andreas Weissel
 */
abstract class Operation {
    public    int        errors;      // number of errors
    public    Operation  next;

    protected Controller controller;
    protected Drive      drive;

    protected static final byte BAD_R_STAT  = (byte)(Controller.STATUS_BSY  | Controller.STATUS_ERR);
    protected static final byte BAD_W_STAT  = (byte)(Controller.STATUS_BSY  | Controller.STATUS_ERR | Controller.STATUS_WFT);
    protected static final byte DRIVE_READY = (byte)(Controller.STATUS_RDY  | Controller.STATUS_SKC);
    protected static final byte DATA_READY  = (byte)(DRIVE_READY | Controller.STATUS_DRQ);

    protected static final int SECTOR_SIZE  = 512;	     // size of one sector in bytes
    protected static final int SECTOR_LONGS = SECTOR_SIZE/4; // size of one sector in words 

    public Operation(Controller controller, Drive drive) {
	this.controller  = controller;
	this.drive       = drive;
	this.errors      = 0;
	this.next        = null;
    }

    public abstract void startOperation() throws IDEException;

    public void stopOperation() {
	endOperation(false);
    }

    public void endOperation(boolean uptodate) {
	errors = 0;
	if (!uptodate)
	    errors = 1;
	controller.nextOperation();
    }

    public void handler() { }

    public void waitForCompletion() { 
	throw new Error("Waiting for completion of this operation is not possible.");
    } // override this method in subclasses

}

