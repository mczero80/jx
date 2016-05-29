package jx.classfile; 

import java.io.*; 
import jx.classfile.constantpool.*; 
import jx.classfile.datatypes.*; 
import jx.zero.Debug; 

/** 
    All data about the bytecode of a method, that can be found in a
    class file (code attribute of method). This is a rather passive class.
    It only reads the data from the class file and stores it.
*/ 
final public class CodeData {
  int maxStack; 
  int maxLocals; 

  byte[] codeBytes; 
  int numHandlers; 
  ExceptionHandlerData[] exceptionHandler; 

    LineAttributeData []lineNumberTable;

    //needed by jx.verifier.wcet.WCETAnalysis.createETCheckedMethod
    public void increaseMaxLocals() {maxLocals++;}
    public void increaseMaxStack() {maxStack++;}

    public void setCodeBytes(byte[] newCode) {codeBytes = newCode;}

  public CodeData() {}
  public CodeData(DataInput input, ConstantPool cPool) 
    throws IOException {
      readFromClassFile(input, cPool); 
  }
    
    //Constructor for copy
    private CodeData(int maxStack,
		     int maxLocals,
		     byte[] codeBytes,
		     ExceptionHandlerData[] exceptionHandler,
		     LineAttributeData []lineNumberTable) {
	this.maxStack = maxStack;
	this.maxLocals = maxLocals;
	this.codeBytes = new byte[codeBytes.length];
	for (int i = 0; i < codeBytes.length; i++)
	    this.codeBytes[i] = codeBytes[i];
	numHandlers = exceptionHandler.length;
	this.exceptionHandler = new ExceptionHandlerData[numHandlers];
	for (int i = 0; i < numHandlers; i++)
	    this.exceptionHandler[i] = exceptionHandler[i].copy();
	this.lineNumberTable = new LineAttributeData[lineNumberTable.length];
	for (int i = 0; i < lineNumberTable.length; i++)
	    this.lineNumberTable[i] = lineNumberTable[i].copy();
    }

    public CodeData copy() {
	return new CodeData(maxStack, maxLocals, codeBytes, exceptionHandler, 
			    lineNumberTable);
    }

  public int getNumStackSlots() {return maxStack;}
  public int getNumLocalVariables() {return maxLocals;}
 
  public byte[] getBytecode() {return codeBytes; }

  public int getBytecodeLength() {return codeBytes.length;}

    public LineAttributeData[] getLineNumberTable() { return lineNumberTable; }

  public ExceptionHandlerData[] getExceptionHandlers() {
    // assert(exceptionHandler.length == numHandlers); 
    return exceptionHandler; 
  }
  
  public void readFromClassFile(DataInput input, ConstantPool cPool) 
    throws IOException {
      // bytes_count already read 
      maxStack = input.readUnsignedShort(); 
      maxLocals = input.readUnsignedShort(); 
      
      int numCodeBytes = input.readInt(); 
      codeBytes = new byte[numCodeBytes]; 
      input.readFully(codeBytes, 0, numCodeBytes); 
      
      // System.out.println(getDescription(cPool)); 

      numHandlers = input.readUnsignedShort(); 
      exceptionHandler = new ExceptionHandlerData[numHandlers]; 
      for(int i=0; i<numHandlers; i++) {
	exceptionHandler[i] = new ExceptionHandlerData(); 
	exceptionHandler[i].readFromClassFile(input, cPool); 
      }
      
      int numAttributes = input.readUnsignedShort(); 
      for(int i=0; i<numAttributes; i++) 
	readAttribute(input, cPool); 
  }

  private void readAttribute(DataInput input, ConstantPool cPool) 
    throws IOException {
      int attrNameCPIndex = input.readUnsignedShort(); 
      int numBytes = input.readInt(); 
      String attrName = cPool.getUTF8StringAt(attrNameCPIndex);
      if (attrName.equals("LineNumberTable")) {
	  int numLines = input.readShort(); 
	  lineNumberTable = new LineAttributeData[numLines];
	  for(int i=0; i<numLines; i++) {
	      lineNumberTable[i] = new LineAttributeData(input.readShort(), input.readShort());
	  }
      } else { 
	  input.skipBytes(numBytes); 
      }
  }

  public String toString() {
    return super.toString() + "\n" +  
      "MaxStack     : " + maxStack + "\n" + 
      "maxLocals    : " + maxLocals + "\n" + 
      "numCodeBytes : " + codeBytes.length + "\n"; 
  }

  public String getDescription(ConstantPool cPool) {
    return toString(); 
  }

}
