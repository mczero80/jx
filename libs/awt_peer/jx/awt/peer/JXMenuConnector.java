package jx.awt.peer;


import java.awt.*;
import jx.wm.*;
import jx.devices.fb.*;
import jx.awt.*;



/**
 * This class represents a menu window.
 */
public class JXMenuConnector
    extends GeneralConnector {
    
    private Menu parent;



    /** Creates a new JXMenuConnector instance */
    public JXMenuConnector(Menu parent, JXToolkit toolkit) {
	super(toolkit, "", new PixelRect(0, 0, 50, 50),
	      new WindowFlags(WindowFlags.WND_NO_BORDER|
			      WindowFlags.WND_NO_FOCUS|
			      WindowFlags.WND_TRANSPARENT));
	this.parent = parent;
    }

    /**
     * Creates a new JXMenuConnector instance for internal use. Used by
     * JXToolkit to find out the screen's dimension. DO NOT USE!!!
     */
    public JXMenuConnector() {
	super(null, "", new PixelRect(0, 0, 0, 0), new WindowFlags());
    }






    /**
     * Gets a copy of the local JXGraphics object.
     */
    public JXGraphics getGraphics() {
	return (JXGraphics) graphics.create();
    }

    /**
     * This method is called when a mouse button has been released.
     * The event is forwarded to the menu's peer.
     */
    public void mouseUp (PixelPoint cMousePos, int nButton) {
	//toolkit.message("mouse button " + nTransit + " pressed.");
	if (parent.getPeer() != null)
	    ((JXMenuPeer) parent.getPeer()).handleMenuMouseUp(cMousePos.X(),
							      cMousePos.Y(),
							      nButton);
    }
    
    /**
     * This method is called when the mouse has been moved.
     * The event is forwarded to the menu's peer.
     */
    public void mouseMoved (PixelPoint cMousePos, int nTransit) {
	//toolkit.message("mouse at " + cMousePos.X() + "," + cMousePos.Y() + ", transit is " + nTransit);
	if (parent.getPeer() != null)
	    ((JXMenuPeer) parent.getPeer()).handleMenuMouseMoved(cMousePos.X(),
								 cMousePos.Y(),
								 (nTransit == WWindowInterface.MOUSE_INSIDE));
    }

    /**
     * This method is called when the window needs to be repainted.
     * The event is forwarded to the menu's peer.
     */
    public void paint(PixelRect cFrame) {
	if (parent.getPeer() != null)
	    ((JXMenuPeer) parent.getPeer()).redrawMenu();
    }

}
