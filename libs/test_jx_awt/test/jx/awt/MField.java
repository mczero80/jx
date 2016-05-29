package test.jx.awt;

/**
 * @(#)MField.java    0.1  23.06.02
 *
 * Copyright (c) 2002 Marco Winter
 * Ellenbacher Str. 49
 * 91217 Hersbruck
 */


import java.awt.*;
import java.awt.event.*;


/**
 * This class represents one field on the game area. It provides all
 * functions needed, inclusive an own paintComponent() method.
 */
class MField
    extends Canvas {

    /* There are four different main states, named HIDDEN, MARKED, */
    /* UNKNOWN and UNCOVERED.                                      */
    /* Each state has its own image associated with it, only the   */
    /* state UNCOVERED provides several images for the substates   */
    /* (number of surrounding mines, etc.).                        */
    public final static int PLACEHIDDEN = 10;
    public final static int PLACEMARKED = 11;
    public final static int PLACEUNKNOWN = 12;
    public final static int PLACEUNCOVERED = 13;
    /* The initial field size, can be altered in the menu */
    private final static int FIELDSIZE = 24;
    /* Some internal variables */
    private int status;
    private int surroundingMines;
    private boolean isMine;

    private MouseListener mouseListener;
    
    

    /**
     * The only constructor actually used. It sets all variables to
     * the most common standard values.
     */
    public MField() {
	setStatus(PLACEHIDDEN);
	setSurroundingMines(0);
	setMinestatus(false);
    }
    


    /**
     * Gets the current field state.
     */
    public int getStatus() {
	return status;
    }

    /**
     * Gets the number of surrounding mines.
     */
    public int getSurroundingMines() {
	return surroundingMines;
    }
    
    /**
     * Tells whether this field contains a bomb or not.
     */
    public boolean getMinestatus() {
	return isMine;
    }

    /**
     * Sets the field state.
     */
    public void setStatus(int status) {
	this.status = status;
    }
    
    /**
     * Sets the number of surrounding mines.
     */
    public void setSurroundingMines(int mines) {
	surroundingMines = mines;
    }
    
    /**
     * Sets whether this field shall contain a bomb or not.
     */
    public void setMinestatus(boolean isMine) {
	this.isMine = isMine;
    }

    public Dimension getPreferredSize() {
	super.getPreferredSize();
	return new Dimension(FIELDSIZE, FIELDSIZE);
    }

    public void setMouseListener(MouseListener m) {
	mouseListener = m;
	addMouseListener(m);
    }

    public void deleteMouseListener() {
	removeMouseListener(mouseListener);
	mouseListener = null;
    }

    /**
     * This method provides all painting for this field. Depending on
     * the current main state and the substate, the proper image is
     * requested from the image handler and the field is painted.
     */
    public void paint(Graphics g) {
	switch (status) {
	case PLACEMARKED:
	case PLACEUNKNOWN:
	case PLACEHIDDEN:
	    drawField(g, false, status);
	    break;
	case PLACEUNCOVERED:
	    if (isMine)
		drawField(g, true, -1);
	    else
		drawField(g, true, surroundingMines);
	    break;
	default:
	    // when no valid state is set, paint this "error" string
	    g.drawString("U!", 3, FIELDSIZE - 3);
	    break;
	}
    }

    /**
     * Used by paint() to paint a 3D box.
     */    
    private void drawField(Graphics g, boolean uncovered, int imgIndex) {
	int maxX = getWidth();
	int maxY = getHeight();

	/* Draw background */
	g.setColor(Color.lightGray);
	g.fillRect(0, 0, maxX - 1, maxY - 1);
	/* Depending on whether the field is uncovered or not, */
	/* the border lines are drawn in the proper manner.    */
    	if (uncovered) {
	    g.setColor(Color.black);
	    g.drawLine(0, 0, maxX - 1, 0);
	    g.drawLine(0, 0, 0, maxY - 1);
	} else {
	    g.setColor(Color.white);
	    g.drawLine(0, 0, maxX - 1, 0);
	    g.drawLine(1, 1, maxX - 2, 1);
	    g.drawLine(0, 0, 0, maxY - 1);
	    g.drawLine(1, 1, 1, maxY - 2);
	    g.setColor(Color.gray);
	    g.drawLine(1, maxY - 1, maxX - 1, maxY - 1);
	    g.drawLine(2, maxY - 2, maxX - 2, maxY - 2);
	    g.drawLine(maxX - 1, 1, maxX - 1, maxY - 1);
	    g.drawLine(maxX - 2, 2, maxX - 2, maxY - 2);
	}
	
	switch(imgIndex) {
	case PLACEMARKED:
	    g.setColor(Color.black);
	    g.drawLine(maxX / 2, 3, maxX / 2, maxY - 3);
	    g.setColor(Color.red);
	    g.drawLine(maxX / 2, 3, maxX - 3, 5);
	    g.drawLine(maxX / 2, 7, maxX - 3, 5);
	    break;
	case PLACEUNKNOWN:
	    g.setColor(Color.black);
	    g.drawString("?", 3, maxY - 3);
	    break;
	case PLACEHIDDEN:
	    break;
	case -1:
	    g.setColor(Color.black);
	    g.drawString("*", 3, maxY - 3);
	    break;
	case 0:
	    break;
	default:
	    switch (imgIndex) {
	    case 1:g.setColor(Color.blue);break;
	    case 2:g.setColor(Color.green);break;
	    case 3:g.setColor(Color.orange);break;
	    case 4:g.setColor(Color.red);break;
	    case 5:g.setColor(Color.pink);break;
	    case 6:g.setColor(Color.magenta);break;
	    case 7:g.setColor(Color.cyan);break;
	    case 8:g.setColor(Color.white);break;
	    default:g.setColor(Color.black);
	    }
	    g.drawString(String.valueOf(imgIndex), 3, maxY - 3);
	    break;
	}
    }
}
