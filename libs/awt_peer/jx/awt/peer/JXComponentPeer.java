package jx.awt.peer;

import java.awt.*;
import java.awt.event.*;
import jx.awt.*;


/**
 * This is the abstract superclass of all JX AWT peers. It provides
 * some common methods and implements some low-level event handlers.
 */
public abstract class JXComponentPeer {
    
    /** The width of a character of the default font */
    protected final static int CHARWIDTH = 9;
    /** The height of a character of the default font */
    protected final static int CHARHEIGHT = 14;

    /** Current peer is normal */
    protected final static int PEER_NORMAL = 0;
    /** Current peer is pressed */
    protected final static int PEER_PRESSED = 1;
    /** Mouse is over current peer */
    protected final static int PEER_HOVER = 2;
    /** Current peer is disabled */
    protected final static int PEER_DISABLED = 3;

    /** A reference to the global JXToolkit instance */
    protected JXToolkit toolkit;
    /** The corresponding component */
    protected Component parent;
    /** A link used to access the event queue */
    protected EventQueue queue;
    /** The x coord of the upper left corner of the peer */
    protected int x;
    /** The y coord of the upper left corner of the peer */
    protected int y;
    /** The width of the peer */
    protected int width;
    /** The height of the peer */
    protected int height;
    /** The preferred width of the peer */
    protected int prefWidth;
    /** The preferred height of the peer */
    protected int prefHeight;
    /** The peer's state (one of the above) */
    protected int peerState = PEER_NORMAL;
    /** Flag indicating whether this peer is enabled or not */
    protected boolean isEnabled = true;
    /** Flag indicating whether this peer is visible or not */
    protected boolean isVisible = true;
    /** Flag indicating this peer is ready to be drawn */
    protected boolean ready = false;

    
    
    /** Creates a new JXComponentPeer instance. */
    public JXComponentPeer(JXToolkit toolkit, Component parent) {
	super();
	this.toolkit = toolkit;
	this.parent = parent;
	setEnabled(parent.isEnabled());
	isVisible = parent.isVisible();
    }



    /******************************************************
     * methods to provide event handling
     ******************************************************/

    /**
     * Forces the peer to repaint itself. This method must be
     * implemented by all JX component peers to assure they
     * can be painted when necessary.
     */
    public abstract void paint(JXGraphics g);

    /**
     * Used from Component to force the peer to repaint itself.
     * This method is used only to be compatible to Component.
     * To implemented own drawing code, override the method
     * above.
     */
    public final void paint(Graphics g) {
	paint((JXGraphics) g);
    }

    /**
     * This method is called by JXWindowConnector when a key press should be passed
     * to this peer. You can override it, but do not forget to call super.keyPressed()
     * in your own method!
     */
    public void keyPressed(int keyValue, int rawValue, int modifier) {
	sendKeyEvent(KeyEvent.KEY_PRESSED, keyValue, rawValue, modifier);
    }

    /**
     * This method is called by JXWindowConnector when a key release should be passed
     * to this peer. You can override it, but do not forget to call super.keyReleased()
     * in your own method!
     */
    public void keyReleased(int keyValue, int rawValue, int modifier, boolean over) {
	sendKeyEvent(KeyEvent.KEY_RELEASED, keyValue, rawValue, modifier);
    }

    /**
     * This method is called by JXWindowConnector when a key click should be passed
     * to this peer. You can override it, but do not forget to call super.keyClicked()
     * in your own method!
     */
    public void keyClicked(int keyValue, int rawValue, int modifier) {
	sendKeyEvent(KeyEvent.KEY_TYPED, keyValue, rawValue, modifier);
    }

    /**
     * This method is called by JXWindowConnector when a mouse button has been pressed
     * on this peer. You can override it, but do not forget to call super.mousePressed()
     * in your own method!
     */
    public void mousePressed(int x, int y, int button) {
	peerState = PEER_PRESSED;
	sendMouseEvent(MouseEvent.MOUSE_PRESSED, x, y, button);
    }

