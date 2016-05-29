package jx.compiler.execenv; 

import jx.compiler.*;
import jx.compiler.symbols.*;

import java.util.Vector;
import java.util.Enumeration; 
//import jx.jit.debug.DebugConf; 
import jx.zero.Debug; 
import java.io.*;  

//import jx.jit.bytecode.execenv.NativeExceptionHandler; 

/** 
    BinaryCode is a class that generates and stores a compiled
    function. It is also responsible for resolving jumps and 
    printing debugging output. 
    It uses the classes from package i386 to do this 
    task. 
*/ 
public class BinaryCode {

  // bytes of the compiled function 
  private byte[] code; 
  private int numCodeBytes; 

  // table of unresolved addresses that need the code base 
  private Vector unresolvedAddresses; 

  // ***** other vars *****

  private static final int initialCodeSize=100;
  private static final int codeChunkSize=50; 
  private static final boolean DEBUG = true; 
  
  /** 
      Initialize this class with an already compiled 
      code. This is not a clean design, but it is 
      necessary for using preproc.BinaryCode. 
  */ 
  public BinaryCode(byte[] code, int numBytes, Vector unresolvedAddr) {
    this.unresolvedAddresses = unresolvedAddr; 
    this.code = code; 
    this.numCodeBytes = numBytes; 
  }
    
  // ***** Install Functions ***** 

  /** 
      Install the compiled method in metaXa. 
  */ 
  public void install(String classname, String methodname, String signature) {
    int handle = allocateNativeCode(numCodeBytes);
    int codeBaseAddress =  nativeCodeBase(handle);

    resolveAddresses(codeBaseAddress);     

    writeNativeCode(handle, code, 0, numCodeBytes);
    replugMethod(classname, methodname, signature, handle);    
  }

  public byte[] getCode() { return code; }
  public int getNumCodeBytes() { return numCodeBytes; }
  public Vector getUnresolvedAddresses() { return unresolvedAddresses; }

   
  /** 
      @param codeStartAddress address of the compiled code in the memory 
      @see nativecode.BinaryCode.addInstrCall
  */ 
  private void resolveAddresses(int codeStartAddress) {
    Enumeration elements = unresolvedAddresses.elements(); 
    while (elements.hasMoreElements()) {
      SymbolTableEntryBase address =
	(SymbolTableEntryBase)elements.nextElement(); 
      address.apply(code, codeStartAddress); 
    }
  }
    

  // ***** Output Functions *****

  private String getBinaryCodeAsHex(int firstByte, int stopByte) {
    String s = ""; 
    for(int i=firstByte; i<stopByte; i++) {
      String hex = Integer.toHexString(code[i] & 0xff); 
      if (hex.length()==1) hex = "0" + hex; 
      s = s + hex  + " "; 
    }
    return s; 
  }

  // returns a hexdump of the compiled function 
  public String getBinaryCodeAsHex() {
    return getBinaryCodeAsHex(0, numCodeBytes); 
  }

  // returns a description of all relative jumps 
  public String getJumpTableDesc() {
    return unresolvedAddresses.toString(); 
  }
  
  /** 
      The interface to the metaXa JVM
  */ 

  /**
   * allocate code memory 
   * @param size size of memory block
   * @return handle to the memory block or -1 if error
   */
    private  int allocateNativeCode(int size) {	throw new Error(); }

  /**
   * Write the native code into the memory assoziated with handle.
   * @param handle Handle to native code memory
   */
  private void writeNativeCode(int handle, byte buffer[],
				      int off, int len) {	throw new Error(); }

  /**
   * Write the native code into the memory assoziated with handle.
   * @param handle Handle to native code memory
   */
    /*
  private void writeNativeCodeWithHandler(int handle, byte buffer[],
						 int off, int len, 
						 NativeExceptionHandler[] 
						 exceptionHandler)
 {	throw new Error(); }
    */



  /**
   * Get the base address of the native code memory.
   * @param handle Handle to code memory.
   * @return unsigned base address of native code memory, 
   *         on 32-bit machines only the lower 32 bits are used
   */
  private int nativeCodeBase(int handle) {	throw new Error(); }

  /**
   * Free the native code memory.
   * @param handle Handle to code memory.
   */
  private void freeNativeCode(int handle) {	throw new Error(); }

  /**
   * Replaces the method code with this native code.
   * @param handle Handle to code memory.
   */
  private void replugMethod(String classname, String methodname,
				   String signature, int handle) {	throw new Error(); }

  /** 
      Deactivate compiled code and use interpreted code for this 
      method. 
  */ 
  public static void unplugMethod(String classname,
					 String methodname, String signature) {	throw new Error(); }

}  

