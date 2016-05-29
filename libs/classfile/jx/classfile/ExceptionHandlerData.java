package jx.classfile; 

import java.io.*; 
import jx.classfile.constantpool.*; 
import jx.classfile.datatypes.*; 
import jx.zero.Debug; 

/** 
    All data about a exception handler, that can be found 
    in a class file. This is a rather passive class. 
    It only reads the data from the class file and 
    stores it. 
*/ 
public class ExceptionHandlerData {
  private int startBCIndex; 
  private int endBCIndex; 
  private int handlerBCIndex; 
  private int catchTypeCPIndex; 

  public ExceptionHandlerData() {}
  public ExceptionHandlerData(DataInput input, ConstantPool cPool) 
    throws IOException {
      readFromClassFile(input, cPool); 
  }
  
    //constructor for copy
    public ExceptionHandlerData(int startBCIndex, int endBCIndex, 
				 int handlerBCIndex, int catchTypeCPIndex) {
	this.startBCIndex = startBCIndex;
	this.endBCIndex = endBCIndex;
	this.handlerBCIndex = handlerBCIndex;
	this.catchTypeCPIndex = catchTypeCPIndex;
    }

    public ExceptionHandlerData copy() {
	return new ExceptionHandlerData(startBCIndex, endBCIndex, handlerBCIndex, 
					catchTypeCPIndex);
    }
  public int getStartBCIndex() {return startBCIndex;}
  public int getEndBCIndex() {return endBCIndex; }
  public int getHandlerBCIndex() {return handlerBCIndex;}
  public int getCatchTypeCPIndex() {return catchTypeCPIndex; }

  public void readFromClassFile(DataInput input, ConstantPool cPool) 
    throws IOException {
      startBCIndex = input.readUnsignedShort(); 
      endBCIndex = input.readUnsignedShort(); 
      handlerBCIndex = input.readUnsignedShort(); 
      catchTypeCPIndex = input.readUnsignedShort(); 
  }

    public String toString() {
	return new String(startBCIndex+" - "+endBCIndex+" handler "+handlerBCIndex+" type "+catchTypeCPIndex);
    }
}
