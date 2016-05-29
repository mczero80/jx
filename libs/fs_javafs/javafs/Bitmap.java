package javafs;

import jx.zero.Debug;
import jx.zero.Memory;
import jx.fs.buffercache.*;

/**
 * Die Klasse Bitmap stellt die Schnittstelle zu den Block- und Inodebitmaps dar.
 * Sie bietet Methoden zum
 * Finden eines freien Bits und zum Setzen und Loeschen einzelner Bits in einer Bitmap.
 * Darueberhinaus besteht die
 * M&ouml;glichkeit, die Zahl gesetzter Bits zu ermitteln und eine Bitmap zu l&ouml;schen.
 */
class Bitmap {
    private Memory bitmap;
    private int blocksize;
    private boolean bitmap_loaded;
    public  int block_nr;
    private BufferHead bh; // public
    private static final int nibblemap[] = new int[] {4, 3, 3, 2, 3, 2, 2, 1, 3, 2, 2, 1, 2, 1, 1, 0};
    private BufferCache buffercache;

    public static final boolean traceBitmap = false;

    public Bitmap(BufferCache bufferCache, int blocksize, int block_nr) {
	this.blocksize = blocksize;
	this.block_nr = block_nr;
	this.buffercache = bufferCache;
	bitmap_loaded = false;
    }

    /**
     * Ermittelt die Anzahl der freien Bl&ouml;cke anhand der Bitmap.
     *
     * @param count die Anzahl zu &uuml;berpr&uuml;fender Bl&ouml;cke
     * @return die Anzahl der freien Bl&ouml;cke
     */
    public  short countFree(int count) throws BufferIOException {
        int i;
	short sum = 0;

	//Debug.out.println("countFreeBlocks(): " + count);
	loadBitmap(); // throws BufferIOException

	for (i = 0; i < (count / 8) /*bitmap.length*/; i++) {
	    sum += nibblemap[bitmap.get8(i) & 0xf] + nibblemap[(bitmap.get8(i)>>4) & 0xf];
	}
	if ((count % 8) != 0) {
	    for (i *= 8; i < count; i++)
		sum += 1 - testBit(i);
	}

        return sum;
    }

    /**
     * Liest die Blockbitmap von Festplatte. Das ist notwendig, falls die folgenden Methoden bSetBit,
     * bClearBit und bTestBit angewandt werden sollen.
     *
     * @exception BufferIOException falls ein Fehler bei der Ein-/Ausgabe auftritt
     */
    public  void loadBitmap() throws BufferIOException {
	if (bitmap_loaded)
	    return;
	bh = buffercache.bread(block_nr);
	if (bh == null)
	    throw new BufferIOException("no buffer head");
	bitmap = bh.getData();
	if (bitmap==null)
	    throw new BufferIOException("no block memory");
	bitmap_loaded = true;
    }

    /**
     * L&ouml;scht die Blockbitmap, d.h. alle Bl&ouml;cke der Gruppe werden als frei markiert.
     */
    public  void clearBitmap() {
	if (bitmap_loaded)
	    for (int k = 0; k < bitmap.size(); k++) {
		/*Debug.out.println("S"+k);*/
		bitmap.set8(k, (byte)0);
	    }
	if (traceBitmap) Debug.out.println("Bitmap "+block_nr+" cleared.");
    }

    /**
     * Markiert den <code>Bufferhead</code>, der die Blockbitmap enth&auml;lt, als "dirty", um dem BufferCache mitzuteilen,
     * da&szlig; er noch geschrieben werden mu&szlig;.
     */
    public void markBitmapDirty() {
	if (bitmap_loaded)
	    bh.markDirty();
    }

    /**
     * Releases the buffer that contains the bitmap.
     */
    public void releaseBitmap() {
	if (bitmap_loaded) {
	    buffercache.bdwrite(bh);
	    bitmap_loaded = false;
	    bitmap = null;
	}
    }

