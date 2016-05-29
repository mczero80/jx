package jx.formats;

import java.io.*;

/**
 * Reads numbers in little endian format.
 * Reads strings by first reading the stringlength and
 * then the *bytes* of the string (no unicode support up to now)
 */
public class LittleEndianInputStream extends FilterInputStream {

    private char[] str_buffer;

    public LittleEndianInputStream(InputStream in) {
	super(in);
    }

    public final short readShort() throws IOException {
	short v;
	int b;
	if ((b = read()) == -1) throw new EOFException();
	v  =  (short)(b & 0xFF);
	if ((b = read()) == -1) throw new EOFException();
	v |= (b & 0xFF) << 8;
	return v;
    }
    
    public final int readInt() throws IOException {
	int v;
	int b;
	if ((b = read()) == -1) throw new EOFException();
	v  =  b & 0xFF;
	if ((b = read()) == -1) throw new EOFException();
	v |= (b & 0xFF) << 8;
	if ((b = read()) == -1) throw new EOFException();
	v |= (b & 0xFF) << 16;
	if ((b = read()) == -1) throw new EOFException();
	v |= (b & 0xFF) << 24;
	return v;
    }

    public final long readLong() throws IOException {
	long v;
	long b;
	if ((b = read()) == -1) throw new EOFException();
	v  =  b & 0xFF;
	if ((b = read()) == -1) throw new EOFException();
	v |= (b & 0xFF) << 8;
	if ((b = read()) == -1) throw new EOFException();
	v |= (b & 0xFF) << 16;
	if ((b = read()) == -1) throw new EOFException();
	v |= (b & 0xFF) << 24;
	if ((b = read()) == -1) throw new EOFException();
	v |= (b & 0xFF) << 32;
	if ((b = read()) == -1) throw new EOFException();
	v |= (b & 0xFF) << 40;
	if ((b = read()) == -1) throw new EOFException();
	v |= (b & 0xFF) << 48;
	if ((b = read()) == -1) throw new EOFException();
	v |= (b & 0xFF) << 56;
	return v;
    }
    
    public final byte readByte() throws IOException {
	int b;
	if ((b = read()) == -1) throw new EOFException();
	return (byte)(b & 0xFF);
    }
    
    public final String readString() throws IOException {
	int len = readInt();

	if (str_buffer==null || str_buffer.length<len) {
	    str_buffer = new char[len+10];
	}

	for (int i = 0; i < len; i++) {
	    str_buffer[i] = (char) (read() & 0xff); 
	}

	return new String(str_buffer,0,len);
    }
    public final String readString2ByteAligned() throws IOException {
	String s = readString();
	if ((s.length() & 1) != 0) {
	    if (read() == -1) throw new EOFException();
	}
	return s;
    }
}
