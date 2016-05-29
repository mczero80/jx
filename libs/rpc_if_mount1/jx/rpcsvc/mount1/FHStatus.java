package jx.rpcsvc.mount1;

import jx.rpc.*;

public class FHStatus implements RPCData, RPCUnion  {
  public static final int SWITCH_FHStatusOK = 0;

  public int status;

    public FHStatus() {}
    public FHStatus(int status) { this.status = status;}
    
}
