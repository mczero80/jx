package java.io;

public class FileOutputStream extends OutputStream
{ 
	private FileDescriptor fd;

	protected synchronized void finalize() throws IOException
	{
		if (fd != null)
			close();
	}

	public synchronized void close() throws IOException
	{
	  // ...
	}

	public FileDescriptor getFD() throws IOException
	{
		if (fd == null)
			throw new IOException();
		return fd;
	}

	public void write(byte[] b, int off, int len) throws IOException
	{
		if (fd == null)
			throw new IOException();
		// ...
	}

	public void write(byte[] b) throws IOException
	{
		write(b, 0, b.length);
	}

	public void write(int b) throws IOException
	{
		byte[] buf = new byte[1];
		buf[0] = (byte) (b & 0xFF);
		write(buf);
	}

	public FileOutputStream(String name) throws IOException
	{
		this.fd = new FileDescriptor();
		// open the file !!
		// ....
		if (!this.fd.valid())
			throw new IOException();
	}

  public FileOutputStream(File file) throws IOException
    {
      this(file.getPath());
    }

	public FileOutputStream(FileDescriptor fd)
	{
		this.fd = fd;
	}
}

