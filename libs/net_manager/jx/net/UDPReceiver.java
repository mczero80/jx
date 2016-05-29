package jx.net;

import jx.zero.Memory;

public interface UDPReceiver extends jx.zero.Portal {
    UDPData receive(Memory buf, int timeoutMillis);
    UDPData receive(Memory buf);
    UDPData receive1(Memory buf, int timeoutMillis);
    UDPData receive1(Memory buf);
    void close();
}
