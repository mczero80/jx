
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
// ***** IMBasicBlock *****

public class IMBasicBlock extends IMNode {

    public  boolean done;

    private String       dbgString;
    private String       labelText;
    private boolean      subroutine;
    private boolean      isEpilog;
    private boolean      isLoopEntry;
    private int counter;
    
    private IMBasicBlock[] successors;
    private int fptr=0;

    private IMOperant[] enterStack;
    private IMOperant[] leaveStack;

    private int nextJumpTarget;
    private UnresolvedJump[] jumpTargets = new UnresolvedJump[10];

    private ExceptionTableSTEntry handler = null;

    public IMBasicBlock(CodeContainer container,boolean epilog) {
	super(container);
	tag = IMNode.BASICBLOCK;
	this.bcPosition=-1;
	this.bytecode=0;
	dbgString        = null;
	enterStack       = null;
	leaveStack       = null;
	successors       = null;
	done             = false;
	subroutine       = false;
	isEpilog         = epilog;
	isLoopEntry      = false;
	counter          = 1;
	nextJumpTarget   = 0;
	labelText = "END";
  
    }	

    public IMBasicBlock(CodeContainer container,int bcPosition) {
	super(container);
        tag = IMNode.BASICBLOCK;
	this.bcPosition=bcPosition;
	this.bytecode=0;
	dbgString  = null;
	enterStack = null;
	leaveStack = null;
	successors = null;
	done       = false;
	subroutine = false;
	counter    = 1;
	nextJumpTarget = 0;
	if (bcPosition==0) labelText = "START";
	else labelText="B" + Integer.toString(bcPosition);
    }

    public void setExceptionHandler(ExceptionTableSTEntry handler) {
	this.handler = handler;
	labelText = "H" + Integer.toString(bcPosition);
    }

    public boolean isEpilog() {
	return isEpilog;
    }

    public boolean isLoopEntry() {
	return isLoopEntry;
    }

    public void setLoopEntry(boolean flag) {
	isLoopEntry=flag;
    }

    public boolean isLastBasicBlock() {
	// if the next basic block is epilog then we are the last 
	// basic block
	for (IMNode node=next;node!=null;node=node.next) {
	    if (node.isBasicBlock()) {
		return ((IMBasicBlock)node).isEpilog();
	    }
	}
	return true;
    }

    public void incCounter() {
	counter++;
    }

    public void setInitStack(IMOperant[] stack) {
	enterStack = stack;
    }

    public IMBasicBlock[] getSucc() {
	return successors;
    }
    
    public void processBasicBlock(VirtualOperantenStack stack) throws CompileException {
	done = true;

	stack.init(enterStack);
	
	IMNode node  = bc_next;
	IMNode instr = null;

	//System.err.println("process basic block: "+this.toReadableString());
	//System.err.println("stack: "+stack.toTypeString());
	
	while (node!=null) {

	    // first process stack for current bytecode

	    try {
		instr = ((IMNode)node).processStack(stack,this);
	    } catch (Exception ex) {
		if (verbose && System.err!=null) {
		    System.err.println("process basic block: "+this);
		    System.err.println("Exception (1): "+node.toString());
		    System.err.println(node.toReadableString());
		    ex.printStackTrace();
		}
		System.exit(-1);
	    }

	    // second do we reach end of basic block ?

	    try {
		if (instr!=null) {
		    stack.join(instr);
		    //if (verbose) System.err.println(instr.toReadableString());
		    // Return from Subroutine
		    if (instr.isReturnSubroutine()) {
			subroutine = true;
			break;
		    }
		    // IMGoto, IMConditionalBranch, IMReturn etc.
		    if (instr.isBranch()) {
			successors = ((IMBranch)instr).getTargets();
			if (successors == null) {
			    // Return 
			    break;
			} 
			// GOTO etc.
			break;
		    }
		}
	    } catch (Exception ex) {
		if (verbose && System.err!=null) System.err.println("2: "+node.toString());
		ex.printStackTrace();
		System.exit(-1);
	    }

	    if (node.isEndOfBasicBlock()) {
		// node is no branch but end of basicblock
		if (node.bc_next!=null) {
		    if (successors==null && node.bc_next.isBasicBlock()) {
			successors = new IMBasicBlock[1];
			successors[0] = (IMBasicBlock)node.bc_next;
		    } else {
			if (verbose && System.err!=null) {
			    System.err.println("warning: bad imcode !\n");
			    System.err.println("bytecode position: "+Integer.toString(node.getBCPosition()));
			}
			System.exit(-1);
		    }
		}
		break;
	    }
	    node = node.bc_next;
	}

	//stack.store();

	leaveStack = stack.leave();
    }

