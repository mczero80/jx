package jx.awt.peer;

import java.awt.*;
import jx.awt.*;


/**
 * This class implements a ScrollPane peer.
 */
public class JXScrollPanePeer
    extends JXContainerPeer
    implements java.awt.peer.ScrollPanePeer {

    /** mouse is in no scroll bar */
    private final int NONE = 0;
    /** mouse is in right scroll bar */
    private final int RIGHTBAR = 2;
    /** mouse is in lower scroll bar */
    private final int LOWERBAR = 3;

    private Point pos;
    private int policy;
    private Dimension viewPort;
    private int lastArea;
    private int lastPressedArea;
    private int barSize;


    /** Creates a new JXScrollPanePeer instance */
    public JXScrollPanePeer (ScrollPane parent, JXToolkit toolkit) {
	super(toolkit, parent);
	InternalScrollbar is = new InternalScrollbar(null, null);
	barSize = is.SLIDERWIDTH;

	policy = parent.getScrollbarDisplayPolicy();
	pos = parent.getScrollPosition();
	if (getChild() != null) {
	    // try to set preferred size
	    prefWidth = getChild().getPreferredSize().width;
	    prefHeight = getChild().getPreferredSize().height;
	}
	if (prefWidth <= 0)
	    prefWidth = 100;
	if (prefHeight <= 0)
	    prefHeight = 100;
	ready = true;
    }


    



    /** Handles all mouse button presses in the scroll pane. */
    public void mousePressed(int x, int y, int button) {
	super.mousePressed(x, y, button);
	switch(lastArea) {
	case RIGHTBAR:getVScrollPeer().mousePressed(x, y, button);break;
	case LOWERBAR:getHScrollPeer().mousePressed(x, y, button);break;
	}
	lastPressedArea = lastArea;
    }
    
    /** Handles all mouse button releases in the scroll pane. */
    public void mouseReleased(int x, int y, int button, boolean over) {
	super.mouseReleased(x, y, button, over);
	switch (lastArea) {
	case RIGHTBAR:
	    getVScrollPeer().mouseReleased(x, y, button, over);
	    // update scroll position
	    pos.y = getVScrollPeer().getValue();
	    redraw();
	    break;
	case LOWERBAR:
	    getHScrollPeer().mouseReleased(x, y, button, over);
	    // update scroll position
	    pos.x = getHScrollPeer().getValue();
	    redraw();
	    break;
	}
	lastPressedArea = NONE;
    }
    
    /** Handles mouse movement in the scroll pane. */
    public void mouseMoved(int x, int y, int button) {
	super.mouseMoved(x, y, button);
	int newArea = getCurrentArea(x, y);
	if (newArea != lastArea) {
	    // handle area change
	    switch (lastArea) {
	    case RIGHTBAR:getVScrollPeer().mouseExited(x, y, button);break;
	    case LOWERBAR:getHScrollPeer().mouseExited(x, y, button);break;
	    }
	    switch (newArea) {
	    case RIGHTBAR:getVScrollPeer().mouseEntered(x, y, button);break;
	    case LOWERBAR:getHScrollPeer().mouseEntered(x, y, button);break;
	    }
	}
	lastArea = newArea;

	switch (lastArea) {
	case RIGHTBAR:getVScrollPeer().mouseMoved(x, y, button);break;
	case LOWERBAR:getHScrollPeer().mouseMoved(x, y, button);break;
	}
	if (button != 0) {
	    // handle dragging
	    switch (lastPressedArea) {
	    case RIGHTBAR:
		int ny = getVScrollPeer().getValue();
		if (ny != pos.y) {
		    pos.y = ny;
		    redraw();
		}
		break;
	    case LOWERBAR:
		int nx = getHScrollPeer().getValue();
		if (nx != pos.x) {
		    pos.x = nx;
		    redraw();
		}
		break;
	    }
	}
    }
    
    public void mouseEntered(int x, int y, int button) {
	super.mouseEntered(x, y, button);
	lastArea = getCurrentArea(x, y);
	switch (lastArea) {
	case RIGHTBAR:getVScrollPeer().mouseEntered(x, y, button);break;
	case LOWERBAR:getHScrollPeer().mouseEntered(x, y, button);break;
	}
    }
    
    public void mouseExited(int x, int y, int button) {
	super.mouseExited(x, y, button);
	switch (lastArea) {
	case RIGHTBAR:getVScrollPeer().mouseExited(x, y, button);break;
	case LOWERBAR:getHScrollPeer().mouseExited(x, y, button);break;
	}
	lastArea = NONE;
    }
    
    /** Paints this peer. */
    public void paint(JXGraphics g) {
	resetLayout();
	paintScrollPane(g);
    }

    /** Recalculates all bars and the scroll pane layout. */
    private void resetLayout() {
	if (getChild() == null)
	    return;
	Dimension d = getChild().getSize();
	viewPort = new Dimension(width, height);
	// calculate child window
	boolean changed;
	if (policy != ScrollPane.SCROLLBARS_NEVER)
	    // find out if scrollbars need to be visible or not
	    do {
		changed = false;
		// every branch must be executed only once!!!
		if (viewPort.height == height) 
		    if (getHScrollbar() != null && 
			(policy == ScrollPane.SCROLLBARS_ALWAYS || d.width > viewPort.width)) {
			if (d.width > viewPort.width)
			    changed = true;
			viewPort.height -= getHScrollbarHeight();
		    }
		if (viewPort.width == width)
		    if (getVScrollbar() != null && 
			(policy == ScrollPane.SCROLLBARS_ALWAYS || d.height > viewPort.height)) {
			if (d.height > viewPort.height)
			    changed = true;
			viewPort.width -= getVScrollbarWidth();
		    }
	    } while (changed);
	// reset scroll position
	if (pos.y + viewPort.height > d.height)
	    pos.y = d.height - viewPort.height;
	if (pos.y < 0)
	    pos.y = 0;
	if (pos.x + viewPort.width > d.width)
	    pos.x = d.width - viewPort.width;
	if (pos.x < 0)
	    pos.x = 0;
	// reset vertical bar
	if (viewPort.width != width) {
	    getVScrollbar().setVisible(true);
	    getVScrollbar().setValues(pos.y, viewPort.height, 0, d.height);
	    getVScrollbar().setPageIncrement(viewPort.height);
	} else
	    if (getVScrollbar() != null)
		getVScrollbar().setVisible(false);
	// reset horizontal bar
	if (viewPort.height != height) {
	    getHScrollbar().setVisible(true);
	    getHScrollbar().setValues(pos.x, viewPort.width, 0, d.width);
	    getHScrollbar().setPageIncrement(viewPort.width);
	} else
	    if (getHScrollbar() != null)
		getHScrollbar().setVisible(false);
    }

    /** Sets the insets according to the visible scroll bars. */
    public Insets insets() {
	int r = (getVScrollbar() != null && getVScrollbar().isVisible()) ? getVScrollbarWidth() : 0;
	int u = (getHScrollbar() != null && getHScrollbar().isVisible()) ? getHScrollbarHeight() : 0;
	return new Insets(0, 0, u, r);
    }

    /** Gets the vertical scroll bar. */
    private Scrollbar getVScrollbar() {
	return (Scrollbar) ((ScrollPane) parent).getVAdjustable();
    }

    /** Gets the horizontal scroll bar. */
    private Scrollbar getHScrollbar() {
	return (Scrollbar) ((ScrollPane) parent).getHAdjustable();
    }

    /** Gets the vertical scroll bar peer. */
    private JXScrollbarPeer getVScrollPeer() {
	return (JXScrollbarPeer) ((Scrollbar) ((ScrollPane) parent).getVAdjustable()).getPeer();
    }

    /** Gets the horizontal scroll bar peer. */
    private JXScrollbarPeer getHScrollPeer() {
	return (JXScrollbarPeer) ((Scrollbar) ((ScrollPane) parent).getHAdjustable()).getPeer();
    }

    /** Gets the embedded child component. */
    private Component getChild() {
	return ((Container) parent).getComponent(0);
    }

    /** Returns the area where the mouse cursor currently is. */
    private int getCurrentArea(int x, int y) {
	Point p = getLocationRelativeToComponent(x, y);
	if (p.x >= viewPort.width && p.y >= viewPort.height)
	    return NONE;
	if (p.x >= viewPort.width && getVScrollPeer() != null && getVScrollbar().isVisible())
	    return RIGHTBAR;
	if (p.y >= viewPort.height && getHScrollPeer() != null && getHScrollbar().isVisible())
	    return LOWERBAR;
	return NONE;
    }

    /** Returns the current scroll position. */
    public Point getScrollPosition() {
	return pos;
    }

    /***********************************************
     * methods implemented from ScrollPanePeer      *
     ***********************************************/
    
    public void childResized(int width, int height) {}
    public void setUnitIncrement(Adjustable item, int inc) {}
    public void setValue(Adjustable item, int value) {}

    public int getHScrollbarHeight() {
	return barSize; // = InternalScrollbar.SLIDERWIDTH
    }

    public int getVScrollbarWidth() {
	return barSize; // = InternalScrollbar.SLIDERWIDTH
    }

    public void setScrollPosition(int h, int v) {
	pos.x = h;
	pos.y = v;
	//redraw();
    }

    /*********************** DRAWING **************************/

    /** Paints the scroll pane. */
    public void paintScrollPane(JXGraphics g) {
	g.setColor(JXColors.normalBgColor);
	g.fillRect(0, 0, width - 1, height - 1);

	// draw bars
	if (viewPort.width != width) {
	    getVScrollbar().setBounds(viewPort.width, 0, width - viewPort.width, viewPort.height);
	    ((JXScrollbarPeer) getVScrollbar().getPeer()).redraw();
	}
	if (viewPort.height != height) {
	    getHScrollbar().setBounds(0, viewPort.height, viewPort.width, height - viewPort.height);
	    ((JXScrollbarPeer) getHScrollbar().getPeer()).redraw();
	}
    }
}
