package jx.wintv;

import jx.framebuffer.*;

public class PFBTools {
   PackedFramebuffer framebuffer;
   int scanlineOffset;
   int pixelOffset;
   
   int byteStart;
   int byteEnd;
   int byteWidth;
   
   
   public PFBTools(PackedFramebuffer framebuffer){
      this.framebuffer = framebuffer;
      
      pixelOffset = framebuffer.pixelOffset();
      scanlineOffset = framebuffer.scanlineOffset();
      
      byteStart = framebuffer.startAddress() - framebuffer.memObj().getStartAddress();
      byteEnd   = byteStart + scanlineOffset * framebuffer.height();
      
      byteWidth = framebuffer.width() * pixelOffset;
      
   }
   
   public int getPixelOffset8(int x, int y){
      return byteStart +
	y * scanlineOffset +
	x * pixelOffset;
   }
   
   public int getPixelOffset16(int x, int y){
      return getPixelOffset8(x, y) >> 1;
   }
   
   public int getPixelOffset32(int x, int y){
      return getPixelOffset8(x, y) >> 2;
   }

   /* Drawing Primitives */
   
   public void drawHLine16(int x, int y, int w, short color){
      w = Math.min(framebuffer.width()-x, w);
      framebuffer.memObj().fill16(color, getPixelOffset16(x, y), w);
   }
   
   public void drawRect16(int x, int y, int w, int h, short color){
      int memoff = getPixelOffset16(x, y);
      int lineoff = scanlineOffset >> 1;
      w = Math.min(framebuffer.width()-x, w);
      h = Math.min(framebuffer.height()-y, h);
      for(;h>0; --h){
	 framebuffer.memObj().fill16(color, memoff, w);
	 memoff += lineoff;
      }
   }
   
   public void drawVLine16(int x, int y, int h, short color){
      int memoff = getPixelOffset16(x, y);
      int lineoff = scanlineOffset >> 1;
      h = Math.min(framebuffer.height()-y, h);
      for(;h>0; --h){
	 framebuffer.memObj().set16(memoff, color);
	 memoff += lineoff;
      }
   }
   
   
   
   public static int calcX(PackedFramebuffer parentbuffer, PackedFramebuffer childbuffer){
      return calcX(parentbuffer, childbuffer.startAddress());
   }
   
   public static int calcY(PackedFramebuffer parentbuffer, PackedFramebuffer childbuffer){
      return calcY(parentbuffer, childbuffer.startAddress());
   }
   
   public static int calcX(PackedFramebuffer parentbuffer, int childStartAddress){
      childStartAddress -= parentbuffer.startAddress();
      if( childStartAddress < 0 )
	throw new Error("parent/child missmatch");/* FIXME: what error/exception */
      
      childStartAddress %= parentbuffer.scanlineOffset();
      childStartAddress /= parentbuffer.pixelOffset();
      if( childStartAddress >= parentbuffer.width() )
	throw new Error("child outside parent: "+childStartAddress+"/"+parentbuffer.width());   /* FIXME: what excetpion */
      return childStartAddress;
   }
   
   public static int calcY(PackedFramebuffer parentbuffer, int childStartAddress){
      childStartAddress -= parentbuffer.startAddress();
      if( childStartAddress < 0 )
	throw new Error("parent/child missmatch");/* FIXME: what error/exception */
      
      childStartAddress /= parentbuffer.scanlineOffset();
      if( childStartAddress >= parentbuffer.height() )
	throw new Error("child outside parent"+childStartAddress+"/"+parentbuffer.height());   /* FIXME: what excetpion */
      return childStartAddress;
   }
}

