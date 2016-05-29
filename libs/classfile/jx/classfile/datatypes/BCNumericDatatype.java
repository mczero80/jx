
package jx.classfile.datatypes; 
// the following classes are used to represent values of
// different datatypes (e.g. to store constants in package bytecode.code) 

abstract public class BCNumericDatatype extends BCBasicDatatype {

  // for combined() 
  public static final int ADD = 0; 
  public static final int SUB = 1; 
  public static final int MUL = 2; 
  public static final int DIV = 3; 
  public static final int REM = 4; 
  public static final int NEG = 5; 
  public static final int SHL = 6; 
  public static final int SHR = 7; 
  public static final int USHR = 8; 
  public static final int AND = 9;  
  public static final int OR = 10;
  public static final int XOR = 11; 
	
  // functions for performing arithmetic operations 
  abstract public BCNumericDatatype combined(int operator, BCNumericDatatype op2); 
  abstract public BCNumericDatatype negated(); 
}
