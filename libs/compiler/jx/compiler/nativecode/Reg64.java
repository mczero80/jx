/**
 * X86-Register Paar for 64 Bit 
 */

package jx.compiler.nativecode;

import jx.classfile.datatypes.BCBasicDatatype;
import jx.compiler.symbols.SymbolTableEntryBase;
import jx.compiler.imcode.*;

final public class Reg64 implements RegObj, Cloneable {

    static public Reg64 any = new Reg64(Reg.any,Reg.any);
    static public Reg64 eax = new Reg64(Reg.eax,Reg.edx);
    static public Reg64 ebx = new Reg64(Reg.ebx,Reg.ecx);
    static public Reg64 esi = new Reg64(Reg.esi,Reg.edi);

    public Reg low;
    public Reg high;

    public Reg64() {
	throw new Error("wrong Reg64 constuctor");
    }

    public Reg64(Reg reg) {
	/*
	Reg low  = Reg.any.getClone();
	Reg high = Reg.any.getClone();
	*/
	low  = Reg.any.getClone();
	high = Reg.any.getClone();
	if (reg.equals(Reg.eax) || reg.equals(Reg.edx)) {
	    low  = Reg.eax.getClone();
	    high = Reg.edx.getClone();
	} else if (reg.equals(Reg.ebx) || reg.equals(Reg.ecx)) {
	    low  = Reg.ebx.getClone();
	    high = Reg.ecx.getClone();
	} else if (reg.equals(Reg.esi) || reg.equals(Reg.edi)) {
	    low  = Reg.esi.getClone();
	    high = Reg.edi.getClone();
	}
    }

    public Reg64(Reg low,Reg high) {
	this.low = low;
	this.high = high;
    }

    public static Reg64 extendLowRegister(Reg low) {	
	if (low.equals(Reg.eax)) {
	    return new Reg64(low,Reg.edx.getClone());
	} else if (low.equals(Reg.ebx)) {
	    return new Reg64(low,Reg.ecx.getClone());
	} else if (low.equals(Reg.esi)) {
	    return new Reg64(low,Reg.edx.getClone());
	}
	return null;
    }

    public int getDatatype() {
	return BCBasicDatatype.LONG;
    }

    public void push(MethodStackFrame frame) {
	frame.push(BCBasicDatatype.LONG, low);
	frame.push(BCBasicDatatype.LONG, high);
    }

    public boolean equals(Reg64 reg) {
	if (reg.low.equals(low) && reg.high.equals(high)) return true;
	if (reg.low.equals(low) || reg.high.equals(high)) throw new Error("unsupported Reg64 !!");
	return false;
    }

    public Reg64 getDeepClone() {
	Reg64 nreg = new Reg64(low);
	if (nreg.low==null || nreg.high==null) throw new Error(low.toString());
	return nreg;
    }

    public Reg64 getClone() {
	Reg64 nreg = null;
	try {
	    nreg = (Reg64)this.clone();
	} catch (CloneNotSupportedException ex) {
	    System.err.println("!!!! CloneNotSupportedException !!!!");
	    nreg = new Reg64(low,high);
	}
	return nreg;
    }
}
