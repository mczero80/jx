package jx.rpcsvc.nfs2;

import jx.fs.FSException;

public class NFSException extends FSException {
  public NFSException(String msg) { super(msg); }
  public NFSException() { super(); }
}
