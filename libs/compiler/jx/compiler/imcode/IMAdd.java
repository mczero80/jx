
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
// ***** IMAdd *****

final  public class IMAdd extends IMBinaryOperator {

    public IMAdd(CodeContainer container,int bc,int bcpos) {
	super(container);
	tag = tag | OPERATOR | COMPERATOR;
	bytecode   = bc;
	bcPosition = bcpos;
	datatype = BCBasicDatatype.INT + (bc-BC.IADD);
    }

    public IMAdd(CodeContainer container,IMOperant lOpr,IMOperant rOpr,int datatype,int bcpos) {	
	super(container);
	tag = tag | OPERATOR | COMPERATOR;
	this.rOpr = rOpr;
	this.lOpr = lOpr;
	this.bcPosition = bcpos;
	this.datatype = datatype;
    }

    public boolean isAdd() {return true;}

    public boolean isSubOrAdd() {return true;}

    public String toReadableString() {
	return "("+lOpr.toReadableString()+"+"+rOpr.toReadableString()+")";
    }

    public IMNode constant_folding() throws CompileException {

	if (rOpr.isOperator()) {
	    rOpr = (IMOperant)((IMOperator)rOpr).constant_folding();
	}

	if (lOpr.isOperator()) {
	    lOpr = (IMOperant)((IMOperator)lOpr).constant_folding();
	}

	if (datatype==BCBasicDatatype.INT) {
	    // simpel case (c+c)
	    if (lOpr.isConstant() && rOpr.isConstant()) {
		if (opts.doVerbose("cf")) Debug.out.println("++ folding c+c "+toReadableString());
		int value = lOpr.nodeToConstant().getIntValue()+rOpr.nodeToConstant().getIntValue();
		IMConstant newNode = new IMConstant(container,-1,bcPosition,value);		
		return newNode;
	    }

	    // (c+...) => (...+c)
	    if (lOpr.isConstant()) {
		IMOperant swap = lOpr;
		lOpr = rOpr;
		rOpr = swap;
	    }

	    // case ((x+/-x)+c2) => (x+c3)
	    if (rOpr.isConstant() && lOpr.isOperator()) {
		int value = rOpr.nodeToConstant().getIntValue();
		if (lOpr instanceof IMAdd) {
		    return ((IMAdd)lOpr).constant_folding2la(this,value);
		}
		if (lOpr instanceof IMSub) {
		    return ((IMSub)lOpr).constant_folding2la(this,value);
		}
	    }
	}

	if (opts.doVerbose("cf")) {
	    if (lOpr.isSubOrAdd() && rOpr.isSubOrAdd()) {
		if (lOpr.hasConstant() && rOpr.hasConstant()) {
		    Debug.out.println("++ no folding ((x+/-x)+(x+/-x)) "+toReadableString());
		    // todo cases ((x+/-x)+(x+/-x))
		}
	    }
	}

	return this;
    }

    public IMOperant constant_folding2la(IMAdd Opr,int value) {
	// case ((x+c1)+c2) => (x+c3)
	if (rOpr.isConstant()) {
	    if (opts.doVerbose("cf")) Debug.out.println("++ folding (x+c)+c) "+Opr.toReadableString());
	    IMConstant rcOpr = rOpr.nodeToConstant();
	    rcOpr.setIntValue(rcOpr.getIntValue() + value);	    
	    return this;
	}
	return Opr;
    }

    // IMAdd
    public void translate(Reg result) throws CompileException {

	if (lOpr.isConstant()) {
	    IMOperant swap = lOpr;
	    lOpr = rOpr;
	    rOpr = swap;
	}
   
	if (rOpr.isConstant()) {
	    int value = rOpr.nodeToConstant().getIntValue();

            IMConstant mult=null;
            int mvalue=0;
	    if (opts.isOption("lea") && lOpr.isMul() && lOpr.getRightOperant().isRealConstant()) {
              mult = lOpr.getRightOperant().nodeToConstant();
              mvalue = mult.getIntValue();
              switch (mvalue) {	
		case 3:
                    lOpr.getLeftOperant().translate(result);
		    code.startBC(bcPosition);
		    regs.writeIntRegister(result);
		    code.lea(result.rdisp(value,result,2),result);
	            break;	
		case 5:
                    lOpr.getLeftOperant().translate(result);
		    code.startBC(bcPosition);
		    regs.writeIntRegister(result);
		    code.lea(result.rdisp(value,result,4),result);
	            break;	
		case 9:
                    lOpr.getLeftOperant().translate(result);
		    code.startBC(bcPosition);
		    regs.writeIntRegister(result);
		    code.lea(result.rdisp(value,result,8),result);
	            break;	
		default:
			mult=null;
              }
              if (mult!=null && opts.doVerbose("lea")) Debug.out.println("++ lea "+toReadableString());	
	    }

            if (mult==null) {
		    lOpr.translate(result);	    

		    code.startBC(bcPosition);
		    regs.writeIntRegister(result);
	    
		    switch (value) {
		    case 0:
			return;
		    case 1:
			code.incl(result);
			break;
		    default:
			code.addl(value,result);
		    }
            } 
	} else {	    
	    Reg reg;
	    
	    lOpr.translate(result);
	    if (result.any()) {
		Debug.out.println(toReadableString());
		throw new Error(getLineInfo()+" get any");
	    }

	    reg = regs.chooseIntRegister(result);
	    rOpr.translate(reg);

	    code.startBC(bcPosition);
	    regs.writeIntRegister(result);

	    code.addl(reg,result);
	    regs.freeIntRegister(reg);
        }

	code.endBC();
    } 

    public void translate(Reg64 result) throws CompileException {
	Reg64 reg;
	    
	lOpr.translate(result);

	reg = regs.chooseLongRegister(result);
	rOpr.translate(reg);

	code.startBC(bcPosition);

	regs.writeLongRegister(result);
	code.addl(reg.low,result.low);
	code.adcl(reg.high,reg.high);

	regs.freeLongRegister(reg);

	code.endBC();
    } 
}
