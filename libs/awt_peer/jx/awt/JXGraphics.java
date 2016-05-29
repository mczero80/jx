package jx.awt;


import java.awt.*;
import java.awt.image.*;
import jx.awt.peer.*;
import jx.wm.*;
import jx.devices.fb.*;


/**
 * This class implements the Graphics class. It provides simple
 * drawing methods as well as some special JX related methods.
 */
public class JXGraphics
    extends Graphics {
    
    /** The baseline of the default font */
    private final int BASELINE = 10;

    /** The reference to the underlying window used to draw on */
    private GeneralConnector connector;
    
    private int transX, transY;
    private int clipX, clipY, clipWidth, clipHeight;
    private Color fgColor;
    /** The flag indicating whether drawing should occur normal or XORed */
    private boolean invert = false;
    /**
     * The "super" clip used to set a component-wide clip. It is
     * used by the JXScrollPane class.
     */
    private Rectangle viewClip;
    
    
    
    public JXGraphics(GeneralConnector connector) {
	this.connector = connector;
    }

    

    /*******************************************************
     * misc. methods                                        *
     *******************************************************/


    /**
     * Converts the coordinates (x,y) which are relative to the host window
     * to coords relative to the calling component.
     */
    public Point getLocationRelativeToComponent(int x, int y) {
	return connector.getLocationOnComponents(x - transX, y - transY);
    }

    /**
     * Converts the coordinates (x,y) which are relative to the calling component
     * to coords relative to the host window.
     */
    public Point getLocationRelativeToWindow(int x, int y) {
	return connector.getLocationOnHost(x + transX, y + transY);
    }

    /**
     * Returns a new Instance of a JXGraphics object.
     */
    public JXGraphics createJXGraphics() {
	JXGraphics g = new JXGraphics(connector);
	g.translate(transX, transY);
	return g;
    }

    /**
     * Increases the internal buffer counter. This is used in the recursive
     * painting algorithm for a window to avoid multiple buffer updates.
     */
    public void setBufferCount() {
	connector.setBufferCount();
    }

    /**
     * This method decreases the internal buffer counter and, if the counter
     * is zero, paints the back buffer to the screen.
     */
    public void drawBackBuffer() {
	connector.drawBackBuffer();
    }

    /**
     * Sets the "super" clip. This clip is used to specify the area in a component
     * to be drawn without the component itself knowing about the area. This is
     * usable for a ScrollPane where you need to specify which part of the component
     * should be shown on the screen.
     */
    public void setViewClip(int x, int y, int w, int h) {
	if (w == 0 || h == 0) {
	    // reset clip if new clip is invalid
	    viewClip = null;
	    return;
	}
	if (viewClip == null)
	    viewClip = new Rectangle(x, y, w, h);
	else {
	    // The new clip can only be a part of the old one!
	    Rectangle r = new Rectangle(x, y, w, h);
	    viewClip = viewClip.intersection(r);
	}
    }
    
    /**
     * Gets the current translation coords.
     */
    public Point getTranslationOrigin() {
	return new Point(transX, transY);
    }

    public void dispose() {
    }

    /**
     * Draws a string with the upper left corner at (x, y).
     */
    public void drawJXString(String string, int x, int y) {
	x += transX;
	y += transY;
	connector.movePenTo(x, y);
	connector.drawString(string);
    }

    /*******************************************************
     * methods implemented from Graphics                    *
     *******************************************************/


    public void copyArea(int x, int y, int width, int height, int dx, int dy) {}
    public void clearRect(int x, int y, int width, int height) {}
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {}
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {}
    public void fillOval(int x, int y, int width, int height) {}
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {}
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {}
    public void fillPolygon(int xPoints[], int yPoints[], int npoints) {}
    public void drawOval(int x, int y, int width, int height) {}

    public Graphics create() {
	return createJXGraphics();
    }
    
    public void translate(int x, int y) {
	//System.out.println("*** called translate() from JXGraphics with x=" +
	//x + ", y=" + y);
	transX += x;
	transY += y;
    }

    public void setPaintMode() {
	invert = false;
    }
    
    public void setXORMode(Color c) {
	invert = true;
	// The color information is NOT used!
    }

    public void clipRect(int x, int y, int width, int height) {
	setClip(x, y, width, height);
    }
    
    public void setClip(int x, int y, int width, int height) {
	clipX = x;
	clipY = y;
	clipWidth = width;
	clipHeight = height;
	if (viewClip != null) {
	    // if the super clip exits, then the new user clip is an
	    // intersection between the super clip and the user clip
	    Rectangle rold = new Rectangle(x + transX, y + transY, width, height);
	    Rectangle r = (rold.isEmpty()) ? viewClip : viewClip.intersection(rold);
	    //System.out.println("user clip is " + rold);
	    //System.out.println("clipping at " + r);
	    connector.setClip(r.x, r.y, r.width, r.height);
	} else
	    connector.setClip(x + transX, y + transY, width, height);
    }

    public void setClip(Shape clip) {
	Rectangle r = clip.getBounds();
	setClip(r.x, r.y, r.width, r.height);
    }

    public Shape getClip() {
	return getClipBounds();
    }

    public Rectangle getClipBounds() {
	return new Rectangle(clipX, clipY,
			     clipWidth, clipHeight);
    }

    public void setColor(Color color) {
	fgColor = color;
	connector.setFgColor(color.getRed(),
			     color.getGreen(),
			     color.getBlue(),
			     color.getAlpha());
    }

    public Color getColor() {
	return fgColor;
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
	x1 += transX;
	y1 += transY;
	x2 += transX;
	y2 += transY;
	connector.movePenTo(x1, y1);
	if (invert)
	    connector.invertLine(x2, y2);
	else
	    connector.drawLine(x2, y2);
    }

    public void fillRect(int x, int y, int width, int height) {
	/*System.out.println("*** called fillRect with x=" + x +
	  ", y=" + y + ", width=" + width +
	  ", height=" + height);*/
	x += transX;
	y += transY;
	connector.fillRect(new PixelRect(x, y, x + width, y + height),
			   ((invert) ? DrawingMode.INVERT : DrawingMode.COPY));
    }

    public void drawPolyline(int xPoints[], int yPoints[], int npoints) {
	if (npoints < 2)
	    return;
	for (int i = 0; i < npoints; i++) {
	    xPoints[i] += transX;
	    yPoints[i] += transY;
	}
	connector.movePenTo(xPoints[0], yPoints[0]);
	for (int i = 1; i < npoints; i++)
	    if (invert)
		connector.invertLine(xPoints[i], yPoints[i]);
	    else
		connector.drawLine(xPoints[i], yPoints[i]);
    }

    public void drawPolygon(int xPoints[], int yPoints[], int npoints) {
	drawPolyline(xPoints, yPoints, npoints);
    }

    public void drawString(String s, int x, int y) {
	drawJXString(s, x, y - BASELINE);
    }

    public boolean drawImage(Image image, int x, int y, ImageObserver observer) {
	x += transX;
	y += transY;
	if (invert)
	    connector.drawBitmap(((JXImage) image).getBitmap(), new PixelPoint(x, y),
				 DrawingMode.INVERT);
	else
	    connector.drawBitmap(((JXImage) image).getBitmap(), x, y);
	return true;
    }

    public boolean drawImage(Image image, int x, int y, 
			     int width, int height, ImageObserver observer) {
	x += transX;
	y += transY;
	PixelRect src = new PixelRect(0, 0, image.getWidth(observer) - 1, image.getHeight(observer) - 1);
	PixelRect dst = new PixelRect(x, y, x + width - 1, y + height - 1);
	connector.drawBitmap(((JXImage) image).getBitmap(), src, dst,
			     ((invert) ? DrawingMode.INVERT : DrawingMode.SCALEDCOPY));
	return true;
    }
    

    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1,
			     int sx2, int sy2, ImageObserver observer) {
	throw new Error("NOT IMPLEMENTED");
    }



    public String toString() {
	return new String("JXGraphics: tx = " + transX + ", ty = " + transY);
    }
}
