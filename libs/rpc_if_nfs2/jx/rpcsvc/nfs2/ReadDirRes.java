package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class ReadDirRes implements RPCData {
    public static final int SWITCH_ReadDirResOK = 0;
    public static final int SWITCH_ReadDirResErrNoEnt = 2;
    public static final int SWITCH_ReadDirResErrIO = 5;
    public static final int SWITCH_ReadDirResErrAcces = 13;
    public static final int SWITCH_ReadDirResErrNotDir = 20;
    public static final int SWITCH_ReadDirResErrStale = 70;

    public static final int SWITCHDEFAULT_ReadDirResError = 1;

  
    public int status;
    
    public ReadDirRes(int status) { this.status = status; }
    public ReadDirRes() {}
}
