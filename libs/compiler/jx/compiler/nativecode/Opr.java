/**
 * X86-Register Class
 */

package jx.compiler.nativecode;

import jx.compiler.imcode.*;

public class Opr implements Cloneable {

    public static int REG=1;
    public static int REF=2;

    /* constants for scale value in sib-byte */
    public static int BYTE  = 0x00;
    public static int SHORT = 0x40;
    public static int WORD  = 0x80;
    public static int DWORD = 0xC0;

    public int value;
    public int tag;
    public int id=-1;
    public LocalVariable slot = null;
    //public boolean       free = false;
    protected boolean free = false;

    private int datatype=-1;

    public void setDatatype(int dtype) {
	datatype=dtype;
    }

    public int getDatatype() {
	return datatype;
    }

    public boolean conflict(Opr opr) {
	return this.value == opr.value;
    }

    public boolean equals(Opr opr) {
	return this.value == opr.value;
    }

    public void free() {free=true;}

    public void unfree() {free=false;}

    public boolean isFree() {return free;}
    
    public String toString() {
	//String rval = "r"+Integer.toString(value);
	String rval = regToString(value);

	if (id>=0) {
	    rval+="("+Integer.toString(id);
	    if (free) rval+="-"; else rval+="+";
	    if (slot!=null) rval+="m";
	    if (tag==REF) rval+="f";
	    rval+=")";
	}

	return rval;
    }

    private static final String[] REGNAME = {
	"ax", "cx", "dx", "bx", "sp", "bp", "si", "di"
    }; 

    public static String regToString(int reg) {
	if (reg<0) return "!any";
	return "%e" + REGNAME[reg]; 
    }
}
