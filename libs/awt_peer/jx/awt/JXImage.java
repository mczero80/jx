package jx.awt;

import java.awt.*;
import java.awt.image.*;
import jx.wm.*;
import jx.devices.fb.*;


/**
 * This class implements the Image class and represents all images
 * used in the JX implementation. It simply covers a WBitmap class
 * that provides the interface to the underlying window manager.
 */
public class JXImage
    extends java.awt.Image {

    /** The underlying bitmap used to store and draw the image */
    private WBitmap bitmap;


    /**
     * This class can't be instanciated directly.
     */    
    private JXImage() {
	super();
    }


    /**
     * This static method creates a new JXImage object. The width and height
     * must be set to valid values, as the underlying bitmap will be created
     * at this time.
     */
    public static JXImage createImage(int width, int height) {
	JXImage image = new JXImage();
	image.setBitmap(WBitmap.createWBitmap(width, height, new ColorSpace(ColorSpace.CS_RGB16)));
	return image;
    }

    /**
     * Gets the underlying bitmap.
     */
    public WBitmap getBitmap() {
	return bitmap;
    }

    /**
     * Sets the underlying bitmap. You should NOT set it directly, it's only
     * used in the construction process of a JXImage.
     */
    public void setBitmap(WBitmap map) {
	bitmap = map;
    }

    /************************************************
     * methods implemented from Image                *
     ************************************************/

    /**
     * Returns the width of the image.
     */
    public int getWidth(ImageObserver observer) {
	return bitmap.getWidth();
    }
    
    /**
     * Returns the height of the image.
     */
    public int getHeight(ImageObserver observer) {
	return bitmap.getHeight();
    }
    
    /**
     * Returns the image's producer.
     */
    public ImageProducer getSource() {
	throw new Error("not implemented!");
    }
    
    /**
     * Returns a Graphics object that can be used to draw directly
     * on the image.
     */
    public Graphics getGraphics() {
	throw new Error("not implemented!");
    }

    /**
     * Returns the corresponding property to a key string.
     */
    public Object getProperty(String name, ImageObserver observer) {
	throw new Error("not implemented!");
    }
    
    /**
     * Executes all buffered draw events.
     */
    public void flush() {
	throw new Error("not implemented!");
    }
}
