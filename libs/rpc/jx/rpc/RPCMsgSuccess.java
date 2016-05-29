package jx.rpc;

public class RPCMsgSuccess extends RPCMsgAccepted {

    public RPCMsgSuccess() {}

    public RPCMsgSuccess(int xid, Auth verf) {
	super(xid, verf, RPCMsgAccepted.SWITCH_RPCMsgSuccess);
    }

}
