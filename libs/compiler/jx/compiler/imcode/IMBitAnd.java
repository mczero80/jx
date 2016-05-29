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
// ***** IMBitAnd *****
public class IMBitAnd extends IMBitOperator {

    public IMBitAnd(CodeContainer container,int bc,int bcpos) {
	super(container);
	bytecode   = bc;
	bcPosition = bcpos;
	int i = bc - BC.ISHL;
	datatype = i & 0x01;
    }

    public String toReadableString() {
	return "("+lOpr.toReadableString()+" & "+rOpr.toReadableString()+")";
    }

    public IMNode constant_folding() throws CompileException {
	super.constant_folding();

	if (datatype==BCBasicDatatype.INT) {
	    if (rOpr.isConstant() && lOpr.isConstant()) {
		IMConstant lcOpr = lOpr.nodeToConstant();
		IMConstant rcOpr = rOpr.nodeToConstant();
		int value = 0;
		if (opts.doVerbose("cf")) Debug.out.println("++ folding c&c "+toReadableString());
		value = lcOpr.getIntValue() & rcOpr.getIntValue();
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
		  if (opts.doVerbose("cf")) Debug.out.println("++ folding c&0 "+toReadableString());	
                  return rOpr;
                }
		if (value==0xffffffff) {
		  if (opts.doVerbose("cf")) Debug.out.println("++ folding c&c "+toReadableString());
		  return lOpr; 
                }
	    }
	}

	return this;
    } 
  
    // IMBitAnd
    public void translate(Reg result) throws CompileException {	
	Reg reg;

	lOpr.translate(result);
	reg = regs.chooseIntRegister(result);
	rOpr.translate(reg);

	code.startBC(bcPosition);

	regs.writeIntRegister(result);
	code.andl(reg,result);

	code.endBC();

	regs.freeIntRegister(reg);
    }

    public void translate(Reg64 result) throws CompileException {	
	Reg64 reg;

	lOpr.translate(result);
	reg = regs.chooseLongRegister(result);
	rOpr.translateLong(reg);

	code.startBC(bcPosition);

	regs.writeLongRegister(result);
	code.andl(reg.low,result.low);
	code.andl(reg.high,reg.high);
	
	code.endBC();

	regs.freeLongRegister(reg);
    }    
}
