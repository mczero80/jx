package jx.xdr;

public interface XDRBuffer {
    void setByte(int index, byte value);
    byte getByte(int index);
    void getBytes(byte[] buf, int index, int len);
    void advance(int offset);
}
