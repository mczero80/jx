package jx.devices.fb.mga;

import jx.devices.DeviceFinder;
import jx.devices.Device;
import jx.zero.*;

import java.util.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.pci.*;
import jx.devices.*;

/**
 * MGA Device finder
 * @author Michael Golm
 */
public class MGA200Finder implements DeviceFinder {
    final static int    FB_VENDOR_ID = 0x102b;           
    final static int    FB_PCI_DEVICE_ID_1 = 0x0520;
    final static int    FB_PCI_DEVICE_ID_2 = 0x0521;

    public Device[] find(String[] args) {
	Debug.out.println("lookup PCI Access Point...");
	PCIAccess bus;
	int counter=0;
	for(;;) {
	    bus = (PCIAccess)InitialNaming.getInitialNaming().lookup("PCIAccess");
	    if (bus == null) {
		if (counter % 20 == 0) { counter = 0; Debug.out.println("NetInit still waiting for PCI");}
		counter++;
		Thread.yield();
	    } else {
		break;
	    }
	}	
	return findDevice(bus);
    }

   public MGA200Impl[] findDevice(PCIAccess pcibus){
       int deviceID = -1;
       int vendorID = -1;
       PCIDevice dev;
       int devc = pcibus.getNumberOfDevices();
       MGA200Impl [] mgas = new MGA200Impl[devc];
       for(int devindex=0; devindex<devc; ++devindex){
	   dev = pcibus.getDeviceAt(devindex);
	   
	   deviceID = dev.getDeviceID() & 0xffff;
	   vendorID = dev.getVendorID() & 0xffff;

	   if (vendorID ==  FB_VENDOR_ID) {          // Matrox Vendor ID
	       switch (deviceID) {
		case FB_PCI_DEVICE_ID_1:
		    Debug.out.println("MGA1 found");
		    return new MGA200Impl[] { new MGA200Impl(dev) };
		case FB_PCI_DEVICE_ID_2:
		    Debug.out.println("MGA2 found");
		    return new MGA200Impl[] { new MGA200Impl(dev) };
		default:				
		    Debug.out.println("ERROR: Unsupported Matrox card found: id="+deviceID);
		    continue;
		}				
	   }
       }
       return null;
   }
    /*
    public Lance[] findDevice(PCIAccess pci) {
	int deviceID = -1;
	int vendorID = -1;
	Lance helper;
	PCIDevice[] devInfo = pci.getDevicesByClass(PCI.CLASSCODE_CLASS_MASK, PCI.BASECLASS_NETWORK);
	if (devInfo == null || devInfo.length == 0) {
	    Debug.out.println("no network devices of any vendor found! ");
	    return null;
	}
	// search for supported NICs
	for (int i = 0; i < devInfo.length; i++) {
	    deviceID = devInfo[i].getDeviceID() & 0xffff;
	    vendorID = devInfo[i].getVendorID() & 0xffff;

	    Debug.out.println("Vendor="+ Integer.toHexString(vendorID)+", Device="+ Integer.toHexString(deviceID));
	
	    if (vendorID ==  NIC_VENDOR_ID) {          // 3COM Vendor ID
		switch (deviceID) {

		case NIC_PCI_DEVICE_ID_Am79C970:
		    Debug.out.println("10/100 Base-TX NIC found");
		    return new Lance[] { new Lance(devInfo[i]) };
		    
		    
		default:				
		    Debug.out.println("ERROR: Unsupported NIC found");
		    continue;
		}				
	    }
	}
	return null;
    }                    
    */
}
