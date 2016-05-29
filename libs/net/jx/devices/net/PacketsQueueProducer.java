package jx.devices.net;

import jx.zero.*;

public interface PacketsQueueProducer extends Portal {
    public Memory add(Memory o, int length); 
    public boolean isEmpty();
    public int size();
}
