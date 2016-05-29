package jx.verifier.wcet;

import jx.classfile.*;
import jx.classfile.constantpool.*;
import jx.classstore.ClassFinder;
import jx.verifier.bytecode.*;
import jx.verifier.*;

//Class for Integers for partial evaluation
public class SimInt extends SimData {
    private int value;
    private boolean valueValid;

    //returns the value of this int
    public int getValue() {
	if (valueValid)
	    return value;
	else {
	    //FEHLER
	    throw new Error("Value for stackword created @ " + Integer.toHexString(bcAddr) + 
			    " not found!");
	}
    }
    public SimInt(int bcAddr, int value) {
	super(bcAddr);
	type = TYPE_INT;
	this.value = value;
	valueValid = true;
    }

    public SimInt(int bcAddr) {
	super(bcAddr);
	type = TYPE_INT;
	valueValid = false;
    }

    public SimData copy() {
	if (valueValid) {
	    return new SimInt(bcAddr, value);
	} else {
	    return new SimInt(bcAddr);
	}
    }
	
}