    /**
     * This method is called by JXWindowConnector when a mouse button has been released
     * on this peer. You can override it, but do not forget to call super.mouseReleased()
     * in your own method!
     */
    public void mouseReleased(int x, int y, int button, boolean over) {
	peerState = (over) ? PEER_HOVER : PEER_NORMAL;
	sendMouseEvent(MouseEvent.MOUSE_RELEASED, x, y, button);
    }

    /**
     * This method is called by JXWindowConnector when a mouse button has been clicked
     * on this peer. You can override it, but do not forget to call super.mouseClicked()
     * in your own method!
     */
    public void mouseClicked(int x, int y, int button) {
	sendMouseEvent(MouseEvent.MOUSE_CLICKED, x, y, button);
    }

    /**
     * This method is called by JXWindowConnector when the mouse has entered this
     * peer. You can override it, but do not forget to call super.mouseEntered()
     * in your own method!
     */
    public void mouseEntered(int x, int y, int button) {
	peerState = (isEnabled) ? PEER_HOVER : PEER_DISABLED;
	sendMouseEvent(MouseEvent.MOUSE_ENTERED, x, y, button);
    }

    /**
     * This method is called by JXWindowConnector when the mouse has left this
     * peer. You can override it, but do not forget to call super.mouseExited()
     * in your own method!
     */
    public void mouseExited(int x, int y, int button) {
	peerState = (isEnabled) ? PEER_NORMAL : PEER_DISABLED;
	sendMouseEvent(MouseEvent.MOUSE_EXITED, x, y, button);
    }

    /**
     * This method is called by JXWindowConnector when the mouse is moved over
     * this peer. You can override it, but do not forget to call super.mouseMoved()
     * in your own method!
     */
    public void mouseMoved(int x, int y, int button) {
	sendMouseEvent(MouseEvent.MOUSE_MOVED, x, y, button);
    }

    /**
     * This method is called by JXWindowConnector when the mouse is dragged over
     * this peer. You can override it, but do not forget to call super.mouseDragged()
     * in your own method!
     */
    public void mouseDragged(int x, int y, int button) {
	sendMouseEvent(MouseEvent.MOUSE_DRAGGED, x, y, button);
    }
    
    /**
     * Posts a key event to the AWT event queue.
     */
    protected void sendKeyEvent(int what, int code, int rcode, int mods) {
	//toolkit.message("send key message to " + parent);
	int keycode = KeyMap.translate(code, rcode);
	int modifiers = 0;
	if (KeyMap.shiftPressed(mods)) modifiers |= InputEvent.SHIFT_MASK;
	if (KeyMap.altPressed(mods)) modifiers |= InputEvent.ALT_MASK;
	if (KeyMap.ctrlPressed(mods)) modifiers |= InputEvent.CTRL_MASK;
	queue = toolkit.getSystemEventQueue();
	queue.postEvent(new KeyEvent(parent, what, modifiers, keycode));
    }

    /**
     * Posts a mouse event to the AWT event queue.
     */
    protected void sendMouseEvent(int what, int x, int y, int button) {
	//toolkit.message("send mouse message to " + parent);
	Point p = getLocationRelativeToComponent(x, y);
	int mButton = 0;
	if (button == 1) mButton |= InputEvent.BUTTON1_MASK;
	if (button == 2) mButton |= InputEvent.BUTTON3_MASK;
	queue = toolkit.getSystemEventQueue();
	queue.postEvent(new MouseEvent(parent, what, mButton, p.x, p.y,
				       0, (button == 2)));
    }

    /**
     * Posts a component event to the AWT event queue.
     */
    protected void sendComponentEvent(int what) {
	queue = toolkit.getSystemEventQueue();
	queue.postEvent(new ComponentEvent(parent, what));
    }

    /******************************************************
     * common methods required for peer layout and handling
     ******************************************************/

    /**
     * Returns whether this peer is enabled or not.
     */
    public boolean enabled() {
	return isEnabled;
    }

    /**
     * Returns whether this peer is visible or not.
     */
    public boolean visible() {
	return isVisible;
    }

