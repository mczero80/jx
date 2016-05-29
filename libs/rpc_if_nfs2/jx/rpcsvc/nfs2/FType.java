package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class FType implements RPCData {
  public static final int ftype_NFNON = 0;
  public static final int ftype_NFREG = 1;
  public static final int ftype_NFDIR = 2;
  public static final int ftype_NFBLK = 3;
  public static final int ftype_NFCHR = 4;
  public static final int ftype_NFLNK = 5;

  public int ftype;
}
