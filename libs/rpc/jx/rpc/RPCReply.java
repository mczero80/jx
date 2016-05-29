package jx.rpc;

public class RPCReply extends RPCMessage  {
  public static final int SWITCH_RPCMsgAccepted = 0;
  public static final int SWITCH_RPCMsgDenied   = 1;
  public int status;

    public RPCReply() {
    }
    public RPCReply(int xid, int status) {
	super(xid, RPCMessage.SWITCH_RPCReply);
	this.status = status;
    }
}
