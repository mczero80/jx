package test.wintv;

import jx.zero.*;
import jx.zero.debug.*;
import jx.framebuffer.*;
import jx.wintv.*;

public class TestNotifier implements Notifier, Service {
    static Naming naming = InitialNaming.getInitialNaming();
   PackedFramebuffer framebuffer;
   PFBTools tool;
   int x = 0;
   int y = 0;
   short color = (short)0xff00;
   int w;
   int h;
   
   public TestNotifier(PackedFramebuffer framebuffer){
      this.framebuffer = framebuffer;
      
      tool = new PFBTools(framebuffer);
      w = framebuffer.width();
      h = framebuffer.height();
      
      naming.registerPortal(this, "test/wintv/TestNotifier");
   }
   
   public void notifyEvent(){
      setNextPixel(color);
      if( x == 0 && y == 0 )
	color = (short) ~color;
   }
   
   public void setNextPixel(short color){
      framebuffer.memObj().set16(tool.getPixelOffset16(x, y), color);
      if( ++x >= w ){
	 ++y;
	 x = 0;
      }
      if( y >= h )
	 y = 0;
   }
   
}
