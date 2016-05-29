package jx.net;

import jx.zero.*;
import jx.buffer.separator.MemoryConsumer;

// used for address mappings 
public interface AddressResolution {
    public boolean register(Object o);
    public void notifyAddressChange(Object o);

    /** map fromAddress to target address */
    public byte[] lookup(byte[] fromAddress) throws UnknownAddressException;
}
