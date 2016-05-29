package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class NFSCookie implements RPCData {
  public int data;

    public NFSCookie(int d) { data = d; }
    public NFSCookie() {  }
}
