package jx.wintv;

import jx.framebuffer.PackedFramebuffer;
import jx.framebuffer.ClippingRectangle;
import jx.zero.Debug;
import jx.zero.debug.DebugPrintStream;


class ClippingEdge {
   int ymin, xmin, ymax;
   
   public ClippingEdge(int ymin, int xmin, int ymax){
      this.ymin = ymin;
      this.xmin = xmin;
      this.ymax = ymax;
   }
   
   public String toString(){
      return "ClippingEdge("+ymin+", "+xmin+", "+ymax+")";
   }
}
