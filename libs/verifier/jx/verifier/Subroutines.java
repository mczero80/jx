package jx.verifier;

import java.util.Vector;
import java.lang.Error;
import jx.classfile.*;
import jx.classfile.constantpool.*;
import jx.verifier.bytecode.*;


public class Subroutines {
    private SubroutineData srs[];
    private BCLinkList code;
    public void registerSrs(BCLinkList code) {
	this.code = code;
	Vector subs = new Vector(4);
	BCBranch actJsr;
	
    FOR:
	for (ByteCode act = code.getFirst(); act != null; act = act.next) {
	    //find all jsrs and save in appropriate srdata element.
	    if (act.getOpCode() == ByteCode.JSR ||
		act.getOpCode() == ByteCode.JSR_W ) {
		actJsr = (BCBranch) act;
		//search SubroutineData for this jsr-target
		SubroutineData srData;
		for (int i=0; i< subs.size(); i++) {
		    srData = (SubroutineData) subs.elementAt(i);
		    if (srData.getBeginAddress() == actJsr.getTargetAddress()) {
			srData.add(act);
			continue FOR;
		    }
		}
		// no appropriate srData in subs. --> create new one.
		srData = new SubroutineData(act);
		subs.addElement(srData);
	    }
		    
		
	}
	//all subroutines are in subs-vector. save in SubroutineData-array
	srs = new SubroutineData[subs.size()];
	for (int i = 0; i < subs.size(); i++) {
	    srs[i] = (SubroutineData) subs.elementAt(i);
	}
    }

    private SubroutineData getSr(int beginAddress) {
	for (int i = 0; i < srs.length; i++) {
	    if (srs[i].getBeginAddress() == beginAddress) 
		return srs[i];
	}
	throw new Error("Internal Error: jsr-Target not registered in Subroutines!");
    }
    private SubroutineData getSr(ByteCode jsr) {
	BCBranch jsrB = (BCBranch) jsr;
	return getSr(jsrB.getTargetAddress());
    }

    //transform state 'oldState' as if the jsr and the subroutine was executed and returned
    //after executing the subroutine all states to which execution might return are changed
    //if necessary
    //oldState is not changed
    public void transformState(JVMState oldState, ByteCode jsr) 
	throws VerifyException {
	SubroutineData sr = getSr(jsr);

	//Verify Subroutine
	sr.setBeforeState(oldState.copy());
	sr.getBeforeState().setlVars(sr.getBeforeState().getlVars().toSRLVars()); 
	SubroutineVerifier srVer = 
	    new SubroutineVerifier(code.getBCAt(sr.getBeginAddress()),
				   sr,
				   this,
				   oldState.getMv().getClassName(),
				   oldState.getMv().getMethod());
	srVer.runChecks();
	sr.setAfterState(srVer.getFinalState());
	//update States of all callers
	sr.updateCallerStates();
	return;
	
    }

    public String toString() {
	String ret = "Subroutines: ";
	SubroutineData srData;
	if (srs.length ==0) 
	    return ret+"(none)\n";
	ret +="\n";
	for (int i=0; i< srs.length; i++) {
	    ret += srs[i].toString();
	}
	
	return ret;
    }

}
