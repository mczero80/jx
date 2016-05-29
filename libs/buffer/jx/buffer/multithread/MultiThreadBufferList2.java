package jx.buffer.multithread;

import jx.buffer.*;

import jx.zero.*;

public class MultiThreadBufferList2 extends MultiThreadBufferList {

    public MultiThreadBufferList2() {
	super(new Buffer2(null), null);
    }

    public MultiThreadBufferList2(Memory[] bufs) {
	super(new Buffer2(bufs[0]), null);
	//if(1==1)throw new Error("TODO: extendRange -> split"); //super(new Buffer2(bufs[0].extendFullRange()), null);
	if (verbose) {
	    cpuManager.dump("MultiThreadBufferList2(Memory["+bufs.length+"])", this);
	}
	for(int i=1; i<bufs.length; i++) {
	    if (verbose) Debug.out.println("       loop:"+i);
	    //if(1==1)throw new Error("TODO: extendRange -> split"); //appendElement(new Buffer2(bufs[i].extendFullRange()));
	    appendElement(new Buffer2(bufs[i]));
	}
	if (verbose) dump();
    }

}
