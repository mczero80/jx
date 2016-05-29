package jx.awt.peer;


import java.awt.*;
import java.awt.peer.*;
import jx.awt.*;


/**
 * This class implements a CheckboxMenuItem peer. This peer is more
 * or less a dummy class, as JXMenuPeer gets all needed information
 * from the parent class CheckboxMenuItem.
 */
public class JXCheckboxMenuItemPeer
    extends JXMenuItemPeer
    implements CheckboxMenuItemPeer {


    /** Creates a new JXCheckboxMenuItemPeer instance */
    public JXCheckboxMenuItemPeer(CheckboxMenuItem parent, JXToolkit toolkit) {
	super(parent, toolkit);
    }

    /**************************************************
     * methods implemented from CheckboxMenuItemPeer   *
     **************************************************/

    public void setState(boolean state) {}
}
