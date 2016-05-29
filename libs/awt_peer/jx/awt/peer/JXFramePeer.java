package jx.awt.peer;

import java.awt.*;
import java.awt.event.*;
import java.awt.peer.FramePeer;
import jx.awt.*;


/**
 * Implements a Frame peer.
 */
public class JXFramePeer 
    extends JXWindowPeer
    implements java.awt.peer.FramePeer {

    /** The frame's menu bar. */
    private MenuBar menubar;
        
    
    /** Creates a new JXFramePeer instance */
    public JXFramePeer(Frame parent, JXToolkit toolkit) {
	super(toolkit, parent);
	ready = true;
    }



    /****************************************************
     * menu interface methods                            *
     ****************************************************/

    /** Does some layout on the menu bar. */
    public void layoutMenu(int width, int height) {
	if (menubar != null)
	    ((JXMenuBarPeer) menubar.getPeer()).layout(width, height);
    }
    
    /** Gets the menu bar entry that is currently selected. */
    public Menu getCurrentMenu() {
	if (menubar != null)
	    return ((JXMenuBarPeer) menubar.getPeer()).getCurrentMenu();
	return null;
    }

    /** Activates the menu bar. */
    public void activateMenuBar() {
	if (menubar != null)
	    ((JXMenuBarPeer) menubar.getPeer()).activateMenuBar();
    }
    
    /** Gets the height in pixels needed by the menu bar to draw itself. */
    public int getMenuHeight() {
	return ((menubar != null) ? ((JXMenuBarPeer) menubar.getPeer()).getHeight() : 0);
    }

    /** Resets the menu bar. */
    public void resetMenuBar() {
	if (menubar != null)
	    ((JXMenuBarPeer) menubar.getPeer()).resetMenuBar();
    }
    
    /****************************************************
     * methods for key and mouse handling                *
     ****************************************************/

    /**
     * Opens a new menu (no submenu). To open a submenu, use the
     * methods provided in JXMenuPeer.
     */
    private void openMenu(Menu newMenu) {
	JXMenuBarPeer peer = (JXMenuBarPeer) menubar.getPeer();
	MenuHandler mh = toolkit.getMenuHandler();
	// update menu bar
	peer.setCurrentMenu(newMenu);
	peer.redrawMenuBar();
	// calculate absolute screen position of the
	// menu's bounding rectangle
	Point p = connector.getWindowOrigin();
	Rectangle r = peer.getMenuBounds(newMenu);
	Rectangle r2 = new Rectangle(r.x + p.x, r.y + p.y,
				     r.width, r.height);
	// let MenuHandler handle the new menu
	mh.setCurrentMenuBar(menubar);
	mh.openMenu(newMenu, r2);
    }
    
    /**
     * Called when the current selected menu bar entry has changed.
     */
    private void switchToMenu(Menu newMenu, boolean menuOpen) {
	JXMenuBarPeer peer = (JXMenuBarPeer) menubar.getPeer();
	MenuHandler mh = toolkit.getMenuHandler();
	if (menuOpen) {
	    if (newMenu != null) {
		// close old menus and open new one
		mh.deleteOpenMenus();
		openMenu(newMenu);
	    }
	} else {
	    // update menu bar
	    peer.setCurrentMenu(newMenu);
	    peer.redrawMenuBar();
	}
    }
    
    /**
     * Called by JXWindowConnector when a key has been pressed and a menu is open
     * or the menu bar is active. This method handles the key event and, if
     * necessary, forwards it to the open menu.
     */
    public void handleMenuKeyDown(int code) {
	Menu newMenu = null;
	MenuHandler mh = toolkit.getMenuHandler();
	// get menu bar peer if existing
	JXMenuBarPeer peer = (mh.isPopupMenuOpen()) ? null : ((JXMenuBarPeer) menubar.getPeer());
	switch (code) {
	case KeyEvent.VK_ENTER:
	case KeyEvent.VK_UP:
	case KeyEvent.VK_DOWN:
	    if (mh.isMenuOpen()) {
		// forward to menu
		mh.performKey(code);
	    } else {
		// open current selected menu
		newMenu = peer.getCurrentMenu();
		openMenu(newMenu);
		JXMenuPeer menuPeer = (JXMenuPeer) newMenu.getPeer();
		// select a fitting menu item
		if (code == KeyEvent.VK_UP)
		    menuPeer.setCurrentMenuItem(menuPeer.getDefaultMenuItem(false));
		else
		    menuPeer.setCurrentMenuItem(menuPeer.getDefaultMenuItem(true));
	    }
	    break;
	case KeyEvent.VK_ESCAPE:
	    if (mh.isMenuOpen()) {
		// forward to menu
		mh.performKey(code);
	    } else
		// reset menu bar
		peer.resetMenuBar();
	    break;
	case KeyEvent.VK_LEFT:
	    if (peer != null)
		newMenu = peer.getLeftMenu();
	    if (mh.isMenuOpen()) {
		// first ask menu to handle left key event
		// if this fails, switch to menu left to
		// the old one
		if (!mh.performKey(code) && !mh.isPopupMenuOpen()) {
		    switchToMenu(newMenu, true);
		}
	    } else
		switchToMenu(newMenu, false);
	    break;
	case KeyEvent.VK_RIGHT:
	    if (peer != null)
		newMenu = peer.getRightMenu();
	    if (mh.isMenuOpen()) {
		// first ask menu to handle right key event
		// if this fails, switch to menu right to
		// the old one
		if (!mh.performKey(code) && !mh.isPopupMenuOpen()) {
		    switchToMenu(newMenu, true);
		}
	    } else
		switchToMenu(newMenu, false);
	    break;
	}
    }

    /**
     * Called by JXWindowConnector when a mouse button has been pressed on the
     * menu bar area. This method opens a new menu, if one was selected.
     */
    public void handleMenuMouseDown(int mx, int my, int button) {
	JXMenuBarPeer peer = (JXMenuBarPeer) menubar.getPeer();
	MenuHandler mh = toolkit.getMenuHandler();
	// find out selected menu bar entry
	Menu newMenu = peer.getMenuEntry(mx, my);
	if (newMenu == null)
	    mh.deleteOpenMenus();
	else
	    openMenu(newMenu);
	// update menu bar
	peer.setCurrentMenu(newMenu);
	peer.redrawMenuBar();
    }
    
    /**
     * Called by JXWindowConnector when the mouse moves over the menu bar area.
     * This method updates the menu bar and switches to new menus, if necessary.
     */
    public void handleMenuMouseMoved(int mx, int my) {
	JXMenuBarPeer peer = (JXMenuBarPeer) menubar.getPeer();
	MenuHandler mh = toolkit.getMenuHandler();
	// find out underlying menu bar entry
	Menu newMenu = peer.getMenuEntry(mx, my);
	if (newMenu != peer.getCurrentMenu()) {
	    // if entry changed, switch to new menu, if necessary
	    boolean menuOpen = mh.isMenuOpen();
	    switchToMenu(newMenu, menuOpen);
	}
    }
    
    /****************************************************
     * misc. methods                                     *
     ****************************************************/

    /**
     * Called when a new connector is initialized.
     * (see JXWindowPeer)
     */
    protected void initWindow() {
	super.initWindow();
	setResizable(((Frame) parent).isResizable());
	setTitle(((Frame) parent).getTitle());
	setMenuBar(((Frame) parent).getMenuBar());
    }

    /**
     * Gets a valid JXGraphics object usable to draw a menu bar.
     */
    public JXGraphics getMenuGraphics() {
	return connector.getMenuGraphics();
    }

    /** Paints this peer. */
    public void paint(JXGraphics g) {
	paintFrame(g);
    }

    /**
     * Closes the underlying connector and the menu bar. After that,
     * it sends a window closed event.
     */
    public void dispose() {
	if (connector != null)
	    connector.dispose();
	if (menubar != null)
	    ((JXMenuBarPeer) menubar.getPeer()).dispose();
	sendWindowEvent(WindowEvent.WINDOW_CLOSED);
	super.dispose();
    }
    
    /**
     * Extends redrawComponent() from JXContainerPeer to draw the menu bar.
     */
    protected void redrawComponent() {
	super.redrawComponent();
	if (ready && menubar != null) {
	    ((JXMenuBarPeer) menubar.getPeer()).setParentFrame((Frame) parent);
	    ((JXMenuBarPeer) menubar.getPeer()).redrawMenuBar();
	}
    }
    
    /****************************************************
     * methods implemented from FramePeer                *
     ****************************************************/
    
    /*    public void setIconImage(Image image) {} */
    
    public void setMenuBar(MenuBar mb) {
	menubar = mb;
    }
    
    public void setResizable(boolean resizable) {
	connector.setResizable(resizable);
    }
    
    public void setTitle(String title) {
	connector.setTitle(title);
    }

    /******************************* DRAWING ****************************/
    
    /**
     * Paints the frame.
     */
    private void paintFrame(JXGraphics g) {
	g.setColor(JXColors.normalBgColor);
	g.fillRect(0, 0, width - 1, height - 1);
	// Avoiding drawing would give some performance boost,
	// but there are cases when the component peers do not
	// cover the whole window...
    }
}
