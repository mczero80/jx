package jx.framebuffer;
import jx.zero.Debug;

public class SubFramebuffer extends PackedFramebufferImpl implements ClippingTransformer {
   protected int xoff, yoff;
   protected int width, height;
   
   public SubFramebuffer(PackedFramebuffer framebuffer,
		  int x, int y, int width, int height){
      super(
	    Math.max(Math.min(framebuffer.width()-x, width), 0),
	    Math.max(Math.min(framebuffer.height()-y, height), 0),
	    framebuffer.depth(),
	    framebuffer.startAddress() + y*framebuffer.scanlineOffset() + x*framebuffer.pixelOffset(),
	    framebuffer.pixelOffset(),
	    framebuffer.scanlineOffset(),
	    framebuffer.memObj()
	    );
      
      this.xoff = x;
      this.yoff = y;
      this.width = width;
      this.height = height;
      
      /* Theese assertions should be at the beginning of the method, but Java forbids this. */
      Debug.assert(x >= 0 && y >= 0 && width >= 0 && height >= 0, "x, y, width or height is negative!");
      Debug.assert( x+width <= framebuffer.width(), "right edge is outside parent buffer ");
      Debug.assert( y+height <= framebuffer.height(), "lower edge is outside parent buffer");
   }
   
   public ClippingRectangle []transformClippings(ClippingRectangle rects[]){
      Debug.out.println(framebufferType()+": translating clippings: delta=("+ -xoff + ", "+ -yoff + ")");

      ClippingRectangle retval[] = new ClippingRectangle[rects.length];
      
      ClippingRectangle r;
      for(int i=0; i<rects.length; ++i){
	 r = rects[i];
	 if( r != null )
	   retval[i] = new ClippingRectangle(r.xmin-xoff, r.ymin-yoff, r.xmax-xoff, r.ymax-yoff);
      }
      return retval;
   }
   
   public String framebufferType(){
      return "SubFramebuffer";
   }
}

