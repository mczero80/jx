package java.io;

public class PrintStream extends FilterOutputStream {
  public PrintStream(OutputStream o) { super(o); }

    private boolean error = false;
    private boolean autoflush = true;

    public boolean checkError()
    {
	return error;
    }

    public void close()
    {
	try
	    {
		out.close();
	    }
	catch (IOException e)
	    {
		error = true;
	    }
    }

    public void flush()
    {
	try
	    {
		out.flush();
	    }
	catch (IOException e)
	    {
		error = true;
	    }
    }

    public  void write(int b)
    {
	try
	    {
		out.write(b);
	    }
	catch (IOException e)
	    {
		error = true;
	    }

	if (autoflush && b == '\n')
	    flush();
    }

    public  void write(byte[] b, int off, int len)
    {
	try
	    {
		out.write(b, off, len);
	    }
	catch (IOException e)
	    {
		error = true;
	    }

	if (!autoflush)
	    return;

	for (int i = 0; i < b.length; i++)
	    {
		if (b[i] != '\n')
		    continue;

		flush();
		break;
	    }
    }

    public void print(char[] s)
    {
	for (int i = 0; i < s.length; i++)
	    write((int) s[i]);
    }

    public void print(String s)
    {
	int len = s.length();
	byte[] buff = new byte[len];
	s.getBytes(0, len, buff, 0);
	write(buff, 0, len);
    }

    public void print(boolean v)
    {
	print(v ? "true" : "false");
    }

    public void print(char v)
    {
	write((int) v);
    }

  /*
    public void print(float v)
    {
	print(Float.toString(v));
    }

    public void print(double v)
    {
	print(Double.toString(v));
    }
    */

    public void print(int v)
    {
	print(Integer.toString(v));
    }

    public void print(long v)
    {
	print(Long.toString(v));
    }

    public  void print(Object v)
    {
	print(v == null ? "null" : v.toString());
    }

    public void println()
    {
	write('\n');
    }

    public  void println(char[] s)
    {
	print(s);
	println();
    }

    public  void println(String s)
    {
	print(s);
	println();
    }

    public  void println(boolean v)
    {
	print(v);
	println();
    }

    public  void println(char v)
    {
	print(v);
	println();
    }

    public  void println(int v)
    {
	print(v);
	println();
    }

    public  void println(long v)
    {
	print(v);
	println();
    }

  /*
    public  void println(float v)
    {
	print(v);
	println();
    }

    public  void println(double v)
    {
	print(v);
	println();
    }
    */
    public  void println(Object v)
    {
	print(v);
	println();
    }

}
