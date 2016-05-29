package jx.fs;

import jx.zero.ReadOnlyMemory;
import jx.zero.Memory;

public interface ReadOnlyRegularFile extends jx.fs.FSObject, jx.zero.Portal {

    public void close() throws Exception;
    public void sync() throws Exception;

    /**
     * Reads up to b.length bytes of data from this file into an array of bytes.
     */
    
    public int read(int pos, Memory mem,int off, int len) throws Exception;    
    public int length() throws Exception;
}
