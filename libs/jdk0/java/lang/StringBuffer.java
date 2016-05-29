package java.lang;

public class StringBuffer
{
	private char[] data;
	private int length;	// actual used bytes in the string

	public StringBuffer(int length)
	{
		data = new char[length];
		this.length = 0;
	}

	public StringBuffer(String str)
	{
		this(str.length() + 16);
		append(str);
	}

	public StringBuffer()
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

	public synchronized StringBuffer reverse()
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

	public synchronized StringBuffer append(char c)
	{
		ensureCapacity(length + 1);
		data[length] = c;
		length++;
		return this;
	}

	public synchronized StringBuffer append(char[] str, int offset, int len)
	{
		ensureCapacity(length + len);
		System.arraycopy(str, offset, data, length, len);
		length += len;
		return this;
	}

	public StringBuffer append(char[] str)
	{
		return append(str, 0, str.length);
	}

	public synchronized StringBuffer append(String str)
	{
		if (str == null)
			return append("null");

		int n = str.length();
		ensureCapacity(length + n);
		str.getChars(0, n, data, length);
		length += n;

		return this;
	}

	public StringBuffer append(boolean b)
	{
		return append(String.valueOf(b));
	}

	public StringBuffer append(int i)
	{
		return append(String.valueOf(i));
	}

    public StringBuffer append(long l)
    {
	return append(String.valueOf(l));
    }
    
	public StringBuffer append(float f)
	{
		return append(String.valueOf(f));
	}

	public StringBuffer append(double d)
	{
		return append(String.valueOf(d));
	}

	public StringBuffer append(Object o)
	{
		if (o == null)
			return append("null");
		return append(String.valueOf(o));
	}

	private void insertGap(int offset, int n)
	{
		ensureCapacity(length + n);
		if (offset < length)
			System.arraycopy(data, offset, data, offset + n, length - offset);
	}

	public synchronized StringBuffer insert(int offset, char c)
	{
		if (offset < 0 || offset > length)
			throw new StringIndexOutOfBoundsException();
		insertGap(offset, 1);
		data[offset] = c;
		length++;
		return this;
	}

	public synchronized StringBuffer insert(int offset, char[] str)
	{
		if (offset < 0 || offset > length)
			throw new StringIndexOutOfBoundsException();
		int n = str.length;
		insertGap(offset, n);
		System.arraycopy(str, 0, data, offset, n);
		length += n;
		return this;
	}

	public synchronized StringBuffer insert(int offset, String str)
	{
		if (offset < 0 || offset > length)
			throw new StringIndexOutOfBoundsException();
		int n = str.length();
		insertGap(offset, n);
		str.getChars(0, n, data, offset);
		length += n;
		return this;
	}

	public StringBuffer insert(int offset, boolean b)
	{
		return insert(offset, String.valueOf(b));
	}

	public StringBuffer insert(int offset, int i)
	{
		return insert(offset, String.valueOf(i));
	}

    /*public StringBuffer insert(int offset, long l)
      {
      return insert(offset, String.valueOf(l));
      }*/

  /*
        public StringBuffer insert(int offset, float f)
	{
		return insert(offset, String.valueOf(f));
	}

	public StringBuffer insert(int offset, double d)
	{
		return insert(offset, String.valueOf(d));
	}
	*/
	public StringBuffer insert(int offset, Object o)
	{
		return insert(offset, String.valueOf(o));
	}

        public StringBuffer delete(int start, int end) {
	    if (start > end || start < 0 || start >= length)
		throw new StringIndexOutOfBoundsException();
	    if (start == end)
		return this;
	    if (end < length)
		System.arraycopy(data, end, data, start, length - end);
	    setLength(length - (end - start));
	    return this;
	}

        public StringBuffer replace(int start, int end, String str) {
	    delete(start, end);
	    insert(start, str);
	    return this;
	}

}

