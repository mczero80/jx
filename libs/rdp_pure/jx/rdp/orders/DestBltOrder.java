package jx.rdp.orders;

public class DestBltOrder implements Order {

    private int x = 0;
    private int y = 0;
    private int cx = 0;
    private int cy = 0;
    private int opcode = 0;

    public DestBltOrder() {
    }

    public int getX() {
	return this.x;
    }

    public int getY() {
	return this.y;
    }
    
    public int getCX() {
	return this.cx;
    }

    public int getCY() {
	return this.cy;
    }

    public int getOpcode() {
	return this.opcode;
    }

    public void setX(int x) {
	this.x = x;
    }

    public void setY(int y) {
	this.y = y;
    }
    
    public void setCX(int cx) {
	this.cx = cx;
    }

    public void setCY(int cy) {
	this.cy = cy;
    }

    public void setOpcode(int opcode) {
	this.opcode = opcode;
    }

    public void reset() {
	x = 0;
	y = 0;
	cx = 0;
	cy = 0;
	opcode = 0;
    }
}
