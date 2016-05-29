package jx.zero.debug;

import jx.zero.Portal;

public interface DebugChannel extends Portal {
    public void write(int b);
    public int read();
    public  void writeBuf(byte[] b, int off, int len);
}


