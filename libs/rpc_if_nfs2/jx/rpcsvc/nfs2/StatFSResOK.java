package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class StatFSResOK extends StatFSRes {
  public int tsize;
  public int bsize; 
  public int blocks; // total blocks
  public int bfree; // free blocks
    public int bavail; // available blocks
}
