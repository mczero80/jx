package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class ReadLinkResOK extends ReadLinkRes {
  public DirPath path;

    public ReadLinkResOK() {
	super(ReadLinkRes.SWITCH_ReadLinkResOK);
    }

    public ReadLinkResOK(DirPath p) {
	super(ReadLinkRes.SWITCH_ReadLinkResOK);
	this.path = p;
    }

}
