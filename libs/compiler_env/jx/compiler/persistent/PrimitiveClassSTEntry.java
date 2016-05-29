package jx.compiler.persistent;

import java.io.*;  

import jx.zero.Debug;

import jx.compiler.symbols.SymbolTableEntryBase; 
import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

public class PrimitiveClassSTEntry extends SymbolTableEntryBase {
    
    int type; // typecode conforming to the constantpool types
    
    public PrimitiveClassSTEntry() {}
    
    public PrimitiveClassSTEntry(int type) {
	this.type = type;
    }
    
    public String getDescription() {
	return super.getDescription()+",PrimitiveClass:"+type;
    }
    
    public int getValue() {
	return 0;
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
	out.writeInt(type);
    }

    public void readEntry(ExtendedDataInputStream in) throws IOException {
	super.readEntry(in);
	type = in.readInt();
    }
}
  
  
