package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class ReadResOK extends ReadRes {
  public FAttr attributes;
  public NFSData data;

    public ReadResOK() {
	super(ReadRes.SWITCH_ReadResOK);
    }
    public ReadResOK(FAttr attributes, NFSData data) {
	super(ReadRes.SWITCH_ReadResOK);
	this.attributes = attributes;
	this.data = data;
    }

}
