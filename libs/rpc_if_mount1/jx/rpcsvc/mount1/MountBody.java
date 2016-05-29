package jx.rpcsvc.mount1;

public class MountBody implements jx.rpc.RPCData  {
  jx.rpcsvc.nfs2.Name       ml_hostname;
  jx.rpcsvc.nfs2.DirPath    ml_directory;
  MountList  ml_next;
}
