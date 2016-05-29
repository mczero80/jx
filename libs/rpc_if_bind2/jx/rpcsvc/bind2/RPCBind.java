package jx.rpcsvc.bind2;

import jx.rpc.*;

public interface RPCBind extends RPCProc {
  public static final int VERSION = 2;
  public static final int PROGRAM = 100000;
  public static final int MID_nullproc_0 = 0;
  public static final int MID_nullproc1_1 = 0;
  public static final int MID_nullproc2_2 = 0;
  public static final int MID_getaddr_3 = 0;
  void nullproc();
  void nullproc1();
  void nullproc2();
  int  getaddr(RPCB r);
  // ...
}
