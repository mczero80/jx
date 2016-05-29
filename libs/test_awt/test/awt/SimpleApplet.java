package test.awt;

import java.awt.*;
import java.applet.Applet;

public class SimpleApplet extends Applet {

    public void paint(Graphics g) {
	g.fillRect(0, 0, 20, 30);
    }

}
