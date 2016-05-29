package test.jx.awt;

import java.awt.*;
import java.awt.event.*;

public class DemoFrame
    extends Frame {
    
    private ListenerDisplay ld;


    public DemoFrame() {
	this("", null);
    }
    
    public DemoFrame(String title, ListenerDisplay ld) {
	super(title);
	this.ld = ld;
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    dispose();
		}
	    });
	addWindowListener(ld);
	addContainerListener(ld);
    }
    
    
    
    

    public void show() {
	center();
	super.show();
    }

    private void center() {
	Rectangle r = getBounds();
	Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
	setBounds((s.width - r.width) / 2, (s.height - r.height)/ 2,
		  r.width, r.height);
    }

}
