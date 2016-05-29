package jx.devices.fb.emulation;


import jx.devices.DeviceFinder;
import jx.devices.Device;
import jx.zero.*;

import java.util.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.*;
import jx.devices.fb.*;
import jx.wm.EventListener;
import jx.wm.Keycode;
import jx.wm.Qualifiers;
import jx.wm.WindowManager;

public class InputEmul {
    public static void main(String[] args) {
	Naming naming = InitialNaming.getInitialNaming();
	String wmName = args[0];
	WindowManager m_cWindowManager = (WindowManager)LookupHelper.waitUntilPortalAvailable(naming, wmName);
	FBEmulation fb = (FBEmulation)InitialNaming.getInitialNaming().lookup("FBEmulation");
	new EmulationEventListener(m_cWindowManager, fb);
    }
}

class EmulationEventListener extends EventListener {
    FBEmulation fb;
    int x = -1;
    int y = -1;
    int mousex = -1;
    int mousey = -1;
    static final boolean debug = true;

    EmulationEventListener(WindowManager wm, FBEmulation fb) {
	super(wm);
	this.fb = fb;
	new Thread("FBEmul-Eventloop") {
		public void run() {
		    eventloop();
		}
	    }.start();
    }
    void eventloop() {
	if (! fb.inputDevicesAvailable()) return;
	FBEmulationEvent event = new FBEmulationEvent();
	int x;
	for(;;) {
	    if(! fb.checkEvent(event)) {
		//if (debug) Debug.out.print("*");
		Thread.yield();
		x = event.eventType;
	    }
	    else {
		//if (debug) Debug.out.println("EVENT");
		switch (event.eventType) {
		case FBEmulationEvent.TYPE_KEY_PRESS:
		    //Debug.out.println("KEY PRESS: "+event.keycode);
		    handleKeyDown (translateKeyCode (event.keycode));
		    break;
		case FBEmulationEvent.TYPE_KEY_RELEASE:
		    handleKeyUp (translateKeyCode (event.keycode));
		    //Debug.out.println("KEY RELEASE: "+event.keycode);
		    break;
		case FBEmulationEvent.TYPE_BUTTON_PRESS:
		    //Debug.out.println("BUTTON PRESS: "+event.button);
		    handleMouseDown (event.button);
		    break;
		case FBEmulationEvent.TYPE_BUTTON_RELEASE:
		    //Debug.out.println("BUTTON RELEASE: "+event.button);		
		    handleMouseUp (event.button);
		    break;
		case FBEmulationEvent.TYPE_MOUSE_MOVE:
		    if (debug) Debug.out.println("MOUSE MOVE: "+event.x + "/"+event.y + ", state="+event.state);
		    handleMousePosition (event.x, event.y);
		    /*
		    if (x == -1 || y == -1)
		    {
		    	x = event.x;
			y = event.y;
		    }
		    else
		    {
			//if (event.x
			handleMouseMoved (-1000, -1000);
			handleMouseMoved (event.x, event.y);
			//handleMouseMoved (event.x - x, event.y - y);
			    x = event.x;
			    y = event.y;
		    }	
		    */
		    break;

		}
	    }
	}
    }
    private int translateKeyCode (int keyCode)
    {
		return keyCode;
    }
}
