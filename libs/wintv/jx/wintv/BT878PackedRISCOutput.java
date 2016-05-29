package jx.wintv;

import jx.zero.*;
import jx.framebuffer.*;

class BT878PackedRISCOutput implements ClippingRasterizerOutput {
   PackedFramebuffer framebuffer;
   RISCFieldBlock rfb;
   int fbp;				/* current framebuffer pointer */
   
   int scanlineOffset;

   
   int loMark;
   int hiMark;
   
   boolean simulateSOL = false;
   
   int byteMult;
   int pixMask;
   
   /* debugging stuff */
   final static boolean debug = false;
   int curline;
   int linewidth;
   
   
   
   BT878PackedRISCOutput(PackedFramebuffer framebuffer, RISCFieldBlock rfb){
      this(framebuffer, rfb, false);
   }

   BT878PackedRISCOutput(PackedFramebuffer framebuffer, RISCFieldBlock rfb, boolean debugOutput){
      this.framebuffer = framebuffer;
      this.rfb = rfb;
      this.fbp = framebuffer.startAddress();
      // FIXME: this.fbe = framebuffer.startAddress() + framebuffer.scanlineOffset() * framebuffer.height();
      // DEBUG ONLY: this.debug = debugOutput;
      
      switch( framebuffer.depth() ){
       case 8:
	 byteMult = 1;
	 pixMask  = RISC.BENABL0;
	 break;
       case 15:
       case 16:
	 byteMult = 2;
	 pixMask  = RISC.BENABL0 | RISC.BENABL1;
	 break;
       case 24:
	 byteMult = 3;
	 pixMask  = RISC.BENABL0 | RISC.BENABL1 | RISC.BENABL2;
	 break;
       case 32:
	 byteMult = 4;
	 pixMask  = RISC.BENABL0 | RISC.BENABL1 | RISC.BENABL2 | RISC.BENABL3;
	 break;
       default:
	 throw new Error("unsupported depth: "+framebuffer.depth());
      }
      
      scanlineOffset = framebuffer.scanlineOffset();
   }
   
   public void write(int width, boolean sol, boolean eol){
      write(width, sol, eol, 0);
   }

   public void write(int width, boolean sol, boolean eol, int options){
      if( width <= 0 )
	Debug.out.println("!!!!****!!!! ERROR: unexspected width in line "+curline+": "+width);
      
      if( sol )
	linewidth = width;
      else
	linewidth += width;
      
      options |= (sol ? RISC.SOL : 0) | (eol ? RISC.EOL : 0);
      int bytes = width * byteMult;
      if( sol || simulateSOL ){
	 simulateSOL = false;
	 rfb.write(options, bytes, fbp);
      }
      else
	rfb.writec(options, bytes);
      
      if( fbp < loMark )
	loMark = fbp;
      if( fbp+bytes > hiMark )
	hiMark = fbp+bytes;
      
      if( eol ){
	 fbp += framebuffer.scanlineOffset();
	 ++curline;
      }
      
      if( debug ){
	 Debug.out.println("write("+width+ (sol? ", SOL": "") + (eol? ", EOL": "") + ")");
	 if( eol )
	   Debug.out.println("scanline "+curline+" width: "+linewidth);
      }
   }
   
   public void skip(int width, boolean sol, boolean eol){
      skip(width, sol, eol, 0);
   }
   
   public void skip(int width, boolean sol, boolean eol, int options){
      if( width <= 0 )
	Debug.out.println("!!!!****!!!! ERROR: unexspected width in line "+curline+": "+width);
      
      if( sol )
	linewidth = width;
      else
	linewidth += width;
      
      options |= eol ? RISC.EOL : 0;
      if( sol || simulateSOL ){
	 simulateSOL = false;
	 
	 /* 
	  * The current address for the following "inner" writec commands
	  * must be set. This is not possible with a simple skip command.
	  * Therefore the following write command doesn't write any data
	  * but sets the internal address.
	  * 
	  * The problem is: a write(RISC.SOL, 0, fbp) confuses the chip.
	  * Therefore I write one pixel (two bytes) and mask out this pixel
	  * via the "byte enables" mechanism. 
	  */
	 rfb.write(RISC.SOL | RISC.BENABL0 | RISC.BENABL1, 2, fbp);
	 rfb.skip(options, (--width)*byteMult);
      }
      else
	rfb.skip(options, width*byteMult);
      
      if( eol ){
	 fbp += framebuffer.scanlineOffset();
	 ++curline;
      }
      
      if( debug ){
	 Debug.out.println("skip("+width+ (sol? ", SOL": "") + (eol? ", EOL": "") + ")");
	 if( eol )
	   Debug.out.println("scanline "+curline+" width: "+linewidth);
      }
   }
   
   public void sol(){			/* FIXME: needed by LinkedPackedFramebuffer */
      simulateSOL = true;
   }
   
   public void eol(){			/* FIXME: needed by LinkedPackedFramebuffer */
      fbp += framebuffer.scanlineOffset();
      ++curline;
      linewidth = 0;
   }
   
   public void eof(){
      linewidth = 0;
      curline = 0;
   }
}
