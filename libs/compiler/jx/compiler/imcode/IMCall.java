
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
// ***** IMCall *****

final  public class IMCall extends IMBranch  {

    private UnresolvedJump jumpTarget;

    public IMCall(CodeContainer container,int bc,int bcpos,IMBasicBlock label) {
	super(container);

	bytecode   = bc;
	bcPosition = bcpos;
	datatype   = -1;

	targets    = new IMBasicBlock[2];
	targets[0] = label;
    }

    public IMBasicBlock[] getTargets() {
	if (bc_next.isBasicBlock()) {
	    targets[1] = (IMBasicBlock)bc_next;
	} else {
	    targets[1] = new IMBasicBlock(container,bc_next.getBCPosition());
	    targets[1].bc_next = bc_next;
	    bc_next = targets[0];
	}
	return targets;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) {
	Debug.out.println("!!! warn: bcjsr used !!!");
	jumpTarget = targets[0].getNewJumpTarget();
	stack.push(new IMPopReturnAddr(container));	
	return this;
    }

    public String toReadableString() {
	return "call "+targets[0].toReadableString();	    
    }

    // IMCall
    public void translate(Reg result) throws CompileException {
	UnresolvedJump retAddr = new UnresolvedJump();

	if (opts.doVerbose("bc_call")) {
	    Debug.out.println("!!! warn: bcjsr used !!!");
	}

	code.startBC(bcPosition);
	
	// fixme: may use code.call() & use frame.push !!
	code.pushl(retAddr);
	code.jmp(jumpTarget);
	code.addJumpTarget(retAddr);

	code.endBC();
    }   
}
