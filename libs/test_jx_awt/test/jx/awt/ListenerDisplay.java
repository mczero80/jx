package test.jx.awt;


import java.awt.*;
import java.awt.event.*;
import jx.awt.*;


public class ListenerDisplay 
    implements ActionListener, AdjustmentListener, ComponentListener,
    ContainerListener, FocusListener, ItemListener, KeyListener,
    MouseListener, MouseMotionListener, TextListener, WindowListener {

    final private String command = "§$&%";
    final private TextArea ta;
    private Button b;
    private Frame f;

    public ListenerDisplay() {
	ta = new TextArea(15, 50);
	ta.setEditable(false);
	b = new Button("reset output");
	b.setActionCommand(command);
	b.addActionListener(this);
	ExtendedPanel p1 = new ExtendedPanel(ExtendedPanel.BORDER_ETCHED, null);
	p1.setLayout(new BorderLayout());
	Panel p2 = new Panel();
	Panel p3 = new Panel();
	p2.add(ta);
	p3.add(b);
	p1.add(p2, BorderLayout.NORTH);
	p1.add(p3, BorderLayout.SOUTH);

	f = new Frame("Listener output");
	f.add(p1);
	f.pack();

	Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
	Rectangle r = f.getBounds();
	f.setBounds(s.width - r.width, 20, r.width, r.height);
	f.setResizable(false);
    }

    

    public void show() {
	f.show();
    }

    public void message(String text) {
	ta.append(text + "\n");
    }

    /****************************************
     * listener implementations             *
     ****************************************/

    public void actionPerformed(ActionEvent e) {
	//System.out.println("command is " + e.getActionCommand());
	//System.out.println("own command is " + command);
	if (e.getActionCommand() != null &&
	    e.getActionCommand().equals(command))
	    ta.setText("");
	else
	    /*	    message("ActionListener: got Event 'actionPerformed' from " +
		    e.getSource() +
		    ": command = " + e.getActionCommand() + ", modifiers = " +
		    e.getModifiers());*/
	    message("ActionListener: got Event 'actionPerformed'");
    }

    public void componentHidden(ComponentEvent e) {
	/*message("ComponentListener: got event 'componentHidden' from " +
	  e.getComponent());*/
	message("ComponentListener: got event 'componentHidden'");
    }

    public void componentShown(ComponentEvent e) {
	/*message("ComponentListener: got event 'componentShown' from " +
	  e.getComponent());*/
	message("ComponentListener: got event 'componentShown'");
    }

    public void componentResized(ComponentEvent e) {
	/*message("ComponentListener: got event 'componentResized' from " +
	  e.getComponent());*/
	message("ComponentListener: got event 'componentResized'");
    }

    public void componentMoved(ComponentEvent e) {
	message("ComponentListener: got event 'componentMoved' from " +
		e.getComponent());
    }

    public void windowOpened(WindowEvent e) {
	message("WindowListener: got event 'windowOpened' from " +
		e.getWindow());
    }

    public void windowClosed(WindowEvent e) {
	message("WindowListener: got event 'windowClosed' from " +
		e.getWindow());
    }

    public void windowClosing(WindowEvent e) {
	message("WindowListener: got event 'windowClosing' from " +
		e.getWindow());
    }

    public void windowIconified(WindowEvent e) {
	message("WindowListener: got event 'windowIconified' from " +
		e.getWindow());
    }

    public void windowDeiconified(WindowEvent e) {
	message("WindowListener: got event 'windowDeiconified' from " +
		e.getWindow());
    }

    public void windowActivated(WindowEvent e) {
	message("WindowListener: got event 'windowActivated' from " +
		e.getWindow());
    }

    public void windowDeactivated(WindowEvent e) {
	message("WindowListener: got event 'windowDeactivated' from " +
		e.getWindow());
    }

    public void componentAdded(ContainerEvent e) {
	message("ContainerListener: got event 'componentAdded' from " +
		e.getContainer() + ", triggered by " + e.getChild());
    }

    public void componentRemoved(ContainerEvent e) {
	message("ContainerListener: got event 'componentRemoved' from " +
		e.getContainer() + ", triggered by " + e.getChild());
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
	/*	String type;
		switch (e.getAdjustmentType()) {
		case AdjustmentEvent.UNIT_INCREMENT:type = "UNIT_INCREMENT";break;
		case AdjustmentEvent.UNIT_DECREMENT:type = "UNIT_DECREMENT";break;
		case AdjustmentEvent.BLOCK_INCREMENT:type = "BLOCK_INCREMENT";break;
		case AdjustmentEvent.BLOCK_DECREMENT:type = "BLOCK_DECREMENT";break;
		case AdjustmentEvent.TRACK:type = "TRACK";break;
		default:type = "UNKNOWN";
		}
		message("AdjustmentListener: got event 'adjustmentValueChanged' from " +
		e.getSource() + ", type is " + type +
		", new value is " + e.getValue());*/
	message("AdjustmentListener: got event 'adjustmentValueChanged'");
    }

    public void focusGained(FocusEvent e) {
	message("FocusListener: got event 'focusGained' from " +
		e.getComponent());
    }

    public void focusLost(FocusEvent e) {
	message("FocusListener: got event 'focusLost' from " +
		e.getComponent());
    }

    public void itemStateChanged(ItemEvent e) {
	/*message("ItemListener: got event 'itemStateChanged' from " +
	  e.getItemSelectable() + ", associated item is " +
	  e.getItem() + ", new state is " +
	  ((e.getStateChange() == ItemEvent.SELECTED)
	  ? "SELECTED" : "DESELECTED"));*/
	message("ItemListener: got event 'itemStateChanged'");
    }

    public void keyPressed(KeyEvent e) {
	/*message("KeyListener: got event 'keyPressed' from " +
	  e.getComponent() + ", key char is " + e.getKeyChar() +
	  ", key code is " + e.getKeyCode() + ", modifiers are " + 
	  e.getModifiers());*/
	message("KeyListener: got event 'keyPressed'");
    }

    public void keyReleased(KeyEvent e) {
	/*message("KeyListener: got event 'keyReleased' from " +
	  e.getComponent() + ", key char is " + e.getKeyChar() +
	  ", key code is " + e.getKeyCode() + ", modifiers are " + 
	  e.getModifiers());*/
	message("KeyListener: got event 'keyReleased'");
    }

    public void keyTyped(KeyEvent e) {
	/*message("KeyListener: got event 'keyTyped' from " +
	  e.getComponent() + ", key char is " + e.getKeyChar() +
	  ", key code is " + e.getKeyCode() + ", modifiers are " + 
	  e.getModifiers());*/
	message("KeyListener: got event 'keyTyped'");
    }

    public void mouseDragged(MouseEvent e) {
	message("MouseMotionListener: got event 'mouseDragged' from " +
		e.getComponent() + ", occured at (" + e.getX() + "," +
		e.getY() + "), modifiers are " + e.getModifiers() + 
		", popup trigger is " + e.isPopupTrigger());
    }

    public void mouseMoved(MouseEvent e) {
	message("MouseMotionListener: got event 'mouseMoved' from " +
		e.getComponent() + ", occured at (" + e.getX() + "," +
		e.getY() + "), modifiers are " + e.getModifiers() + 
		", popup trigger is " + e.isPopupTrigger());
    }

    public void mouseEntered(MouseEvent e) {
	message("MouseListener: got event 'mouseEntered' from " +
		e.getComponent() + ", occured at (" + e.getX() + "," +
		e.getY() + "), modifiers are " + e.getModifiers() + 
		", popup trigger is " + e.isPopupTrigger());
    }

    public void mouseExited(MouseEvent e) {
	message("MouseListener: got event 'mouseExited' from " +
		e.getComponent() + ", occured at (" + e.getX() + "," +
		e.getY() + "), modifiers are " + e.getModifiers() + 
		", popup trigger is " + e.isPopupTrigger());
    }

    public void mousePressed(MouseEvent e) {
	message("MouseListener: got event 'mousePressed' from " +
		e.getComponent() + ", occured at (" + e.getX() + "," +
		e.getY() + "), modifiers are " + e.getModifiers() + 
		", popup trigger is " + e.isPopupTrigger());
    }

    public void mouseReleased(MouseEvent e) {
	message("MouseListener: got event 'mouseReleased' from " +
		e.getComponent() + ", occured at (" + e.getX() + "," +
		e.getY() + "), modifiers are " + e.getModifiers() + 
		", popup trigger is " + e.isPopupTrigger());
    }

    public void mouseClicked(MouseEvent e) {
	message("MouseListener: got event 'mouseClicked' from " +
		e.getComponent() + ", occured at (" + e.getX() + "," +
		e.getY() + "), modifiers are " + e.getModifiers() + 
		", popup trigger is " + e.isPopupTrigger());
    }

    public void textValueChanged(TextEvent e) {
	message("TextListener: got event 'textValueChanged' from " +
		e.getSource());
    }
}
