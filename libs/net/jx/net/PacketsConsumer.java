package jx.net;

import jx.zero.*;
import jx.buffer.separator.MemoryConsumer;


public interface PacketsConsumer  extends MemoryConsumer  {

    // public Memory sendPacket(byte[] addr, Memory buf, int offset, int count);
    /**
     * Get number of bytes need for header information
     */
    public int requiresSpace();
    /**
     * Get Maximum Transfer Unit of the underlying network 
     */
    public int getMTU();
}
