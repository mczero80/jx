package jx.compiler.symbols;

import java.io.IOException;
import jx.zero.Debug;

import jx.compiler.*;
import jx.compiler.symbols.SymbolTableEntryBase;
import jx.compiler.symbols.StringTable;
import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

import jx.classfile.ExceptionHandlerData;
import jx.classfile.constantpool.*;

public class ExceptionTableSTEntry extends UnresolvedJump {

    ExceptionHandlerData handler;    
    String className;
    int     stringID;
    boolean validID;

    public ExceptionTableSTEntry(ConstantPool cPool, ExceptionHandlerData handler) {
	int cpIndex = handler.getCatchTypeCPIndex();	
	this.handler   = handler;

	if (cpIndex==0) {
	    this.className = "any";
	} else {
	    this.className = cPool.classEntryAt(cpIndex).getClassName();
	}
    }

    public String getDescription() {
	return super.getDescription()+",symbols.ExceptionTableSTEntry";
    }
    
    public boolean isResolved() {
	return false;
    }

    public int getValue() {
	Debug.throwError();
	return 0;
    }

    public void apply(byte[] code, int codeBase) {
	Debug.throwError();
    }

    public void writeEntry(ExtendedDataOutputStream out) throws IOException {
	super.writeEntry(out);
	out.writeInt(handler.getStartBCIndex());
	out.writeInt(handler.getEndBCIndex());
	//out.writeString(className);
	if (!validID) throw new Error("invalid String ID");
	out.writeInt(stringID);
    }  

    public void registerStrings(StringTable stringTable) {
	stringID = stringTable.getIdentifier(className);
	validID  = true;
    }
}

