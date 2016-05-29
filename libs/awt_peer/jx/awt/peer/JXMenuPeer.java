package jx.awt.peer;


import java.awt.*;
import java.awt.peer.*;
import java.awt.event.*;
import java.util.*;
import jx.awt.*;


/**
 * This class implements a Menu peer. It maintains a complete menu
 * window with all its embedded menu items. If the menu window is
 * higher than the screen's height, then its height is adjusted,
 * and it gets two scroll buttons to scroll through the window's
 * contents.
 */
public class JXMenuPeer
    extends JXMenuItemPeer
    implements MenuPeer {

    /** The item representing a menu separator */
    private final MenuItem separator = new MenuItem("-");
    /** The border around the item entries */
    private final int BORDER = 3;
    /** The width of the arrow indicating a submenu */
    private final int ARROWWIDTH = 10;
    /** The width (and height) of a checkbox */
    private final int CHECKBOXWIDTH = 15;
    /** The height of the window scroll button */
    private final int SCROLLBUTTONHEIGHT = 10;
    /**
     * The amount of pixels the window can be scrolled
     * up and down with one button press.
     */
    private final int SCROLLAMOUNT = 20;
    /**
     * The flag indicating whether you want the menu behavior
     * to be click-triggered or mouse-sensitive
     */
    private final boolean sensitive = false;
    

    private JXMenuConnector connector;
    private Vector menuItems;
    private Vector menuItemSizes;
    private MenuItem currentMenuItem;
    private Menu subMenu;
    
    private int scrollOffset;
    private Rectangle upperScroll;
    private Rectangle lowerScroll;
    private int x;
    private int y;
    private int width;
    private int height;
    private int fullHeight;
    private int visibleHeight;
    
    
    
    /** Creates a new JXMenuPeer instance */
    public JXMenuPeer(Menu parent, JXToolkit toolkit) {
	super(parent, toolkit);
	connector = new JXMenuConnector(parent, toolkit);
	// read in all items and their sizes
	menuItems = new Vector();
	menuItemSizes = new Vector();
	for (int i = 0; i < parent.getItemCount(); i++)
	    addItem(parent.getItem(i));
	calculateSize();
	ready = true;
    }
    
    


    /************************************************
     * methods for event handling                      *
     ************************************************/

    /** Sets the highlighted menu item. */
    public void setCurrentMenuItem(MenuItem m) {
	currentMenuItem = m;
    }

    /** Gets the highlighted menu item. */
    public MenuItem getCurrentMenuItem() {
	return currentMenuItem;
    }

    /** Tells whether this menu item is selectable or not. */
    public boolean isSelectable(MenuItem m) {
	return (m != null && m.isEnabled());
    }

    /**
     * Finds the next selectable menu item. You can choose whether
     * to look up- or downwards, and where to start from. If you
     * start from position "-1", you begin either on top or on
     * bottom of the list.
     */
    private MenuItem nextSelectableMenuItem(int start, boolean downwards) {
	if (start < -1 || start >= menuItems.size())
	    return null;
	if (start == -1 && downwards == false)
	    start = 0;
	MenuItem m;
	int i = start;
	if (downwards) {
	    do {
		i = (i + 1) % menuItems.size();
		m = (MenuItem) menuItems.elementAt(i);
	    } while (i != start && !isSelectable(m));
	    return ((m.isEnabled()) ? m : null);
	} else {
	    do {
		i = (i == 0) ? (menuItems.size() - 1) : (i - 1);
		m = (MenuItem) menuItems.elementAt(i);
	    } while (i != start && !isSelectable(m));
	    return ((m.isEnabled()) ? m : null);
	}
    }

    /**
     * Finds the next selectable menu item below the current
     * highlighted item.
     */
    public MenuItem getLowerMenuItem() {
	if (currentMenuItem == null)
	    return getDefaultMenuItem(true);
	int index = menuItems.indexOf(currentMenuItem);
	return nextSelectableMenuItem(index, true);
    }
    
    /**
     * Finds the next selectable menu item above the current
     * highlighted item.
     */
    public MenuItem getUpperMenuItem() {
	if (currentMenuItem == null)
	    return getDefaultMenuItem(false);
	int index = menuItems.indexOf(currentMenuItem);
	return nextSelectableMenuItem(index, false);
    }

    /**
     * Gets the default entry in the item list. Depending on the
     * search direction, this is either the first or the last
     * selectable item in the list.
     */
    public MenuItem getDefaultMenuItem(boolean downwards) {
	return nextSelectableMenuItem(-1, downwards);
    }

    /** Gets the current submenu. */
    public Menu getChildMenu() {
	return subMenu;
    }

    /** Adjusts the scroll offset if in invalid range. */
    public void adjustScrollOffset() {
	if (currentMenuItem == null)
	    return;
	int index = menuItems.indexOf(currentMenuItem);
	Rectangle r = (Rectangle) menuItemSizes.elementAt(index);
	if (r.y < scrollOffset) {
	    scrollOffset = r.y;
	    redrawMenu();
	}
	if (r.y + r.height > scrollOffset + visibleHeight) {
	    scrollOffset = r.y + r.height - visibleHeight;
	    redrawMenu();
	}
    }

    /**
     * This method is called when the user selects a final item, i.e.
     * a MenuItem or a CheckboxMenuItem. It delivers the item to the
     * MenuHandler and tells it to perform the associated operations.
     */
    public void performMenuItemAction(MenuItem mi) {
	// this is the most important part of the menu subsystem!!!
	// by calling the following method, the JX menu thread is
	// activated, cleans up everything and fires the needed
	// events
	toolkit.getMenuThread().setSelectedItem(mi);
    }
    
    /**
     * This method handles all key events that occur when a menu is
     * open. Depending on the key, it either handles it on its own
     * or forwards the key to the next open submenu.
     */
    public boolean performKey(int code) {
	MenuItem mi;
	if (subMenu != null) {
	    if ((code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_LEFT) &&
		((JXMenuPeer) subMenu.getPeer()).getChildMenu() == null) {
		// if child is deepest open menu, then close it
		deleteSubMenus();
		return true;
	    } else {
		// forward key to submenu
		return ((JXMenuPeer) subMenu.getPeer()).performKey(code);
	    }
	} else {
	    // this part of the code is reached only when
	    // this menu is the deepest in the menu hierarchy!
	    switch (code) {
	    case KeyEvent.VK_ENTER:
		mi = currentMenuItem;
		if (isSelectable(mi)) {
		    if (mi instanceof Menu) {
			// open new submenu and initiate it
			openSubMenu((Menu) mi);
			JXMenuPeer menuPeer = (JXMenuPeer) mi.getPeer();
			menuPeer.setCurrentMenuItem(menuPeer.getDefaultMenuItem(true));
		    } else {
			// execute associated handler
			performMenuItemAction(mi);
		    }
		}
		break;
	    case KeyEvent.VK_DOWN:
		mi = getLowerMenuItem();
		setCurrentMenuItem(mi);
		adjustScrollOffset();
		redrawMenu();
		break;
	    case KeyEvent.VK_UP:
		mi = getUpperMenuItem();
		setCurrentMenuItem(mi);
		adjustScrollOffset();
		redrawMenu();
		break;
	    case KeyEvent.VK_RIGHT:
		mi = currentMenuItem;
		if (isSelectable(mi) && mi instanceof Menu)
		    // open new submenu
		    openSubMenu((Menu) mi);
		else {
		    // The selected item is no submenu, so we can allow
		    // the menu bar (if existing) to open the right
		    // neighbour of this menu tree.
		    return false;
		}
		break;
	    case KeyEvent.VK_LEFT:
		// This case should happen only when this is the main menu
		// and receives a LEFT event and has no child menus open.
		// In this case we always allow the menu bar (if existing)
		// to open the left neighbour of this menu tree.
		return false;
	    default:
		return false;
	    }
	}
	return true;
    }

    /**
     * This method handles any mouse presses on the menu window. If the press occured
     * in a scroll button area, then the window is updated and repainted. Otherwise
     * the selected item is either opened (if it's a submenu) or executed.
     */
    public void handleMenuMouseUp(int mx, int my, int button) {
	if (inScrollRange(mx, my)) {
	    // handle mouse press in scroll button area
	    if (upperScroll.contains(mx, my) && scrollOffset > 0) {
		scrollOffset -= SCROLLAMOUNT;
		if (scrollOffset < 0)
		    scrollOffset = 0;
		deleteSubMenus();
		redrawMenu();
	    }
	    if (lowerScroll.contains(mx, my) && scrollOffset < (fullHeight - visibleHeight)) {
		scrollOffset += SCROLLAMOUNT;
		if (scrollOffset > fullHeight - visibleHeight)
		    scrollOffset = fullHeight - visibleHeight;
		deleteSubMenus();
		redrawMenu();
	    }
	} else {
	    MenuItem m = getMenuEntry(mx, my);
	    if (isSelectable(m)) {
		if (!(m instanceof Menu))
		    performMenuItemAction(m);
		else
		    openSubMenu((Menu) m);
	    }
	}
    }

    /**
     * Handles all mouse movements in the menu window. It is possible to switch
     * between click-triggered and mouse-sensitive behavior. While the first
     * type means you have to click on every submenu entry to open it, with
     * the second one all submenus open as soon as you pass their entries with
     * the mouse.
     */
    public void handleMenuMouseMoved(int mx, int my, boolean isInside) {
	MenuItem m;
	MenuItem oldItem = getCurrentMenuItem();
	// if mouse is not inside the menu, then clear current selection
	// unless we just leaved an submenu entry!
	if (!isInside) {
	    if (sensitive)
		m = (oldItem instanceof Menu) ? oldItem : null;
	    else
		m = null;
	} else
	    m = getMenuEntry(mx, my);
	
	if (oldItem != m) {
	    // update only if menu entry has changed
	    if (sensitive) {
		// use mouse-sensitive behavior
		if (isSelectable(oldItem) && oldItem instanceof Menu)
		    deleteSubMenus();
		if (subMenu == null) {
		    setCurrentMenuItem(m);
		    redrawMenu();
		}
		if (isSelectable(m) && m instanceof Menu)
		    openSubMenu((Menu) m);
	    } else {
		// use click-triggered behavior
		setCurrentMenuItem(m);
		redrawMenu();
	    }
	}
    }
    
    /***************************************************
     * methods for drawing                              *
     ***************************************************/
    
    /** Sets the bounds of the window. */
    public void setBounds(int x, int y, int width, int height) {
	this.x = x;
	this.y = y;
	this.width = width;
	this.height = height;
	connector.setBounds(x, y, width, height);
    }
    
    /** Sets the position of the window. */
    public void setLocation(int x, int y) {
	setBounds(x, y, this.width, this.height);
    }

    /** Gets the position of the window. */
    public Point getLocation() {
	return connector.getWindowOrigin();
    }

    /** Gets the dimensions of the window. */
    public Dimension getMenuSize() {
	return new Dimension(width, height);
    }

    /** Shows or hides the window. */
    public void setVisible(boolean visible) {
	connector.show(visible);
    }
    
    /** Gets the associated AWT component to this peer. */
    public Menu getParent() {
	return (Menu) parent;
    }

    /** Paints this peer. */
    private void paint(JXGraphics g) {
	paintMenu(g, currentMenuItem);
    }

    /** Starts repainting of this peer. */
    public void redrawMenu() {
	JXGraphics g = connector.getGraphics();
	paint(g);
    }

    /** Tells whether the mouse is in the scroll button area or not. */
    public boolean inScrollRange(int mx, int my) {
	return (upperScroll != null &&
		(upperScroll.contains(mx, my) ||
		 lowerScroll.contains(mx, my)));
    }

    /** Gets the menu entry currently lying under (mx, my). */
    public MenuItem getMenuEntry(int mx, int my) {
	if (upperScroll != null) {
	    if (inScrollRange(mx, my))
		return null;
	    my -= upperScroll.height - scrollOffset;
	}
	for (int i = 0; i < menuItems.size(); i++)
	    if (((Rectangle) menuItemSizes.elementAt(i)).contains(mx, my) &&
		!isSeparator((MenuItem) menuItems.elementAt(i)))
		return ((MenuItem) menuItems.elementAt(i));
	return null;
    }

    /**
     * Finds a fitting position for a new submenu. At default, the new
     * submenu is placed on the right of the menu, at height of the
     * corresponding menu entry. If there's not enough room, the new
     * submenu's position will be adjusted to fit.
     */
    public Point findSubMenuLocationPoint() {
	int index = menuItems.indexOf(subMenu);
	Point p = getLocation();
	Dimension s = toolkit.getScreenSize();
	Dimension d = ((JXMenuPeer) subMenu.getPeer()).getMenuSize();
	int x = p.x + ((Rectangle) menuItemSizes.elementAt(index)).width - 1;
	int y = p.y + ((Rectangle) menuItemSizes.elementAt(index)).y;
	// adjust y coord if scroll buttons are visible
	if (upperScroll != null)
	    y += upperScroll.height - scrollOffset;
	// check lower border
	if (y + d.height > s.height)
	    y = s.height - d.height;
	// check right border
	if (x + d.width > s.width)
	    x = p.x - d.width;
	return new Point(x, y);
    }

    /**
     * Deletes all submenus and removes their peers. This method works
     * recursive and destroys all open menus down to the leaf.
     */
    public void deleteSubMenus() {
	if (subMenu != null) {
	    ((JXMenuPeer) subMenu.getPeer()).deleteSubMenus();
	    subMenu.removeNotify();
	}
	subMenu = null;
    }
    
    /** Shows the menu window at screen position (x, y). */
    protected void show(int x, int y) {
	((JXMenuPeer) subMenu.getPeer()).setLocation(x, y);
	((JXMenuPeer) subMenu.getPeer()).setVisible(true);
    }
    
    /**
     * Opens a new submenu. If necessary, existing submenus are destroyed.
     * If m is null, no further tasks are performed.
     */
    public void openSubMenu(Menu m) {
	if (subMenu == m)
	    return;
	// delete all existing child menus
	deleteSubMenus();
	subMenu = m;
	if (m == null)
	    return;
	subMenu.addNotify();
	Point p = findSubMenuLocationPoint();
	show(p.x, p.y);
    }

    /** Tests whether the item is a separator or not. */
    private boolean isSeparator(MenuItem m) {
	return (m.getLabel().equals(separator.getLabel()));
    }

    /** Gets the with in pixels of the string. */
    private int textWidth(String s) {
	return CHARWIDTH * s.length() + 2 * BORDER;
    }
    
    /** Gets the height in pixels of a text line. */
    private int textHeight() {
	return CHARHEIGHT + 2 * BORDER;
    }
    
    /** Gets the height in pixels of a separator. */
    private int separatorHeight() {
	return 2 + 2 * BORDER;
    }
    
    /** Calculates the complete layout of the menu window. */
    private void calculateSize() {
	int maxHeight = toolkit.getScreenSize().height;
	width = 0;
	height = 0;
	// find out maximal width and height
	for (int i = 0; i < menuItems.size(); i++) {
	    int size = textWidth(((MenuItem) menuItems.elementAt(i)).getLabel());
	    if (width < size)
		width = size;
	    height += ((Rectangle) menuItemSizes.elementAt(i)).height;
	}
	// sets complete dimensions
	width += 4 * BORDER + ARROWWIDTH + CHECKBOXWIDTH;
	height += 2 * BORDER;
	fullHeight = height;
	visibleHeight = height;
	
	if (height > maxHeight) {
	    // window is higher than the screen
	    // so install scroll buttons
	    height = maxHeight;
	    upperScroll = new Rectangle(0, 0,
					width, SCROLLBUTTONHEIGHT);
	    lowerScroll = new Rectangle(0, height - SCROLLBUTTONHEIGHT,
					width, SCROLLBUTTONHEIGHT);
	    scrollOffset = 0;
	    visibleHeight = height - upperScroll.height - lowerScroll.height;
	}
	// adjust menuitem width
	// here the width is set the first time!
	for (int i = 0; i < menuItems.size(); i++)
	    ((Rectangle) menuItemSizes.elementAt(i)).width = width;
    }

    public void dispose() {
	connector.dispose();
	super.dispose();
    }
    
    /***************************************************
     * methods implemented from MenuPeer                *
     ***************************************************/
    
    public void addItem(MenuItem item) {
	int oldY = BORDER;
	if (isSeparator(item) || !menuItems.contains(item)) {
	    // add only if separator or not already added
	    menuItems.addElement(item);
	    if (!menuItemSizes.isEmpty())
		// Calculate y coord for this entry
		try {
		    oldY = ((Rectangle) menuItemSizes.lastElement()).y +
			((Rectangle) menuItemSizes.lastElement()).height;
		} catch (Exception e) {}
	    // add dimensions of new entry
	    menuItemSizes.addElement(new Rectangle(0, oldY, 0,
						   (isSeparator(item)) ?
						    separatorHeight() :
						    textHeight()));
	    // disable separator entries by default
	    if (isSeparator(item))
		item.setEnabled(false);
	}
    }

    public void addSeparator() {
	// this method is never called in the Classpath AWT!
	System.out.println("*** addSeparator() called!");
    }

    public void delItem(int index) {
	menuItems.remove(index);
	menuItemSizes.remove(index);
    }

    /***************************** DRAWING ********************************/

    /**
     * Paints the complete menu window.
     */
    public void paintMenu(JXGraphics g, MenuItem aktItem) {
	g.setClip(0, 0, width, height);
	g.setColor(JXColors.menuBgColor);
	if (lowerScroll != null) {
	    // paint layout with scroll buttons
	    g.fill3DRect(upperScroll.x, upperScroll.y,
			 upperScroll.width, upperScroll.height,
			 true);
	    g.fill3DRect(lowerScroll.x, lowerScroll.y,
			 lowerScroll.width, lowerScroll.height,
			 true);
	    g.fill3DRect(0, upperScroll.height, width, visibleHeight, true);
	    g.setColor(JXColors.arrowColor);
	    g.drawLine(width / 2, upperScroll.y + 2,
		       width / 2 - 5, upperScroll.y + 7);
	    g.drawLine(width / 2, upperScroll.y + 2,
		       width / 2 + 5, upperScroll.y + 7);
	    g.drawLine(width / 2 + 5, upperScroll.y + 7,
		       width / 2 - 5, upperScroll.y + 7);
	    
	    g.drawLine(width / 2 - 5, lowerScroll.y + 2,
		       width / 2, lowerScroll.y + 7);
	    g.drawLine(width / 2 + 5, lowerScroll.y + 2,
		       width / 2, lowerScroll.y + 7);
	    g.drawLine(width / 2 + 5, lowerScroll.y + 2,
		       width / 2 - 5, lowerScroll.y + 2);
	    g.setClip(0, upperScroll.height, width, visibleHeight);
	    g.translate(0, upperScroll.height - scrollOffset);
	} else {
	    // paint normal layout
	    g.fill3DRect(0, 0, width, height, true);
	}
	
	Color c = JXColors.normalBgColor;
	Color c1 = c.darker();
	Color c2 = c.brighter();
	for (int i = 0; i < menuItems.size(); i++) {
	    Rectangle r = (Rectangle) menuItemSizes.elementAt(i);
	    // paint only if in visible range!
	    if (r.y + r.height > scrollOffset ||
		r.y < scrollOffset + visibleHeight) {
		MenuItem m = (MenuItem) menuItems.elementAt(i);
		if (isSeparator(m)) {
		    // draw separator
		    g.setColor(c1);
		    g.drawLine(0, r.y + BORDER, width, r.y + BORDER);
		    g.setColor(c2);
		    g.drawLine(0, r.y + BORDER + 1, width, r.y + BORDER + 1);
		} else {
		    // draw menu entry
		    if (m == aktItem && m.isEnabled()) {
			g.setColor(JXColors.menuSelBgColor);
			g.fillRect(0, r.y, width, r.height);
			g.setColor(c1);
			g.drawLine(0, r.y, width, r.y);
			g.setColor(c2);
			g.drawLine(0, r.y + r.height, width, r.y + r.height);
			g.setColor(JXColors.menuSelTextColor);
		    } else {
			if (m.isEnabled())
			    g.setColor(JXColors.menuTextColor);
			else
			    g.setColor(JXColors.disabledTextColor);
		    }
		    g.drawJXString(m.getLabel(), 2 * BORDER + CHECKBOXWIDTH, r.y + BORDER);
		}
		// draw checkbox for CheckboxMenuItem
		if (m instanceof CheckboxMenuItem) {
		    g.setColor(JXColors.normalBgColor);
		    g.draw3DRect(BORDER, r.y + BORDER, CHECKBOXWIDTH, CHECKBOXWIDTH, false);
		    g.fill3DRect(BORDER + 1, r.y + BORDER + 1, CHECKBOXWIDTH - 1,
				 CHECKBOXWIDTH - 1, true);
		    if (((CheckboxMenuItem) m).getState()) {
			if (m.isEnabled())
			    g.setColor(JXColors.checkboxFgColor);
			else
			    g.setColor(JXColors.disabledTextColor);
			g.drawLine(BORDER, r.y + BORDER + CHECKBOXWIDTH / 2,
				   BORDER + CHECKBOXWIDTH / 2, r.y + BORDER + CHECKBOXWIDTH);
			g.drawLine(BORDER + CHECKBOXWIDTH, r.y + BORDER,
				   BORDER + CHECKBOXWIDTH / 2, r.y + BORDER + CHECKBOXWIDTH);
		    }
		}
		// draw arrow for Menu
		if (m instanceof Menu) {
		    if (m.isEnabled())
			g.setColor(JXColors.arrowColor);
		    else
			g.setColor(JXColors.disabledArrowColor);
		    g.drawLine(width - BORDER - ARROWWIDTH, r.y + BORDER,
			       width - BORDER - ARROWWIDTH, r.y + BORDER + ARROWWIDTH);
		    g.drawLine(width - BORDER - ARROWWIDTH, r.y + BORDER,
			       width - BORDER, r.y + BORDER + ARROWWIDTH / 2);
		    g.drawLine(width - BORDER - ARROWWIDTH, r.y + BORDER + ARROWWIDTH,
			       width - BORDER, r.y + BORDER + ARROWWIDTH / 2);
		}
	    }
	}
    }
}
