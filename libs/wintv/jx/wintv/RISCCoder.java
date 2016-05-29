package jx.wintv;

import jx.framebuffer.PackedFramebuffer;
import jx.zero.*;
import jx.zero.debug.*;

class RISCCoder {
   final static boolean verbose			= false;
   final static int defaultFieldBlockSize	= 3000;
   
   
   /********************************************************************/
   /* render simple unclipped rectangles                               */
   /********************************************************************/
   
   
   static RISCFieldBlock renderDirectRectangle(FieldBlockCreator creator, int syncType, PackedFramebuffer framebuffer){
      if( framebuffer instanceof LinkedPackedFramebuffer ){
	 if( verbose )
	   Debug.out.println("**** taking redirection of code ****");
	 return renderRectangleWithOutput(creator, syncType, (LinkedPackedFramebuffer)framebuffer);
      }
      
      // calculate size of RISC-Code
      int size = 0;
      size += 8;			/* sync(FM1) */
      size += framebuffer.height() * 8;	/* write for each scanline */
      size += 8;			/* sync(VRO/VRE) */
      size += 8;			/* jump */
      size += 4;			/* inval */
      
      RISCFieldBlock rb = creator.createFieldBlock(size/4);
      
      int fbp = framebuffer.startAddress();
      int scanlineOffset = framebuffer.scanlineOffset();
      int scanlineLength = framebuffer.width() * framebuffer.pixelOffset();
      int height = framebuffer.height();
      
      if( verbose )
	Debug.out.println("renderRectangle: scanlineLength = "+scanlineLength+" byte");

      if( false ){
	 /* 
	  * FIXME: Warning: IRQ Requests are generated only for FIFO_VRE which
	  * is helpfull for debugging. 
	  */
	 final int syncTime = 0;
	 
	 if( syncTime == -1 && syncType == RISC.FIFO_VRE )
	   rb.sync(RISC.FIFO_FM1 | RISC.RESYNC | RISC.IRQ);
	 else
	   rb.sync(RISC.FIFO_FM1 | RISC.RESYNC );
	 
	 for(int y=0; y<height; ++y){
	    if( y == syncTime && syncType == RISC.FIFO_VRE )
	      rb.write(RISC.SOL|RISC.EOL|RISC.IRQ, scanlineLength, fbp);
	    else
	      rb.write(RISC.SOL|RISC.EOL, scanlineLength, fbp);
	    fbp += scanlineOffset;
	 }
	 if( syncTime == height && syncType == RISC.FIFO_VRE )
	   rb.sync(syncType | RISC.RESYNC | RISC.IRQ );
	 else
	   rb.sync(syncType | RISC.RESYNC);
	 rb.jump(0x0);
	 rb.inval();
      }
      else {
	 rb.sync(RISC.FIFO_FM1 | RISC.RESYNC );
	 for(int y=0; y<height; ++y, fbp+=scanlineOffset)
	   rb.write(RISC.SOL|RISC.EOL, scanlineLength, fbp);
	 rb.sync(syncType | RISC.RESYNC | RISC.IRQ);
	 rb.jump(0x0);
	 rb.inval();
      }
      
      return rb;
   }
   
   static RISCFieldBlock renderRectangleWithOutput(FieldBlockCreator creator, int syncType, PackedFramebuffer framebuffer){
      if( framebuffer instanceof LinkedPackedFramebuffer ){
	 if( verbose )
	   Debug.out.println("**** taking redirection of code ****");
	 return renderRectangleWithOutput(creator, syncType, (LinkedPackedFramebuffer)framebuffer);
      }
      
      // calculate size of RISC-Code
      int size = 0;
      size += 8;			/* sync(FM1) */
      size += framebuffer.height() * 8;	/* write for each scanline */
      size += 8;			/* sync(VRO/VRE) */
      size += 8;			/* jump */
      size += 4;			/* inval */
      
      RISCFieldBlock rb = creator.createFieldBlock(size/4);
      BT878PackedRISCOutput output = new BT878PackedRISCOutput(framebuffer, rb);

      int width  = framebuffer.width();
      int height = framebuffer.height();
      
      rb.sync(RISC.FIFO_FM1 | RISC.RESYNC);
      for(int y=0; y<height; ++y){
	 output.write(width, true, true);
      }
      output.eof();
      rb.sync(syncType | RISC.RESYNC | RISC.IRQ);
      rb.jump(0x0);
      rb.inval();
      
      return rb;
   }
   
   static RISCFieldBlock renderRectangleWithOutput(FieldBlockCreator creator, int syncType, LinkedPackedFramebuffer framebuffer){
      if( verbose )
	Debug.out.println("**** render LinkedPackedFramebuffer ****");
      
      final int nx = framebuffer.getNHoriz();
      final int ny = framebuffer.getNVert();
      
      int fx, fy, height;
      boolean sol, eol;
      
      RISCFieldBlock rb = creator.createFieldBlock(defaultFieldBlockSize);   // FIXME: calculate
      
      BT878PackedRISCOutput output[] = new BT878PackedRISCOutput[nx]; 
      
      rb.sync(RISC.FIFO_FM1 | RISC.RESYNC);
      for(fy=0; fy<ny; ++fy){
	 
	 for(fx=0; fx<output.length; ++fx)
	   output[fx] = new BT878PackedRISCOutput(framebuffer.getFramebuffer(fx, fy), rb);
	 
	 height = framebuffer.getHeight(fy);
	 for(int y=0; y<height; ++y){
	    for(fx=0; fx<nx; ++fx){
	       sol = (fx == 0);
	       eol = (fx == nx-1);
	       if( !sol )
		 output[fx].sol();	//simulate start of line
	       output[fx].write(framebuffer.getWidth(fx), 
				    sol, eol);
	       if( !eol )
		 output[fx].eol();	// simulate line switch
	    }
	 }
	 for(fx=0; fx<nx; ++fx)
	   output[fx].eof();
      }
      rb.sync(syncType | RISC.RESYNC | RISC.IRQ);
      rb.jump(0x0);
      rb.inval();
      
      if( verbose )
	Debug.out.println("renderRectangleWithOutput(LinkedPackedFramebuffer): used " +rb.getRiscIndex() + " of " + rb.getMaxIndex() + " dwords" );
      return rb;
   }
}

