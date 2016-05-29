
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

// ***** IMReturn *****

final public class IMReturn extends IMBranch  {

    private IMOperant    retValue;
    private IMBasicBlock ownBasicBlock;

    public IMReturn(CodeContainer container,int bc,int bcpos) {
	super(container);
	tag = INSTRUCTION | BRANCH | BB_END | RETURN;

	bytecode   = bc;
	bcPosition = bcpos;
	// see processStack for datatype
	datatype   = -1;

	targets = null;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException { 
	ownBasicBlock = basicBlock;
	if (bytecode==BC.RETURN) return this;
	retValue = stack.pop();
	datatype = retValue.getDatatype();
	//stack.flush();
	//basicBlock.leave(stack);
	return this;
    }

    public void setOperant(IMOperant opr) {
	retValue = opr;
    }

    public IMOperant getOperant() {
	return retValue;
    }

    public boolean isVoidReturn() {
	return (retValue==null);
    }

    public IMNode constant_folding() throws CompileException{
	if (retValue!=null) retValue = (IMOperant)retValue.constant_folding();
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
        //throw new Error("use wrong assignNewVars method for IMReturn");
	init(newContainer);
	if (retValue!=null) {
	  return (IMOperant)retValue.assignNewVars(newContainer,slots,opr,retval,bcPos);
	} else {
	  return null;
	}
    }

    public void assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,IMCodeVector retCode,int bcPos) throws CompileException {
	IMBasicBlock epilog = container.getEpilog();
	init(newContainer);
	if (retValue!=null) {
	    retValue = (IMOperant)retValue.assignNewVars(newContainer,slots,opr,retval,bcPos);
	    retCode.add(new IMStoreLocalVariable(container,retval,retValue,bcPos));
	}
	if (!ownBasicBlock.isLastBasicBlock()) {
	   retCode.add(new IMGoto(container,-1,bcPos,epilog));
	}
    }

    public String toReadableString() {
	if (retValue==null) return "return";
	return "return "+retValue.toReadableString();	    
    }    

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	if (retValue==null) return this;
	retValue = (IMOperant)retValue.inlineCode(iCode, depth, forceInline);
	return this;
    }

    public void getCollectVars(Vector vars) { if (retValue!=null) retValue.getCollectVars(vars); }

    public int getNrRegs() { 
	if (retValue!=null) return 1+retValue.getNrRegs(); 
	return 0;
    }

    // IMReturn
    public void translate(Reg result) throws CompileException {
		
	if (retValue!=null) {
	    Reg retval = regs.getIntRegister(Reg.eax);
	    if (retValue.isConstant()) {
		int value = ((IMConstant)retValue).getIntValue();
		if (value==0) {
		    code.xorl(retval,retval);
		} else {
		    code.movl(value,retval);
		}
	    } else {
	      retValue.translate(retval);
	    }
	    regs.freeIntRegister(retval);
	}

	if (!ownBasicBlock.isLastBasicBlock()) {
	  IMBasicBlock epilog = container.getEpilog();
	  code.jmp(epilog.getNewJumpTarget());
	}
    }

    public void translate(Reg64 result) throws CompileException {		
	if (retValue==null) throw new CompileException("unsupported return without value!");

	Reg64 retval = regs.getLongRegister(Reg64.eax);
	if (opts.isOption("long")) {
	    retValue.translate(retval);
	} else {
	    code.xorl(retval.low,retval.low);
	    code.xorl(retval.high,retval.high);
	}
	regs.freeLongRegister(retval);
	if (!ownBasicBlock.isLastBasicBlock()) {
	  IMBasicBlock epilog = container.getEpilog();
	  code.jmp(epilog.getNewJumpTarget());
	}
    }
}
