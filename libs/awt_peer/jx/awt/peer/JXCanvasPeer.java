package jx.awt.peer;


import java.awt.*;
import java.awt.event.*;
import java.awt.peer.CanvasPeer;
import jx.awt.*;


/**
 * This class implements a Canvas peer.
 */
public class JXCanvasPeer
    extends JXComponentPeer
    implements java.awt.peer.CanvasPeer {

    
    /** Creates a new JXCanvasPeer instance */
    public JXCanvasPeer(Canvas parent, JXToolkit toolkit) {
	super(toolkit, parent);
	ready = true;
    }


    /** Paints this peer. */
    public void paint(JXGraphics g) {
	paintCanvas(g);
    }

    /************************* DRAWING ***************************/

    /** Paints the Canvas (only the peer part!) */
    public void paintCanvas(JXGraphics g) {
	// Drawing is not necessary, as JXFramePeer already
	// cleaned the background, and all real drawing
	// should occur at the user level.
	// The AWT user part of the Canvas is drawn
	// by the Canvas itself.
    }
}
