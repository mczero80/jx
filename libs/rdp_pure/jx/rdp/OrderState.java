package jx.rdp;

import jx.rdp.orders.*;

class OrderState {
    private int order_type = 0;
    private BoundsOrder bounds = null;
    
    private DestBltOrder destblt = null;
    private PatBltOrder patblt = null;
    private ScreenBltOrder screenblt = null;
    private LineOrder line = null;
    private RectangleOrder rect = null;
    private DeskSaveOrder desksave = null;
    private MemBltOrder memblt = null;
    private TriBltOrder triblt = null;
    private PolyLineOrder polyline = null;
    private Text2Order text2 = null;
    
    public OrderState(){
	bounds = new BoundsOrder();
	destblt = new DestBltOrder();
	patblt = new PatBltOrder();
	screenblt = new ScreenBltOrder();
	line = new LineOrder();
	rect = new RectangleOrder();
	desksave = new DeskSaveOrder();
	memblt = new MemBltOrder();
	triblt = new TriBltOrder();
	polyline = new PolyLineOrder();
	text2 = new Text2Order();
    }

    public int getOrderType() {
	return this.order_type;
    }

    public void setOrderType(int order_type) {
	this.order_type = order_type;
    }

    public BoundsOrder getBounds() {
	return this.bounds;
    }

    public DestBltOrder getDestBlt() {
	return this.destblt;
    }    

    public PatBltOrder getPatBlt() {
	return this.patblt;
    }

    public ScreenBltOrder getScreenBlt() {
	return this.screenblt;
    }
    
    public LineOrder getLine() {
	return this.line;
    }
    
    public RectangleOrder getRectangle() {
	return this.rect;
    }

    public DeskSaveOrder getDeskSave() {
	return this.desksave;
    }

    public MemBltOrder getMemBlt() {
	return this.memblt;
    }

    public TriBltOrder getTriBlt() {
	return this.triblt;
    }

    public PolyLineOrder getPolyLine() {
	return this.polyline;
    }

    public Text2Order getText2() {
	return this.text2;
    }
    
    public void reset() {
	bounds.reset();
	destblt.reset();
	patblt.reset();
	screenblt.reset();
	line.reset();
	rect.reset();
	desksave.reset();
	memblt.reset();
	triblt.reset();
	polyline.reset();
	text2.reset();
    }
}
    
