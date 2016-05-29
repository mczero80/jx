package jx.rdp; 

import java.awt.*;
import java.awt.event.*;

public class KeyCode {
	
    /**
     * X scancodes for the printable keys of a standard 102 key MF-II Keyboard
     */
    /*    private final int[] main_key_scan_qwerty = {
	0x29, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D,
	0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B,
	0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x2B,
	0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35,
	0x56};
    
    private String[] layout = null;
    
    public KeyCode(String[] layout) {
	this.layout = layout;
    }

    public int getScancode(KeyEvent e) {
	
	if((e.getKeyChar() & KeyEvent.CHAR_UNDEFINED) == KeyEvent.CHAR_UNDEFINED) {
	    return translateNonTypeable(e);
	} else {
	    char type = e.getKeyChar();
	    
	    for(int i=0; i<layout.length; i++) {
		for(int j = 0; j < layout[i].length(); j++) {
		    if(layout[i].charAt(j) == type) {
			return main_key_scan_qwerty[i];
		    }
		}
	    }
	    
	    return translateNonTypeable(e);
	}
    }
    
    private int translateNonTypeable(KeyEvent e) {
	int i = e.getKeyCode();
	switch(i) {
	    
	case(KeyEvent.VK_ALT):
	    return 0x38;
	    
	case(KeyEvent.VK_ALT_GRAPH):
	    return (0x38|0x80);
	    
	case(KeyEvent.VK_ENTER):
	    return 0x1c;
	    
	case(KeyEvent.VK_BACK_SPACE):
	    return  0x0e;
	    
	case(KeyEvent.VK_CONTROL):
	    return 0x1d;
	    
	case(KeyEvent.VK_TAB):
	    return 0x0f;
	    
	case(KeyEvent.VK_SHIFT):
	    return 0x2a;
	    
	case(KeyEvent.VK_CAPS_LOCK):
	    return 0x3a;
	    
	case(KeyEvent.VK_SPACE):
	    return 0x39;
	    
	case(KeyEvent.VK_ESCAPE):
	    return 0x01;
	    
	case(KeyEvent.VK_PAGE_UP):
	    return (0x49 | 0x80);
	    
	case(KeyEvent.VK_PAGE_DOWN):
	    return (0x51 | 0x80);
	    
	case(KeyEvent.VK_END):
	    return (0x4f | 0x80);
	    
	case(KeyEvent.VK_HOME):
	    return (0x47 | 0x80);
	    
	case(KeyEvent.VK_LEFT):
	    return (0x4b | 0x80);
	    
	case(KeyEvent.VK_UP):
	    return (0x48 | 0x80);
	    
	case(KeyEvent.VK_RIGHT):
	    return (0x4d | 0x80);
	    
	case(KeyEvent.VK_DOWN):
	    return (0x50 | 0x80);
	    
	case(KeyEvent.VK_PRINTSCREEN):
	    return (0x37 | 0x80);
	    
	case(KeyEvent.VK_INSERT):
	    return (0x52 | 0x80);
	
	case(KeyEvent.VK_F1):
	    return 0x3b;

	case(KeyEvent.VK_F2):
	    return 0x3c;

	case(KeyEvent.VK_F3):
	    return 0x3d;

	case(KeyEvent.VK_F4):
	    return 0x3e;
	    
	case(KeyEvent.VK_F5):
	    return 0x3f;

	case(KeyEvent.VK_F6):
	    return 0x40;

	case(KeyEvent.VK_F7):
	    return 0x41;

	case(KeyEvent.VK_F8):
	    return 0x42;

	case(KeyEvent.VK_F9):
	    return 0x43;

	case(KeyEvent.VK_F10):
	    return 0x44;

	case(KeyEvent.VK_F11):
	    return 0x57;

	case(KeyEvent.VK_F12):
	    return 0x58;
	    
	case(KeyEvent.VK_NUM_LOCK):
	    return 0x45;
	    
	case(KeyEvent.VK_DELETE):
	    return (0x53 | 0x80);

	default:
	    return 0;
	}
	}*/
	
}
