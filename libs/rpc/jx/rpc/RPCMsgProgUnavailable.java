package jx.rpc;

public class RPCMsgProgUnavailable extends RPCMsgAccepted {

    public RPCMsgProgUnavailable() {
    }
    public RPCMsgProgUnavailable(int xid, Auth verf) {
	super(xid, verf, RPCMsgAccepted.SWITCH_RPCMsgProgUnavailable);
    }
}
