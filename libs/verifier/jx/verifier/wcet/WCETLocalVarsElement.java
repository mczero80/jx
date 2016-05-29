package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;

//Class is defined abstract, because it does not hold any information yet!
// has to be added by subclasses (e.g. typecheck.TCOPStackElement
public class WCETLocalVarsElement extends JVMLocalVarsElement {
    
    public boolean necessary = false; //if true, this lvars element is needed for partial evaluation of loops

    //returns the merged lVars Element. throws Exception if not mergeable.
    //if merging changes the data of the Element, 
    //a NEW(!) JVMLocalVarsElement should be returned (i.e. merge should not change this or
    // other!).
    public JVMLocalVarsElement merge(JVMLocalVarsElement other) 
	throws VerifyException {
	//Erst mal gar nichts machen; evtl ein ...
	//FEHLER
	return this;
    }
    //must return true, if the data of other equals the data of this.
    public  boolean dataEquals(JVMLocalVarsElement other) {
	return true;
    }

    public WCETLocalVarsElement(int bcAddr) {
	super(bcAddr);
    }
    public String toString() {
	return "("+necessary+")";
	
    }
    public WCETLocalVarsElement copy() {
	WCETLocalVarsElement tmp = new WCETLocalVarsElement(getBcAddr());
	tmp.necessary = necessary;
	return tmp;
    }
}

