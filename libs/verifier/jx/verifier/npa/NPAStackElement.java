package jx.verifier.npa;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;

public class NPAStackElement extends JVMOPStackElement {
    //merges the two StackElements. Has to return a NEW(!) JVMOPStackElement (even if this
    //and other were the same!), whose prev pointer must be null! 
    //throws Exception if not mergeable
    public JVMOPStackElement merge(JVMOPStackElement other) throws VerifyException {
	return new NPAStackElement(type.merge(((NPAStackElement)other).getType()), 
				   null, 
				   ADDR_MERGE);
    }
    //must return true, if the data of other equals the data of this.
    public boolean dataEquals(JVMOPStackElement other) {
	return (((NPAStackElement)other).getType().equals(this.type));
    }

    private NPAValue type;
    public NPAValue getType() { return type;}
    public void setType(NPAValue type) { this.type = type;}
    public NPAStackElement(NPAValue type, JVMOPStackElement prev, int bcAddr) {
	super(prev, bcAddr);
	this.type = type;
    }
    //returns copy of this with prev==null
    public NPAStackElement copyNull() {
	return new NPAStackElement(type, null, getBCAddr());
    }

    public String toString() {
	return (getPrev() != null)? getPrev().toString() + ": <" + type + "("+ addressString() + ")>"
	    : 
	    "<" + type + "(" + addressString() + ")>";
    }
}
