package jx.framebuffer;

import jx.zero.Memory;
import jx.zero.Debug;

/**
 * Base class of the various variants of Packed Framebuffers.
 */
public class PackedFramebufferImpl implements PackedFramebuffer {
   int width;
   int height;
   int depth;
   
   int startAddress;
   int pixelOffset;
   int scanlineOffset;
   
   Memory memObj;
   
   // This constructor is *not* public by intention!
   PackedFramebufferImpl(int width, int height, int depth,
			 int startAddress, int pixelOffset, int scanlineOffset,
			 Memory memObj){
      Debug.assert(width > 0 && height > 0 && depth >= 8, "wrong width, height or depth" );
      Debug.assert(scanlineOffset == 0 || scanlineOffset > pixelOffset, "wrong scanlineOffset or pixelOffset");
      
      this.width = width;
      this.height = height;
      this.depth = depth;
      
      if( startAddress !=  0 )
	this.startAddress = startAddress;
      else 
	this.startAddress = memObj.getStartAddress();
      
      if( pixelOffset != 0 )
	this.pixelOffset = pixelOffset;
      else
	this.pixelOffset = depthToBPP(depth);
      
      if( scanlineOffset != 0 )
	this.scanlineOffset = scanlineOffset;
      else
	this.scanlineOffset = pixelOffset * width;
      
      this.memObj = memObj;
   }
   
   static int depthToBPP(int depth){
      switch(depth){
       case 8:
	 return 1;
       case 15:
       case 16:
	 return 2;
       case 24:
	 return 3;
       case 32:
	 return 4;
      }
      throw new IllegalArgumentException("unsupported depth: "+depth);
   }
   
   protected void setNewStartAddress(int newStartAddress){
      startAddress = newStartAddress;
   }
   
   public int width(){ return width; }
   public int height(){ return height; }
   public int depth(){ return depth; }
   
   public int startAddress(){ return startAddress; }
   public int scanlineOffset(){ return scanlineOffset; }
   public int pixelOffset(){ return pixelOffset; }
   
   public Memory memObj(){ return memObj; }
   
   public String toString(){
      return toString(this);
   }
   public static String toString(PackedFramebuffer framebuffer){
      String retval = framebuffer.framebufferType()+"(";
      retval += framebuffer.width() + "x" + framebuffer.height() + "x" +framebuffer.depth();
      retval += "@0x" + toHexString(framebuffer.startAddress());
      retval += " pOff=" + framebuffer.pixelOffset();
      retval += " sOff=" + framebuffer.scanlineOffset();
      retval += " memObj@0x" + toHexString(framebuffer.memObj().getStartAddress());
      retval += ") ";
      return retval;
   }
   
   public void clear(){
      int fbi = (startAddress - memObj.getStartAddress()) ;
      for(int y=0; y<height; ++y){
	 memObj.fill16((short)0xffff, fbi/2, width*pixelOffset/2);
	 fbi += scanlineOffset;
      }
   }
   
   // This one should be overridden in derived classes!
   public String framebufferType(){
      return "PackedFramebuffer";
   }
   
   static String toHexString(int val){
      return toHexString(val, 8);
   }
   static String toHexString(int val, int width){
      StringBuffer sb = new StringBuffer(Integer.toHexString(val));
      while( sb.length() < width )
	sb.insert(0, '0');
      return sb.toString();
   }
}

