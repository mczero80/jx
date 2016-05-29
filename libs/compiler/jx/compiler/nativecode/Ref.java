/**
 * X86-Register Class
 */

package jx.compiler.nativecode;

import jx.compiler.symbols.SymbolTableEntryBase;

final public class Ref extends Opr implements Cloneable {

    public int disp;
    public SymbolTableEntryBase sym_disp;
    public boolean hasIndex;
    public int sib;
    public int scale;

    public static Ref eax = new Ref(0);
    public static Ref ecx = new Ref(1);
    public static Ref edx = new Ref(2);
    public static Ref ebx = new Ref(3);
    public static Ref esp = new Ref(4); /*special*/
    public static Ref ebp = new Ref(5);
    public static Ref esi = new Ref(6);
    public static Ref edi = new Ref(7);

    public Ref(int value) {
	this.value    = value & 0x07;
	this.disp     = 0;
	this.hasIndex = false;
	this.sib      = 0;
	this.scale    = 0;
	this.tag      = REF;
    }

    public Ref(int value, int disp) {
	this.value = value;
	this.disp  = disp;
	this.tag   = REF;
    }

    public Ref disp(int d) {
	Ref nref = null;
	try {
	    nref = (Ref)this.clone();
	} catch (CloneNotSupportedException ex) {
	    System.err.println("!!!! CloneNotSupportedException !!!!");
	    nref = new Ref(value);
	}
	nref.disp    = d;
	return nref;
    }

    public Ref disp(int d,Reg index,int size) {
	Ref nref = null;
	try {
	    nref = (Ref)this.clone();
	} catch (CloneNotSupportedException ex) {
	    System.err.println("!!!! CloneNotSupportedException !!!!");
	    nref = new Ref(value);
	}
	nref.setdisp(d,index,size);
	return nref;
    }

    public void setdisp(int d,Reg index,int size) {
	disp = d;
	hasIndex = true;
	switch (size) {
	case 0:
	case 1: /* 8 Bit */
	    sib = (Opr.BYTE|(index.value<<3)|value);
	    break;
	case 2: /* 16 Bit */
	    sib = (Opr.SHORT|(index.value<<3)|value);
	    break;
	case 4: /* 4 = 32 Bit */
	     sib = (Opr.WORD|(index.value<<3)|value);
	     break;
	case 8: /* 64 Bit */
	    sib = (Opr.DWORD|(index.value<<3)|value);
	    break;
	default: 
	    throw new Error("unsupported scalar for sib-index");
	}
    } 

    public Ref getClone() {
	Ref nref = null;
	try {
	    nref = (Ref)this.clone();
	} catch (CloneNotSupportedException ex) {
	    System.err.println("!!!! CloneNotSupportedException !!!!");
	    nref = new Ref(value);
	}
	return nref;
    }
}
