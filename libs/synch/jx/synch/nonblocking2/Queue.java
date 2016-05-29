package jx.synch.nonblocking2;

import jx.zero.*;

public interface Queue {
    void enqueue(Object value);
    Object dequeue() throws QueueEmptyException;
}

