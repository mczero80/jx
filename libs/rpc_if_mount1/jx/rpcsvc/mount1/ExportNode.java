package jx.rpcsvc.mount1;

public class ExportNode implements jx.rpc.RPCData {
  public jx.rpcsvc.nfs2.DirPath  ex_dir;
  public Groups   ex_groups;
  public Exports  next;
}
