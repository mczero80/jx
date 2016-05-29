package test.jx.awt;

import java.awt.*;

public class ScreenBackground
    extends Frame {
    
    private String picName = "backgrnd.ppm";
    
    
    public ScreenBackground() {}
    
    
    
    public void show() {
	// Show waiting window
	Frame wf = new Frame("Loading screen...");
	wf.setSize(400, 0);
	wf.setLocation(200, 200);
	wf.show();
	// initialize window
	Toolkit tk = Toolkit.getDefaultToolkit();
	Dimension scr = tk.getScreenSize();
	Image bckgnd = tk.getImage(picName);
	// get new Canvas showing picture in screen size
	ImageCanvas ic1 = new ImageCanvas(bckgnd,
					  scr.width,
					  scr.height);
	add(ic1);
	pack();
	// hide border
	((jx.awt.peer.JXFramePeer) getPeer()).showBorder(false);
	// set size to whole screen
	setBounds(0, 0, scr.width, scr.height);
	// close waiting window and show frame
	wf.dispose();
	super.show();
    }
}
