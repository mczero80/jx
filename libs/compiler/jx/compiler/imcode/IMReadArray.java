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

// ***** IMReadArray *****

final public class IMReadArray extends IMOperant {

    private IMOperant iOpr;
    private IMOperant aOpr;

    public IMReadArray(CodeContainer container,int bc,int bcpos) {
	super(container);
	bytecode   = bc;
	bcPosition = bcpos;
	datatype   = BCBasicDatatype.INT + (bc-BC.IALOAD);
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) {
	iOpr = stack.pop();
	aOpr = stack.pop();
	//stack.store();
	stack.push(this);
	return null;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	aOpr = (IMOperant)aOpr.inlineCode(iCode, depth, forceInline);
	iOpr = (IMOperant)iOpr.inlineCode(iCode, depth, forceInline);
	return this;
    }

    public IMNode constant_folding() throws CompileException{
	aOpr = (IMOperant)aOpr.constant_folding();
	iOpr = (IMOperant)iOpr.constant_folding();
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException{
	bcPosition = bcPos;
	init(newContainer);

	aOpr = (IMOperant)aOpr.assignNewVars(newContainer,slots,opr,retval,bcPos);
	iOpr = (IMOperant)iOpr.assignNewVars(newContainer,slots,opr,retval,bcPos);
	
	return this;
    }

    public String toReadableString() {
	return aOpr.toReadableString()+"["+iOpr.toReadableString()+"]";
    }   

    public int getNrRegs() { return aOpr.getNrRegs() + iOpr.getNrRegs(); }

    public void getCollectVars(Vector vars) { 
	aOpr.getCollectVars(vars);
	iOpr.getCollectVars(vars);
    }

    // IMReadArray
    public void translate(Reg result) throws CompileException {
	
	Reg array = regs.chooseIntRegister(result);
	aOpr.translate(array);

	code.startBC(bcPosition);

	if (aOpr.checkReference()) 
	    execEnv.codeCheckReference(this,array,bcPosition);

	if (false && iOpr.isConstant() && (((IMConstant)iOpr).getIntValue()<128)) {
	    int index = ((IMConstant)iOpr).getIntValue();
	    if (aOpr.checkArrayRange(index))
		execEnv.codeCheckArrayRange(this,array,index,bcPosition);
	    execEnv.codeGetArrayField(this,array,datatype,index,result,bcPosition);
	} else {
	    Reg indx = regs.chooseIntRegister(result,array);
	    iOpr.translate(indx);
	    if (aOpr.checkArrayRange(iOpr)) 
		execEnv.codeCheckArrayRange(this,array,indx,bcPosition);
	    execEnv.codeGetArrayField(this,array,datatype,indx,result,bcPosition);
	    regs.freeIntRegister(indx);
	}

	code.endBC();

	regs.freeIntRegister(array);
    }

    // IMReadArray
    public void translate(Reg64 result) throws CompileException {
	
	Reg array = regs.chooseIntRegister(result.low,result.high);
	aOpr.translate(array);

	code.startBC(bcPosition);

	if (aOpr.checkReference()) 
	    execEnv.codeCheckReference(this,array,bcPosition);

	Reg indx = regs.chooseIntRegister(result.low,result.high,array);
	iOpr.translate(indx);
	if (aOpr.checkArrayRange(iOpr)) 
		execEnv.codeCheckArrayRange(this,array,indx,bcPosition);

	execEnv.codeGetArrayFieldLong(this,array,datatype,indx,result,bcPosition);
	regs.freeIntRegister(indx);

	code.endBC();

	regs.freeIntRegister(array);
    }
}
