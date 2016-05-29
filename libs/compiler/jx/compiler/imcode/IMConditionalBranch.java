
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
// ***** IMConditionalBranch *****

final  public class IMConditionalBranch extends IMBranch  {

    private IMOperant rOpr;
    private IMOperant lOpr;
    private UnresolvedJump jumpTarget;

    public IMConditionalBranch(CodeContainer container,int bc,int bcpos,IMBasicBlock label) {
	super(container);

	bytecode   = bc;
	bcPosition = bcpos;
	datatype   = -1;

	targets    = new IMBasicBlock[2];
	targets[1] = label;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) {
	if (bytecode<BC.IF_ICMPEQ ||
	    bytecode==BC.IFNULL   ||
	    bytecode==BC.IFNONNULL ) {
	    rOpr = null;
	    lOpr = stack.pop();
	} else {
	    rOpr = stack.pop();
	    lOpr = stack.pop();
	}
	jumpTarget = targets[1].getNewJumpTarget();
	//stack.flush();
	//basicBlock.leave(stack);
	saveVarStackMap(frame);
	return this;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	if (rOpr!=null) rOpr = (IMOperant)rOpr.inlineCode(iCode, depth, forceInline);
	lOpr = (IMOperant)lOpr.inlineCode(iCode, depth, forceInline);
	return this;
    }

