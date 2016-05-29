package test.wintv.file;

import jx.zero.*;
import jx.zero.debug.*;
import jx.framebuffer.*;
import jx.wintv.*;

import jx.bio.BlockIO;

public class TestNotifier implements Notifier, Service {
    static Naming naming = InitialNaming.getInitialNaming();
   PackedFramebuffer framebuffer;
   PFBTools tool;
   int x = 0;
   int y = 0;
   short color = (short)0xff00;
   int w;
   int h;
    BlockIO bio;
    PackedFramebuffer visframebuffer;
    Memory sectors;
    MemoryManager memoryManager;

    int width; 
    int height; 
    int viswidth;
    int bytesPerPixel = 2;
    int bytesPerImage;
    int sectorsPerImage;
    int sectorNumber = 1;

   public TestNotifier(PackedFramebuffer framebuffer, BlockIO bio, PackedFramebuffer visframebuffer){
       this.bio = bio;
      this.framebuffer = framebuffer;
      this.visframebuffer =visframebuffer  ;

      this.memoryManager = (MemoryManager)naming.lookup("MemoryManager");

      tool = new PFBTools(framebuffer);
      w = framebuffer.width();
      h = framebuffer.height();
      Debug.out.println("FROM: "+framebuffer.width()+", TO: "+visframebuffer.width());
      width=framebuffer.width();
      height=framebuffer.height();
      viswidth =visframebuffer.width();
      
      bytesPerImage = height * width * bytesPerPixel;
      sectorsPerImage = (bytesPerImage + bio.getSectorSize() - 1)/bio.getSectorSize();
      sectors = memoryManager.alloc(sectorsPerImage*bio.getSectorSize());
   
   }
   
   public void notifyEvent(){
       //       Debug.out.println(framebuffer.memObj().get16(0));
       Memory to   = visframebuffer.memObj();
       Memory from = framebuffer.memObj();
       for(int i=0; i<height; i++) {
	   to.copyFromMemory(from, i*width*bytesPerPixel, i*viswidth*bytesPerPixel, width*bytesPerPixel);
       }
       /*
       sectors.copyFromMemory(from, 0, 0, bytesPerImage);       
       bio.writeSectors(sectorNumber, sectorsPerImage, sectors, true);
       sectorNumber += sectorsPerImage;
       */
   }
   
}
