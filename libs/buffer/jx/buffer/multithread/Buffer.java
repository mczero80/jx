package jx.buffer.multithread;

import jx.zero.Debug;

import jx.zero.Memory;
import jx.zero.*;

public class Buffer {
    AtomicVariable next;
    Memory  data;
    Object moreData;

    CPUManager cpuManager ;

    public Buffer(Memory mem) {
	data = mem;
	 cpuManager = (CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager");
	next = cpuManager.getAtomicVariable();
	/*if (data != null && data.size() != 1514) {
	    throw new Error("Ether: NEED LARGER MEMORY");
	    }*/
    }

    public Memory getMem() {
	//return ((Buffer) next.get()).data;
	return data;
    }

    public void setMem(Memory m) {
	data=m;
	/*if (data != null && data.size() != 1514) {
	    throw new Error("Ether: NEED LARGER MEMORY");
	    }*/
    }

    public Memory getData() {
	return data;
    }

    public Object getMoreData() {
	return moreData;
    }

    public void setMoreData(Object m) {
	if (m==null) throw new Error("");
	moreData = m;
    }
    public void setData(Memory m) {
	/* debug FIXME HACK */
	data=m;
	/*if (data != null && data.size() != 1514) {
	    throw new Error("Ether: NEED LARGER MEMORY");
	    }*/
    }

    public void copyDataFrom(Buffer b) {
	data=b.data;
	moreData=b.moreData;
	if (data != null && data.size() != 1514) {
	  //throw new Error("Ether: NEED LARGER MEMORY");
	  //cpuManager.dump("TOO SMALL", data);
	  //cpuManager.printStackTrace();
	}
    }


    /** 
     * make the buffer reusable; init (zero-out) fields
     */
    public void init() {
	data = null;
	moreData = null;
    }
}
