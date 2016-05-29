package java.io;
import jx.zero.Debug;

public class DataInputStream extends FilterInputStream implements DataInput { 

    public DataInputStream(InputStream in) {
	super((in instanceof PushbackInputStream)
	      ? (PushbackInputStream) in
	      : new PushbackInputStream(in)
		  );
    }

    private final static int UTFBits(byte c) throws UTFDataFormatException
    {
	if ((c & 0xC0) != 0x80)
	    throw new UTFDataFormatException();
	return c & 0x3F;
    }

    public final static String readUTF(DataInput in) throws IOException
    {
	int len = in.readUnsignedShort();
	byte[] src = new byte[len];
	in.readFully(src);

	StringBuffer buff = new StringBuffer(len);
	int n;

	for (int i = 0; i < len; )
	    {
		int c = (int) src[i++] & 0xFF;

		switch (c >> 4)
		    {
		    case 0x0:
		    case 0x1:
		    case 0x2:
		    case 0x3:
		    case 0x4:
		    case 0x5:
		    case 0x6:
		    case 0x7:
			n = c;
			break;
		    case 0x8:
		    case 0x9:
		    case 0xA:
		    case 0xB:
			throw new UTFDataFormatException();
		    case 0xC:
		    case 0xD:
			if (i >= len)
			    throw new UTFDataFormatException();
			int d = UTFBits(src[i++]);
			n = ((c & 0x1F) << 6) + d;
			break;
		    case 0xE:
			if (len < 2)
			    throw new UTFDataFormatException();
			int e = UTFBits(src[i++]);
			int f = UTFBits(src[i++]);
			n = ((c & 0x0F) << 12) + (e << 6) + f;
			break;
		    case 0xF:
			throw new UTFDataFormatException();
		    default:
			throw new UTFDataFormatException();
		    }

		buff.append((char) n);
	    }

	return buff.toString();
    }

    private static int last;

    private final void unread0() throws IOException
    {
	((PushbackInputStream) in).unread(last);
    }

    private final int read0() throws IOException
    {
	int c = super.read();
	if (c < 0)
	    throw new EOFException();
	last = c;
	return c;
    }

    public final int read(byte[] b) throws IOException
    {
	return super.read(b, 0, b.length);
    }

    public final int read(byte[] b, int off, int len) throws IOException
    {
	return super.read(b, off, len);
    }

    public final void readFully(byte[] b) throws IOException
    {
	readFully(b, 0, b.length);
    }

    public final synchronized void readFully(byte[] b, int off, int len) throws IOException
    {
	while (len > 0)
	    {
		int n = read(b, off, len);
		if (n < 0)
		    throw new EOFException();
		off += n;
		len -= n;
	    }
    }

    public final boolean readBoolean() throws IOException
    {
	return read0() != 0;
    }

    public final byte readByte() throws IOException
    {
	return (byte) read0();
    }

    public final int readUnsignedByte() throws IOException
    {
	return read0();
    }

    public final synchronized char readChar() throws IOException
    {
	int n = read0();
	n <<= 8;	n += read0();
	return (char) n;
    }

    public final synchronized short readShort() throws IOException
    {
	int n = read0();
	n <<= 8;	n += read0();
	return (short) n;
    }

    public final synchronized int readUnsignedShort() throws IOException
    {
	int n = read0();
	n <<= 8;	n += read0();
	return n;
    }

    public final synchronized int readInt() throws IOException
    {
	int n = read0();
	n <<= 8;	n += read0();
	n <<= 8;	n += read0();
	n <<= 8;	n += read0();
	return n;
    }

    public final synchronized long readLong() throws IOException
    {
	long n = read0();
	n <<= 8;	n += read0();
	n <<= 8;	n += read0();
	n <<= 8;	n += read0();
	n <<= 8;	n += read0();
	n <<= 8;	n += read0();
	n <<= 8;	n += read0();
	n <<= 8;	n += read0();
	return n;
    }

    public final float readFloat() throws IOException
    {
	int n = readInt();
	return Float.intBitsToFloat(n);
    }

    public final double readDouble() throws IOException
    {
	throw new IOException("not implemented");
	/*
	long n = readLong();
	return Double.longBitsToDouble(n);
	*/
    }

    public final synchronized String readLine() throws IOException {
	StringBuffer buff = new StringBuffer();
	
	while (true) {
	    int c = read0();
	    
	    if (c == '\n')
		break;
	    
	    if (c == '\r') {
		c = read0();
		if (c != '\n')
		    unread0();		    
		break;
	    }
	    buff.append((char) c);
	    
	}
	
	return buff.toString();
    }

    public final String readUTF() throws IOException
    {
	return readUTF(this);
    }

    public final int skipBytes(int n) throws IOException
    {
	for (int i=0;i<n;i++) {
	    read0();
	}
	return n;

	/*
	try
	    {
		return (int) super.skip((long) n);
	    }
	catch (EOFException e)
	    {
		return 0;
	    }
	*/
	// not reached
    }

}

