package jx.devices.fb.vmware;


import jx.devices.DeviceFinder;
import jx.devices.Device;
import jx.zero.*;

import java.util.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.pci.*;
import jx.devices.*;

/**
 * VMWare SVGA Device finder
 * @author Michael Golm
 */
public class VMWareSVGAFinder implements DeviceFinder {
    final static int    FB_VENDOR_ID       = 0x15ad;           
    final static int    FB_PCI_DEVICE_ID   = 0x0710;
    final static int    FB_PCI_DEVICE_ID1  = 0x0405;

    public Device[] find(String[] args) {
	Debug.out.println("lookup PCI Access Point...");
	PCIAccess bus;
	int counter=0;
	CPUManager cpuManager = (CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager");
	for(;;) {
	    bus = (PCIAccess)InitialNaming.getInitialNaming().lookup("PCIAccess");
	    if (bus == null) {
		if (counter % 20 == 0) { counter = 0; Debug.out.println("VMWareSVGAFinder still waiting for PCI");}
		counter++;
		cpuManager.yield();
	    } else {
		break;
	    }
	}	
	return findDevice(bus);
    }

   public VMWareSVGAImpl[] findDevice(PCIAccess pcibus){
       int deviceID = -1;
       int vendorID = -1;
       PCIDevice dev;
       int devc = pcibus.getNumberOfDevices();
       VMWareSVGAImpl [] mgas = new VMWareSVGAImpl[devc];
       for(int devindex=0; devindex<devc; ++devindex){
	   dev = pcibus.getDeviceAt(devindex);
	   
	   deviceID = dev.getDeviceID() & 0xffff;
	   vendorID = dev.getVendorID() & 0xffff;

	   if (vendorID ==  FB_VENDOR_ID) {          // vmware Vendor ID
	       switch (deviceID) {
		case FB_PCI_DEVICE_ID:
		    Debug.out.println("VMWare SVGA found");
		    return new VMWareSVGAImpl[] { new VMWareSVGAImpl(dev, true) };
		case FB_PCI_DEVICE_ID1:
		    Debug.out.println("VMWare SVGA1 found");
		    return new VMWareSVGAImpl[] { new VMWareSVGAImpl(dev, false) };
		default:				
		    Debug.out.println("ERROR: Unsupported VMWare card found");
		    continue;
		}				
	   }
       }
       return null;
   }
}


