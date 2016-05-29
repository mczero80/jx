package test.jx.awt;

import java.awt.*;

public class FontCanvas
    extends Canvas {

    private final int BORDER = 20;
    private final int MODVALUE = 16;
    private final int NUMWIDTH = 40;
    private final int CWIDTH = 20;
    private final int HEIGHT = 20;
    
    private int width;
    private int height;
    

    
    public FontCanvas() {
	width = 2 * BORDER + MODVALUE * (NUMWIDTH + CWIDTH);
	height = 2 * BORDER + HEIGHT * (256 / MODVALUE);
    }




    public Dimension getPreferredSize() {
	return new Dimension(width, height);
    }

    public Dimension getMinimumSize() {
	return getPreferredSize();
    }

    public void paint(Graphics g) {
	g.setColor(Color.white);
	g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
	for (int i = 0; i < 256; i++) {
	    int x = (i % MODVALUE) * (NUMWIDTH + CWIDTH) + BORDER;
	    int y = (i / MODVALUE) * HEIGHT + BORDER;
	    g.setColor(Color.gray);
	    g.drawString(String.valueOf(i), x, y);
	    g.setColor(Color.black);
	    g.drawString(new String(new char[] {(char) i}), x + NUMWIDTH, y);
	}
    }

}
