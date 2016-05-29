package jx.wintv;

import jx.framebuffer.*;
import jx.zero.*;
import jx.zero.MemoryManager;
import jx.zero.debug.*;
import java.util.Vector;
import java.util.Enumeration;

public class tests {
   static Clock clock = null;
   
   final static boolean testPrototypeRectangles	= false;
   final static boolean testProductionCode	= false;

   final static boolean testScanlineClipping	= true;
   final static boolean testBitmapClipping	= false;
   
   public static void clippingTests(Naming naming, ClippingRectangle clippings[]){
      MemoryManager memMgr = (MemoryManager)naming.lookup("MemoryManager");
      PackedFramebuffer framebuffer = new MemoryFramebuffer(memMgr, 800, 600, 16);
      
      FieldBlockCreator creator = RISCFieldBlock.getFieldCreator();
      RISCFieldBlock rfb1 =
	ScanlineClippingRasterizer.rasterizeField(creator, RISC.FIFO_VRO, framebuffer, clippings);
      
      Debug.out.println();
      Debug.out.println("RISC Field Block with Scanline Rasterizer:");
      Hex.dumpHex32(Debug.out, rfb1.riscbuffer, 16*512);
      
      
      RISCFieldBlock rfb2 = 
	BitmapClippingRasterizer.rasterizeField(creator, RISC.FIFO_VRO, framebuffer, clippings);
      
      Debug.out.println();
      Debug.out.println("RISC Field Block with Bitmap Rasterizer:");
      Hex.dumpHex32(Debug.out, rfb2.riscbuffer, 16*512);
      
   }

   public static void timingTests(){
      Naming naming = InitialNaming.getInitialNaming();
      clock = (Clock)naming.lookup("Clock");

      MemoryManager memMgr = (MemoryManager)naming.lookup("MemoryManager");
      
      int width = 922; int height = 526/2;   /* PAL max */
      // int width = 768; int height = 576/2;   /* PAL SQP */
      
      width  >>= 0;
      height >>= 0;
      PackedFramebuffer fbOdd  = new MemoryFramebuffer(memMgr, width, height, 16);
      PackedFramebuffer fbEven = new MemoryFramebuffer(memMgr, width, height, 16);
      
      Memory riscbuffer = memMgr.allocAligned(20*1024, 4);
      
      ClippingRectangle clippingsSimple[]=null;
      ClippingRectangle clippingsComplex[]=null;
      ClippingRectangle clippingsExtreme[]=null;
      if( testScanlineClipping || testBitmapClipping ){
	 clippingsSimple  = getClippings(fbOdd, 0);
	 clippingsComplex = getClippings(fbOdd, 1);
	 clippingsExtreme = getClippings(fbOdd, 2);
      }

      // int tmp[] = { 1, 10, 100, 500, 1000};
      int tmp[] = { 50, 100, 200};
      int loops;
      Debug.out.println("****");
      for(int i=0; i<tmp.length; ++i){
	 loops=tmp[i];
	 
	 Debug.out.println("loops = "+loops);
	 if( testPrototypeRectangles ){
	    Debug.out.println("    Prototype  Rectangle Code: "+
			      calcFPS(loops, timeSimpleCode(loops, riscbuffer, fbOdd, fbEven)));
	    
	    Debug.out.println("               Relocation Constructor: "+
			      timeRelocGen(loops, riscbuffer, fbOdd, fbEven));
	    Debug.out.println("               Relocation: "+
			      timeRelocation(loops, riscbuffer, fbOdd, fbEven));
	    
	 }
	 
	 if( testProductionCode ){
	 
	    Debug.out.println("    Production Rectangle Direct : " +
			      calcFPS(loops, timeDirectRectangleCode(loops, memMgr, fbOdd, fbEven)));
	    Debug.out.println("               Relocation Render: "+
			      calcFPS(loops, timeDirectRectangleReloc(loops, memMgr, fbOdd, fbEven)));
	    
	    Debug.out.println("    Production Rectangle Output : " +
			      calcFPS(loops, timeOutputRectangleCode(loops, memMgr, fbOdd, fbEven)));
	    Debug.out.println("               Relocation Render: "+
			      calcFPS(loops, timeOutputRectangleReloc(loops, memMgr, fbOdd, fbEven)));
	    
	    Debug.out.println("    RISC Code Relocation        : "+
			      calcFPS(loops, timeRiscBlockReloc(loops, memMgr, fbOdd, fbEven)));
	 }
	 
	 if( testScanlineClipping ){
	    Debug.out.println("    RISC Block Clipping Edge Table (Complex): "+
			      calcFPS(loops, timeClippingEdgeTable(loops, memMgr, fbOdd, fbEven, clippingsComplex)));
	    Debug.out.println("    RISC Block Clipping Render (Simple) : "+
			      calcFPS(loops, timeRiscBlockClippingRenderScanline(loops, memMgr, fbOdd, fbEven, clippingsSimple)));
	    Debug.out.println("    RISC Block Clipping Render (Complex): "+
			      calcFPS(loops, timeRiscBlockClippingRenderScanline(loops, memMgr, fbOdd, fbEven, clippingsComplex)));
	    Debug.out.println("    RISC Block Clipping Render (Extreme): "+
			      calcFPS(loops, timeRiscBlockClippingRenderScanline(loops, memMgr, fbOdd, fbEven, clippingsExtreme)));
	 }
	 
	 if( testBitmapClipping ){
	    Debug.out.println("    RISC Block Clipping Render Bitmap (Simple) : "+
			      calcFPS(loops, timeRiscBlockClippingRenderBitmap(loops, memMgr, fbOdd, fbEven, clippingsSimple)));
	    Debug.out.println("    RISC Block Clipping Render Bitmap (Complex): "+
			      calcFPS(loops, timeRiscBlockClippingRenderBitmap(loops, memMgr, fbOdd, fbEven, clippingsComplex)));
	    Debug.out.println("    RISC Block Clipping Render Bitmap (Extreme): "+
			      calcFPS(loops, timeRiscBlockClippingRenderBitmap(loops, memMgr, fbOdd, fbEven, clippingsExtreme)));
	 }
      }
   }
   
