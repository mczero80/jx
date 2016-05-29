package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class DirOpRes implements RPCData {
    public static final int SWITCH_DirOpResOK = 0;
    public static final int SWITCH_DirOpResNoSuchFile = 2;
    public static final int SWITCH_DirOpResErrStale = 70;
    public static final int SWITCH_DirOpResErrIO = 5;
    public static final int SWITCH_DirOpResErrNotDir = 20;
    public static final int SWITCH_DirOpResErrAcces = 13;
    public static final int SWITCH_DirOpResErrExist = 17;

    public static final int SWITCHDEFAULT_DirOpResError = 99;
 

    public int status;
    
    public DirOpRes() {}
    public DirOpRes(int s ) {status = s;}
}
