package jx.rdp.orders;

public class ScreenBltOrder extends DestBltOrder {

    private int stcx = 0;
    private int stcy = 0;

    public ScreenBltOrder() {
	super();
    }

    public int getSTCX() {
	return this.stcx;
    }
    
    public int getSTCY() {
	return this.stcy;
    }

    public void setSTCX(int stcx) {
	this.stcx = stcx;
    }

    public void setSTCY(int stcy) {
	this.stcy = stcy;
    }

    public void reset() {
	super.reset();
	stcx = 0;
	stcy = 0;
    }
}
