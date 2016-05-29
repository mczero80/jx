package jx.fs;

import jx.zero.ReadOnlyMemory;
import jx.zero.Memory;

public interface RegularFile extends jx.fs.FSObject, jx.zero.Portal {

    public void close() throws Exception;

    public void sync() throws Exception;

    /**
     * Reads up to b.length bytes of data from this file into an array of bytes.
     */
    
    public int read(int pos, Memory mem,int off, int len) throws Exception;
    
    /**
     * Writes len bytes from the specified byte array starting at offset off to this file.
     */
    
    public int write(int pos, Memory mem, int off, int len) throws Exception;
    
    /**
     * Appends len bytes from the specified byte array starting at offset off to this file.
     */
    
    public int append(Memory mem, int off, int len) throws Exception;
    
    /**
     * Sets the length of this file.
     */

    public int setLength(long newLength) throws Exception;

    public int length() throws Exception;
}