   /********************************************************************/
   /* Prototype Timing Tests */
   /********************************************************************/
   
   static int timeSimpleCode(int loops, Memory riscbuffer, PackedFramebuffer framebufferOdd, PackedFramebuffer framebufferEven){
      int t1 = getStartTime();
      for(int i=0; i<loops; ++i){
	 SimpleCode.renderRectFrame(riscbuffer, framebufferOdd, framebufferEven);
      }
      return getTime()-t1;
   }
   
   static int timeRelocGen(int loops, Memory riscbuffer, PackedFramebuffer framebufferOdd, PackedFramebuffer framebufferEven){
      int t1 = getStartTime();
      for(int i=0; i<loops; ++i){
	 RISCCodeRelocation rr = new RISCCodeRelocation(riscbuffer);
	 rr.renderRectFrameTempl(framebufferOdd, framebufferEven);
      }
      return getTime()-t1;
   }
   
   static int timeRelocation(int loops, Memory riscbuffer, PackedFramebuffer framebufferOdd, PackedFramebuffer framebufferEven){
      RISCCodeRelocation rr = new RISCCodeRelocation(riscbuffer);
      rr.renderRectFrameTempl(framebufferOdd, framebufferEven);
      int t1 = getStartTime();
      for(int i=0; i<loops; ++i){
	 rr.relocate(FIELD.ODD, i*0x100000);
	 rr.relocate(FIELD.EVEN, i*0x100000);
      }
      return getTime()-t1;
   }

   /********************************************************************/
   /* Production Code Timing Tests */
   /********************************************************************/
   
   static int timeDirectRectangleCode(int loops, MemoryManager memMgr, PackedFramebuffer fbOdd, PackedFramebuffer fbEven){
      RISCJumpBlock rb = new RISCJumpBlock(2);
      FieldBlockCreator creator  = RISCFieldBlock.getFieldCreator();
      
      RISCFieldBlock rfOdd, rfEven;
      int t1 = getStartTime();
      for(int i=0; i<loops; ++i){
	 rfOdd  = RISCCoder.renderDirectRectangle(creator, RISC.FIFO_VRO, fbOdd);
	 rfEven = RISCCoder.renderDirectRectangle(creator, RISC.FIFO_VRE, fbEven);
	 rb.setFieldBlock(0, rfOdd);
	 rb.setFieldBlock(1, rfEven);
	 rb.render();
      }
      return getTime()-t1;
   }
   
   static int timeOutputRectangleCode(int loops, MemoryManager memMgr, PackedFramebuffer fbOdd, PackedFramebuffer fbEven){
      RISCJumpBlock rb = new RISCJumpBlock(2);
      FieldBlockCreator creator  = RISCFieldBlock.getFieldCreator();
      
      RISCFieldBlock rfOdd, rfEven;
      int t1 = getStartTime();
      for(int i=0; i<loops; ++i){
	 rfOdd  = RISCCoder.renderRectangleWithOutput(creator, RISC.FIFO_VRO, fbOdd);
	 rfEven = RISCCoder.renderRectangleWithOutput(creator, RISC.FIFO_VRE, fbEven);
	 rb.setFieldBlock(0, rfOdd);
	 rb.setFieldBlock(1, rfEven);
	 rb.render();
      }
      return getTime()-t1;
   }
   
