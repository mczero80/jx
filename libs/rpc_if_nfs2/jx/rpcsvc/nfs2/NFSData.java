package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class NFSData implements RPCData {
  public byte[] data;

    public NFSData() {}
    public NFSData(byte[] data) {this.data = data;}
}
