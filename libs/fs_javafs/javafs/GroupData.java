package javafs;

import jx.fs.buffercache.*;

class GroupData extends BufferHeadAccess {
    public GroupData(BufferHead bh, int offset) {
	init(bh, offset);
	length = 32;
    }

    // Blocks bitmap block
    public int   bg_block_bitmap()             { return readInt(0); }
    public void  bg_block_bitmap(int v)        { writeInt(0, v); }

    // Inodes bitmap block
    public int   bg_inode_bitmap()             { return readInt(4); }
    public void  bg_inode_bitmap(int v)        { writeInt(4, v); }

    // Inodes table block
    public int   bg_inode_table()              { return readInt(8); }
    public void  bg_inode_table(int v)         { writeInt(8, v); }

    // Free blocks count
    public short bg_free_blocks_count()        { return (short)readShort(12); }
    public void  bg_free_blocks_count(short v) { writeShort(12, v); }

    // Free inodes count
    public short bg_free_inodes_count()        { return (short)readShort(14); }
    public void  bg_free_inodes_count(short v) { writeShort(14, v); }

    // Directories count
    public short bg_used_dirs_count()          { return (short)readShort(16); }
    public void  bg_used_dirs_count(short v)   { writeShort(16, v); }

    // reserved: 14
}
