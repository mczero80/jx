package jx.rdp.orders;

public class TriBltOrder extends PatBltOrder {

    private int color_table = 0;
    private int cache_id = 0;
    private int cache_idx = 0;
    private int srcx = 0;
    private int srcy = 0;
    private int unknown = 0;

    public TriBltOrder() {
	super();
    }

    public int getColorTable(){
	return this.color_table;
    }
    
    public int getCacheID() {
	return this.cache_id;
    }
    
    public int getCacheIDX() {
	return this.cache_idx;
    }
    
    public int getSRCX() {
	return this.srcx;
    }
    
    public int getSRCY() {
	return this.srcy;
    }
    
    public int getUnknown() {
	return this.unknown;
    }
    
    public void setColorTable(int color_table) {
	this.color_table = color_table;
    }

    public void setCacheID(int cache_id) {
	this.cache_id = cache_id;
    }

    public void setCacheIDX(int cache_idx) {
	this.cache_idx = cache_idx;
    }

    public void setSRCX(int stcx) {
	this.srcx = srcx;
    }
    
    public void setSRCY(int stcy) {
	this.srcy = srcy;
    }
    
    public void setUnknown(int unknown) {
	this.unknown = unknown;
    }
    
    public void reset() {
	super.reset();
	color_table = 0;
	cache_id = 0;
	cache_idx = 0;
	srcx = 0;
	srcy = 0;
	unknown = 0;
    }
}
