package jx.awt.peer;

import java.awt.*;
import java.awt.event.*;
import jx.awt.*;


/**
 * This is the abstract superclass for all peers that have direct
 * access to a native WindowManager window.
 */
public abstract class JXWindowPeer
    extends JXContainerPeer
    implements java.awt.peer.WindowPeer {
    
    /** The underlying WindowManager window */
    protected JXWindowConnector connector;


    
    /** Creates a new JXWindowPeer instance */
    public JXWindowPeer(JXToolkit toolkit, Window parent) {
	super(toolkit, parent);
	connector = new JXWindowConnector(toolkit, parent);
	initWindow();
    }




    /**********************************************************
     * Menu related methods expected from JXWindowConnector.   *
     * As they are used only in JXFramePeer, they are left     *
     * empty here or return dummy values.                      *
     **********************************************************/

    public void layoutMenu(int width, int height) {}
    public void activateMenuBar() {}
    public void resetMenuBar() {}
    public Menu getCurrentMenu() {return null;}
    public int getMenuHeight() {return 0;}

    public void handleMenuKeyUp(int code) {}
    public void handleMenuKeyDown(int code) {}
    public void handleMenuMouseUp(int mx, int my, int button) {}
    public void handleMenuMouseDown(int mx, int my, int button) {}
    public void handleMenuMouseMoved(int mx, int my) {}

    /**********************************************************
     * Common methods needed by all derived classes            *
     **********************************************************/

    /**
     * Sets the window to default values. This method is called every time
     * a new connector is created.
     */
    protected void initWindow() {
	Rectangle bounds = parent.getBounds();
	setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Posts a window event in the AWT event queue.
     */
    protected void sendWindowEvent(int what) {
	queue = toolkit.getSystemEventQueue();
	queue.postEvent(new WindowEvent((Window) parent, what));
    }

    /**
     * Shows or hides the border of the window.
     */
    public void showBorder(boolean visible) {
	connector.showBorder(visible);
    }

    /**
     * This method is called whenever a window becomes activated or
     * deactivated.
     */
    public void windowActivated(boolean active) {
	int what = (active)
	    ? WindowEvent.WINDOW_ACTIVATED
	    : WindowEvent.WINDOW_DEACTIVATED;
	sendWindowEvent(what);
    }

    /**
     * This method is called whenever the close button is pressed on
     * the window.
     */
    public void closeRequested() {
	sendWindowEvent(WindowEvent.WINDOW_CLOSING);
    }

    /**
     * Shows or hides the window.
     */
    public void setVisible(boolean visible) {
	// if visible window is set to visible
	// again (perhaps to update window), then
	// just exit here
	if (isVisible && visible)
	    return;

	super.setVisible(visible);
	if (visible) {
	    if (connector == null) {
		connector = new JXWindowConnector(toolkit, (Frame) parent);
		initWindow();
	    } else
		sendWindowEvent(WindowEvent.WINDOW_OPENED);
	    connector.show(true);
	}
	if (!visible) {
	    connector.dispose();
	    connector = null;
	}
    }

    /**
     * Sets the bounds of the window.
     */
    public void setBounds(int x, int y, int width, int height) {
	super.setBounds(x, y, width, height);
	connector.setBounds(x, y, width, height);
    }

    /**
     * Gets a valid Graphics object that can be used to perform drawing
     * operations on the window.
     */
    public Graphics getGraphics() {
	return connector.getComponentGraphics();
    }

    /**
     * Returns the coords of the upper left window corner.
     */
    public Point getLocationOnScreen() {
	return connector.getComponentAreaOrigin();
    }

    /**
     * Gets the preferred size of the window (normally not used).
     */
    public Dimension getPreferredSize() {
	// should perhaps be replaced by LayoutManager hints
	return new Dimension(50,50);
    }

    /**
     * Resets the insets.
     */
    public Insets insets() {
	return new Insets(0, 0, 0, 0);
    }
    
    /****************************************************
     * methods implemented from JXWindowPeer             *
     ****************************************************/
    
    /**
     * Moves the window below all other windows.
     */
    public void toBack() {}

    /**
     * Moves the window on top of all other windows.
     */
    public void toFront() {
	connector.moveToFront();
    }

}
