package jx.rdp;

import java.io.*;
import java.net.*;
import jx.zero.Debug;
import java.awt.*;
import jx.rdp.orders.*;

public class Orders {

    private OrderState os = null;
    private RdesktopCanvas surface = null;
    private Cache cache = null;
    
    private static final int RDP_ORDER_STANDARD = 0x01;
    private static final int RDP_ORDER_SECONDARY = 0x02;
    private static final int RDP_ORDER_BOUNDS = 0x04;
    private static final int RDP_ORDER_CHANGE = 0x08;
    private static final int RDP_ORDER_DELTA = 0x10;
    private static final int RDP_ORDER_LASTBOUNDS = 0x20;
    private static final int RDP_ORDER_SMALL = 0x40;
    private static final int RDP_ORDER_TINY = 0x80;
    
    /* standard order types */    
    private static final int RDP_ORDER_DESTBLT = 0;
    private static final int RDP_ORDER_PATBLT = 1;
    private static final int RDP_ORDER_SCREENBLT = 2;
    private static final int RDP_ORDER_LINE = 9;
    private static final int RDP_ORDER_RECT = 10;
    private static final int RDP_ORDER_DESKSAVE = 11;
    private static final int RDP_ORDER_MEMBLT = 13;
    private static final int RDP_ORDER_TRIBLT = 14;
    private static final int RDP_ORDER_POLYLINE = 22;
    private static final int RDP_ORDER_TEXT2 = 27;
    
    /* secondary order types */
    private static final int RDP_ORDER_RAW_BMPCACHE = 0;
    private static final int RDP_ORDER_COLCACHE = 1;
    private static final int RDP_ORDER_BMPCACHE = 2;
    private static final int RDP_ORDER_FONTCACHE = 3;

    private static final int MIX_TRANSPARENT = 0;
    private static final int MIX_OPAQUE = 1;
    
    private static final int TEXT2_VERTICAL = 0x04;
    private static final int TEXT2_IMPLICIT_X = 0x20;

   
    public Orders() {
	os = new OrderState();
    }
    
    public  void resetOrderState() {
	this.os.reset();
	os.setOrderType(this.RDP_ORDER_PATBLT);
	return;
    }

    private int inPresent(Packet data, int flags, int size) {
	int present = 0;
	int bits = 0;
	int i=0;

	if((flags & this.RDP_ORDER_SMALL) != 0) {
	    size--;
	}
	
	if((flags & this.RDP_ORDER_TINY) != 0) {
	    
	    if(size < 2) {
		size = 0;
	    } else {
		size -=2;
	    }
	}

	for(i=0; i < size; i++) {
	    bits = data.get8();
	    present |= (bits << (i*8));
	}
	return present;
    }
    
