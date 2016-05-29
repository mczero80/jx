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
// ***** IMBinaryOperator *****

public class IMBinaryOperator extends IMOperator {

    protected IMOperant rOpr;
    protected IMOperant lOpr;    

    public IMBinaryOperator(CodeContainer container) {
	super(container);
    }

    public boolean isBinaryOperator() {return true;}

    public boolean hasConstant() { return rOpr.isConstant() || rOpr.isConstant(); }

    public IMOperant getLeftOperant() {
	return lOpr;
    }

    public IMOperant getRightOperant() {
	return rOpr;
    }

    public void swapOperants() {
	IMOperant swap = rOpr;
	rOpr = lOpr;
	lOpr = swap;
    }

    public void setLeftOperant(IMOperant opr) {
	lOpr = opr;
    }

    public void setRightOperant(IMOperant opr) {
	rOpr = opr;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) {
	rOpr = stack.pop();
	lOpr = stack.pop();
	stack.push(this);
	return null;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	rOpr = (IMOperant)rOpr.inlineCode(iCode, depth, forceInline);
	lOpr = (IMOperant)lOpr.inlineCode(iCode, depth, forceInline);
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);

	rOpr = (IMOperant)rOpr.assignNewVars(newContainer,slots,opr,retval,bcPos);
	lOpr = (IMOperant)lOpr.assignNewVars(newContainer,slots,opr,retval,bcPos);	
	return this;
    }

    public IMNode constant_folding() throws CompileException {
	if (rOpr.isOperator()) {
	    rOpr = (IMOperant)((IMOperator)rOpr).constant_folding();
	}
	if (lOpr.isOperator()) {
	    lOpr = (IMOperant)((IMOperator)lOpr).constant_folding();
	}
	return this;
    }

    public IMNode constant_forwarding(IMNodeList varList) throws CompileException {
	if (rOpr.isOperator()) {
	    rOpr = (IMOperant)((IMOperator)rOpr).constant_forwarding(varList);
	}
	if (lOpr.isOperator()) {
	    lOpr = (IMOperant)((IMOperator)lOpr).constant_forwarding(varList);
	}
	return this;
    }


    public int getNrRegs() { return lOpr.getNrRegs() + rOpr.getNrRegs(); }

    public void getCollectVars(Vector vars) {  
	rOpr.getCollectVars(vars);
	lOpr.getCollectVars(vars);
    }
}
