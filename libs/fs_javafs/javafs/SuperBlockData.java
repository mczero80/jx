package javafs;

import jx.zero.*;

import jx.fs.buffercache.*;

final class SuperBlockData extends BufferHeadAccess {
    public SuperBlockData(BufferHead bh) {
	init(bh, 0);
	length = 1024;
    }

    // Gesamtzahl Inodes = (Gesamtzahl Bloecke * Blockgroesse)/4096
    final public int    s_inodes_count()           { return readInt(0); }
    final public void   s_inodes_count(int v)      { writeInt(0, v); }

    // Gesamtzahl Bloecke (s. metaxa.os.devices.ide.IDEDevice.getCapacity())
    final public int    s_blocks_count()           { return readInt(4); }
    final public void   s_blocks_count(int v)      { writeInt(4, v); }

    // Gesamtzahl reservierte Bloecke
    final public int    s_r_blocks_count()         { return readInt(8); }
    final public void   s_r_blocks_count(int v)    { writeInt(8, v); }

    // Gesamtzahl freier Bloecke
    final public int    s_free_blocks_count()      { return readInt(12); }
    final public void   s_free_blocks_count(int v) { writeInt(12, v); }

    // Gesamtzahl freier Inodes
    final public int    s_free_inodes_count()      { return readInt(16); }
    final public void   s_free_inodes_count(int v) { writeInt(16, v); }

    // = 0 falls block_size > 1024, = 1 sonst (erster Block des FS, enthaelt Superbl.)
    final public int    s_first_data_block()       { return readInt(20); }
    final public void   s_first_data_block(int v)  { writeInt(20, v); }

    // Blockgroesse
    final public int    s_log_block_size()          { return readInt(24); }
    final public void   s_log_block_size(int v)     { writeInt(24, v); }

    // Fragmentgroesse
    final public int    s_log_frag_size()          { return readInt(28); }
    final public void   s_log_frag_size(int v)     { writeInt(28, v); }

    // Anzahl Bloecke pro Blockgruppe = Blockgroesse * 8
    final public int    s_blocks_per_group()       { return readInt(32); }
    final public void   s_blocks_per_group(int v)  { writeInt(32, v); }

    // Anzahl Fragmente pro Blockgruppe
    final public int    s_frags_per_group()       { return readInt(36); }
    final public void   s_frags_per_group(int v)  { writeInt(36, v); }

    // Anzahl Inodes pro Blockgruppe = (Gesamtzahl Inodes)/(Anzahl Blockgruppen)
    final public int    s_inodes_per_group()       { return readInt(40); }
    final public void   s_inodes_per_group(int v)  { writeInt(40, v); }

    // Zeit des letzten mounts
    final public int    s_mtime()                  { return readTime(44); }
    final public void   s_mtime(int v)             { writeTime(44, v); }

    // Zeit der letzten Aenderung
    final public int    s_wtime()                  { return readTime(48); }
    final public void   s_wtime(int v)             { writeTime(48, v); }

    // Mountcount
    final public short  s_mnt_count()              { return (short)readShort(52); }
    final public void   s_mnt_count(short v)       { writeShort(52, v); }

    // Max. Mountcount
    final public short  s_max_mnt_count()          { return (short)readShort(54); }
    final public void   s_max_mnt_count(short v)   { writeShort(54, v); }

    // Magic signature
    final public short  s_magic()                  { return (short)readShort(56); }
    final public void   s_magic(short v)           { writeShort(56, v); }

    // Status
    final public short  s_state()                  { return (short)readShort(58); }
    final public void   s_state(short v)           { writeShort(58, v); }

    // Fehler
    final public short  s_errors()                 { return (short)readShort(60); }
    final public void   s_errors(short v)          { writeShort(60, v); }

    // Revision
    final public short  s_minor_rev_level()        { return (short)readShort(62); }
    final public void   s_minor_rev_level(short v) { writeShort(62, v); }

    // Zeit des letzten Dateisystem-Checks
    final public int    s_lastcheck()              { return readTime(64); }
    final public void   s_lastcheck(int v)         { writeTime(64, v); }

    // Max. Zeit zwischen zwei Checks
    final public int    s_checkinterval()          { return readTime(68); }
    final public void   s_checkinterval(int v)     { writeTime(68, v); }
    
    // Betriebssystem
    final public int    s_creator_os()             { return readInt(72); }
    final public void   s_creator_os(int v)        { writeInt(72, v); }

