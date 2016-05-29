package jx.zero;

public class FBEmulationEvent {
    public static final int TYPE_KEY_PRESS      = 1<<0;
    public static final int TYPE_KEY_RELEASE    = 1<<1;
    public static final int TYPE_BUTTON_PRESS   = 1<<2;
    public static final int TYPE_BUTTON_RELEASE = 1<<3;
    public static final int TYPE_MOUSE_MOVE     = 1<<4;

    public static final int STATE_BUTTON1       = 1<<0;
    public static final int STATE_BUTTON2       = 1<<1;
    public static final int STATE_BUTTON3       = 1<<2;
    public static final int STATE_SHIFT         = 1<<3;

    public int eventType;
    public int x,y;
    public int keycode;
    public int button;

    public int state; // keyboard keys pressed or buttons pressed while mouse moved
}
