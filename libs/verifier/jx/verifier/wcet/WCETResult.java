package jx.verifier.wcet;

import jx.classfile.VerifyResult;
import jx.classfile.MethodData;


public class WCETResult extends VerifyResult {
    private ExecutionTime eTime = null;
    private MethodData rTCheckedMethod = null;
    public ExecutionTime getETime() {return eTime;}
    public WCETResult(ExecutionTime eTime) {
	super(WCET_RESULT);
	this.eTime = eTime;
    }
    public WCETResult(MethodData rTCheckedMethod) {
	super(WCET_RESULT);
	this.rTCheckedMethod = rTCheckedMethod;
    }
}
