// -*-Java-*-

package jx.classfile.datatypes; 
// COMMON_HEADER: package jx.classfile.datatypes; 

import jx.zero.Debug; 
// COMMON_HEADER: import jx.zero.Debug; 

/** 
    This class is used to parse the type descriptor strings
    for field and method types.
*/ 

public class BasicTypeDescriptor {
  private String typeDesc; 

  /** 
      @param typedesc a JVM type descriptor for a datatype 
  */ 
  public BasicTypeDescriptor(String typeDesc) {
    Debug.assert(typeDesc.length() >= 1); 
    this.typeDesc = typeDesc;
  }

  private char firstChar() {return typeDesc.charAt(0);}

  /**  
      @return true, if the type descriptor represents a
      primitive type (e.g. an int or a long, but not an object) 
  */ 
  public boolean isPrimitive() {
    return firstChar() != CLASS && firstChar() != ARRAY; 
  }

  /** 
      used for primitive types, to convert them to basic datatypes 
  */ 
  public int toBasicDatatype() {
    return toBasicDatatype(firstChar());
  }

    public boolean isArray() { return firstChar()==ARRAY; }
    public boolean isClass() { return firstChar()==CLASS; }
    public boolean isVoid() { return firstChar() == VOID; }
    public boolean isInteger() { return firstChar() == INT; }
    public boolean isShort() { return firstChar() == SHORT; }
    public boolean isLong() { return firstChar() == LONG; }
    public boolean isByte() { return firstChar() == BYTE; }
    public boolean isFloat() { return firstChar() == FLOAT; }
    public boolean isDouble() { return firstChar() == DOUBLE; }
    public boolean isChar() { return firstChar() == CHAR; }
    public boolean isBoolean() { return firstChar() == BOOLEAN; }

  public int getArrayDimension() {
    int numDimensions = 0; 
    while (typeDesc.charAt(numDimensions)=='[') numDimensions++; 
    return numDimensions;
  }
  
  /** 
      @return the type descriptor of the array fields
      (without the ['s ) 
  */ 
  public String getArrayTypeDesc() {
    return typeDesc.substring(getArrayDimension()); 
  }


  public BasicTypeDescriptor getComponentType() {
      if (! isArray()) return null;
    return new BasicTypeDescriptor(typeDesc.substring(getArrayDimension())); 
  }

  /** 
      @return the name of the class. 
      NOTE: typeDesc must represent a class and not
      a basic datatype !!! 
  */ 
  public String getClassName() {
    // remove leading 'L' and trailing ';' 
      // Debug.out.println("***"+typeDesc+"***");
    return typeDesc.substring(1, typeDesc.length()-1 ); 
  }  

    /**
       @return type name that would be used for this type at the Java language level
    */
    public String getJavaLanguageType() {
	if (isClass()) return getClassName().replace('/', '.');
	if (firstChar()=='[') {
	    String s = typeDesc.substring(1, typeDesc.length());
	    String at = new BasicTypeDescriptor(s).getJavaLanguageType();
	    return at+"[]";
	}
	switch(firstChar()) {
	case 'B': return "byte";
	case 'C': return "char";
	case 'D': return "double";
	case 'F': return "float";
	case 'I': return "int";
	case 'J': return "long";
	case 'S': return "short";
	case 'Z': return "boolean";
	case 'V': return "void";
	}
	throw new Error();
    }

  // type descriptors for basic datatypes 
  public static final char BYTE    = 'B'; 
  public static final char CHAR    = 'C'; 
  public static final char DOUBLE  = 'D'; 
  public static final char FLOAT   = 'F'; 
  public static final char INT     = 'I'; 
  public static final char LONG    = 'J'; 
  public static final char SHORT   = 'S'; 
  public static final char BOOLEAN = 'Z'; 
  public static final char CLASS   = 'L'; 
  public static final char ARRAY   = '['; 
  public static final char VOID    = 'V'; 

  /** 
      @return true, if it is a basic datatype (no reference) 
  */ 
  public static final boolean isPrimitive(char firstChar) {
    return firstChar != CLASS && firstChar != ARRAY;
  }

  public static final boolean isArray(char firstChar) {
    return firstChar == ARRAY;
  }

  public static final boolean isClass(char firstChar) {
    return firstChar == CLASS;
  }

    
  /** 
      @return basic datatype, if typeDescriptor represents actually
      a basic datatype
  */ 
  public static final int getBasicDatatypeOf(String typeDescriptor) {
    return toBasicDatatype(typeDescriptor.charAt(0)); 
  }

  public static final int toBasicDatatype(char typeDescriptor) {
    switch (typeDescriptor) {
    case BYTE:    return BCBasicDatatype.BYTE; 
    case CHAR:    return BCBasicDatatype.CHAR; 
    case DOUBLE:  return BCBasicDatatype.DOUBLE; 
    case FLOAT:   return BCBasicDatatype.FLOAT; 
    case INT:     return BCBasicDatatype.INT; 
    case LONG:    return BCBasicDatatype.LONG; 
    case SHORT:   return BCBasicDatatype.SHORT;
    case BOOLEAN: return BCBasicDatatype.BOOLEAN; 
    case CLASS: 
    case ARRAY:   return BCBasicDatatype.REFERENCE; 
    case VOID:    return BCBasicDatatype.VOID; 
    default: 
      // Debug.throwError("Illegal Type Descriptor"); 
      return BCBasicDatatype.UNKNOWN_TYPE; 
    }
  }

    public String toString() { return "BasicTypeDescriptor("+typeDesc+")";}
}
