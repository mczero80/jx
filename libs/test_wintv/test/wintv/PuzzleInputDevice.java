package test.wintv;

public interface PuzzleInputDevice {
   int KEY_UP		= 1;
   int KEY_DOWN 	= 2;
   int KEY_LEFT		= 3;
   int KEY_RIGHT	= 4;
   int KEY_AUTOPLAY	= 5;
   int KEY_RESET	= 6;
     
   int getKey();
}
