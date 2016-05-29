package jx.awt.peer;


import java.awt.*;
import java.awt.peer.*;
import jx.awt.*;


/**
 * This is the abstract superclass of all menu-related components.
 */
public abstract class JXMenuComponentPeer
    implements MenuComponentPeer {
    
    /** The width of a character of the default font */
    protected final static int CHARWIDTH = 9;
    /** The height of a character of the default font */
    protected final static int CHARHEIGHT = 14;

    /** The host component associated with this peer */
    protected MenuComponent parent;
    /** A reference to the global JXToolkit instance */
    protected JXToolkit toolkit;
    /**
     * A flag indicating whether this component is ready
     * to be drawn or not
     */
    protected boolean ready = false;


    /** Creates a new JXMenuComponentPeer instance */
    public JXMenuComponentPeer(MenuComponent parent, JXToolkit toolkit) {
	this.parent = parent;
	this.toolkit = toolkit;
    }
    
    /**************************************************
     * methods implemented from MenuComponentPeer      *
     **************************************************/

    public void dispose() {}
}