    public boolean isSubroutine() {
	return subroutine;
    }

    public IMOperant[] getLeaveStack() {
	return leaveStack;
    }
     
    public void setDebugString(String msg) {
	dbgString=msg;
    }

    public String getDebugString() {
	String ret = " #"+Integer.toString(counter);
	if (enterStack!=null) {
	    for (int i=0;i<enterStack.length;i++) if (enterStack[i]!=null) {
		ret += " "+ BCBasicDatatype.toString(enterStack[i].getDatatype());
	    }
	}
	    
	if (dbgString!=null) ret+=" "+dbgString;	
	
	return ret;
    }

    public String toReadableString() {
        String ret=toLabel();
	if (isLowPriorityPath()) {
	    ret = "-" + ret;
	} else {
	    ret = "+" + ret;
	}
	if (enterStack==null) return ret;
	for (int i=0; i<enterStack.length;i++) {
	    if (enterStack[i]!=null) ret = ret + enterStack[i].toReadableString();
	}
	return ret;
    }

    public String toLabel() {
	return labelText;
    }

    public String toString() {
	return super.toString() + "B #" + Integer.toString(counter);
    }

    public UnresolvedJump getNewJumpTarget() {
	if (nextJumpTarget>=jumpTargets.length) {
	    UnresolvedJump newArray[] = new UnresolvedJump[nextJumpTarget+10];
	    for (int i=0;i<jumpTargets.length;i++) {
		newArray[i] = jumpTargets[i];
	    }
	    jumpTargets = newArray;
	}	    
	jumpTargets[nextJumpTarget] = new UnresolvedJump();
	jumpTargets[nextJumpTarget].setBCPosition(bcPosition);
	return jumpTargets[nextJumpTarget++];
    }

    public void removeJumpTarget(UnresolvedJump jumpTarget) {
	for (int i=0;i<jumpTargets.length;i++) {
	    if (jumpTarget==jumpTargets[i]) {
		jumpTargets[i]=null;
		return;
	    }
	}
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	//labelText = container.getBCMethod().getName()+":"+labelText;
	bcPosition=bcPos;
	isEpilog=false;
	init(newContainer);
	return this;
    }

    public void constant_forwarding() throws CompileException {
	IMNodeList varList = new IMNodeList();
	IMNode node = next;
	while (node!=null && !node.isBasicBlock()) {
	    node.constant_forwarding(varList);
	    node = node.next;
	}
    }

    // IMBasicBlock
    public void translate(Reg result) throws CompileException {
	IMNode node=next;

	try {	

	    // insert prolog
	    if (bcPosition==0) {
		execEnv.codeProlog();
	    }

	    if (handler!=null) {
		code.addExceptionTarget(handler);
	    }

	    // insert jump targets
	    for (int i=0;i<jumpTargets.length;i++) {
		if (jumpTargets[i]!=null) code.addJumpTarget(jumpTargets[i]);
	    }
	    
	    // add epilog
	    if (isEpilog) {
		execEnv.codeEpilog();
	    } else {
		node = next;
		
		regs.startBasicBlock();
		
		while (node!=null && !node.isBasicBlock()) {
		    //System.err.println(node.toReadableString());
		    switch (node.getDatatype()) {
		    case BCBasicDatatype.FLOAT:
		    case BCBasicDatatype.DOUBLE:
			code.nop();
			//execEnv.codeThrow(this,-11,bcPosition);
			break;
		    case BCBasicDatatype.LONG: 
			if (opts.isOption("long")) {
			    Reg64 result64 = regs.chooseLongRegister();
			    node.translateLong(result64);
			    regs.freeLongRegister(result64);
			} else {
			    code.nop();
			}
			break;
		    case BCBasicDatatype.INT:
		    case BCBasicDatatype.BOOLEAN:
		    case BCBasicDatatype.VOID:
		    default:
			Reg rval = regs.getIntRegister(Reg.any);
			node.translate(rval);
			regs.freeIntRegister(rval);
		    }
		    node = node.next;
		}
		
		regs.endBasicBlock();
	    }
	} catch (CompileException ex) {
	    Debug.out.println(toReadableString());
	    IMNode dnode = next;		
	    while (dnode!=null && !dnode.isBasicBlock()) {
		if (dnode==node) {
		    Debug.out.println("->"+dnode.toReadableString());
		} else {
		    Debug.out.println("  "+dnode.toReadableString());
		}
 		dnode=dnode.next;
	    }
	    throw ex;
	}
    }

 

}
