package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;


//class is abstract because variable initialization method intVars is needed!

public class WCETLocalVars extends JVMLocalVars {
    //returns new SRLvars, with same content in lVars as this.
    public JVMSRLocalVars toSRLVars() {
	return new WCETSRLocalVars(this);
    }

    //has to provide initial Values for local Variables. isStatic is true,
    //if the method whose local Variables are initialized is static.
    protected void initVars(String className,
			    String methodTypeDesc,
			    boolean isStatic)
	throws VerifyException {
	for (int i = 0; i < getNumVars(); i++) {
	    write(i, new WCETLocalVarsElement(-1));
	}
    }
    
    public JVMLocalVars copy() {
	return new WCETLocalVars(lVars);
    }

    public WCETLocalVars (String className, String methodTypeDesc, boolean isStatic, 
			  int numVars) throws VerifyException {
	super(className, methodTypeDesc, isStatic, numVars);
    }

    //constructor for copy
    protected WCETLocalVars(JVMLocalVarsElement lVars[]) {
	super(lVars);
	//make real COPIES of local Vars elements
	for (int i = 0; i < lVars.length; i++) {
	    this.lVars[i] = ((WCETLocalVarsElement)lVars[i]).copy();
	}
    }



}


