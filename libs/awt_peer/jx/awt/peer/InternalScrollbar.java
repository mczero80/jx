package jx.awt.peer;

import java.awt.*;
import jx.awt.*;



/**
 * This is the internal representation of a scroll bar.
 * It is necessary to have this class, as many other peers
 * need to implement one or more private scroll bars.
 *
 * The use of a internal scroll bar is quite easy: You have
 * to call the mouseInScrollbarXXX() methods the right time,
 * that is when your component peer gets the corresponding
 * events from the JXWindowConnector; And you have to paint
 * the scroll bars whenever you paint your own component.
 */
public class InternalScrollbar {
    
    /** The width of a vertical slider button */
    public final int SLIDERWIDTH = 15;
    /** The height of a vertical slider button */
    public final int SLIDERHEIGHT = 20;

    /** mouse is not in the scroll bar area */
    public final int NONE = 0;
    /** mouse is in the upper/left button */
    public final int ARROWTOP = 1;
    /** mouse is in the lower/right button */
    public final int ARROWBOTTOM = 2;
    /** mouse is in the upper/left dragging area */
    public final int SLIDERTOP = 3;
    /** mouse is in the lower/right dragging area */
    public final int SLIDERBOTTOM = 4;
    /* mouse is in the dragging button (bubble) */
    public final int BUBBLE = 5;
    

    private JXComponentPeer host;
    private JXToolkit toolkit;
    private boolean enabled = true;
    private boolean translateCoords = true;

    private int orientation;
    private int lineIncrement;
    private int pageIncrement;
    private int value;
    private int visible;
    private int minimum;
    private int maximum;

    private Rectangle scrollArea;
    private Rectangle arrowTop;
    private Rectangle arrowBottom;
    private Rectangle sliderTop;
    private Rectangle sliderBottom;
    private Rectangle bubble;

    /** The current part of the scroll bar under the mouse */
    private int currentArea = NONE;
    /** Flag indicating whether bubble is dragged or not */
    private boolean dragging = false;
    /** the x coord of the dragging point */
    private int spotX;
    /** the y coord of the dragging point */
    private int spotY;

    

    
    /** Creates a new InternalScrollbar instance */
    public InternalScrollbar(JXComponentPeer host, JXToolkit toolkit) {
	this.host = host;
	this.toolkit = toolkit;
	arrowTop = new Rectangle(0, 0, 0, 0);
	arrowBottom = new Rectangle(0, 0, 0, 0);
	sliderTop = new Rectangle(0, 0, 0, 0);
	sliderBottom = new Rectangle(0, 0, 0, 0);
	bubble = new Rectangle(0, 0, 0, 0);
	scrollArea = new Rectangle(0, 0, 0, 0);
    }
    
    
    
    
    
    
    /**
     * Sets the "translate" flag. This is used by the Choice window.
     */
    public void setTranslate(boolean tl) {
	translateCoords = tl;
    }

    /**
     * Sets the coords of the area where to draw the scroll bar.
     */
    public void setScrollArea(int x, int y, int width, int height) {
	scrollArea.x = x;
	scrollArea.y = y;
	scrollArea.width = width;
	scrollArea.height = height;
    }

    /**
     * Checks whether the coords lie in the scroll bar area or not.
     */
    public boolean inScrollArea(int x, int y) {
	Point p = (translateCoords) ? host.getLocationRelativeToComponent(x, y) : new Point(x, y);
	//toolkit.message("coords are " + x + "," + y);
	//toolkit.message("rel. coords are " + p.x + "," + p.y);
	//toolkit.message("ScrollArea is " + scrollArea.x + "," + scrollArea.y +
	//	"," + scrollArea.width + "," + scrollArea.height);
	return (scrollArea.contains(p.x, p.y));
    }

    /**
     * Translates the coords into coords relative to the scroll bar.
     */
    private Point getScrollLocation(int x, int y) {
	Point p = (translateCoords) ? host.getLocationRelativeToComponent(x, y) : new Point(x, y);
	p.x -= scrollArea.x;
	p.y -= scrollArea.y;
	return p;
    }

    /**
     * Enables or disables the scroll bar.
     */
    public void setEnabled(boolean enabled) {
	this.enabled = enabled;
    }

    /**
     * Tells whether the scroll bar is enabled or not.
     */
    public boolean isEnabled() {
	return enabled;
    }

    /**
     * Returns the preferred width of a vertical scroll bar.
     */
    private int getPrefVerticalWidth() {
	return SLIDERWIDTH;
    }

    /**
     * Returns the preferred height of a vertical scroll bar.
     */
    private int getPrefVerticalHeight() {
	return SLIDERHEIGHT + visible + (maximum - minimum) + SLIDERHEIGHT;
    }

