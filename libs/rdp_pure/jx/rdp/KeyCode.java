package jx.rdp;

import jx.wm.Keycode; 
import jx.zero.Debug;

public class KeyCode {
    
    private int[] layout = null;
    
    public KeyCode(int[] layout) {
	this.layout = layout;
    }

    public int getScancode(Keycode e) {
	return layout[e.getValue()];
    }
}
