package jx.awt.peer;


import java.awt.*;
import java.awt.event.*;
import java.awt.peer.CheckboxPeer;
import jx.awt.*;


/**
 * This class implements a Checkbox peer.
 */
public class JXCheckboxPeer
    extends JXComponentPeer
    implements java.awt.peer.CheckboxPeer {
    
    /** The border around the checkbox's text */
    private final static int BORDER = 2;
    /** The size of the checkbox itself */
    private final static int CHECKSIZE = 10;
    
    /** The checkbox's text */
    private String text;
    /** The flag indicating whether this checkbox is selected or not */
    private boolean selected = false;
    /** Used to avoid a certain infinite loop occasion */
    private boolean checkboxReady = true;
    /** Reference to the controlling CheckboxGroup object */
    private CheckboxGroup group = null;

    

    /** Creates a new JXCheckboxPeer instance */
    public JXCheckboxPeer(Checkbox parent, JXToolkit toolkit) {
	super(toolkit, parent);
	setCheckboxGroup(parent.getCheckboxGroup());
	setState(parent.getState());
	setLabel(parent.getLabel());
	ready = true;
    }




    /** Redraws the checkbox when space key is pressed. */
    public void keyPressed(int keyValue, int rawValue, int modifier) {
	super.keyPressed(keyValue, rawValue, modifier);
	if (keyValue == KeyEvent.VK_SPACE) {
	    peerState = PEER_PRESSED;
	    redraw();
	}
    }

    /** Redraws the checkbox when space key is released. */
    public void keyReleased(int keyValue, int rawValue, int modifier, boolean over) {
	super.keyReleased(keyValue, rawValue, modifier, over);
	if (keyValue == KeyEvent.VK_SPACE) {
	    peerState = (over) ? PEER_HOVER : PEER_NORMAL;
	    redraw();
	}
    }

    /** Sets new checkbox state and sends an item event. */
    public void keyClicked(int keyValue, int rawValue, int modifier) {
	super.keyClicked(keyValue, rawValue, modifier);
	if (keyValue == KeyEvent.VK_SPACE) {
	    int newState;
	    if (group != null) {
		newState = ItemEvent.SELECTED;
	    } else {
		newState = (selected) ? ItemEvent.DESELECTED : ItemEvent.SELECTED;
	    }
	    ((Checkbox) parent).setState((newState == ItemEvent.SELECTED));
	    queue = toolkit.getSystemEventQueue();
	    queue.postEvent(new ItemEvent(((Checkbox) parent),
					  ItemEvent.ITEM_STATE_CHANGED,
					  parent, newState));
	}
    }

    public void mousePressed(int x, int y, int button) {
	super.mousePressed(x, y, button);
	redraw();
    }
    
    public void mouseReleased(int x, int y, int button, boolean over) {
	super.mouseReleased(x, y, button, over);
	redraw();
    }
    
    /** Sets new checkbox state and sends an item event. */
    public void mouseClicked(int x, int y, int button) {
	super.mouseClicked(x, y, button);
	int newState;
	if (group != null) {
	    newState = ItemEvent.SELECTED;
	} else {
	    newState = (selected) ? ItemEvent.DESELECTED : ItemEvent.SELECTED;
	}
	((Checkbox) parent).setState((newState == ItemEvent.SELECTED));
	queue = toolkit.getSystemEventQueue();
	queue.postEvent(new ItemEvent(((Checkbox) parent),
				      ItemEvent.ITEM_STATE_CHANGED,
				      parent, newState));
    }

    public void mouseEntered(int x, int y, int button) {
	super.mouseEntered(x, y, button);
	redraw();
    }

    public void mouseExited(int x, int y, int button) {
	super.mouseExited(x, y, button);
	redraw();
    }

    /** Makes Checkbox traversable to keyboard focus. */
    public boolean isFocusTraversable() {
	return true;
    }

    /** Paints this peer. */
    public void paint(JXGraphics g) {
	boolean grouped = (group != null);
	boolean focus = hasFocus();
	switch (peerState) {
	case PEER_NORMAL:
	    if (grouped)
		paintCheckboxGrouped(g, selected, focus, false, false, false);
	    else
		paintCheckboxUngrouped(g, selected, focus, false, false, false);
	    break;
	case PEER_PRESSED:
	    if (grouped)
		paintCheckboxGrouped(g, selected, focus, true, false, false);
	    else
		paintCheckboxUngrouped(g, selected, focus, true, false, false);
	    break;
	case PEER_HOVER:
	    if (grouped)
		paintCheckboxGrouped(g, selected, focus, false, true, false);
	    else
		paintCheckboxUngrouped(g, selected, focus, false, true, false);
	    break;
	case PEER_DISABLED:
	    if (grouped)
		paintCheckboxGrouped(g, selected, focus, false, false, true);
	    else
		paintCheckboxUngrouped(g, selected, focus, false, false, true);
	    break;
	default:System.out.println("*** unsupported checkbox state!!!");
	}
    }

    /*****************************************************
     * methods implemented from CheckboxPeer              *
     *****************************************************/

    public void setCheckboxGroup(CheckboxGroup group) {
	if (this.group != group) {
	    this.group = group;
	    redraw();
	}
    }

    public void setState(boolean selected) {
	if (!checkboxReady)
	    return;
	if (this.selected != selected) {
	    this.selected = selected;
	    redraw();
	    if (group != null && (selected)) {
		// Use checkboxReady to avoid infinite loops when
		// the same checkbox is switched from "on" to "off"
		// and back
		checkboxReady = false;
		group.setSelectedCheckbox((Checkbox) parent);
		checkboxReady = true;
	    }
	}
    }

    public void setLabel(String text) {
	this.text = text;
	prefWidth = 3 * BORDER + CHECKSIZE + text.length() * CHARWIDTH;
	prefHeight = 2 * BORDER + CHARHEIGHT;
	redraw();
    }
    
    /**************************** DRAWING ****************************/

    /** Paints a grouped checkbox. */
    public void paintCheckboxGrouped(JXGraphics g, boolean selected, boolean focus,
				      boolean pressed, boolean hover, boolean disabled) {
	int px = 0;
	int py = 0;
	
	if (hover)
	    g.setColor(JXColors.hoverColor);
	else
	    g.setColor(JXColors.normalBgColor);
	Color light = g.getColor().brighter();
	Color dark = g.getColor().darker();
	g.fillRect(px, py, width, height);
	if (pressed) {
	    px++;
	    py++;
	}
	if (focus) {
	    g.setColor(JXColors.focusColor);
	    g.drawRect(px + 1, py + 1, width - 3, height - 3);
	}
	if (disabled)
	    g.setColor(JXColors.disabledTextColor);
	else
	    if (selected)
		g.setColor(dark);
	    else
		g.setColor(light);
	g.drawLine(px + BORDER, py + height / 2, px + BORDER + CHECKSIZE / 2, py + (height - CHECKSIZE) / 2);
	g.drawLine(px + BORDER, py + height / 2, px + BORDER + CHECKSIZE / 2, py + (height + CHECKSIZE) / 2);
	if (disabled)
	    g.setColor(JXColors.disabledTextColor);
	else
	    if (selected)
		g.setColor(light);
	    else
		g.setColor(dark);
	g.drawLine(px + BORDER + CHECKSIZE, py + height / 2, px + BORDER + CHECKSIZE / 2, py + (height - CHECKSIZE) / 2);
	g.drawLine(px + BORDER + CHECKSIZE, py + height / 2, px + BORDER + CHECKSIZE / 2, py + (height + CHECKSIZE) / 2);
	    
	if (disabled)
	    g.setColor(JXColors.disabledTextColor);
	else
	    g.setColor(JXColors.normalTextColor);
	g.drawJXString(text, px + 2 * BORDER + CHECKSIZE,
		     py + (height - CHARHEIGHT) / 2);
    }

    /** Paints a ungrouped checkbox. */
    public void paintCheckboxUngrouped(JXGraphics g, boolean selected, boolean focus,
					boolean pressed, boolean hover, boolean disabled) {
	int px = 0;
	int py = 0;
	if (hover)
	    g.setColor(JXColors.hoverColor);
	else
	    g.setColor(JXColors.normalBgColor);
	g.fillRect(px, py, width, height);
	if (pressed) {
	    px++;
	    py++;
	}
	if (focus) {
	    g.setColor(JXColors.focusColor);
	    g.drawRect(px + 1, py + 1, width - 3, height - 3);
	}
	if (hover)
	    g.setColor(JXColors.hoverColor);
	else
	    g.setColor(JXColors.normalBgColor);
	g.fill3DRect(px + BORDER, py + (height - CHECKSIZE) /2,
		     CHECKSIZE, CHECKSIZE, false);
	if (disabled)
	    g.setColor(JXColors.disabledBgColor);
	else
	    g.setColor(JXColors.checkboxBgColor);
	g.fillRect(px + BORDER + 1, py + (height - CHECKSIZE) /2 + 1,
		   CHECKSIZE - 3, CHECKSIZE - 3);
	if (disabled)
	    g.setColor(JXColors.disabledTextColor);
	else
	    g.setColor(JXColors.checkboxFgColor);
	if (selected) {
	    g.drawLine(px + BORDER, py + height / 2,
		       px + BORDER + CHECKSIZE / 2,
		       py + (height + CHECKSIZE) /2);
	    g.drawLine(px + BORDER + CHECKSIZE,
		       py + (height - CHECKSIZE) / 2,
		       px + BORDER + CHECKSIZE / 2,
		       py + (height + CHECKSIZE) / 2);
	}
	if (disabled)
	    g.setColor(JXColors.disabledTextColor);
	else
	    g.setColor(JXColors.normalTextColor);
	g.drawJXString(text, px + 2 * BORDER + CHECKSIZE,
		     py + (height - CHARHEIGHT) / 2);
    }
}
