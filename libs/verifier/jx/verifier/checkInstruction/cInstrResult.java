package jx.verifier.checkInstruction;

import jx.classfile.VerifyResult;
import java.util.Vector;

public class cInstrResult extends VerifyResult {
    private  int[] checkedInstr;
    private  int[] usedInstr;

    public cInstrResult(int[] checkedInstr, int[] usedInstr) {
	super(CINSTR_RESULT);
	this.checkedInstr = checkedInstr;
	this.usedInstr = usedInstr;
    }
    public cInstrResult(int[] checkedInstr, Vector usedInstr) {
	super(CINSTR_RESULT);
	this.checkedInstr = checkedInstr;
	this.usedInstr = new int[usedInstr.size()];
	for (int i = 0; i < usedInstr.size(); i++) {
	    this.usedInstr[i] = ((Integer)usedInstr.elementAt(i)).intValue();
	}
    }

    /**check if instruction with this opCode is used in method
     * @param opCode opCode of the instruction to check
     * @return 1 if instruction is used, -1 if instruction is not used, 0 if method was not checked for that instruction
     */
    public int instructionUsed(int opCode) {
	for (int i=0; i < usedInstr.length; i++) {
	    if (usedInstr[i] == opCode) {
		for (int j=0; j < checkedInstr.length; j++) {
		    if (opCode == checkedInstr[j])
			return 1;
		}
		return -1;
	    }
	}
	return 0;
    }

}
