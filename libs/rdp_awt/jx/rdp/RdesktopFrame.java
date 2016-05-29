package jx.rdp;

import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.awt.event.*;
import jx.rdp.Rdp;

public class RdesktopFrame extends Frame {
    private RdesktopCanvas canvas = null;
    private Rdp rdp = null;

    public RdesktopFrame(int width, int height) {
	super();
	this.canvas = new RdesktopCanvas(width, height);
	add(this.canvas);
	setTitle("Rdesktop for Java");
	setLocation(20, 20);
	pack();
	//addWindowListener(new RdesktopWindowAdapter());
	setResizable(false);
    }
    
    public RdesktopCanvas getCanvas() {
	return this.canvas;
    }

    public void registerCommLayer(Rdp rdp) {
	this.rdp = rdp;
	canvas.registerCommLayer(rdp);
    }
    
    public void registerKeyboard(KeyCode keys) {
	//canvas.registerKeyboard(keys);
    }
    
    /*class RdesktopWindowAdapter extends WindowAdapter {
	
	public void windowClosing(WindowEvent e) {
	    hide();
	    dispose();
	    System.exit(0);
	}
	}*/
}
