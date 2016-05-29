package test.wintv;

import jx.devices.Keyboard;
import jx.zero.Debug;

public class PuzzleKeybdInput implements PuzzleInputDevice {
   Keyboard keybd;
   
   public PuzzleKeybdInput(Keyboard keybd){
      this.keybd = keybd;
   }
   public int getKey(){
      int key;
      while(true){
//	 Debug.out.println("Waiting for key...");
	 key = keybd.getc();
//	 Debug.out.println("Key pressed: "+key);
	 
/*
	 switch( key ){
	  case 'h':
	    return KEY_LEFT;
	  case 'l':
	    return KEY_RIGHT;
	  case 'j':
	    return KEY_DOWN;
	  case 'k':
	    return KEY_UP;
	  case 'r' :
	    return KEY_RESET;
	  case 'a':
	    return KEY_AUTOPLAY;
	 }
*/
	 if (key == 'h') return KEY_LEFT;
	 else if (key == 'j') return KEY_DOWN;
	 else if (key == 'k') return KEY_UP;
	 else if (key == 'r') return KEY_RESET;
	 else if (key == 'a') return KEY_AUTOPLAY;
	 
      }
   }
}
