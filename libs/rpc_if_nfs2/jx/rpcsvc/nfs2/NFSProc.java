package jx.rpcsvc.nfs2;

import jx.rpc.*;

/**
 * RFC 1094
 */
public interface NFSProc extends RPCProc {
  public static final int VERSION = 2;
  public static final int PROGRAM = 100003;

  public static final int MID_nullproc_0 = 0;
  public static final int MID_getattr_1 = 0;
  public static final int MID_setattr_2 = 0;
  public static final int MID_root_3 = 0;
  public static final int MID_lookup_4 = 0;
  public static final int MID_readlink_5 = 0;
  public static final int MID_read_6 = 0;
  public static final int MID_writeCache_7 = 0;
  public static final int MID_write_8 = 0;
  public static final int MID_create_9 = 0;
  public static final int MID_remove_10 = 0;
  public static final int MID_rename_11 = 0;
  public static final int MID_link_12 = 0;
  public static final int MID_symlink_13 = 0;
  public static final int MID_mkdir_14 = 0;
  public static final int MID_rmdir_15 = 0;
  public static final int MID_readdir_16 = 0;
  public static final int MID_statfs_17 = 0;

  void        nullproc();
  AttrStat    getattr(FHandle a);
  AttrStat    setattr(FHandle file, SAttr attributes);
  void        root();
  DirOpRes    lookup(FHandle  dir, Name name);
  ReadLinkRes readlink(FHandle a);
  ReadRes     read(FHandle file, int offset, int count, int totalcount);
  void        writeCache();
  AttrStat    write(FHandle file, int beginoffset, int offset, int totalcount, NFSData data);
  DirOpRes    create(FHandle  dir, Name name, SAttr attributes);
  Stat        remove(FHandle  dir, Name name);
  Stat        rename(FHandle fromDir, Name fromName, FHandle  toDir, Name toName);
  Stat        link(FHandle from, FHandle  toDir, Name toName);
  Stat        symlink(FHandle fromDir, Name fromName, DirPath to, SAttr attributes);
  DirOpRes    mkdir(FHandle  dir, Name name, SAttr attributes);
  Stat        rmdir(FHandle  dir, Name name);
  ReadDirRes  readdir(FHandle dir, NFSCookie cookie, int count);
  StatFSRes   statfs(FHandle dir);
}
