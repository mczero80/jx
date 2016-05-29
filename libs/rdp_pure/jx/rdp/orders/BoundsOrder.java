package jx.rdp.orders;

public class BoundsOrder implements Order {

    private int left = 0;
    private int right = 0;
    private int top = 0;
    private int bottom = 0;

    public BoundsOrder() {
    }

    public int getLeft() {
	return this.left;
    }

    public int getRight() {
	return this.right;
    }

    public int getTop() {
	return this.top;
    }

    public int getBottom() {
	return this.bottom;
    }

    public void setLeft(int left) {
	this.left = left;
    }

    public void setRight(int right) {
	this.right = right;
    }

    public void setTop(int top) {
	this.top = top;
    }

    public void setBottom(int bottom) {
	this.bottom = bottom;
    }
    
    public void reset() {
	left = 0;
	right = 0;
	top = 0;
	bottom = 0;
    }
}


