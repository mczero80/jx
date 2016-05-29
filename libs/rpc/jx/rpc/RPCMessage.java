package jx.rpc;

public class RPCMessage implements RPCData {
  /* message type */
  public static final int SWITCH_RPCCall = 0;
  public static final int SWITCH_RPCReply = 1;

  public int xid;
  public int msgType;  


  public RPCMessage() {}
  public RPCMessage(int xid, int msgType) {
      this.xid = xid;
      this.msgType = msgType;
  }

  public int getID() { return xid; } 



}