   static int timeDirectRectangleReloc(int loops, MemoryManager memMgr, PackedFramebuffer fbOdd, PackedFramebuffer fbEven){
      RISCJumpBlock rb = new RISCJumpBlock(2);
      FieldBlockCreator creator  = RISCFieldBlockReloc.getFieldCreator();
      
      RISCFieldBlockReloc rfOdd, rfEven;
      int t1 = getStartTime();
      for(int i=0; i<loops; ++i){
	 rfOdd  = (RISCFieldBlockReloc)RISCCoder.renderDirectRectangle(creator, RISC.FIFO_VRO, fbOdd);
	 rfEven = (RISCFieldBlockReloc)RISCCoder.renderDirectRectangle(creator, RISC.FIFO_VRE, fbEven);
	 rb.setFieldBlock(0, rfOdd);
	 rb.setFieldBlock(1, rfEven);
	 rb.render();
      }
      return getTime()-t1;
   }
   
   static int timeOutputRectangleReloc(int loops, MemoryManager memMgr, PackedFramebuffer fbOdd, PackedFramebuffer fbEven){
      RISCJumpBlock rb = new RISCJumpBlock(2);
      FieldBlockCreator creator  = RISCFieldBlockReloc.getFieldCreator();
      
      RISCFieldBlockReloc rfOdd, rfEven;
      int t1 = getStartTime();
      for(int i=0; i<loops; ++i){
	 rfOdd  = (RISCFieldBlockReloc)RISCCoder.renderRectangleWithOutput(creator, RISC.FIFO_VRO, fbOdd);
	 rfEven = (RISCFieldBlockReloc)RISCCoder.renderRectangleWithOutput(creator, RISC.FIFO_VRE, fbEven);
	 rb.setFieldBlock(0, rfOdd);
	 rb.setFieldBlock(1, rfEven);
	 rb.render();
      }
      return getTime()-t1;
   }
   
   
   static int timeRiscBlockReloc(int loops, MemoryManager memMgr, PackedFramebuffer framebufferOdd, PackedFramebuffer framebufferEven){
      RISCJumpBlock rb = new RISCJumpBlock(2);
      FieldBlockCreator creator  = RISCFieldBlockReloc.getFieldCreator();
      RISCFieldBlockReloc rfOdd  = (RISCFieldBlockReloc)RISCCoder.renderDirectRectangle(creator, RISC.FIFO_VRO, framebufferOdd);
      RISCFieldBlockReloc rfEven = (RISCFieldBlockReloc)RISCCoder.renderDirectRectangle(creator, RISC.FIFO_VRE, framebufferEven);
      rb.setFieldBlock(0, rfOdd);
      rb.setFieldBlock(1, rfEven);
      rb.render();

      int t1 = getStartTime();
      for(int i=0; i<loops; ++i){
	 rfOdd.relocateField(i*0x100000);
	 rfEven.relocateField(i*0x100000);
      }
      return getTime()-t1;
   }
   
   /********************************************************************/
   /* Clipping Code Timing Tests (Production) */
   /********************************************************************/
   
   static int timeClippingEdgeTable(int loops, MemoryManager memMgr, PackedFramebuffer framebufferOdd, PackedFramebuffer framebufferEven, ClippingRectangle clippings[]){
      ClippingEdgeTable et;
      
      int t1 = getStartTime();
      for(int i=0; i<loops; ++i){
	 et = new ClippingEdgeTable(new ClippingRectangle(0, 0, framebufferOdd.width(), framebufferOdd.height()),
				    clippings);
      }
      return getTime() - t1;
   }

   static int timeRiscBlockClippingRenderScanline(int loops, MemoryManager memMgr, PackedFramebuffer framebufferOdd, PackedFramebuffer framebufferEven, ClippingRectangle clippings[]){
      RISCJumpBlock rb = new RISCJumpBlock(2);
      FieldBlockCreator creator  = RISCFieldBlock.getFieldCreator();
      
      RISCFieldBlock rfOdd, rfEven;
      int t1 = getStartTime();
      for(int i=0; i<loops; ++i){
	 rfOdd  = ScanlineClippingRasterizer.rasterizeField(creator, RISC.FIFO_VRO, framebufferOdd , clippings);
	 rfEven = ScanlineClippingRasterizer.rasterizeField(creator, RISC.FIFO_VRE, framebufferEven, clippings);
	 rb.setFieldBlock(0, rfOdd);
	 rb.setFieldBlock(1, rfEven);
	 rb.render();
      }
      return getTime()-t1;
   }
   
