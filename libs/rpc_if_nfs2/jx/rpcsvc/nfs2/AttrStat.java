package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class AttrStat implements RPCData {
    public static final int SWITCH_AttrStatOK = 0;
    public static final int SWITCH_AttrStatErrNoEnt = 2;
    public static final int SWITCH_AttrStatErrIO = 5;
    public static final int SWITCH_AttrStatErrAcces = 13;
    public static final int SWITCH_AttrStatErrNotDir = 20;
    public static final int SWITCH_AttrStatErrStale = 70;

    public static final int SWITCHDEFAULT_AttrStatError = 1;
  
  
  public int status;

    public AttrStat() {}
    public AttrStat(int s) { status = s;}

}
