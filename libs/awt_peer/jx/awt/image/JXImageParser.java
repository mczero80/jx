package jx.awt.image;

import jx.awt.*;

/**
 * This is the interface for all parser classes used with
 * JXImageLoader. Every parser must implement the following
 * methods to allow the import of its supported file format.
 */
public interface JXImageParser {
    
    /**
     * Indicates to the parser that it should start preparing
     * the file for evaluation. A parser might now perhaps load
     * the complete file in memory, or just set the file seek
     * pointer to a certain start value. The concrete implementation
     * is left to the parser.
     */
    public void readImageFile(String name);
    /**
     * Gets the picture's width in pixels.
     */
    public int getImageWidth();
    /**
     * Gets the picture's height in pixels.
     */
    public int getImageHeight();
    /**
     * Gets the red value of the pixel at (x, y).
     */
    public int getRedAt(int x, int y); 
    /**
     * Gets the green value of the pixel at (x, y).
     */
    public int getGreenAt(int x, int y); 
    /**
     * Gets the blue value of the pixel at (x, y).
     */
    public int getBlueAt(int x, int y); 
}
