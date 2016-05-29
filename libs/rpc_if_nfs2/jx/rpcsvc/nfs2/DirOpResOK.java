package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class DirOpResOK extends DirOpRes {
  public FHandle file;
  public FAttr attributes;


    public DirOpResOK() {super(DirOpRes.SWITCH_DirOpResOK); }
    public DirOpResOK(FHandle file, FAttr attributes) {
	super(DirOpRes.SWITCH_DirOpResOK);
	this.file = file;
	this.attributes = attributes;
    }
}
