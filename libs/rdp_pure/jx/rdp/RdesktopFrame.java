package jx.rdp;

import jx.wm.*;
import jx.zero.*;
import jx.zero.Debug;
import jx.wm.message.*;
import jx.wm.bitmap.*;
import jx.devices.fb.*;
import jx.rdp.orders.*;
import java.io.IOException;
import jx.rdp.Rdp;

public class RdesktopFrame extends WWindow {
    
    private boolean debug = true;
    private HexDump dump = null;

    private Rdp rdp = null;

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
    private PixelColor[] colors = null;
 
    private Cache cache = null;
 
    private Memory backstore = null;
    private Naming naming = null;

    private Object lock = new Object();
    private int time = 0;
    
    //Clip region
    private int top = 0;
    private int left = 0;
    private int right = 0;
    private int bottom = 0;

    private ColorSpace colorspace = null;
    private WBitmap picture = null;
    private DrawingMode dm = null;

    public RdesktopFrame(int width, int height) {
	super("Rdesktop for JX", new PixelRect(100, 100,width+100,height+100), new WindowFlags());
	this.width = width;
	this.height = height;
	this.right = width;
	this.bottom = height;
	
	naming = InitialNaming.getInitialNaming();
	MemoryManager mm = (MemoryManager)naming.lookup("MemoryManager");
	backstore = mm.alloc(width*height);
	this.colorspace = new ColorSpace(ColorSpace.CS_CMAP8);
	this.dm = new DrawingMode(DrawingMode.DM_COPY);
	this.dump = new HexDump();

	this.picture = WBitmap.createWBitmap(null, width, height, this.colorspace, width, backstore, 0);
    }
    
    private synchronized int getTime() {
	    time ++;
	    if (time == Integer.MAX_VALUE)
		time =1;

	    return time;
    }
    
    public void paint(PixelRect cUpdateRect) {
	//Debug.out.println("Paint!");
	if (colors == null) {
	    Debug.out.println("NULL!");
	    return;
	}		
	drawBitmap(picture, cUpdateRect, cUpdateRect, dm);
    }

    public void registerCommLayer(Rdp rdp) {
	this.rdp = rdp;
    }
    
    public void registerKeyboard(KeyCode keys) {
       	this.keys = keys;
    }

    public void registerCache(Cache cache) {
	this.cache = cache;
    }
    
    public void registerPalette(PixelColor[] colors) {
        this.colors = colors;
	this.colorspace.setCMAP(colors);
    }

    public void displayImage(byte[] data, int w, int h, int x, int y, int cx, int cy) throws RdesktopException {
	int pbackstore = (y*this.width)+x;
	int pdata = 0;
	
	if(this.colors==null) {
	    throw new RdesktopException("Register a palette first!");
	}
	for(int i = 0; i < h; i++) {
	    backstore.copyFromByteArray(data, pdata, pbackstore, cx);
	    pbackstore+=this.width;
	    pdata+=w;
	}
	this.paint(new PixelRect(x, y, cx + x , cy + y));
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
	//this.paint();
	this.paint(new PixelRect(x-10, y-10, cx + x + 10, cy + y+10));
    }
	
    public void resetClip() {
	PixelRect bounds = this.getFrame();
	this.setClip(bounds.left(), bounds.top(), bounds.width(), bounds.height());
	this.top = 0;
	this.left = 0;
	this.right = this.width;
	this.bottom = this.height;
    }

    public void setClip(BoundsOrder bounds) {
	setClip(bounds.getLeft(), bounds.getTop(), bounds.getLeft() - bounds.getRight(), bounds.getBottom() - bounds.getTop());
	this.top = bounds.getTop();
	this.left = bounds.getLeft();
	this.right = bounds.getRight();
	this.bottom = bounds.getBottom();
    }

    public void movePointer(int x, int y) {
	//Point p = this.getLocationOnScreen();
	//x = x + p.x;
	//y = y + p.y;
	//robot.mouseMove(x, y);
    }

    public void fillRectangle(int x, int y, int cx, int cy, int color) {
	/*int pbackstore = (y*this.width)+x;

	for(int i = 0; i < cy; i++) {
	    for(int j = 0; j < cx; j++) {
		if((y + j < this.left) || (y + j > this.right) || (x + i < this.top) || (x + i > this.bottom)) { // Clip
		} else {
		    backstore.set8((pbackstore+j), (byte)color);
		}
	    }
	    pbackstore += this.width;
	    }*/

	picture.fillRect(new PixelRect(x, y, cx + x, cy + y), colorspace.CMAP8toPixelColor((byte)color) , dm);
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

	int size = w * h;
	int scanline = w/8;
	int offset = 0;
	byte[] mask = new byte[size];
	int[] cursor = new int[size];
	int pcursor = 0, pmask = 0;
	
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
	
	result = new MSCursor(x, y, w, h, cursor);
	//Image wincursor = this.createImage(new MemoryImageSource(w, h, cursor, 0, w));
	//result = tk.createCustomCursor(wincursor, p, "");
	return result;	
    }

    public void mouseDown(PixelPoint cMousePos, int nButton) {
	int time = getTime();
	if(rdp != null && rdp.getConnectionStatus()) {
	    if(nButton == 1) {
		rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON1 | MOUSE_FLAG_DOWN, cMousePos.X(), cMousePos.Y());
	    } else if(nButton == 4) {
		rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON2 | MOUSE_FLAG_DOWN, cMousePos.X(), cMousePos.Y());
	    } else if (nButton == 2) {
		rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON3 | MOUSE_FLAG_DOWN, cMousePos.X(), cMousePos.Y());
	    }
	}
    }
	
    public void mouseUp(PixelPoint cMousePos, int nButton) {
	int time = getTime();
	if(rdp != null && rdp.getConnectionStatus()) {
	    if(nButton == 1) {
		rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON1, cMousePos.X(), cMousePos.Y());
	    } else if (nButton == 4) {
		rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON2, cMousePos.X(), cMousePos.Y());
	    } else if (nButton == 2) {
		rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON3, cMousePos.X(), cMousePos.Y());
	    }
	}
    }

   
    public void mouseMoved(PixelPoint cMousePos, int nTransit) {
	int time = getTime();
	if(rdp != null && rdp.getConnectionStatus()) {
	    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_MOVE, cMousePos.X(), cMousePos.Y());
	}
    }

    public void keyDown(Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers) {
	int time = getTime();
	int scancode = keys.getScancode(eRawCode);
	
	if(rdp != null && rdp.getConnectionStatus()) {
	    rdp.sendInput(time, RDP_INPUT_SCANCODE, 0, scancode, 0);
	}
    }

    public void keyUp(Keycode eKeyCode, Keycode eRawCode, Qualifiers eQualifiers) {
	int time = getTime();
	int scancode = keys.getScancode(eRawCode);
	
	if(rdp != null && rdp.getConnectionStatus()) {
	    rdp.sendInput(time, RDP_INPUT_SCANCODE, KBD_FLAG_DOWN | KBD_FLAG_UP, scancode, 0);
	}
    }
    
}