    /**
     * Returns the preferred width of this scroll bar.
     */
    public int getPrefWidth() {
	switch (orientation) {
	case Scrollbar.HORIZONTAL:return getPrefVerticalHeight();
	case Scrollbar.VERTICAL:return getPrefVerticalWidth();
	}
	return -1;
    }

    /**
     * Returns the preferred height of this scroll bar.
     */
    public int getPrefHeight() {
	switch (orientation) {
	case Scrollbar.HORIZONTAL:return getPrefVerticalWidth();
	case Scrollbar.VERTICAL:return getPrefVerticalHeight();
	}
	return -1;
    }

    /**
     * Returns the current value of the scroll bar.
     */
    public int getValue() {
	return value;
    }

    /**
     * Sets the current value of the scroll bar. The value is adjusted
     * silently if out of range.
     */
    public void setValue(int value) {
	if (value < minimum)
	    value = minimum;
	if (value > maximum)
	    value = maximum;
	this.value = value;
    }

    /**
     * This method is called by the host peer when a mouse button might have been
     * pressed in the scroll bar area. It returns the current part of the scroll
     * bar under the mouse button.
     */
    public int mouseInScrollbarPressed(int x, int y, int button) {
	if (!enabled || !inScrollArea(x, y))
	    return NONE;
	if (currentArea == NONE)
	    currentArea = getCurrentArea(x, y);
	if (currentArea == BUBBLE) {
	    // get coords relative to scroll bar
	    Point p = getScrollLocation(x, y);
	    // save coords relative to bubble
	    spotX = p.x - bubble.x;
	    spotY = p.y - bubble.y;
	    // assume dragging
	    dragging = true;
	}
	return currentArea;
    }

    /**
     * This method is called by the host peer when a mouse button might have been
     * released in the scroll bar area. It returns the current part of the scroll
     * bar under the mouse button. It also adjusts its value.
     */
    public int mouseInScrollbarReleased(int x, int y, int button, boolean over) {
	if (!enabled || !inScrollArea(x, y))
	    return NONE;
	int area = NONE;
	int newValue = value;
	dragging = false;
	if (currentArea == ARROWTOP) {
	    if (value >= minimum + lineIncrement)
		newValue = value - lineIncrement;
	    else
		newValue = minimum;
	}
	if (currentArea == ARROWBOTTOM) {
	    if (value <= maximum - lineIncrement)
		newValue = value + lineIncrement;
	    else
		newValue = maximum;
	}
	if (currentArea == SLIDERTOP) {
	    if (value >= minimum + pageIncrement)
		newValue = value - pageIncrement;
	    else
		newValue = minimum;
	}
	if (currentArea == SLIDERBOTTOM) {
	    if (value <= maximum - pageIncrement)
		newValue = value + pageIncrement;
	    else
		newValue = maximum;
	}
	area = currentArea;
	value = newValue;
	// reset areas
	setPaintValues();
	currentArea = getCurrentArea(x, y);
	return area;
    }

    /**
     * This method is called by the host peer when the mouse might have been
     * moved in the scroll bar area. When a mouse button is pressed, then it
     * returns BUBBLE if the bubble is dragged (and value changes!), otherwise
     * NONE. If no button is pressed, it returns the current part of the scroll
     * bar under the mouse cursor.
     */
    public int mouseInScrollbarMoved(int x, int y, int button) {
	if (!enabled)
	    return NONE;
	// if mouse is dragged, this method returns BUBBLE if bubble is dragged,
	// else NONE. if mouse is not dragged, it returns the current area
	int area = NONE;
	currentArea = getCurrentArea(x, y);
	if (button == 0)
	    dragging = false;
	if (dragging) {
	    // bubble is being dragged
	    // calculate mouse coords relative to whole scrollbar
	    int oldValue = value;
	    Point p = getScrollLocation(x, y);
	    // calculate new bubble value
	    if (orientation == Scrollbar.HORIZONTAL) {
		while (p.x < bubble.x + spotX && value >= minimum) {
		    value -= lineIncrement;
		    setPaintValues();
		}
		while (p.x > bubble.x + spotX && value <= maximum) {
		    value += lineIncrement;
		    setPaintValues();
		}
	    } else {
		while (p.y < bubble.y + spotY && value >= minimum) {
		    value -= lineIncrement;
		    setPaintValues();
		}
		while (p.y > bubble.y + spotY && value <= maximum) {
		    value += lineIncrement;
		    setPaintValues();
		}
	    }
	    if (value < minimum)
		value = minimum;
	    if (value > maximum)
		value = maximum;
	    if (oldValue != value)
		area = BUBBLE;
	} else
	    area = currentArea;
	return area;
    }

    /**
     * This method is called by the host peer when the mouse has
     * entered the scroll bar area.
     */
    public void mouseInScrollbarEntered(int x, int y, int button) {	
	if (!enabled || !inScrollArea(x, y))
	    return;
	currentArea = getCurrentArea(x, y);
    }

