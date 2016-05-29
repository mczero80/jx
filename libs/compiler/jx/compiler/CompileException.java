package jx.compiler; 
/** 
    Base class for all exception that happen during 
    compilation. 
*/  
public class CompileException extends Exception {
  public CompileException(String message) {
    super(message); 
  }
  public CompileException() {
  }
}
