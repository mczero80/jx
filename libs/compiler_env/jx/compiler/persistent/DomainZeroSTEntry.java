package jx.compiler.persistent;

import java.io.*; 

import jx.zero.Debug;

import jx.compiler.symbols.SymbolTableEntryBase; 
import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

public class DomainZeroSTEntry extends SymbolTableEntryBase {
   
    public DomainZeroSTEntry() {}

    public String getDescription() {
	return super.getDescription()+",DomainZero";
    }

    public boolean isResolved() {return false;} 

    public void apply(byte[] code, int codeBase) {
	Debug.assert(isReadyForApply()); 
	applyValue(code, codeBase, getValue()); 
    }
    
    public String toGASFormat() {
	return "$0x"+Integer.toHexString(getValue());
    }    
}
  
  
