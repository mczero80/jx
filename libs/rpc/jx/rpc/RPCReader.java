package jx.rpc;

public interface RPCReader {
  public Object read(byte[] buf, int offset);
}
