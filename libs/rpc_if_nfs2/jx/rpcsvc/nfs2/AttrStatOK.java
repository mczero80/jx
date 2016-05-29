package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class AttrStatOK extends AttrStat {
    public FAttr attributes;
    public AttrStatOK(FAttr a) { attributes = a; }
    public AttrStatOK() {}
}
