package jx.rpc;

 /* Format of the RPC header, RPC version 2: (RFC 1831)
  */

public class RPCHeader implements RPCData {
  public int xid;
  public RPCMessage msg;
}
