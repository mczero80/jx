package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class StatFSRes implements RPCData {
    public static final int SWITCH_StatFSResOK = 0;
    public static final int SWITCH_StatFSResErrNoEnt = 2;
    public static final int SWITCH_StatFSResErrIO = 5;
    public static final int SWITCH_StatFSResErrAcces = 13;
    public static final int SWITCH_StatFSResErrNotDir = 20;
    public static final int SWITCH_StatFSResErrStale = 70;

    public static final int SWITCHDEFAULT_StatFSResError = 1;
  
    public int status;

    public StatFSRes() {}
    public StatFSRes(int status) {this.status = status;}

}
