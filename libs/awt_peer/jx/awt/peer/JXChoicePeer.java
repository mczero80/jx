package jx.awt.peer;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.ItemEvent;
import jx.awt.*;
import java.util.*;


/**
 * This class implements a Choice peer. It uses a JXChoiceConnector
 * class to implement its choice window.
 */
public class JXChoicePeer
    extends JXComponentPeer
    implements java.awt.peer.ChoicePeer {

    /** The border around the text lines */
    private final int BORDER = 3;
    /** The space between the text lines */
    private final int TEXTBORDER = 1;
    /** The width of the small button */
    private final int BUTTONWIDTH = 15;
    /** The maximal number of entries visible at a time */
    private final int MAXENTRIES = 8;
    
    private int selectedIndex;
    private int tempIndex;
    private Vector items;
    private boolean windowOpen;
    private JXChoiceConnector connector;
    private Rectangle windowSize;
    private int visHeight;
    private int realHeight;
    private int offset;
    private InternalScrollbar bar;
    private int lastArea = -1;
    private boolean redrawAfterSelect = true;



    /** Creates a new JXChoicePeer instance */
    public JXChoicePeer(Choice parent, JXToolkit toolkit) {
	super(toolkit, parent);
	items = new Vector();
	for (int i = 0; i < parent.getItemCount(); i++)
	    add(parent.getItem(i), i);
	select(parent.getSelectedIndex());
	windowOpen = false;
	prefWidth = getPrefWidth();
	prefHeight = getPrefHeight();
	ready = true;
    }



    /** Redraws the peer if necessary. */
    public void keyPressed(int keyValue, int rawValue, int modifier) {
	super.keyPressed(keyValue, rawValue, modifier);
	switch (KeyMap.translate(keyValue)) {
	case KeyEvent.VK_SPACE:
	case KeyEvent.VK_ENTER:
	    peerState = PEER_PRESSED;
	    redraw();
	}
    }

    /** Redraws the peer if necessary. */
    public void keyReleased(int keyValue, int rawValue, int modifier, boolean over) {
	super.keyReleased(keyValue, rawValue, modifier, over);
	switch (KeyMap.translate(keyValue)) {
	case KeyEvent.VK_SPACE:
	case KeyEvent.VK_ENTER:
	    peerState = (over) ? PEER_HOVER : PEER_NORMAL;
	    redraw();
	}
    }
    
    /**
     * Handles all key events that occur on the choice itself or in the choice's
     * window. 
     */
    public void keyClicked(int keyValue, int rawValue, int modifier) {
	super.keyClicked(keyValue, rawValue, modifier);
	switch (KeyMap.translate(keyValue)) {
	case KeyEvent.VK_SPACE:
	case KeyEvent.VK_ENTER:
	    if (!windowOpen)
		openChoiceWindow();
	    else
		setChoice();
	    break;
	case KeyEvent.VK_LEFT:
	case KeyEvent.VK_UP:
	    if (!windowOpen) {
		if (selectedIndex > 0) {
		    selectedIndex--;
		    setParentIndex(true);
		    sendItemEvent();
		}
	    } else {
		if (tempIndex > 0) {
		    tempIndex--;
		    updateOffset();
		    redrawChoiceWindow(false);
		}
	    }
	    break;
	case KeyEvent.VK_RIGHT:
	case KeyEvent.VK_DOWN:
	    if (!windowOpen) {
		if (selectedIndex < items.size() - 1) {
		    selectedIndex++;
		    setParentIndex(true);
		    sendItemEvent();
		}
	    } else {
		if (tempIndex < items.size() - 1) {
		    tempIndex++;
		    updateOffset();
		    redrawChoiceWindow(false);
		}
	    }
	    break;
	case KeyEvent.VK_ESCAPE:
	    abortChoice();
	}
    }
    
    public void mousePressed(int x, int y, int button) {
	super.mousePressed(x, y, button);
	redraw();
    }
    
    /** Opens the choice's window */
    public void mouseReleased(int x, int y, int button, boolean over) {
	super.mouseReleased(x, y, button, over);
	if (!windowOpen)
	    openChoiceWindow();
	else
	    abortChoice();
	redraw();
    }
    
    public void mouseEntered(int x, int y, int button) {
	super.mouseEntered(x, y, button);
	redraw();
    }
    
    public void mouseExited(int x, int y, int button) {
	super.mouseExited(x, y, button);
	redraw();
    }

    /** Paints this peer. */
    public void paint(JXGraphics g) {
	boolean focus = hasFocus();
	switch (peerState) {
	case PEER_PRESSED:paintChoice(g, focus, true, false, false);break;
	case PEER_NORMAL:paintChoice(g, focus, false, false, false);break;
	case PEER_HOVER:paintChoice(g, focus, false, true, false);break;
	case PEER_DISABLED:paintChoice(g, focus, false, false, true);break;
	default:System.out.println("*** JXChoicePeer: Unknown state!!!");
	}
    }

    /** Make Choice traversable to keyboard focus. */
    public boolean isFocusTraversable() {
	return true;
    }

    /** Sets the selected item and fires an item event. */
    public void setChoice() {
	closeChoiceWindow();
	selectedIndex = tempIndex;
	setParentIndex(true);
	sendItemEvent();
    }

    /** Aborts the current selection. */
    public void abortChoice() {
	closeChoiceWindow();
    }

    /** Gets the preferred width of the Choice. */
    private int getPrefWidth() {
	int w = 0;
	for (int i = 0; i < items.size(); i++) {
	    int l = CHARWIDTH * ((String) items.elementAt(i)).length();
	    if (w < l)
		w = l;
	}
	return w + 2 * BORDER + BUTTONWIDTH;
    }

    /** Gets the preferred height of the Choice. */
    private int getPrefHeight() {
	return CHARHEIGHT + 2 * BORDER;
    }

    /** Updates the choice's window's scroll offset. */
    private void updateOffset() {
	if (offset > tempIndex)
	    offset = tempIndex;
	if (offset < tempIndex - MAXENTRIES + 1)
	    offset = tempIndex - MAXENTRIES + 1;
	if (offset < 0)
	    offset = 0;
	if (items.size() > MAXENTRIES) {
	    if (offset > items.size() - MAXENTRIES)
		offset = items.size() - MAXENTRIES;
	} else {
	    if (offset > 0)
		offset = 0;
	}
	if (bar != null)
	    bar.setValue(offset);
    }

    /** Informs the Choice class about current selection. */
    private void setParentIndex(boolean draw) {
	redrawAfterSelect = draw;
	((Choice) parent).select(selectedIndex);
	redrawAfterSelect = true;
    }
    
    /** Sends an item event. */
    private void sendItemEvent() {
	queue = toolkit.getSystemEventQueue();
	queue.postEvent(new ItemEvent((ItemSelectable) parent,
				      ItemEvent.ITEM_STATE_CHANGED,
				      items.elementAt(selectedIndex),
				      ItemEvent.SELECTED));
    }

    /**********************************************************
     * methods for choice window handling                      *
     **********************************************************/
    
    /** Converts number of rows to needed height in pixels. */
    private int rowsToHeight(int rows) {
	return rows * CHARHEIGHT + (rows - 1) * TEXTBORDER + 2 * BORDER;
    }

    /** Gets the entry currently under (x, y). */
    private int getCurrentSelection(int x, int y) {
	if (y < BORDER || y > windowSize.height - BORDER)
	    return -1;
	return ((y - BORDER) / (CHARHEIGHT + TEXTBORDER)) + offset;
    }

    /** Opens the choice's window. */
    public void openChoiceWindow() {
	// close window if already open
	if (windowOpen) {
	    closeChoiceWindow();
	    return;
	}
	windowOpen = true;
	// register window to be closed on irregular exit
	toolkit.getSlaveWindowHandler().registerWindow(this);
	// get new Connector
	connector = new JXChoiceConnector((Choice) parent, toolkit);
	Point p = getLocationOnScreen();
	Dimension s = toolkit.getScreenSize();
	bar = null;
	offset = 0;
	tempIndex = selectedIndex;
	// calculate window size and position
	realHeight = rowsToHeight(items.size());
	if (items.size() > MAXENTRIES) {
	    // There are more list entries than can be shown at
	    // a time, so install a scroll bar
	    visHeight = rowsToHeight(MAXENTRIES);
	    bar = new InternalScrollbar(this, toolkit);
	    bar.setTranslate(false);
	    bar.setOrientation(Scrollbar.VERTICAL);
	    bar.setValues(selectedIndex, MAXENTRIES, 0, items.size());
	    bar.setSmallIncrement(1);
	    bar.setBigIncrement(MAXENTRIES);
	} else
	    visHeight = realHeight;
	int visX = p.x;
	int visY = p.y + height;
	if (visX + width > s.width)
	    visX = s.width - width;
	if (visY + visHeight > s.height)
	    visY = p.y - visHeight;
	// set window size
	windowSize = new Rectangle(visX, visY, width, visHeight);
	connector.setBounds(windowSize.x, windowSize.y,
			    windowSize.width, windowSize.height);
	// show window
	connector.show(true);
    }

    /** Closes the choice's window. */
    public void closeChoiceWindow() {
	if (!windowOpen)
	    return;
	windowOpen = false;
	connector.dispose();
    }
    
    /**
     * Called by JXChoiceConnector when mouse button has been pressed.
     * Redraws the scroll bar if necessary.
     */
    public void handleChoiceMouseDown(int x, int y, int button) {
	if (bar != null) {
	    int where = bar.mouseInScrollbarPressed(x, y, button);
	    if (where != bar.NONE)
		redrawChoiceWindow(true);
	}
    }

    /**
     * Called by JXChoiceConnector when mouse button has been released.
     * Redraws the scroll bar and updates the scroll offset if necessary.
     */
    public void handleChoiceMouseUp(int x, int y, int button) {
	if (bar != null) {
	    int where = bar.mouseInScrollbarReleased(x, y, button, (x > windowSize.width - BUTTONWIDTH));
	    if (where != bar.NONE) {
		offset = bar.getValue();
		redrawChoiceWindow(false);
		return;
	    }
	}
	setChoice();
    }

    /**
     * Called by JXChoiceConnector when mouse is moved.
     * Redraws the scroll bar and the window if necessary.
     */
    public void handleChoiceMouseMoved(int x, int y, int button, boolean inside) {
	boolean draw = false;
	if (bar != null) {
	    if (bar.inScrollArea(x, y)) {
		// handle mouse inside scroll bar area
		int where = bar.mouseInScrollbarMoved(x, y, button);
		if (where != bar.NONE) {
		    if (where != lastArea) {
			lastArea = where;
			redrawChoiceWindow(button != 0);
		    }
		} else
		    lastArea = bar.NONE;
		return;
	    } else {
		// handle mouse outside scroll bar area
		if (lastArea != bar.NONE) {
		    //bar.mouseInScrollbarMoved(x, y, button);
		    bar.mouseInScrollbarExited(x, y, button);
		    lastArea = bar.NONE;
		    draw = true;
		}
	    }
	}
	int newSelection = getCurrentSelection(x, y);
	if ((newSelection != tempIndex && newSelection >= 0) || draw) {
	    // redraw if dragging or mouse moves over new entry
	    tempIndex = newSelection;
	    redrawChoiceWindow(button != 0);
	}
    }

    /** Paints the choice's window. */
    public void redrawChoiceWindow(boolean pressed) {
	if (bar != null)
	  offset = bar.getValue();
	JXGraphics g = connector.getGraphics();
	paintChoiceWindow(g, tempIndex, pressed);
    }

    /**********************************************************
     * methods implemented from ChoicePeer                     *
     **********************************************************/

    public void add(String item, int index) {
	items.insertElementAt(item, index);
    }
    
    public void addItem(String item, int index) {
	add(item, index);
    }
    
    public void remove(int index) {
	items.remove(index);
    }
    
    public void select(int index) {
	selectedIndex = index;
	if (redrawAfterSelect)
	    redraw();
    }

    /******************************* DRAWING *****************************/

    /** Paints the choice. */
    public void paintChoice(JXGraphics g, boolean focus, boolean pressed,
			    boolean hover, boolean disabled) {
	int px = width - BUTTONWIDTH;
	int py = 0;
	// draw text field
	g.setColor(JXColors.normalBgColor);
	g.draw3DRect(0, 0, px - 1, height - 1, false);
	if (!disabled)
		g.setColor(JXColors.textBgColor);
	g.fillRect(1, 1, px - 3, height - 3);
	// draw text
	String s = ((String) items.elementAt(selectedIndex));
	if (disabled)
	    g.setColor(JXColors.disabledTextColor);
	else
	    g.setColor(JXColors.normalTextColor);
	g.drawJXString(s, BORDER, BORDER);
	// draw button
	if (hover)
	    g.setColor(JXColors.hoverColor);
	else
	    g.setColor(JXColors.normalBgColor);
	g.fill3DRect(px, py, BUTTONWIDTH, height, !pressed);
	if (focus) {
	    g.setColor(JXColors.focusColor);
	    g.drawRect(px + 1, py + 1, BUTTONWIDTH - 3, height - 3);
	}
	if (disabled)
	    g.setColor(JXColors.disabledArrowColor);
	else
	    g.setColor(JXColors.arrowColor);
	
	g.drawLine(px + 4, py + 4, width - 5, py + 4);
	g.drawLine(px + 4, py + 4, (px + width) / 2, height - 5);
	g.drawLine(width - 5, py + 4, (px + width) / 2, height - 5);
    }

    /** Paints the choice's window. */
    public void paintChoiceWindow(JXGraphics g, int selected, boolean pressed) {
	g.setClip(0, 0, windowSize.width, windowSize.height);
	g.setColor(JXColors.textBgColor);
	g.fillRect(0, 0, windowSize.width - 1, windowSize.height - 1);
	g.setColor(JXColors.normalTextColor);
	g.drawRect(0, 0, windowSize.width - 1, windowSize.height - 1);
	// draw text lines
	int y = BORDER;
	for (int i = offset; i < offset + MAXENTRIES; i++)
	    if (i < items.size()) {
		if (i == selected) {
		    g.setColor(JXColors.menuSelBgColor);
		    g.fillRect(1, y, windowSize.width - 3, CHARHEIGHT);
		    g.setColor(JXColors.menuSelTextColor);
		} else
		    g.setColor(JXColors.normalTextColor);
		g.drawJXString((String) items.elementAt(i), BORDER, y);
		y += CHARHEIGHT + TEXTBORDER;
	    }
	// paint scrollbar
	if (bar != null) {
	    bar.setScrollArea(windowSize.width - BUTTONWIDTH, 0, BUTTONWIDTH, windowSize.height);
	    bar.paintScrollbar(g, pressed, false);
	}
    }
}
