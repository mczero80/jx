package jx.rdp;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import jx.rdp.orders.*;
import jx.zero.*;
//import com.sun.image.codec.jpeg.*;
import java.io.*;

public class RdesktopCanvas extends Canvas {

    private static final int KBD_FLAG_RIGHT = 0x0001;
    private static final int KBD_FLAG_EXT = 0x0100;
    private static final int KBD_FLAG_QUIET = 0x1000;
    private static final int KBD_FLAG_DOWN  = 0x4000;
    private static final int KBD_FLAG_UP = 0x8000;

    private static final int MOUSE_FLAG_MOVE = 0x0800;
    private static final int MOUSE_FLAG_BUTTON1 = 0x1000;
    private static final int MOUSE_FLAG_BUTTON2 = 0x2000;
    private static final int MOUSE_FLAG_BUTTON3 = 0x4000;
    private static final int MOUSE_FLAG_DOWN = 0x8000;

    private static final int RDP_INPUT_SYNCHRONIZE = 0;
    private static final int RDP_INPUT_CODEPOINT = 1;
    private static final int RDP_INPUT_VIRTKEY = 2;
    private static final int RDP_INPUT_SCANCODE = 4;
    private static final int RDP_INPUT_MOUSE = 0x8001;
    
    private static final int MIX_TRANSPARENT = 0;
    private static final int MIX_OPAQUE = 1;

    private static final int TEXT2_VERTICAL = 0x04;
    private static final int TEXT2_IMPLICIT_X = 0x20;

    private KeyCode keys = null;
    private int width = 0;
    private int height = 0;
    private int[] colors = null;
    //private Image surface = null;
    //private IndexColorModel colormap = null;
    //private MemoryImageSource is = null;
    private Cache cache = null;
    //private Robot robot = null;
    private Rdp rdp = null;
    private int name = 0;
    private Memory backstore = null;
    //Clip region
    private int top = 0;
    private int left = 0;
    private int right = 0;
    private int bottom = 0;
 
    private Naming naming = null;
    private int time = 0;

    public RdesktopCanvas(int width, int height) {
	super();
	this.width = width;
	this.height = height;
	this.right = width;
	this.bottom = height;
	setSize(width, height);
	naming = InitialNaming.getInitialNaming();
	MemoryManager mm = (MemoryManager)naming.lookup("MemoryManager");
	backstore = mm.alloc(width*height);
	this.addMouseListener(new RdesktopMouseAdapter());
	this.addMouseMotionListener(new RdesktopMouseMotionAdapter());
	//this.addKeyListener(new RdesktopKeyAdapter());
    }
    
    public void paint(Graphics g) {
	Dimension d = getSize();
	//g.drawImage(surface, 0, 0, this);
	byte[] data = new byte[this.width*this.height];
	backstore.copyToByteArray(data, 0, 0, data.length);
	//g.drawImage(this.createImage(new MemoryImageSource(this.width, this.height, this.colormap, data, 0, this.width)), 0, 0, this);
	
    }
    
    public void update(Graphics g) {
	paint(g);
    }

    private synchronized int getTime() {
	time++;
	if (time == Integer.MAX_VALUE)
	    time =1;
	return time;
    }

    /*public void registerPalette(IndexColorModel cm) {
	this.colormap = cm;
	this.colors = new int[cm.getMapSize()];
	cm.getRGBs(colors);
	}*/
    
    public void registerCommLayer(Rdp rdp) {
	this.rdp = rdp;
    }
    
    public void registerKeyboard(KeyCode keys) {
	this.keys = keys;
    }

    public void registerCache(Cache cache) {
	this.cache = cache;
    }

    public void addNotify() {
	super.addNotify();
	Dimension d = getSize();
	/*if (surface == null) {
	    surface = createImage(d.width, d.height);
	    }*/
	/*if (robot == null) {
	    try {
		robot = new Robot();
	    } catch(AWTException e) {
		Debug.out.println("Pointer movement not allowed");
	    }
	    }*/
    }
    
    public void displayImage(byte[] data, int w, int h, int x, int y, int cx, int cy) throws RdesktopException {
	int pbackstore = (y*this.width)+x;
	int pdata = 0;
	/*
	if(this.colormap==null) {
	    throw new RdesktopException("Register a palette first!");
	    }*/
	//Graphics g = surface.getGraphics();
	//g.drawImage(this.createImage(new MemoryImageSource(scale_x, h, this.colormap, data, 0, w)), x, y, this);
	for(int i = 0; i < h; i++) {
	    backstore.copyFromByteArray(data, pdata, pbackstore, cx);
	    pbackstore+=this.width;
	    pdata+=w;
	}
	this.repaint();
    }

