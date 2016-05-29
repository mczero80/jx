package java.lang;

public final class Long extends Number
{
    public static final Class TYPE = Class.getPrimitiveClass("long");
	public static final long MIN_VALUE = 0x8000000000000000L;
	public static final long MAX_VALUE = 0x7FFFFFFFFFFFFFFFL;

	private long value;

	private static String toUnsignedString(long value, int shift)
	{
		if( value == 0 )
			return "0";
		
		String result = "";
		int radix = 1 << shift;
		int mask = radix - 1;
		while (value != 0)
		{
			result = Character.forDigit((int)(value & mask), radix) + result;
			value >>>= shift;
		}
		return result;
	}

	public static String toString(long value)
	{
		return toString(value, 10);
	}

	public static String toString(long value, int radix)
	{
		if (value == 0)
			return "0";

		boolean negative = false;
		if (value < 0)
		{
			value = -value;
			negative = true;
		}

		String result = "";

		while (value > 0)
		{
			result = Character.forDigit((int)(value % radix), radix) + result;
			value /= radix;
		}

		return negative ? ("-" + result) : result;
	}

	public String toString()
	{
		return toString(value);
	}

	public static String toHexString(long i)
	{
		return toUnsignedString(i, 4);
	}

	public static String toOctalString(long i)
	{
		return toUnsignedString(i, 3);
	}

	public static String toBinaryString(long i)
	{
		return toUnsignedString(i, 1);
	}

	public static long parseLong(String s) throws NumberFormatException
	{
		return parseLong(s, 10);
	}

	public static long parseLong(String s, int radix) throws NumberFormatException
	{
		if (s == null || s.equals(""))
			throw new NumberFormatException();

		if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
			throw new NumberFormatException();

		int position = 0;
		long result = 0;
		boolean signed = false;

		if (s.charAt(position) == '-')
		{
			signed = true;
			position++;
		}

		int digit;

		for (; position < s.length(); position++)
		{
			digit = Character.digit(s.charAt(position), radix);
			if (digit < 0)
				throw new NumberFormatException();
			result = (result * radix) - digit;
		}

		if (!signed && result < -MAX_VALUE)
			throw new NumberFormatException();

		return (signed ? result : -result );
	}

	public static Long valueOf(String s) throws NumberFormatException
	{
		return new Long(parseLong(s));
	}

	public static Long valueOf(String s, int radix) throws NumberFormatException
	{
		return new Long(parseLong(s, radix));
	}

	public static Long getLong(String nm)
	{
		return getLong(nm, null);
	}

	public static Long getLong(String nm, long val)
	{
		Long result = getLong(nm, null);
		return ((result == null) ? new Long(val) : result);
	}

	public static Long getLong(String nm, Long val)
	{
		String value = System.getProperty(nm);
		if (value == null)
			return val;
		try
		{
			if (value.startsWith("0x"))
				return valueOf(value.substring(2), 16);
			if (value.startsWith("#"))
				return valueOf(value.substring(1), 16);
			if (value.startsWith("0"))
				return valueOf(value.substring(1), 8);
			return valueOf(value, 10);
		}
		catch (NumberFormatException ex)
		{
		}
		return val;
	}

	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		if (!(obj instanceof Long))
			return false;

		Long l = (Long) obj;

		return value == l.longValue();
	}

	public int hashCode()
	{
		return (int)(value ^ (value >>> 32));
	}

	public int intValue()
	{
		return (int) value;
	}

	public long longValue()
	{
		return (long) value;
	}

  
	public float floatValue()
	{
	    /*return (float) value;*/ throw new Error();
	}

	public double doubleValue()
	{
	     throw new Error();
	     /*return (double) value;*/
	}
	
	public Long(long value)
	{
		this.value = value;
	}

	public Long(String s) throws NumberFormatException
	{
		this.value = valueOf(s).longValue();
	}



}

