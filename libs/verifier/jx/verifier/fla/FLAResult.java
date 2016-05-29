package jx.verifier.fla;

import jx.classfile.VerifyResult;


public class FLAResult extends VerifyResult {
    boolean systemFinal;
    boolean leaf;

    public boolean isSystemFinal() {return systemFinal;}
    public boolean isLeaf() {return leaf;}
    public FLAResult() {
	super(FLA_RESULT);
	leaf = false;
	systemFinal = false;
    }
}
