package jx.classfile; 

/** 
    Adapter for sources of the data of 
    bytecode methods. 
    See subclasses 
*/ 

abstract public class MethodSource {

  abstract public ClassSource getDeclaringClassSource();
  abstract public String getMethodName(); 
  abstract public String getMethodType(); 

  abstract public boolean isPublic();
  abstract public boolean isPrivate();
  abstract public boolean isProtected();
  abstract public boolean isStatic();
  abstract public boolean isFinal();
  abstract public boolean isAbstract();
  abstract public boolean isNative();

  abstract public byte[] getBytecode();
  abstract public int getNumInstr();
  abstract public int getNumLocalVariables(); 
  abstract public int getNumStackSlots(); 
  abstract public ExceptionHandlerData[] getExceptionHandlers(); 
  abstract public LineAttributeData[] getLineNumberTable(); 

  abstract public VerifyResult getVerifyResult(int type);
  abstract public void setVerifyResult(VerifyResult newElm);
}
