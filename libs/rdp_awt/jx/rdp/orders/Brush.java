package jx.rdp.orders;

public class Brush {

    private int xorigin = 0;
    private int yorigin = 0;
    private int style = 0;
    private byte[] pattern = new byte[8];

    public Brush() {
    }

    public int getXOrigin() {
	return this.xorigin;
    }

    public int getYOrigin() {
	return this.yorigin;
    }

    public int getStyle() {
	return this.style;
    }

    public byte[] getPattern(){
	return this.pattern;
    }

    public void setXOrigin(int xorigin) {
	this.xorigin = xorigin;
    }
    
    public void setYOrigin(int yorigin) {
	this.yorigin = yorigin;
    }

    public void setStyle(int style) {
	this.style = style;
    }

    public void getPattern(byte[] pattern){
	this.pattern = pattern;
    }

    public void reset() {
	xorigin = 0;
	yorigin = 0;
	style = 0;
	pattern = new byte[8];
    }
}
