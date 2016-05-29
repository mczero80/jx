package jx.awt;

import java.awt.*;
import java.awt.image.*;

public class Frame extends jx.awt.Window implements java.awt.peer.FramePeer {

    Frame(jx.awt.Toolkit toolkit) {
	super(toolkit);
    }

    public void setIconImage(Image image){throw new Error("not implemented");}
    public void setMenuBar(MenuBar mb) {throw new Error("not implemented");}
    public void setResizable(boolean resizable){throw new Error("not implemented");}
    public void setTitle(String title){throw new Error("not implemented");}
    public void setState(int s){throw new Error("not implemented");}
    public int getState() {throw new Error("not implemented");}
}
