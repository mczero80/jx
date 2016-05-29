package jx.awt.image;

import java.util.*;
import jx.zero.*;
import jx.zero.memory.*;


/**
 * This class implements a picture parser for the PPM 24-bit raw
 * format.
 */
public class PPMParser
    implements JXImageParser {
    
    private MemoryInputStream mem;
    private int width;
    private int height;

    
    /** Reads a complete line from the input stream. */
    private String readLine(MemoryInputStream memStream) {
	StringBuffer s = new StringBuffer();
	char c;
	do {
	    c = (char) memStream.read();
	    if (c != '\n')
		s.append(c);
	} while (c != '\n');
	//System.out.println("read " + s);
	return s.toString();
    }

    /**
     * Opens the JX boot file system and gets a stream to the
     * specified file, if existing.
     */
    private MemoryInputStream getFileStream(String name) {
	BootFS bootFS = (BootFS) InitialNaming.getInitialNaming().lookup("BootFS");
        if (bootFS == null)
	    throw new Error("No BootFS found!");
	return new MemoryInputStream(bootFS.getFile(name));
    }

    /**
     * Implemented from JXImageParser. It just checks the file version and,
     * if fitting, reads the picture's dimensions and sets the seek pointer
     * to the start position of the file's picture data.
     */
    public void readImageFile(String name) {
	// get stream for file
	mem = getFileStream(name);
	// check PPM version
	if (!readLine(mem).startsWith("P6"))
	    throw new Error("Unsupported file format!");
	// skip next line (normally a comment)
	readLine(mem);
	// read picture size
	StringTokenizer st = new StringTokenizer(readLine(mem), " ");
	width = Integer.parseInt(st.nextToken());
	height = Integer.parseInt(st.nextToken());
	//System.out.println("picture size is " + width + "x" + height);
	// skip next line (color depth information)
	readLine(mem);
    }

    /**
     * Implemented from JXImageParser. Gets the width of the picture.
     */
    public int getImageWidth() {
	return width;
    }

    /**
     * Implemented from JXImageParser. Gets the height of the picture.
     */
    public int getImageHeight() {
	return height;
    }

    /**
     * Implemented from JXImageParser. Gets the red color part of a pixel.
     * Note that this implementation would not work if the loading
     * algorithm in JXImageLoader would be altered too much!
     */
    public int getRedAt(int x, int y) {
	return mem.read();
    }
    /**
     * Implemented from JXImageParser. Gets the green color part of a pixel.
     * Note that this implementation would not work if the loading
     * algorithm in JXImageLoader would be altered too much!
     */
    public int getGreenAt(int x, int y) {
	return mem.read();
    }
    /**
     * Implemented from JXImageParser. Gets the blue color part of a pixel.
     * Note that this implementation would not work if the loading
     * algorithm in JXImageLoader would be altered too much!
     */
    public int getBlueAt(int x, int y) {
	return mem.read();
    }
}
