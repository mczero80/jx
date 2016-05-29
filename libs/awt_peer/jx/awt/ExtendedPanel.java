package jx.awt;

import java.awt.*;



/**
 * This class provides an extended panel. Unlike a normal
 * AWT panel, this component is able to draw a certain
 * decoration with a title string around it.
 */
public class ExtendedPanel
    extends Panel {
    // As with ExtendedLabel, this class only
    // says the JXPanelPeer what to do by just
    // being an ExtendedPanel

    /** No decoration */
    public static final int BORDER_NONE = 0;
    /** A simple line around the panel */
    public static final int BORDER_LINE = 1;
    /** A raised effect */
    public static final int BORDER_RAISED = 2;
    /** A lowered effect */
    public static final int BORDER_LOWERED = 3;
    /** An etched effect */
    public static final int BORDER_ETCHED = 4;


    /** The border type which must be one of the constants above */
    private int borderType;
    /** The used title string */
    private String title;


    /**
     * Creates a new panel with no decoration and no title. Note that
     * this panel, as all its derivates, has its insets set to
     * (5, 5, 5, 5), so you always have some room in an extended panel.
     */
    public ExtendedPanel() {
	this(BORDER_NONE, null);
    }

    /**
     * Creates an extended panel with the selected border type and the
     * selected title. If title is null, no title is used.
     */
    public ExtendedPanel(int borderType, String title) {
	super();
	this.borderType = borderType;
	this.title = title;
    }

    /**
     * Creates an extended panel with the selected LayoutManager that has
     * no decorations and no title.
     */
    public ExtendedPanel(LayoutManager l) {
	this(l, BORDER_NONE, null);
    }

    /**
     * Creates an extended panel with the selected LayoutManager, the
     * selected border type and the selected title. If title is null,
     * no title is used.
     */
    public ExtendedPanel(LayoutManager l, int borderType, String title) {
	super(l);
	this.borderType = borderType;
	this.title = title;
    }




    /**
     * Gets the current border type.
     */
    public int getBorderType() {
	return borderType;
    }

    /**
     * Gets the current title.
     */
    public String getTitle() {
	return title;
    }
    
    /**
     * Sets the current border type. borderType must be one of
     * the constants above.
     */
    public void setBorderType(int borderType) {
	this.borderType = borderType;
    }
    
    /**
     * Sets the current title. If title is null, no title
     * is used.
     */
    public void setTitle(String title) {
	this.title = title;
    }
    
}
