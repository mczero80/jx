package jx.awt.peer;

import java.util.*;
import jx.awt.*;


/**
 * This class provides some simple handling of so-called
 * "slave windows", which means in this case that these
 * windows are dependent on some mother windows, and should
 * somehow be handled when the mother windows close.
 */
public class SlaveWindowHandler {

    /** The Vector containing all registered windows */
    private Vector windows;

    /** Creates a new SlaveWindowHandler instance */
    public SlaveWindowHandler() {
	windows = new Vector();
    }



    /** Registers a mother window to be handled when it closes. */
    public void registerWindow(JXComponentPeer peer) {
	if (peer != null)
	    windows.addElement(peer);
    }

    /** Tells whether there are registered windows or not */
    public boolean windowsRegistered() {
	return (windows.size() > 0);
    }

    /**
     * Performs the default close operation. This operation depends
     * on the type of the mother window. After performing the operation,
     * the mother window is unregistered.
     */
    public void performCloseOperation() {
	while (windows.size() > 0) {
	    if (windows.firstElement() instanceof JXChoicePeer)
		((JXChoicePeer) windows.firstElement()).abortChoice();
	    windows.remove(0);
	}
    }
    
    public void dispose() {
	windows = null;
    }
}
