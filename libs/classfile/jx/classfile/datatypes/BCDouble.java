
package jx.classfile.datatypes; 
final public class BCDouble extends BCFloatDatatype {
  private double value; 

  public BCDouble(double value) {this.value = value;}
  public double value() {return value;}
  public double doubleValue() {return value;}
    //public String toString() {return String.valueOf(value); }
  public int type() {return DOUBLE;}
  public long getBits() {return Double.doubleToLongBits(value);}

  public static final BCDouble VALUE_0 = new BCDouble(0.0); 
  public static final BCDouble VALUE_1 = new BCDouble(1.0); 

  public BCNumericDatatype combined(int operator, BCNumericDatatype op2) {
    double val1 = value; 
    double val2 = ((BCDouble)op2).value; 
    double result=0; 
    switch (operator) {
    case ADD:  result = val1 + val2;   break; 
    case SUB:  result = val1 - val2;   break; 
    case MUL:  result = val1 * val2;   break; 
    case DIV:  result = val1 / val2;   break; 
    }
    return new BCDouble(result); 
  }

  public BCNumericDatatype negated() {return new BCDouble(-value);}

}
