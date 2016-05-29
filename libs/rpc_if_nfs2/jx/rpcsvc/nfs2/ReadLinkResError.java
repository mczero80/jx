package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class ReadLinkResError extends ReadLinkRes {

    public ReadLinkResError() {super(ReadLinkRes.SWITCHDEFAULT_ReadLinkResError); }
    public ReadLinkResError(int err) {super(err); }
}
