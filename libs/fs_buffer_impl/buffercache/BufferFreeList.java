package buffercache;

import jx.buffer.*;

import jx.zero.Debug;
//import jx.fs.buffercache.BufferHead;

/**
 * Requirements: 
 *  -every BufferHead is part of at most one list 
 */
/*
final class BufferFreeList {
    static final boolean check = false;
    static final boolean trace = false;
    int size=0;
    BufferHead first = null;
    BufferHead last = null;
    int size() {
	return size;
    }
    boolean contains(BufferHead bh) {
	return bh.inlist;
    }
    void appendElement(BufferHead bh) {
	if (bh.inlist) throw new Error("BH already in list");
	bh.inlist = true;
	bh.prev = last;
	if (last==null) {
	    first = bh;
	} else {
	    last.next = bh;
	}
	bh.next = null;
	last = bh;
	if (trace) Debug.out.println("APPEND "+bh.id);
	size++;
	if (trace) Debug.out.println("Freelist size:"+size);
	if (check) checkConsistency();
    }
    void prependElement(BufferHead bh) {
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
	if (trace) Debug.out.println("PREPEND "+bh.id);
	size++;
	if (check) checkConsistency();
    }

    BufferHead firstElement() {
	return first;
    }

    BufferHead undockFirstElement() {
	
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
    void removeElement(BufferHead bh) {
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
*/

final class BufferFreeList extends BufferList {
}
