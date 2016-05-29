package jx.rpc;

public class RPCMsgAccepted extends RPCReply {
  public static final int SWITCH_RPCMsgSuccess = 0;
  public static final int SWITCH_RPCMsgProgUnavailable   = 1;
  public static final int SWITCH_RPCMsgProgMismatch   = 2;

  public Auth verf;
  public int stat;

    public RPCMsgAccepted() {
    }

    public RPCMsgAccepted(int xid, Auth verf, int stat) {
	super(xid, RPCReply.SWITCH_RPCMsgAccepted);
	this.verf = verf;
	this.stat = stat;
    }

}
