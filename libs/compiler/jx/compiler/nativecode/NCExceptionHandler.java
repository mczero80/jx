package jx.compiler.nativecode; 

public class NCExceptionHandler {
  private int startNCIndex, endNCIndex; 
  private int handlerNCIndex; 

  // methodID and handlerIndex are a ID for this handler 
  // (maybe, they are useless) 
  private Object methodID; 
  private int handlerIndex; 
  
  public NCExceptionHandler(int handlerIndex, Object methodID) {
    startNCIndex = -1; 
    endNCIndex = -1; 
    handlerNCIndex = -1; 
    // methodID and handlerIndex is not necessarily correctly initialized 
    // (see bytecode.code.BCExceptionTableEntry) 
    this.handlerIndex = handlerIndex; 
    this.methodID = methodID; 
  }

  public void setRangeStart(int nativecodeIndex) {
    startNCIndex = nativecodeIndex; 
  }

  public void setRangeEnd(int nativecodeIndex) {
    endNCIndex = nativecodeIndex; 
  }

  public void setHandlerStart(int nativecodeIndex) {
    handlerNCIndex = nativecodeIndex; 
  }

  public boolean isFinished() {
    return startNCIndex >= 0 && endNCIndex >= 0 && handlerNCIndex >= 0; 
  }

  public String toString() {
    return "Exceptionhandler(" + startNCIndex + ", " + endNCIndex + ") -> " + 
      handlerNCIndex + ":"; 
  }
}
