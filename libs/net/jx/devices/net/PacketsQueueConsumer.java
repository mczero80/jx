package jx.devices.net;

import jx.zero.*;

public interface PacketsQueueConsumer extends Portal {
    /**
     * get next packet
     * blocks until packet is available
     */
    public Memory get(Memory o);
    public boolean isEmpty();
    public int size();
}
