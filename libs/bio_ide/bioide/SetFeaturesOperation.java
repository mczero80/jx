package bioide;

/**
 * Base class for configuration operations (SETFEATURES): EnableDMAOperation, EnablePIOOperation.
 * @author Michael Golm
 * @author Andreas Weissel
 */
abstract class SetFeaturesOperation extends Operation {
    protected static final byte CMD_SETFEATURES = (byte)0xef;   // (unter anderem) DMA aktivieren

    public SetFeaturesOperation(Controller controller, Drive drive) {
        super(controller, drive);
    }
}
