package jx.rdp.orders;

public class PolyLineOrder implements Order {

    private int x = 0;
    private int y = 0;
    private int flags = 0;
    private int fgcolor = 0;
    private int lines = 0;
    private int datasize = 0;
    byte[] data = new byte[256];
    
    public PolyLineOrder() {
    }

    public int getX() {
	return this.x;
    }
    
    public int getY() {
	return this.y;
    }

    public int getFlags() {
	return this.flags;
    }

    public int getForegroundColor() {
	return this.fgcolor;
    }
    
    public int getLines() {
	return this.lines;
    }

    public int getDataSize() {
	return this.datasize;
    }
    
    public byte[] getData() {
	return this.data;
    }
    
    public void setX(int x) {
	this.x = x;
    }
    
    public void setY(int y) {
	this.y = y;
    }
    
    public void setFlags(int flags) {
	this.flags = flags;
    }

    public void setForegroundColor(int fgcolor) {
	this.fgcolor = fgcolor;
    }

    public void setDataSize(int datasize) {
	this.datasize = datasize;
    }

    public void setData(byte[] data) {
	this.data = data;
    }

    public void reset() {
	x = 0;
	y = 0;
	flags = 0;
	fgcolor = 0;
	lines = 0;
	datasize = 0;
	data = new byte[256];
    }
}
