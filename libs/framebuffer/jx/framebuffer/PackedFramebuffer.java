package jx.framebuffer;

import jx.zero.Memory;

/**
 * Definition of the interface between the graphic subsystem and the
 * framegrabber subsystem.
 */
public interface PackedFramebuffer {
   public int width();			/* width of framebuffer in pixel */
   public int height();			/* height of framebuffer in pixel */
   public int depth();			/* depth of framebuffer in bit per pixel */
   
   public int startAddress();		/* address of pixel(0,0) */
   public int scanlineOffset();		/* offset between two scanlines */
   public int pixelOffset();		/* offset between two pixels */
   
 public Memory memObj();		/* Debugging: *full* memory object */
   // public String toString();	// Compiler Crash
   public void clear();
   
   public String framebufferType();	/* argl should not be public */
}

