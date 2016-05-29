package jx.compiler.execenv;

import java.io.IOException;

public interface ExtendedDataInputStream {
    public int readInt() throws IOException;
    public byte readByte() throws IOException;
    public String readString() throws IOException;
    public short readShort() throws IOException;
}
