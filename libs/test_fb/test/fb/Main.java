package test.fb;

import jx.zero.*;
import jx.zero.debug.*;

import jx.devices.fb.FramebufferDevice;
import jx.devices.fb.FramebufferConfiguration;
import jx.devices.fb.ColorSpace;
import jx.devices.DeviceFinder;
import jx.devices.Device;


import jx.devices.pci.PCIAccess;

import jx.devices.fb.*;

public class Main {
    public static void init(Naming naming, String[] argv) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	if (argv != null) {
	    for(int i=0; i<argv.length; i++) {
		if (argv[i] != null) {
		    Debug.out.println("PARAMETER["+i+"]: "+argv[i]);
		}
	    }
	}
	test(naming);
    }
    public static boolean test(Naming naming) {
	Debug.out.println("lookup PCI Access Point...");
	PCIAccess bus;
	CPUManager cpuManager = (CPUManager)naming.lookup("CPUManager");
	for(;;) {
	    bus = (PCIAccess)naming.lookup("PCIAccess");
	    if (bus != null) break;
	    cpuManager.yield();
	}	
	bus.dumpDevices();

	Debug.out.println("scanning PCIBus for framebuffer devices...");
	DeviceFinder[] finder = { new jx.devices.fb.vmware.VMWareSVGAFinder(),
				  new jx.devices.fb.mga.MGA200Finder(),
	};
	FramebufferDevice[] fbs = null;
	for(int i=0; i<finder.length; i++) {
	    fbs = (FramebufferDevice[]) finder[i].find();
	    if (fbs != null && fbs.length != 0) break;
	}
	if (fbs == null) throw new Error("No supported framebuffer hardware found.");

	FramebufferDevice fb = fbs[0];

	fb.open(new FramebufferConfiguration(640, 480, new ColorSpace(ColorSpace.CS_RGB16)));

  	PixelRect p = new PixelRect (20, 20, 100, 200);
	PixelColor cColor = new PixelColor (255, 0, 0);

	//fb.fillRect(new PixelRect[] { p }, 1, cColor);

	fb.close();

	return true;
    }
}
