package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class ReadRes implements RPCData {
    public static final int SWITCH_ReadResOK = 0;
    public static final int SWITCH_ReadResErrNoEnt = 2;
    public static final int SWITCH_ReadResErrIO = 5;
    public static final int SWITCH_ReadResErrAcces = 13;
    public static final int SWITCH_ReadResErrNotDir = 20;
    public static final int SWITCH_ReadResErrStale = 70;

    public static final int SWITCHDEFAULT_ReadResError = 1;

    public int status;
    
    public ReadRes() {}
    public ReadRes(int status) {this.status = status;}
}
