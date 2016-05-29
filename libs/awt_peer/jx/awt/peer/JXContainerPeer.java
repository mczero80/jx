package jx.awt.peer;

import java.awt.*;
import java.awt.event.*;
import jx.awt.*;


/**
 * This is the abstract superclass for all peers that
 * can contain other peers.
 */
public abstract class JXContainerPeer
    extends JXComponentPeer
    implements java.awt.peer.ContainerPeer {
    

    /** Creates a new JXContainerPeer instance */
    public JXContainerPeer(JXToolkit toolkit, Component parent) {
	super(toolkit, parent);
    }


    /**
     * Extends redrawComponent() from JXComponentPeer to repaint all known
     * child peers.
     */
    protected void redrawComponent() {
	super.redrawComponent();
	if (parent != null) {
	    // repaint all children, if necessary
	    Container c = (Container) parent;
	    for (int i = 0; i < c.getComponentCount(); i++) {
		JXComponentPeer peer = (JXComponentPeer) c.getComponent(i).getPeer();
		if (peer != null)
		    peer.redrawComponent();
	    }
	}
    }

    /**************************************************
     * methods implemented from ContainerPeer          *
     **************************************************/

    public Insets insets() {
	return new Insets(0, 0, 0, 0);
    }
    
    public Insets getInsets() {
	return insets();
    }

    public void beginValidate() {}
    public void endValidate() {}
}
