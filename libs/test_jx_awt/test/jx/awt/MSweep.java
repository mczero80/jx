package test.jx.awt;

/**
 * @(#)MSweep.java    0.1  23.06.02
 *
 * Copyright (c) 2002 Marco Winter
 * Ellenbacher Str. 49
 * 91217 Hersbruck
 */


import java.awt.*;
import java.awt.event.*;


/**
 * The main class for the game "MSweep", a simple clone
 * of "BombSweep"
 */
public class MSweep
    extends Frame {
    
    /* Some constants defining the actual */
    /* game status                        */
    public final static int GAME_NORMAL = 0;
    public final static int GAME_LOST = 1;
    public final static int GAME_WON = 2;
    /* Version String */
    private final String frameString = "MSweep V0.1";
    /* Some classes for the layout */
    private MSweep mainFrame;
    private MenuBar menuBar;
    private Panel gamePanel;
    private Button resetButton;
    private Label numOfMines;
    /* Reference to the game area */
    private MArea gameArea;
    /* all menuItems */    
    private MenuItem restartArea;
    private MenuItem endProgram;
    private MenuItem setLevelNovice;
    private MenuItem setLevelNormal;
    private MenuItem setLevelExpert;


    
    /*    private void checkTree(Container c, int offset) {
	  for (int j = 0; j < offset; j++)
	  System.out.print(" ");
	  System.out.println("Container " + c.getClass().getName());
	  
	  for (int i = 0; i < c.getComponentCount(); i++) {
	  Component comp = c.getComponent(i);
	  if (comp instanceof Container)
	  checkTree((Container) comp, offset + 2);
	  else {
	  for (int j = 0; j < offset + 2; j++)
	  System.out.print(" ");
	  System.out.println(comp.getClass().getName());
	  }
	  }
	  }*/

    /**
     * Redraws the window. This method is used after some information
     * showed on the screen has been altered.
     */
    private void updateWindow() {
	//System.out.println("Aktuelle Hierarchie:");
	//checkTree(this, 0);

	// Use setResizable() here to avoid some strange effects on some
	// platforms, like the frame being packed extremely small!
	setResizable(true);
	pack();
	setResizable(false);
	setVisible(true);
    }
    
    /**
     * Shows the number of marked bombs.
     */
    public void changeMarkedMines(int bombs) {
	numOfMines.setText(String.valueOf(bombs));
    }
    
    /**
     * Sets the reset button image according to the actual
     * game status. GAME_NORMAL is used during normal
     * gameplay, GAME_LOST to indicate a lost game, and
     * GAME_WON indicates a won game.
     */
    public void setGameStatus(int status) {
	switch (status) {
	case GAME_NORMAL:
	    resetButton.setLabel("reset");
	    break;
	case GAME_LOST:
	    resetButton.setLabel("lost!");
	    break;
	case GAME_WON:
	    resetButton.setLabel("WON!");
	    break;
	default:
	    break;
	}
	resetButton.repaint();
    }
    
    /**
     * Does all layout and painting for the main window. Is also
     * sets all action listeners.
     */
    private void doTopLayout() {

	/* Initializing all action and window listeners */
	restartArea = new MenuItem("New game");
	ActionListener restartListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    /* Reset actual game settings */
		    gameArea.setValues();
		    updateWindow();
		}
	    };
	restartArea.addActionListener(restartListener);
 	endProgram = new MenuItem("Exit");
	endProgram.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    mainFrame.dispose();
		}
	    });
	setLevelNovice = new MenuItem("Anfaenger");
	setLevelNovice.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    gameArea.setValues(MArea.LEVELNOVICE);
		    updateWindow();
		}
	    });
	setLevelNormal = new MenuItem("Normal");
	setLevelNormal.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    gameArea.setValues(MArea.LEVELNORMAL);
		    updateWindow();
		}
	    });
	setLevelExpert = new MenuItem("Experte");
	setLevelExpert.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    gameArea.setValues(MArea.LEVELEXPERT);
		    updateWindow();
		}
	    });
	/* Adding a new menu bar and building all entries */
       	menuBar = new MenuBar();
	setMenuBar(menuBar);
	
	Menu menu1 = new Menu("Datei");
	Menu menu2 = new Menu("Level");
	menu1.add(restartArea);
	menu1.addSeparator();
	menu1.add(endProgram);
	menu2.add(setLevelNovice);
	menu2.add(setLevelNormal);
	menu2.add(setLevelExpert);
	menuBar.add(menu1);
	menuBar.add(menu2);
	
	/* Building all layout not concerning the menu bar, */
	/* like reset button, timer field, etc.             */
	resetButton = new Button();
	resetButton.addActionListener(restartListener);
	numOfMines = new Label("");

	/* Panels for the upper region */
	Panel panel = new Panel();
	Panel leftPanel = new Panel();
	Panel rightPanel = new Panel();
	leftPanel.setLayout(new BorderLayout());
	rightPanel.setLayout(new BorderLayout());
	leftPanel.add(new Label("Bombs"), BorderLayout.NORTH);
	leftPanel.add(numOfMines, BorderLayout.CENTER);
	rightPanel.add(new Label("Have fun!"), BorderLayout.NORTH);
	panel.setLayout(new FlowLayout());
	panel.add(leftPanel);
	panel.add(resetButton);
	panel.add(rightPanel);
	/* Panel for the center region */
	gamePanel = new Panel();
	/* Connecting all panels to the main window */
	add(panel, BorderLayout.NORTH);
	add(gamePanel, BorderLayout.CENTER);
    }
    
    /**
     * Does all startup initialization and painting.
     */
    public void doInit() {
	
	MField helper = new MField();
	/* Presetting some values */
	mainFrame = this;
	/* Doing all layout for the main window, especially */
	/* the upper part of it                             */
	setTitle(frameString);
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    dispose();
		}
	    });
	doTopLayout();
	/* Doing all settings concerning the game area */
	gameArea = new MArea(this);
	gameArea.setValues(MArea.LEVELNOVICE);
	gamePanel.add(gameArea, BorderLayout.CENTER);
	/* Some last settings and then show it! */
	setLocation(100, 100);
	updateWindow();
    }

    public static void main(String[] args) {
	MSweep m = new MSweep();
	m.doInit();
    }
}
