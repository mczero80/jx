package jx.awt;

import java.awt.*;
import java.awt.peer.*;
import java.awt.image.*;
import java.awt.datatransfer.Clipboard;

public class Window extends jx.awt.Container implements WindowPeer {

    Window(jx.awt.Toolkit toolkit) {
	super(toolkit);
    }

    public void toBack(){throw new Error("not implemented");}
    public void toFront(){throw new Error("not implemented");    }
    public int handleFocusTraversalEvent(java.awt.event.KeyEvent e) {throw new Error("not implemented");    }
}

