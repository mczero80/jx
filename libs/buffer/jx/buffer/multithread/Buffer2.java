package jx.buffer.multithread;

import jx.zero.Debug;

import jx.zero.Memory;
import jx.zero.*;

public class Buffer2 extends Buffer {
    int offs, size;
    private static boolean avoidSplitting = true;

    public Buffer2(Memory mem) {
	super(join(mem));
	offs = 0;
	if (mem != null) size = mem.size();
    }

    public Memory getMem() {
	throw new Error("subrange!");
	//return data;
    }

    public Memory getData() {
	//Debug.out.println("    getData: offs="+offs+", size=" + size);
	//if (1==1) throw new Error();
	return data.getSubRange(offs, size);
	//return data;
    }


    public void setData(Memory m) {
	/*
	if(1==1)throw new Error("TODO: extendRange -> split"); //
	setData(m, -1, -1);
	*/
	//Memory x = m.extendFullRange();


	//Memory x = join(m);
	setData(m, 0, m.size());
    }

    public void setData(Memory m,int offs, int size) {
        /*
	if(1==1)throw new Error("TODO: extendRange -> split"); //
	this.data = m;
        */
	//this.data = m.extendFullRange();
	if (avoidSplitting) {
	    if (m.size() == 1514) this.data = m;
	    else this.data = m.joinAll();
	} else {
	    this.data = m.joinAll();
	}
	this.offs = offs;
	this.size = size;
    }

    public void copyDataFrom(Buffer b) {
	super.copyDataFrom(b);
	Buffer2 b2 = (Buffer2)b;
	offs = b2.offs;
	size = b2.size;
    }

    public Memory getRawData() {
	return data;
    }

    public int getOffset() {
	return offs;
    }

    public int getSize() {
	return size;
    }

    static private Memory join(Memory m) {
	if (m==null) return null;
	Memory x = m.joinAll();
	if (x.size() < 1514) {
	    Debug.out.println("error: got="+x.size()+", need=1514");
	    CPUManager cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	    cpuManager.dump("setData x: ", x);
	    cpuManager.dump("setData:m ", m);
	    throw new Error("NEED LARGER MEMORY");
	}
	return x;
    }

}
