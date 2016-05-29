package jx.keyboard;

import jx.zero.debug.*;

public class Scancodes {

    final static int LSHIFT = 42;
    final static int RSHIFT = 54;
    final static int CONTROL = 0x1d;
    final static int ALT = 0x38;
  
    final static int F1  = 0x3b;
    final static int F2  = 0x3c;
    final static int F3  = 0x3d;
    final static int F4  = 0x3e;
    final static int F5  = 0x3f;
    final static int F6  = 0x40;
    final static int F7  = 0x41;
    final static int F8  = 0x42;
    final static int F9  = 0x43;
    final static int F10 = 0x44;

    final static int DEL = 83;

    final static int NUMLOCK = 0x45;
    final static int SCROLLLOCK = 0x46;
    final static int CAPSLOCK = 0x3a;


    final static char ASCII_ESC = 27;
    final static char ASCII_BS  = 8;
    final static char ASCII_TAB = '\t';
    final static char ASCII_LF = '\n';

    final char scanTable[] =  {' ', ASCII_ESC, '1', '2', '3', '4', '5', '6', '7', '8',
			      '9', '0', '-', '=', ASCII_BS,  ASCII_TAB, 'q', 'w', 'e', 'r',
			      't', 'y', 'u', 'i', 'o', 'p', '[', ']', ASCII_LF,  ' ',
			      'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';',
			      '\'', '`', ' ', '\\', 'z', 'x', 'c', 'v', 'b', 'n',
			      'm', ',', '.', '/', ' ', ' ', ' ', ' ', ' '};
    final char shiftTable [] = {' ', ASCII_ESC, '!', '@', '#', '$', '%', '^', '&', '*',
				   '(', ')', '_', '+', ' ', ' ', 'Q', 'W', 'E', 'R',
				   'T', 'Y', 'U', 'I', 'O', 'P', '{', '}', ASCII_LF,  ' ',
				   'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ':',
				   '\"', '~', ' ', '|', 'Z', 'X', 'C', 'V', 'B', 'N',
				   'M', '<', '>', '?', ' ', ' ', ' ', ' ', ' '};

    private boolean shiftDown;
    private boolean controlDown;
    private boolean altDown;

    DebugPrintStream out;

    public Scancodes(DebugPrintStream out) {
	this.out = out;
    }

    /**
     * Translate scancode to character
     * Tracks the state of the special key "shift"
     * @return character that corresponds to scancode
     */
    public int translate(int scancode) {
	scancode &= 0xff;
	if ((scancode & 0x80) != 0) {
	    /* key release */
	    scancode &= 0x7f;
	    if (scancode == LSHIFT ||scancode == RSHIFT ) shiftDown = false;
	    else if (scancode == CONTROL) controlDown = false;
	    else if (scancode == ALT) altDown = false;
	    return -1; // no character, only release of shift key
	} else {
	    /* key press */
	    if (scancode == LSHIFT ||scancode == RSHIFT ) {
		shiftDown = true;
		return -1;// no character, only press of shift key
	    }
	    if (scancode == CONTROL) {
		controlDown = true;
		return -1;// no character, only press of shift key
	    }
	    if (DebugConf.debug) {
		if (scancode >= scanTable.length) {
		    out.println("scancode number too large: " + scancode);
		    return -1;
		}
	    }
	    return  shiftDown ? shiftTable[scancode] : scanTable[scancode];
	}
    }
    
    public int functionKey(int scancode) {
	if (scancode >= F1 && scancode <= F10) return scancode - F1 + 1;
	return 0;
    } 

    public boolean isReset(int scancode) {
	//	return controlDown && altDown && (scancode == DEL);
	// use F1 as reset key -- faster for interrupt handler
	//return scancode == F1; 
	return false;
    } 

    public boolean isNumLock(int scancode) {
	return scancode == NUMLOCK; 
    } 

}
