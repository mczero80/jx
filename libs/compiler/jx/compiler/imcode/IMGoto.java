
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

// ***** IMGoto *****

final  public class IMGoto extends IMBranch  {

    private UnresolvedJump jumpTarget;

    public IMGoto(CodeContainer container,int bc,int bcpos,IMBasicBlock label) {
	super(container);

	bytecode    = bc;
	bcPosition  = bcpos;
	datatype    = -1;

	targets    = new IMBasicBlock[1];
	targets[0] = label;
    }

    public IMGoto(CodeContainer container,int bcpos,IMBasicBlock label, UnresolvedJump target) {
	super(container);

	bytecode    = 0;
	bcPosition  = bcpos;
	datatype    = -1;

	targets    = new IMBasicBlock[1];
	targets[0] = label;

	jumpTarget  = target;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) {
	//stack.flush();
	//basicBlock.leave(stack);

	// get jumpTarget Object 
	jumpTarget = targets[0].getNewJumpTarget();
	saveVarStackMap(frame);
	return this;
    }
    
    public String toReadableString() {
	return "goto "+targets[0].toReadableString();	    
    }

    // IMGoto
    public void translate(Reg result) throws CompileException {
	int ip=code.getCurrentIP();
	code.jmp(jumpTarget);
	addDebugInfo(frame.stackMapToString(this));
	execEnv.codeStackMap(this,ip);
    }   
}
