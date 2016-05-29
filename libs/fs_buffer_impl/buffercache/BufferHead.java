package buffercache;

import jx.zero.Debug;
import jx.zero.Memory;
import jx.zero.*;

class BufferHead extends jx.fs.buffercache.BufferHead {
    static int sequence = 1;
    int id;

    private final static boolean paranoid = false;

    /** vom BufferHashtable verwendet */
    int hashtable_hashkey;
    BufferHead hashtable_nextInChain;

    /** vom BufferCache verwendet */
    int    b_flushtime;
    /** vom BufferCache verwendet */
    private  int     b_count, b_list;
    private boolean dirty, locked, uptodate;  // BufferCache

    /** die Blocknummer innerhalb des Dateisystems */
    int     b_block;
    /** die Blockgr&ouml;&szlig;e (1024, 2048 oder 4096 Byte) */
    int     b_size;


    BufferHead(MemoryManager memMgr, int block, int size) {
	super(memMgr.allocAligned(size, 4));
	this.b_block  = block;
	this.b_size   = size;
	b_count = 1;
	hashtable_hashkey = this.b_block;
	id = sequence++;
	if (size != 1024) throw new Error("only size 1024 supported");
    }

    final public int getBlock() { return b_block; }
    final public int getSize() { return b_size; }

    /**
     * Markiert den BufferHead als "dirty", d.h. sein Inhalt hat sich ge&auml;ndert und stimmt nicht mehr mit seinem Abbild
     * auf der Festplatte &uuml;berein (dem Inhalt des entsprechenden Blocks); er muss noch geschrieben werden.
     *
     * @param value falls <code>true</code>, muss der Inhalt noch geschrieben werden
     */
    final public void markDirty()    { dirty = true; }
    final public void markClean()    { dirty = false; }

    /**
     * Gibt an, ob der Inhalt des BufferHeads ge&auml;ndert und noch nicht auf die Festplatte geschrieben wurde.
     *
     * @return <code>true</code>, falls der Inhalt des BufferHeads nicht mit dem auf der Festplatte &uuml;bereinstimmt
     */
    final boolean dirty()    { return dirty; }
    final public boolean isDirty()    { return dirty; }

    /**
     * Sperrt den BufferHead bzw. gibt die Sperre des BufferHeads frei. F&uuml;r die Dauer der Sperre k&ouml;nnen keine anderen
     * Threads darauf zugreifen. Um auf das Ende der Sperre zu warten, dient die Methode <code>waitOn</code>; bei einer Freigabe
     * der Sperre lassen sich andere Threads/Prozesse mittels <code>notifyAll</code> wieder aktivieren.
     *
     * @param value der neue Zustand der Sperre
     */
    final public void lock() { locked = true; }
    final public void unlock() { locked = false; }

    /**
     * Liefert den Zustand der Sperre zur&uuml;ck.
     *
     * @param value <code>true</code>, falls der BufferHead gesperrt ist
     */
    final public boolean isLocked()   { return locked; }


    public void waitUntilUnlocked() {
	// TODO: make this atomic!
	throw new Error("NOT IMPLEMENTED");
	//	 if (isLocked()) {
	//  waitOn();
	//}
    }

    /**
     * Legt den Zustand des BufferHead-Inhalts fest. "Uptodate" bedeutet, dass die Leseoperation erfolgreich war und der
     * Inhalt g&uuml;ltig ist.
     *
     * @param value der neue Zustand des Inhalts: <code>false</code> bedeutet, dass bei der Operation ein Fehler aufgetreten
     *              und der Inhalt nicht mehr g&uuml;ltig ist
     */
    final public void markUptodate() { uptodate = true; }

    /**
     * Liefert den Zustand des BufferHead-Inhalts zur&uuml;ck.
     *
     * @return <code>false</code> bedeutet, dass bei der Operation ein Fehler aufgetreten und der Inhalt nicht mehr
     *         g&uuml;ltig ist
     */
    final public boolean isUptodate() { return uptodate; }

    /**
     * Wartet, bis die Sperre des BufferHeads aufgehoben wird.
     */
    public void waitOn() {
	if (locked) {
	    b_count++;
	    while (locked)
		Debug.out.println("TODO: implement WAIT QUEUE!");
	    ((CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager")).block();
	    b_count--;
	}
    }

    /**
     * Beendet den Lese- bzw. Schreibevorgang. Diese Methode wird vom Treiber aufgerufen.
     *
     * @param error zeigt an, ob bei der Operation ein Fehler aufgetreten ist (<code>true</code>)
     * @param synchronous falls <code>true</code>, wird auf das Ende der Operation (mittels <code>sleep</code>) gewartet
     */
    public void endIo(boolean error, boolean synchronous) {
	if (error)
	    throw new Error("IO Error");
	uptodate = true;
	locked = false;
	//if (synchronous)
	//    notifyAll();
    }

    /**
     * Clears the buffer.
     */
    final public void clear() {
	data.clear();
	//data.fill32((short)0,0,b_size>>2);
	//data.fill16((short)0, 0, b_size>>1);
	//for (int i = 0; i < b_size; i++)
	//  data.set8(i, (byte)0);
    }

    /**
     * L&ouml;scht einen Bereich des BufferHeadinhalts. Der Bereich wird auf 0 gesetzt.
     *
     * @param from das erste zu l&ouml;schende Byte des Inhalts
     * @param to   das erste Byte des Bereichs, das nicht mehr zu gel&ouml;scht werden soll
     */
    public void clear(int from, int to) {
	if ((from < 0) || (from >= b_size) || (to < 0) || (to > b_size))
	    return;
	for (int i = from; i < to; i++)
	    data.set8(i, (byte)0);
    }


    void init(int block) {
        b_count = 1;
        b_flushtime = 0;
        b_block = block;
	hashtable_hashkey = block;
	//clear(); // not necessary to clear buffer because it is set NOT up-to-date
	dirty = false;
	uptodate = false;
	locked = false;
    }

    final void ref() { b_count++; }
    final void unref() { 
	if (paranoid) {
	    if (b_count <= 0) throw new Error(); 
	}
	b_count--;
    }
    final boolean isUnused() { return b_count==0; }
    final public boolean isUsed() { return b_count>0; }
    final public boolean isUsedOnlyByMe() { return b_count==1; }


    final boolean isInFreeList() { return inlist; }
}
