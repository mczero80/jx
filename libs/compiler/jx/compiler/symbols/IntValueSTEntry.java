package jx.compiler.symbols; 

import java.io.IOException;

import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

/** 

*/ 

public abstract class IntValueSTEntry extends SymbolTableEntryBase {

    public void apply(byte[] code, int codeBase) {
	throw new Error("this is the wrong method to call!");
    }

    public abstract void applyValue(byte[] code);

    public boolean isResolved() {
	throw new Error("this is the wrong method to call!");	
    }

    public boolean isRelative() {
	return false;
    }

    public int getValue() {
	throw new Error("this is the wrong method to call!");	
    }

    public boolean isValueRelativeToCodeBase() {
	return false;
    }

    public void initNCIndexRelative(int immediateNCIndex, int numBytes, int nextInstrNCIndex) {
	throw new Error("this is the wrong method to call!");
    }

    public void initNCIndexAbsolute(int immediateNCIndex,  int numBytes) {
	throw new Error("this is the wrong method to call!");
    }
    
    public void writeEntry(ExtendedDataOutputStream out) throws IOException {
	super.writeEntry(out);
    }
    
    public String getDescription() {
	if (isResolved()) {
	    return super.getDescription()+",IntValueSTEntry="+getValue();
	} else {
	    return super.getDescription()+",IntValueSTEntry: unresolved";
	}
    }
}
