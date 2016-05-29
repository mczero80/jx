package jx.rpcsvc.nfs2;

public class DirPath implements jx.rpc.RPCData {
  public static final int MAXLEN_data = 1024;
  public String data; 
  public DirPath() { }
  public DirPath(String d) { data = d; }
}
