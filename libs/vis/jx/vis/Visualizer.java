package jx.vis;

public interface Visualizer {
    public static final int ALIGN_LEFT   = 1;
    public static final int ALIGN_RIGHT  = 2;
    public static final int ALIGN_CENTER = 3;

    public static final int ROTATE_0   =   0;
    public static final int ROTATE_180 = 180;
    public static final int ROTATE_270 = 270;

    public static final int STYLE_DEFAULT = 0;
    public static final int STYLE_BOLD    = 1;
    public static final int STYLE_ITALIC  = 2;

    public void init();
    public void finish();
    public void drawLine(int x0, int y0, int x1, int y1);
    public void drawThinLine(int x0, int y0, int x1, int y1);
    public void drawEllipse(int x, int y, int w, int h);
    public void drawText(String text, int x, int y, int fontSize, int align);
    public void drawText(String text, int x, int y, int fontSize, int align, int rotate, int style);
    public void drawRect(int x, int y, int w, int h, String fill);
}
