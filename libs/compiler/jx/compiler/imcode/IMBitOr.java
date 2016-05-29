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

// ***** IMBitOr *****
public class IMBitOr extends IMBitOperator {

    public IMBitOr(CodeContainer container,int bc,int bcpos) {
	super(container);
	bytecode   = bc;
	bcPosition = bcpos;
	int i = bc - BC.ISHL;
	datatype = i & 0x01;
    }

    public String toReadableString() {
	return "("+lOpr.toReadableString()+" | "+rOpr.toReadableString()+")";
    }

    public IMNode constant_folding() throws CompileException {
	super.constant_folding();

	if (datatype==BCBasicDatatype.INT) {
	    if (rOpr.isConstant() && lOpr.isConstant()) {
		IMConstant lcOpr = lOpr.nodeToConstant();
		IMConstant rcOpr = rOpr.nodeToConstant();
		int value = 0;
		if (opts.doVerbose("cf")) Debug.out.println("++ folding c|c "+toReadableString());
		value = lcOpr.getIntValue() | rcOpr.getIntValue();
		lcOpr.setIntValue(value);
		return lcOpr;
	    }

	    if (lOpr.isConstant()) {
		IMOperant swap = lOpr;
		lOpr = rOpr;
		rOpr = swap;
	    }

	    if (rOpr.isConstant()) {
    		int value = rOpr.nodeToConstant().getIntValue(); 		
                if (value==0) {
		  if (opts.doVerbose("cf")) Debug.out.println("++ folding c|0 "+toReadableString());	
                  return lOpr;
                }  
                if (value==0xffffffff) {
		  if (opts.doVerbose("cf")) Debug.out.println("++ folding c|0xffffffff "+toReadableString());
                  return rOpr;
                }
	    }
	}

	return this;
    } 
  
    // IMBitOr
    public void translate(Reg result) throws CompileException {	
	Reg reg;

	lOpr.translate(result);
	reg = regs.chooseIntRegister(result);
	rOpr.translate(reg);
	regs.writeIntRegister(result);
	code.orl(reg,result);
	regs.freeIntRegister(reg);
    }

    // IMBitOr Long

    public void translate(Reg64 result) throws CompileException {	
	Reg64 reg;

	lOpr.translate(result);
	reg = regs.chooseLongRegister(result);
	rOpr.translateLong(reg);
	
	code.startBC(bcPosition);
	
	regs.writeLongRegister(result);
	code.orl(reg.low,result.low);
	code.orl(reg.high,reg.high);

	code.endBC();

	regs.freeLongRegister(reg);
    } 
}
