package jx.buffer.multithread;

import jx.zero.Debug;

/**
 * The operations that are available to the consumer of buffers.
 */
public interface BufferConsumer {
    public Buffer undockFirstElement();
}