    /**
     * This method is called by the host peer when the mouse has
     * left the scroll bar area.
     */
    public void mouseInScrollbarExited(int x, int y, int button) {	
	if (!enabled)
	    return;
	currentArea = NONE;
    }

    /**
     * Sets the values of the scroll bar. "value" is the current value of the bar,
     * "visible" means the width/height of the bubble, "min" is the lower and "max"
     * the upper border of the value range. If not set before, this method also
     * sets the small increment to "1" and the big increment to "visible".
     */
    public void setValues(int value, int visible, int min, int max) {

	if (visible > max - min)
	    visible = max - min;
	if (value + visible > max)
	    value = max - visible;
	this.value = value;
	this.visible = visible;
	this.minimum = min;
	this.maximum = max - visible;

	if (lineIncrement == 0)
	    lineIncrement = 1;
	if (pageIncrement == 0)
	    pageIncrement = visible;
    }

    /**
     * Sets the small increment, when the mouse presses on a scroll bar button (not bubble!).
     */
    public void setSmallIncrement(int increment) {
	lineIncrement = increment;
    }

    /**
     * Sets the big increment, when the mouse presses on a scroll bar dragging area.
     */
    public void setBigIncrement(int increment) {
	pageIncrement = increment;
    }

    /**
     * Sets the orientation of the scroll bar.
     */
    public void setOrientation(int o) {
	orientation = o;
    }

    /** Used to calculate the proper position of some scroll bar elements. */
    private int valueToPixel(int val, int min, int max, int range) {
	if (max - min == 0)
	    return 0;
	else
	    return (val * range) / (max - min);
    }

    /**
     * Returns the part of the scroll bar that contains the point (x, y).
     */
    private int getCurrentArea(int x, int y) {
	Point p = getScrollLocation(x, y);
	if (sliderTop.contains(p.x, p.y))
	    return SLIDERTOP;
	if (sliderBottom.contains(p.x, p.y))
	    return SLIDERBOTTOM;
	if (arrowTop.contains(p.x, p.y))
	    return ARROWTOP;
	if (arrowBottom.contains(p.x, p.y))
	    return ARROWBOTTOM;
	if (bubble.contains(p.x, p.y))
	    return BUBBLE;
	return NONE;
    }

    /** Calculates all scroll bar painting coords to draw it properly. */
    private void setPaintValues() {
	switch (orientation) {
	case Scrollbar.HORIZONTAL:
	    arrowTop.x = 0;
	    arrowTop.y = 0;
	    arrowTop.width = SLIDERHEIGHT;
	    arrowTop.height = scrollArea.height;
	    arrowBottom.x = scrollArea.width - SLIDERHEIGHT;
	    arrowBottom.y = 0;
	    arrowBottom.width = SLIDERHEIGHT;
	    arrowBottom.height = scrollArea.height;
	    bubble.x = SLIDERHEIGHT + valueToPixel(value, minimum, maximum + visible,
					     scrollArea.width - arrowTop.width - arrowBottom.width);
	    bubble.y = 0;
	    bubble.width = valueToPixel(visible, minimum, maximum + visible,
				       scrollArea.width - arrowTop.width - arrowBottom.width);
	    bubble.height = scrollArea.height;
	    sliderTop.x = SLIDERHEIGHT;
	    sliderTop.y = 0;
	    sliderTop.width = bubble.x - sliderTop.x;
	    sliderTop.height = scrollArea.height;
	    sliderBottom.x = bubble.x + bubble.width;
	    sliderBottom.y = 0;
	    sliderBottom.width = arrowBottom.x - (bubble.x + bubble.width);
	    sliderBottom.height = scrollArea.height;
	    break;
	case Scrollbar.VERTICAL:
	    arrowTop.x = 0;
	    arrowTop.y = 0;
	    arrowTop.width = scrollArea.width;
	    arrowTop.height = SLIDERHEIGHT;
	    arrowBottom.x = 0;
	    arrowBottom.y = scrollArea.height - SLIDERHEIGHT;
	    arrowBottom.width = scrollArea.width;
	    arrowBottom.height = SLIDERHEIGHT;
	    bubble.x = 0;
	    bubble.y = SLIDERHEIGHT + valueToPixel(value, minimum, maximum + visible,
					     scrollArea.height - arrowTop.height - arrowBottom.height);
	    bubble.width = scrollArea.width;
	    bubble.height = valueToPixel(visible, minimum, maximum + visible,
					scrollArea.height - arrowTop.height - arrowBottom.height);
	    sliderTop.x = 0;
	    sliderTop.y = SLIDERHEIGHT;
	    sliderTop.width = scrollArea.width;
	    sliderTop.height = bubble.y - sliderTop.y;
	    sliderBottom.x = 0;
	    sliderBottom.y = bubble.y + bubble.height;
	    sliderBottom.width = scrollArea.width;
	    sliderBottom.height = arrowBottom.y - (bubble.y + bubble.height);
	    break;
	}
    }

