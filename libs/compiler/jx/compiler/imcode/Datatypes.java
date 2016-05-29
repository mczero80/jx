
package jx.compiler.imcode;

final public class Datatypes {

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

}