    public void processOrders(Packet data, int next_packet) throws OrderException, RdesktopException {
	
	int present = 0;
	int n_orders = 0;
	int order_flags = 0, order_type = 0;
	int size = 0, processed = 0;
	boolean delta;
	
	data.incrementPosition(2); //pad
	n_orders = data.getLittleEndian16();
	data.incrementPosition(2); //pad
	
	while(processed < n_orders) {
	    
	    order_flags = data.get8();
	    
	    if ((order_flags & this.RDP_ORDER_STANDARD) == 0) {
		throw new OrderException("Order parsing failed!");
	    }

	    if ((order_flags & this.RDP_ORDER_SECONDARY) != 0) {
		this.processSecondaryOrders(data);
	    } else {
		
		if ((order_flags & this.RDP_ORDER_CHANGE) != 0) {
		    os.setOrderType(data.get8());
		}
		
		switch (os.getOrderType()) {
		case RDP_ORDER_TRIBLT:
		case RDP_ORDER_TEXT2:
		    size = 3;
		    break;
		    
		case RDP_ORDER_PATBLT:
		case RDP_ORDER_MEMBLT:
		case RDP_ORDER_LINE:
		    size = 2;
		    break;
		    
		default:
		    size = 1;
		}

		present = this.inPresent(data, order_flags, size);
		
		if ((order_flags & this.RDP_ORDER_BOUNDS) != 0) {

		    if((order_flags & this.RDP_ORDER_LASTBOUNDS) == 0) {
			this.parseBounds(data, os.getBounds());
		    }
		    
		    surface.setClip(os.getBounds());
		}
		
		delta = ((order_flags & this.RDP_ORDER_DELTA) != 0);
			
		switch (os.getOrderType()) {
		case RDP_ORDER_DESTBLT:
		    Debug.out.println("DestBlt Order");
		    this.processDestBlt(data, os.getDestBlt(), present, delta);
		    break;
		    
		case RDP_ORDER_PATBLT:
		    Debug.out.println("PatBlt Order");
		    this.processPatBlt(data, os.getPatBlt(), present, delta);
		    break;
		    
		case RDP_ORDER_SCREENBLT:
		    Debug.out.println("ScreenBlt Order");
		    this.processScreenBlt(data, os.getScreenBlt(), present, delta);
		    break;
		    
		case RDP_ORDER_LINE:
		    Debug.out.println("Line Order");
		    this.processLine(data, os.getLine(), present, delta);
		    break;
		    
		case RDP_ORDER_RECT:
		    Debug.out.println("Rectangle Order");
		    this.processRectangle(data, os.getRectangle(), present, delta);
		    break;
		    
		case RDP_ORDER_DESKSAVE:
		    //Debug.out.println("Desksave!");
		    this.processDeskSave(data, os.getDeskSave(), present, delta);
		    break;
		    
		case RDP_ORDER_MEMBLT:
		    Debug.out.println("MemBlt Order");
		    this.processMemBlt(data, os.getMemBlt(), present, delta);
		    break;
		    
		case RDP_ORDER_TRIBLT:
		    Debug.out.println("TriBlt Order");
		    this.processTriBlt(data, os.getTriBlt(), present, delta);
		    break;
		    
		case RDP_ORDER_POLYLINE:
		    Debug.out.println("Polyline Order");
		    this.processPolyLine(data, os.getPolyLine(), present, delta);
		    break;
		    
		case RDP_ORDER_TEXT2:
		    //Debug.out.println("Text2 Order");
		    this.processText2(data, os.getText2(), present, delta);
		    break;
		    
		default:
		    throw new OrderException("Unimplemented " + order_type);
		}
		
		if((order_flags & this.RDP_ORDER_BOUNDS) != 0) {
		    surface.resetClip();
		    //Debug.out.println("Reset clip");
		}
	    }
	    
	    processed++;   
	}
	if (data.getPosition() != next_packet) {
	    throw new OrderException("End not reached!");
	}
    }
 
    public void registerDrawingSurface(RdesktopCanvas surface) {
	this.surface = surface;
	surface.registerCache(cache);
    }

    private void processSecondaryOrders(Packet data) throws OrderException, RdesktopException {
	int length = 0;
	int type = 0;
	int next_order = 0;
	
	length = data.getLittleEndian16();
	data.incrementPosition(2);

	type = data.get8();

	next_order = data.getPosition() + length + 7;
	
	switch(type) {
	    
	case RDP_ORDER_RAW_BMPCACHE:
	    Debug.out.println("Raw BitmapCache Order");
	    this.processRawBitmapCache(data);
	    break;
	    
	case RDP_ORDER_COLCACHE:
	    Debug.out.println("Colorcache Order");
	    this.processColorCache(data);
	    break;
	    
	case RDP_ORDER_BMPCACHE:
	    Debug.out.println("Bitmapcache Order");
	    this.processBitmapCache(data);
	    break;
	    
	case RDP_ORDER_FONTCACHE:
	    //Debug.out.println("Fontcache Order");
	    this.processFontCache(data);
	    break;
	    
	default:
	    throw new OrderException("Unimplemented!");
	}

	data.setPosition(next_order);
    }

    private void processRawBitmapCache(Packet data) {
    }

    private void processColorCache(Packet data) {
    }
    
    private void processBitmapCache(Packet data) {
    }

    private void processFontCache(Packet data) throws RdesktopException {
	Glyph glyph = null;
	
	int font = 0, nglyphs = 0;
	int character = 0, offset = 0, baseline = 0, width = 0, height = 0;
	int datasize = 0;
	byte[] fontdata = null;

	font = data.get8();
	nglyphs = data.get8();
	
	for(int i = 0; i < nglyphs; i++) {
	    character = data.getLittleEndian16();
	    offset = data.getLittleEndian16();
	    baseline = data.getLittleEndian16();
	    width = data.getLittleEndian16();
	    height = data.getLittleEndian16();
	    datasize = (height * ((width + 7) /8) + 3) & ~3;
	    fontdata = new byte[datasize];

	    data.copyToByteArray(fontdata, 0, data.getPosition(), datasize);
	    data.incrementPosition(datasize);
	    glyph = new Glyph(font, character, offset, baseline, width, height, fontdata);
	    cache.putFont(glyph);
	}
    }
   

   
    private void processDestBlt(Packet data, DestBltOrder destblt,int present, boolean delta) {
    }
    
