package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class ReadLinkRes implements RPCData {
    
    public static final int SWITCH_ReadLinkResOK = 0;
    public static final int SWITCH_ReadLinkResErrNoEnt = 2;
    public static final int SWITCH_ReadLinkResErrIO = 5;
    public static final int SWITCH_ReadLinkResErrAcces = 13;
    public static final int SWITCH_ReadLinkResErrNotDir = 20;
    public static final int SWITCH_ReadLinkResErrStale = 70;

    public static final int SWITCHDEFAULT_ReadLinkResError = 1;
  
  public int status;

    public ReadLinkRes(int status) { this.status = status; }
    public ReadLinkRes() {}

}
