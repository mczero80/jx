package javafs;

import jx.fs.buffercache.*;

class DirEntryData extends BufferHeadAccess {
    public static final int FT_REG_FILE = 1;
    public static final int FT_DIR      = 2;
    public static final int FT_SYMLINK  = 3;

    public DirEntryData(BufferHead bh, int offset) {
	init(bh, offset);
    }

    public DirEntryData() { }

    // Inode number
    public int    inode()           { return readInt(0); }
    public void   inode(int v)      { writeInt(0, v); }

    // Directory entry length
    public short  rec_len()         { return (short)readShort(4); }
    public void   rec_len(short v)  { writeShort(4, v); }

    // Name length
    public short  name_len()        { return (short)readShort(6); }
    public void   name_len(short v) { writeShort(6, v); }

    // File type
    //public byte   file_type()       { return readByte(7); }
    //public void   file_type(byte v) { writeByte(7, v); }

    // File name
    public String name()            { return readString(8, name_len()); }
    public void   name(String v)    { writeString(8, v, rec_len()-8); }
}
