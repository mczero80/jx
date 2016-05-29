/* java.lang.reflect.Array
   Copyright (C) 1998, 1999 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.
 
GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

As a special exception, if you link this library with other files to
produce an executable, this library does not by itself cause the
resulting executable to be covered by the GNU General Public License.
This exception does not however invalidate any other reasons why the
executable file might be covered by the GNU General Public License. */


package java.lang.reflect;

/**
 ** Array is a set of static helper functions that allow you to create and manipulate arrays
 ** without knowing their type.<P>
 **
 ** <B>Note:</B> This class uses a Class object to tell what type of thing to work with.  If you wish
 ** to work with primitive types, you may still use the Class functions; there are Class types
 ** defined that represent each different primitive type.  They are <code>java.lang.Boolean.TYPE,
 ** java.lang.Byte.TYPE, </code>etc.  These are not to be confused with the classes
 ** <code>java.lang.Boolean, java.lang.Byte</code>, etc., which are real classes.<P>
 **
 ** <B>Also:</B> If the type of the array is primitive, the accessor functions will wrap the returned
 ** value in the appropriate class type (boolean = java.lang.Boolean, etc.).<P>
 **
 ** <B>Performance note:</B> This class performs best when it does not have to convert primitive types.  The further
 ** along the chain it has to convert, the worse performance will be.  You're best off using the array as whatever
 ** type it already is, and then converting the result.  You will do even worse if you do this and use the generic
 ** set() function.
 **
 ** @author John Keiser
 ** @version 1.1.0, 31 May 1998
 ** @see java.lang.Boolean#TYPE
 ** @see java.lang.Byte#TYPE
 ** @see java.lang.Short#TYPE
 ** @see java.lang.Character#TYPE
 ** @see java.lang.Integer#TYPE
 ** @see java.lang.Long#TYPE
 ** @see java.lang.Float#TYPE
 ** @see java.lang.Double#TYPE
 **/
public final class Array {
	// Make this class uninstantiable.
	private Array() {}

	/** Creates a new single-dimensioned array.  Will return null if the array is Void.
	 ** @param componentType	the type of the array to create.
	 ** @param length		the length of the array to create.
	 ** @exception NegativeArraySizeException when length is less than 0.
	 ** @return the created array, cast to an Object.
	 **/
	public static Object newInstance(Class componentType, int length)
		throws NegativeArraySizeException {
		if(componentType == Boolean.TYPE) {
			return new boolean[length];
		} else if(componentType==Byte.TYPE) {
			return new byte[length];
		} else if(componentType==Character.TYPE) {
			return new char[length];
		} else if(componentType==Short.TYPE) {
			return new short[length];
		} else if(componentType==Integer.TYPE) {
			return new int[length];
		} else if(componentType==Long.TYPE) {
			return new long[length];
		} else if(componentType==Float.TYPE) {
			return new float[length];
		} else if(componentType==Double.TYPE) {
			return new double[length];
		} else if(componentType==Void.TYPE) {
			return null;
		} else {
			return createObjectArray(componentType, length);
		}
	}

	/** Creates a new multi-dimensioned array.  Returns null if array is Void.
	 ** @param componentType	the type of the array to create.
	 ** @param dimensions		the dimensions of the array to create.  Each element in the
	 ** 				<code>dimensions</code> represents another dimension of the created
	 **				array.  Thus, <code>newInstance(java.lang.Boolean, {1,2,3})</code>
	 **				is the same as <code>new java.lang.Boolean[1][2][3]</code>.
	 ** @exception NegativeArraySizeException	when any of the dimensions is less than 0.
	 ** @exception IllegalArgumentException		if the the size of <code>dimensions</code> is 0
	 **						or exceeds the maximum number of array dimensions
	 **						the underlying JVM can handle.
	 ** @return the created array, cast to an Object.
	 **/
	public static Object newInstance(Class componentType, int[] dimensions)
		throws IllegalArgumentException,
		       NegativeArraySizeException {

		if(dimensions.length<=0) {
			throw new IllegalArgumentException("Empty dimensions array.");
		}
		return createDimensionedArray(componentType, dimensions, dimensions.length - 1);
	}


