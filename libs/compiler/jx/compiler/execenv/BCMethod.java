package jx.compiler.execenv; 

import jx.classfile.constantpool.*; 
import jx.classfile.*; 
import jx.classfile.datatypes.*; 
import jx.zero.Debug;
import jx.classfile.VerifyResult;

/** 
    This class represents a Java method without its bytecode. 
    If you need a Java method with bytecode (e.g. for 
    translating it), take the subclass of BCMethod, BCMethodWithCode. 
    This class is used for tasks where the code isn't required, e.g. 
    for testing if a method is 'inlineable'. 
*/ 

public class BCMethod {

  private boolean isSmallEnoughForInlining; 
  protected boolean isOverrideable; 
  protected boolean isStatic; 

  protected String name; 
  protected String typeDescString; 
  
  protected int[] argumentTypes; 
  protected int returnType; 
  private static final int MAX_INLINE_LENGTH = 20; 

  protected MethodSource methodSource;

  public BCMethod(MethodSource methodSource) {
    if (methodSource == null) 
	Debug.out.println("BCMethod methodSource == null");

    this.methodSource = methodSource;

    isSmallEnoughForInlining = false;
    if (methodSource!=null && 
	methodSource.getNumInstr()>0 &&
	methodSource.getNumInstr()<=MAX_INLINE_LENGTH) isSmallEnoughForInlining=true;

    isStatic = methodSource.isStatic(); 

    ClassSource classSource = methodSource.getDeclaringClassSource();

    isOverrideable = !(methodSource.isPrivate() || 
		       methodSource.isFinal() || 
		       methodSource.isStatic() ||
		       classSource.isFinal()); 

    name = methodSource.getMethodName(); 
    //Debug.out.println("BCMethod::name="+name);
    typeDescString = methodSource.getMethodType();

    // translate the type descriptor to basic types 
    MethodTypeDescriptor typeDesc = new MethodTypeDescriptor(typeDescString);
    argumentTypes = typeDesc.getBasicArgumentTypes(); 
    returnType = typeDesc.getBasicReturnType(); 

  }

    public boolean isStatic() { return methodSource.isStatic(); }
    public boolean isConstructor() { return name.equals("<init>"); } 
    public boolean isAbstract() { return methodSource.isAbstract(); }
    // == false : inlining problemlos 
    public boolean isOverrideable() {return isOverrideable;}  
    public boolean isSmallEnoughForInlining() {return isSmallEnoughForInlining; }
    public String getSignature() {return typeDescString; }
    public String getName() {return name; }
    public boolean isNative() { return methodSource.isNative(); }
    public String getClassName() {return methodSource.getDeclaringClassSource().getClassName();}

    public String toString() {
	return "BCMethod("+name+typeDescString+")";
    }

    public int[] getArgumentTypes() { return argumentTypes; }
    public int getReturnType() { return returnType; }
    public boolean returnsReference() { return returnType == BCBasicDatatype.REFERENCE; }

    public LineAttributeData[] getLineNumberTable() { return methodSource.getLineNumberTable(); }

    public int getLineNumber(int bytecodePosition) {
	LineAttributeData lineNumbers[] = methodSource.getLineNumberTable();
	int line=0;
	if (lineNumbers != null) {
	    for(int k=0; k<lineNumbers.length; k++) {		
		if (bytecodePosition<lineNumbers[k].startBytecodepos) return line;
		line=lineNumbers[k].lineNumber;
	    }
	}
	return -1;
    }

    public VerifyResult  getVerifyResult(int type) {
	return methodSource.getVerifyResult(type);
    }
}

