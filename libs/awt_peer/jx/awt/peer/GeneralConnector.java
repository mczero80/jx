package jx.awt.peer;


import java.awt.*;
import jx.wm.*;
import jx.devices.fb.*;
import jx.awt.*;


/**
 * This is the superclass of all WWindow derived classes used in the JX AWT
 * implementation. It is the only class that directly extends WWindow.
 */
public abstract class GeneralConnector 
    extends WWindow {

    /** A reference to the global JXToolkit instance */
    protected JXToolkit toolkit;
    /** A reference to the JXGraphics object used to draw */
    protected JXGraphics graphics;
    
    /** Used to avoid back coupling in the setBounds() method */
    protected boolean connectorEnabled = true;
    /** The x coord of the component area's origin */
    protected int componentOffsetX;
    /** The y coord of the component area's origin */
    protected int componentOffsetY;
    /**
     * Counter used to count the actual depth of the redraw()
     * recursion.
     */
    protected int bufferCount = 0;
    
    
    
    /** Creates a new GeneralConnector instance */
    public GeneralConnector(JXToolkit toolkit, String title,
			    PixelRect size, WindowFlags flags) {
	// call WWindow constructor
	super(title, size, flags);
	// toolkit could be null if a new Connector has only been
	// instanciated to find out the screen's dimensions
	if (toolkit != null) {
	    this.toolkit = toolkit;
	    graphics = new JXGraphics(this);
	    //enableBackBuffer(true);
	}
    }







    /**
     * Helper method for getLocationRelativeToComponent() from JXGraphics. Adjusts the coords
     * to fit the component area's offset.
     */
    public Point getLocationOnComponents(int x, int y) {
	return new Point(x + componentOffsetX, y + componentOffsetY);
    }
    
    /**
     * Helper method for getLocationRelativeToWindow() from JXGraphics. Adjusts the coords
     * to fit the component area's offset.
     */
    public Point getLocationOnHost(int x, int y) {
	return new Point(x - componentOffsetX, y - componentOffsetY);
    }
    
    /**
     * Sets the bounds of the window. This method normally is called from the user level,
     * so the size must be adjusted considering the component area's offset.
     */
    public void setBounds(int x, int y, int width, int height) {
	// Perform action only if allowed...
	if (connectorEnabled)
	    setFrame(new PixelRect(x, y,
				   x + width + componentOffsetX - 1,
				   y + height + componentOffsetY - 1));
	//resetBackBuffer();
    }
    
    /**
     * Gets the bounds of the window. This method normally is called from the user level,
     * so the size must be adjusted considering the component area's offset.
     */
    public Rectangle getBounds() {
	PixelRect r = getFrame();
	return new Rectangle(r.x0(), r.y0(),
			     r.width() - componentOffsetX + 1,
			     r.height() - componentOffsetY + 1);
    }

    /**
     * Returns the location of (componentOffsetX, componentOffsetY), which is the origin for
     * all Component derived classes, on the screen
     */
    public Point getComponentAreaOrigin() {
	Point p = getWindowOrigin();
	p.x += componentOffsetX;
	p.y += componentOffsetY;
	return p;
    }

    /**
     * Returns the location of the origin of this window.
     */
    public Point getWindowOrigin() {
	PixelRect p = getFrame();
	return new Point(p.x0(), p.y0());
    }

    /** Actually shows the window on the screen and, if set visible, moves it to front. */
    public void show(boolean visible) {
	super.show(visible);
	if (visible)
	    moveToFront();
    }

    public void dispose() {
	graphics.dispose();
	graphics = null;
	show(false);
	quit();
    }

    /** Increases the bufferCount variable. */
    public void setBufferCount() {
	bufferCount++;
    }

    /** Decreases the bufferCount variable and, if zero, paints the back buffer. */
    public void drawBackBuffer() {
	bufferCount--;
	//if (bufferCount == 0)
	//  super.drawBackBuffer();
    }
}
