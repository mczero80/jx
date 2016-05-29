package jx.rpcsvc.nfs2;

public class NFSMountError extends NFSException {
  int status;
  public NFSMountError(int status) { super("Status: " + status); }
}
