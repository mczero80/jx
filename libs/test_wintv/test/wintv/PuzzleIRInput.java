package test.wintv;

import jx.framebuffer.*;
import jx.wintv.*;
import jx.zero.*;
import jx.zero.debug.*;

public class PuzzleIRInput implements PuzzleInputDevice {
   IRReceiver ir;
   
   RCKey key;
   public PuzzleIRInput(IRReceiver ir){
      this.ir = ir;
   }
   
   public int getKey(){
      while(true){
	 key = ir.poll();
	 if (key == null) continue;
	 if( key.equals(RCKeys.key_ChUp) || key.equals(RCKeys.key_2) )
	   return KEY_UP;
	 if( key.equals(RCKeys.key_ChDown) || key.equals(RCKeys.key_8) )
	   return KEY_DOWN;
	 if( key.equals(RCKeys.key_VolDown) || key.equals(RCKeys.key_4) )
	   return KEY_LEFT;
	 if( key.equals(RCKeys.key_VolUp) || key.equals(RCKeys.key_6) )
	   return KEY_RIGHT;
	 if( key.equals(RCKeys.key_5) || key.equals(RCKeys.key_FullScreen) )
	    return KEY_RESET;
	 if( key.equals(RCKeys.key_Reserved) )
	   return KEY_AUTOPLAY;
      }
   }
}
