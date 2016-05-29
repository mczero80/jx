
package jx.compiler.imcode; 
import jx.classfile.constantpool.*; 
import jx.classfile.datatypes.*; 
import jx.classfile.*;
import jx.zero.Debug; 
import jx.compiler.*;
import jx.compiler.nativecode.*;
import jx.compiler.symbols.*;
import jx.compiler.execenv.*;
import java.util.Vector;
// ***** IMShiftLeft *****
final public class IMShiftLeft extends IMBitOperator {

    public IMShiftLeft(CodeContainer container,int bc,int bcpos) {
	super(container);
	bytecode   = bc;
	bcPosition = bcpos;
	int i = bc - BC.ISHL;
	datatype = i & 0x01;
    }

    public String toReadableString() {
	return "("+lOpr.toReadableString()+" << "+rOpr.toReadableString()+")";
    }

    public IMNode constant_folding() throws CompileException {
	super.constant_folding();

	if (datatype==BCBasicDatatype.INT) {
	    if (rOpr.isConstant() && lOpr.isConstant()) {
		IMConstant lcOpr = lOpr.nodeToConstant();
		IMConstant rcOpr = rOpr.nodeToConstant();
		if (opts.doVerbose("cf")) Debug.out.println("++ folding c<<c "+toReadableString());
		lcOpr.setIntValue(lcOpr.getIntValue() << rcOpr.getIntValue());
		return lcOpr;
	    }
	}

	return this;
    } 
  
    // IMShiftLeft
    public void translate(Reg result) throws CompileException {	
	Reg reg,regECX;

	if (rOpr.isRealConstant()) {
            int value = rOpr.nodeToConstant().getIntValue();
	    lOpr.translate(result);
	    regs.writeIntRegister(result);
            if (value==1) {
		code.addl(result,result);
	    } else {	
	        code.shll(value,result);
            }
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
		code.shll(reg);
		
		regs.freeIntRegister(regECX);
		regs.allocIntRegister(result,BCBasicDatatype.INT);
		code.movl(reg,result);
	    } else {
		lOpr.translate(result);
		rOpr.translate(regECX);
		
		regs.writeIntRegister(result);
		regs.readIntRegister(regECX);
		
		code.shll(result);
	    }
	    regs.freeIntRegister(regECX);
	}
    }

    public void translate(Reg64 result) throws CompileException {
	if (datatype!=BCBasicDatatype.LONG) throw new Error();
	if (rOpr.isConstant()) {
	    IMConstant rcOpr = (IMConstant)rOpr;
	    lOpr.translate(result);
	    regs.writeLongRegister(result);
	    code.shld(rcOpr.getIntValue(),result.low,result.high);
	    code.shll(rcOpr.getIntValue(),result.low);
	} else {
	    execEnv.codeLongShl(this,lOpr,rOpr,result,bcPosition);
	}
    }
}
