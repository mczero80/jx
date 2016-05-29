package jx.compiler.symbols;

import jx.zero.Debug;
import java.io.*;  
import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

public class TCBOffsetSTEntry extends SymbolTableEntryBase {

    public final static int STACKTOP       = 1;

    int kind;

    public TCBOffsetSTEntry() {}

    public TCBOffsetSTEntry(int kind) {
	this.kind = kind;
    }

    public String getDescription() {
	return super.getDescription()+",ProfileSTEntry";
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
      out.writeInt(kind);
  }

  public void readEntry(ExtendedDataInputStream in) throws IOException {
      super.readEntry(in);
      kind   = in.readInt();
  }
  
}
  
  
