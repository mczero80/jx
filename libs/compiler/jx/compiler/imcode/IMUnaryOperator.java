
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
// ***** IMUnaryOperator *****

public class IMUnaryOperator extends IMOperator {

    protected IMOperant operant;
    
    public IMUnaryOperator(CodeContainer container) {
	super(container);
    }
    
    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	operant=(IMOperant)operant.inlineCode(iCode, depth, forceInline);
	return this;
    }
    
    public IMNode constant_forwarding(IMNodeList varList) throws CompileException {
       if (operant.isOperator()) {
	    operant = (IMOperant)((IMOperator)operant).constant_forwarding(varList);
	}
	return this;
    }

    public IMNode constant_folding() throws CompileException {
	if (operant.isOperator()) {
	    operant = (IMOperant)((IMOperator)operant).constant_folding();
	}
	return this;
    }	

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);
	operant = (IMOperant)operant.assignNewVars(newContainer,slots,opr,retval,bcPos);	
	return this;
    }

    public void getCollectVars(Vector vars) { 
	operant.getCollectVars(vars); 
	return; 
    }

    public int getNrRegs() { return operant.getNrRegs(); }
}
