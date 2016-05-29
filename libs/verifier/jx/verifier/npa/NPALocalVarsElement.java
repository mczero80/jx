package jx.verifier.npa;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;

public class NPALocalVarsElement extends JVMLocalVarsElement {
    
    public JVMLocalVarsElement merge(JVMLocalVarsElement other) 
	throws VerifyException {
	NPAValue mergedType = type.merge(((NPALocalVarsElement)other).getType());
	if (mergedType != type) {
	    return new NPALocalVarsElement(mergedType, -1);
	} else {
	    return this;
	}
    }
	    
    public boolean dataEquals(JVMLocalVarsElement other) {
	return type.equals(((NPALocalVarsElement)other).getType());
    }
    
    private NPAValue type;
    public NPAValue getType() { return type;}
    public void setType(NPAValue type) { this.type = type;}

    public NPALocalVarsElement(NPAValue type, int bcAddr) {
	super(bcAddr);
	this.type = type;
    }
    public String toString() {
	return type + "("+((getBcAddr() >=0)?Integer.toHexString(getBcAddr()):"init")+")";
	
    }
}