    /**
     * Starts the painting of this peer and its child peers. This
     * method is called whenever a peer needs to be repainted.
     */
    public final void redraw() {
	if (ready && parent != null) {
	    JXGraphics g = getJXGraphics();
	    // enable back buffer
	    g.setBufferCount();
	    // do redrawing
	    redrawComponent();
	    // draw back buffer
	    g.drawBackBuffer();
	}
    }

    /**
     * Repaints this peer. This method should never be overriden by a peer!
     * To write own painting code, override the paint(JXGraphics) method.
     */
    protected void redrawComponent() {
	if (ready && parent != null) {
	    JXGraphics g = getJXGraphics();
	    // do all component depending painting if component
	    // is visible
	    if (isVisible) {
		// paint JX Peer
		paint(g);
		// paint AWT Component
		queue = toolkit.getSystemEventQueue();
		queue.postEvent(new PaintEvent(parent, PaintEvent.PAINT,
					       new Rectangle (x, y, width, height)));
	    } else
		paintInvisibleComponent(g);
	}
    }
    
    /** Tells whether this peer has the keyboard focus or not */
    protected boolean hasFocus() {
	// this peer has focus if then focused component
	// is the own parent
	return (toolkit.getFocusHandler().getFocusedComponent() == parent);
    }

    /** Sets the keyboard focus to this peer */
    public void setFocus() {
	toolkit.getFocusHandler().setFocusedComponent(parent);
    }

    /**
     * Converts the coords (x,y) which are relative to the hosting window
     * to new coords which are relative to this component peer
     */
    protected Point getLocationRelativeToComponent(int x, int y) {
	Point p2 = getJXGraphics().getLocationRelativeToComponent(x, y);
	//System.out.println("Old coords: " + x + "," + y);
	//System.out.println("New coords: " + p2.x + "," + p2.y);
	return p2;
    }
    
    /**
     * Converts the coords (x,y) which are relative to this component peer
     * to new coords which are relative to the hosting window
     */
    protected Point getLocationRelativeToWindow(int x, int y) {
	Point p2 = getJXGraphics().getLocationRelativeToWindow(x, y);
	//System.out.println("Old coords: " + x + "," + y);
	//System.out.println("New coords: " + p2.x + "," + p2.y);
	return p2;
    }

    /**
     * Gets the host window into which this peer is embedded.
     */
    protected Window getHostWindow() {
	Component c = parent;
	while (!(c instanceof Window))
	    c = c.getParent();
	return (Window) c;
    }

    /**
     * Gets a valid JXGraphics object that enables this peer to paint itself.
     */
    protected JXGraphics getJXGraphics() {
	int tx = 0;
	int ty = 0;
	int sx = 0;
	int sy = 0;
	Dimension viewPort = null;
	Component c = parent;
	
	// find direct window connection
	while (!(c instanceof Frame)) {
	    // adjust translation coords
	    Rectangle bounds = c.getBounds();
	    tx += bounds.x;
	    ty += bounds.y;
	    // check if component is (real) child of ScrollPane
	    // and prepare JXGraphics scroll coords
	    if (c.getParent() instanceof ScrollPane &&
		(c != ((ScrollPane) c.getParent()).getHAdjustable() &&
		 c != ((ScrollPane) c.getParent()).getVAdjustable())) {
		// NOTE: supports only one ScrollPane in container hierarchy!!!
		viewPort = ((ScrollPane) c.getParent()).getViewportSize();
		// adjust scroll coords
		Point p = ((JXScrollPanePeer) c.getParent().getPeer()).getScrollPosition();
		sx -= p.x;
		sy -= p.y;
	    }
	    c = c.getParent();
	}
	// get JXGraphics object from underlying Window
	JXGraphics g = (JXGraphics) ((JXFramePeer) c.getPeer()).getGraphics();
	// set viewport clip if necessary
	if (viewPort != null) {
	    Point p = g.getTranslationOrigin();
	    p.x += tx;
	    p.y += ty;
	    //System.out.println("Origin is " + p + ",viewPort is " + viewPort);
 	    g.setViewClip(p.x, p.y, viewPort.width, viewPort.height);
	}
	// set coords and user clip
	g.translate(tx + sx, ty + sy);
	g.setClip(0, 0, width, height);
	return g;
    }

