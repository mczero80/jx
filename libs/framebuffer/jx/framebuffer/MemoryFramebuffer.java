package jx.framebuffer;

import jx.zero.*;

public class MemoryFramebuffer extends PackedFramebufferImpl {
   
   public MemoryFramebuffer(int width, int height, int depth){
      this((MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager"), 
	   width, height, depth);
   }

   public MemoryFramebuffer(DeviceMemory mem, int width, int height, int depth){
      super(width, height, depth, 0, 0, 0, mem);
   }
   
   public MemoryFramebuffer(MemoryManager memMgr, int width, int height, int depth){
      super(width, height, depth, 
	    0, 0, 0, 
	    memMgr.allocAligned(height*width*depthToBPP(depth), 4));
   }
   
   public String framebufferType(){
      return "MemoryFramebuffer";
   }
}

