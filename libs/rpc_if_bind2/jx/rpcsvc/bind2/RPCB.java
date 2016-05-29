package jx.rpcsvc.bind2;


public class RPCB implements jx.rpc.RPCData {
  public int prog;    /* program number */
  public int vers;    /* version number */
  public int protocol;
  public int args;
  
  public static final int IPPROTO_TCP = 6;   
  public static final int IPPROTO_UDP = 17;
  
  public RPCB() {}
  public RPCB(int prog, int vers, int protocol, int args) {
    this.prog = prog;    
    this.vers = vers;    
    this.protocol = protocol;
    this.args = args;
  }
  public RPCB(int prog, int vers) {
    this(prog, vers, IPPROTO_UDP, 0);
  }
};