    // Revision
    final public int    s_rev_level()              { return readInt(76); }
    final public void   s_rev_level(int v)         { writeInt(76, v); }

    // voreingestellte UID fuer reservierte Bloecke
    final public short  s_def_resuid()             { return (short)readShort(80); }
    final public void   s_def_resuid(short v)      { writeShort(80, v); }
    
    // voreingestellte GID fuer reservierte Bloecke
    final public short  s_def_resgid()             { return (short)readShort(82); }
    final public void   s_def_resgid(short v)      { writeShort(82, v); }

    // Erste Inode, die nicht reserviert ist
    final public int    s_first_ino()              { return readInt(84); }
    final public void   s_first_ino(int v)         { writeInt(84, v); }

    // Groesse der Inodestruktur in Byte
    final public short  s_inode_size()             { if (s_minor_rev_level() > 0 ) return (short)readShort(88); else return 128; } // TODO: change all call sites of s_inode_size
    final public void   s_inode_size(short v)      { writeShort(88, v); }

    // Nummer der Blockgruppe, die diese Kopie des Superblocks enthaelt
    final public short  s_block_group_nr()         { return (short)readShort(90); }
    final public void   s_block_group_nr(short v)  { writeShort(90, v); }

    // compatible feature set
    final public int    s_feature_compat()         { return readInt(92); }
    final public void   s_feature_compat(int v)    { writeInt(92, v); }

    // incompatible feature set
    final public int    s_feature_incompat()       { return readInt(96); }
    final public void   s_feature_incompat(int v)  { writeInt(96, v); }

    // readonly-compatible feature set
    final public int    s_feature_ro_compat()       { return readInt(100); }
    final public void   s_feature_ro_compat(int v)  { writeInt(100, v); }

    // 128-bit uuid for volume
    final public byte   s_uuid(int i)              {
	if (i >= 0 && i < 16) return (byte)readByte(104 + i);
	else return 0;
    }
    final public void   s_uuid(int i, byte v)      {
	if (i >= 0 && i < 16) writeByte(104 + i, v);
    }

    // Name der Partition
    final public String s_volume_name()            { return readString(120, 16); }
    final public void   s_volume_name(String v)    { writeString(120, v, 16); }

    // Verzeichnis, in dem das Dateisystem zuletzt gemounted wurde
    final public String s_last_mounted()           { return readString(136, 64); }
    final public void   s_last_mounted(String v)   { writeString(136, v, 64); }

    // Kompression
    final public int    s_alg_usage_bitmap()       { return readInt(200); }
    final public void   s_alg_usage_bitmap(int v)  { writeInt(200, v); }

    // Nr of blocks to try to preallocate
    final public byte   s_prealloc_blocks()        { return (byte)readByte(204); }
    final public void   s_prealloc_blocks(byte v)  { writeByte(204, v); }

    // Nr to preallocate for dirs
    final public byte   s_prealloc_dir_b()         { return (byte)readByte(205); }
    final public void   s_prealloc_dir_b(byte v)   { writeByte(205, v); }

    // reserved: 818


    public void dump() {
	Debug.out.println("-- Superblock data --");
	Debug.out.println("  Inodes             : "+s_inodes_count());
	Debug.out.println("  Blocks             : "+s_blocks_count());
	Debug.out.println("  Reserved Blocks    : "+s_r_blocks_count());
	Debug.out.println("  Free Blocks        : "+s_free_blocks_count());
	Debug.out.println("  Free Inodes        : "+s_free_inodes_count());
	Debug.out.println("  First Data Block   : "+s_first_data_block());
	Debug.out.println("  Log block size     : "+s_log_block_size());
	Debug.out.println("  Blocks per group   : "+s_blocks_per_group());
	Debug.out.println("  Inodes per group   : "+s_inodes_per_group());
	Debug.out.println("  Mmount count       : "+s_mnt_count());
	Debug.out.println("  Magic              : "+s_magic());
	Debug.out.println("  State              : "+s_state());
	Debug.out.println("  Errors             : "+s_errors());
	Debug.out.println("  Minor Revision     : "+s_minor_rev_level());
	if (s_minor_rev_level() > 0 ) {
	    Debug.out.println("  Inode size       : "+s_inode_size());
	}
	Debug.out.println("  Last mounted       : "+s_last_mounted());
	Debug.out.println("  Volume name        : "+s_volume_name());
	if (s_magic() != (short)0xEF53) {
	    throw new Error("wrong ext2 magic");
	}
    }
}
