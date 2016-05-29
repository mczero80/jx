package jx.buffer.separator;

import jx.zero.*;

/**
 * This interface should be implemented by a class in a domain that generates
 * data.
 * This interface is used by a class in a domain
 * that processes data to inform the producer of the data
 * that processing is again ready.
 */
public interface MemoryProducer {
    void restartProduction();
}
