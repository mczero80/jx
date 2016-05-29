
package jx.classfile.datatypes; 
final public class BCByte extends BCIntegerDatatype {
  private byte value; 

  public BCByte(byte value) {this.value = value;}
  public byte value() {return value;}
  public long longValue() {return value;}
  public String toString() {return String.valueOf(value); }
  public int type() {return BYTE;}

  protected BCIntegerDatatype getObjectFor(long value) {
    return new BCByte((byte)value); 
  }
}
