package test.jx.awt;

/**
 * @(#)MArea.java    0.1  23.06.02
 *
 * Copyright (c) 2002 Marco Winter
 * Ellenbacher Str. 49
 * 91217 Hersbruck
 */


import java.awt.*;


/**
 * This class provides the main game area that contains all the
 * bomb fields on it.
 */
class MArea
    extends Panel {

    /* There are three predefined settings for the game area, */
    /* novice, normal, expert.                                */
    public final static int LEVELNOVICE = 1;
    public final static int LEVELNORMAL = 2;
    public final static int LEVELEXPERT = 3;
    
    /* The size of the game area */
    private int areaX;
    private int areaY;
    /* The actual game area */
    private MField area[][];
    /* Pointer to the parent MSweep class */
    /* for calling some layout methods       */
    private MSweep parent;
    /* some controlling information */
    /* about the actual game        */
    private int numMines;
    private int remainingMines;
    private int remainingFields;
    private boolean gameStarted;
    /* The initial status of the marker. The marker */
    /* (normally a '?') is used on the area to mark */
    /* unknown fields.                              */
    private boolean markerStatus = true;

    private int seed = 34153;
   

    /**
     * This contructor simply registers the parent class and
     * calls the constructor from this mother class.
     */
    public MArea(MSweep parent) {
	super();
	this.parent = parent;
	area = null;
    }




    /**
     * (Re)sets all bomb fields on the area. This method is called
     * when an new game is started OR the existing game layout
     * has been altered, so that the window is correctly repainted.
     */
    private void setAreaEntries(boolean newEntries) {
	removeAll();
	for (int y = 0; y < areaY; y++)
	    for (int x = 0; x < areaX; x++) {
		if (newEntries) {
		    area[x][y] = new MField();
		}
		MAreaMouseListener ml = new MAreaMouseListener(this, area[x][y]);
		area[x][y].setMouseListener(ml);
		add(area[x][y]);
	    }
    }

    public Point getAreaEntry(MField m) {
	for (int x = 0; x < areaX; x++)
	    for (int y = 0; y < areaY; y++)
		if (area[x][y] == m)
		    return new Point(x, y);
	return null;
    }

    /**
     * Resets the game values. Used when you press the reset button.
     */
    public void setValues() {
	buildArea(areaX, areaY, numMines);
    }

    /**
     * Sets new game values. Used when you change to the "custom" level.
     */
    public void setValues(int sizeX, int sizeY, int numMines) {
	buildArea(sizeX, sizeY, numMines);
    }

    /**
     * Sets new game values. Used when you change to a predefined level.
     * There are three predefined game levels:
     * NOVICE - 8x8, 10 mines
     * NORMAL - 16x16, 40 mines
     * EXPERT - 30x16, 99 mines
     */
    public void setValues(int mode) {
	switch (mode) {
	case LEVELNOVICE:
	    buildArea(8, 8, 10);
	    break;
	case LEVELNORMAL:
	    buildArea(16, 16, 40);
	    break;
	case LEVELEXPERT:
	    buildArea(30, 16, 99);
	    break;
	default:
	    // Used when an invalid game size is specified
	    buildArea(4, 4, 2);
	}
    }

    /**
     * The method called by all three setValues() methods. It does
     * all layout for a new game area, but no bomb placement! This
     * is done right after the first click, in method placeAllMines().
     */
    private void buildArea(int sizeX, int sizeY, int mines) {

	/* Inform parent that game is set to normal status */
	parent.setGameStatus(parent.GAME_NORMAL);
	/* create new area */
	area = new MField[sizeX][sizeY];
	areaX = sizeX;
	areaY = sizeY;
	numMines = mines;
	/* set all entries in the area */
	setLayout(new GridLayout(areaY, areaX));
	setAreaEntries(true);
	/* set all game values */
	remainingMines = numMines;
	remainingFields = areaX * areaY;
	gameStarted = false;
	/* set all displays */
	parent.changeMarkedMines(numMines);
	/* finally, show the new area on the screen */
	repaint();
    }

    private int random(int uBorder) {
	jx.zero.Naming naming = jx.zero.InitialNaming.getInitialNaming();
	jx.zero.Clock clock = (jx.zero.Clock) naming.lookup("Clock");
	if (clock == null)
	    System.out.println("Clock portal not found!");
	int newSeed = clock.getTimeInMillis();
	if (newSeed % 256 == 0)
	    seed = (seed + 1) % 0xFFFF;
	seed += newSeed;
	//System.out.println(seed);
	return (seed % uBorder);
    }
    
    /**
     * Sets alls mines in the area. Should only be called once
     * before the game starts. (Should be called right before
     * startCounter(), and should NOT change <gameStarted>,
     * which is still used by startCounter()!)
     * The coords (nx, ny) specify the first click coords per-
     * formed. Remember that the first field selected should
     * never cover a bomb!
     */
    public void placeAllMines(int nx, int ny) {
	int mines;

	/* Check if game has not yet been started */
	if (gameStarted)
	    return;
	gameStarted = true;
	/* Place all mines in the area */
	for (int i = 0; i < numMines; i++) {
	    int x, y;
	    do {
		x = random(areaX);
		y = random(areaY);
		// do NOT place a new bomb over a existing one
		// or over the (nx, ny) coords!
	    } while ((area[x][y].getMinestatus()) ||
		     ((x == nx) && (y == ny)));
	    area[x][y].setMinestatus(true);
	    System.out.println("Bomb " + i + ": " + x + "," + y);
	}
	/* Set up all bombfields (this is done to avoid long-time */
	/* calculations during the game). Each field learns how   */
	/* many mines are placed in the neighourhood.             */
	for (int x = 0; x < areaX; x++)
	    for (int y = 0; y < areaY; y++) {
		mines = 0;
		for (int ix = x - 1; ix <= x + 1; ix++)
		    for (int iy = y - 1; iy <= y + 1; iy++)
			if ((ix >= 0) && (ix < areaX) &&
			    (iy >= 0) && (iy < areaY))
			    if (area[ix][iy].getMinestatus())
				mines++;
		area[x][y].setSurroundingMines(mines);
	    }
	/* Shows field settings (for debug) */
 	for (int y = 0; y < areaY; y++) {
	    for (int x = 0; x < areaX; x++)
		if (area[x][y].getMinestatus())
		    System.out.print("*");
		else
		    System.out.print(area[x][y].getSurroundingMines());
	    System.out.println("");
	}
    }
    
    /**
     * Sets the marker status. The marker is used in the game
     * to mark fields width unknown content.
     */
    public void setMarker(boolean newStatus) {
	markerStatus = newStatus;
    }
    
    /**
     * Stops the current game with the current game status. Used
     * when you won or lost. <gameStatus> might either be GAME_WON
     * or GAME_LOST.
     */
    private void stopCurrentGame(int gameStatus) {
	/* Delete all listeners */
	for (int ix = 0; ix < areaX; ix++)
	    for (int iy = 0; iy < areaY; iy++)
		area[ix][iy].deleteMouseListener();
	parent.setGameStatus(gameStatus);
	gameStarted = false;
    }

    /**
     * Checks whether the condition for the game goal has been
     * reached.
     */
    public void checkForEnd() {
	/* The "win" condition is simple: If there are as many covered  */
	/* fields on the area as there are hidden mines, you have found */
	/* all non-bomb fields, and done!                               */
	/* This check should be done after every doAreaUpdate()...      */
	if (remainingFields == numMines) {
	    stopCurrentGame(parent.GAME_WON);
	    System.out.println("You've won!");
	}
    }
    
    /**
     * Called by the mouse listener to change the field status of the
     * selected field. The status changes whenever you do a right-click
     * on a field, and the field is still covered.
     */
    public void toggleStatus(int x, int y) {
	/* We have to check whether the coordinates are in */
	/* the proper range                                */
	if ((x < 0) || (x >= areaX) || (y < 0) || (y >= areaY)) 
	    return;
	
	int status = area[x][y].getStatus();
	
	switch (status) {
	case MField.PLACEHIDDEN:
	    // When a field is marked, the current amount of mines has
	    // to be decreased (needs not to be true... :-))
	    area[x][y].setStatus(MField.PLACEMARKED);
	    remainingMines--;
	    parent.changeMarkedMines(remainingMines);
	    break;
	case MField.PLACEMARKED:
	    /* If the marker status is set, the field gets the */
	    /* "unknown" state, otherwise it becomes hidden.   */
	    if (markerStatus)
		area[x][y].setStatus(MField.PLACEUNKNOWN);
	    else
		area[x][y].setStatus(MField.PLACEHIDDEN);
	    // When it is unmarked, the amount of mines is increased
	    remainingMines++;
	    parent.changeMarkedMines(remainingMines);
	    break;
	case MField.PLACEUNKNOWN:
	    area[x][y].setStatus(MField.PLACEHIDDEN);
	    break;
	default:
	    break;
	}
	/* Repaint current field to show changes */
	area[x][y].repaint();
    }

    /**
     * Handles all situations that may occur when the player presses the
     * left button on a field.
     */
    public void doAreaUpdate(int x, int y) {
	/* We have to check whether the coordinates are in */
	/* the proper range                                */
	if ((x < 0) || (x >= areaX) || (y < 0) || (y >= areaY))
	    return;
	/* All following actions depend on the current field status */
	int status = area[x][y].getStatus();
	/* We can't do anything with an uncovered field! */
	if (status != MField.PLACEHIDDEN)
	    return;
	/* Check whether we've found a bomb or not */
	if (area[x][y].getMinestatus()) {
	    /* Bomb found! Stop current game, signal to parent   */
	    /* class that the game has been lost, and reveal all */
	    /* mines on the game area.                           */
	    stopCurrentGame(parent.GAME_LOST);
	    for (int ix = 0; ix < areaX; ix++)
		for (int iy = 0; iy < areaY; iy++) {
		    if (area[ix][iy].getMinestatus()) {
			area[ix][iy].setStatus(MField.PLACEUNCOVERED);
		    }
		    area[ix][iy].repaint();
		}
	    System.out.println("Found bomb!");
	} else {
	    /* No bomb found. This is the most interesting part  */
	    /* in the whole game! We have to check recursively   */
	    /* all neighbour fields. If no one of them contains  */
	    /* a bomb, all neighbours are revealed and checked   */
	    /* themselves, etc. If at least one neighbour hides  */
	    /* a bomb, we just reveal the current field and stop */
	    /* looking further.                                  */
	    /* First, we have to uncover the current field and   */
	    /* fix the <remainingfields> value (must be done     */
	    /* here to avoid an infinite loop!)                  */
	    area[x][y].setStatus(MField.PLACEUNCOVERED);
	    area[x][y].repaint();
	    /* Counter for covered fields must be updated */
	    remainingFields--;
	    /* Only if no bomb is found in the neighbourhood, */
	    /* all neighbours are uncovered themselves.       */
	    if (area[x][y].getSurroundingMines() == 0)
		for (int ix = x - 1; ix <= x + 1; ix++)
		    for (int iy = y - 1; iy <= y + 1; iy++)
			doAreaUpdate(ix, iy);
	}
    }
}
