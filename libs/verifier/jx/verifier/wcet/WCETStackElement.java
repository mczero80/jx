package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;

public class WCETStackElement extends JVMOPStackElement {
    
    public boolean necessary = false; //if true, this stack element is necessary for partial evaluation of loops

    //merges the two StackElements. Has to return a NEW(!) JVMOPStackElement (even if this
    //and other were the same!), whose prev pointer must be null! 
    //throws Exception if not mergeable
    public JVMOPStackElement merge(JVMOPStackElement other) throws VerifyException {
	return new WCETStackElement(null);
    }

    //must return true, if the data of other equals the data of this.
    public boolean dataEquals(JVMOPStackElement other) {
	return true;
    }


    public WCETStackElement(JVMOPStackElement prev) {
	super(prev);
    }
    public WCETStackElement(JVMOPStackElement prev, int bcAddr) {
	super(prev, bcAddr);
    }
    
    public String toString() {
	return (getPrev() != null)? getPrev().toString() + ";("+ necessary + ")"
	    : 
	    "(" + necessary + ")";
    }
}
