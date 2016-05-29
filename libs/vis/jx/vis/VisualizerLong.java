package jx.vis;

public interface VisualizerLong {
    public void init();
    public void finish();
    public void drawLine(long x0, long y0, long x1, long y1);
    public void drawThinLine(long x0, long y0, long x1, long y1);
    public void drawEllipse(long x, long y, long w, long h);
    public void drawText(String text, long x, long y, long fontSize, int align);
    public void drawRect(long x, long y, long w, long h, String fill);
}
