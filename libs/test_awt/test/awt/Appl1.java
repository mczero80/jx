package test.awt;

import jx.zero.*;
import java.awt.*;
import java.awt.event.*;

public class Appl1 {
    public static void main(String[] args) {
	System.out.println("APPL0 speaking.");
	Frame frame = new Frame("TEST");
	Button b = new Button("OK");
	frame.add(b);
	b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    System.out.println("OK pressed");		    
		}
	    });
	frame.pack();
	frame.show();
    }
}
