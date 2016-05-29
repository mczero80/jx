package java.lang;

public final class Double extends Number {
    public static final double POSITIVE_INFINITY = 1.0 / 0.0;
    public static final double NEGATIVE_INFINITY = -1.0 / 0.0;
    public static final double NaN = 0.0d / 0.0;
    public static final double MAX_VALUE = 1.79769313486231570e+308;
    public static final double MIN_VALUE = 4.94065645841246544e-324;
    //public static final Class	TYPE = Class.getPrimitiveClass("double");

    public Double(double a) {
	value = a;
    }

    public static String toString(double d){
	return "???";
    }
    private double value;

    public byte byteValue() {
	return (byte)value;
    }

    public short shortValue() {
	return (short)value;
    }

    public int intValue() {
	return (int)value;
    }

    public long longValue() {
	return (long)value;
    }

    public float floatValue() {
	return (float)value;
    }

    public double doubleValue() {
	return (double)value;
    }

    public static Double valueOf(String s) throws NumberFormatException { 
	return new Double(valueOf0(s));
    }

    static public boolean isNaN(double v) {
	return (v != v);
    }

    static public boolean isInfinite(double v) {
	return (v == POSITIVE_INFINITY) || (v == NEGATIVE_INFINITY);
    }


    public Double(String s) throws NumberFormatException {
	// REMIND: this is inefficient
	this(valueOf(s).doubleValue());
    }

    public boolean isNaN() {
	return isNaN(value);
    }

    public boolean isInfinite() {
	return isInfinite(value);
    }

    public String toString() {
	return String.valueOf(value);
    }


    public int hashCode() {
	long bits = doubleToLongBits(value);
	return (int)(bits ^ (bits >> 32));
    }

    public boolean equals(Object obj) {
	return (obj != null)
	       && (obj instanceof Double) 
	       && (doubleToLongBits(((Double)obj).value) == 
		      doubleToLongBits(value));
    }
    public static double longBitsToDouble(long bits){throw new Error();}

    static double valueOf0(String s) throws NumberFormatException{throw new Error();}


    private static final long serialVersionUID = -9172774392245257468L;
    

    public static long doubleToLongBits(double value) {
	throw new Error();
    }

    public static double parseDouble(java.lang.String s) { throw new Error(); }
    public static int compare(double a, double b) {throw new Error();}
}
