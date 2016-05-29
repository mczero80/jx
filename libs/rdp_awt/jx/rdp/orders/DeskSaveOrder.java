package jx.rdp.orders;

public class DeskSaveOrder extends BoundsOrder {

    private int offset = 0;
    private int action = 0;
    
    public DeskSaveOrder() {
	super();
    }

    public int getOffset() {
	return this.offset;
    }

    public int getAction() {
	return this.action;
    }

    public void setOffset(int offset) {
	this.offset = offset;
    }

    public void setAction(int action) {
	this.action = action;
    }
    
    public void reset() {
	super.reset();
	offset = 0;
	action = 0;
    }
}
