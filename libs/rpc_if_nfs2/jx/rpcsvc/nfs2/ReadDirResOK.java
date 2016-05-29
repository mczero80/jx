package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class ReadDirResOK extends ReadDirRes {
  public Entries entries;
  public int eof;

    public ReadDirResOK() {
	super(ReadDirRes.SWITCH_ReadDirResOK);
    }
    public ReadDirResOK(Entries entries, int eof) {
	super(ReadDirRes.SWITCH_ReadDirResOK);
	this.entries = entries;
	this.eof = eof;
    }

}
