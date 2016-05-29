package jx.awt.peer;


import java.awt.*;
import java.awt.event.*;
import jx.awt.*;

/**
 * This class provides handling of the menu windows, both normal
 * and popup menus. More precisely, it handles the first menu window 
 * in the menu window hierarchie, which is simply called "the menu"
 * by the users (All windows below are called "submenus"!).
 */
public class MenuHandler {
    
    /** A reference to the global JXToolkit instance */
    private JXToolkit toolkit;
    /** The menu that is currently selected */
    private Menu currentMenu = null;
    /** The menu bar that is currently active */
    private MenuBar currentMenuBar = null;
    /** Tells whether the menu is shown or not */
    private boolean menuOpen = false;
    /**
     * The notification flag, can be used to distinguish
     * between "normal" and popup menus.
     */
    private boolean notification = true;



    /** Creates a new MenuHandler instance */
    public MenuHandler(JXToolkit toolkit) {
	this.toolkit = toolkit;
    }




    /** Gets the current selected menu. */
    public Menu getCurrentMenu() {
	return currentMenu;
    }

    /** Tells whether the menu is open or not. */
    public boolean isMenuOpen() {
	return menuOpen;
    }

    /** 
     * Tells whether the menu is open and it is a popup
     * menu or not.
     */
    public boolean isPopupMenuOpen() {
	return (isMenuOpen() && !notification);
    }

    /** Sets the current menu bar */
    public void setCurrentMenuBar(MenuBar mb) {
	currentMenuBar = mb;
    }

    /** Gets the current menu bar. */
    public MenuBar getCurrentMenuBar() {
	return currentMenuBar;
    }

    /**
     * Finds a suitable position for the menu window. The Rectangle contains
     * the actual size and position of the associated menu bar entry. 
     */
    private Point findMenuLocationPoint(Rectangle r) {
	JXMenuPeer peer = (JXMenuPeer) currentMenu.getPeer();
	Dimension s = toolkit.getScreenSize();
	Dimension d = peer.getMenuSize();
	// set initial values (lower left corner of menu bar entry coords)
	int x = r.x;
	int y = r.y + r.height;
	// check for left border
	if (x < 0)
	    x = 0;
	// check for right border
	if (x + d.width > s.width)
	    x = s.width - d.width;
	// check for lower border
	if (y + d.height > s.height)
	    y = r.y - d.height;
	// check for full-size menu
	if (d.height > s.height) {
	    // This case should never happen, as the menu's size should
	    // already be adjusted to be not higher than the screen's size!
	    System.out.println("Error on calculating menu size!");
	}
	if (d.height == s.height) {
	    y = 0;
	}
	return new Point(x, y);
    }

    /**
     * Closes all open menus and submenus. This method is called whenever the current
     * visible menu hierarchie should be destroyed.
     */
    public void deleteOpenMenus() {
	if (!isMenuOpen())
	    return;
	JXMenuPeer peer = ((JXMenuPeer) currentMenu.getPeer());
	peer.deleteSubMenus();
	currentMenu.removeNotify();
	if (!notification)
	    currentMenu.addNotify();
	menuOpen = false;
    }
    
    /**
     * Opens a new normal menu. The Rectangle contains the coords of the
     * associated menu bar entry.
     */
    public void openMenu(Menu m, Rectangle bounds) {
	openMenu(m, bounds, true);
    }
    
    /**
     * Opens a new menu. The Rectangle contains the coords of the associated
     * menu bar entry, and the flag indicates whether it is a popup menu or not.
     */
    public void openMenu(Menu menu, Rectangle bounds, boolean notPopup) {
	notification = notPopup;
	if (isMenuOpen())
	    deleteOpenMenus();
	currentMenu = menu;
	if (menu == null)
	    return;
	if (notification)
	    menu.addNotify();
	JXMenuPeer peer = (JXMenuPeer) menu.getPeer();
	Point p = findMenuLocationPoint(bounds);
	peer.setLocation(p.x, p.y);
	peer.setVisible(true);
	menuOpen = true;
    }
    
    /**
     * Handles all key presses. When a menu is open, then all key events are forwarded to
     * this method. Here, it is first checked whether there are submenus open and, if so,
     * the events are forwarded to them. Otherwise, they are handled here.
     */
    public boolean performKey(int code) {
	if (!menuOpen) {
	    System.out.println("error: menu should be open!");
	    return false;
	}
	MenuItem mi;
	JXMenuPeer peer = (JXMenuPeer) currentMenu.getPeer();
	if (peer.getChildMenu() != null) {
	    return peer.performKey(code);
	} else {
	    switch (code) {
	    case KeyEvent.VK_ENTER:
		mi = peer.getCurrentMenuItem();
		if (peer.isSelectable(mi)) {
		    if (mi instanceof Menu) {
			peer.openSubMenu((Menu) mi);
			JXMenuPeer menuPeer = (JXMenuPeer) mi.getPeer();
			menuPeer.setCurrentMenuItem(menuPeer.getDefaultMenuItem(true));
		    } else {
			peer.performMenuItemAction(mi);
		    }
		}
		break;
	    case KeyEvent.VK_ESCAPE:
		deleteOpenMenus();
		break;
	    case KeyEvent.VK_DOWN:
		mi = peer.getLowerMenuItem();
		peer.setCurrentMenuItem(mi);
		peer.adjustScrollOffset();
		peer.redrawMenu();
		break;
	    case KeyEvent.VK_UP:
		mi = peer.getUpperMenuItem();
		peer.setCurrentMenuItem(mi);
		peer.adjustScrollOffset();
		peer.redrawMenu();
		break;
	    case KeyEvent.VK_LEFT:
	    case KeyEvent.VK_RIGHT:
		return peer.performKey(code);
	    default:
		return false;
	    }
	}
	return true;
    }

    public void dispose() {
	toolkit = null;
	currentMenu = null;
	currentMenuBar = null;
    }
}
