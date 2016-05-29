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
// ***** IMInc *****

final public class IMInc extends IMVarAccess  {

    private int value;

    public IMInc(CodeContainer container,int bc,int bcpos,int ivar,int value) {
	super(container);

	bytecode   = bc;
	bcPosition = bcpos;
	this.ivar  = ivar;
	this.value = value;
	writeAccess=true;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
	LocalVariable[] vars = frame.getLocalVars();
	/*
	if (ivar<vars.length) {
	    if (vars[ivar].getDatatype()!=BCBasicDatatype.INT) {
		addDebugInfo("v"+ivar+BCBasicDatatype.toSymbol(vars[ivar].getDatatype()));
	    }
	}
	*/
	//lvar = frame.getLocalVar(ivar,datatype);
	lvar = frame.getLocalVar(ivar,BCBasicDatatype.INT);
	stack.store2(bcPosition,ivar);
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);
	
	if (opr[ivar]!=null) {
	    throw new CompileException("Can`t propagate left operant! var.index="+ivar+" operant="+opr[ivar].toReadableString());
	}

	ivar = slots[ivar];
	
	return this;
    }

    public String toReadableString() {
	if (value==1) return "vi"+Integer.toString(ivar)+"++";
	if (value==-1) return "vi"+Integer.toString(ivar)+"--";
	return "vi"+Integer.toString(ivar)+"+="+Integer.toString(value);
    }

    public int getNrRegs() { return 1; }

    public void getCollectVars(Vector vars) { vars.addElement(this); return; }

    // IMInc
    public void translate(Reg no_result) throws CompileException {
	
	if (value==0) return;

	Reg ireg = regs.getIntRegister(Reg.any);
	regs.readIntRegisterFromSlot(lvar,ireg,BCBasicDatatype.INT);
	regs.writeIntRegister(ireg);

	switch (value) {
	case 1:
	    code.incl(ireg);
	    break;
	case -1:
	    code.decl(ireg);
	    break;
	default:
	    code.addl(value,ireg);
	}
	
	regs.writeIntRegisterToSlot(ireg,lvar);
	regs.freeIntRegister(ireg);
    }
}