    private void processPatBlt(Packet data, PatBltOrder patblt, int present, boolean delta){
    }
    
    private void processScreenBlt(Packet data, ScreenBltOrder screenblt, int present, boolean delta){
    }
    
    private void processLine(Packet data, LineOrder line, int present, boolean delta){
    }
    
    private void processRectangle(Packet data, RectangleOrder rect, int present, boolean delta){
    }
    
    private void processDeskSave(Packet data, DeskSaveOrder desksave, int present, boolean delta) throws RdesktopException {
	
	int width = 0, height = 0;

	if ((present & 0x01) != 0) {
	    desksave.setOffset(data.getLittleEndian32());
	}

	if ((present & 0x02) != 0) {
	    desksave.setLeft(this.setCoordinate(data, desksave.getLeft(), delta));
	}

	if ((present & 0x04) != 0) {
	    desksave.setTop(this.setCoordinate(data, desksave.getTop(), delta));
	}

	if ((present & 0x08) != 0) {
	    desksave.setRight(this.setCoordinate(data, desksave.getRight(), delta));
	}

	if ((present & 0x10) != 0) {
	    desksave.setBottom(this.setCoordinate(data, desksave.getBottom(), delta));
	}

	if ((present & 0x20) != 0) {
	    desksave.setAction(data.get8());
	}

	width = desksave.getRight() - desksave.getLeft() + 1;
	height = desksave.getBottom() - desksave.getTop() + 1;

	if(desksave.getAction() == 0) {
	    byte[] pixel = surface.getImage(desksave.getLeft(), desksave.getTop(), width, height);
	    cache.putDesktop((int)desksave.getOffset(), width, height, pixel);
	} else{
	    byte[] pixel  = cache.getDesktop((int)desksave.getOffset(), width, height);
	    surface.putImage(desksave.getLeft(), desksave.getTop(), width, height, pixel);
	}
    }
    
    private void processMemBlt(Packet data, MemBltOrder memblt, int present, boolean delta){
    }
    
    private void processTriBlt(Packet data, TriBltOrder triblt, int present, boolean delta){
    }
    
    private void processPolyLine(Packet data, PolyLineOrder polyline, int present, boolean delta){
    }
    
    private void processText2(Packet data, Text2Order text2, int present, boolean delta) throws RdesktopException {
	DataBlob entry = null;
	 
	if ((present & 0x000001) != 0) {
	    text2.setFont(data.get8());
	}
	if ((present & 0x000002) != 0) {
	    text2.setFlags(data.get8());
	}
	
	if ((present & 0x000004) != 0) {
	    text2.setUnknown(data.get8());
	}
	
	if ((present & 0x000008) != 0) {
	    text2.setMixmode(data.get8());
	}
	
	if ((present & 0x000010) != 0) {
	    text2.setForegroundColor(this.setColor(data));
	}
	
	if ((present & 0x000020) != 0) {
	    text2.setBackgroundColor(this.setColor(data));
	}
	
	if ((present & 0x000040) != 0) {
	    text2.setClipLeft(data.getLittleEndian16());
	}

	if ((present & 0x000080) != 0) {
	    text2.setClipTop(data.getLittleEndian16());
	}

	if ((present & 0x000100) != 0) {
	    text2.setClipRight(data.getLittleEndian16());
	}

	if ((present & 0x000200) != 0) {
	    text2.setClipBottom(data.getLittleEndian16());
	}

	if ((present & 0x000400) != 0) {
	    text2.setBoxLeft(data.getLittleEndian16());
	}

	if ((present & 0x000800) != 0) {
	    text2.setBoxTop(data.getLittleEndian16());
	}

	if ((present & 0x001000) != 0) {
	    text2.setBoxRight(data.getLittleEndian16());
	}

	if ((present & 0x002000) != 0) {
	    text2.setBoxBottom(data.getLittleEndian16());
	}

	if ((present & 0x080000) != 0) {
	    text2.setX(data.getLittleEndian16());
	}
	    
	if ((present & 0x100000) != 0) {
	    text2.setY(data.getLittleEndian16());
	}
	
	if ((present & 0x200000) != 0) { 
	    text2.setLength(data.get8());
	    
	    byte[] text = new byte[text2.getLength()];
	    data.copyToByteArray(text, 0, data.getPosition(), text.length);
	    data.incrementPosition(text.length);
	    text2.setText(text);

	    /*Debug.out.println("X: " + text2.getX() + " Y: " + text2.getY() +
			       " Left Clip: " + text2.getClipLeft() + " Top Clip: " + text2.getClipTop() +
			       " Right Clip: " + text2.getClipRight() + " Bottom Clip: " + text2.getClipBottom() +
			       " Left Box: " + text2.getBoxLeft() + " Top Box: " + text2.getBoxTop() +
			       " Right Box: " + text2.getBoxRight() + " Bottom Box: " + text2.getBoxBottom() +
			       " Foreground Color: " + text2.getForegroundColor() +
			       " Background Color: " + text2.getBackgroundColor() +
			       " Font: " + text2.getFont() + " Flags: " + text2.getFlags() +
			       " Mixmode: " + text2.getMixmode() + " Unknown: " + text2.getUnknown() +
			       " Length: " + text2.getLength());*/
	}
	
	this.drawText(text2, text2.getClipRight() - text2.getClipLeft(), text2.getClipBottom() - text2.getClipTop(),
			    text2.getBoxRight() - text2.getBoxLeft(), text2.getBoxBottom() - text2.getBoxTop());
	
    }
	
