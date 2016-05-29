/**
 * X86-Register Class
 */

package jx.compiler.nativecode;

import jx.compiler.imcode.*;
import jx.compiler.symbols.SymbolTableEntryBase;

final public class Reg extends Opr implements RegObj, Cloneable {

    public static Reg any = new Reg(-1);

    public static Reg eax = new Reg(0);
    public static Reg ecx = new Reg(1);
    public static Reg edx = new Reg(2);
    public static Reg ebx = new Reg(3);
    public static Reg esp = new Reg(4);
    public static Reg ebp = new Reg(5);
    public static Reg esi = new Reg(6);
    public static Reg edi = new Reg(7); 

    public boolean valid = true;

    public Reg(int reg) {
	if (true) {
	    if (reg==-1) value = -1;
	    else this.value = reg & 0x07;
	} else {
	    this.value = reg & 0x07;
	}
	this.tag   = Opr.REG;
    }

    public void free() {
	if (this== any ) throw new Error("any");
	if (this== eax ) throw new Error("eax");
	if (this== ecx ) throw new Error("ecx");
	if (this== edx ) throw new Error("edx");
	if (this== ebx ) throw new Error("ebx");
	if (this== esp ) throw new Error("esp");
	if (this== ebp ) throw new Error("ebp");
	if (this== esi ) throw new Error("esi");
	if (this== edi ) throw new Error("edi"); 
	free=true;
    }

    public Ref ref() {
	Ref nref = Ref.eax.getClone();
	nref.value = value;
	return nref;
    }

    public boolean any() {
	return value==-1;
    }

    public void push(MethodStackFrame frame) {
	frame.push(getDatatype(),this);
    }

    public Ref rdisp(int disp) {
	Ref nref = Ref.eax.getClone();
	nref.value = value;
	nref.disp  = disp;
	return nref;
    }

    public Ref rdisp(SymbolTableEntryBase entry) {
	Ref nref = Ref.eax.getClone();
	nref.value = value;
	nref.sym_disp  = entry;
	return nref;
    }

    public Ref rdisp(int disp,Reg index,int size) {
	Ref nref = Ref.eax.getClone();
	nref.value = value;
	nref.setdisp(disp,index,size);
	return nref;
    }
    
    public Reg getClone() {
	Reg nreg = null;
	try {
	    nreg = (Reg)this.clone();
	} catch (CloneNotSupportedException ex) {
	    System.err.println("!!!! CloneNotSupportedException !!!!");
	    nreg = new Reg(value);
	}
	if (nreg==null) {
	    System.err.println("can`t clone myself :-(");
	    nreg = new Reg(value);
	}
	return nreg;
    }
}
