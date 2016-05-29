package java.io;

public abstract class OutputStream
{ 
    public void close() throws IOException
    {
    }
    
    public void flush() throws IOException
    {
    }
    
    public abstract void write(int b) throws IOException;
    
    public synchronized void write(byte[] b, int off, int len) throws IOException
    {
	for (int i = 0; i < len; i++)
	    write(b[off + i]);
    }
    
    public void write(byte[] b) throws IOException
    {
	write(b, 0, b.length);
    }
    
    public OutputStream()
    {
    }
}


