package jx.awt.peer;


import java.awt.*;
import java.awt.event.FocusEvent;
import jx.awt.*;


/**
 * This class stores the component that actually has the focus.
 */
public class FocusHandler {

    private JXToolkit toolkit;
    /** The component that has the focus */
    private Component focusedComponent = null;



    /** Creates a new FocusHandler instance */
    public FocusHandler(JXToolkit toolkit) {
	this.toolkit = toolkit;
    }




    /** Gets the focused component. */
    public Component getFocusedComponent() {
	return focusedComponent;
    }
    
    /**
     * Sets the new focused component and sends some
     * focus events, if necessary.
     */
    public void setFocusedComponent(Component c) {
	if (focusedComponent != null)
	    sendFocusEvent(focusedComponent, FocusEvent.FOCUS_LOST);
	focusedComponent = c;
	if (focusedComponent != null)
	    sendFocusEvent(focusedComponent, FocusEvent.FOCUS_GAINED);
    }

    /** Sends a focus event. */
    private void sendFocusEvent(Component c, int what) {
	EventQueue queue = toolkit.getSystemEventQueue();
	queue.postEvent(new FocusEvent(c, what, false));
    }
}
