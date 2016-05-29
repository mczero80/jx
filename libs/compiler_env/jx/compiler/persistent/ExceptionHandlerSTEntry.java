package jx.compiler.persistent;

import java.io.*;

import jx.zero.Debug;

import jx.compiler.symbols.SymbolTableEntryBase; 
import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

public class ExceptionHandlerSTEntry extends SymbolTableEntryBase {
   
    public ExceptionHandlerSTEntry() {}
    
    public String getDescription() {
	return super.getDescription()+",ExceptionHandler";
    }

    public void apply(byte[] code, int codeBase) {
	Debug.assert(isReadyForApply()); 
	myApplyValue(code, codeBase, getValue()); 
    }
    
    public String toGASFormat() {
	return "0x"+Integer.toHexString(getValue());
    }
}
  
  
