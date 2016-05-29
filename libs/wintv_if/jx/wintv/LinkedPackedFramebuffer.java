package jx.wintv;

import jx.framebuffer.PackedFramebuffer;
import jx.zero.Debug;
import jx.zero.Memory;

/*
 * Note: The Compiler does't support multidimensional arrays currently.
 * Therefore a multidimensional array is simulated via the getIndex method.
 */
public class LinkedPackedFramebuffer implements PackedFramebuffer {
   PackedFramebuffer framebuffer[];
   int nx, ny;
   
   // all framebuffers must comply with this
   int depth = -1;
   int bpp   = -1;	// byte per pixel
   
   int framebufferWidth[];
   int framebufferHeight[];
   
   public LinkedPackedFramebuffer(int nx, int ny){
      this.nx = nx;
      this.ny = ny;
      
      framebuffer = new PackedFramebuffer[nx*ny];
      
      framebufferWidth = new int[nx];
      for(int i=0; i<nx; ++i)
	framebufferWidth[i] = -1;
      
      framebufferHeight = new int[ny];
      for(int i=0; i<ny; ++i)
	framebufferHeight[i] = -1;
   }
   
   public PackedFramebuffer getFramebuffer(int x, int y){
      return framebuffer[getIndex(x, y)];
   }
   
   public PackedFramebuffer getFramebuffer(int i){
      return framebuffer[i];
   }
   
   public void setFramebuffer(int x, int y, PackedFramebuffer fb){
      Debug.assert( x < nx, "argument x out of range");
      Debug.assert( y < ny, "argument x out of range");
      
      if( depth < 0 ){
	 depth = fb.depth();
	 bpp   = fb.pixelOffset();
      }
      else {
	 if( depth != fb.depth() || bpp != fb.pixelOffset() )
	   throw new IncompatibleFramebuffer("color format");
      }
      
      if( framebufferWidth[x] < 0 )
	framebufferWidth[x] = fb.width();
      else if ( framebufferWidth[x] != fb.width() )
	throw new IncompatibleFramebuffer("width != "+framebufferWidth[x]);
      
      if( framebufferHeight[y] < 0 )
	framebufferHeight[y] = fb.height();
      else if( framebufferHeight[y] != fb.height() )
	throw new IncompatibleFramebuffer("height != "+framebufferHeight[y]);
      
      framebuffer[getIndex(x, y)] = fb;
   }
   
   final int getIndex(int x, int y){
      return x * ny + y;
   }
   
   public int getNHoriz(){ return nx; }
   public int getNVert() { return ny; }
   
   public int getWidth(int x) { return framebufferWidth[x]; }
   public int getHeight(int y){ return framebufferHeight[y]; }
      
   
   /* compatibility methods for PackedFramebuffer */
   /* NOTE: some methods have changed semantics! */
   
   public void clear(){
      for(int i=0; i<framebuffer.length; ++i)
	if( framebuffer[i] != null )
	  framebuffer[i].clear();
   }
   
   // return total width of all subframebuffers
   public int width(){
      int retval = 0;
      for(int i=0; i<nx; ++i)
	retval += framebufferWidth[i];
      return retval;
   }
   
   // return total height of all subframebuffers
   public int height(){
      int retval = 0;
      for(int i=0; i<ny; ++i)
	retval += framebufferHeight[i];
      return retval;
   }
   
   public int depth(){ return depth; }
   public int pixelOffset(){ return bpp; }
   public String framebufferType(){ return "LinkedPackedFramebuffer"; }
   
   public String toString(){
      return framebufferType() + "(" + nx + ", " + ny + ", " +width() + "x" + height() + "x" +depth()+")";
   }
   
   public String subbuffersString(){
      StringBuffer retval = new StringBuffer();
      PackedFramebuffer f;
      for(int y=0; y<ny; ++y){
	 for(int x=0; x<nx; ++x){
	    f = framebuffer[getIndex(x,y)];
	    if( f == null )
	      retval.append(Hex.toHexString(0));
	    else
	      retval.append(Hex.toHexString(f.startAddress()));
	    if( x != nx-1 )
	      retval.append("   ");
	 }
	 retval.append("\n");
      }
      return retval.toString();
   }
   
   // this methods have no real usefull semantics
   public int scanlineOffset(){ throw new NotImpl(); }
   public int startAddress(){ throw new NotImpl(); }
   public Memory memObj(){ throw new NotImpl(); }
}

