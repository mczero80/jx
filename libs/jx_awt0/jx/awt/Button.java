package jx.awt;

import java.awt.*;

class Button extends jx.awt.Component implements java.awt.peer.ButtonPeer {

    Dimension size;


    Button(jx.awt.Toolkit toolkit) {
	super(toolkit);
	size = new Dimension(10,20);
    }

    public void handleEvent(java.awt.AWTEvent evt) {
    }

    public void coalescePaintEvent(java.awt.event.PaintEvent e) {throw new Error("not implemented");}
    
    public void setLabel(java.lang.String l){throw new Error("not implemented");}

    /* overrides Component.preferredSize */
    public  Dimension preferredSize(){
	Dbg.msg("Button.preferredSize "+this);
	return size;
    }

}
