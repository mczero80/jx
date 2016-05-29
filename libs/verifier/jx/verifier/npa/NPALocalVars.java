package jx.verifier.npa;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;


public class NPALocalVars extends JVMLocalVars implements NPALocalVarsInterface {
    //returns new SRLvars, with same content in lVars as this.
    public JVMSRLocalVars toSRLVars() {
	return new NPASRLocalVars(this);
    }

    //has to provide initial Values for local Variables. isStatic is true,
    //if the method whose local Variables are initialized is static.
    protected void initVars(String className,
			    String methodTypeDesc,
			    boolean isStatic)
	throws VerifyException {
	int i = 0;
	if (!isStatic) {
	    write(i, new NPALocalVarsElement(NPAValue.newNONNULL(), -1)); //this
	    i++;
	}
	for(;i < getNumVars(); i++) {
	    write(i, new NPALocalVarsElement(NPAValue.newOTHER(), -1));
	}
    }

    public JVMLocalVars copy() {return new NPALocalVars(getLVars());} 

    public NPALocalVars(String className, String methodTypeDesc, boolean isStatic, 
			int numVars) throws VerifyException {
	super(className, methodTypeDesc, isStatic, numVars);
    }

    //constructor for copy
    protected NPALocalVars(JVMLocalVarsElement lVars[]) {
	super(lVars);
    }

    public void write(int index, NPAValue type, int bcAddr) throws VerifyException {
	write(index, new NPALocalVarsElement(type, bcAddr));
    }

    //returns OHTER if lVars[index] not found, because writes of non-reference values are
    //ignored, so even a typechecked method might issue a read to an empty loca var.
    //see NPABCEffect.genericOp(...)
    public NPAValue NPAread(int index) {
	try {
	    return ((NPALocalVarsElement)read(index)).getType();
	} catch (VerifyException e) {
	    return NPAValue.newOTHER();
	}
    }

    //find all local Vars with same id as value (must be a valid id!) and change their
    //value to newVal.
    public void setValue(NPAValue value, int newVal) {
	for (int i = 0; i < lVars.length; i++) {
	    if (lVars[i] != null && 
		((NPALocalVarsElement)lVars[i]).getType().getId() == value.getId()) {
		//the values are the same so update information
		((NPALocalVarsElement)lVars[i]).setType(new NPAValue(newVal, value.getId()));
	    }
	}
    }
    
    
}


