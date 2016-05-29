package jx.net;

import jx.zero.Memory;

public interface UDPSender extends jx.zero.Portal {
    Memory send(Memory m);
    Memory send1(Memory m, int offset, int size);
    void close();
    int getLocalPort();
    int getRemotePort();
    IPAddress getDestination();
}
