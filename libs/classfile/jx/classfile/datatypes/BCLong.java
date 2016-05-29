
package jx.classfile.datatypes; 
final public class BCLong extends BCIntegerDatatype {
  private long value; 
  
  public BCLong(long value) {this.value = value;}
  public long value() {return value;}
  public long longValue() {return value;}
  public int type() {return LONG;}

  public String toString() {return String.valueOf(value); }

  public static final BCLong VALUE_0 = new BCLong(0); 
  public static final BCLong VALUE_1 = new BCLong(1); 

  protected BCIntegerDatatype getObjectFor(long value) {
    return new BCLong(value); 
  }

}
