package jx.compiler.imcode; 

import jx.classfile.constantpool.*; 
import jx.classfile.datatypes.*; 
import jx.classfile.*;
import jx.zero.Debug; 
import jx.compiler.*;
import jx.compiler.nativecode.*;
import jx.compiler.symbols.*;
import jx.compiler.execenv.*;

// ***** IMShiftRight2 *****
public class IMShiftRight2 extends IMBitOperator {

    public IMShiftRight2(CodeContainer container,int bc,int bcpos) {
	super(container);
	bytecode   = bc;
	bcPosition = bcpos;
	int i = bc - BC.ISHL;
	datatype = i & 0x01;
    }

    public String toReadableString() {
	return "("+lOpr.toReadableString()+" >>> "+rOpr.toReadableString()+")";
    }

    public IMNode constant_folding() throws CompileException {
	super.constant_folding();

	if (datatype==BCBasicDatatype.INT) {
	    if (rOpr.isConstant() && lOpr.isConstant()) {
		IMConstant lcOpr = lOpr.nodeToConstant();
		IMConstant rcOpr = rOpr.nodeToConstant();
		int value = 0;
		if (opts.doVerbose("cf")) Debug.out.println("++ folding c>>>c "+toReadableString());
		value = lcOpr.getIntValue() >>> rcOpr.getIntValue();
		lcOpr.setIntValue(value);
		return lcOpr;
	    }
	}

	return this;
    } 
  
    // IMShiftRight2
    public void translate(Reg result) throws CompileException {
	
	Reg reg,regECX;

	if (rOpr.isConstant()) {
	    lOpr.translate(result);
	    regs.writeIntRegister(result);
	    code.shrl(((IMConstant)rOpr).getIntValue(),result);
	} else {
	    regECX = regs.getIntRegister(Reg.ecx);
	    
	    if (result.any()) 
		regs.setAnyIntRegister(result,regs.chooseIntRegister(regECX));
	    
	    if (result.equals(Reg.ecx)) {
		reg = regs.chooseIntRegister(regECX);
		
		lOpr.translate(reg);
		rOpr.translate(regECX);
		
		regs.writeIntRegister(reg);
		regs.readIntRegister(regECX);
		code.shrl(reg);
		
		regs.freeIntRegister(regECX);
		regs.allocIntRegister(result,BCBasicDatatype.INT);
		code.movl(reg,result);
	    } else {
		lOpr.translate(result);
		rOpr.translate(regECX);
		
		regs.writeIntRegister(result);
		regs.readIntRegister(regECX);
		
		code.shrl(result);
	    }
	    regs.freeIntRegister(regECX);
	}
    }
   
    public void translate(Reg64 result) throws CompileException {
	if (datatype!=BCBasicDatatype.LONG) throw new Error();
	execEnv.codeLongUShr(this,lOpr,rOpr,result,bcPosition);
    }

}
