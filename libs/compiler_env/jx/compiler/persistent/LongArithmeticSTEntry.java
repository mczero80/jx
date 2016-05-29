package jx.compiler.persistent;

import java.io.*;  

import jx.zero.Debug;

import jx.compiler.symbols.SymbolTableEntryBase; 
import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

public class LongArithmeticSTEntry extends SymbolTableEntryBase {
    int operation;

    public static final int DIV  = 1;
    public static final int REM  = 2;
    public static final int SHR  = 3;
    public static final int SHL  = 4;
    public static final int USHR = 5;
    public static final int CMP  = 6;
    public static final int MUL  = 7;

    public LongArithmeticSTEntry() {}

    public LongArithmeticSTEntry(int operation) {
	this.operation = operation;
    }
    
    public String getDescription() {
	return super.getDescription()+",AllocArray";
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
	out.writeInt(operation);
    }
    
    public void readEntry(ExtendedDataInputStream in) throws IOException {
	super.readEntry(in);
	operation = in.readInt();
    }

}
  
  
