package jx.compiler.execenv;

import java.io.IOException;

public interface ExtendedDataOutputStream {
    public void writeByte(byte i) throws IOException;
    public void writeInt(int i) throws IOException;
    public void writeString(String s) throws IOException;
    public void writeShort(short i) throws IOException;
}
