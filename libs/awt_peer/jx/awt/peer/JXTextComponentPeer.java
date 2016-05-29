package jx.awt.peer;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.TextEvent;
import java.awt.peer.TextComponentPeer;
import jx.awt.*;
import jx.wm.Keycode;
import java.util.*;


/**
 * This is the abstract superclass for all TextComponent peers.
 */
public abstract class JXTextComponentPeer
    extends JXComponentPeer
    implements TextComponentPeer {


    /** A string containing the "new line" character */
    protected final String newline = System.getProperty("line.separator");
    /** The border around the text line(s) */
    protected final int BORDER = 4;
    /** The space between the text lines */
    protected final int TEXTBORDER = 1;

    /** The buffer containing the whole text */
    protected StringBuffer text;
    /** The start of a selection */
    protected int selectionStart;
    /** The end of a selection */
    protected int selectionEnd;
    /** The current caret position */
    protected int caretPosition;
    /** The number of rows used */
    protected int rows;
    /** The number of columns used */
    protected int columns;
    /** The x scroll offset of the text line(s) */
    protected int offsetX;
    /** The y scroll offset of the text line(s) */
    protected int offsetY;
    /** The area containing the visible text line(s) */
    protected Rectangle textSpace;
    /** The flag indicating whether the text is editable or not */
    protected boolean editable;
    
    

    public JXTextComponentPeer(JXToolkit toolkit, TextComponent parent) {
	super(toolkit, parent);
	
	setText(parent.getText());
	setCaretPosition(parent.getCaretPosition());
	select(parent.getSelectionStart(),
	       parent.getSelectionEnd());
	setEditable(parent.isEditable());
    }
    
    
    
    /**
     * This method is called when the text buffer is altered. Every
     * subclass must implement this method to be able to react to
     * such events.
     */
    protected abstract void handleTextAltered();

    /** Make every TextComponent traversable by keyboard focus */
    public boolean isFocusTraversable() {
	return true;
    }

    /** Gets the default dimensions of the visible text area. */
    protected Rectangle initialTextSpace() {
	return new Rectangle(0, 0,
			     2 * BORDER + columnsToWidth(columns),
			     2 * BORDER + rowsToHeight(rows));
    }

    /** Gets the lower value of the selection markers. */
    protected int getLowerSelection() {
	return (selectionStart < selectionEnd) ? selectionStart : selectionEnd;
    }

    /** Gets the higher value of the selection markers. */
    protected int getUpperSelection() {
	return (selectionStart > selectionEnd) ? selectionStart : selectionEnd;
    }

    /** Returns the width in pixels of the string. */
    protected int textWidth(String s) {
	return columnsToWidth(s.length());
    }

    /** Returns the width in pixels of the desired number of columns. */
    protected int columnsToWidth(int columns) {
	return columns * CHARWIDTH;
    }

    /** Returns the height in pixels of the desired number of rows. */
    protected int rowsToHeight(int rows) {
	return rows * CHARHEIGHT + (rows - 1) * TEXTBORDER;
    }
    
    /** Returns the number of rows to the height in pixels. */
    protected int heightToRows(int height) {
	return (height + TEXTBORDER) / (CHARHEIGHT + TEXTBORDER);
    }

    /**
     * Gets the position in the text that currently lies under (x, y). The coords
     * must be relative to (0, 0), not relative to the offset!
     */
    protected int getTextPosition(int x, int y) {
	String s = "";
	int mx = 0;
	int my = 0;
	int count = 0;
	StringTokenizer st = new StringTokenizer(text.toString(), newline);
	// find row
	while (st.hasMoreTokens() && my <= y) {
	    s = st.nextToken();
	    my += TEXTBORDER + CHARHEIGHT;
	    count += s.length() + 1;
	}
	count -= s.length() + 1;
	// find column
	while (mx <= x && mx <= textWidth(s)) {
	    mx += CHARWIDTH;
	    count++;
	}
	if (mx > 0)
	    count--;

	return count;
    }

    /**
     * Converts an int value to its corresponding char value. This method is
     * used to get the visible character from a Keycode int value.
     */
    protected char intToChar(int value) {
	return (char) value;
    }

    /**
     * Gets the position of the start of the line where the caret
     * currently is.
     */
    protected int getLineStart() {
	int i = caretPosition;
	while (i > 0 && !newline.equals(String.valueOf(text.charAt(i - 1))))
	    i--;
	return (i >= 0) ? i : 0;;
    }

    /**
     * Gets the position of the end of the line where the caret
     * currently is.
     */
    protected int getLineEnd() {
	int i = caretPosition;
	while (i < text.length() && !newline.equals(String.valueOf(text.charAt(i))))
	    i++;
	return (i < text.length()) ? i : text.length();
    }

    /**
     * Gets the index of the position that is "rows" rows above
     * the current position.
     */
    protected int getUpPosition(int rows) {
	// make backup of current position
	int backup = caretPosition;
	int column = caretPosition - getLineStart();
	// find valid row
	caretPosition = getLineStart();
	for (int i = 0; i < rows; i++) {
	    if (caretPosition > 0)
		caretPosition--;
	    caretPosition = getLineStart();
	}
	// check if new line is shorter and adjust value
	if (column > getLineEnd() - getLineStart())
	    column = getLineEnd() - getLineStart();
	// set final position
	caretPosition += column;
	int j = caretPosition;
	// restore backup
	caretPosition = backup;
	return j;
    }

    /**
     * Gets the index of the position that is "rows" rows below
     * the current position.
     */
    protected int getDownPosition(int rows) {
	// make backup of current position
	int backup = caretPosition;
	int column = caretPosition - getLineStart();
	// find valid row
	for (int i = 0; i < rows; i++) {
	    caretPosition = getLineEnd();
	    if (caretPosition < text.length())
		caretPosition++;
	}
	// check if new line is shorter and adjust value
	if (column > getLineEnd() - getLineStart())
	    column = getLineEnd() - getLineStart();
	// set final position
	caretPosition += column;
	//if (caretPosition > text.length())
	//  caretPosition = text.length();
	int j = caretPosition;
	// restore backup
	caretPosition = backup;
	return j;
    }

    /**
     * Performs all text editing. Special keys like UP, DOWN, LEFT, RIGHT
     * move the caret, HOME moves to the start of the line, END moves to
     * the end of the line, BACKSPACE deletes one character, etc.
     * You can hold down the shift key to set a selection, on which all
     * text manipulations will be performed.
     */
    protected void performTextEditing(int keyValue, int modifier, boolean moreLines) {
	// if a special key is pressed, do not evaluate
	if (keyValue == 0)
	    return;
	boolean deleteSelection = true;
	boolean specialKey = true;
	switch (keyValue) {
	    // non-modifying keys
	case Keycode.VK_TAB:
	    break;
	case Keycode.VK_RIGHT_ARROW:
	    caretPosition = (caretPosition < text.length()) ? (caretPosition + 1) : text.length();
	    deleteSelection = !KeyMap.shiftPressed(modifier);
	    break;
	case Keycode.VK_LEFT_ARROW:
	    caretPosition = (caretPosition > 0) ? (caretPosition - 1) : 0;
	    deleteSelection = !KeyMap.shiftPressed(modifier);
	    break;
	case Keycode.VK_UP_ARROW:
	    if (moreLines) {
		caretPosition = getUpPosition(1);
		deleteSelection = !KeyMap.shiftPressed(modifier);
	    }
	    break;
	case Keycode.VK_DOWN_ARROW:
	    if (moreLines) {
		caretPosition = getDownPosition(1);
		deleteSelection = !KeyMap.shiftPressed(modifier);
	    }
	    break;
	case Keycode.VK_HOME:
	    caretPosition = getLineStart();
	    deleteSelection = !KeyMap.shiftPressed(modifier);
	    break;
	case Keycode.VK_END:
	    caretPosition = getLineEnd();
	    deleteSelection = !KeyMap.shiftPressed(modifier);
	    break;
	case Keycode.VK_PAGE_UP:
	    if (moreLines) {
		caretPosition = getUpPosition(heightToRows(textSpace.height));
		deleteSelection = !KeyMap.shiftPressed(modifier);
	    }
	    break;
	case Keycode.VK_PAGE_DOWN:
	    if (moreLines) {
		caretPosition = getDownPosition(heightToRows(textSpace.height));
		deleteSelection = !KeyMap.shiftPressed(modifier);
	    }
	    break;
	    // modifying keys
	case Keycode.VK_BACKSPACE:
	    if (editable) {
		if (selectionStart != selectionEnd) {
		    // delete selection
		    text.delete(getLowerSelection(), getUpperSelection());
		    caretPosition = getLowerSelection();
		} else
		    if (caretPosition > 0) {
			text.delete(caretPosition - 1, caretPosition);
			caretPosition--;
		    }
		handleTextAltered();
		sendTextEvent();
	    }
	    break;
	case Keycode.VK_DELETE:
	    if (editable) {
		if (selectionStart != selectionEnd) {
		    // delete selection
		    text.delete(getLowerSelection(), getUpperSelection());
		    caretPosition = getLowerSelection();
		} else
		    if (caretPosition < text.length())
			text.delete(caretPosition, caretPosition + 1);
		handleTextAltered();
		sendTextEvent();
	    }
	    break;
	case Keycode.VK_ENTER:
	    // if called from TextField, fire ActionEvent
	    if (!moreLines) {
		if (editable) {
		    queue = toolkit.getSystemEventQueue();
		    queue.postEvent(new ActionEvent(parent, ActionEvent.ACTION_PERFORMED,
						    text.toString(), 0));
		}
		break;
	    }
	    // if enter is pressed in TextArea, the key is forwarded
	    // to the "other keys" handler
	default:
	    // no key that is handled above, so assume a
	    // printable character and pass it to the text
	    specialKey = false;
	}

	if (!specialKey) {
	    if (caretPosition < 0 || caretPosition > text.length())
		return;
	    if (editable) {
		// delete selected text
		if (selectionStart != selectionEnd)
		    text.delete(getLowerSelection(), getUpperSelection());
		// insert new character into text
		text.insert(caretPosition, intToChar(keyValue));
		caretPosition++;
		handleTextAltered();
		sendTextEvent();
	    }
	}

	if (deleteSelection) {
	    selectionStart = caretPosition;
	    selectionEnd = caretPosition;
	} else
	    selectionEnd = caretPosition;
    }

    /** Send a text event. */
    protected void sendTextEvent() {
	queue = toolkit.getSystemEventQueue();
	queue.postEvent(new TextEvent(parent, TextEvent.TEXT_VALUE_CHANGED));
    }

    public void dispose() {
	text = null;
	textSpace = null;
	super.dispose();
    }

    /*********************************************
     * methods implemented from TextComponentPeer *
     *********************************************/

    public int getSelectionEnd() {
	return selectionEnd;
    }

    public int getSelectionStart() {
	return selectionStart;
    }

    public void select(int start_pos, int end_pos) {
	selectionStart = start_pos;
	selectionEnd = end_pos;
	caretPosition = end_pos;
	redraw();
    }

    public String getText() {
	return text.toString();
    }

    public void setText(String text) {
	this.text = new StringBuffer(text);
	if (caretPosition > text.length())
	    caretPosition = text.length();
	handleTextAltered();
	redraw();
	sendTextEvent();
    }
    
    public void setEditable(boolean editable) {
	this.editable = editable;
	redraw();
    }
    
    public int getCaretPosition() {
	return caretPosition;
    }
    
    public void setCaretPosition(int pos) {
	caretPosition = pos;
	redraw();
    }
    
    /****************************** DRAWING ******************************/

    /** Paints the caret. */
    public void paintCaret(JXGraphics g, int x, int y) {
	g.setColor(JXColors.caretColor);
	g.drawRect(x, y, 1, CHARHEIGHT);
    }
}