    /****************************** DRAWING *******************************/

    /**
     * Draws the scroll bar. This method is called from the host peer when it
     * repaints itself.
     */
    public void paintScrollbar(Graphics g, boolean pressed, boolean hostDisabled) {
	// the scrollbar is enabled only if both the host and the scrollbar itself
	// are enabled
	boolean disabled = !enabled || hostDisabled;
	// reset current paint positions
	setPaintValues();
	// translate and clip drawing area
	g.setClip(scrollArea);
	g.translate(scrollArea.x, scrollArea.y);

	if (currentArea == SLIDERTOP && !disabled)
	    g.setColor(JXColors.hoverColor);
	else
	    g.setColor(JXColors.normalBgColor);
	g.fillRect(sliderTop.x, sliderTop.y, sliderTop.width - 1, sliderTop.height - 1);
	if (currentArea == SLIDERBOTTOM && !disabled)
	    g.setColor(JXColors.hoverColor);
	else
	    g.setColor(JXColors.normalBgColor);
	g.fillRect(sliderBottom.x, sliderBottom.y, sliderBottom.width - 1, sliderBottom.height - 1);
	
	g.setColor(JXColors.normalBgColor.darker());
	g.drawRect(0, 0, scrollArea.width - 1, scrollArea.height - 1);
	
	if (currentArea == ARROWTOP && !disabled)
	    g.setColor(JXColors.hoverColor);
	else
	    g.setColor(JXColors.normalBgColor);
	g.fill3DRect(arrowTop.x, arrowTop.y, arrowTop.width, arrowTop.height, !(pressed && currentArea == ARROWTOP));
	if (currentArea == ARROWBOTTOM && !disabled)
	    g.setColor(JXColors.hoverColor);
	else
	    g.setColor(JXColors.normalBgColor);
	g.fill3DRect(arrowBottom.x, arrowBottom.y, arrowBottom.width, arrowBottom.height, !(pressed && currentArea == ARROWBOTTOM));
	
	switch (orientation) {
	case Scrollbar.HORIZONTAL:
	    if (disabled)
		g.setColor(JXColors.disabledArrowColor);
	    else
		g.setColor(JXColors.arrowColor);
	    g.drawLine(2, scrollArea.height / 2, arrowTop.width / 2, 2);
	    g.drawLine(arrowTop.width / 2, 2, arrowTop.width / 2, scrollArea.height - 3);
	    g.drawLine(2, scrollArea.height / 2, arrowTop.width / 2, scrollArea.height - 3);
	    g.drawLine(scrollArea.width - arrowBottom.width / 2, 2, scrollArea.width - arrowBottom.width / 2, scrollArea.height - 3);
	    g.drawLine(scrollArea.width - arrowBottom.width / 2, 2, scrollArea.width - 3, scrollArea.height / 2);
	    g.drawLine(scrollArea.width - arrowBottom.width / 2, scrollArea.height - 3, scrollArea.width - 3, scrollArea.height / 2);
	    break;
	case Scrollbar.VERTICAL:
	    if (disabled)
		g.setColor(JXColors.disabledArrowColor);
	    else
		g.setColor(JXColors.arrowColor);
	    g.drawLine(scrollArea.width / 2, 2, 2, arrowTop.height / 2);
	    g.drawLine(scrollArea.width / 2, 2, scrollArea.width - 3, arrowTop.height / 2);
	    g.drawLine(2, arrowTop.height / 2, scrollArea.width - 3, arrowTop.height / 2);
	    g.drawLine(scrollArea.width / 2, scrollArea.height - 3, 2, scrollArea.height - arrowBottom.height / 2);
	    g.drawLine(scrollArea.width / 2, scrollArea.height - 3, scrollArea.width - 3, scrollArea.height - arrowBottom.height / 2);
	    g.drawLine(2, scrollArea.height - arrowBottom.height / 2, scrollArea.width - 3, scrollArea.height - arrowBottom.height / 2);
	    break;
	}

	if (currentArea == BUBBLE && !disabled)
	    g.setColor(JXColors.hoverColor);
	else
	    g.setColor(JXColors.normalBgColor);
	if (disabled) {
	    g.fillRect(bubble.x + 1, bubble.y + 1, bubble.width - 3, bubble.height - 3);
	    g.setColor(JXColors.hoverColor);
	    g.drawRect(bubble.x, bubble.y, bubble.width - 1, bubble.height - 1);
	} else
	    g.fill3DRect(bubble.x, bubble.y, bubble.width, bubble.height, !(pressed && currentArea == BUBBLE));
    }
    
}
