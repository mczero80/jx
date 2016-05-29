package jx.awt.peer;


import java.awt.*;
import java.awt.peer.*;
import java.awt.event.*;
import jx.awt.*;
import java.util.*;


/**
 * This class implements a MenuBar peer.
 */
public class JXMenuBarPeer
    extends JXMenuComponentPeer
    implements MenuBarPeer {
    
    /** The border around each menu bar entry */
    private final int BORDER = 2;

    private Menu currentMenu;
    private Vector menus;
    private Vector menuSizes;
    private Menu helpMenu;
    private int barHeight;
    private int barWidth;
    private Frame parentFrame;
    
    
    
    /** Creates a new JXMenuBarPeer instance */
    public JXMenuBarPeer(MenuBar parent, JXToolkit toolkit) {
	super(parent, toolkit);
	menus = new Vector();
	menuSizes = new Vector();
	// import all menus added until now, including the help menu
	for (int i = 0; i < parent.getMenuCount(); i++)
	    addMenu(parent.getMenu(i));
	addHelpMenu(parent.getHelpMenu());
	ready = true;
    }
    
    
    


    /****************************************************
     * methods for menu bar handling                     *
     ****************************************************/
    
    /** Sets the frame to which this menu bar belongs. */
    public void setParentFrame(Frame p) {
	parentFrame = p;
    }
      
    /** Sets the current selected menu bar entry. */
    public void setCurrentMenu(Menu m) {
	currentMenu = m;
    }

    /** Gets the current selected menu bar entry. */
    public Menu getCurrentMenu() {
	return currentMenu;
    }

    /** Tells whether the menu is selectable or not. */
    public boolean isSelectable(Menu m) {
	return (m != null && m.isEnabled());
    }

    /** Starts painting of this peer. */
    public void redrawMenuBar() {
	if (ready) {
	    JXGraphics g = (JXGraphics) ((JXFramePeer) parentFrame.getPeer()).getMenuGraphics();
	    //g.enableBackBuffer();
	    paintMenuBar(g, currentMenu);
	    //g.drawBackBuffer();
	}
    }
    
    /**
     * Activates the menu bar by highlighting the default
     * menu bar entry.
     */
    public void activateMenuBar() {
	currentMenu = getDefaultMenu();
	redrawMenuBar();
    }

    /**
     * Resets the menu bar by deselecting any highlighted
     * menu bar entry.
     */
    public void resetMenuBar() {
	currentMenu = null;
	redrawMenuBar();
    }

    /**
     * Finds the next selectable menu. You can choose whether
     * to look up- or downwards, and where to start from. If you
     * start from position "-1", you begin either on the left or
     * on the right of the list.
     */
    private Menu nextSelectableMenu(int start, boolean downwards) {
	if (start < -1 || start >= menus.size())
	    return null;
	if (start == -1)
	    downwards = true;
	Menu m;
	int i = start;
	if (downwards) {
	    do {
		i = (i + 1) % menus.size();
		m = (Menu) menus.elementAt(i);
	    } while (i != start && !m.isEnabled());
	    return ((m.isEnabled()) ? m : null);
	} else {
	    do {
		i = (i == 0) ? (menus.size() - 1) : (i - 1);
		m = (Menu) menus.elementAt(i);
	    } while (i != start && !m.isEnabled());
	    return ((m.isEnabled()) ? m : null);
	}
    }

    /**
     * Finds the next selectable menu left to the current
     * highlighted menu.
     */
    public Menu getLeftMenu() {
	if (currentMenu == null)
	    return getDefaultMenu();
	int index = menus.indexOf(currentMenu);
	return nextSelectableMenu(index, false);
    }
    
    /**
     * Finds the next selectable menu right to the current
     * highlighted menu.
     */
    public Menu getRightMenu() {
	if (currentMenu == null)
	    return getDefaultMenu();
	int index = menus.indexOf(currentMenu);
	return nextSelectableMenu(index, true);
    }

    /**
     * Finds the default menu bar entry which is always the leftmost
     * selectable entry.
     */
    public Menu getDefaultMenu() {
	return nextSelectableMenu(-1, true);
    }
    
    public void dispose() {
	currentMenu = null;
	menus = null;
	menuSizes = null;
	helpMenu = null;
	parentFrame = null;
    }

    /************************************************
     * methods for drawing                           *
     ************************************************/
    
    /** Paints this peer. */
    public void paintMenuBar(JXGraphics g, Menu aktMenu) {
	// draw background
	paintMenuBackground(g);
	// draw every entry
	for (int i = 0; i < menus.size(); i++) {
	    Menu m = (Menu) menus.elementAt(i);
	    Rectangle r = (Rectangle) menuSizes.elementAt(i);
	    g.setClip(r);
	    paintMenuEntry(g, m, r, (m == aktMenu));
	}
    }
    
    /** Gets the menu bar entry currently lying under (mx, my). */
    public Menu getMenuEntry(int mx, int my) {
	for (int i = 0; i < menuSizes.size(); i++) {
	    Rectangle r = (Rectangle) menuSizes.elementAt(i);
	    if (r.contains(mx, my) && isSelectable((Menu) menus.elementAt(i)))
		return ((Menu) menus.elementAt(i));
	}
	return null;
    }
    
    /** Gets the height in pixels of a single menu bar entry. */
    public int getHeight() {
	return barHeight;
    }
    
    /** Does a complete layout of the menu bar. */
    public void layout(int width, int height) {
	int x, y;
	boolean once;
	barWidth = width;
	// if help menu exits, the width is adjusted
	if (helpMenu != null) {
	    Rectangle r = getMenuBounds(helpMenu);
	    r.x = width - r.width;
	    r.y = 0;
	    width -= r.width;
	}
	x = 0;
	y = 0;
	once = false;
	// layout all menubar entries
	// at least one entry should be in every line
	// so we need the "once" flag to avoid no entry
	// being set in a line (--> infinite loop!)
	for (int i = 0; i < menuSizes.size(); i++)
	    // the help menu entry is not laid out!
	    if (menus.elementAt(i) != helpMenu) {
		Rectangle r = (Rectangle) menuSizes.elementAt(i);
		if (once && (x + r.width > width)) {
		    y += menuHeight();
		    x = 0;
		    once = false;
		}
		r.x = x;
		r.y = y;
		x += r.width;
		once = true;
	    }
	// barHeight stores the complete height of the menu bar
	barHeight = y + menuHeight() + 1;
    }

    /** Gets the bounds of a menu bar entry. */
    public Rectangle getMenuBounds(Menu m) {
	int index = menus.indexOf(m);
	if (index == -1)
	    return null;
	else
	    return ((Rectangle) menuSizes.elementAt(index));
    }
    
    /** Gets the width in pixels of the string. */
    private int menuWidth(String s) {
	return CHARWIDTH * s.length() + 2 * BORDER;
    }
    
    /** Gets the height in pixels of a text line. */
    private int menuHeight() {
	return CHARHEIGHT + 2 * BORDER;
    }
    
    /************************************************
     * methods implemented from MenuBarPeer          *
     ************************************************/
    
    public void addMenu(Menu m) {
	// add only if not yet added
	if (!menus.contains(m)) {
	    menus.addElement(m);
	    menuSizes.addElement(new Rectangle(0, 0,
					       menuWidth(m.getLabel()),
					       menuHeight()));
	}
    }
    
    public void addHelpMenu(Menu m) {
	if (m != null) {
	    helpMenu = m;
	    addMenu(m);
	}
    }
    
    public void delMenu(int index) {
	if (menus.elementAt(index) == helpMenu)
	    helpMenu = null;
	menus.remove(index);
	menuSizes.remove(index);
    }
    
    /************************ DRAWING ***************************/

    /** Paints the menu bar's background. */
    public void paintMenuBackground(JXGraphics g) {
	g.setColor(JXColors.menuBgColor);
	g.setClip(0, 0, barWidth, barHeight);
	g.fillRect(0, 0, barWidth - 1, barHeight - 1);
	g.setColor(JXColors.menuBgColor.darker());
	g.drawLine(0, barHeight - 1, barWidth, barHeight - 1);
    }

    /** Paints a menu bar entry. */
    public void paintMenuEntry(JXGraphics g, Menu m, Rectangle r, boolean hover) {
	if (hover) {
	    g.setColor(JXColors.menuSelBgColor);
	    g.fillRect(r.x, r.y, r.width - 1, r.height - 1);
	    g.setColor(JXColors.menuSelTextColor);
	} else
	    if (m.isEnabled())
		g.setColor(JXColors.menuTextColor);
	    else
		g.setColor(JXColors.disabledTextColor);
	g.drawJXString(m.getLabel(), r.x + BORDER, r.y + BORDER);
    }
}
