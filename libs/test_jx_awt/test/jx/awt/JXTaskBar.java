package test.jx.awt;

import java.awt.*;
import java.awt.event.*;
import jx.awt.peer.*;


public class JXTaskBar 
    implements MouseListener {

    final private Frame taskFrame;
    //private MenuItem rebootItem;
    //private MenuItem infoItem;
    private Button taskButton;
    private Dimension resolution;
    final private PopupMenu taskMenu;
    

    public JXTaskBar() {
	taskFrame = new Frame();
	taskButton = new Button("Start");
	taskMenu = new PopupMenu("Start Menu");

	taskButton.add(taskMenu);
	taskButton.addMouseListener(this);
	taskFrame.setLayout(new BorderLayout());
	taskFrame.add(taskButton, BorderLayout.WEST);
    }
    
    public void mouseReleased(MouseEvent e) {
	//System.out.println("got popup event.");
	taskMenu.show(taskButton, 0, -1);
    }

    public void mousePressed(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}

    public void add(MenuItem m) {
	taskMenu.add(m);
    }

    public void addSeparator() {
	taskMenu.addSeparator();
    }

    public void show() {
	taskFrame.pack();
	// hide frame borders
	JXFramePeer peer = ((JXFramePeer) taskFrame.getPeer());
	peer.showBorder(false);
	Rectangle bounds = taskFrame.getBounds();
	resolution = Toolkit.getDefaultToolkit().getScreenSize();
	System.out.println("Screen resolution is " + resolution.width + "x" + resolution.height);
	// place taskbar in lower screen border
	taskFrame.setBounds(0, resolution.height - 1 - bounds.height,
			    resolution.width - 1, bounds.height);
	taskFrame.show();
    }
}
