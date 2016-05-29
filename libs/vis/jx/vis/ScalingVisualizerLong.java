package jx.vis;

public class ScalingVisualizerLong implements VisualizerLong {
    Visualizer vis;
    long xoff;
    long yoff;
    long virtualW;
    long virtualH;
    long realW;
    long realH;
    public ScalingVisualizerLong(Visualizer vis, long xoff, long yoff, long virtualW, long virtualH, long realW, long realH) {
	this.vis = vis;
	this.xoff = xoff;
	this.yoff = yoff;
	this.virtualW = virtualW;
	this.virtualH = virtualH;
	this.realW = realW;
	this.realH = realH;
    }
    public void init() {}
    public void finish() {}

    public void drawLine(long x0, long y0, long x1, long y1) {
	vis.drawLine(convX(x0), convY(y0), convX(x1), convY(y1));
    }
    public void drawThinLine(long x0, long y0, long x1, long y1) {
	vis.drawThinLine(convX(x0), convY(y0), convX(x1), convY(y1));
    }

    public void drawEllipse(long x, long y, long w, long h) {
	vis.drawEllipse(convX(x), convY(y), convW(w), convH(h));
    }

    public void drawText(String text, long x, long y, long fontSize, int align) {
	vis.drawText(text, convX(x), convY(y), convH(fontSize), align);
    }

    public void drawText(String text, long x, long y, long fontSize, int align, int rotate, int style) {
	int rh = convH(fontSize);
	//	if (mirrorY) rh = -rh;
	int ry = convY(y);
	if (rh < 0) {
	    ry = rh + ry;
	    rh = -rh;
	}
	vis.drawText(text, convX(x), ry, rh, align, rotate, style);
    }


    public void drawRect(long x, long y, long w, long h, String fill) {
	vis.drawRect(convX(x), convY(y), convW(w), convH(h), fill);
    }





    private int convX(long x) {
	return (int)(xoff + ((realW * x) / virtualW));
    }
    private int convY(long y) {
	return (int)(yoff + ((realH * y) / virtualH));
    }

    private int convW(long w) {
	return (int)(((realW * w) / virtualW));
    }
    private int convH(long h) {
	return (int)(((realH * h) / virtualH));
    }
}
