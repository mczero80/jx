package jx.net;

import jx.zero.*;

public interface IPProducer {
    public boolean registerConsumer(IPConsumer consumer, String name);
}