   static int timeRiscBlockClippingRenderBitmap(int loops, MemoryManager memMgr, PackedFramebuffer framebufferOdd, PackedFramebuffer framebufferEven, ClippingRectangle clippings[]){
      RISCJumpBlock rb = new RISCJumpBlock(2);
      FieldBlockCreator creator  = RISCFieldBlock.getFieldCreator();
      
      RISCFieldBlock rfOdd, rfEven;
      int t1 = getStartTime();
      for(int i=0; i<loops; ++i){
	 rfOdd  = BitmapClippingRasterizer.rasterizeField(true, true, creator, RISC.FIFO_VRO, framebufferOdd , clippings);
	 rfEven = BitmapClippingRasterizer.rasterizeField(true, true,creator, RISC.FIFO_VRE, framebufferEven, clippings);
	 rb.setFieldBlock(0, rfOdd);
	 rb.setFieldBlock(1, rfEven);
	 rb.render();
      }
      return getTime()-t1;
   }
   

   /********************************************************************/
   /* Support Stuff */
   /********************************************************************/
   
   static ClippingRectangle[] getClippings(PackedFramebuffer framebuffer, int clippingType){
      int MAXX = framebuffer.width()-1;
      int MAXY = framebuffer.height()-1;
      
      ClippingRectangle clippings[] = null;
      
      if( clippingType == 0 ){		/* Simple Clippings */
	 clippings = new ClippingRectangle[]{
	 };
      }
      else if( clippingType == 1 ){	/* Complex Clippings */
	 final int ex = MAXX / 40;
	 final int ey = MAXY / 25;
	 
	 clippings = new ClippingRectangle[] {
	    /* two intersecting rectangles */
	    new ClippingRectangle(  1*ex,  5*ey,  11*ex,  9*ey ),
	    new ClippingRectangle(  6*ex,  7*ey,  16*ex,  11*ey ),
	    
	    /* two 'side by side' rectangles */
	    new ClippingRectangle(  1*ex,   1*ey,  3*ex-1,    2*ey ),
	    new ClippingRectangle(  3*ex,   1*ey,    6*ex,    2*ey ),
	    
	    /* a "standalone" rectangle */
	    new ClippingRectangle( 25*ex, 10*ey, 35*ex, 19*ey),
	    
	    /* "on the edge" rectangles */
	    new ClippingRectangle(           0,       17*ey,  5*ex,  21*ey ),	/* left edge */
	    new ClippingRectangle( MAXX - 5*ex,        8*ey,  MAXX,  10*ey ),	/* right edge  */
	    new ClippingRectangle( MAXX - 5*ex, MAXY - 4*ey,  MAXX,     MAXY)	/* lower right edge */
	 };
      }
      else if( clippingType == 2 ){	/* Extreme Clippings */
	 final int RX = 10;		/* number of rects horizontaly */
	 final int RY = 10;		/* number of rects verticaly */
	 
	 clippings = new ClippingRectangle[RX*RY];
	 
	 final int BX = MAXX/RX;		/* width of the "box" around each rect */
	 final int BY = MAXY/RY;		/* height */
      
	 for(int y=0; y<RY; ++y){
	    for(int x=0; x<RX; ++x){
	       clippings[y*RX+x] = 
		 new ClippingRectangle(BX*x + BX/2, BY*y + BY/2, 
				       BX*x + BX  , BY*y + BY);
	    }
	 }
      }
      else 
	throw new NotImpl("unknown clipping type");
      
      return clippings;
   }
   
   static String calcFPS(int loops, int t){
      if( t == 0 )
	return t + " == INF fr/s";
      return t + " == " + (loops*1000/t) + " fr/s";
   }
   
   static int getStartTime(){
      return getTime();
   }
   
   static int getTime(){
      return clock.getTimeInMillis();
   }
   
}

   
/********************************************************************/

interface FIELD {
   int ODD  = 1;
   int EVEN = 2;
}

/********************************************************************/

class SimpleCode implements FIELD {
   static void renderRectFrame(Memory risccode,
			PackedFramebuffer framebufferOdd, 
			PackedFramebuffer framebufferEven){
      RISCGenerator rg = new RISCGenerator(risccode);
      renderRectField(rg, RISC.FIFO_VRE, framebufferOdd);
      renderRectField(rg, RISC.FIFO_VRO, framebufferEven);
      // FIXME: no jump
   }
   
