
package jx.classfile.datatypes; 
final public class BCShort extends BCIntegerDatatype {
  private short value; 

  public BCShort(short value) {this.value = value;}
  public short value() {return value;}
  public long longValue() {return value;}
  public String toString() {return String.valueOf(value); }
  public int type() {return SHORT;}

  protected BCIntegerDatatype getObjectFor(long value) {
    return new BCShort((short)value); 
  }
}
