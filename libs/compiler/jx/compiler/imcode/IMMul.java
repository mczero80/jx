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

// ***** IMMul *****

final  public class IMMul extends IMBinaryOperator {
    
    public IMMul(CodeContainer container,int bc,int bcpos) {
	super(container);
	bytecode   = bc;
	bcPosition = bcpos;
	datatype = BCBasicDatatype.INT + (bc-BC.IMUL);
    }

    public boolean isMul() {return true;}

    public boolean isDivOrMult() {return true;}    
    
    public String toReadableString() {
	return "("+lOpr.toReadableString()+"*"+rOpr.toReadableString()+")";
    }
    
    public IMNode constant_folding() throws CompileException {
	IMOperant newNode = this;
	int       value = 0;;
	
	if (rOpr.isOperator()) {
	    rOpr = (IMOperant)((IMOperator)rOpr).constant_folding();
	}
	
	if (lOpr.isOperator()) {
	    lOpr = (IMOperant)((IMOperator)lOpr).constant_folding();
	}

	if (datatype == BCBasicDatatype.INT) {

	    // simpel case (c*c)
	    if (lOpr.isConstant() && rOpr.isConstant()) {
		if (opts.doVerbose("cf")) Debug.out.println("++ folding c*c "+toReadableString());
		value = lOpr.nodeToConstant().getIntValue() * rOpr.nodeToConstant().getIntValue();
		newNode = new IMConstant(container,-1,bcPosition,value);
		return newNode;
	    }
	    
	    // case (c*(...)) => ((...)*c)
	    if (lOpr.isConstant()) {
		IMOperant swap = rOpr;
		rOpr = lOpr;
		lOpr = swap;
	    }
	    
	    // simpel case ((...)*0) and ((...)*1)
	    if (rOpr.isConstant()) {
		switch (rOpr.nodeToConstant().getIntValue()) {
		case 0:
		    if (opts.doVerbose("cf")) Debug.out.println("++ folding x*0 "+toReadableString());
		    return rOpr;
		case 1:
		    if (opts.doVerbose("cf")) Debug.out.println("++ folding x*1 "+toReadableString());
		    return lOpr;
		case 2:
		case 3:
		case 5:
		case 9:
                    // todo (x+c)*c -> (x*c)+(c*c) 
	            if (opts.doVerbose("lea")) {
                       if (lOpr.isSubOrAdd()) {
			    Debug.out.println("!! use lea or add (x+c)*c -> (x*c)+(c*c) "+toReadableString());	
                       } else {
			    Debug.out.println("++ use lea or add"+toReadableString());	
                       }
		    }
		}
	    }

	    // todo ((x*c)*(x*c))

	    if (opts.doVerbose("cf")) {
		if (lOpr.isDivOrMult() && rOpr.isDivOrMult()) {
		    if (lOpr.hasConstant() && rOpr.hasConstant()) {
			//if (opts.doVerbose("cf"))
			    Debug.out.println("++ no folding ((x*c)*(x*c)) "+toReadableString());
		    }
		}
	    }
	}
	
	// todo ...
	
	return this;
    }

    // IMMul
    public void translate(Reg result) throws CompileException {
	if (rOpr.isRealConstant()) {
            int value = ((IMConstant)rOpr).getIntValue();
	    lOpr.translate(result);
	    code.startBC(bcPosition);
	    regs.writeIntRegister(result);
            switch (value) {
		case 2:
			code.addl(result,result);
			break;
		case 3:
			code.lea(result.rdisp(0,result,2),result);
                        break;
		case 5:
			code.lea(result.rdisp(0,result,4),result);
                        break;
		case 9:
			code.lea(result.rdisp(0,result,8),result);
                        break;
		default:
	       		code.imull(value,result);
            }
	} else if (lOpr.isRealConstant()) {
            int value = ((IMConstant)lOpr).getIntValue();
	    rOpr.translate(result);
	    code.startBC(bcPosition);
	    regs.writeIntRegister(result);
            if (value==2) {
		code.addl(result,result);
            } else {	
	       	code.imull(value,result);
            }
	} else {	    
	    Reg rtmp;
	    lOpr.translate(result);
	    rtmp = regs.chooseIntRegister(result);
	    rOpr.translate(rtmp);
	    code.startBC(bcPosition);
	    regs.writeIntRegister(result);
	    code.imull(rtmp,result);
	    regs.freeIntRegister(rtmp);
        }
	code.endBC();
    } 

    public void translate(Reg64 result) throws CompileException {
	if (datatype!=BCBasicDatatype.LONG) throw new Error();
	execEnv.codeLongMul(this,lOpr,rOpr,result,bcPosition);
    }
}
