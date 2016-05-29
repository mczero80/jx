
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
// ***** IMSub *****

final  public class IMSub extends IMBinaryOperator {

    public IMSub(CodeContainer container,int bc,int bcpos) {
	super(container);
	tag = tag | OPERATOR | COMPERATOR;	
	bytecode   = bc;
	bcPosition = bcpos;
	datatype = BCBasicDatatype.INT + (bc-BC.ISUB);
    }

    public IMSub(CodeContainer container,IMOperant lOpr,IMOperant rOpr,int datatype,int bcpos) {	
	super(container);
	tag = tag | OPERATOR | COMPERATOR;
	this.rOpr = rOpr;
	this.lOpr = lOpr;
	this.bcPosition = bcpos;
	this.datatype = datatype;
    }

    public boolean isSub() {return true;}

    public boolean isSubOrAdd() {return true;}

    public String toReadableString() {
	return "("+lOpr.toReadableString()+"-"+rOpr.toReadableString()+")";
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
	    // simpel case (c-c)
	    if (lOpr.isConstant() && rOpr.isConstant()) {
		if (opts.doVerbose("cf")) Debug.out.println("++ folding c-c "+toReadableString());
		value = lOpr.nodeToConstant().getIntValue() - rOpr.nodeToConstant().getIntValue();
		newNode = new IMConstant(container,-1,bcPosition,value);
		return newNode;
	    }
	    // case ((...)-c) => ((...)+(-c)) -> IMAdd
	    if (rOpr.isConstant()) {
		IMConstant rcOpr = rOpr.nodeToConstant();
		rcOpr.setIntValue(-rcOpr.getIntValue());
		IMAdd newAdd = new IMAdd(container,lOpr,rOpr,datatype,bcPosition);
		if (lOpr.isOperator()) return newAdd.constant_folding();
		return newAdd;
	    }
	}

	if (rOpr.isSubOrAdd() && rOpr.hasConstant()) {
	    if (lOpr.isConstant()) {
		// todo cases (c-(.+/-.))
		//if (opts.doVerbose("cf")) 
		    Debug.out.println("++ no folding  (c-(.+/-.)) "+toReadableString());
	    } else if (lOpr.isSubOrAdd() && lOpr.hasConstant()) {
		// case ((.+/-.)-(.-.)) => ((.+/-.)+(.-.)) -> IMAdd		
		if (rOpr.isSub()) {
		    ((IMSub)rOpr).swapOperants();
		    IMAdd newAdd = new IMAdd(container,lOpr,rOpr,datatype,bcPosition);
		    return newAdd.constant_folding();
		}

		if (lOpr.isSub()) {
		    // case ((.-.)-(x2+c2)) => ((x1-x2)+c)
		    //if (opts.doVerbose("cf")) 
			Debug.out.println("++ no folding  ((.-.)-(x+c)) "+toReadableString());
		    return this;
		} else {	
		    // case ((x1+c1)-(x2+c2)) => ((x1-x2)+c)
		    if (opts.doVerbose("cf")) 
			Debug.out.println("++ folding  ((x+c)-(x+c)) "+toReadableString());

		    IMOperant  x1 = lOpr.getLeftOperant();
		    IMConstant c1 = lOpr.getRightOperant().nodeToConstant();
		    IMOperant  x2 = rOpr.getLeftOperant();
		    IMConstant c2 = rOpr.getRightOperant().nodeToConstant();

		    value = c1.getIntValue() - c2.getIntValue();
		    c1.setIntValue(-value);

		    IMSub newSub = new IMSub(container,x1,x2,datatype,bcPosition);
		    
		    lOpr = (IMOperant)newSub.constant_folding();
		    rOpr = c1;

		    if (opts.doVerbose("cf")) 
			Debug.out.println("++ folding  => "+toReadableString());

		    return this;
		}
	    }
	}

	return this;
    }

    public IMOperant constant_folding2la(IMAdd Opr,int c2) {
	if (lOpr.isConstant()) {
	    // ((c1-x)+c2) => (c3-x)
	    if (opts.doVerbose("cf")) Debug.out.println("++ folding (c-x)+c "+Opr.toReadableString());
	    IMConstant lcOpr = lOpr.nodeToConstant();
	    int c3 = lcOpr.getIntValue() + c2;	    
	    if (c3==0) return rOpr;
	    lcOpr.setIntValue(c3);
	    return this;
	}
	return Opr;
    }

    // IMSub
    public void translate(Reg result) throws CompileException {
	if (rOpr.isRealConstant()) {
	    int value = ((IMConstant)rOpr).getIntValue();
	    lOpr.translate(result);

	    code.startBC(bcPosition);
	    regs.writeIntRegister(result);

	    switch (value) {
	    case 0:
		return;
	    case 1:
		code.decl(result);
		break;
	    default:
		code.subl(value,result);
	    }
	} else {
	    
	    Reg reg;
	    lOpr.translate(result);

	    code.startBC(bcPosition);

	    reg = regs.chooseIntRegister(result);
	    rOpr.translate(reg);
	    regs.writeIntRegister(result);
	    code.subl(reg,result);
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
	code.subl(reg.low,result.low);
	code.sbbl(reg.high,result.high);

	regs.freeLongRegister(reg);

	code.endBC();
    }
}