    public IMNode constant_folding() throws CompileException{
	if (rOpr!=null) rOpr = (IMOperant)rOpr.constant_folding();
	lOpr = (IMOperant)lOpr.constant_folding();

	solveTargets();

	if (lOpr.isConstant()) {
	    IMConstant clOpr = lOpr.nodeToConstant();
	    if (rOpr==null) {
		switch (bytecode) {
		case BC.IFEQ: 
		    {
			if (opts.doVerbose("cf")) Debug.out.println("++ folding "+toReadableString());
			if (clOpr.getIntValue()==0) {
			    IMGoto ngoto = new IMGoto(container,bcPosition,targets[1],jumpTarget);
			    if (opts.doVerbose("cf")) Debug.out.println("++ true => "+ngoto.toReadableString());
			    return ngoto;
			} else {
			    targets[1].removeJumpTarget(jumpTarget);
			    IMGoto ngoto = new IMGoto(container,bcPosition,targets[0],targets[0].getNewJumpTarget());
			    if (opts.doVerbose("cf")) Debug.out.println("++ false => "+ngoto.toReadableString());
			    return ngoto;
			}
		    }
		case BC.IFNE:
		    {
			if (opts.doVerbose("cf")) Debug.out.println("++ folding "+toReadableString());
			if (clOpr.getIntValue()!=0) {
			    IMGoto ngoto = new IMGoto(container,bcPosition,targets[1],jumpTarget);
			    if (opts.doVerbose("cf")) Debug.out.println("++ true => "+ngoto.toReadableString());
			    return ngoto;
			} else {
			    targets[1].removeJumpTarget(jumpTarget);
			    IMGoto ngoto = new IMGoto(container,bcPosition,targets[0],targets[0].getNewJumpTarget());
			    if (opts.doVerbose("cf")) Debug.out.println("++ false => "+ngoto.toReadableString());
			    return ngoto;
			}
		    }
		case BC.IFNULL:
		    {
			    if (opts.doVerbose("cf")) Debug.out.println("++ folding "+toReadableString());
			    if (clOpr.getIntValue()==0) {
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[1],jumpTarget);
				    if (opts.doVerbose("cf")) Debug.out.println("++ true => "+ngoto.toReadableString());
				    return ngoto;
			    } else {
				    targets[1].removeJumpTarget(jumpTarget);
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[0],targets[0].getNewJumpTarget());
				    if (opts.doVerbose("cf")) Debug.out.println("++ false => "+ngoto.toReadableString());
				    return ngoto;
			    }
		    }
		case BC.IFNONNULL:
		    {
			    if (opts.doVerbose("cf")) Debug.out.println("++ folding "+toReadableString());
			    if (clOpr.getIntValue()!=0) {
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[1],jumpTarget);
				    if (opts.doVerbose("cf")) Debug.out.println("++ true => "+ngoto.toReadableString());
				    return ngoto;
			    } else {
				    targets[1].removeJumpTarget(jumpTarget);
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[0],targets[0].getNewJumpTarget());
				    if (opts.doVerbose("cf")) Debug.out.println("++ false => "+ngoto.toReadableString());
				    return ngoto;
			    }
		    }
		case BC.IFLT:
		    {
			    if (opts.doVerbose("cf")) Debug.out.println("++ folding "+toReadableString());
			    if (clOpr.getIntValue()<0) {
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[1],jumpTarget);
				    if (opts.doVerbose("cf")) Debug.out.println("++ true => "+ngoto.toReadableString());
				    return ngoto;
			    } else {
				    targets[1].removeJumpTarget(jumpTarget);
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[0],targets[0].getNewJumpTarget());
				    if (opts.doVerbose("cf")) Debug.out.println("++ false => "+ngoto.toReadableString());
				    return ngoto;
			    }
		    }
		case BC.IFGE:
		    {
			    if (opts.doVerbose("cf")) Debug.out.println("++ folding "+toReadableString());
			    if (clOpr.getIntValue()>=0) {
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[1],jumpTarget);
				    if (opts.doVerbose("cf")) Debug.out.println("++ true => "+ngoto.toReadableString());
				    return ngoto;
			    } else {
				    targets[1].removeJumpTarget(jumpTarget);
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[0],targets[0].getNewJumpTarget());
				    if (opts.doVerbose("cf")) Debug.out.println("++ false => "+ngoto.toReadableString());
				    return ngoto;
			    }
		    }
		case BC.IFGT:
		    {
			    if (opts.doVerbose("cf")) Debug.out.println("++ folding "+toReadableString());
			    if (clOpr.getIntValue()>0) {
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[1],jumpTarget);
				    if (opts.doVerbose("cf")) Debug.out.println("++ true => "+ngoto.toReadableString());
				    return ngoto;
			    } else {
				    targets[1].removeJumpTarget(jumpTarget);
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[0],targets[0].getNewJumpTarget());
				    if (opts.doVerbose("cf")) Debug.out.println("++ false => "+ngoto.toReadableString());
				    return ngoto;
			    }
		    }
		case BC.IFLE:
		    {
			    if (opts.doVerbose("cf")) Debug.out.println("++ folding "+toReadableString());
			    if (clOpr.getIntValue()<=0) {
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[1],jumpTarget);
				    if (opts.doVerbose("cf")) Debug.out.println("++ true => "+ngoto.toReadableString());
				    return ngoto;
			    } else {
				    targets[1].removeJumpTarget(jumpTarget);
				    IMGoto ngoto = new IMGoto(container,bcPosition,targets[0],targets[0].getNewJumpTarget());
				    if (opts.doVerbose("cf")) Debug.out.println("++ false => "+ngoto.toReadableString());
				    return ngoto;
			    }
		    }
		}
	    } else { 
		if (rOpr.isConstant()) {
		    if (opts.doVerbose("cf")) Debug.out.println("++ no folding "+toReadableString());
		}
	    }
	}

	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);
	
	if (rOpr!=null) rOpr = (IMOperant)rOpr.assignNewVars(newContainer,slots,opr,retval,bcPos);
	lOpr = (IMOperant)lOpr.assignNewVars(newContainer,slots,opr,retval,bcPos);
	
	return this;
    }

    private void solveTargets() {
	if (targets[0]==null) {
	    if (bc_next.isBasicBlock()) {
		targets[0] = (IMBasicBlock)bc_next;
	    } else {
		targets[0] = new IMBasicBlock(container,bc_next.getBCPosition());
		targets[0].bc_next = bc_next;
		bc_next = targets[0];
	    } 
	}
    }
    
    public IMBasicBlock[] getTargets() {
	solveTargets();
	return targets;
    }

    public void swapJumpTargets() {
	IMBasicBlock swap;

	swap       = targets[0];
	targets[0] = targets[1];;
	targets[1] = swap;

	jumpTarget = targets[1].getNewJumpTarget();

	switch (bytecode) {
	case BC.IFEQ:
	    bytecode = BC.IFNE;
	    break;
	case BC.IFNE:
	    bytecode = BC.IFEQ;
	    break;
	case BC.IFLT:
	    bytecode = BC.IFGE;
	    break;
	case BC.IFGE:
	    bytecode = BC.IFLT;
	    break;
	case BC.IFGT:
	    bytecode = BC.IFLE;
	    break;
	case BC.IFLE:
	    bytecode = BC.IFGT;
	    break;
	case BC.IF_ICMPEQ:
	    bytecode = BC.IF_ICMPNE;
	    break;
	case BC.IF_ICMPNE:
	    bytecode = BC.IF_ICMPEQ;
	    break;
	case BC.IF_ICMPLT:
	    bytecode = BC.IF_ICMPGE;
	    break;
	case BC.IF_ICMPGE:
	    bytecode = BC.IF_ICMPLT;
	    break;
	case BC.IF_ICMPGT:
	    bytecode = BC.IF_ICMPLE;
	    break;
	case BC.IF_ICMPLE:
	    bytecode = BC.IF_ICMPGT;
	    break;
	case BC.IF_ACMPEQ:
	    bytecode = BC.IF_ACMPNE;
	    break;
	case BC.IF_ACMPNE:
	    bytecode = BC.IF_ACMPEQ;
	    break;
	case BC.IFNULL:
	    bytecode = BC.IFNONNULL;
	    break;
	case BC.IFNONNULL:
	    bytecode = BC.IFNULL;
	    break;
	}
    }

    public String toReadableString() {
	switch (bytecode) {
	case BC.IFEQ:
	    return "if "+lOpr.toReadableString()+"==0 goto "+targets[1].toLabel();
	case BC.IFNE:
	    return "if "+lOpr.toReadableString()+"!=0 goto "+targets[1].toLabel();	    
	case BC.IFLT:
	    return "if "+lOpr.toReadableString()+"< 0 goto "+targets[1].toLabel();
	case BC.IFGE:
	    return "if "+lOpr.toReadableString()+">=0 goto "+targets[1].toLabel();
	case BC.IFGT:
	    return "if "+lOpr.toReadableString()+"> 0 goto "+targets[1].toLabel();
	case BC.IFLE:
	    return "if "+lOpr.toReadableString()+"<=0 goto "+targets[1].toLabel();
	case BC.IF_ICMPEQ:
	    return "if "+lOpr.toReadableString()+"=="+rOpr.toReadableString()+" goto "+targets[1].toLabel();
	case BC.IF_ICMPNE:
	    return "if "+lOpr.toReadableString()+"!="+rOpr.toReadableString()+" goto "+targets[1].toLabel();
	case BC.IF_ICMPLT:
	    return "if "+lOpr.toReadableString()+"<"+rOpr.toReadableString()+" goto "+targets[1].toLabel();
	case BC.IF_ICMPGE:
	    return "if "+lOpr.toReadableString()+">="+rOpr.toReadableString()+" goto "+targets[1].toLabel();
	case BC.IF_ICMPGT:
	    return "if "+lOpr.toReadableString()+">"+rOpr.toReadableString()+" goto "+targets[1].toLabel();
	case BC.IF_ICMPLE:
	    return "if "+lOpr.toReadableString()+"<="+rOpr.toReadableString()+" goto "+targets[1].toLabel();
	case BC.IF_ACMPEQ:
	    return "if "+lOpr.toReadableString()+"=="+rOpr.toReadableString()+" goto "+targets[1].toLabel();
	case BC.IF_ACMPNE:
	    return "if "+lOpr.toReadableString()+"!="+rOpr.toReadableString()+" goto "+targets[1].toLabel();
	case BC.IFNULL:
	    return "if "+lOpr.toReadableString()+"==null goto "+targets[1].toLabel();
	case BC.IFNONNULL:
	    return "if "+lOpr.toReadableString()+"!=null goto "+targets[1].toLabel();
	}
	return "<unknown branch>";
    }

    public void getCollectVars(Vector vars) { 
	if (rOpr!=null) {
	    rOpr.getCollectVars(vars);
	}
	lOpr.getCollectVars(vars);
    }

    public int getNrRegs() { 
	if (rOpr!=null) {
	    return lOpr.getNrRegs() + rOpr.getNrRegs();
	} else {
	    return lOpr.getNrRegs();
	}
    }

    // IMConditionalBranch
    public void translate(Reg result) throws CompileException {
	int ip;

	lOpr.translate(result);
	
	if (bytecode < BC.IF_ICMPEQ) {
	    if (!lOpr.isComperator()) {
		/* no compare after add, sub needed */
		code.test(result,result);
	    }
	    ip=code.getCurrentIP();
	    code_condjump(bytecode-BC.IFEQ);
	} else if (bytecode < BC.IF_ACMPEQ) {
	    if (rOpr.isRealConstant()) {
		int value = ((IMConstant)rOpr).getIntValue();
		if (value==0) {
		    code.test(result,result);
		} else {
		    code.cmpl(value,result);
		}
	    } else {
		Reg rtmp = regs.chooseIntRegister(result);
		rOpr.translate(rtmp);
		regs.readIntRegister(result);
		code.cmpl(rtmp,result);
	    }
	    ip=code.getCurrentIP();
	    code_condjump(bytecode-BC.IF_ICMPEQ);
	} else if (bytecode < BC.IFNULL) {
	    if (rOpr.isRealConstant()) {
		int value = ((IMConstant)rOpr).getIntValue();
		if (value==0) {
		    code.test(result,result);
		} else {
		    code.cmpl(value,result);
		}
	    } else {
		Reg rtmp = regs.chooseIntRegister(result);
		rOpr.translate(rtmp);
		regs.readIntRegister(result);
		code.cmpl(rtmp,result);
	    }
	    ip=code.getCurrentIP();
	    code_condjump(bytecode-BC.IF_ACMPEQ);
	} else {
	    code.test(result,result);
	    ip=code.getCurrentIP();
	    code_condjump(bytecode-BC.IFNULL);
	}

	// all Branches get a stackMap
	execEnv.codeStackMap(this,ip);
	//addDebugInfo(frame.stackMapToString(this));
    }   
    
    private void code_condjump(int opr) throws CompileException {
	switch (opr) {
	case 0:
	    // equal
	    code.je(jumpTarget);
	    break;
	case 1:
	    // not equal
	    code.jne(jumpTarget);
	    break;
	case 2:
	    // less
	    code.jl(jumpTarget);
	    break;
	case 3:
	    // greater or equal
	    code.jge(jumpTarget);
	    break;
	case 4:
	    // greater
	    code.jg(jumpTarget);
	    break;
	case 5:
	    // less or equal
	    code.jle(jumpTarget);
	    break;
	default:
	    throw new CompileException("unknown branch "+Integer.toString(opr));
	}	
    }
}
