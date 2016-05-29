package jx.compiler.symbols;

import jx.zero.Debug;

import java.io.*;  

import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

public class CurrentThreadPointerSTEntry extends SymbolTableEntryBase {
            
    public String getDescription() {
	return super.getDescription()+",CurrentThreadPointer";
    }
    
    public int getValue() {
	return 0;
    }
    
    public void apply(byte[] code, int codeBase) {
	throw new Error();
    }

    public String toGASFormat() {
	return "0x"+Integer.toHexString(getValue());
    }

  public void writeEntry(ExtendedDataOutputStream out) throws IOException {
      super.writeEntry(out);
  }

  public void readEntry(ExtendedDataInputStream in) throws IOException {
      super.readEntry(in);
  }
  
}
  
  
