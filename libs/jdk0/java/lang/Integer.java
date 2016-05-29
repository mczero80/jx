package java.lang;

public class Integer extends Number
{
    public static final int MIN_VALUE = 0x80000000;
    public static final int MAX_VALUE = 0x7FFFFFFF;
    public static final Class TYPE = Class.getPrimitiveClass("int");

    private int value;

    public Integer(int value)
    {
	this.value = value;
    }

    public Integer(String s) throws NumberFormatException
    {
	value = parseInt(s);
    }

    public String toString()
    {
	return toString(value);
    }

    public boolean equals(Object obj)
    {
	if (obj != null && obj instanceof Integer)
	    {
		return (value == ((Integer)obj).value);
	    }
	return false;
    }

    public int hashCode()
    {
	return value;
    }

    public int intValue()
    {
	return value;
    }

    public long longValue()
    {
	return (long)value;
    }

    
      public float floatValue()
      {
	  throw new Error(); /*return (float)value;*/
      }

      public double doubleValue()
      {
      throw new Error(); /*return (double)value;*/
      }
    
    public static String toString(int i)
    {
	return toString(i, 10);
    }

    public static String toString(int value, int radix) {
	StringBuffer buffer = new StringBuffer(10);

	if (value == 0) {
	    buffer.append('0');
	    return buffer.toString();
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

	return buffer.toString();
    }


    private static String toUnsignedString(int value, int shift)
    {
        if( value == 0 )
	    return "0";
       
	String result = "";
	int radix = 1 << shift;
	int mask = radix - 1;
	while (value != 0)
	    {
		result = Character.forDigit(value & mask, radix) + result;
		value >>>= shift;
	    }
	return result;
    }

    public static String toHexString(int i)
    {
	return toUnsignedString(i, 4);
    }

    public static String toOctalString(int i)
    {
	return toUnsignedString(i, 3);
    }

    public static String toBinaryString(int i)
    {
	return toUnsignedString(i, 1);
    }

    public static int parseInt(String s) throws NumberFormatException
    {
	return parseInt(s, 10);
    }

    public static int parseInt(String s, int radix) throws NumberFormatException
    {
	if (s == null || s.equals(""))
	    throw new NumberFormatException();
	if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
	    throw new NumberFormatException();

	int position = 0;
	int result = 0;
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
    
    public static Integer decode(String s) { return valueOf(s); }
    
    public static Integer valueOf(String s) throws NumberFormatException
    {
	return new Integer(parseInt(s));
    }

    public static Integer valueOf(String s, int radix) throws NumberFormatException
    {
	return new Integer(parseInt(s, radix));
    }

    public static Integer getInteger(String nm)
    {
	return getInteger(nm, null);
    }

    public static Integer getInteger(String nm, int val)
    {
	Integer result = getInteger(nm, null);
	return ((result == null) ? new Integer(val) : result);
    }

    public static Integer getInteger(String nm, Integer val)
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
}

