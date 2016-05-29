package jx.rpcsvc.mount1;

import jx.rpc.*;

public class FHStatusOK extends FHStatus  {
  public jx.rpcsvc.nfs2.FHandle directory;


    public FHStatusOK() {}
    public FHStatusOK(jx.rpcsvc.nfs2.FHandle d) {
	super(FHStatus.SWITCH_FHStatusOK);
	directory = d;
    }
}
