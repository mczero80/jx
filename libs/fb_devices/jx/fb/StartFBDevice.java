package jx.fb;

import jx.zero.*;
import jx.devices.*;

import jx.devices.fb.*;

import java.util.Vector;

/**
 * Query all framebuffer drivers for a device.
 * Register the device under the name given as argument.
 */
public class StartFBDevice {
    public static void main(String[] args) throws Exception {
	Naming naming = InitialNaming.getInitialNaming();
	Device[] devices = null;
	DeviceConfigurationTemplate cModes[] = null;
	FramebufferConfigurationTemplate cMode;
	
	ComponentManager componentManager = (ComponentManager) naming.lookup("ComponentManager");

	for(int i=0; i<args.length; i++) {
	    Debug.out.println("args["+i+"]="+args[i]);
	}

	String fbName = args[0];

	for(int i=1; i<args.length;) {
	    String className = args[i++];
	    String libName = args[i++];
	    Debug.out.println("Trying class "+className+" in component "+libName);

	    Vector dargs = new Vector();
	    while(! args[i].equals("null")) {
		Debug.out.println("DRIVER ARG: "+args[i]);
		dargs.addElement(args[i++]);
	    }
	    i++;
	    String[] devargs = new String [dargs.size()]; 
	    dargs.copyInto(devargs);

	    componentManager.load(libName);
	    DeviceFinder finder = (DeviceFinder)Class.forName(className).newInstance();
	    devices = finder.find(devargs);
	    if (devices != null && devices[0] != null) {
		FramebufferDevice fb = (FramebufferDevice) devices[0];
		naming.registerPortal(fb, fbName);
		return;
	    }
	}

	throw new Error("Unable to find suitable framebuffer driver.");
    
    }
}
