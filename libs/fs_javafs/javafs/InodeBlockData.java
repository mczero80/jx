package javafs;

import jx.fs.buffercache.*;

final class InodeBlockData extends BufferHeadAccess {

    public InodeBlockData() {
    }

    public InodeBlockData(BufferHead bh, int offset) {
	init(bh, offset);
    }

    final public void bd_init(BufferHead bh, int offset) {
	init(bh,offset);
    }

    final public int  bd_entry()      { return readInt(0); }
    final public void bd_entry(int v) { writeInt(0, v); }
}