    public byte[] getImage(int x, int y, int cx, int cy) {
	int pdata = 0;
	int pbackstore = (y*this.width)+x;
	byte[] data = new byte[cx*cy];

	for(int i = 0; i<cy; i++) {
	    backstore.copyToByteArray(data, pdata, pbackstore, cx);
	    pdata += cx;
	    pbackstore += this.width;
	}
	return data;
    }

    public void putImage(int x, int y, int cx, int cy, byte[] data) {
	int pdata = 0;
	int pbackstore = (y*this.width)+x;
	
	for(int i = 0; i<cy; i++) {
	    backstore.copyFromByteArray(data, pdata, pbackstore, cx);
	    pdata += cx;
	    pbackstore += this.width;
	}
	this.repaint();
    }
	
    public void resetClip() {
	Graphics g = this.getGraphics();
	Rectangle bounds = this.getBounds();
	g.setClip(bounds.x, bounds.y, bounds.width, bounds.height);
	this.top = 0;
	this.left = 0;
	this.right = this.width;
	this.bottom = this.height;
    }

    public void setClip(BoundsOrder bounds) {
	Graphics g = this.getGraphics();
	g.setClip(bounds.getLeft(), bounds.getTop(), bounds.getRight() - bounds.getLeft(), bounds.getBottom() - bounds.getTop());
	this.top = bounds.getTop();
	this.left = bounds.getLeft();
	this.right = bounds.getRight();
	this.bottom = bounds.getBottom();
    }

    public void movePointer(int x, int y) {
	Point p = this.getLocationOnScreen();
	x = x + p.x;
	y = y + p.y;
	//robot.mouseMove(x, y);
    }

    public void fillRectangle(int x, int y, int cx, int cy, int color) {
	int pbackstore = (y*this.width)+x;

	for(int i = 0; i < cy; i++) {
	    for(int j = 0; j < cx; j++) {
		if((y + j < this.left) || (y + j > this.right) || (x + i < this.top) || (x + i > this.bottom)) { // Clip
		} else {
		    backstore.set8((pbackstore+j), (byte)color);
		}
	    }
	    pbackstore += this.width;
	}
    }

    public void drawGlyph(int mixmode, int x, int y, int cx, int cy, byte[] data, int srcx, int srcy, int bgcolor, int fgcolor) {
	int pbackstore = (y*this.width)+x;
	int pdata = 0;
	int index = 0x80;

	if(mixmode == MIX_TRANSPARENT) { // FillStippled
	    for(int i = 0; i < cy; i++) {
		for(int j = 0; j < cx; j++) {
		    if((y + j < this.left) || (y + j > this.right) || (x + i < this.top) || (x + i > this.bottom)) { // Clip
		    } else {
			if((data[pdata]&index) !=0) {
			    backstore.set8(pbackstore + j,(byte)fgcolor);
			}
		    }
		    index >>=1;
		    if(index == 0) {
			index = 0x80;
		    }
		}
		pdata++;
		index = 0x80;
		pbackstore += this.width;
		if(pdata == data.length) {
		    pdata=0;
		}
	    }
	} else { // FillOpaqueStippled
	    for(int i = 0; i < cy; i++) {
		for(int j = 0; j < cx; j++) {
		    if((y + j < this.left) || (y + j > this.right) || (x + i < this.top) || (x + i > this.bottom)) { // Clip
		    } else {
			if((data[pdata]&index) !=0) {
			    backstore.set8(pbackstore + j, (byte)fgcolor);
			} else {
			    backstore.set8(pbackstore + j, (byte)bgcolor);
			}
		    }
		    index >>=1;
		    if(index == 0) {
			index = 0x80;
		    }
		}
		pdata++;
		index = 0x80;
		pbackstore += this.width;
		if(pdata == data.length) {
		    pdata = 0;
		}
	    }
	}
    } 
    
