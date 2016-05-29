package jx.net;


import jx.zero.Memory;

public class UDPData {
    public IPAddress sourceAddress;
    public int sourcePort;
    public Memory mem;
    public int offset,size; // only used when memory splitting is avoided
}
