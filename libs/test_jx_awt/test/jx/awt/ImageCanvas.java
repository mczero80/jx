package test.jx.awt;

import java.awt.*;

public class ImageCanvas
    extends Canvas {

    private Image image;
    private int width;
    private int height;



    public ImageCanvas(Image image) {
	this(image, image.getWidth(null), image.getHeight(null));
    }

    public ImageCanvas(Image image, int width, int height) {
	this.image = image;
	this.width = width;
	this.height = height;
    }




    public Dimension getPreferredSize() {
	return new Dimension(width, height);
    }

    public Dimension getMinimumSize() {
	return getPreferredSize();
    }

    public void paint(Graphics g) {
	int x = (getWidth() - width) / 2;
	int y = (getHeight() - height) / 2;
	if (x < 0)
	    x = 0;
	if (y < 0)
	    y = 0;
	g.drawImage(image, x, y, width, height, null);
    }

}
