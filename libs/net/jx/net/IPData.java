package jx.net;


import jx.zero.Memory;

public class IPData {
    public IPAddress sourceAddress;
    public IPAddress destinationAddress;
    public Memory mem;
    public int offset,size; // only used when memory splitting is avoided
}
