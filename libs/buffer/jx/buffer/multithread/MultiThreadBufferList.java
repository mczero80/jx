package jx.buffer.multithread;

import jx.buffer.*;

import jx.zero.*;

/**
 * Requirements: 
 *  - every BufferHead is part of at most one list 
 *  - single producer, single consumer
 *  - one thread removes buffers from the list head (consumer)
 *  - another thread appends buffer at the list tail (producer)
 *  - the producer can interrupt the consumer
 * - prev link is not maintained, i.e. only a single-linked list is implemented
 * @author Michael Golm
 */
public class MultiThreadBufferList implements BufferProducer, BufferConsumer {
    /* Implementation details:
     * If the buffer contains two elements, producer and consumer have no shared data and
     * can operate in parallel on the list.
     * The list initially contains no elements.
     * The consumer atomically checks first_producer and blocks. (lost update problem if not done atomically!)
     * 
     */ 
    static final boolean check = false;
    static final boolean trace = false;


    boolean verbose = false;
    boolean requireMoreData = false;
    Buffer first;
    Buffer last;
    CPUManager cpuManager;
    CPUState consumer;

    int size;

    String name; // for debugging

    int event_list;
    boolean recording=false;

    public MultiThreadBufferList() {
	this(new Buffer(null), null);
	//throw new Error();
    }

    /**
     * Create list from Memory array
     */
    public MultiThreadBufferList(Memory[] bufs) {
	this(new Buffer(bufs[0]), null);
	if (verbose) {
	//    cpuManager.dump("MultiThreadBufferList(Memory["+bufs.length+"])", this);
	}
	for(int i=1; i<bufs.length; i++) {
	    if (verbose) Debug.out.println("       loop:"+i);
	    appendElement(new Buffer(bufs[i]));
	}
	if (verbose) dump();
    }

    public MultiThreadBufferList(Buffer initial, CPUState consumer) {
	if (verbose) Debug.out.println("MultiThreadBufferList(Buffer,CPUState)");
	first = initial;
	last = initial;
	this.consumer = consumer;
	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	if (verbose) cpuManager.dump("MultiThreadBufferList",this);
	size = 1;
    }

     /** for debugging */
    public void setListName(String name) {
	this.name = name;
    }

    public void enableRecording(String eventName) {
	event_list = cpuManager.createNewEvent(eventName);
	recording = true;
    }

    public MultiThreadBufferList(Buffer initial) {
	this(initial, null);
    }

    public void setConsumer(CPUState consumer) {
	this.consumer = consumer;
    }

    public CPUState getConsumer() {
	return consumer;
    }

    public void appendElement(Buffer bh) {
	//if (verbose) Debug.out.println("LIST::appendElement data="+bh.data);
	if (requireMoreData && bh.moreData == null) throw new Error("Buffer contains no moreData");
	if (check) checkConsistency();
	//if (verbose) cpuManager.dump("MultiThreadBufferList::APPEND",this);
	/* debug FIXME HACK */
	/*
	if (bh.data != null && bh.data.size() != 1514) {
	    //Debug.out.println("D3C905: error: got="+bh.data.size()+", need=1514");
	    //throw new Error("Ether: NEED LARGER MEMORY");
	    bh.data = bh.data.extendFullRange();
	}
	*/
	bh.next.set(null);
	last.next.atomicUpdateUnblock(bh, consumer);
	last = bh;
	size++;
	if (recording) cpuManager.recordEventWithInfo(event_list, 0);
	if (check) checkConsistency();
    }
    
    
    public Buffer undockFirstElement() {	
	if (verbose) cpuManager.dump("MultiThreadBufferList::UNDOCK",this);
	if (check) checkConsistency();
	if (consumer==null) {consumer=cpuManager.getCPUState();}

	/*
        Buffer x;
        do {
           first.next.blockIfEqual(null);
           x = (Buffer)first.next.get();
           } while(x == null);
       //if (x==null) throw new Error("Unblocked and element is null?? Should not happen if I am the only consumer in list "         +(name!=null?name:"unnamed list")); // TimerManager unblocks us???
       first = x;
	*/
	first.next.blockIfEqual(null);
	consumer = null;
	if (first.next.get() == null) {
	    Debug.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    Debug.out.println("unblocked, but no element in list!!. This  may be an error!");
	    Debug.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    return null; // erlaubt einen Unblock von aussen
	}
	Buffer ret = first;  // beware of race conditions!
	first = (Buffer)first.next.get();

	ret.copyDataFrom(first); // move memref from next to current
	size--;
	cpuManager.recordEventWithInfo(event_list, 1);
	if (check) checkConsistency();
	return ret;
    }

    public Buffer nonblockingUndockFirstElement() {	
	if (verbose) {
	    cpuManager.dump("MultiThreadBufferList::NONBLOCKINGUNDOCK",this);
	    dump();
	}
	if (check) checkConsistency();
	if (first.next.get() == null) return null;
	Buffer ret = first;
	first = (Buffer)first.next.get();
	ret.copyDataFrom(first); // move memref from next to current
	size--;
	if (recording) cpuManager.recordEventWithInfo(event_list, 1);
	if (check) checkConsistency();
	return ret;
    }

    public void dump() {
	Buffer b = first;
	cpuManager.dump("Dump of list: ", this);
	while(b!=null) {
	    cpuManager.dump(" Node: ", b);
	    //cpuManager.dump(" Data: ", b.data);
	    b = (Buffer) b.next.get();
	}
    }

    final private void checkConsistency() {
	
	// check last pointer
	if (last.next.get() != null)
	    Debug.out.println("last.next != null");
	
	// check size
	Buffer b = first;
	int sz=0;
	while(b!=null) {
	    b = (Buffer) b.next.get();
	    sz++;
	}
	if (sz != size) {
	    Debug.out.println("size info is not valid ["+name+"] (is: "+sz+" should:"+size+")");
	    dump();
	}
	
	// check for duplicates
	Buffer b1 = first;
	Buffer b2 = first;
	
	for (b1 = first; b1 != null; b1 = (Buffer) b1.next.get()) {
	    for (b2 = (Buffer)b1.next.get(); b2 != null; b2 = (Buffer) b2.next.get()) {
		if (b1 == b2) {
		    Debug.out.print("duplicate Node found!!! ["+name+"] ");
		    cpuManager.dump(" duplicate: ", b1);
		    dump();
		    throw new Error("duplicates found!!!");
		}
	    }
	}
    }
    
    public int size() { return size; }

    public void setVerbose(boolean v) { verbose = v; }
    public void requireMoredata(boolean v) { requireMoreData = v; }
}