	/** Gets the array length.
	 ** @param array		the array.
	 ** @return			the length of the array.
	 ** @exception IllegalArgumentException if <code>array</code> is not an array.
	 **/
    public static  int getLength(Object array) throws IllegalArgumentException { throw new Error(); }

	/** Gets an element of an array.  Primitive elements will be wrapped in the corresponding class type.<P>
	 **
	 ** <B>Note:</B> For performance reasons, all true booleans will return the same true object (Boolean.TRUE)
	 ** and all false booleans will return the same false object (Boolean.FALSE).
	 **
	 ** @param array	the array to access.
	 ** @param index	the array index to access.
	 ** @exception IllegalArgumentException		if <code>array</code> is not an array.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
	 ** @return the element at index <code>index</code> in the array <code>array</code>.
         **/
	public static Object get(Object array, int index)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof Object[]) {
			return ((Object[])array)[index];
		} else if(array instanceof boolean[]) {
			return ((boolean[])array)[index] ? Boolean.TRUE : Boolean.FALSE;
		} else if(array instanceof byte[]) {
			return new Byte(((byte[])array)[index]);
		} else if(array instanceof char[]) {
			return new Character(((char[])array)[index]);
		} else if(array instanceof short[]) {
			return new Short(((short[])array)[index]);
		} else if(array instanceof int[]) {
			return new Integer(((int[])array)[index]);
		} else if(array instanceof long[]) {
			return new Long(((long[])array)[index]);
		} else if(array instanceof float[]) {
			return new Float(((float[])array)[index]);
		} else if(array instanceof double[]) {
			return new Double(((double[])array)[index]);
		} else {
			throw new IllegalArgumentException("Parameter not an array.");
		}
	}

	/** Gets an element of a boolean array.
	 ** @param array	the array to access.
	 ** @param index	the array index to access.
	 ** @exception IllegalArgumentException		if <code>array</code> is not a boolean array.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
	 ** @return the boolean element at index <code>index</code> in the array <code>array</code>.
         **/
	public static boolean getBoolean(Object array, int index)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof boolean[]) {
			return ((boolean[])array)[index];
		} else {
			throw new IllegalArgumentException("Not a boolean array.");
		}
	}

	/** Gets an element of a byte array.
	 ** @param array	the array to access.
	 ** @param index	the array index to access.
	 ** @exception IllegalArgumentException		if <code>array</code> is not a byte array.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
	 ** @return the byte element at index <code>index</code> in the array <code>array</code>.
         **/
	public static byte getByte(Object array, int index)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof byte[]) {
			return ((byte[])array)[index];
		} else {
			throw new IllegalArgumentException("Array is not of appropriate type.");
		}
	}

	/** Gets an element of a short array.
	 ** @param array	the array to access.
	 ** @param index	the array index to access.
	 ** @exception IllegalArgumentException		if the elements of <code>array</code> cannot be
	 **						converted via widening conversion to a short.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
	 ** @return the short element at index <code>index</code> in the array <code>array</code>.
         **/
	public static short getShort(Object array, int index)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof short[]) {
			return ((short[])array)[index];
		} else {
			return getByte(array,index);
		}
	}

	/** Gets an element of a char array.
	 ** @param array	the array to access.
	 ** @param index	the array index to access.
	 ** @exception IllegalArgumentException		if <code>array</code> is not a char array.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
	 ** @return the char element at index <code>index</code> in the array <code>array</code>.
         **/
	public static char getChar(Object array, int index)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof char[]) {
			return ((char[])array)[index];
		} else {
			throw new IllegalArgumentException("Not a char array.");
		}
	}

	/** Gets an element of an int array.
	 ** @param array	the array to access.
	 ** @param index	the array index to access.
	 ** @exception IllegalArgumentException		if the elements of <code>array</code> cannot be
	 **						converted via widening conversion to an int.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
	 ** @return the int element at index <code>index</code> in the array <code>array</code>.
         **/
	public static int getInt(Object array, int index)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof int[]) {
			return ((int[])array)[index];
		} else if(array instanceof char[]) {
			return ((char[])array)[index];
		} else {
			return getShort(array,index);
		}
	}

	/** Gets an element of a long array.
	 ** @param array	the array to access.
	 ** @param index	the array index to access.
	 ** @exception IllegalArgumentException		if the elements of <code>array</code> cannot be
	 **						converted via widening conversion to a long.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
	 ** @return the long element at index <code>index</code> in the array <code>array</code>.
         **/
	public static long getLong(Object array, int index)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof long[]) {
			return ((long[])array)[index];
		} else {
			return getInt(array,index);
		}
	}

	/** Gets an element of a float array.
	 ** @param array	the array to access.
	 ** @param index	the array index to access.
	 ** @exception IllegalArgumentException		if the elements of <code>array</code> cannot be
	 **						converted via widening conversion to a float.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
	 ** @return the float element at index <code>index</code> in the array <code>array</code>.
         **/
	public static float getFloat(Object array, int index)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof float[]) {
			return ((float[])array)[index];
		} else {
			return getLong(array,index);
		}
	}

	/** Gets an element of a double array.
	 ** @param array	the array to access.
	 ** @param index	the array index to access.
	 ** @exception IllegalArgumentException		if the elements of <code>array</code> cannot be
	 **						converted via widening conversion to a double.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
	 ** @return the double element at index <code>index</code> in the array <code>array</code>.
         **/
	public static double getDouble(Object array, int index)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof double[]) {
			return ((double[])array)[index];
		} else {
			return getFloat(array,index);
		}
	}

	
	/** Sets an element of an array.  If the array is primitive, then the new value must be of the
	 ** corresponding wrapper type (boolean = java.lang.Boolean).<P>
	 **
	 ** 
	 ** @param array	the array to set a value of.
	 ** @param index	the array index to set the value to.
	 ** @param value	the value to set.
	 ** @exception IllegalArgumentException		if <code>array</code> is not an array or the value
	 **                                             is of the wrong type for the array.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
         **/
	public static void set(Object array, int index, Object value)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof Object[]) {
			((Object[])array)[index] = value;
		} else if(value instanceof Boolean) {
			setBoolean(array,index,((Boolean)value).booleanValue());
		} else if(value instanceof Byte) {
			setByte(array,index,((Byte)value).byteValue());
		} else if(value instanceof Character) {
			setChar(array,index,((Character)value).charValue());
		} else if(value instanceof Short) {
			setShort(array,index,((Short)value).shortValue());
		} else if(value instanceof Integer) {
			setInt(array,index,((Integer)value).intValue());
		} else if(value instanceof Long) {
			setLong(array,index,((Long)value).longValue());
		} else if(value instanceof Float) {
			setFloat(array,index,((Float)value).floatValue());
		} else if(value instanceof Double) {
			setDouble(array,index,((Double)value).doubleValue());
		} else {
			throw new IllegalArgumentException("Tried to set a value on a non-array.");
		}
	}		

	/** Sets an element of a boolean array.
	 **
	 ** @param array	the array to set a value of.
	 ** @param index	the array index to set the value to.
	 ** @param value	the value to set.
	 ** @exception IllegalArgumentException		if <code>array</code> is not a boolean array.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
         **/
	public static void setBoolean(Object array, int index, boolean value)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof boolean[]) {
			((boolean[])array)[index] = value;
		} else {
			throw new IllegalArgumentException("Not a boolean array.");
		}
	}

	/** Sets an element of a byte array.
	 **
	 ** @param array	the array to set a value of.
	 ** @param index	the array index to set the value to.
	 ** @param value	the value to set.
	 ** @exception IllegalArgumentException		if the value cannot be converted via widening
	 **						conversion to the type of <code>array</code>.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
         **/
	public static void setByte(Object array, int index, byte value)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof byte[]) {
			((byte[])array)[index] = value;
		} else {
			setShort(array,index,value);
		}
	}

	/** Sets an element of a char array.
	 **
	 ** @param array	the array to set a value of.
	 ** @param index	the array index to set the value to.
	 ** @param value	the value to set.
	 ** @exception IllegalArgumentException		if the value cannot be converted via widening
	 **						conversion to the type of <code>array</code>.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
         **/
	public static void setChar(Object array, int index, char value)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof char[]) {
			((char[])array)[index] = value;
		} else {
			setInt(array,index,value);
		}
	}

	/** Sets an element of a short array.
	 **
	 ** @param array	the array to set a value of.
	 ** @param index	the array index to set the value to.
	 ** @param value	the value to set.
	 ** @exception IllegalArgumentException		if the value cannot be converted via widening
	 **						conversion to the type of <code>array</code>.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
         **/
	public static void setShort(Object array, int index, short value)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof short[]) {
			((short[])array)[index] = value;
		} else {
			setInt(array,index,value);
		}
	}

	/** Sets an element of an int array.
	 **
	 ** @param array	the array to set a value of.
	 ** @param index	the array index to set the value to.
	 ** @param value	the value to set.
	 ** @exception IllegalArgumentException		if the value cannot be converted via widening
	 **						conversion to the type of <code>array</code>.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
         **/
	public static void setInt(Object array, int index, int value)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof float[]) {
			((int[])array)[index] = value;
		} else {
			setLong(array,index,value);
		}
	}

	/** Sets an element of a long array.
	 **
	 ** @param array	the array to set a value of.
	 ** @param index	the array index to set the value to.
	 ** @param value	the value to set.
	 ** @exception IllegalArgumentException		if the value cannot be converted via widening
	 **						conversion to the type of <code>array</code>.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
         **/
	public static void setLong(Object array, int index, long value)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof long[]) {
			((long[])array)[index] = value;
		} else {
			setFloat(array,index,value);
		}
	}

	/** Sets an element of a float array.
	 **
	 ** @param array	the array to set a value of.
	 ** @param index	the array index to set the value to.
	 ** @param value	the value to set.
	 ** @exception IllegalArgumentException		if the value cannot be converted via widening
	 **						conversion to the type of <code>array</code>.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
         **/
	public static void setFloat(Object array, int index, float value)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof float[]) {
			((float[])array)[index] = value;
		} else {
			setDouble(array,index,value);
		}
	}

	/** Sets an element of a double array.
	 **
	 ** @param array	the array to set a value of.
	 ** @param index	the array index to set the value to.
	 ** @param value	the value to set.
	 ** @exception IllegalArgumentException		if <code>array</code> is not a double array.
	 ** @exception ArrayIndexOutOfBoundsException	if <code>index</code> is out of bounds.
         **/
	public static void setDouble(Object array, int index, double value)
		throws IllegalArgumentException,
		ArrayIndexOutOfBoundsException {
		if(array instanceof double[]) {
			((double[])array)[index] = value;
		} else {
			throw new IllegalArgumentException("Array is not of appropriate primitive type");
		}
	}


	/*
	 * PRIVATE HELPERS
	 */

	private static Class objectClass;
	static {
		try {
			objectClass = Class.forName("java.lang.Object");
		} catch(Exception E) {
		}
	}

	private static Object createDimensionedArray(Class type, int[] dimensions, int dimensionToAdd)
		throws IllegalArgumentException,
		       NegativeArraySizeException {
		if(dimensionToAdd > 0) {
			Object toAdd = createDimensionedArray(type,dimensions,dimensionToAdd-1);
			Class thisType = toAdd.getClass();
			Object[] retval = (Object[])createObjectArray(thisType, dimensions[dimensionToAdd]);
			if(dimensions[dimensionToAdd]>0) {
				retval[0] = toAdd;
			}
			for(int i=1;i<dimensions[dimensionToAdd];i++) {
				retval[i] = createDimensionedArray(type,dimensions,dimensionToAdd-1);
			}
			return retval;
		} else {
			Object toAdd = newInstance(type,dimensions[0]);
			return toAdd; 
		}
	}

    private static Object createObjectArray(Class type, int dim) { throw new Error(); }
}
