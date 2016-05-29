package jx.compiler.persistent;

import java.io.*; 

import jx.zero.Debug;

import jx.compiler.symbols.SymbolTableEntryBase; 
import jx.compiler.symbols.StringTable;

import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

public class StringSTEntry extends SymbolTableEntryBase {
   
    String  value;
    int     stringID;
    boolean validID;

    public StringSTEntry() {}

    public StringSTEntry(String value) {
	this.value   = value;
	this.validID = false;
    }

    public String getDescription() {
	return super.getDescription()+",String:"+value;
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
	//out.writeString(value);
	if (!validID) throw new Error("invalid String ID");
	out.writeInt(stringID);
    }
    
    public void readEntry(ExtendedDataInputStream in) throws IOException {
	super.readEntry(in);
	value = in.readString();
    }

    public void registerStrings(StringTable stringTable) {
	stringID = stringTable.getIdentifier(value);
	validID  = true;
    }
}
  
  
