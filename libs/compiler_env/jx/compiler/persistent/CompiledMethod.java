/*
 * representation of a native compiled method
 *
 * Copyright 2000 Michael Golm
 */
package jx.compiler.persistent;

import jx.compiler.symbols.SymbolTableEntryBase;
import java.util.Vector;
import jx.compiler.*;

public class CompiledMethod {
    String methodName;
    String methodType;
    int numCodeBytes;
    int vtableIndex;
    byte[] nativeCode;
    SymbolTableEntryBase[] symbols;
    LineInfo[] lineTable;

    public CompiledMethod(String methodName, String methodType, int numCodeBytes, int vtableIndex, SymbolTableEntryBase[] symbols, LineInfo[] lineTable) {
	this.methodName = methodName;
	this.methodType = methodType;
	this.numCodeBytes = numCodeBytes;
	this.vtableIndex = vtableIndex;
	this.symbols = symbols;
	this.lineTable = lineTable;
    }

    public String getName() {
	return methodName;
    }

    public String getType() {
	return methodType;
    }

    public boolean isAbstract() {
	return numCodeBytes == 0;
    }

    void setCode(byte[] code) {
	this.nativeCode = code;
    }

    public byte[] getCode() {
	return nativeCode;
    }

    public LineInfo[] getLineTable() {
	return lineTable;
    }
}
