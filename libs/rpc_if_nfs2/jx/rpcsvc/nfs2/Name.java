package jx.rpcsvc.nfs2;

public class Name implements jx.rpc.RPCData {
  public static final int MAXLEN_data = 255;
  public String data; 

  public Name() {}
  public Name(String d) { data = d; }
}