    /**
     * Setzt das entsprechende Bit in der Blockbitmap, damit wird der Block als belegt markiert.
     *
     * @param nr die Nummer des Bits innerhalb der Blockbitmap
     */
    public  void setBit(int nr) {
	bitmap.set8(nr >> 3, (byte)(bitmap.get8(nr>>3) | (1 << (nr & 7))));
	if (traceBitmap) Debug.out.println("Bitmap "+block_nr+" set bit "+nr);
    }

    /**
     * L&ouml;scht das entsprechende Bit in der Blockbitmap, damit wird der Block als frei markiert.
     *
     * @param nr die Nummer des Bits innerhalb der Blockbitmap
     */
    public  void clearBit(int nr) {
	bitmap.set8(nr >> 3, (byte)(bitmap.get8(nr>>3) & ~(1 << (nr & 7))));
	if (traceBitmap) Debug.out.println("Bitmap "+block_nr+" clear bit "+nr);
    }

    /**
     * Gibt das entsprechende Bit in der Blockbitmap zur&uuml;ck (1 bedeutet Block belegt, 0 frei).
     *
     * @param nr die Nummer des Bits innerhalb der Blockbitmap
     * @return das Bit mit der angegebenen Nummer
     */
    public  int testBit(int nr) {
	if ((bitmap.get8(nr>>3) & (1 << (nr & 7))) > 0)
	    return 1;
	return 0;
    }

    /**
     * Sucht nach 8 freien Bit (einem Byte), d.h. nach einer Gruppe von Bl&ouml;cken, die nicht verwendet werden.
     *
     * @return die Nummer (Position in der Blockbitmap) des ersten Blocks
     */
    public  int findFirstZeroByte() {
	int k;

	for (int j = 0; j < bitmap.size(); j++) {
	    if (bitmap.get8(j) == 0) {
		for (k = 0; k < 7 && j > 0 && ((bitmap.get8(j-1) & (1 << k)) == 0); k++) ;
		return (j*8 + k);
	    }
	}
	return -1;
    }

    /**
     * Sucht ab der angegebenen Position nach 8 freien Bit (einem Byte), d.h. nach einer Gruppe von Bl&ouml;cken, die nicht
     * verwendet werden.
     *
     * @param offset der Offset innerhalb der Blockbitmap, ab dem gesucht werden soll
     * @return die Nummer (Position in der Blockbitmap) des ersten Blocks
     */
    public  int findNextZeroByte(int offset) { // Bitoffset
	int k;

	for (int j = offset>>3; j < bitmap.size(); j++) {
	    if (bitmap.get8(j) == 0) {
		for (k = 0; k < 7 && j > 0 && ((bitmap.get8(j-1) & (1 << k)) == 0); k++) ;
		return (j*8 + k);
	    }
	}
	return -1;
    }

    /**
     * Sucht nach dem ersten freien Bit, d.h. nach einem Block, der nicht verwendet wird.
     *
     * @return die Nummer (Position in der Blockbitmap) des ersten freien Blocks
     */
    public  int findFirstZeroBit() {
	for (int j = 0; j < bitmap.size(); j++) {
	    if (bitmap.get8(j) != (byte)0xff) {
		for (int k = 0; k < 8; k++) {
		    if (((bitmap.get8(j) >> k) & 1) == 0)
			return (j*8 + k);
		}
	    }
	}
	return -1;
    }

    /**
     * Sucht ab der angegebenen Position nach einem freien Bit, d.h. nach einem Block, der nicht verwendet wird.
     *
     * @param offset der Offset innerhalb der Blockbitmap, ab dem gesucht werden soll
     * @return die Nummer (Position in der Blockbitmap) des freien Blocks
     */
    public  int findNextZeroBit(int offset) {
	for (int j = offset>>3; j < bitmap.size(); j++) {
	    if (bitmap.get8(j) != (byte)0xff) {
		for (int k = 0; k < 8; k++) {
		    if (((bitmap.get8(j) >> k) & 1) == 0)
			return (j*8 + k);
		}
	    }
	}
	return -1;	
    }
}
