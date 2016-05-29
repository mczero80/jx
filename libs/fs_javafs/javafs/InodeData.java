package javafs;

import jx.fs.buffercache.*;

class InodeData extends BufferHeadAccess {
    public InodeData(BufferHead bh, int offset) {
	init(bh, offset);
	length = 128;
    }

    // File mode
    final public short  i_mode()               { return (short)readShort(0); }
    final public void   i_mode(short v)        { writeShort(0, v); }

    // Owner Uid
    final public short  i_uid()                { return (short)readShort(2); }
    final public void   i_uid(short v)         { writeShort(2, v); }

    // Size in bytes
    final public int    i_size()               { return readInt(4); }
    final public void   i_size(int v)          { writeInt(4, v); }

    // Access time
    final public int   i_atime()              { return readTime(8); }
    final public void  i_atime(int v)         { 
	// I inlined getLittleEndian32 manually mg
	memory.setLittleEndian32(offset+8, v);
	//writeTime(8, v);
    }

    // Creation time
    final public int    i_ctime()              { return readTime(12); }
    final public void   i_ctime(int v)        { writeTime(12, v); }

    // Modification time
    final public int    i_mtime()              { return readTime(16); }
    final public void   i_mtime(int v)        { writeTime(16, v); }

    // Deletion Time
    final public int    i_dtime()              { return readTime(20); }
    final public void   i_dtime(int v)        { writeTime(20, v); }

    // Group Id
    final public short  i_gid()                { return (short)readShort(24); }
    final public void   i_gid(short v)         { writeShort(24, v); }

    // Links count
    final public short  i_links_count()        { return (short)readShort(26); }
    final public void   i_links_count(short v) { writeShort(26, v); }

    // Blocks count
    final public int    i_blocks()             { return readInt(28); }
    final public void   i_blocks(int v)        { writeInt(28, v); }

    // Flags
    final public int    i_flags()              { return readInt(32); }
    final public void   i_flags(int v)         { writeInt(32, v); }

    // Pointers to blocks
    /*
    final public int    i_block(int i)         {
	// I inlined getLittleEndian32 manually mg
	if (i >= 0 && i < 15) return memory.getLittleEndian32(offset+ 40 + i*4); //return readInt(40 + i*4);
	else throw new Error();
    }
    */
    final public int    i_block(int i)         {
	if (i >= 0 && i < 15) return readInt(40 + i*4);
	else throw new Error();
    }

    final public void   i_block(int i, int v)  {
	if (i >= 0 && i < 15) writeInt(40 + i*4, v);
	else throw new Error();
    }

    // oder: Symlink-String
    final public String i_symlink(int len)     { return readString(40, len); } // max. 60
    final public void   i_symlink(String v)    { writeString(40, v, v.length()); }

    // Version
    final public int    i_version()            { return readInt(100); }
    final public void   i_version(int v)       { writeInt(100, v); }

    // File ACL
    final public int    i_file_acl()           { return readInt(104); }
    final public void   i_file_acl(int v)      { writeInt(104, v); }

    // Dir ACL
    final public int    i_dir_acl()            { return readInt(108); }
    final public void   i_dir_acl(int v)       { writeInt(108, v); }

    // Fragment Adress
    final public int    i_faddr()              { return readInt(112); }
    final public void   i_faddr(int v)         { writeInt(112, v); }

    // Fragment Number
    final public byte   l_i_frag()             { return (byte)readByte(116); }
    final public void   l_i_frag(byte v)       { writeByte(116, v); }

    // Fragment Size
    final public byte   l_i_fsize()            { return (byte)readByte(117); }
    final public void   l_i_fsize(byte v)      { writeByte(117, v); }

    // reserved: 10
}
