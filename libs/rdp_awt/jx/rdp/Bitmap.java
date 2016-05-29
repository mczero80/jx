package jx.rdp;

public class Bitmap {

    private byte[] data = null;
    private int width = 0;
    private int height = 0;
    private int x = 0;
    private int y = 0;

    public Bitmap(byte[] data, int width, int height, int x, int y) {
	this.data = data;
	this.width = width;
	this.height = height;
	this.x = x;
	this.y = y;
    }

    public byte[] getBitmapData() {
	return this.data;
    }

    public int getWidth() {
	return this.width;
    }

    public int getHeight() {
	return this.height;
    }

    public int getX() {
	return this.x;
    }

    public int getY() {
	return this.y;
    }
}
