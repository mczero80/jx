package jx.devices.mga;

import jx.zero.*;
import jx.framebuffer.PackedFramebuffer;
import jx.devices.Device;
import jx.devices.fb.FramebufferDevice;

public interface MGA200 extends Portal, FramebufferDevice {
   
   /**
    * Return the description of the graphic framebuffer memory.
    */
   PackedFramebuffer getFramebuffer();
   
   /*
    * Do some simple test on the framebuffer.
    */
   public void testFramebuffer();
}

