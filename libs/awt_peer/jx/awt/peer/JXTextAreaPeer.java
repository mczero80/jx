package jx.awt.peer;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.peer.TextAreaPeer;
import java.util.*;
import jx.awt.*;


/**
 * This class implements the TextArea peer.
 */
public class JXTextAreaPeer
    extends JXTextComponentPeer
    implements TextAreaPeer {


    private InternalScrollbar rightBar;
    private InternalScrollbar lowerBar;

    private Vector textStrings;
    private int lastRightArea;
    private int lastLowerArea;
    private int lastRightPressed;
    private int lastLowerPressed;

    private int visibleRows;
    private int visibleColumns;


    /** Creates a new JXTextAreaPeer instance */
    public JXTextAreaPeer(TextArea parent, JXToolkit toolkit) {
	super(toolkit, parent);
	columns = parent.getColumns();
	rows = parent.getRows();
	visibleRows = rows;
	visibleColumns = columns;
	offsetX = 0;
	offsetY = 0;
	textSpace = initialTextSpace();
	setScrollbars(parent.getScrollbarVisibility());
	prefWidth = textSpace.width;
	prefHeight = textSpace.height;
	ready = true;
    }
    
    
    


    /** Paints this peer. */
    public void paint(JXGraphics g) {
	resetLayout();
	boolean focus = hasFocus();
	switch (peerState) {
	case PEER_NORMAL:paintTextArea(g, focus, false, false, false);break;
	case PEER_PRESSED:paintTextArea(g, focus, true, false, false);break;
	case PEER_HOVER:paintTextArea(g, focus, false, true, false);break;
	case PEER_DISABLED:paintTextArea(g, focus, false, false, true);break;
	}
    }

    /** Performs any text editing when a key is pressed. */
    public void keyPressed(int keyValue, int rawValue, int modifier) {
	super.keyPressed(keyValue, rawValue, modifier);
	performTextEditing(keyValue, modifier, true);
	updateOffsets();
	redraw();
    }

    /**
     * Handles all mouse press events. If the mouse is over a scroll bar,
     * this bar is updated, otherwise the caret is set to the mouse's
     * current position.
     */
    public void mousePressed(int x, int y, int button) {
	super.mousePressed(x, y, button);
	if ((rightBar != null && (lastRightPressed = rightBar.mouseInScrollbarPressed(x, y, button)) != rightBar.NONE) ||
	    (lowerBar != null && (lastLowerPressed = lowerBar.mouseInScrollbarPressed(x, y, button)) != lowerBar.NONE))
	    redraw();
	else {
	    Point p = getLocationRelativeToComponent(x - BORDER + offsetX * CHARWIDTH,
						     y - BORDER + offsetY * CHARHEIGHT + (offsetY - 1) * TEXTBORDER);
	    caretPosition = getTextPosition(p.x, p.y);
	    selectionStart = caretPosition;
	    selectionEnd = caretPosition;
	    updateOffsets();
	    redraw();
	}
    }

    /**
     * Handles all mouse release events. If the mouse is over a scroll bar,
     * this bar and the offset are updated.
     */
    public void mouseReleased(int x, int y, int button, boolean over) {
	super.mouseReleased(x, y, button, over);
	if ((rightBar != null && handleScrollbarReleased(rightBar, x, y, button, over)) ||
	    (lowerBar != null && handleScrollbarReleased(lowerBar, x, y, button, over))) {
	    if (rightBar != null) {
		offsetY = rightBar.getValue();
		lastRightPressed = rightBar.NONE;
	    }
	    if (lowerBar != null) {
		offsetX = lowerBar.getValue();
		lastLowerPressed = lowerBar.NONE;
	    }
	    redraw();
	}
    }

    /**
     * Handles all mouse move events. If the mouse is over a scroll bar,
     * the bar and the offset are updated. Otherwise, if a button is
     * pressed, the selection on the text is altered.
     */
    public void mouseMoved(int x, int y, int button) {
	super.mouseMoved(x, y, button);
	boolean draw = false;
	// handle scroll areas
	if ((rightBar != null && handleScrollbarMoved(rightBar, x, y, button)) ||
	    (lowerBar != null && handleScrollbarMoved(lowerBar, x, y, button))) {
	    if (rightBar != null && offsetY != rightBar.getValue()) {
		offsetY = rightBar.getValue();
		draw = true;
	    }
	    if (lowerBar != null && offsetX != lowerBar.getValue()) {
		offsetX = lowerBar.getValue();
		draw = true;
	    }
	    if (draw || button == 0)
		redraw();
	    return;
	}
	// handle text area
	Point m = getLocationRelativeToComponent(x, y);
	if (button != 0 && lastRightPressed == 0 && lastLowerPressed == 0) {
	    // pressed mouse is over text area
	    Point p = getLocationRelativeToComponent(x - BORDER + offsetX * CHARWIDTH,
						     y - BORDER + offsetY * CHARHEIGHT + (offsetY - 1) * TEXTBORDER);
	    int newpos = getTextPosition(p.x, p.y);
	    if (newpos != selectionEnd) {
		selectionEnd = newpos;
		caretPosition = selectionEnd;
		updateOffsets();
		redraw();
	    }
	}
    }

    /** Updates the scroll bars. */
    public void mouseEntered(int x, int y, int button) {
	super.mouseEntered(x, y, button);
	if (rightBar != null)
	    rightBar.mouseInScrollbarEntered(x, y, button);
	if (lowerBar != null)
	    lowerBar.mouseInScrollbarEntered(x, y, button);
	redraw();
    }
    
    /** Updates the scroll bars. */
    public void mouseExited(int x, int y, int button) {
	super.mouseExited(x, y, button);
	if (rightBar != null)
	    rightBar.mouseInScrollbarExited(x, y, button);
	if (lowerBar != null)
	    lowerBar.mouseInScrollbarExited(x, y, button);
	redraw();
    }
    
    /** Helper method to handle mouse button releases on scroll bar. */
    private boolean handleScrollbarReleased(InternalScrollbar is, int x, int y, int button, boolean over) {
	return (is.mouseInScrollbarReleased(x, y, button, over) != is.NONE);
    }

    /**
     * Helper method to handle mouse movements on scroll bar. This
     * method returns true if something occured on a scroll bar that
     * needs the bar to be updated.
     */
    private boolean handleScrollbarMoved(InternalScrollbar is, int x, int y, int button) {
	int ret;
	ret = is.mouseInScrollbarMoved(x, y, button);
	if (button != 0 && ret == is.BUBBLE)
	    return true;
	if ((is == rightBar && ret != lastRightArea)) {
	    lastRightArea = ret;
	    return true;
	}
	if ((is == lowerBar && ret != lastLowerArea)) {
	    lastLowerArea = ret;
	    return true;
	}
	return false;
    }

    /** Adjusts offset position if in a invalid range. */
    private void updateOffsets() {
	// find caret coords
	int count = 0;
	int x = 0;
	int y = 0;
	for (int i = 0; i < textStrings.size(); i++) {
	    if (count <= caretPosition && count + ((String) textStrings.elementAt(i)).length() >= caretPosition) {
		x = caretPosition - count;
		break;
	    }
	    y++;
	    count += ((String) textStrings.elementAt(i)).length() + 1;
	}
	// adjust offset
	if (offsetX > x)
	    offsetX = x;
	if (offsetX < x - visibleColumns + 2)
	    offsetX = x - visibleColumns + 2;
	if (offsetY > y)
	    offsetY = y;
	if (offsetY < y - visibleRows + 2)
	    offsetY = y - visibleRows + 2;
	// update scroll bars
	if (rightBar != null)
	    rightBar.setValue(offsetY);
	if (lowerBar != null)
	    lowerBar.setValue(offsetX);
    }

    /** Initializes the needed scroll bars. */
    private void setScrollbars(int visibility) {
	rightBar = null;
	lowerBar = null;
	if (visibility == TextArea.SCROLLBARS_BOTH ||
	    visibility == TextArea.SCROLLBARS_VERTICAL_ONLY) {
	    rightBar = new InternalScrollbar(this, toolkit);
	    rightBar.setOrientation(Scrollbar.VERTICAL);
	    rightBar.setSmallIncrement(1);
	    lastRightPressed = rightBar.NONE;
	}
	if (visibility == TextArea.SCROLLBARS_BOTH ||
	    visibility == TextArea.SCROLLBARS_HORIZONTAL_ONLY) {
	    lowerBar = new InternalScrollbar(this, toolkit);
	    lowerBar.setOrientation(Scrollbar.HORIZONTAL);
	    lowerBar.setSmallIncrement(1);
	    lastLowerPressed = lowerBar.NONE;
	}
    }

    /** Converts the width in pixels to the number of visible columns. */
    private int widthToColumns(int width) {
	return width / CHARWIDTH;
    }
      
    /** Updates the layout of the TextArea. */
    private void resetLayout() {
	// set new dimensions
	int nWidth = width;
	int nHeight = height;
	if (rightBar != null)
	    nWidth -= rightBar.getPrefWidth();
	if (lowerBar != null)
	    nHeight -= lowerBar.getPrefHeight();
	// reset current columns & rows settings
	int nRows = 0;
	int nColumns = 0;
	for (int i = 0; i < textStrings.size(); i++) {
	    int l = ((String) textStrings.elementAt(i)).length();
	    if (l > nColumns)
		nColumns = l;
	}
	nRows = textStrings.size();
	// to show caret when over last character
	nColumns++;
	// show at least one row
	if (nRows == 0)
	    nRows = 1;

	// Only update layout if necessary
	if (nWidth != textSpace.width || nHeight != textSpace.height ||
	    nRows != rows || nColumns != columns) {
	    // update values
	    textSpace.width = nWidth;
	    textSpace.height = nHeight;
	    rows = nRows;
	    columns = nColumns;
	    // update scroll bars and offsets
	    if (rightBar != null) {
		visibleRows = heightToRows(textSpace.height - 2 * BORDER);
		rightBar.setValues(rightBar.getValue(),
				   visibleRows,
				   0,
				   rows);
		offsetY = rightBar.getValue();
		rightBar.setBigIncrement(visibleRows);
	    }
	    if (lowerBar != null) {
		visibleColumns = widthToColumns(textSpace.width - 2 * BORDER);
		lowerBar.setValues(lowerBar.getValue(),
				   visibleColumns,
				   0,
				   columns);
		offsetX = lowerBar.getValue();
		lowerBar.setBigIncrement(visibleColumns);
	    }
	}
    }

    /** Performs some internal conversions. */
    protected void handleTextAltered() {
	// Cut text buffer into its text lines. This should
	// improve performance and resource handling.
	textStrings = new Vector();
	StringTokenizer st = new StringTokenizer(text.toString(), newline);
	while (st.hasMoreTokens())
	    textStrings.addElement(st.nextToken());
    }

    public void dispose() {
	rightBar = null;
	lowerBar = null;
	super.dispose();
    }

    /************************************************************
     * methods implemented from TextAreaPeer                     *
     ************************************************************/

    public Dimension minimumSize(int rows, int cols) {
	Dimension d = new Dimension(textSpace.x + textSpace.width,
				    textSpace.y + textSpace.height);
	if (rightBar != null)
	    d.width += rightBar.getPrefWidth();
	if (lowerBar != null)
	    d.height += lowerBar.getPrefHeight();
	return d;
    }

    public Dimension getMinimumSize(int rows, int cols) {
	return minimumSize(rows, cols);
    }

    public Dimension preferredSize(int rows, int cols) {
	return minimumSize(rows, cols);
    }

    public Dimension getPreferredSize(int rows, int cols) {
	return preferredSize(rows, cols);
    }

    public void insert(String text, int pos) {
	this.text.insert(pos, text);
	if (pos <= caretPosition)
	    caretPosition += text.length();
	handleTextAltered();
	redraw();
    }

    public void insertText(String text, int pos) {
	insert(text, pos);
    }

    public void replaceRange(String text, int start_pos, int end_pos) {
	this.text.replace(start_pos, end_pos, text);
	handleTextAltered();
	redraw();
    }

    public void replaceText(String text, int start_pos, int end_pos) {
	replaceRange(text, start_pos, end_pos);
    }

    /************************* DRAWING **************************/

    /** Paints the text area. */
    public void paintTextArea(JXGraphics g, boolean focus, boolean pressed,
			      boolean hover, boolean disabled) {
	g.setColor(JXColors.normalBgColor);
	g.draw3DRect(textSpace.x, textSpace.y, textSpace.width - 1, textSpace.height - 1, false);
	if (!disabled && editable)
	    g.setColor(JXColors.textBgColor);
	else
	    g.setColor(JXColors.disabledBgColor);
	g.fillRect(textSpace.x + 1, textSpace.y + 1, textSpace.width - 3, textSpace.height - 3);
	if (focus) {
	    g.setColor(JXColors.focusColor);
	    g.drawRect(textSpace.x + 1, textSpace.y + 1, textSpace.width - 3, textSpace.height - 3);
	}
	if (lowerBar != null && rightBar != null) {
	    g.setColor(JXColors.normalBgColor);
	    g.fillRect(textSpace.width, textSpace.height,
		       width - textSpace.width,
		       height - textSpace.height);
	}

	// draw text
	g.setClip(BORDER, BORDER, textSpace.width - 2 * BORDER, textSpace.height - 2 * BORDER);
	int x = BORDER - offsetX * CHARWIDTH;
	int y = BORDER - offsetY * CHARHEIGHT - (offsetY - 1) * TEXTBORDER;
	int count = 0;
	int selStart = getLowerSelection();
	int selEnd = getUpperSelection();
	boolean selecting = false;
	boolean endSelect = false;
	for (int index = 0; index < textStrings.size(); index++) {
	    String s = ((String) textStrings.elementAt(index));
	    if (y + CHARHEIGHT > BORDER && y < textSpace.height - BORDER && x + textWidth(s) > BORDER) {
		if (disabled)
		    g.setColor(JXColors.disabledTextColor);
		else
		    g.setColor(JXColors.textTextColor);
		g.drawJXString(s, x, y);
		// draw caret
		if (count <= caretPosition && count + s.length() >= caretPosition && focus) {
		    int rx = (caretPosition - count - offsetX) * CHARWIDTH + BORDER;
		    paintCaret(g, rx, y);
		}
	    }
	    // draw selection
	    if (selEnd != selStart) {
		int factor1;
		int factor2;
		// calculating left x coordinate
		if (!selecting && count <= selStart && count + s.length() >= selStart) {
		    factor1 = selStart - count;
		    selecting = true;
		} else
		    factor1 = 0;
		// calculating right x coordinate
		if (selecting && count <= selEnd && count + s.length() >= selEnd) {
		    factor2 = selEnd - count;
		    endSelect = true;
		} else
		    factor2 = s.length();
		// draw current selection rectangle
		if (selecting) {
		    int rx = (factor1 - offsetX) * CHARWIDTH + BORDER;
		    int rw = (factor2 - factor1) * CHARWIDTH;
		    // adjust coords to draw only in visible area
		    if (rx < BORDER) {
			rw -= (BORDER - rx);
			rx = BORDER;
		    }
		    if (rx + rw > textSpace.width)
			rw = textSpace.width - rx;
		    g.setXORMode(Color.black);
		    g.fillRect(rx, y, rw - 1, CHARHEIGHT);
		    g.setPaintMode();
		}
		if (endSelect)
		    selecting = false;
	    }
	    y += TEXTBORDER + CHARHEIGHT;
	    // don't forget to count '\n'!
	    count += s.length() + 1;
	}
	// draw scroll bars
	paintScrollbars(g, pressed, disabled);
    }

    /** Paints the used scroll bars. */
    private void paintScrollbars(Graphics g, boolean pressed, boolean disabled) {
	// do scrollbar specific drawing
	if (rightBar != null) {
	    rightBar.setScrollArea(textSpace.width, 0, width - textSpace.width, textSpace.height);
	    rightBar.paintScrollbar(g.create(), pressed, disabled);
	}
	if (lowerBar != null) {
	    lowerBar.setScrollArea(0, textSpace.height, textSpace.width, height - textSpace.height);
	    lowerBar.paintScrollbar(g.create(), pressed, disabled);
	}
    }
}
