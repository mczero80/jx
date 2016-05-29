package jx.awt.image;

import jx.awt.*;
import jx.devices.fb.*;


/**
 * Loads a picture from disk. It is designed to support several parser
 * classes for different file formats.
 * To make a new file format accessible for the JXImageLoader, you have
 * to extend both methods to recognize the new type, and to write a new
 * parser class which must implement JXImageParser.
 */
public class JXImageLoader {

    /** Constant for an unknown file type */
    private final int UNKNOWN = 0;
    /** Constant for a PPM 24-bit raw file*/
    private final int PPMFILE = 1;


    
    /**
     * Tries to recognize the file type by evaluating the file extension.
     */
    private int getFileType(String name) {
	String s = name.trim().toLowerCase();
	
	if (s.endsWith(".ppm"))
	    return PPMFILE;
	return UNKNOWN;
    }
    
    /**
     * Loads the specified file into an image. First the type is checked,
     * then the associated parser class is called to prepare the file for
     * import. In the last step, the file content is transformed into an
     * image.
     */
    public JXImage loadImage(String name) {
	JXImage image;
	JXImageParser p;
	// find out file type and invoke parser
	int type = getFileType(name);
	switch (type) {
	case PPMFILE:p = new PPMParser();break;
	case UNKNOWN:
	default:
	    System.out.println("Type of " + name + " is unknown!");
	    return null;
	}
	// prepare file for evaluation
	p.readImageFile(name);
	int width = p.getImageWidth();
	int height = p.getImageHeight();
	System.out.println("got image size " + width + "x" + height);
	// prepare new image
	image = JXImage.createImage(width, height);
	// load file data into image
	// the loading algorithm should stay exactly as it is, otherwise
	// it would be very difficult to write a parser that doesn't
	// load the whole file in memory before evaluation.
	PixelRect pRect = new PixelRect(0, 0, 0, 0);
	PixelColor color = new PixelColor(0, 0, 0, 0);
	for (int y = 0; y < height; y++)
	    for (int x = 0; x < width; x++) {
		pRect.setTo(x, y, x, y);
		color.setTo(p.getRedAt(x, y), p.getGreenAt(x, y), p.getBlueAt(x, y), 0);
		image.getBitmap().drawLine(pRect, pRect, color, DrawingMode.OVER);
	    }
	return image;
    }
}
