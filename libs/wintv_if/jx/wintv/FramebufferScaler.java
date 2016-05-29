package jx.wintv;

import jx.framebuffer.PackedFramebuffer;
import jx.zero.Debug;

public class FramebufferScaler {
   BT878GeometryInterface bt;
   
   public FramebufferScaler(BT878GeometryInterface bt){
      this.bt = bt;
   }
   
   public FramebufferScaler(CaptureDevice card){
      this(card.getGeometryInterface());
   }
   
   /* 
    * Want complete video picture scaled down to fit into the given
    * framebufer. 
    */
   
   public void setScalingOdd(TVNorm tvnorm, PackedFramebuffer framebuffer, boolean twoFields){
      setScaling(true, tvnorm, framebuffer, twoFields);
   }
   public void setScalingOdd(TVNorm tvnorm, PackedFramebuffer framebuffer){
      setScaling(true, tvnorm, framebuffer, false);
   }
   public void setScalingEven(TVNorm tvnorm, PackedFramebuffer framebuffer, boolean twoFields){
      setScaling(false, tvnorm, framebuffer, twoFields);
   }
   public void setScalingEven(TVNorm tvnorm, PackedFramebuffer framebuffer){
      setScaling(false, tvnorm, framebuffer, false);
   }
   
   public void setScaling(boolean oddField, 
			  TVNorm tvnorm, 
			  PackedFramebuffer framebuffer,
			  boolean twoFields){
      int inputWidth = tvnorm.width;
      int inputTotalWidth = tvnorm.totalWidth;
      int outputWidth = framebuffer.width();
      
      int inputHeight = tvnorm.height;
      int inputTotalHeight = tvnorm.totalHeight;
      int outputHeight = framebuffer.height();
      
      setScaling(oddField,
		 tvnorm.totalWidth, tvnorm.width, tvnorm.xoff, 
		 framebuffer.width(),
		 tvnorm.totalHeight, tvnorm.height, tvnorm.yoff,
		 framebuffer.height(), 
		 twoFields);
   }
   
   public void setScaling(boolean oddField,
			  int iTotalWidth, int iWidth, int ixoff ,int oWidth,
			  int iTotalHeight, int iHeight, int iyoff, int oHeight,
			  boolean twoFields ){
      
      int scaledTotal = oWidth * iTotalWidth / iWidth;
      int hscale  = calcHScale(iTotalWidth, scaledTotal);
      int hdelay  = (ixoff * scaledTotal / iTotalWidth) & 0x3fe;
      int hactive = oWidth;
      
      int vscale, vdelay, vactive;
      
      oHeight *= 2;
      if( iHeight < oHeight ){
	 Debug.out.println("WARNING: FramebufferScaler: cannot upscale picture!");
	 oHeight = iHeight;
      }
      // FIXME: honor twoFields (but it works already!)
      vscale = calcVScale(iHeight, oHeight);
      vdelay = iyoff;
      vactive = iHeight;
      
      if( oddField )
	bt.setGeometryOdd (hscale, hdelay, hactive, vscale, vdelay, vactive);
      else
	bt.setGeometryEven(hscale, hdelay, hactive, vscale, vdelay, vactive);
   }
   
   final private int calcHScale(int t, int p){
      // seems al little complicated due the fact that no floating point ops are used.
      return t*4096/p-4096;
   }
   
   final private int calcVScale(int total, int wanted){
      return (0x10000 - total*512 / wanted + 512) & 0x1fff;
   }
}
