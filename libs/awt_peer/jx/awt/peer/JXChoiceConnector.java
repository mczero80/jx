package jx.awt.peer;

import java.awt.*;
import jx.awt.*;
import jx.wm.*;
import jx.devices.fb.*;


/**
 * This class represents the window that opens when you
 * press the button on a Choice component.
 */
public class JXChoiceConnector
    extends GeneralConnector {

    private Choice parent;
    private int lastButtonPressed = 0;



    /** Creates a new JXChoiceConnector instance */
    public JXChoiceConnector(Choice parent, JXToolkit toolkit) {
	super(toolkit, "", new PixelRect(0, 0, 50, 50),
	      new WindowFlags(WindowFlags.WND_NO_BORDER|
			      WindowFlags.WND_NO_FOCUS|
			      WindowFlags.WND_TRANSPARENT));
	this.parent = parent;
    }




    /**
     * Gets a copy of the local JXGraphics object.
     */
    public JXGraphics getGraphics() {
	return (JXGraphics) graphics.create();
    }

    /**
     * This method is called by the WindowManager when a mouse button is pressed.
     * The event is forwarded to the Choice component's peer.
     */
    public void mouseDown (PixelPoint cMousePos, int nButton) {
	lastButtonPressed = nButton;
	if (parent.getPeer() != null)
	    ((JXChoicePeer) parent.getPeer()).handleChoiceMouseDown(cMousePos.X(),
								    cMousePos.Y(),
								    nButton);
    }

    /**
     * This method is called by the WindowManager when a mouse button is pressed.
     * The event is forwarded to the Choice component's peer.
     */
    public void mouseUp (PixelPoint cMousePos, int nButton) {
	//toolkit.message("mouse button " + nTransit + " pressed.");
	lastButtonPressed = 0;
	if (parent.getPeer() != null)
	    ((JXChoicePeer) parent.getPeer()).handleChoiceMouseUp(cMousePos.X(),
								  cMousePos.Y(),
								  nButton);
    }
    
    /**
     * This method is called by the WindowManager when the mouse is moved.
     * The event is forwarded to the Choice component's peer.
     */
    public void mouseMoved (PixelPoint cMousePos, int nTransit) {
	//toolkit.message("mouse at " + cMousePos.X() + "," + cMousePos.Y() + ", transit is " + nTransit);
	if (parent.getPeer() != null)
	    ((JXChoicePeer) parent.getPeer()).handleChoiceMouseMoved(cMousePos.X(),
								     cMousePos.Y(),
								     lastButtonPressed,
								     (nTransit == WWindowInterface.MOUSE_INSIDE));
    }
    
    /**
     * This method is called by the WindowManager when the window needs
     * to be repainted. The event is forwarded to the Choice component's
     * peer.
     */
    public void paint(PixelRect cFrame) {
	if (parent.getPeer() != null)
	    ((JXChoicePeer) parent.getPeer()).redrawChoiceWindow(lastButtonPressed != 0);
    }
}
