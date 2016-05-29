package jx.zero.memory;

import java.io.OutputStream;
import java.io.IOException;
import jx.zero.Memory;

public class MemoryOutputStream extends OutputStream { 
    private Memory buf;
    private int pos;
    private int size;

    public MemoryOutputStream(Memory buf) {
	this.buf = buf;
	size = buf.size();
    }

    public void write(int b) throws IOException {
	if (pos == size) throw new IOException("Memory overflow");
	buf.set8(pos++, (byte)b);
    }
}

