// -*-Java-*-

package jx.classfile.datatypes; 
// COMMON_HEADER: package jx.classfile.datatypes; 

import jx.zero.Debug; 

/**
   The datatypes of the virtual machine are organized in a
   hierarchy. Their classes are mainly used to store numbers
   and have rather little functionality. 
   This class hierarchy distinquishes only between basic datatypes, 
   i.e. all references are treated as equal (no class types). 
   The constants in BCBasicDatatype are used very frequently. 
*/
		       
abstract public class BCBasicDatatype {

  // the following constants for the basic datatypes are
  // choosen according to the JVM opcode values. 
  // DO NOT MODIFY !!!
  // (JVM instructions with the same sematics but different
  //  datatypes (e.g. XSTORE) are enumerated in the sequence
  //  int,long, float, douhle, ref, byte, char, short) 

  // basic datatypes 
  // according to Bytecode instructions (e.g. iaload ff.)
  public static final int INT = 0;  
  public static final int LONG = 1; 
  public static final int FLOAT = 2; 
  public static final int DOUBLE = 3;
  public static final int REFERENCE = 4; 
  public static final int BYTE = 5; 
  public static final int CHAR = 6; 
  public static final int SHORT = 7; 

  public static final int BOOLEAN = 8; 
  public static final int RETURN_ADDRESS = 9;  // possibly not necessary 
  public static final int VOID = 10; 

  public static final int UNKNOWN_TYPE = -1; 
  // have to adapt some methods of this class before using the next
  // two constants 
  private static final int SINGLE_WORD_TYPE = -2;   // currently not used 
  private static final int DOUBLE_WORD_TYPE = -3;   // currently not used 
  public static final int SECOND_SLOT = -4;   // second slot of a long/double


  /** 
      @param a basic datatype 
      @return size of datatype in words
      (the number of words the JVM needs for this datatype when used 
       for a local variable, e.g. sizeInWords(BYTE)==1) 
  */ 
  public static final int sizeInWords(int datatype) {
    if (datatype == LONG || datatype == DOUBLE) 
      return 2;
    else if (datatype == VOID) 
      return 0; 
    else 
      return 1; 
  }

  /** 
      @return JVM-style type descriptor for a datatype.
      e.g. toChar(BYTE)=='B'. 
      if you want a complete type descriptor for a reference, 
      you better take toTypeDesc().
  */ 
  public static final char toChar(int datatype) {
    switch (datatype) {
    case INT: return BasicTypeDescriptor.INT; 
    case LONG: return BasicTypeDescriptor.LONG; 
    case FLOAT: return BasicTypeDescriptor.FLOAT; 
    case DOUBLE: return BasicTypeDescriptor.DOUBLE; 
    case REFERENCE : return BasicTypeDescriptor.CLASS; 
    case BYTE: return BasicTypeDescriptor.BYTE; 
    case CHAR : return BasicTypeDescriptor.CHAR; 
    case SHORT : return BasicTypeDescriptor.SHORT; 
    case BOOLEAN: return BasicTypeDescriptor.BOOLEAN; 
    case VOID: return BasicTypeDescriptor.VOID; 
    default: Debug.throwError(); 
      return 'x'; 
    }
  }
  
  /** 
      @return a JVM-style type descriptor for a datatype.
      this method is similar to toChar(), but returns 
      a type descriptor for java/lang/Object for references 
  */ 
  public static final String toTypeDesc(int datatype) {
    if (datatype==REFERENCE) 
      return "Ljava/lang/Object";
    else 
      return String.valueOf(toChar(datatype)); 
  }

  /** 
      @return true, if the datatype has two words 
  */ 
  public static final boolean isDouble(int datatype) {
    return datatype==LONG || datatype==DOUBLE;
  }
  
  /** 
      @return number of bytes at least required for this
      datatype 
  */ 
  public static final int sizeInByte(int datatype) {
    switch (datatype) {
    case INT: return 4; 
    case LONG: return 8; 
    case FLOAT: return 4; 
    case DOUBLE: return 8; 
    case REFERENCE: return 4; 
    case BYTE: return 1; 
    case CHAR: return 2; 
    case SHORT: return 2; 
    case BOOLEAN: return 1; 
    default: return 4; 
    }
  }

  /** 
      translate a datatype of the frontend (defined in this class) into a 
      datatype for the backend of the compiler (class nativecode.IMCode).
      Both modules use the same constants for some datatypes, so that
      this translation is very straightforward. 
  */ 
  public static final int translateToIMType(int datatype) {
    Debug.assert(datatype != VOID); 
    if (datatype >= REFERENCE && datatype <= RETURN_ADDRESS) 
      return INT; 
    else 
      return datatype; 
  }

  // nonstatic methods (type(), sizeInWords()) 
  // necessary if this class is used to represent a value
  // sematics equal to the static methods above 
    abstract public int type();

  public final int sizeInWords() {return sizeInWords(type());}

  public final static String toSymbol(int datatype) {
    switch (datatype) {
    case INT: return "i";
    case LONG: return "l";
    case FLOAT: return "l"; 
    case DOUBLE: return "d"; 
    case REFERENCE: return "R"; 
    case BYTE: return "b"; 
    case CHAR: return "c"; 
    case SHORT: return "s"; 
    case BOOLEAN: return "b"; 
    case VOID: return "V"; 
    default: return "?"; 
    }
  }

  public final static String toString(int datatype) {
    switch (datatype) {
    case INT: return "int";
    case LONG: return "long";
    case FLOAT: return "float"; 
    case DOUBLE: return "double"; 
    case REFERENCE: return "reference"; 
    case BYTE: return "byte"; 
    case CHAR: return "char"; 
    case SHORT: return "short"; 
    case BOOLEAN: return "boolean"; 
    case VOID: return "void"; 
    default: return "unknown"; 
    }
  }
}
