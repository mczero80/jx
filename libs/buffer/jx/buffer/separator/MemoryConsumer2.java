package jx.buffer.separator;

import jx.zero.*;

/**
 * This interface should be implemented by a class in a domain
 * that processes data. For each Memory object that is passed
 * an equivalent Memory object must be returned. If the consumer
 * fails to return such a Memory object, the producer stops
 * producing data.
 *
 * This interface should be used by a domain that generates
 * data. Such a domain must also provide an object that
 * implements the MemoryProducer interface.
 */
public interface MemoryConsumer2 {
    Memory processMemory(Memory data, int offset, int size);
}