    private void parseBounds(Packet data, BoundsOrder bounds) throws OrderException {
	int present = 0;

	present = data.get8();

	if((present & 1) != 0) {
	    bounds.setLeft(this.setCoordinate(data, bounds.getLeft(), false));
	} else if ((present & 16) != 0) {
	    bounds.setLeft(this.setCoordinate(data, bounds.getLeft(), true));
	}

	if((present & 2) != 0) {
	    bounds.setTop(this.setCoordinate(data, bounds.getTop(), false));
	} else if ((present & 32) != 0) {
	    bounds.setTop(this.setCoordinate(data, bounds.getTop(), true));
	}

	if((present & 4) != 0) {
	    bounds.setRight(this.setCoordinate(data, bounds.getRight(), false));
	} else if ((present & 64) != 0) {
	    bounds.setRight(this.setCoordinate(data, bounds.getRight(), true));
	}

	if((present & 8) != 0) {
	    bounds.setBottom(this.setCoordinate(data, bounds.getBottom(), false));
	} else if ((present & 128) != 0) {
	    bounds.setBottom(this.setCoordinate(data, bounds.getBottom(), true));
	}

	if(data.getPosition() > data.getEnd()) {
	    throw new OrderException("Too far!");
	}
    }
	
    private int setCoordinate(Packet data, int coordinate,  boolean delta) {
	byte change = 0;

	if(delta) {
	    change = (byte)data.get8();
	    coordinate += (int)change;
	    return coordinate;
	} else {
	    coordinate = data.getLittleEndian16();
	    return coordinate;
	}
    }

    private int setColor(Packet data) {
	int color = 0;
	color = data.get8();
	data.incrementPosition(2);
	return color;
    }
    
    public void registerCache(Cache cache) {
	this.cache = cache;
    }
 
