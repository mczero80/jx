package javafs;

import jx.fs.buffercache.*;

class SymlinkBlockData extends BufferHeadAccess {
    public SymlinkBlockData(BufferHead bh, int offset) {
	init(bh, offset);
    }

    public String i_symlink(int len)  { return readString(0, len); }
    public void   i_symlink(String v) { writeString(0, v, v.length()); }
}
