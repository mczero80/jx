package jx.compiler.execenv;

import java.util.Vector;

public interface NativeCodeContainer {
    public BinaryCode getMachineCode();
    public Vector     getInstructionTable();
    public int        getLocalVarSize();
}
