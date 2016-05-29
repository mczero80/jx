package java.io;

public class FileInputStream extends InputStream
{
  private FileDescriptor fd;
  
  protected synchronized void finalize() throws IOException {
    if (fd != null)
      close();
  }
  
  public synchronized void close() throws IOException {
    if (fd == null)
      throw new IOException();
    fd = null;
  }
  
  public FileDescriptor getFD() throws IOException	{
    if (fd == null)
      throw new IOException();
    return fd;
  }

  public int available() throws IOException	{
    if (fd == null)
      throw new IOException();
    //return nfs.available(fd);
    return 0;
  }
  
  public int read() throws IOException
    {
      return 0;
    }
  
  public int read(byte[] b, int off, int len) throws IOException
	{
	  return 0;
	}

	public int read(byte[] b) throws IOException
	{
	  return 0;
	}
  
  public long skip(long n) throws IOException
    {
      return 0;
    }

  public FileInputStream(String name) throws FileNotFoundException
    {
      this(new File(name));
      
    }
  
  public FileInputStream(File file) throws FileNotFoundException
    {
      //this.file = file;
      // ....
    }
  
  public FileInputStream(FileDescriptor fd)
    {
      this.fd = fd;
      // ....
	}
}

