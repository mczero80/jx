/* 
 * BitmapClippingRasterizer
 * 
 * This is the same algorithm for clipping which is used by the bttv driver
 * in Linux 2.2.16. This algorithm uses a 1024x625 bitmap (max video
 * picture size) and marks the bits which are inside the clipping
 * rectangles. After every clipping rectangles is written into the bitmap,
 * each line is scanned for continous segements of equal bits and the
 * segments are transformed into RISC code. 
 * 
 * The classes are not used in "production code", they are used only for
 * comparison benchmarks with the scanline algorithm.
 * 
 * CAVEATS: If the code is compiled for benchmarks, the following "quirks"
 *     apply:
 * 
 *     - The bitmap is allocated only once and will be recycled for each field.
 * 
 *     - The bitmap is not cleared before writing the clipping rectangles. 
 * 
 * This means: The resulting RISC code is very likly *INCORRECT*.
 * 
 */

package jx.wintv;

import jx.framebuffer.PackedFramebuffer;
import jx.framebuffer.ClippingRectangle;
import jx.zero.Debug;
import jx.framebuffer.*;

public class BitmapClippingRasterizer {
   
   /* Debuging options */
   final static boolean dumpBitmap	= false;
   
   public static RISCFieldBlock rasterizeField(FieldBlockCreator creator, int synctype, PackedFramebuffer framebuffer, ClippingRectangle rectangles[]){
      return rasterizeField(true, false, creator, synctype, framebuffer, rectangles);
   }
   
   public static RISCFieldBlock rasterizeField(boolean clearBitmap, boolean reuseBitmap, FieldBlockCreator creator, int synctype, PackedFramebuffer framebuffer, ClippingRectangle rectangles[]){
      
      ClippingBitmap bitmap = new ClippingBitmap(reuseBitmap);
      if( clearBitmap )
	bitmap.clear(0x0);
      for(int i=0; i<rectangles.length; ++i){
	 bitmap.drawRectangle(rectangles[i]);
      }
      
      if( dumpBitmap ){
	 Debug.out.println("Clipping bitmap:");
	 Hex.dumpHex(Debug.out, bitmap.clipmap, 0x0000);
      }
      
      RISCFieldBlock rfb = creator.createFieldBlock(16*1024);
      ClippingRasterizerOutput output = new BT878PackedRISCOutput(framebuffer, rfb);
      
      rfb.sync(RISC.FIFO_FM1 | RISC.RESYNC | RISC.IRQ);
      bitmap.translate(output, framebuffer);
      rfb.sync(synctype | RISC.RESYNC);
      rfb.jump(0x0);
      rfb.inval();
      
      return rfb;
   }
}

/********************************************************************/

class ClippingBitmap {
   static byte lmaskt[] = { (byte)0xff, (byte)0xfe, (byte)0xfc, (byte)0xf8, (byte)0xf0, (byte)0xe0, (byte)0xc0, (byte)0x80 };
   static byte rmaskt[] = { (byte)0x01, (byte)0x03, (byte)0x07, (byte)0x0f, (byte)0x1f, (byte)0x3f, (byte)0x7f, (byte)0xff };
   
   byte clipmap[];
   
   static byte globalClipmap[];		/* for timing tests only */
   
   ClippingBitmap(){
      this(false);
   }
   
   ClippingBitmap(boolean reuseBitmap){
      if( reuseBitmap ){
	 if( globalClipmap == null )
	   globalClipmap = new byte[1024*625 / 8];
	 clipmap = globalClipmap;
	 return;
      }
      clipmap = new byte[1024*625 / 8];
   }
   
   void drawRectangle(ClippingRectangle rect){
      int x = rect.xmin();
      int y = rect.ymin();
      int w = rect.xmax() - rect.xmin();
      int h = rect.ymax() - rect.ymin() +1;
      
      if( x < 0 ){
	 w += x;
	 x = 0;
      }
      if( y < 0 ){
	 h += y;
	 y = 0;
      }
      if( w < 0 || h < 0 )
	return;
      
      if( y+h >= 625 )
	h = 625 - y;
      if( x+w >= 1024 )
	w = 1024-x;
      
      int l = x >> 3;
      int r = (x+w) >> 3;
      int W = r-l-1;
      
      byte lmask = lmaskt[x&7];
      byte rmask = rmaskt[(x+w)&7];
      int p = 128*y+l;
      
      if( W>0 ){
	 for(int i=0; i<h; ++i, p+=128){
	    clipmap[p] |= lmask;
	    memset(p+1, (byte)0xff, W);
	    clipmap[p+W+1] |= rmask;
	 }
      }
      else if( W == 0 ){
	 for(int i=0; i<h; ++i, p+=128){
	    clipmap[p]   |= lmask;
	    clipmap[p+1] |= rmask;
	 }
      }
      else {
	 for(int i=0; i<h; ++i, p+=128)
	   clipmap[p] |= (lmask & rmask);
      }
   }
   
   /* tanslate bitmap to risc code */
   void translate(ClippingRasterizerOutput output, PackedFramebuffer framebuffer){
      
      int adr	= framebuffer.startAddress();
      int bpl	= framebuffer.scanlineOffset();
      int height= framebuffer.height();
      int width = framebuffer.width();
      
      int dx, sx, x, y;
      boolean lastbit, cbit;
      for(y=0; y < height; ++y, adr += bpl){
	 lastbit = (clipmap[y<<7] & 1) != 0;
	 for(x=dx=1, sx=0; x<=width; ++x){
	    cbit = (clipmap[(y<<7)+(x>>3)] & (1<<(x&7))) != 0;
	    if( x<width && lastbit == cbit )
	      ++dx;
	    else {
	       if( !lastbit )
		 output.write(dx, (sx==0), (sx+dx == width));
	       else
		 output.skip(dx, (sx==0), (sx+dx == width));
	       lastbit = cbit;
	       sx += dx;
	       dx = 1;
	    }
	 }
      }
   }
   
   void memset(int startoffset, byte value, int length){
      int endoffset = startoffset + length;
      for(int i=startoffset; i<endoffset; ++i)
	clipmap[i] = value;
   }
   
   void clear(int value){
      memset(0, (value == 0) ? (byte)0x00 : (byte)0xff, clipmap.length);
   }
}
