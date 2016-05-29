package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class SAttr implements RPCData {
  public int mode;
  public int uid;
  public int gid;
  public int size;
  public Timeval      atime;
  public Timeval      mtime;


  public SAttr() {}

  // init with reasonable defaults
  public SAttr(int mode) {
    this.mode = mode;
    uid = 10412;
    gid = 10430;
    size = 0;
    atime = new Timeval(true);
    mtime = new Timeval(true);
  }

}
