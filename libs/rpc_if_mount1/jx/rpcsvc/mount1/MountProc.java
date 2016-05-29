package jx.rpcsvc.mount1;

import jx.rpc.*;

/**
 * RFC 1094
 */
public interface MountProc extends RPCProc {
  public static final int VERSION = 1;
  public static final int PROGRAM = 100005;

  public static final int MID_nullproc_0 = 0;
  public static final int MID_mnt_1 = 0;
  public static final int MID_dump_2 = 0;
  public static final int MID_umnt_3 = 0;
  public static final int MID_umntall_4 = 0;
  public static final int MID_export_5 = 0;

  void       nullproc();
  FHStatus   mnt(jx.rpcsvc.nfs2.DirPath d);
  MountList  dump();
  void       umnt(jx.rpcsvc.nfs2.DirPath d);
  void       umntall(); 
  Exports    export(); 
}