    private void drawText(Text2Order text2, int clipcx, int clipcy, int boxcx, int boxcy) throws RdesktopException {
	byte[] text = text2.getText();
	DataBlob entry = null;
	Glyph glyph = null;
	int offset = 0;
	int ptext = 0;
	int length = text2.getLength();
	int x = text2.getX();
	int y = text2.getY();

	if(text2.getBoxLeft() > 1) {
	    surface.fillRectangle(text2.getBoxLeft(), text2.getBoxTop(), boxcx, boxcy, text2.getBackgroundColor());
	} else if (text2.getMixmode() == MIX_OPAQUE) {
	    surface.fillRectangle(text2.getClipLeft(), text2.getClipTop(), clipcx, clipcy, text2.getBackgroundColor());
	}
	
	/*Debug.out.println("X: " + text2.getX() + " Y: " + text2.getY() +
			   " Left Clip: " + text2.getClipLeft() + " Top Clip: " + text2.getClipTop() +
			   " Right Clip: " + text2.getClipRight() + " Bottom Clip: " + text2.getClipBottom() +
			   " Left Box: " + text2.getBoxLeft() + " Top Box: " + text2.getBoxTop() +
			   " Right Box: " + text2.getBoxRight() + " Bottom Box: " + text2.getBoxBottom() +
			   " Foreground Color: " + text2.getForegroundColor() +
			   " Background Color: " + text2.getBackgroundColor() +
			   " Font: " + text2.getFont() + " Flags: " + text2.getFlags() +
			   " Mixmode: " + text2.getMixmode() + " Unknown: " + text2.getUnknown() +
			   " Length: " + text2.getLength());*/
	
	for(int i = 0; i < length;) {
	    switch(text[ptext + i]&0x000000ff) {
	    case (0xff):
		if(i + 2 < length) {
		    byte[] data = new byte[text[ptext + i + 2]&0x000000ff];
		    System.arraycopy(text ,ptext , data, 0, text[ptext + i + 2]&0x000000ff);
		    DataBlob db = new DataBlob(text[ptext + i + 2]&0x000000ff, data);
		    cache.putText(text[ptext + i + 1]&0x000000ff, db);
		} else {
		    throw new RdesktopException();
		}
		length -= i + 3;
		ptext = i + 3;
		i = 0;
		break;
		
	    case (0xfe):
		entry = cache.getText(text[ptext + i + 1]&0x000000ff);
		if((entry.getData()[1] == 0) && ((text2.getFlags() & TEXT2_IMPLICIT_X) == 0)) {
		    if((text2.getFlags() & 0x04) != 0) {
			y += text[ptext + i + 2]&0x000000ff;
		    } else {
			x += text[ptext + i + 2]&0x000000ff;
		    }
		}
		if(i + 2 < length) {
		    i += 3;
		} else {
		    i += 2;
		}
		length -= i;
		ptext = i;
		i = 0;
		byte[] data = entry.getData();
		for(int j = 0; j < entry.getSize(); j++) {
		    glyph = cache.getFont(text2.getFont(), data[j]&0x000000ff);
		    if((text2.getFlags() & TEXT2_IMPLICIT_X) == 0) {
			offset = data[++j]&0x000000ff;
			if((offset & 0x80) !=0) {
			    if((text2.getFlags() & TEXT2_VERTICAL) != 0) {
				y += (data[++j]&0x000000ff) | ((data[++j]&0x000000ff) << 8); 
			    } else {
				x += (data[++j]&0x000000ff) | ((data[++j]&0x000000ff) << 8);
			    }
			} else {
			    if((text2.getFlags() & TEXT2_VERTICAL) != 0) {
				y += offset;
			    } else {
				x += offset;
			    }
			}
		    }
		    if(glyph != null) {
			surface.drawGlyph(text2.getMixmode(), x + (short)glyph.getOffset(),
					  y + (short)glyph.getBaseLine(), glyph.getWidth(),
					  glyph.getHeight(), glyph.getFontData(), 0, 0,
					  text2.getBackgroundColor(), text2.getForegroundColor());
			
			if((text2.getFlags() & TEXT2_IMPLICIT_X) != 0) {
			    x += glyph.getWidth();
			}
		    }    
		}
		break;

	    default:
		glyph = cache.getFont(text2.getFont(), text[ptext + i]&0x000000ff);
		if((text2.getFlags() & TEXT2_IMPLICIT_X) == 0) {
		    offset = text[ptext + (++i)]&0x000000ff;
		    if((offset & 0x80) !=0) {
			if((text2.getFlags() & TEXT2_VERTICAL) != 0) {
			    y += (text[ptext + (++i)]&0x000000ff) | ((text[ptext + (++i)]&0x000000ff) << 8); 
			} else {
			    x += (text[ptext + (++i)]&0x000000ff) | ((text[ptext + (++i)]&0x000000ff) << 8);
			}
		    } else {
			if((text2.getFlags() & TEXT2_VERTICAL) != 0) {
			    y += offset; 
			} else {
			    x += offset;
			}
		    }
		}
		if(glyph != null) {
		    surface.drawGlyph(text2.getMixmode(), x + (short)glyph.getOffset(),
				      y + (short)glyph.getBaseLine(), glyph.getWidth(),
				      glyph.getHeight(), glyph.getFontData(), 0, 0,
				      text2.getBackgroundColor(), text2.getForegroundColor());
		    
		    if((text2.getFlags() & TEXT2_IMPLICIT_X) != 0) {
			x += glyph.getWidth();
		    }
		}
		i++;
		break;
	    }
	}
    }    
}


