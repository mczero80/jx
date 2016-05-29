package jx.rdp;

import java.awt.*;

public class Cache {

    private Bitmap[][] bitmapcache = new Bitmap[3][600];
    private MSCursor[] cursorcache = new MSCursor[32];
    private Glyph[][] fontcache = new Glyph[12][256];
    private DataBlob[] textcache = new DataBlob[256];
    private byte[] deskcache = new byte[921600];

    public Cache() {
    }


    public Bitmap getBitmap(int cache_id, int cache_idx) throws RdesktopException {

	Bitmap bitmap = null;

	if((cache_id < bitmapcache.length) && (cache_idx < bitmapcache[0].length)) {
	    bitmap = bitmapcache[cache_id][cache_idx];
	    if (bitmap !=null) {
		return bitmap;
	    }
	}

	throw new RdesktopException("Could not get Bitmap!");
    }

    public void putBitmap(int cache_id, int cache_idx, Bitmap bitmap) throws RdesktopException {

       	if((cache_id < bitmapcache.length) && (cache_idx < bitmapcache[0].length)) {
	    bitmapcache[cache_id][cache_idx] = bitmap;
	} else {
	    throw new RdesktopException("Could not put Bitmap!");
	}
    }

    public MSCursor getCursor(int cache_idx) throws RdesktopException {
	MSCursor cursor = null;

	if (cache_idx < cursorcache.length) {
	    cursor = cursorcache[cache_idx];
	    if(cursor != null) {
		return cursor;
	    }
	}
	throw new RdesktopException("Cursor not found");
    }

    public void putCursor(int cache_idx, MSCursor cursor) throws RdesktopException {
	
	if(cache_idx < cursorcache.length) {
	    cursorcache[cache_idx] = cursor;
	} else {
	    throw new RdesktopException("Could not put Cursor!");
	}
    }
	
    public void putFont(Glyph glyph) throws RdesktopException {
	if((glyph.getFont() < fontcache.length) && (glyph.getCharacter() < fontcache[0].length)) {
	    fontcache[glyph.getFont()][glyph.getCharacter()] = glyph;
	} else {
	    throw new RdesktopException("Could not put font");
	}
    }

    public Glyph getFont(int font, int character) throws RdesktopException {
	
	if((font < fontcache.length) && ( character < fontcache[0].length)) {
	    Glyph glyph = fontcache[font][character];
	    if(glyph != null) {
		return glyph;
	    }
	}
	throw new RdesktopException("Could not get Font:" + font + ", " + character);
    }

    public DataBlob getText(int cache_id) throws RdesktopException {
	DataBlob entry = null;
	if(cache_id < textcache.length) {
	    entry = textcache[cache_id];
	    if(entry != null) {
		if(entry.getData() != null) {
		    return entry;
		}
	    }
	}

	throw new RdesktopException("Could not get Text:" + cache_id);
    }

    public void putText(int cache_id, DataBlob entry) throws RdesktopException {
	if(cache_id < textcache.length) {
	    textcache[cache_id] = entry;
	} else {
	    throw new RdesktopException("Could not put Text");
	}
    }

    public void putDesktop(int offset, int cx, int cy, byte[] data) throws RdesktopException {
	int length = cx * cy;
	int pdata = 0;

	if((int)offset + length <= deskcache.length) {
	    for(int i = 0; i < cy; i++) {
		System.arraycopy(data, pdata, deskcache, offset, cx);
		offset += cx;
		pdata += cx;
	    }
	} else {
	    throw new RdesktopException("Could not put Desktop");
	}
    }

    public byte[] getDesktop(int offset, int cx, int cy) throws RdesktopException {
	int length = cx * cy;
	int pdata = 0;
	byte[] data = new byte[length];

	if((int)offset + length <= deskcache.length) {
	    for(int i = 0; i < cy; i++) {
		System.arraycopy(deskcache, offset, data, pdata, cx);
		offset += cx;
		pdata += cx;
	    }
	    return data;
	}
	throw new RdesktopException("Could not get Bitmap");
    }
	
}

    
