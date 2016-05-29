package jx.rpcsvc.nfs2;
import jx.rpc.*;

public class Entries implements RPCOptional,RPCData {
  public Entry  node;

    public Entries() {}
    public Entries(Entry n) { node = n;}
}
