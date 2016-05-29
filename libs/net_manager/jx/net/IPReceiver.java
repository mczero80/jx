package jx.net;

import jx.zero.Memory;

public interface IPReceiver extends jx.zero.Portal {
    IPData receive(Memory buf, int timeoutMillis);
    IPData receive(Memory buf);
    void close();
}
