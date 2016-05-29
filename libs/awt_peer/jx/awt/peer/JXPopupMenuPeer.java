package jx.awt.peer;


import java.awt.*;
import java.awt.peer.*;
import jx.awt.*;


/**
 * This class represents a PopupMenu peer. As you might have found
 * out, the Classpath implementation of PopupMenu does NOT create
 * this peer at all, so I decided to force Component to create any
 * PopupMenu peers when it creates its own peer.
 * This means that a PopupMenu peer is created as soon as the peer
 * of its host component is created.
 */
public class JXPopupMenuPeer
    extends JXMenuPeer
    implements PopupMenuPeer {

    
    /** Creates a new instance of JXPopupMenuPeer */
    public JXPopupMenuPeer(PopupMenu parent, JXToolkit toolkit) {
	super(parent, toolkit);
	this.parent = parent;
    }


    /************************************************
     * methods implemented from PopupMenuPeer        *
     ************************************************/
    
    public void show(Component comp, int x, int y) {
	MenuHandler mh = toolkit.getMenuHandler();
	// popup menu should not be created if there is already
	// a menu open
	if (mh.isMenuOpen())
	    return;
	Point p = ((JXComponentPeer) comp.getPeer()).getLocationOnScreen();
	// tell MenuHandler to handle this menu
	mh.setCurrentMenuBar(null);
	mh.openMenu((Menu) parent, new Rectangle(p.x + x, p.y + y, 0, 0), false);
    }
}
