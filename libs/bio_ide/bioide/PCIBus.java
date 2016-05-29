package bioide;

import jx.zero.*;

/**
 * Access to pci bus (must still be integrated into JX PCI)
 * @author Michael Golm
 * @author Andreas Weissel
 */
public class PCIBus {

    /*
     * The registers (ports) CONFIG_ADDRESS and CONFIG_DATA
     * are used to access the PCI configuration space.
     */
    private static final int CONFIG_ADDRESS    = 0x0cf8;
    private static final int CONFIG_DATA       = 0x0cfc;
    
    private static final int BUS_OFFSET_BIT = 16;
    private static final int DEV_OFFSET_BIT = 11;
    private static final int FUN_OFFSET_BIT = 8;
    private static final int REG_OFFSET_BIT = 2;
    
    private static final int ECD_MASK = 0x80000000; // Enable CONFIG_DATA
    private static final int BUS_MASK = 0x00ff0000;
    private static final int DEV_MASK = 0x0000f800;
    private static final int FUN_MASK = 0x00000700;
    private static final int REG_MASK = 0x000000fc;

    private static final int MAX_PCI_AGENTS = 32;
    private static final int MAX_PCI_BUSSES = 256;

    public PCIBus() {
    }

    int createAddress(int bus, int device, int function, int register) {
	if (device < MAX_PCI_AGENTS) {
	    return ECD_MASK 
		| ((bus      << BUS_OFFSET_BIT) & BUS_MASK)
		| ((device   << DEV_OFFSET_BIT) & DEV_MASK)
		| ((function << FUN_OFFSET_BIT) & FUN_MASK)
		| ((register << REG_OFFSET_BIT) & REG_MASK);
	}
	return 0;
    }
    
    public int readConfig(int bus, int device, int function, int reg) {
	int addr = createAddress(bus, device, function, reg);
	return readConfig(addr);
    }
    
    public int readConfig(int addr) {
	int data = 0;
	Env.ports.outl(CONFIG_ADDRESS, addr);
	data = Env.ports.inl(CONFIG_DATA);
	Env.ports.outl (CONFIG_ADDRESS, 0);
	return data;
    }

    public void writeConfig(int bus, int device, int function, int reg, int value) {
	int addr = createAddress(bus, device, function, reg);
	writeConfig(addr, value);
    }
    
    public void writeConfig(int addr, int value) {
	Env.ports.outl(CONFIG_ADDRESS, addr);
	Env.ports.outl(CONFIG_DATA, value);
	Env.ports.outl (CONFIG_ADDRESS, 0);
    }
}

