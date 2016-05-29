package jx.awt;

import java.awt.*;
import java.awt.peer.*;
import java.awt.image.*;
import java.awt.datatransfer.Clipboard;

import java.awt.*;

public class Component implements java.awt.peer.ComponentPeer, java.awt.peer.LightweightPeer{
    jx.awt.Toolkit toolkit;

    int x;
    int y;
    int width;
    int height;

    Component(jx.awt.Toolkit toolkit) {
	this.toolkit = toolkit;
    }

    public  int checkImage(Image img, int width, int height, 
			   ImageObserver ob){throw new Error("not implemented");}
    public  Image createImage(ImageProducer prod){throw new Error("not implemented");}
    public  Image createImage(int width, int height){throw new Error("not implemented");}
    public  void disable(){throw new Error("not implemented");}
    public  void dispose(){throw new Error("not implemented");}
    public  void enable(){throw new Error("not implemented");}
    public  ColorModel getColorModel(){throw new Error("not implemented");}
    public  FontMetrics getFontMetrics(Font f){throw new Error("not implemented");}
    public  Graphics getGraphics(){throw new Error("not implemented");}
    public  Point getLocationOnScreen(){throw new Error("not implemented");}
    public  Dimension getMinimumSize(){throw new Error("not implemented");}
    public  Dimension getPreferredSize(){
	throw new Error("not implemented");
    }
    public  java.awt.Toolkit getToolkit(){
	return toolkit;
    }
    public  void handleEvent(AWTEvent e){throw new Error("not implemented");}
    public  void hide(){setVisible(false);}
    public  boolean isFocusTraversable(){throw new Error("not implemented");}
    public  Dimension minimumSize(){throw new Error("not implemented");}

    public  Dimension preferredSize(){
	Dbg.msg("Component.preferredSize "+this);
	throw new Error("Component has no size information");
    }

    public  void paint(Graphics graphics){throw new Error("not implemented");}
    public  boolean prepareImage(Image img, int width, int height,
				 ImageObserver ob){throw new Error("not implemented");}
    public  void print(Graphics graphics){throw new Error("not implemented");}
    public  void repaint(long tm, int x, int y, int width, int height){throw new Error("not implemented");}
    public  void requestFocus(){throw new Error("not implemented");}
    public  void reshape(int x, int y, int width, int height){throw new Error("not implemented");}
    public  void setBackground(Color color){throw new Error("not implemented");}

    public  void setBounds(int x, int y, int width, int height){
	Dbg.msg("Component.setBounds "+this);
	this.x = x;
	this.y = y;
	this.width = width;
	this. height = height;
    }

    public  void setCursor(Cursor cursor){throw new Error("not implemented");}
    public  void setEnabled(boolean enabled){throw new Error("not implemented");}
    public  void setFont(Font font){throw new Error("not implemented");}
    public  void setForeground(Color color){throw new Error("not implemented");}

    public  void setVisible(boolean visible){
	Dbg.msg("Component.setVisible "+this);
    }

    public  void show() {
	setVisible(true);
    }

    public void coalescePaintEvent(java.awt.event.PaintEvent e) {throw new Error("not implemented");}
    public java.awt.GraphicsConfiguration  getGraphicsConfiguration(){throw new Error("not implemented");}
}

