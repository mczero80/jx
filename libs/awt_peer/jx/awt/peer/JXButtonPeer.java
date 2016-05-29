package jx.awt.peer;


import java.awt.*;
import java.awt.event.*;
import java.awt.peer.ButtonPeer;
import jx.awt.*;


/**
 * This class implements a Button peer.
 */
public class JXButtonPeer
    extends JXComponentPeer
    implements java.awt.peer.ButtonPeer {

    /** The border around the button's text */
    private final static int BORDER = 4;
    /** The button's text */
    private String text;


    /** Creates a new JXButtonPeer instance */
    public JXButtonPeer(Button parent, JXToolkit toolkit) {
	super(toolkit, parent);
	setLabel(parent.getLabel());
	ready = true;
    }




    /**
     * Extends keyPressed from JXComponentPeer. If the space key is pressed,
     * the button state is set to PEER_PRESSED.
     */
    public void keyPressed(int keyValue, int rawValue, int modifier) {
	super.keyPressed(keyValue, rawValue, modifier);
	if (keyValue == KeyEvent.VK_SPACE) {
	    peerState = PEER_PRESSED;
	    redraw();
	}
    }

    /**
     * Extends keyReleased from JXComponentPeer. If the space key is released,
     * the button state is set back to normal state.
     */
    public void keyReleased(int keyValue, int rawValue, int modifier, boolean over) {
	super.keyReleased(keyValue, rawValue, modifier, over);
	if (keyValue == KeyEvent.VK_SPACE) {
	    peerState = (over) ? PEER_HOVER : PEER_NORMAL;
	    redraw();
	}
    }
    
    /**
     * Extends keyClicked from JXComponentPeer. If the space key is typed,
     * an action event is sent.
     */
    public void keyClicked(int keyValue, int rawValue, int modifier) {
	super.keyClicked(keyValue, rawValue, modifier);
	if (keyValue == KeyEvent.VK_SPACE)
	    sendActionEvent();
    }

    /** Simply redraws the button. */
    public void mousePressed(int x, int y, int button) {
	super.mousePressed(x, y, button);
	redraw();
    }
    
    /** Simply redraws the button. */
    public void mouseReleased(int x, int y, int button, boolean over) {
	super.mouseReleased(x, y, button, over);
	redraw();
    }
    
    /** Simply redraws the button. */
    public void mouseClicked(int x, int y, int button) {
	super.mouseClicked(x, y, button);
	sendActionEvent();
    }

    /** Simply redraws the button. */
    public void mouseEntered(int x, int y, int button) {
	super.mouseEntered(x, y, button);
	redraw();
    }
    
    /** Simply redraws the button. */
    public void mouseExited(int x, int y, int button) {
	super.mouseExited(x, y, button);
	redraw();
    }

    /** Sends an action event. */
    private void sendActionEvent() {
	queue = toolkit.getSystemEventQueue();
	queue.postEvent(new ActionEvent(parent, ActionEvent.ACTION_PERFORMED,
					((Button) parent).getActionCommand(), 0));
    }
    
    /** Make a button traversable to keyboard focus. */
    public boolean isFocusTraversable() {
	return true;
    }

    /** Paints this peer. */
    public void paint(JXGraphics g) {
	boolean focus = hasFocus();
	switch (peerState) {
	case PEER_PRESSED:paintButton(g, focus, true, false, false);break;
	case PEER_NORMAL:paintButton(g, focus, false, false, false);break;
	case PEER_HOVER:paintButton(g, focus, false, true, false);break;
	case PEER_DISABLED:paintButton(g, focus, false, false, true);break;
	default:System.out.println("*** JXButtonPeer: Unknown state!!!");
	}
    }

    /**********************************************
     * methods implemented from ButtonPeer         *
     **********************************************/

    public void setLabel(String text) {
	this.text = text;
	prefWidth = 2 * BORDER + text.length() * CHARWIDTH;
	prefHeight = 2 * BORDER + CHARHEIGHT;
	redraw();
    }

    /************************** DRAWING *****************************/

    /** Paints the button. */
    protected void paintButton(JXGraphics g, boolean focus, boolean pressed,
			       boolean hover, boolean disabled) {
	int px = 0;
	int py = 0;
	if (pressed) {
	    px++;
	    py++;
	}
	if (hover)
	    g.setColor(JXColors.hoverColor);
	else
	    g.setColor(JXColors.normalBgColor);
	g.fill3DRect(0, 0, width, height, !pressed);
	if (focus) {
	    g.setColor(JXColors.focusColor);
	    g.drawRect(1, 1, width - 3, height - 3);
	}
	if (disabled)
	    g.setColor(JXColors.disabledTextColor);
	else
	    g.setColor(JXColors.normalTextColor);
	int tx = px + (width  - text.length() * CHARWIDTH) / 2;
	int ty = px + (height - CHARHEIGHT) / 2;
	g.drawJXString(text, tx, ty);
    }

}

