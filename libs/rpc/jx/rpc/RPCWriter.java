package jx.rpc;

public interface RPCWriter {
  public int write(byte[] buf, int offset, Object data);
}
