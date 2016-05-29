package jx.awt.peer;

import java.awt.*;
import java.awt.peer.TextFieldPeer;
import jx.awt.*;


/**
 * This class implements the TextField peer.
 */
public class JXTextFieldPeer
    extends JXTextComponentPeer
    implements TextFieldPeer {

    /** The "null" character */
    private final int NO_ECHO = '\u0000';

    private char echo;
    private String textString;


    /** Creates a new JXTextFieldPeer instance */
    public JXTextFieldPeer(TextField parent, JXToolkit toolkit) {
	super(toolkit, parent);
	columns = parent.getColumns();
	rows = 1;
	setEchoChar(parent.getEchoChar());
	offsetX = 0;
	offsetY = 0;
	textSpace = initialTextSpace();
	prefWidth = textSpace.width;
	prefHeight = textSpace.height;
	ready = true;
    }





    /** Paints the peer. */
    public void paint(JXGraphics g) {
	resetLayout();
	boolean focus = hasFocus();
	switch (peerState) {
	case PEER_NORMAL:paintTextField(g, focus, false, false, false);break;
	case PEER_PRESSED:paintTextField(g, focus, true, false, false);break;
	case PEER_HOVER:paintTextField(g, focus, false, true, false);break;
	case PEER_DISABLED:paintTextField(g, focus, false, false, true);break;
	}
    }
    
    /** Performs all text editing. */
    public void keyPressed(int keyValue, int rawValue, int modifier) {
	super.keyPressed(keyValue, rawValue, modifier);
	performTextEditing(keyValue, modifier, false);
	updateOffsets();
	redraw();
    }

    /**
     * Handles all mouse press events. The caret is set to the mouse's
     * current position.
     */
    public void mousePressed(int x, int y, int button) {
	super.mousePressed(x, y, button);
	Point p = getLocationRelativeToComponent(x - BORDER + offsetX, y - BORDER + offsetY);
	caretPosition = getTextPosition(p.x, p.y);
	selectionStart = caretPosition;
	selectionEnd = caretPosition;
	updateOffsets();
	redraw();
    }

    /**
     * Handles all mouse move events. If a button is pressed, then the
     * current selection is altered.
     */
    public void mouseMoved(int x, int y, int button) {
	super.mouseMoved(x, y, button);
	Point m = getLocationRelativeToComponent(x, y);
	if (button != 0) {
	    // update selection
	    Point p = getLocationRelativeToComponent(x - BORDER + offsetX, y - BORDER + offsetY);
	    int newpos = getTextPosition(p.x, p.y);
	    if (newpos != selectionEnd) {
		selectionEnd = newpos;
		caretPosition = selectionEnd;
		updateOffsets();
		redraw();
	    }
	}
    }
    
    /**
     * Creates an echo string of the desired length, filled with
     * the desired character.
     */
    private String createEchoString(char echoChar, int length) {
	char echoString[] = new char[length];
	for (int i = 0; i < length; i++)
	    echoString[i] = echoChar;
	return new String(echoString);
    }

    /** Updates the layout of the text field. */
    private void resetLayout() {
	textSpace.width = width;
	textSpace.height = height;
	columns = text.length();
    }

    /** Adjusts the offset if necessary. */
    private void updateOffsets() {
	// find caret coord
	x = caretPosition * CHARWIDTH + BORDER;
	// adjust offset coords
	if (offsetX > x - BORDER)
	    offsetX = x - BORDER;
	if (offsetX < x - (textSpace.width - 2 * BORDER - CHARWIDTH))
	    offsetX = x - (textSpace.width - 2 * BORDER - CHARWIDTH);
	if (offsetX < 0)
	    offsetX = 0;
    }

    /** Updates the visible string. */
    protected void handleTextAltered() {
	// create new string based on the echo state
	// and the text buffer
	textString = (echo == NO_ECHO) ? text.toString() : createEchoString(echo, text.length());
    }

    /*************************************************
     * methods implemented from TextFieldPeer         *
     *************************************************/

    public Dimension minimumSize(int len) {
	return new Dimension(textSpace.x + textSpace.width,
			     textSpace.y + textSpace.height);
    }

    public Dimension getMinimumSize(int len) {
	return minimumSize(len);
    }

    public Dimension preferredSize(int len) {
	return minimumSize(len);
    }

    public Dimension getPreferredSize(int len) {
	return preferredSize(len);
    }

    public void setEchoChar(char echo_char) {
	echo = echo_char;
	handleTextAltered();
    }

    public void setEchoCharacter(char echo_char) {
	setEchoChar(echo_char);
    }

    /************************ DRAWING ****************************/

    /** Paints the text field. */
    public void paintTextField(JXGraphics g, boolean focus, boolean pressed,
			       boolean hover, boolean disabled) {
	g.setColor(JXColors.normalBgColor);
	g.draw3DRect(0, 0, width - 1, height - 1, false);
	if (!disabled && editable)
	    g.setColor(JXColors.textBgColor);
	else
	    g.setColor(JXColors.disabledBgColor);
	g.fillRect(textSpace.x + 1, textSpace.y + 1, textSpace.width - 3, textSpace.height - 3);
	if (focus) {
	    g.setColor(JXColors.focusColor);
	    g.drawRect(textSpace.x + 1, textSpace.y + 1, textSpace.width - 3, textSpace.height - 3);
	}
	// draw text
	g.setClip(BORDER, BORDER, textSpace.width - 2 * BORDER, textSpace.height - 2 * BORDER);
	if (disabled)
	    g.setColor(JXColors.disabledTextColor);
	else
	    g.setColor(JXColors.textTextColor);
	g.drawJXString(textString, BORDER - offsetX, BORDER);
	// draw selection
	if (selectionStart != selectionEnd) {
	    int rx = getLowerSelection() * CHARWIDTH + BORDER - offsetX;
	    int rw = (getUpperSelection() - getLowerSelection()) * CHARWIDTH;
	    // adjust coords to draw only in visible area
	    if (rx < BORDER) {
		rw -= (BORDER - rx);
		rx = BORDER;
	    }
	    if (rx + rw > textSpace.width)
		rw = textSpace.width - rx;
	    g.setXORMode(Color.black);
	    g.fillRect(rx, BORDER, rw - 1, CHARHEIGHT);
	    g.setPaintMode();
	}
	// draw caret
	if (focus) {
	    int rx = caretPosition * CHARWIDTH + BORDER - offsetX;
	    paintCaret(g, rx, BORDER);
	}
    }
}
