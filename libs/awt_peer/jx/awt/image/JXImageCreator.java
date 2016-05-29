package jx.awt.image;

import java.awt.image.*;
import java.util.*;
import jx.awt.*;
import jx.devices.fb.*;


/**
 * This class is used to load images from any ImageProducer
 * class, especially from MemoryImageSource.
 */
public class JXImageCreator
    implements ImageConsumer {

    private JXImage image;
    private boolean complete;
    

    /**
     * Gets the image from the producer. This method starts the 
     * production process of the producer and lets it do the work.
     * After its work is finished, the new image is returned.
     */
    public JXImage createImage(ImageProducer producer) {
	complete = false;
	producer.startProduction(this);
	if (!complete) {
	    System.out.println("Error on creating image!");
	    return null;
	} else
	    return image;
    }

    /******************************************
     * methods implemented from ImageConsumer  *
     ******************************************/

    /**
     * Called by the producer when the picture's dimensions are known.
     */
    public void setDimensions(int width, int height) {
	//System.out.println("got image size " + width + "x" + height);
	image = JXImage.createImage(width, height);
    }

    /**
     * Loads the pixels into the image. This method is not used, but it
     * would be useful to load grayscaled pictures.
     */
    public void setPixels(int x, int y, int w, int h, 
			  ColorModel model, byte[] pixels, int offset, int scansize) {
	int index;
	int g;
	PixelRect pRect = new PixelRect(0, 0, 0, 0);
	PixelColor color = new PixelColor(0, 0, 0, 0);
	for (int y1 = y; y1 < y + h; y1++)
	    for (int x1 = x; x1 < x + w; x1++) {
		index = y1 * scansize + x1 + offset;
		g = pixels[index];
		pRect.setTo(x1, y1, x1, y1);
		color.setTo(g, g, g, 0);
		image.getBitmap().drawLine(pRect, pRect, color, DrawingMode.OVER);
	    }
    }
    
    /**
     * Called by the producer to load the pixels into the image. This
     * method fills the image with a similar algorithm like the one
     * used in JXImageLoader. 
     */
    public void setPixels(int x, int y, int w, int h, 
			  ColorModel model, int[] pixels, int offset, int scansize) {
	int index;
	int r, g, b;
	PixelRect pRect = new PixelRect(0, 0, 0, 0);
	PixelColor color = new PixelColor(0, 0, 0, 0);
	for (int y1 = y; y1 < y + h; y1++)
	    for (int x1 = x; x1 < x + w; x1++) {
		index = y1 * scansize + x1 + offset;
		r = (pixels[index] >> 16) & 0xFF;
		g = (pixels[index] >> 8) & 0xFF;
		b = pixels[index] & 0xFF;
		pRect.setTo(x1, y1, x1, y1);
		color.setTo(r, g, b, 0);
		image.getBitmap().drawLine(pRect, pRect, color, DrawingMode.OVER);
	    }
    }
    
    /**
     * Called by the producer when the image loading
     * is complete.
     */
    public void imageComplete(int status) {
	complete = true;
    }

    /** Sets some properties. Not used in this implementation. */
    public void setProperties(Hashtable props) {}
    /** Sets some hints. Not used in this implementation. */
    public void setHints(int flags) {}
    public void setColorModel(ColorModel model) {}

}
