package jx.zero.memory;

import java.io.InputStream;
import jx.zero.ReadOnlyMemory;

public class MemoryInputStream extends InputStream { 
    private ReadOnlyMemory buf;
    private int count;
    private int pos;
    
    public int available() {
	return count - pos;
    }

    public void reset() {
	pos = 0;
    }

    public int read() {
	if (pos >= count)
	    return -1;
	else
	    return ((int)buf.get8(pos++)) & 0xFF;
    }

    public int read(byte[] b, int off, int len)	{
	if (pos >= count)
	    return -1;
	int d = count - pos;
	int bytes = d < len ? d : len;
	buf.copyToByteArray(b, off, pos, bytes);
	pos += bytes;
	return bytes;
    }

    public long skip(long n) {
	int target = (int) (pos + n);
	if (target > count) {
	    int bytes = count - pos;
	    pos = count;
	    return bytes;
	}
	
	pos = (int) target;
	
	return n;
    }

    public MemoryInputStream(ReadOnlyMemory m) {
	buf = m;
	count = m.size();
    }
}

