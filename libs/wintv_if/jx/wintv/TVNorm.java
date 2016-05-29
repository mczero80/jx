package jx.wintv;

public class TVNorm {
   public String name;
   public int fsc;			/* Fsc in Hz */
   public int xoff;
   public int yoff;
   public int width;
   public int height;
   public int totalWidth;
   public int totalHeight;
   
   public int btFormat;
   
   public TVNorm(String name, int fsc, int xoff, int yoff, int width, int height, int totalWidth, int totalHeight, int btFormat){
      this.name = name;
      this.fsc = fsc;
      this.xoff = xoff;
      this.yoff = yoff;
      this.width = width;
      this.height = height;
      this.totalWidth = totalWidth;
      this.totalHeight = totalHeight;
      
      this.btFormat = btFormat;
   }
   
   public boolean equals(TVNorm other){
      if (this.name.equals(other.name) && 
	  this.fsc == other.fsc &&
	  this.xoff == other.xoff &&
	  this.yoff == other.yoff &&
	  this.width == other.width &&
	  this.height == other.height &&
	  this.totalWidth == other.totalWidth &&
	  this.totalHeight == other.totalHeight)
	return true;
      else
	return false;
   }
   
   public String toString(){
      return name 
	+ " " + fsc
	+ " " + xoff
	+ " " + yoff
	+ " " + width
	+ " " + height
	+ " " + totalWidth
	+ " " + totalHeight;
   }
}

