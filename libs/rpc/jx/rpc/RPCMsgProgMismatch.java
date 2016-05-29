package jx.rpc;

public class RPCMsgProgMismatch extends RPCMsgAccepted {
  public int low;
  public int high;


    public RPCMsgProgMismatch() {}
    public RPCMsgProgMismatch(int xid, Auth verf, int low, int high) {
	super(xid, verf, RPCMsgAccepted.SWITCH_RPCMsgProgMismatch);
	this.low = low;
	this.high = high;
    }
}
