package jx.rdp;

public class MSCursor {

    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;
    //private byte[] mask = null;
    private int[] pixel = null;

    public MSCursor(int x, int y, int width, int height, int[] pixel) {
	this.x = x;
	this.y = y;
	this.width = width;
	this.height = height;
	//this.mask = mask;
	this.pixel = pixel;
    }

    public int getX() {
	return this.x;
    }

    public int getY() {
	return this.y;
    }

    public int getWidth() {
	return this.width;
    }

    public int getHeight() {
	return this.height;
    }
    
    public int[] getPixel() {
	return this.pixel;
    }
    /*
    public byte[] getMask() {
	return this.mask;
	}*/
}
