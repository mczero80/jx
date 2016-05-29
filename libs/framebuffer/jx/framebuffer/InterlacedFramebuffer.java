package jx.framebuffer;

import jx.zero.*;
import jx.zero.debug.*;

public class InterlacedFramebuffer extends PackedFramebufferImpl implements ClippingTransformer {
   int fieldno;
   
   public InterlacedFramebuffer(PackedFramebuffer framebuffer, int field){
      /*
       * if the height of the original framebuffer is odd, the first field
       * gets the surplus line.
       */
      super(
	    framebuffer.width(),
	    framebuffer.height()/2 + field*framebuffer.height()%2,
	    framebuffer.depth(),
	    
	    framebuffer.startAddress() + field*framebuffer.scanlineOffset(),
	    framebuffer.pixelOffset(),
	    framebuffer.scanlineOffset()*2,
	    
	    framebuffer.memObj()
	    );
      
      this.fieldno = field;
      Debug.assert(field == 0 || field == 1, "field id out of range");
   }
   
   public ClippingRectangle []transformClippings(ClippingRectangle rects[]){
      Debug.out.println(framebufferType()+": translating clippings: fieldno="+fieldno);

      int n= rects.length;
      ClippingRectangle retval[] = new ClippingRectangle[n];
      ClippingRectangle r;
      for(int i=0; i<n; ++i){
	 r = rects[i];
	 if( r.ymax-r.ymin == 0 ){	/* rectangles with 1 pixel height must be handled specially  */
	    // FIXME
	    if( (r.ymax%2) == fieldno )
	      retval[i] = new ClippingRectangle(r.xmin, r.ymin/2, r.xmax, r.ymax/2);
	    else
	      retval[i] = null;
	    continue;
	 }
	 if( fieldno == 0 ){		/* odd field */
	    retval[i] = 
	      new ClippingRectangle(r.xmin, (r.ymin+1)/2, 
				    r.xmax, r.ymax/2 );
	 }
	 else {				/* even field */
	    retval[i] = 
	      new ClippingRectangle(r.xmin, r.ymin/2,
				    r.xmax, (r.ymax-1)/2 );
	 }
      }
      return retval;
   }
   
   public String framebufferType(){
      return "InterlacedFramebuffer";
   }
}
