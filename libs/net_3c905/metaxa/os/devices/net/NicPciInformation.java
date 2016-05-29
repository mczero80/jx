package metaxa.os.devices.net;
import jx.devices.pci.PCIDevice;
class NicPciInformation {
	private int InterruptVector;
	private int IoBaseAddress;
    PCIDevice dev;

    public NicPciInformation(PCIDevice dev) {this.dev = dev;}

    /*
    public NicPciInformation(int interrupt, int iobase) {
	InterruptVector = interrupt;
	IoBaseAddress = iobase;
    }
    */

    public int get_Interrupt() {
	return dev.getInterruptLine();
    }
    public int get_IoBaseAddress() {
	return dev.getBaseAddress(0);
    }
    /*
    public void set_Interrupt(int i) {
	InterruptVector = i;
    }
    public void set_IoBaseAddress(int i) {
	IoBaseAddress = i;
    }
    */
} 
