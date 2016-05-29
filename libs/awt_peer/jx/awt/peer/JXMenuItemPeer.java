package jx.awt.peer;


import java.awt.*;
import java.awt.peer.*;
import jx.awt.*;


/**
 * This class symbolizes a simple menu item. This peer is more
 * or less a dummy class because JXMenuPeer gets all necessary
 * information from the parent class MenuItem and therefore
 * never needs the peer. But, to be compatible to the AWT
 * specifications, here it is.
 */
public class JXMenuItemPeer
    extends JXMenuComponentPeer
    implements MenuItemPeer {


    /** Creates a new JXMenuItemPeer instance */
    public JXMenuItemPeer(MenuItem parent, JXToolkit toolkit) {
	super(parent, toolkit);
    }

    /************************************************
     * methods implemented from MenuItemPeer         *
     ************************************************/

    public void enable() {setEnabled(true);}
    public void disable() {setEnabled(false);}
    public void setEnabled(boolean enabled) {}
    public void setLabel(String label) {}
}