    public MSCursor createCursor(int x, int y, int w, int h, byte[] andmask, byte[] xormask) {
	int pxormask = 0;
	int pandmask = 0;
	MSCursor result = null;
	Point p = new Point(x, y);
	//Toolkit tk = Toolkit.getDefaultToolkit();

	int size = w * h;
	int scanline = w/8;
	int offset = 0;
	byte[] mask = new byte[size];
	int[] cursor = new int[size];
	int pcursor = 0, pmask = 0;
;
	offset = size;
	
	for(int i = 0; i < h; i++) {
	    offset -=w;
	    pmask = offset;
	    for(int j = 0; j < scanline; j++) {
		for(int bit=0x80; bit>0; bit>>=1) {
		    if((andmask[pandmask] & bit) !=0) {
			mask[pmask] = 0;
		    } else {
			mask[pmask] = 1;
		    }
		    pmask++;
		}
		pandmask++;
	    }
	}

	offset = size;
	pcursor = 0;

	for(int i = 0; i < h; i++) {
	    offset -=w;
	    pcursor = offset;
	    for(int j = 0; j < w; j++) {
		cursor[pcursor] = ((xormask[pxormask+2]<<16)&0x00ff0000) |
		    ((xormask[pxormask+1]<<8)&0x0000ff00) |
		    (xormask[pxormask]&0x000000ff);
		pxormask +=3;
		pcursor++;
	    }
	    
	}
	
	offset = size;
	pmask = 0;
	pcursor = 0;
	pxormask = 0;
	
	for(int i = 0; i < h; i++) {
	    for(int j = 0; j < w; j++) {
		if((mask[pmask] == 0) && (cursor[pcursor] !=0)) {
		    cursor[pcursor] = ~(cursor[pcursor]);
		    cursor[pcursor] |= 0xff000000;
		} else if((mask[pmask] == 1) || (cursor[pcursor] !=0)){
		    cursor[pcursor] |= 0xff000000;
		}
		pcursor++;
		pmask++;
	    }
	}

	//Image wincursor = this.createImage(new MemoryImageSource(w, h, cursor, 0, w));
	//result = tk.createCustomCursor(wincursor, p, "");
	result = new MSCursor(x, y, w, h, cursor);
	return result;	
    }
    
    class RdesktopMouseAdapter extends MouseAdapter {
	
	public RdesktopMouseAdapter() {
	    super();
	}
	
	public void mousePressed(MouseEvent e) {
	    int time = getTime();
	    if(rdp != null) {
		if((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
		    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON1 | MOUSE_FLAG_DOWN, e.getX(), e.getY());
		} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
		    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON2 | MOUSE_FLAG_DOWN, e.getX(), e.getY());
		} else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) {
		    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON3 | MOUSE_FLAG_DOWN, e.getX(), e.getY());
		}
	    }
	}
	
	public void mouseReleased(MouseEvent e) {
	    int time = getTime();
	    if(rdp != null) {
		if((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
		    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON1, e.getX(), e.getY());
		} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
		    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON2, e.getX(), e.getY());
		} else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) {
		    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON3, e.getX(), e.getY());
		}
	    }
	}
    }

    class RdesktopMouseMotionAdapter extends MouseMotionAdapter {

	public RdesktopMouseMotionAdapter() {
	    super();
	}

	public void mouseMoved(MouseEvent e) {
	    int time = getTime();
	    if(rdp != null) {
		rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_MOVE, e.getX(), e.getY());
	    }
	}
    }

    /*class RdesktopKeyAdapter extends KeyAdapter {
	
	public RdesktopKeyAdapter() {
	    super();
	}

	public void keyPressed(KeyEvent e) {
	    long time = e.getWhen();
	    int scancode = keys.getScancode(e);
	    
	    if(rdp != null) {
		rdp.sendInput(time, RDP_INPUT_SCANCODE, 0, scancode, 0);
	    }
	}

	public void keyReleased(KeyEvent e) {
	    long time = e.getWhen();
	    int scancode = keys.getScancode(e);
	    
	    if(rdp != null) {
		rdp.sendInput(time, RDP_INPUT_SCANCODE, KBD_FLAG_DOWN | KBD_FLAG_UP, scancode, 0);
	    }
	}

	public void keyTyped(KeyEvent e) {
	    long time = e.getWhen();
	    int scancode = 0;
	    scancode = keys.getScancode(e);
	    if(scancode == 0) {
		return;
	    }
	    if(rdp != null) {
		rdp.sendInput(time, RDP_INPUT_SCANCODE, KBD_FLAG_DOWN | KBD_FLAG_UP, scancode, 0);
	    }
	}
	
	}*/
	
}
