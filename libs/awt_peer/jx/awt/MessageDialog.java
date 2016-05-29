package jx.awt;


import java.awt.*;
import java.awt.event.*;


/**
 * This class implements a simple dialog window that informs
 * the user. It only provides one button to leave the dialog.
 */
public class MessageDialog {
    
    private MessageDialog() {
	// this class can't be instanciated
	// it just offers some common static
	// message dialogs
    }

    /**
     * Centers the frame relative to the screen.
     */
    private static void centerFrame(Frame f) {
	Rectangle r = f.getBounds();
	Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
	f.setBounds((s.width - r.width) / 2, (s.height - r.height)/ 2,
		    r.width, r.height);
    }

    /**
     * Opens a new info dialog. The window can be closed by pressing the button
     * or by pressing the window close button. The info text may contain more
     * than one line (separated by '\n') as it uses a ExtendedLabel to display
     * the text.
     */
    public static void infoMessageDialog(String fText, String text, String bText) {
	ExtendedLabel l = new ExtendedLabel(text);
	Button b = new Button(bText);
	final Frame f = new Frame(fText);
	Panel p = new Panel();
	f.add(l, BorderLayout.CENTER);
	f.add(p, BorderLayout.SOUTH);
	p.add(b);
	b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    f.dispose();
		}
	    });
	f.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    f.dispose();
		}
	    });
	
	f.pack();
	centerFrame(f);
	f.setResizable(false);
	f.show();
    }
}

