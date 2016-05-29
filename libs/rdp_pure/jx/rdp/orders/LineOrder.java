package jx.rdp.orders;

public class LineOrder implements Order {

    private int mixmode = 0;
    private int startx = 0;
    private int starty = 0;
    private int endx = 0;
    private int endy = 0;
    private int bgcolor = 0;
    private int opcode = 0;
    Pen pen = null;
    
    public LineOrder() {
	pen = new Pen();
    }

    public int getMixmode() { 
	return this.mixmode;
    }

     public int getStartX() {
	return this.startx;
    }

     public int getStartY() {
	return this.starty;
    }
    
    public int getEndX() {
	return this.endx;
    }
    
    public int getEndY() {
	return this.endy;
    }

    public int getBackgroundColor() {
	return this.bgcolor;
    }

    public int getOpcode() {
	return this.opcode;
    }

    public void setMixmode(int mixmode) { 
	this.mixmode = mixmode;
    }

     public void setStartX(int startx) {
	this.startx = startx;
    }

     public void setStartY(int starty) {
	this.starty = starty;
    }
    
    public void setEndX(int endx) {
	this.endx = endx;
    }
    
    public void setEndY(int endy) {
	this.endy = endy;
    }

    public void setBackgroundColor(int bgcolor) {
	this.bgcolor = bgcolor;
    }

    public void setOpcode(int opcode) {
	this.opcode = opcode;
    }

    public void reset() {
	mixmode = 0;
	startx = 0;
	starty = 0;
	endx = 0;
	endy = 0;
	bgcolor = 0;
	opcode = 0;
	pen.reset();
    }
}
