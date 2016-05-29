package jx.rpc;

public class RPCCall extends RPCMessage implements RPCData {
  public int version;
  public int prog;
  public int progVersion;
  public int proc;
  public Auth a;
  public Auth c;

  public RPCCall() {}

  public RPCCall(int xid, int prog, int progVersion, int proc, Auth a, Auth c) {
    super(xid, RPCMessage.SWITCH_RPCCall);
    this.version = 2;
    this.prog = prog;
    this.progVersion = progVersion;
    this.proc = proc;
    this.a = a;
    this.c = c;
  }
}
