package jx.verifier.wcet;

import jx.classfile.*;
import jx.classfile.constantpool.*;
import jx.classstore.ClassFinder;
import jx.verifier.bytecode.*;
import jx.verifier.*;

//Class for Data words for partial evaluation
public class SimData {
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_INT = 1;

    //type of this datum
    public int type;

    //address of the bytecode that generated this value
    public int bcAddr;

    // Constructor; should only be called directly for UNKNOWN types
    public SimData(int bcAddr) {
	this.bcAddr = bcAddr;
	this.type = TYPE_UNKNOWN;
    }

    public SimData copy() {
	SimData newData = new SimData(bcAddr);
	newData.type = type;
	return newData;
    }
	
}
