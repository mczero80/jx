package jx.awt.peer;


import java.awt.*;
import java.awt.peer.*;
import java.awt.event.*;
import jx.wm.*;
import jx.wm.message.*;
import jx.devices.fb.*;
import jx.awt.*;



/**
 * This class represents the WindowManager interface for an AWT Frame. As it's
 * derived from GeneralConnector, it also represents a complete window.
 */
public class JXWindowConnector
    extends GeneralConnector {


    /** The AWT Window instance that belongs to this Connector */
    private Window parent;
    /** The peer that belongs to this Connector */
    private JXWindowPeer parentPeer;
    
    private boolean isActive = false;
    private boolean menuRange = false;
    private int lastPressedButton = 0;
    private Component target;
    private Component lastPressedTarget;
    private Component lastMovedTarget;
    private Component lastFocusedTarget;
    private int lastPressedKey;
    private JXComponentPeer peer;
    private int oldClipX, oldClipY, oldClipWidth, oldClipHeight;
    private int flags = WindowFlags.WND_TRANSPARENT|WindowFlags.WND_FULL_UPDATE_ON_RESIZE;
    
    
    
    /** Creates a new JXWindowConnector instance */
    public JXWindowConnector(JXToolkit toolkit, Window parent) {
	super(toolkit, "", new PixelRect(0, 0, 30, 30),
	      new WindowFlags(WindowFlags.WND_TRANSPARENT|WindowFlags.WND_FULL_UPDATE_ON_RESIZE));
	this.parent = parent;
	//componentOffsetX = 10;
    }
    
    /*****************************************************
     * service and adapter methods
     *****************************************************/
    
    
    /**
     * Returns a new copy of the associated JXGraphics object. This copy
     * has its origin set to (componentOffsetX, componentOffsetY) so that
     * component painting coords will be adjusted automatically.
     */
    public JXGraphics getComponentGraphics() {
	JXGraphics g = (JXGraphics) graphics.create();
	g.translate(componentOffsetX, componentOffsetY);
	return g;
    }
    
    /**
     * Returns a new copy of the associated JXGraphics object.
     */
    public JXGraphics getMenuGraphics() {
	return (JXGraphics) graphics.create();
    }
    
    /**
     * Sets the "resizable" flag of the window.
     */
    public void setResizable(boolean resizable) {
	if (resizable)
	    flags &= ~(WindowFlags.WND_NOT_RESIZABLE);
	else
	    flags |= WindowFlags.WND_NOT_RESIZABLE;
	setFlags(new WindowFlags(flags));
    }

    /**
     * Allows a window to hide its borders.
     */
    public void showBorder(boolean visible) {
	if (visible)
	    flags &= ~(WindowFlags.WND_NO_BORDER);
	else
	    flags |= WindowFlags.WND_NO_BORDER;
	setFlags(new WindowFlags(flags));
    }

    /**
     * Sets the current drawing clip of the window.
     */
    public void setClip(int x, int y, int w, int h) {
	// this method is overridden to avoid the same clipping
	// region being set several times
	if (x == oldClipX && y == oldClipY &&
	    w == oldClipWidth && h == oldClipHeight)
	    return;
	oldClipX = x;
	oldClipY = y;
	oldClipWidth = w;
	oldClipHeight = h;
	super.setClip(x, y, w, h);
    }

    /**
     * This method repaints the whole window.
     */
    private void doRepaint(boolean relayout) {
	if (parent != null) {
	    parentPeer = ((JXWindowPeer) parent.getPeer());
	    // do invalidation to signal Layout Manager to recalculate
	    if (relayout) {
		parentPeer.layoutMenu(parent.getWidth() + componentOffsetX,
				      parent.getHeight() + componentOffsetY);
		parent.invalidate();
		parent.validate();
	    }
	    // reshape window to fit current menu setting
	    if (componentOffsetY != parentPeer.getMenuHeight()) {
		Rectangle r = getBounds();
		componentOffsetY = parentPeer.getMenuHeight();
		setBounds(r.x, r.y, r.width, r.height);
	    }
	    // redraw parent frame (this implies redrawing of all
	    // components in this frame)
	    parentPeer.redraw();
	    Thread.yield();
	}
    }
    
    /**
     * Checks whether the mouse cursor is in the menu bar area or not.
     */
    private boolean inMenuRange(int x, int y) {
	return (y >= 0 && y < componentOffsetY &&
		x >= 0 && x < parent.getWidth() + componentOffsetX);
    }

    /**
     * This method finds the component that currently lies under the mouse cursor. Although
     * the Container class has its own method findComponentAt(), the method is insensitive
     * to the insets set in every container. Therefore, this method was created.
     */
    private Component findTargetComponent(Container container, int x, int y) {
	if (! container.contains (x, y))
	    return null;
	
	for (int i = 0; i < container.getComponentCount(); ++i)
	    {
		Component c = container.getComponent(i);
		// Ignore invisible children...
		if (!c.isVisible())
		    continue;
		
		int x2 = x - c.getX();
		int y2 = y - c.getY();
		if (c instanceof Container) {
		    if (c.contains(x2, y2)) {
			Insets is = ((Container) c).getInsets();
			if (x2 >= is.left && y2 >= is.top && x2 < c.getWidth() - is.right && y2 < c.getHeight() - is.bottom) {
			    // (x, y) inside the insets
			    Component r = findTargetComponent((Container) c, x2, y2);
			    if (r != null)
				return r;
			} else
			    // (x,y) lies in the border of container c
			    return c;
		    }
		} else if (c.contains (x2, y2))
		    return c;
	    }
	return container;
    }

    /**
     * Returns the component currently lying under the mouse cursor.
     */
    private Component getTarget(int x, int y) {
	if (parent == null)
	    return null;
	Component target = findTargetComponent(parent, x, y);
	//System.out.println("Component is " + ((target == null) ? "null" : target.getClass().getName()));
	return ((target == null) ? parent : target);
    }

    /**
     * Tells whether the component is prepared to be used.
     */
    private boolean validTarget(Component comp) {
	// still under construction...
	return (comp != null && comp.getPeer() != null &&
		comp.isEnabled() && comp.isVisible());
    }

    public void dispose() {
	parent = null;
	super.dispose();
    }
    
    /**
     * Informs the frame that the window size has been altered, and repaints
     * the window if necessary.
     */
    private void doWindowChange(PixelRect cFrame, boolean resized) {
	// disable connector methods to avoid feedback in setBounds()!
	connectorEnabled = false;
	parent.setBounds(cFrame.x0(), cFrame.y0(),
			 cFrame.width() - componentOffsetX + 1,
			 cFrame.height() - componentOffsetY + 1);
	connectorEnabled = true;
	if (resized)
	    doRepaint(true);
	Thread.yield();
    }

    
    /*****************************************************
     * Event controlled methods (listeners)               *
     *****************************************************/
    
    /**
     * This method is called by the WindowManager whenever a key has been pressed. When a
     * menu is open, then the event is forwarded to this menu, otherwise it is passed to
     * the component that has the keyboard focus.
     */
    public void keyDown (Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers) {
	//System.out.println("keyDown called (Keycode: " + eKeyCode +
	//", Rawcode: " + eRawCode + ", Qualifiers: " +
	//eQualifiers + ").");
	if (isActive) {
	    if (toolkit.getMenuHandler().isMenuOpen() || parentPeer.getCurrentMenu() != null) {
		// menu bar or menu is open, forward key to menu
		parentPeer.handleMenuKeyDown(KeyMap.translate(eKeyCode.getValue()));
	    } else {
		// handle ALT key (used to open menu bar)
		// NOTE: change when code for ALT has been added to Keycode!
		if (KeyMap.translate(eKeyCode.getValue(), eRawCode.getValue()) == KeyEvent.VK_ALT) {
		    return;
		}
		// find actual focus
		Component c = toolkit.getFocusHandler().getFocusedComponent();
		
		switch (eKeyCode.getValue()) {
		case Keycode.VK_TAB:
		    toolkit.getSlaveWindowHandler().performCloseOperation();
		    if (c == null)
			parent.transferFocus();
		    else
			c.transferFocus();
		default:
		    lastPressedKey = eKeyCode.getValue();
		}
		// forward key to focused component
		if (c != null)
		    ((JXComponentPeer) c.getPeer()).keyPressed(eKeyCode.getValue(),
							       eRawCode.getValue(),
							       eQualifiers.getValue());
	    }
	}
    }

    /**
     * This method is called by the WindowManager whenever a key has been released. When a
     * menu is open, then the event is forwarded to this menu, otherwise it is passed to
     * the component that has the keyboard focus.
     */
    public void keyUp (Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers) {
	//System.out.println("keyUp called (Keycode: " + eKeyCode +
	//", Rawcode: " + eRawCode + ", Qualifiers: " +
	//eQualifiers + ").");
	if (isActive) {
	    if (toolkit.getMenuHandler().isMenuOpen() || parentPeer.getCurrentMenu() != null) {
		// menu bar or menu is open, forward key to menu
		parentPeer.handleMenuKeyUp(KeyMap.translate(eKeyCode.getValue()));
	    } else {
		// handle ALT key (used to open menu bar)
		// NOTE: change when code for ALT has been added to Keycode!
		if (KeyMap.translate(eKeyCode.getValue(), eRawCode.getValue()) == KeyEvent.VK_ALT) {
		    parentPeer.activateMenuBar();
		    return;
		}
		// find actual focus
		Component c = toolkit.getFocusHandler().getFocusedComponent();
		// forward key to focused component
		if (c != null) {
		    if (lastPressedKey == eKeyCode.getValue())
			((JXComponentPeer) c.getPeer()).keyClicked(eKeyCode.getValue(),
								   eRawCode.getValue(),
								   eQualifiers.getValue());
		    if (c.getPeer() != null)
			((JXComponentPeer) c.getPeer()).keyReleased(eKeyCode.getValue(),
								    eRawCode.getValue(),
								    eQualifiers.getValue(),
								    (target == c));
		}
		lastPressedKey = 0;
	    }
	}
    }

    /**
     * This method is called by the WindowManager whenever a mouse button has been pressed.
     * When the mouse is inside the menu bar's area, then the menu bar gets the event. When
     * a menu is open, the event is ignored, otherwise the event is passed to the currently
     * underlying component.
     */
    public void mouseDown (PixelPoint cMousePos, int nButton) {
	/*	System.out.println("mouseDown called (Position: " + cMousePos.X() +
		"/" + cMousePos.Y() + ", Button: " + nButton +
		").");*/
	if (!isActive) return;
	
	if (inMenuRange(cMousePos.X(), cMousePos.Y()) && !toolkit.getMenuHandler().isPopupMenuOpen()) {
	    // forward mouse event to menu bar
	    parentPeer.handleMenuMouseDown(cMousePos.X(), cMousePos.Y(), nButton);
	} else {
	    // do NOT handle mouse events if a menu is open!
	    if (toolkit.getMenuHandler().isMenuOpen() ||
		toolkit.getSlaveWindowHandler().windowsRegistered())
		return;
	    // adjust coords to let the following code work properly
	    cMousePos.m_nX -= componentOffsetX;
	    cMousePos.m_nY -= componentOffsetY;
	    
	    if (lastPressedButton != 0)
		mouseUp(cMousePos, nButton);
	    
	    lastPressedButton = nButton;
	    target = getTarget(cMousePos.X(), cMousePos.Y());
	    
	    if (validTarget(target)) {
		lastPressedTarget = target;
		target.requestFocus();
		peer = ((JXComponentPeer) target.getPeer());
		peer.mousePressed(cMousePos.X(), cMousePos.Y(), nButton);
	    }
	    Thread.yield();
	}
    }
    
    /**
     * This method is called by the WindowManager whenever a mouse button has been released.
     * When the mouse is inside the menu bar's area, then the menu bar gets the event. When
     * a menu is open, the event is ignored, otherwise the event is passed to the currently
     * underlying component.
     */
    public void mouseUp (PixelPoint cMousePos, int nButton) {
	/*System.out.println("mouseUp called (Position: " + cMousePos.X() +
	  "/" + cMousePos.Y() + ", Button: " + nButton +
	  ").");*/
	if (!isActive) return;
	
	if (inMenuRange(cMousePos.X(), cMousePos.Y()) && !toolkit.getMenuHandler().isPopupMenuOpen()) {
	    // forward mouse event to menu bar
	    parentPeer.handleMenuMouseUp(cMousePos.X(), cMousePos.Y(), nButton);
	} else {
	    // do NOT handle mouse events if a menu is open!
	    if (toolkit.getMenuHandler().isMenuOpen() ||
		toolkit.getSlaveWindowHandler().windowsRegistered()) 
		return;
	    // adjust coords to let the following code work properly
	    cMousePos.m_nX -= componentOffsetX;
	    cMousePos.m_nY -= componentOffsetY;
	    
	    target = getTarget(cMousePos.X(), cMousePos.Y());
	    
	    if (target == lastPressedTarget && nButton == lastPressedButton) {
		if (validTarget(target)) {
		    peer = ((JXComponentPeer) target.getPeer());
		    peer.mouseClicked(cMousePos.X(), cMousePos.Y(), nButton);
		}
	    }
	    if (lastPressedTarget != null)
		if (validTarget(lastPressedTarget)) {
		    peer = ((JXComponentPeer) lastPressedTarget.getPeer());
		    peer.mouseReleased(cMousePos.X(), cMousePos.Y(), nButton,
				       (lastPressedTarget == target));
		}
	    lastPressedTarget = null;
	    lastPressedButton = 0;
	}
	Thread.yield();
    }
    
    /**
     * This method is called by the WindowManager whenever the mouse has been moved.
     * When the mouse cursor is in the menu bar area, then the area gets the event,
     * otherwise the event is handled here. All components that are involved in the
     * movement process, e.g. components that just have been left or entered by the
     * mouse cursor, are handled by invoking their relevant handler methods.
     */
    public void mouseMoved (PixelPoint cMousePos, int nTransit) {
	/*	System.out.println("mouseMoved called (Position: " + cMousePos.X() + 
		"/" + cMousePos.Y() + ", Transit: " + nTransit +
		").");*/
	if (!isActive) return;
	
	if (inMenuRange(cMousePos.X(), cMousePos.Y()) && !toolkit.getMenuHandler().isPopupMenuOpen()) {
	    // forward mouse event to menu bar
	    parentPeer.handleMenuMouseMoved(cMousePos.X(), cMousePos.Y());
	    menuRange = true;
	} else {
	    //if (toolkit.getMenuHandler().isMenuOpen() ||
	    //toolkit.getSlaveWindowHandler().windowsRegistered()) 
	    //return;

	    // update menu display if just leaving the "unused" menu bar
	    if (menuRange && !toolkit.getMenuHandler().isMenuOpen()) {
		parentPeer.resetMenuBar();
		menuRange = false;
	    }
	    // adjust coords to let the following code work properly
	    cMousePos.m_nX -= componentOffsetX;
	    cMousePos.m_nY -= componentOffsetY;
	    
	    // if mouse is pressed, then all actions are forwarded
	    // to the pressed target, else they are passed to the
	    // underlying target (useful for scroll bars!)
	    target = (lastPressedTarget != null) ? lastPressedTarget :
		getTarget(cMousePos.X(), cMousePos.Y());
	    
	    if (lastMovedTarget != target) {
		// we're moving from one component to the other
		// Allow peers to react to enter & exit events
		//System.out.println("switch from " + lastMovedTarget + " to " + target);
		if (validTarget(lastMovedTarget)) {
		    peer = ((JXComponentPeer) lastMovedTarget.getPeer());
		    peer.mouseExited(cMousePos.X(), cMousePos.Y(), lastPressedButton);
		}
		if (validTarget(target)) {
		    peer = ((JXComponentPeer) target.getPeer());
		    peer.mouseEntered(cMousePos.X(), cMousePos.Y(), lastPressedButton);
		}
	    } else {
		// we're moving on the same component (frame is special case!)
		if (validTarget(target) && target != parent) {
		    peer = ((JXComponentPeer) target.getPeer());
		    peer.mouseMoved(cMousePos.X(), cMousePos.Y(), lastPressedButton);
		}
		if (target == parent)
		    // getTarget() returned parent frame, requires special attention
		    switch (nTransit) {
		    case WWindowInterface.MOUSE_ENTERED:
			peer = ((JXComponentPeer) target.getPeer());
			peer.mouseEntered(cMousePos.X(), cMousePos.Y(), lastPressedButton);
			break;
		    case WWindowInterface.MOUSE_EXITED:
			peer = ((JXComponentPeer) target.getPeer());
			peer.mouseExited(cMousePos.X(), cMousePos.Y(), lastPressedButton);
			break;
		    case WWindowInterface.MOUSE_INSIDE:
			peer = ((JXComponentPeer) target.getPeer());
			peer.mouseMoved(cMousePos.X(), cMousePos.Y(), lastPressedButton);
			break;
		    case WWindowInterface.MOUSE_OUTSIDE:
		    default:break;
		    }
	    }
	    // called if mouse button is pressed
	    if (lastPressedButton != 0) {
		if (validTarget(target)) {
		    peer = ((JXComponentPeer) target.getPeer());
		    peer.mouseDragged(cMousePos.X(), cMousePos.Y(), lastPressedButton);
		}
	    }
	    lastMovedTarget = target;
	}
	Thread.yield();
    }

    /**
     * This method is called by the WindowManager whenever the window is activated
     * or deactivated, or when the window borders are selected. Here, all open
     * windows (menus or "slave" windows) are closed, and the focus is set, if
     * necessary.
     */
    public void windowActivated (boolean bActivated) {
	// close all open menu windows
	MenuHandler mh = toolkit.getMenuHandler();
	if (mh.isMenuOpen()) {
	    mh.deleteOpenMenus();
	    parentPeer.resetMenuBar();
	}
	// perform any close handling operation on registered windows
	toolkit.getSlaveWindowHandler().performCloseOperation();

	if (isActive == bActivated)
	    return;
	
	isActive = bActivated;
	if (isActive) {
	    // get focus if lost
	    if (lastFocusedTarget == null)
		parent.requestFocus();
	    else
		lastFocusedTarget.requestFocus();
	} else {
	    // save this focused component
	    lastFocusedTarget = toolkit.getFocusHandler().getFocusedComponent();
	}
	parentPeer.windowActivated(isActive);
    }

    /**
     * This method is called by the WindowManager whenever the window
     * has been resized.
     */
    public void windowResized (PixelRect cFrame) {
	doWindowChange(cFrame, true);
    }
    
    /**
     * This method is called by the WindowManager whenever the window
     * has been moved.
     */
    public void windowMoved (PixelRect cFrame) {
	doWindowChange(cFrame, false);
    }
    
    /**
     * This method is called by the WindowManager whenever the window's
     * close button has been pressed.
     */
    public void closeRequested() {
	parentPeer.closeRequested();
    }

    /**
     * This method is called by the WindowManager whenever some part of
     * the window needs to be repainted.
     */
    public void paint(PixelRect cFrame) {
	// do repaint
	doRepaint(true);
    }
}
