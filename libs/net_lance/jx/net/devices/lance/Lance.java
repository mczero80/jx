package jx.net.devices.lance;

import jx.devices.net.NetworkDevice;
import jx.zero.*;
import jx.devices.pci.*;
import jx.devices.*;

import jx.buffer.separator.*;

class Lance implements NetworkDevice {
    Lance(PCIDevice dev) {}
    public DeviceConfigurationTemplate[] getSupportedConfigurations () {
	return null;
    }

    public void open(DeviceConfiguration conf) {}
    public void close() {}
    public void setReceiveMode(int mode) {}
    public Memory transmit(Memory buf) {return null;}
    public Memory transmit1(Memory buf, int offset, int size) {return null;}
    public byte[] getMACAddress() {return null; }
    public int getMTU() {return 0;}
    public boolean registerNonBlockingConsumer(NonBlockingMemoryConsumer consumer){throw new Error();}
    
}
