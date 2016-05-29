package jx.verifier.bytecode;

import jx.classfile.*;
import jx.classfile.constantpool.*;


/** Alle Befehle die Zeiger auf den Constantpool beinhalten */
public class BCCPArgOp extends ByteCode {
    protected int CPIndex;
    protected ConstantPool cPool;

    public int getCPIndex() { return CPIndex;}
    public ConstantPoolEntry getCPEntry() { return cPool.constantEntryAt(CPIndex);} 
    public ConstantPool getCP() {return cPool;}
    public BCCPArgOp(int opCode, ByteIterator code, ConstantPool cPool) {
	super(opCode, code);
	this.cPool = cPool;

	if (opCode == LDC) { //only 1 byte index
	    CPIndex = byteArgs[0] & 0xff;
	} else {
	    CPIndex = ((byteArgs[0]<<8)&0xff00) | (byteArgs[1] &0xff);
	}
    }

    public String toString() {
	String retval =  super.toString();
	ConstantPoolEntry cpe = getCPEntry();
	if (cpe instanceof ClassMemberCPEntry) {
	    retval += " " + ((ClassMemberCPEntry)cpe).getDescription();
	} else {
	    retval += " " + cpe.toString();
	}
	return retval;	
    }

}
