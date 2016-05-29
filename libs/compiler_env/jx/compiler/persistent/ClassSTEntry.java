package jx.compiler.persistent;

import jx.zero.Debug;

import java.io.*;  

import jx.compiler.*;
import jx.compiler.symbols.SymbolTableEntryBase;
import jx.compiler.symbols.StringTable;
import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

public class ClassSTEntry extends SymbolTableEntryBase {
   
    String className;
    int     stringID;
    boolean validID;

    public ClassSTEntry() {}
    public ClassSTEntry(String className) {
	this.className = className;
    }
    
    public String getDescription() {
	return super.getDescription()+",Class:"+className;
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
	//out.writeString(className);
	if (!validID) throw new Error("invalid String ID");
	out.writeInt(stringID);
    }
    
    public void readEntry(ExtendedDataInputStream in) throws IOException {
	super.readEntry(in);
	className = in.readString();
    }

    public void registerStrings(StringTable stringTable) {
	stringID = stringTable.getIdentifier(className);
	validID  = true;
    }
}
  
  