    /************************************************
     * common methods required for (Component) peers
     ************************************************/


    public void setCursor(Cursor cursor) {}
    public void print(Graphics g) {}
    //public void setEventMask(long mask) {}
    public void setEventMask(int mask) {}
    public void handleEvent(AWTEvent e) {}
    public void setForeground(Color color) {}
    public void setBackground(Color color) {}



    public void setVisible(boolean visible) {
	ready = visible;

	isVisible = visible;
	int evt = (visible)
	    ? ComponentEvent.COMPONENT_SHOWN
	    : ComponentEvent.COMPONENT_HIDDEN;
	sendComponentEvent(evt);
	// only redraw if no Frame or Dialog
	if (!(parent instanceof Window))
	    redraw();
    }

    public void requestFocus() {
	// save old focus component
	JXComponentPeer peer;
	Component fc = toolkit.getFocusHandler().getFocusedComponent();
	if (fc != null)
	    peer = ((JXComponentPeer) fc.getPeer());
	else
	    peer = null;
	// set new focus
	setFocus();
	// redraw new and (if necessary) old focus component
	// Note that Window derived classes can also request
	// focus, but are not traversable to focus.
	if (peer != null && peer.isFocusTraversable())
	    peer.redraw();
	if (isFocusTraversable())
	    redraw();
    }                                                                            
    
    public boolean isFocusTraversable() {
	// must be set true in anything that can get
	// focus, e.g. Button or Checkbox
	return false;
    }

    public void enable() {
	setEnabled(true);
    }
    
    public void disable() {
	setEnabled(false);
    }
    
    public void setEnabled(boolean enabled) {
	if (enabled != isEnabled) {
	    isEnabled = enabled;
	    peerState = (isEnabled) ? PEER_NORMAL : PEER_DISABLED;
	    if (!isEnabled && hasFocus())
		toolkit.getFocusHandler().setFocusedComponent(null);
	    redraw();
	}
    }

    public void repaint(/*long tm,*/ int x, int y, int width, int height) {
	// Simply redraws the Peer. This method should only be called
	// from Component.repaint()...
	redraw();
    }
    
    public void show() {
	setVisible(true);
    }
    
    public void hide() {
	setVisible(false);
    }

    public void dispose() {
	ready = false;
    }

    public void setBounds(int x, int y, int width, int height) {
	if (this.width != width || this.height != height)
	    sendComponentEvent(ComponentEvent.COMPONENT_RESIZED);
	else
	    sendComponentEvent(ComponentEvent.COMPONENT_MOVED);
	this.x = x;
	this.y = y;
	this.width = width;
	this.height = height;
    }

    public void reshape(int x, int y, int width, int height) {
	setBounds(x, y, width, height);
    }

    public Toolkit getToolkit() {
	return toolkit;
    }

    public Graphics getGraphics() {
	return getJXGraphics();
    }

    public Point getLocationOnScreen() {
	Window w = getHostWindow();
	if (w instanceof Frame) {
	    Point p1 = w.getLocationOnScreen();
	    Point p2 = getLocationRelativeToWindow(0, 0);
	    return new Point(p1.x + p2.x, p1.y + p2.y);
	}
	System.out.println("could not find host window!");
 	return new Point(x, y);
    }

    public Dimension getPreferredSize() {
	return new Dimension(prefWidth, prefHeight);
    }
    
    public Dimension getMinimumSize() {
	return getPreferredSize();
    }

    public Dimension minimumSize() {
	return getMinimumSize();
    }

    public Dimension preferredSize() {
	return getPreferredSize();
    }    

    /****************************** DRAWING *******************************/


    /**
     * Paints an invisible peer.
     */
    public void paintInvisibleComponent(JXGraphics g) {
	g.setColor(JXColors.normalBgColor);
	g.fillRect(0, 0, width - 1, height - 1);
    }
}
