package jx.zero.debug;

import jx.zero.debug.DebugOutputStream;
import jx.zero.debug.DebugChannel;
import java.io.IOException;
import java.io.OutputStream;

//import java.lang.JXInteger;

// cannot extend FilterOutputStream, because we dont want synchronized write methods!
// use ONE StringBuffer with ONE char array, instead of allocating
// a new one each time 
public class DebugPrintStream 
{ 
    // DebugOutputStream out;  
    OutputStream out;  

    private boolean error = false;
    private boolean autoflush = true;

    private JXStringBuffer buffer; 
    // public DebugPrintStream(DebugOutputStream out)
    public DebugPrintStream(OutputStream out)
    {
	this.out = out;
	buffer = new JXStringBuffer(256);
    }

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
	try  {
	    out.write(b, off, len);
	} catch (IOException e)   {
	    error = true;
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
	intToString(buffer, v, 10);
	printBuffer();
    }

    public void print(long v)
    {
	print(Long.toString(v));
    }

    public  void print(Object v)
    {
	print(v == null ? "null" : v.toString());
    }

    private void printBuffer() {
	for(int i=0; i<buffer.length(); i++) {
	    write(buffer.charAt(i));
	}
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
	int len = s.length();
	byte[] buff = new byte[len+1];
	s.getBytes(0, len, buff, 0);
	buff[len] = (byte)'\n';
	write(buff, 0, len+1);
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


    public static void intToString(JXStringBuffer buffer, int value, int radix) {
	buffer.clear();

	if (value == 0) {
	    buffer.append('0');
	    return;
	}
	
	boolean isNegative = false;
        if( value < 0 ){
	    isNegative = true;
	    value = -value;
	}
	
	while (value > 0){
	    buffer.append(Character.forDigit(value % radix, radix));
	    value /= radix;
	}
        
        if (isNegative)
	    buffer.append('-');
        
	buffer.reverse();

    }




public class JXStringBuffer
{
	private char[] data;
	private int length;	// actual used bytes in the string

	public JXStringBuffer(int length)
	{
		data = new char[length];
		this.length = 0;
	}

	public JXStringBuffer(String str)
	{
		this(str.length() + 16);
		append(str);
	}

	public JXStringBuffer()
	{
		this(16);
	}

	public int length()
	{
		return length;
	}

	public int capacity()
	{
		return data.length;
	}

	public synchronized void ensureCapacity(int minimumCapacity)
	{
		if (data.length >= minimumCapacity)
			return;
		int n = Math.max(minimumCapacity, data.length * 2 + 2);
		char[] buff = new char[n];
		System.arraycopy(data, 0, buff, 0, data.length);
		data = buff;
	}

	public synchronized String toString()
	{
		return new String(data, 0, length);
	}

	public synchronized char charAt(int index)
	{
		if (index < 0 || index >= length)
			throw new StringIndexOutOfBoundsException();
		return data[index];
	}

	public synchronized void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)
	{
		if (srcBegin < 0 || srcEnd >= length || srcEnd < srcBegin)
			throw new StringIndexOutOfBoundsException();
		System.arraycopy(data, srcBegin, dst, dstBegin, srcEnd - srcBegin);
	}

	public synchronized void setCharAt(int index, char c)
	{
		if (index < 0 || index >= length)
			throw new StringIndexOutOfBoundsException();
		data[index] = c;
	}

	public synchronized void setLength(int newLength)
	{
		ensureCapacity(newLength);
		for (int i = length; i < newLength; i++)
			data[i] = '\u0000';
		length = newLength;
	}

	public synchronized JXStringBuffer reverse()
	{
		for (int i = 0; i < length / 2; i++)
		{
			int j = length - 1 - i;
			char tmp = data[i];
			data[i] = data[j];
			data[j] = tmp;
		}

		return this;
	}

	public synchronized JXStringBuffer append(char c)
	{
		ensureCapacity(length + 1);
		data[length] = c;
		length++;
		return this;
	}

	public synchronized JXStringBuffer append(char[] str, int offset, int len)
	{
		ensureCapacity(length + len);
		System.arraycopy(str, offset, data, length, len);
		length += len;
		return this;
	}

	public JXStringBuffer append(char[] str)
	{
		return append(str, 0, str.length);
	}

	public synchronized JXStringBuffer append(String str)
	{
		if (str == null)
			return append("null");

		int n = str.length();
		ensureCapacity(length + n);
		str.getChars(0, n, data, length);
		length += n;

		return this;
	}

	public JXStringBuffer append(boolean b)
	{
		return append(String.valueOf(b));
	}

	public JXStringBuffer append(int i)
	{
		return append(String.valueOf(i));
	}

	public JXStringBuffer append(long l)
	{
		return append(String.valueOf(l));
	}

  /*
	public JXStringBuffer append(float f)
	{
		return append(String.valueOf(f));
	}

	public JXStringBuffer append(double d)
	{
		return append(String.valueOf(d));
	}
	*/
	public JXStringBuffer append(Object o)
	{
		return append(String.valueOf(o));
	}

	private void insertGap(int offset, int n)
	{
		ensureCapacity(length + n);
		if (offset < length)
			System.arraycopy(data, offset, data, offset + n, length - offset);
	}

	public synchronized JXStringBuffer insert(int offset, char c)
	{
		if (offset < 0 || offset > length)
			throw new StringIndexOutOfBoundsException();
		insertGap(offset, 1);
		data[offset] = c;
		length++;
		return this;
	}

	public synchronized JXStringBuffer insert(int offset, char[] str)
	{
		if (offset < 0 || offset > length)
			throw new StringIndexOutOfBoundsException();
		int n = str.length;
		insertGap(offset, n);
		System.arraycopy(str, 0, data, offset, n);
		length += n;
		return this;
	}

	public synchronized JXStringBuffer insert(int offset, String str)
	{
		if (offset < 0 || offset > length)
			throw new StringIndexOutOfBoundsException();
		int n = str.length();
		insertGap(offset, n);
		str.getChars(0, n, data, offset);
		length += n;
		return this;
	}

	public JXStringBuffer insert(int offset, boolean b)
	{
		return insert(offset, String.valueOf(b));
	}

	public JXStringBuffer insert(int offset, int i)
	{
		return insert(offset, String.valueOf(i));
	}

	public JXStringBuffer insert(int offset, long l)
	{
		return insert(offset, String.valueOf(l));
	}

  /*
        public JXStringBuffer insert(int offset, float f)
	{
		return insert(offset, String.valueOf(f));
	}

	public JXStringBuffer insert(int offset, double d)
	{
		return insert(offset, String.valueOf(d));
	}
	*/
	public JXStringBuffer insert(int offset, Object o)
	{
		return insert(offset, String.valueOf(o));
	}

    /**
     * Remove all characters from JXStringBuffer.
     * This method sets the JXStringBuffer back to its initial state.
     * @since JX
     */
    public synchronized void clear() {
	this.length = 0;
    }
}
}

