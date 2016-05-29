
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
// ***** IMBranch ***** 

public class IMBranch extends IMOperant {

    protected IMBasicBlock[] targets;

    public IMBranch(CodeContainer container) {
	super(container);
	tag    = tag | IMNode.BRANCH | IMNode.BB_END;
	targets = null;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {	
	return this;
    }


    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	bcPosition = bcPos;
	init(newContainer);

	return this;
    }

    public IMBasicBlock[] getTargets() {
	return targets;
    }
}
