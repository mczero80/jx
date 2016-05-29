package test.jx.awt;

/**
 * @(#)MAreaMouseListener.java    0.1  23.06.02
 *
 * Copyright (c) 2002 Marco Winter
 * Ellenbacher Str. 49
 * 91217 Hersbruck
 */


import java.awt.Point;
import java.awt.event.*;



/**
 * This class represents the mouse listener that is used to select a
 * certain field on the area. There is only one listener for the area
 * instead of one for each field.
 */
class MAreaMouseListener
    implements MouseListener {
    
    /* Offset used for calculating the exact coords of the */
    /* mouse pointer (remember, MArea has a border! )   */
    private final int offset = 0;
    /* Reference to the parent component to be watched */    
    private MArea parentArea;
    private MField parentField;
    
    
    

    /**
     * The only constructor.
     */
    public MAreaMouseListener(MArea parent, MField parent2) {
	parentArea = parent;
	parentField = parent2;
    }
    
    
    
    /**
     * This is the only method actually used from the MouseListener
     * interface. Depending on the position where the mouse button was
     * pressed, the index of the corresponding field is calculated,
     * and depending on which button was pressed, the according method
     * is called.
     */
    public void mousePressed(MouseEvent e) {
	/* Find out which button was pressed where */
	int buttons = e.getModifiers();
	/* Test whether the found coords are in the desired range */
	if ((e.getX() < offset) || (e.getY() < offset))
	    return;
	/* Calculate field index */
	Point p = parentArea.getAreaEntry(parentField);
	int x = p.x;
	int y = p.y;
	System.out.println("Mouse press occured at field " + x + "," + y);
	/* If one of the desired buttons (left or right) is pressed, */
	/* AND it is the first button press in the actual game (this */
	/* condition is tested in the two methods below), all mines  */
	/* are distributed on the area and the timer is started.     */
	if ((buttons & InputEvent.BUTTON2_MASK) != InputEvent.BUTTON2_MASK) {
	    // The coords of the button press are used to determine
	    // where NOT to place a bomb!
	    parentArea.placeAllMines(x, y);
	}
	/* If the right button is pressed, it is tried to change the */
	/* field's current state.                                    */
	if ((buttons & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
	    parentArea.toggleStatus(x, y);
	} else
	    /* If the left button is pressed, it is tried to uncover */
	    /* the corresponding field and do all consequent area    */
	    /* changes. Then it is checked whether we have reached   */
	    /* a winning position.                                   */
	    if ((buttons & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
		parentArea.doAreaUpdate(x, y);
		parentArea.checkForEnd();
	    }
    }
    
    /* All other methods below are not necessary for the listener! */
    public void mouseClicked(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
}
