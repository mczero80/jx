package jx.net;

import jx.zero.Memory;

public interface TCPSocket extends jx.zero.Portal {
    public Memory processTCP(IPData m);
    public Memory processTCPServer(IPData m); 
    public void processClientSocket();       // call periodically for retransmissions ... 
    public TCPSocket accept(Memory[] newbufs) throws java.io.IOException;
    public void close() throws java.io.IOException;
    public void open(IPAddress remoteIP, int remotePort) throws UnknownAddressException, java.io.IOException;
    //    public void send(byte data);
    public byte[] readFromInputBuffer();
    public void send(byte[] byteArr) throws java.io.IOException;
}
