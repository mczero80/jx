package jx.vis;

public class ScalingVisualizer implements Visualizer {
    Visualizer vis;
    int xoff;
    int yoff;
    int virtualW;
    int virtualH;
    int realW;
    int realH;
    boolean mirrorY;
    int virtualYoff;

    public ScalingVisualizer(Visualizer vis, int xoff, int yoff, int virtualW, int virtualH, int realW, int realH) {
	this.vis = vis;
	this.xoff = xoff;
	this.yoff = yoff;
	this.virtualW = virtualW;
	this.virtualH = virtualH;
	this.realW = realW;
	this.realH = realH;
    }
    /** allows mirroring and moving the offset in the virtual coordinate system */
    public ScalingVisualizer(Visualizer vis, int xoff, int yoff, int virtualW, int virtualH, int realW, int realH, boolean mirrorY, int zeroY) {
	this(vis,xoff,yoff,virtualW,virtualH,realW,realH);
	this.mirrorY = mirrorY;
	this.virtualYoff = zeroY;
    }

    public void init() {}
    public void finish() {}

    public void drawLine(int x0, int y0, int x1, int y1) {
	vis.drawLine(convX(x0), convY(y0), convX(x1), convY(y1));
    }
    public void drawThinLine(int x0, int y0, int x1, int y1) {
	vis.drawThinLine(convX(x0), convY(y0), convX(x1), convY(y1));
    }

    public void drawEllipse(int x, int y, int w, int h) {
	vis.drawEllipse(convX(x), convY(y), convW(w), convH(h));
    }

    public void drawText(String text, int x, int y, int fontSize, int align) {
	drawText(text, x, y, fontSize, align, ROTATE_0, STYLE_DEFAULT);
    }
    public void drawText(String text, int x, int y, int fontSize, int align, int rotate, int style) {
	int rh = convH(fontSize);
	if (mirrorY) rh = -rh;
	int ry = convY(y);
	if (rh < 0) {
	    ry = rh + ry;
	    rh = -rh;
	}
	vis.drawText(text, convX(x), ry, rh, align, rotate, style);
    }

    public void drawRect(int x, int y, int w, int h, String fill) {
	int rh = convH(h);
	int ry = convY(y);
	if (rh < 0) {
	    ry = rh + ry;
	    rh = -rh;
	}
	vis.drawRect(convX(x), ry, convW(w), rh, fill);
    }





    private int convX(int x) {
	return xoff + ((realW * x) / virtualW) ;
    }
    private int convY(int y) {

	if (! mirrorY) return yoff + ((realH * (y)) / virtualH) ;
	int yy = 0;//realH;
	return yy + yoff + ((realH * ((virtualH-y)-virtualYoff)) / virtualH) ;
    }

    private int convW(int w) {
	return ((realW * w) / virtualW) ;
    }
    private int convH(int h) {
	int rh = ((realH * h) / virtualH) ;
	if (mirrorY) rh = -rh;
	return rh;
    }
}
