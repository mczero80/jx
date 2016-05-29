
package jx.classfile.datatypes; 
final public class BCFloat extends BCFloatDatatype {
  private float value; 

  public BCFloat(float value) {this.value = value;}
  public float value() {return value;}
  public double doubleValue() {return value;}
    //public String toString() {return String.valueOf(value); }
  public int type() {return FLOAT;}
  public int getBits() {return Float.floatToIntBits(value);}

  public static final BCFloat VALUE_0 = new BCFloat(0); 
  public static final BCFloat VALUE_1 = new BCFloat(1); 
  public static final BCFloat VALUE_2 = new BCFloat(2); 

  public BCNumericDatatype combined(int operator, BCNumericDatatype op2) {
    float val1 = value; 
    float val2 = ((BCFloat)op2).value; 
    float result=0; 
    switch (operator) {
    case ADD:  result = val1 + val2;   break; 
    case SUB:  result = val1 - val2;   break; 
    case MUL:  result = val1 * val2;   break; 
    case DIV:  result = val1 / val2;   break; 
    }
    return new BCFloat(result); 
  }

  public BCNumericDatatype negated() {return new BCFloat(-value);}

}
