package jx.compiler.persistent;

import java.io.*;

import jx.zero.Debug;
import jx.compiler.symbols.SymbolTableEntryBase; 
import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

public class DEPFunctionSTEntry extends SymbolTableEntryBase {
   
    String className, methodName, methodSignature;

    public DEPFunctionSTEntry() {}
    public DEPFunctionSTEntry(String className, String methodName, String methodSignature) {
	this.className = className;
	this.methodName = methodName;
	this.methodSignature = methodSignature;
    }
    
    public String getDescription() {
	return super.getDescription()+",DEP:"+className+ "."+ methodName+ methodSignature;
    }

    public void apply(byte[] code, int codeBase) {
	Debug.assert(isReadyForApply()); 
	myApplyValue(code, codeBase, getValue()); 
    }

    public String toGASFormat() {
	return "0x"+Integer.toHexString(getValue());
    }

    public void writeEntry(ExtendedDataOutputStream out) throws IOException {
	super.writeEntry(out);
	out.writeString(className);
	out.writeString(methodName);
	out.writeString(methodSignature);
    }
    
    public void readEntry(ExtendedDataInputStream in) throws IOException {
	super.readEntry(in);
	className = in.readString();
	methodName = in.readString();
	methodSignature = in.readString();
    }
}
  
  
