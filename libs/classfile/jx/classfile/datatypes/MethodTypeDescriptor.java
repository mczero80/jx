
package jx.classfile.datatypes; 
import jx.zero.Debug; 
/** 
    Type descriptor for methods. 
*/
public class MethodTypeDescriptor {
  private String typeDesc; 
  int iteratorIndex,newIteratorIndex; 

  /** 
      @param typeDesc type descriptor for a method 
  */ 
  public MethodTypeDescriptor(String typeDesc) {
    this.typeDesc = typeDesc;
  }

  /** 
      @return an array with all the basic datatypes of the
      parameters (no return type, same order). 
  */ 
  public int[] getBasicArgumentTypes() {
    
    int[] argType = new int[getNumArguments()]; 
    int curArgNum=0; 

    int curCharIndex = 1; // start behind '(' 
    char currentChar = typeDesc.charAt(curCharIndex); 

    while (currentChar != ')') {
      boolean argIsArray = false; 
      
      while (currentChar == '[') { 
	argIsArray = true; 
	curCharIndex++; 
	currentChar = typeDesc.charAt(curCharIndex); 
      }
      
      if (BasicTypeDescriptor.isClass(currentChar)) {
	argType[curArgNum] = BCBasicDatatype.REFERENCE; 
	curCharIndex = typeDesc.indexOf(';', curCharIndex) + 1;
      }
      else {
	if (argIsArray) 
	  argType[curArgNum] = BCBasicDatatype.REFERENCE; 
	else 
	  argType[curArgNum] = BasicTypeDescriptor.
	    toBasicDatatype(currentChar); 
	curCharIndex++; 
      }
      
      curArgNum++; 
      currentChar = typeDesc.charAt(curCharIndex); 
    }
    return argType;  
  }

  /** 
      @return basic datatype of the return type 
  */ 
  public int getBasicReturnType() {
      int returnTypeIdx = 1 + typeDesc.lastIndexOf(')'); 
      return BasicTypeDescriptor.
	toBasicDatatype(typeDesc.charAt(returnTypeIdx));
  }

  public String getReturnTypeDesc() {
    int returnTypeIdx = 1 + typeDesc.lastIndexOf(')'); 
    return typeDesc.substring(returnTypeIdx); 
  }

  public String getJavaReturnType() {
    int returnTypeIdx = 1 + typeDesc.lastIndexOf(')'); 
    String s= typeDesc.substring(returnTypeIdx); 
    return new BasicTypeDescriptor(s).getJavaLanguageType();
  }


  public String[] getArgumentTypeDesc() {
      int n = getNumArguments();
      String[] result = new String[n];
      initReadArguments();
      for(int i=0; i<n; i++) {
	  result[i] = readArgumentTypeDesc();
      }
      return result;
  }

    /** Java language argument list */
  public String getJavaArgumentList() {
      int n = getNumArguments();
      String result = "";
      initReadArguments();
      for(int i=0; i<n; i++) {
	  result += new BasicTypeDescriptor(readArgumentTypeDesc()).getJavaLanguageType();
	  result += " arg"+i;
	  if (i<n-1) result +=", ";
      }
      return result;
  }

  // **** the following functions are currently not used **** 
  // **** (they are rather unimportant and deprecated) **** 


  // call this function before reading arguments 
  private void initReadArguments() {
    iteratorIndex = 1; // Position of the first argument 
  }

  // each call of this function yields an argument of the method, 
  // if there are no more arguments, 'null' is returned
  private String readArgumentTypeDesc() {
      String s = peekArgumentTypeDesc();
      iteratorIndex = newIteratorIndex;
      return s;
  }

    // return argument type but dont update position to next argument
  private String peekArgumentTypeDesc() {

      newIteratorIndex = iteratorIndex;

    // assert(typeDesc has correct syntax) 
    char currentChar = typeDesc.charAt(newIteratorIndex); 

    if (currentChar == ')') return null; 
        
    int startIndex = newIteratorIndex; 

    // skip '['  
    while (currentChar == '[') {
      newIteratorIndex++; 
      currentChar = typeDesc.charAt(newIteratorIndex); 
    }

    if (BasicTypeDescriptor.isPrimitive(currentChar)) {
      newIteratorIndex++; 
      return typeDesc.substring(startIndex, newIteratorIndex);
    }

    if (BasicTypeDescriptor.isClass(currentChar)) {
      newIteratorIndex = typeDesc.indexOf(';', newIteratorIndex); 
      newIteratorIndex++; 
      String arg = typeDesc.substring(startIndex, newIteratorIndex);
      return arg;
    }

    Debug.throwError("Illegal Type Descriptor"); 
    return null; 
  }

    private boolean hasArgumentStringType() {
	String s = peekArgumentTypeDesc();
	if (s==null) return false;
	return s.equals("Ljava/lang/String;");
    }

  private int getNumArguments() {
    int numArgs = 0; 
    int curCharIndex = 1; // start behind '(' 
    char currentChar = typeDesc.charAt(curCharIndex); 
    while (currentChar != ')') {
      numArgs++; 
      
      while (currentChar == '[') {
	curCharIndex++; 
	currentChar = typeDesc.charAt(curCharIndex); 
      }
      
      if (BasicTypeDescriptor.isClass(currentChar)) 
	curCharIndex = typeDesc.indexOf(';', curCharIndex) + 1;
      else 
	curCharIndex++; 
      
      currentChar = typeDesc.charAt(curCharIndex); 
    }
    return numArgs; 
  }


    /******************************************/
    /* NEW INTERFACE */

    public DataType getReturnType() {
	return new DataType(getReturnTypeDesc());
    }

    public DataType[] getArguments() {
	int n = getNumArguments();
	DataType ret[] = new DataType[n];
	initReadArguments();
	for(int i=0; i<n; i++) {
	  ret[i] = new DataType(readArgumentTypeDesc());
	}
	return ret;
    }

}
