package jx.compiler.persistent;

import java.io.*;

import jx.zero.Debug;

import jx.compiler.symbols.SymbolTableEntryBase;
import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

public class VMAbsoluteSTEntry extends VMSupportSTEntry {
    public VMAbsoluteSTEntry(String name) {
	super(name);
    }
}
  
  
