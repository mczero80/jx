package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class DirOpResError extends DirOpRes {
    public DirOpResError() {super(DirOpRes.SWITCHDEFAULT_DirOpResError); }
    public DirOpResError(int err) {super(err); }
}
