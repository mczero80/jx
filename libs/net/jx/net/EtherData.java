package jx.net;


import jx.zero.Memory;

public class EtherData {
    public byte[] srcAddress;
    public byte[] dstAddress;
    public Memory mem;
    public int offset,size; // only used when memory splitting is avoided
}
