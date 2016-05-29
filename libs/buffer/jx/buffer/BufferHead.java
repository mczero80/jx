package jx.buffer;

import jx.zero.Debug;

import jx.zero.Memory;
import jx.zero.*;

/**
 * Objekte der Klasse BufferHead werden verwendet, um Daten zwischen NIC und Netzwerkstack und innerhalb
 * des Netzwerkstacks zu &uuml;bertragen. Sie besitzen ein Memory-Objekt (<code>b_data</code>) in der vom NIC verwendeten
 * Blockgr&ouml;&szlig;e.
 */
public class BufferHead {
    /** vom BufferHashtable verwendet */
    //BufferHeadHashKey b_hashkey;

    /** vom BufferFreeList verwendet */
    BufferHead prev, next;
    protected boolean inlist=false;

    /** das Speicherobjekt mit dem Inhalt des Blocks */
    public  Memory  data;

    /** die Blockgr&ouml;&szlig;e */
    public  int     size;

    public BufferHead(int size) {
	this.size   = size;
	MemoryManager memorymanager = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	data = memorymanager.alloc(size);
    }
    public BufferHead(Memory mem) {
	this.size   = mem.size();
	data = mem;
    }

    public final Memory getData() { return data; }
    public final void setData(Memory data) { this.data = data; }
}