   static void renderRectField(RISCGenerator rg, int sync, PackedFramebuffer framebuffer){
      int fbp = framebuffer.startAddress();
      int scanlineLength = framebuffer.width()*framebuffer.pixelOffset();
      int scanlineOffset = framebuffer.scanlineOffset();
      int height = framebuffer.height();
      
      rg.sync(RISC.FIFO_FM1 | RISC.RESYNC | RISC.IRQ);
      for(int y=0; y<height; ++y){
	 rg.write(RISC.SOL|RISC.EOL, scanlineLength, fbp);
	 fbp += scanlineOffset;
      }
      rg.sync( sync | RISC.RESYNC);
   }
   
}


/********************************************************************/

class SimpleCode2 implements FIELD {
   static void renderRectFrame(RISCGenerator rg,
			PackedFramebuffer framebufferOdd, 
			PackedFramebuffer framebufferEven){
      rg.reset();
      renderRectField(rg, RISC.FIFO_VRE, framebufferOdd);
      renderRectField(rg, RISC.FIFO_VRO, framebufferEven);
      // FIXME: no jump
   }
   
   static void renderRectField(RISCGenerator rg, int sync, PackedFramebuffer framebuffer){
      int fbp = framebuffer.startAddress();
      int scanlineLength = framebuffer.width()*framebuffer.pixelOffset();
      int scanlineOffset = framebuffer.scanlineOffset();
      int height = framebuffer.height();
      
      rg.sync(RISC.FIFO_FM1 | RISC.RESYNC | RISC.IRQ);
      for(int y=0; y<height; ++y){
	 rg.write(RISC.SOL|RISC.EOL, scanlineLength, fbp);
	 fbp += scanlineOffset;
      }
      rg.sync( sync | RISC.RESYNC);
   }
   
}

/********************************************************************/

class RISCCodeRelocation extends RISCGenerator implements FIELD {
   int addrIndexOdd[] = null;
   int addrIndexEven[] = null;
   int baseAddressOdd = 0;
   int baseAddressEven = 0;
   
   RISCCodeRelocation(Memory mem){
      super(mem);
   }
   
   void renderRectFrameTempl(PackedFramebuffer framebufferOdd, 
			     PackedFramebuffer framebufferEven){
//      riscbuffer.fill16((short)0, 0, riscbuffer.size()/2);
      addrIndexOdd = new int[framebufferOdd.height()];
      addrIndexEven = new int[framebufferEven.height()];
      renderRectFieldTempl(RISC.FIFO_VRE, addrIndexOdd,  framebufferOdd);
      renderRectFieldTempl(RISC.FIFO_VRO, addrIndexEven, framebufferEven);
      // FIXME: no jump
      
      baseAddressOdd = framebufferOdd.startAddress();
      baseAddressEven = framebufferEven.startAddress();
      
//      relocate(baseAddressOdd, addrIndexOdd,  framebufferOdd.memObj().getStartAddress());
//      relocate(baseAddressEven, addrIndexEven, framebufferEven.memObj().getStartAddress());
   }
   
   void renderRectFieldTempl(int sync, int addrIndex[], PackedFramebuffer framebuffer){
      int fbp = framebuffer.startAddress();
      int scanlineLength = framebuffer.width()*framebuffer.pixelOffset();
      int scanlineOffset = framebuffer.scanlineOffset();
      int height = framebuffer.height();
      
      sync(RISC.FIFO_FM1 | RISC.RESYNC | RISC.IRQ);
      for(int y=0; y<height; ++y){
	 write(RISC.SOL|RISC.EOL, scanlineLength, fbp);
	 // FIXME: internal knowledge of the RISC code is needed here.
	 addrIndex[y] = risc_index-1;
	 fbp += scanlineOffset;
      }
      sync( sync | RISC.RESYNC);
   }
   
   void relocate(int f, int startAddress){
      if( f == FIELD.ODD )
	relocate(baseAddressOdd, addrIndexOdd, startAddress);
      else
	relocate(baseAddressEven, addrIndexEven, startAddress);
   }
   
   void relocate(int baseAddress, int addrIndex[], int startAddr){
      int offset = startAddr - baseAddress;
      
      for(int i=0; i<addrIndex.length; ++i){
	 riscbuffer.set32(addrIndex[i], 
			  riscbuffer.get32(addrIndex[i]) + offset);
      }
   }
}
