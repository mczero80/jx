package jx.buffer;

import jx.zero.Debug;
import jx.zero.Memory;

/**
 * Requirements: 
 *  -every BufferHead is part of at most one list 
 * @author Michael Golm
 */
public class BufferList {
    private static final boolean check = false;
    private static final boolean trace = false;
    private static final boolean paranoid = false;
    int size=0;
    BufferHead first = null;
    BufferHead last = null;

    public BufferList() {}

    /**
     * Create list from Memory array
     */
    public BufferList(Memory[] bufs) {
	for(int i=0; i<bufs.length; i++) {
	    appendElement(new BufferHead(bufs[i]));
	}
    }

    final public int size() {
	return size;
    }
    /*   public boolean contains(BufferHead bh) {
	return bh.inlist;
    }
    */
    public void appendElement(BufferHead bh) {
	if (paranoid) {
	    if (bh.inlist) throw new Error("BH already in list");
	}
	bh.inlist = true;
	bh.prev = last;
	if (last==null) {
	    first = bh;
	} else {
	    last.next = bh;
	}
	bh.next = null;
	last = bh;
	//if (trace) Debug.out.println("APPEND "+bh.id);
	size++;
	if (trace) Debug.out.println("Freelist size:"+size);
	if (check) checkConsistency();
    }
    public void prependElement(BufferHead bh) {
	if (bh.inlist) throw new Error("BH already in list");
	bh.inlist = true;
	bh.next = first;
	if (last == null) {
	    last = bh;
	    first = bh;
	} else {
	    first.prev = bh;
	}
	bh.prev = null;
	first = bh;	
	//if (trace) Debug.out.println("PREPEND "+bh.id);
	size++;
	if (check) checkConsistency();
    }

    final public BufferHead firstElement() {
	return first;
    }

    final public BufferHead undockFirstElement() {
	
	if (first == null) return null;
	BufferHead ret = first;
	first = first.next;
	if (first != null) first.prev = null;
	ret.next = null;
	ret.prev = null;
	ret.inlist = false;
	size--;
	if (trace) Debug.out.println("Freelist size:"+size);
	if (size==0) {first=null; last=null;}
	if (check) checkConsistency();
	return ret;
    }
    final public void removeElement(BufferHead bh) {
	if (check) if (! bh.inlist) throw new Error("BH not in list");
	if (bh.prev == null) { // the first one
	    first = bh.next;
	} else {
	    bh.prev.next = bh.next; 
	}
	if (bh.next==null) { // the last one
	    last = bh.prev;
	} else {
	    bh.next.prev = bh.prev;
	}
	bh.next = null;
	bh.prev = null;
	bh.inlist = false;
	size--;
	if (trace) Debug.out.println("Freelist size:"+size);
	if (check) checkConsistency();
    }

    private void checkConsistency() {
	int c=0;
	BufferHead bh1;
	BufferHead bh = first;
	while(bh != null) {
	    if (! bh.inlist) throw new Error("inconsistent freelist");
	    c++;
	    if (c > size) throw new Error("inconsistent freelist");
	    if (c == size && bh != last) throw new Error("inconsistent freelist");
	    bh1 = bh.next;
	    if (bh1 != null && bh1.prev != bh) throw new Error("inconsistent freelist");
	    bh = bh1;
	}
	if (c != size) {
	    Debug.out.println("Computetd size: "+c);
	    Debug.out.println("size variable: "+size);
	    throw new Error("inconsistent freelist");
	}
	if (first != null && first.prev != null) throw new Error("inconsistent freelist");
	if (last != null && last.next != null) throw new Error("inconsistent freelist");
    }
}
