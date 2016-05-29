package java.io;

public abstract class InputStream
{ 
    public int available() throws IOException
    {
	return 0;
    }
    
    public void close() throws IOException
    {
    }
    
    public boolean markSupported()
    {
		return false;
    }
    
    public void mark(int readlimit)
    {
    }
    
    public abstract int read() throws IOException;
    
    public synchronized int read(byte[] b, int off, int len) throws IOException
    {
	for (int i = 0; i < len; i++)
	    {
		int c = read();
		if (c < 0)
		    return i == 0 ? -1 : i;
		b[off + i] = (byte) c;
	    }
	
	return len;
    }
    
    public int read(byte[] b) throws IOException
    {
	return read(b, 0, b.length);
    }
    
    public void reset() throws IOException
    {
	throw new IOException();
    }
    
    public synchronized long skip(long n) throws IOException
    {
	for (int i = 0; i < n; i++)
	    {
		int c = read();
		if (c < 0)
		    return i;
	    }
	
	return n;
    }
    
    public InputStream()
    {
    }
}

