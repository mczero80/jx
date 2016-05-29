package jx.awt.peer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import jx.awt.*;
import java.util.Vector;


/**
 * This class implements a List peer.
 */
public class JXListPeer
    extends JXComponentPeer
    implements java.awt.peer.ListPeer {

    /** The border around the text lines */
    private final int BORDER = 3;
    /** The space between the text lines */
    private final int TEXTBORDER = 1;
    /** Number of characters visible in a text line */
    private final int INITPREFWIDTH = 20;
    
    private Vector items;
    private boolean multipleMode;
    private int cursor;
    private int offset;
    private int markStart;

    private InternalScrollbar bar;
    private int lastBarArea;


    /** Creates a new JXListPeer instance */
    public JXListPeer(List parent, JXToolkit toolkit) {
	super(toolkit, parent);
	setMultipleMode(parent.isMultipleMode());
	items = new Vector();
	for (int i = 0; i < parent.getItemCount(); i++)
	    add(parent.getItem(i), i);
	bar = new InternalScrollbar(this, toolkit);
	bar.setOrientation(Scrollbar.VERTICAL);
	lastBarArea = bar.NONE;
	Dimension d = getPrefSize(parent.getRows());
	prefWidth = d.width;
	prefHeight = d.height;
	offset = 0;
	cursor = 0;
	markStart = -1;
	ready = true;
    }



    /** Handles all key presses in the list. */
    public void keyPressed(int keyValue, int rawValue, int modifier) {
	super.keyPressed(keyValue, rawValue, modifier);
	boolean modified = false;
	// evaluate shift key (mark start of multiple selections)
	if (KeyMap.translate(keyValue, rawValue) == KeyEvent.VK_SHIFT)
	    markStart = cursor;
	else
	    if (!KeyMap.shiftPressed(modifier))
		markStart = -1;

	switch (KeyMap.translate(keyValue)) {
	case KeyEvent.VK_UP:
	    if (cursor > 0) {
		cursor--;
		if (!multipleMode)
		    select(cursor);
		updateOffset(true);
		modified = true;
	    }
	    break;
	case KeyEvent.VK_DOWN:
	    if (cursor < items.size() - 1) {
		cursor++;
		if (!multipleMode)
		    select(cursor);
		updateOffset(true);
		modified = true;
	    }
	    break;
	case KeyEvent.VK_SPACE:
	    // perform selection with shift
	    setMarkedSelection();
	    // flip current selection
	    flipSelection(cursor);
	    modified = true;
	    break;
	case KeyEvent.VK_ENTER:
	    // send action event
	    sendActionEvent(cursor);
	    break;
	default:
	    modified = false;
	}
	// redraw only if processed key has been pressed
	if (modified) {
	    redraw();
	}
    }
    
    /** Handles all mouse button presses in the list. */
    public void mousePressed(int x, int y, int button) {
	super.mousePressed(x, y, button);
	if (bar.inScrollArea(x, y)) {
	    int where = bar.mouseInScrollbarPressed(x, y, button);
	    if (where != bar.NONE)
		redraw();
	} else {
	    // find new entry and set cursor
	    int i = getListEntry(x, y);
	    if (i >= 0) {
		cursor = i;
		redraw();
	    }
	}
    }
    
    /** Handles all mouse button releases in the list. */
    public void mouseReleased(int x, int y, int button, boolean over) {
	super.mouseReleased(x, y, button, over);
	if (bar.inScrollArea(x, y)) {
	    int where = bar.mouseInScrollbarReleased(x, y, button, over);
	    if (where != bar.NONE) {
		offset = bar.getValue();
		redraw();
	    }
	} else {
	    // perform selection with shift
	    setMarkedSelection();
	    // flip current selection
	    flipSelection(cursor);
	    redraw();
	}
    }

    /** Handles all mouse moves on the list. */
    public void mouseMoved(int x, int y, int button) {
	super.mouseMoved(x, y, button);
	if (bar.inScrollArea(x, y)) {
	    int newArea = bar.mouseInScrollbarMoved(x, y, button);
	    if (newArea != lastBarArea) {
		if (newArea != bar.NONE) {
		    lastBarArea = newArea;
		    redraw();
		}
	    }
	    if (button != 0 && newArea == bar.BUBBLE) {
		offset = bar.getValue();
		redraw();
	    }
	} else {
	    if (lastBarArea != bar.NONE) {
		lastBarArea = bar.NONE;
		bar.mouseInScrollbarExited(x, y, button);
		redraw();
	    }
	}
    }
      
    public void mouseEntered(int x, int y, int button) {
	super.mouseEntered(x, y, button);
	bar.mouseInScrollbarEntered(x, y, button);
	redraw();
    }
    
    public void mouseExited(int x, int y, int button) {
	super.mouseExited(x, y, button);
	bar.mouseInScrollbarExited(x, y, button);
	lastBarArea = bar.NONE;
	redraw();
    }

    /** Paints this peer. */
    public void paint(JXGraphics g) {
	updateOffset(false);
	boolean focus = hasFocus();
	switch (peerState) {
	case PEER_PRESSED:paintList(g, focus, true, false, false);break;
	case PEER_NORMAL:paintList(g, focus, false, false, false);break;
	case PEER_HOVER:paintList(g, focus, false, true, false);break;
	case PEER_DISABLED:paintList(g, focus, false, false, true);break;
	default:System.out.println("*** JXChoicePeer: Unknown state!!!");
	}
    }

    /** Make List traversable to keyboard focus. */
    public boolean isFocusTraversable() {
	return true;
    }

    /** Get preferred size of list peer. */
    private Dimension getPrefSize(int rows) {
	int w = CHARWIDTH * INITPREFWIDTH + 2 * BORDER;
	int h = rows * CHARHEIGHT + (rows - 1) * TEXTBORDER + 2 * BORDER;
	return new Dimension(w, h);
    }

    /** Converts height in pixels to number of showable text lines. */
    private int heightToRows(int height) {
	return (height + TEXTBORDER) / (CHARHEIGHT + TEXTBORDER);
    }

    /** Gets the list entry currently under (x, y). */
    private int getListEntry(int x, int y) {
	Point p = getLocationRelativeToComponent(x, y);
	return offset + (p.y - BORDER) / (CHARHEIGHT + TEXTBORDER);
    }

    /**
     * Performs a shift-selection, i.e. selects all list entries between the
     * current selection and the one marked with markStart.
     */
    private void setMarkedSelection() {
	if (multipleMode && markStart != -1) {
	    if (cursor > markStart) {
		for (int i = markStart; i < cursor; i++)
		    select(i);
	    } else
		if (cursor < markStart) {
		    for (int i = cursor + 1; i <= markStart; i++)
			select(i);
		}
	    markStart = -1;
	}
    }
    
    /**
     * Updates the scroll offset and the scroll bar.
     */
    private void updateOffset(boolean adjustToCursor) {
	int visHeight = heightToRows(height - 2 * BORDER);
	if (adjustToCursor) {
	    if (offset > cursor)
		offset = cursor;
	    if (offset + visHeight - 1 < cursor)
		offset = cursor - visHeight + 1;
	}
	if (offset + visHeight >= items.size())
	    offset = items.size() - visHeight;
	if (offset < 0)
	    offset = 0;

	bar.setValues(offset, visHeight,
		      0, items.size());
    }
    
    /**
     * Checks whether the current selection made is valid and, if not,
     * performs some fixing operations.
     */
    private void validateSelection(int index, boolean selected) {
	if (!multipleMode) {
	    if (selected) {
		// delete old selections
		for (int i = 0; i < items.size(); i++)
		    if (i != index && ((JXListElement) items.elementAt(i)).selected)
			((JXListElement) items.elementAt(i)).selected = false;
	    } else {
		// reselect unselected item
		((JXListElement) items.elementAt(index)).selected = true;
	    }
	}
    }

    /**
     * Flips the selection state of the chosen list entry.
     */
    private void flipSelection(int index) {
	if (((JXListElement) items.elementAt(index)).selected)
	    deselect(index);
	else
	    select(index);
    }

    /** Sends an action event. */
    private void sendActionEvent(int index) {
	if (items.size() > 0) {
	    queue = toolkit.getSystemEventQueue();
	    queue.postEvent(new ActionEvent(parent, ActionEvent.ACTION_PERFORMED,
					    ((JXListElement) items.elementAt(index)).text,
					    0));
	}
    }

    /** Sends an item event. */
    private void sendItemEvent(int index) {
	int newState = (((JXListElement) items.elementAt(index)).selected)
	    ? ItemEvent.SELECTED
	    : ItemEvent.DESELECTED;
	queue = toolkit.getSystemEventQueue();
	queue.postEvent(new ItemEvent((ItemSelectable) parent,
				      ItemEvent.ITEM_STATE_CHANGED,
				      new Integer(index),
				      newState));
    }

    /*************************************************
     * methods implemented from ListPeer              *
     *************************************************/

    public void add(String item, int index) {
	JXListElement l = new JXListElement(item);
	if (index == -1 || index >= items.size())
	    items.addElement(l);
	else
	    items.insertElementAt(l, index);
    }

    public void addItem(String item, int index) {
	add(item, index);
    }

    public void delItems(int start_index, int end_index) {
	for (int i = end_index; i >= start_index; i--)
	    items.removeElementAt(i);
    }
    
    public void select(int index) {
	((JXListElement) items.elementAt(index)).selected = true;
	validateSelection(index, true);
	sendItemEvent(index);
    }
    
    public void deselect(int index) {
	((JXListElement) items.elementAt(index)).selected = false;
	validateSelection(index, false);
	sendItemEvent(index);
    }
    
    public int[] getSelectedIndexes() {
	// find number of selected rows
	int count = 0;
	for (int i = 0; i < items.size(); i++)
	    if (((JXListElement) items.elementAt(i)).selected)
		count++;
	// create result array
	int[] selected = new int[count];
	// fill array
	count = 0;
	for (int i = 0; i < items.size(); i++)
	    if (((JXListElement) items.elementAt(i)).selected) {
		selected[count] = i;
		count++;
	    }
	return selected;
    }

    public void makeVisible(int index) {
	offset = index;
	updateOffset(true);
    }

    public Dimension minimumSize(int s) {
	return getPrefSize(s);
    }

    public Dimension preferredSize(int s) {
	return minimumSize(s);
    }

    public void removeAll() {
	items = new Vector();
    }

    public void clear() {
	removeAll();
    }

    public void setMultipleMode(boolean multi) {
	multipleMode = multi;
    }

    public void setMultipleSelections(boolean multi) {
	setMultipleMode(multi);
    }
    
    /*************************** DRAWING **************************/

    /** Paints the list. */
    public void paintList(JXGraphics g, boolean focus, boolean pressed,
			  boolean hover, boolean disabled) {
	// draw background
	g.setColor(JXColors.normalBgColor);
	g.draw3DRect(0, 0, width - bar.getPrefWidth() - 1, height - 1, false);
	if (!disabled)
	    g.setColor(JXColors.textBgColor);
	else
	    g.setColor(JXColors.disabledBgColor);
	g.fillRect(1, 1, width - bar.getPrefWidth() - 3, height - 3);
	if (focus) {
	    g.setColor(JXColors.focusColor);
	    g.drawRect(1, 1, width - bar.getPrefWidth() - 3, height - 3);
	}
	// draw list
	int end = heightToRows(height - 2 * BORDER);
	int y = BORDER;
	for (int i = offset; (i < offset + end) && (i < items.size()); i++) {
	    JXListElement l = (JXListElement) items.elementAt(i);
	    if (l.selected) {
		g.setColor(JXColors.menuSelBgColor);
		g.fillRect(BORDER, y, width - bar.getPrefWidth() - 2 * BORDER - 1, CHARHEIGHT);
		g.setColor(JXColors.menuSelTextColor);
	    } else
		g.setColor(JXColors.normalTextColor);
	    if (disabled)
		g.setColor(JXColors.disabledTextColor);
	    g.drawJXString(l.text, BORDER, y);
	    // draw cursor
	    if (i == cursor && !disabled) {
		g.setColor(JXColors.normalTextColor);
		g.drawRect(BORDER, y, width - bar.getPrefWidth() - 2 * BORDER - 1, CHARHEIGHT);
	    }
	    y += CHARHEIGHT + TEXTBORDER;
	}
	
	// paint scrollbar
	bar.setScrollArea(width - bar.getPrefWidth(), 0, bar.getPrefWidth(), height);
	bar.paintScrollbar(g, pressed, disabled);
    }
}



/**
 * This is a helper class used to build an internal list.
 */
class JXListElement {

    /** The entry's text*/
    public String text;
    /** The entry's selection state */
    public boolean selected;

    /** Creates a new JXListElement instance */
    public JXListElement(String text) {
	this.text = text;
	selected = false;
    }
}
