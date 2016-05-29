package jx.rpc;

public class RPCMsgDenied extends RPCReply {
  public static final int SWITCH_RPCMsgRejectMismatch = 0;/* RPC version number != 2          */
  public static final int SWITCH_RPCMsgRejectAuth   = 1; /* remote can't authenticate caller */

  public int status;
}
