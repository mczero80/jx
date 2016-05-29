package test.jx.awt;

import java.awt.*;
import java.awt.event.*;
import java.util.*;


class MyRectangle {
    public Rectangle r;
    public Color c;
}

class MyLine {
    public Rectangle l;
    public Color c;
}

public class MyCanvas
    extends Canvas
    implements MouseListener, MouseMotionListener {

    public static final int RECTANGLE = 1;
    public static final int LINE = 2;

    private final int WIDTH = 400;
    private final int HEIGHT = 400;

    private Color color;
    private int drawType;

    private int x0;
    private int y0;
    private int x1;
    private int y1;

    private Vector elements;


    public MyCanvas() {
	super();
	addMouseMotionListener(this);
	addMouseListener(this);
	elements = new Vector();
	x0 = y0 = x1 = y1 = -1;
    }


    /*****************************
     * Interface methods         *
     *****************************/

    public void setColor(Color c) {
	color = c;
    }

    public void setType(int type) {
	drawType = type;
    }

    public void reset() {
	elements = new Vector();
	repaint();
    }

    /*****************************
     * Canvas methods            *
     *****************************/

    public Dimension getPreferredSize() {
	return new Dimension(WIDTH, HEIGHT);
    }

    public Dimension getMinimumSize() {
	return getPreferredSize();
    }

    public void paint(Graphics g) {
	g.setColor(Color.white);
	g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
	for (int i = 0; i < elements.size(); i++) {
	    if (elements.elementAt(i) instanceof MyLine) {
		MyLine m = (MyLine) elements.elementAt(i);
		g.setColor(m.c);
		g.drawLine(m.l.x, m.l.y, m.l.width, m.l.height);
	    }
	    if (elements.elementAt(i) instanceof MyRectangle) {
		MyRectangle m = (MyRectangle) elements.elementAt(i);
		g.setColor(m.c);
		g.drawRect(m.r.x, m.r.y, m.r.width, m.r.height);
	    }
	}
	if (x0 >= 0 && y0 >= 0) {
	    int a = (x0 < x1) ? x0 : x1;
	    int b = (y0 < y1) ? y0 : y1;
	    int c = (x0 > x1) ? x0 : x1;
	    int d = (y0 > y1) ? y0 : y1;
	    g.setColor(color);
	    switch (drawType) {
	    case RECTANGLE:g.drawRect(a, b, c - a, d - b);break;
	    case LINE:g.drawLine(x0, y0, x1, y1);break;
	    }
	}
    }

    /******************************
     * listener methods           *
     ******************************/

    public void mousePressed(MouseEvent e) {
	x0 = x1 = e.getX();
	y0 = y1 = e.getY();
	repaint();
    }

    public void mouseReleased(MouseEvent e) {
	int a = (x0 < x1) ? x0 : x1;
	int b = (y0 < y1) ? y0 : y1;
	int c = (x0 > x1) ? x0 : x1;
	int d = (y0 > y1) ? y0 : y1;
	switch (drawType) {
	case RECTANGLE:
	    MyRectangle m1 = new MyRectangle();
	    m1.r = new Rectangle(a, b, c - a, d - b);
	    m1.c = color;
	    elements.addElement(m1);
	    break;
	case LINE:
	    MyLine m2 = new MyLine();
	    m2.l = new Rectangle(x0, y0, x1, y1);
	    m2.c = color;
	    elements.addElement(m2);
	    break;
	}
	x0 = y0 = -1;
	repaint();
    }

    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {
	x0 = y0 = -1;
	repaint();
    }

    public void mouseDragged(MouseEvent e) {
	x1 = e.getX();
	y1 = e.getY();
	repaint();
    }
}
