package jx.zero;

public interface IRQ extends Portal {

    /* SMP: installs the First Level Handler for the current CPU */
    public void installFirstLevelHandler(int irq, FirstLevelIrqHandler h);

    public void enableIRQ(int irq);

    public void disableIRQ(int irq);

    public void enableAll();

    public void disableAll();

    /** sets the destination Processor for an IRQ (-1=all) */
    public void set_destination(int irq, int dest_cpu);
}
