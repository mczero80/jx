package jx.verifier.typecheck;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import jx.verifier.JVMLocalVarsElement;
import java.util.Vector;


public class TCLocalVarsElement extends JVMLocalVarsElement{
    private TCTypes type;
    public TCTypes getType() {return type;}
    public TCLocalVarsElement(TCTypes type, int bcAddr) {
	super(bcAddr);
	this.type = type;
    }
    public String toString() {
	return type.toString()+"("+
	    ((getBcAddr() >=0)? Integer.toHexString(getBcAddr()) : "init")+")";
	
    }
    //returns the merged lVars Element. throws Exception if not mergeable.
    //if merging changes the data of the Element, 
    //a NEW(!) JVMLocalVarsElement should be returned (i.e. merge should not change this or
    // other!).
    public JVMLocalVarsElement merge(JVMLocalVarsElement other) 
	throws VerifyException {
	TCLocalVarsElement otherVar = (TCLocalVarsElement) other;
	if (otherVar == null) {
	    return new TCLocalVarsElement(TCTypes.T_UNKNOWN, -1);
	} else if (this != otherVar) {
		TCTypes tmp = this.getType().merge(otherVar.getType());
		//if something changed, merge returns a new object
		if(tmp != this.getType()) {
		    return new TCLocalVarsElement(tmp, -1);
		} else {
		    return this;
		}
	} else {
	    //this == otherVar
	    return this;
	}

    }

    //must return true, if the data of other equals the data of this.
    public boolean dataEquals(JVMLocalVarsElement other) {
	return (other != null)? 
	    ((TCLocalVarsElement)other).getType().equals(type) :
	    false;
    }


}

