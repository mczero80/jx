package jx.framebuffer;

public class ClippingRectangle implements Cloneable {
   int xmin, ymin;
   int xmax, ymax;
   
   public ClippingRectangle(int x1, int y1, int x2, int y2){
      xmin = Math.min(x1, x2);
      ymin = Math.min(y1, y2);
      xmax = Math.max(x1, x2);
      ymax = Math.max(y1, y2);
   }
   
   public int xmin(){ return xmin; }
   public int ymin(){ return ymin; }
   public int xmax(){ return xmax; }
   public int ymax(){ return ymax; }
   
   public Object clone() {
      try {
	 return super.clone();
      }
      catch(CloneNotSupportedException e){}
      return null;
   }   
}

